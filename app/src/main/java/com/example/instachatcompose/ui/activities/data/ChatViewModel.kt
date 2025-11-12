package com.example.instachatcompose.ui.activities.data

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.instachatcompose.ui.activities.data.core.NotificationHelper
import com.example.instachatcompose.ui.activities.data.crypto.CryptoUtil
import com.example.instachatcompose.ui.activities.data.local.FriendEntity
import com.example.instachatcompose.ui.activities.data.local.GroupEntity
import com.example.instachatcompose.ui.activities.data.local.UserEntity
import com.example.instachatcompose.ui.activities.data.repository.ChatRepository
import com.example.instachatcompose.ui.activities.mainpage.ChatItem
import com.example.instachatcompose.ui.activities.mainpage.Friend
import com.example.instachatcompose.ui.activities.mainpage.GroupChat
import com.google.firebase.Firebase
import com.google.firebase.database.database
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.SecureRandom


class ChatViewModel(
    application: Application,
    private val repo: ChatRepository

) : AndroidViewModel(application) {

    private val chatAesKeysMap = mutableMapOf<String, ByteArray>()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _isFriendTyping = MutableStateFlow(false)
    val isFriendTyping: StateFlow<Boolean> = _isFriendTyping

    private val _groupMembers = MutableStateFlow<List<String>>(emptyList())
    val groupMembers: StateFlow<List<String>> = _groupMembers

    private val _groupChats = MutableStateFlow<List<GroupChat>>(emptyList())
    val groupChats: StateFlow<List<GroupChat>> = _groupChats

    private val _combinedChatList = MutableStateFlow<List<ChatItem>>(emptyList())
    val combinedChatList: StateFlow<List<ChatItem>> = _combinedChatList

    // internal mutable state for messages when observing a chat
    private val messageBuffer = mutableListOf<Message>()

    // --------------------------
    // utility: network check
    private fun isOnline(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val nw = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(nw) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    // --------------------------
    private fun scheduleMessageAutoDelete(chatId: String, messageId: String, timestamp: Long) {
        viewModelScope.launch {
            val twentyFourHours = 24 * 60 * 60 * 1000L
            val deleteTime = timestamp + twentyFourHours
            val delay = deleteTime - System.currentTimeMillis()

            if (delay > 0) {
                delay(delay)
                // Auto-delete after 24 hours
                deleteExpiredMessage(chatId, messageId)
            } else if (delay <= 0) {
                // Message is already expired, delete immediately
                deleteExpiredMessage(chatId, messageId)
            }
        }
    }

    private suspend fun deleteExpiredMessage(chatId: String, messageId: String) {
        withContext(Dispatchers.IO) {
            try {
                val db = Firebase.database.reference
                val messageRef = db.child("chats")
                    .child(chatId)
                    .child("messages")
                    .child(messageId)

                messageRef.removeValue().await()

                // Remove from local buffer
                messageBuffer.removeAll { it.id == messageId }
                val sorted = messageBuffer.sortedByDescending { it.timestamp }
                withContext(Dispatchers.Main) {
                    _messages.value = sorted
                }

                Log.d("ChatViewModel", "Auto-deleted expired message: $messageId")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to auto-delete expired message", e)
            }
        }
    }

    // --------------------------
    // refreshCombinedChatList (delegates to repo/local fetching)
    fun refreshCombinedChatList(
        currentUserId: String,
        friendList: List<Pair<Friend, Map<String, String>>>,
        searchQuery: String,
        context: Context,
        groupChats: List<GroupChat>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val online = isOnline(context)

            // friends
            val friendItems = if (online && friendList.isNotEmpty()) {
                friendList.map { (friend, details) ->
                    val chatId = if (currentUserId < friend.friendId)
                        "${currentUserId}_${friend.friendId}" else "${friend.friendId}_${currentUserId}"
                    val timestamp = repo.fetchLastMessageTimestamp(chatId)

                    repo.saveUserToLocal(
                        UserEntity(
                            userId = friend.friendId,
                            username = details["username"] ?: "",
                            email = "",
                            bio = "",
                            profileImageUri = details["profileImageUri"] ?: ""
                        )
                    )

                    repo.saveFriendEntities(
                        listOf(
                            FriendEntity(
                                friendId = friend.friendId,
                                username = details["username"] ?: "",
                                profileImageUri = details["profileImageUri"] ?: "",
                                timestamp = timestamp,
                                userId = currentUserId
                            )
                        )
                    )

                    ChatItem.FriendItem(friend, details, timestamp)
                }
            } else {
                repo.loadFriendsFromLocal(currentUserId).map {
                    val details = mapOf("username" to it.username, "profileImageUri" to it.profileImageUri)
                    ChatItem.FriendItem(Friend(it.friendId), details, it.timestamp)
                }
            }.filter {
                it.details["username"]?.contains(searchQuery, ignoreCase = true) ?: false
            }

            // groups
            val groupItems = if (online && groupChats.isNotEmpty()) {
                groupChats.map {
                    val timestamp = repo.fetchLastMessageTimestamp(it.groupId)
                    repo.saveGroupEntities(
                        GroupEntity(
                            groupId = it.groupId,
                            userId = currentUserId,
                            groupName = it.groupName,
                            groupImageUri = it.groupImage,
                            memberIds = it.members.joinToString(",")
                        )
                    )
                    ChatItem.GroupItem(it, timestamp)
                }
            } else {
                repo.loadGroupsForUser(currentUserId)
                    .first()
                    .map {
                        val timestamp = repo.fetchLastMessageTimestamp(it.groupId)
                        ChatItem.GroupItem(
                            GroupChat(
                                groupId = it.groupId,
                                groupName = it.groupName,
                                groupImage = it.groupImageUri ?: "",
                                members = it.memberIds.split(",")
                            ),
                            timestamp
                        )
                    }
            }.filter {
                it.group.groupName.contains(searchQuery, ignoreCase = true)
            }

            _combinedChatList.value = (friendItems + groupItems).sortedByDescending {
                when (it) {
                    is ChatItem.FriendItem -> it.timestamp
                    is ChatItem.GroupItem -> it.timestamp
                }
            }
        }
    }

//    fun fetchAndStoreChatAesKey(chatId: String, currentUserId: String, onReady: (ByteArray?) -> Unit) {
//        val chatRef = Firebase.database.reference.child("chats").child(chatId)
//        chatRef.get().addOnSuccessListener { snapshot ->
//            val encryptedForMe = snapshot.child("encryptedAesKeyForReceiver").getValue(String::class.java)
//                ?: snapshot.child("encryptedAesKeyForSender").getValue(String::class.java)
//
//            if (encryptedForMe != null) {
//                try {
//                    val aesKey = CryptoUtil.decryptAesKeyWithRsa(currentUserId, encryptedForMe)
//                    chatAesKeysMap[chatId] = aesKey
//                    onReady(aesKey)
//                } catch (e: Exception) {
//                    Log.e("Chat", "Failed to decrypt AES key", e)
//                    onReady(null)
//                }
//            } else {
//                onReady(null)
//            }
//        }.addOnFailureListener {
//            onReady(null)
//        }
//    }

    // --------------------------
    // updateMessages (decryption)
    fun updateMessages(chatId: String, newList: List<Message>) {
        viewModelScope.launch {
            _messages.value = newList
        }
    }

    // --------------------------
    // ⭐ UPDATED: observeMessages - now checks for expired messages and schedules auto-deletion
    fun observeMessages(
        context: Context,
        chatId: String,
        currentUserId: String,
        isChatOpen: Boolean,
        requestNotificationPermission: () -> Unit
    ) {
        messageBuffer.clear()
        repo.observeMessages(chatId, onNewMessage = { snapshot, message, hasLoadedInitial ->
            // message deleted for current user?
            if (message.deletedFor?.containsKey(currentUserId) == true) {
                messageBuffer.removeAll { it.id == message.id }
                val sorted = messageBuffer.sortedByDescending { it.timestamp }
                updateMessages(chatId, sorted)
                return@observeMessages
            }

            // ⭐ NEW: Check if message is older than 24 hours
            val currentTime = System.currentTimeMillis()
            val twentyFourHours = 24 * 60 * 60 * 1000L
            val messageAge = currentTime - message.timestamp

            if (messageAge > twentyFourHours) {
                // Message is expired, delete it immediately
                viewModelScope.launch {
                    deleteExpiredMessage(chatId, message.id)
                }
                return@observeMessages
            } else {
                // ⭐ NEW: Schedule auto-deletion for this message
                scheduleMessageAutoDelete(chatId, message.id, message.timestamp)
            }

            // mark seen if appropriate
            if (message.receiverId == currentUserId && !message.seen && isChatOpen) {
                try {
                    snapshot.ref.child("seen").setValue(true)
                } catch (e: Exception) {
                    Log.e("ChatVM", "Failed to mark seen: ${e.message}")
                }
            }

            // notification logic: only trigger when after initial load
            if (hasLoadedInitial && message.senderId != currentUserId && message.receiverId == currentUserId) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    val perm = android.content.pm.PackageManager.PERMISSION_GRANTED
                    val status = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    if (status != perm) {
                        requestNotificationPermission()
                    } else {
                        NotificationHelper.showNotification(context, "New message from ${message.senderName}", message.text)
                    }
                } else {
                    NotificationHelper.showNotification(context, "New message from ${message.senderName}", message.text)
                }
            }

            // keep buffer and update state flow (sorted by timestamp)
            messageBuffer.add(0, message)
            val sorted = messageBuffer.sortedByDescending { it.timestamp }
            updateMessages(chatId, sorted)
        }, onCancelled = { error ->
            Log.e("ChatVM", "observeMessages cancelled: ${error.message}")
        })
    }

    fun stopObservingMessages(chatId: String) {
        repo.removeMessageListener(chatId)
    }

    // --------------------------
    // observeTyping
    fun observeTyping(chatId: String, receiverId: String) {
        repo.observeTyping(chatId, receiverId, onTypingChanged = { typing ->
            _isFriendTyping.value = typing
        }, onCancelled = { error ->
            Log.e("ChatVM", "Typing listener failed: ${error.message}")
        })
    }

    fun stopObservingTyping(chatId: String, receiverId: String) {
        repo.removeTypingListener(chatId, receiverId)
    }

    // --------------------------
    // fetch current username (callback style to stay compatible)
    fun fetchCurrentUserName(userId: String, onResult: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val name = repo.fetchUsername(userId)
            withContext(Dispatchers.Main) {
                onResult(name)
            }
        }
    }

    // fetch group members
    fun fetchGroupMembers(groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val names = repo.fetchGroupMembers(groupId)
            _groupMembers.value = names
        }
    }

    // --------------------------
    // send message and typing
    fun sendMessage(chatId: String, message: Message) {
        repo.sendMessage(chatId, message)
        // ⭐ NEW: Schedule auto-deletion for new message
        scheduleMessageAutoDelete(chatId, message.id, message.timestamp)
    }

    fun setTypingStatus(chatId: String, currentUserId: String, isTyping: Boolean) {
        repo.setTypingStatus(chatId, currentUserId, isTyping)
    }

    // --------------------------
    // ⭐ EXISTING: Delete message for current user only
    fun deleteMessageForSelf(chatId: String, messageId: String, currentUserId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = Firebase.database.reference
                val messageRef = db.child("chats")
                    .child(chatId)
                    .child("messages")
                    .child(messageId)

                messageRef.child("deletedFor").child(currentUserId).setValue(true)
                    .await()

                Log.d("ChatViewModel", "Message deleted for self: $messageId")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to delete message for self", e)
            }
        }
    }

    // --------------------------
    // ⭐ EXISTING: Delete message for everyone (only sender, within 24 hours)
    fun deleteMessageForEveryone(
        chatId: String,
        messageId: String,
        currentUserId: String,
        message: Message,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (message.senderId != currentUserId) {
                    withContext(Dispatchers.Main) {
                        onError("Only sender can delete for everyone")
                    }
                    return@launch
                }

                val currentTime = System.currentTimeMillis()
                val timeSinceSent = currentTime - message.timestamp
                val twentyFourHours = 24 * 60 * 60 * 1000

                if (timeSinceSent > twentyFourHours) {
                    withContext(Dispatchers.Main) {
                        onError("Can only delete within 24 hours")
                    }
                    return@launch
                }

                val db = Firebase.database.reference
                val messageRef = db.child("chats")
                    .child(chatId)
                    .child("messages")
                    .child(messageId)

                messageRef.removeValue().await()

                messageBuffer.removeAll { it.id == messageId }
                val sorted = messageBuffer.sortedByDescending { it.timestamp }
                withContext(Dispatchers.Main) {
                    _messages.value = sorted
                    onSuccess()
                }

                Log.d("ChatViewModel", "Message deleted for everyone: $messageId")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to delete message for everyone", e)
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Delete failed")
                }
            }
        }
    }

    // --------------------------
    // remove friend (both firebase and local)
    fun removeFriendFromDatabase(currentUserId: String, friendId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.removeFriend(currentUserId, friendId)
        }
    }

    // --------------------------
    // load group chats (from repo/local)
    fun loadGroupChats(userId: String) {
        viewModelScope.launch {
            repo.loadGroupsForUser(userId).collect { groups ->
                val groupChats = groups.map {
                    GroupChat(
                        groupId = it.groupId,
                        groupName = it.groupName,
                        groupImage = it.groupImageUri ?: "",
                        members = it.memberIds.split(",")
                    )
                }
                _groupChats.value = groupChats
            }
        }
    }

    fun fetchUserProfile(
        context: Context,
        userId: String,
        onResult: (String?, String?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val username = repo.fetchUsername(userId)
                val profilePicUrl = repo.fetchUserProfilePic(userId)
                withContext(Dispatchers.Main) {
                    onResult(username, profilePicUrl)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(null, null)
                    Toast.makeText(context, "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun leaveGroup(currentUserId: String, groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.leaveGroup(currentUserId, groupId)
            _groupChats.value = _groupChats.value.filterNot { it.groupId == groupId }
        }
    }

    fun removeGroupChat(groupId: String) {
        _groupChats.value = _groupChats.value.filterNot { it.groupId == groupId }
    }

    fun loadFriendsWithDetails(
        userId: String,
        onFriendsLoaded: (List<Pair<Friend, Map<String, String>>>) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val friends = repo.loadFriendsWithDetails(userId)
                withContext(Dispatchers.Main) {
                    onFriendsLoaded(friends)
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to load friends: ${e.message}")
                withContext(Dispatchers.Main) {
                    onFriendsLoaded(emptyList())
                }
            }
        }
    }

    fun createChat(senderId: String, receiverId: String, onComplete: (String) -> Unit) {
        val chatId = Firebase.database.reference.push().key ?: return

        Firebase.database.reference.child("users").child(receiverId).child("publicKey").get()
            .addOnSuccessListener { snapshot ->
                val publicKeyBase64 = snapshot.getValue(String::class.java)
                if (publicKeyBase64 != null) {
                    val publicKeyBytes = Base64.decode(publicKeyBase64, Base64.NO_WRAP)

                    val aesKey = ByteArray(32)
                    SecureRandom().nextBytes(aesKey)

                    val encryptedAesKey = CryptoUtil.encryptAesKeyWithRsa(publicKeyBytes, aesKey)

                    val chatData = mapOf(
                        "senderId" to senderId,
                        "receiverId" to receiverId,
                        "encryptedAesKeyForReceiver" to Base64.encodeToString(encryptedAesKey, Base64.NO_WRAP)
                    )

                    Firebase.database.reference.child("chats").child(chatId)
                        .setValue(chatData)
                        .addOnSuccessListener { onComplete(chatId) }
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        repo.removeAllListeners()
    }
}

class ChatViewModelFactory(
    private val application: Application,
    private val repo: ChatRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(application, repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    var text: String = "",
    val timestamp: Long = 0,
    val seen: Boolean = false,
    val replyTo: String? = null,
    val edited: Boolean = false,
    val deletedFor: Map<String, Boolean>? = null,
    val replyToIv: String? = null,
    var decryptedText: String? = null,
    val deletedForEveryone: Boolean = false
)