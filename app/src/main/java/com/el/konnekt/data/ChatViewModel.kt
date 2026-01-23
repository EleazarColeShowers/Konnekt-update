package com.el.konnekt.data

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.el.konnekt.data.local.AppDatabase
import com.el.konnekt.data.local.FriendEntity
import com.el.konnekt.data.local.GroupEntity
import com.el.konnekt.data.local.UserEntity
import com.el.konnekt.data.models.Friend
import com.el.konnekt.data.models.Message
import com.el.konnekt.data.repository.ChatRepository
import com.el.konnekt.data.repository.FriendRepository
import com.el.konnekt.data.repository.GroupRepository
import com.el.konnekt.data.repository.MessageRepository
import com.el.konnekt.ui.activities.mainpage.ChatItem
import com.el.konnekt.ui.activities.mainpage.GroupChat
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
import kotlin.collections.map


class ChatViewModel(
    application: Application,
    private val repo: ChatRepository,
    private val messageRepository: MessageRepository,
    private val groupRepository: GroupRepository,
    private val friendRepository: FriendRepository
) : AndroidViewModel(application) {

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

    private val _receivedRequestsCount = MutableStateFlow(0)
    val receivedRequestsCount: StateFlow<Int> = _receivedRequestsCount

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

                // Remove from local state
                _messages.value = _messages.value.filter { it.id != messageId }

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

    // updateMessages - for manual updates
    fun updateMessages(chatId: String, newList: List<Message>) {
        viewModelScope.launch {
            _messages.value = newList.sortedByDescending { it.timestamp }
        }
    }

    // observeMessages - listen to real-time updates from repository
    fun observeMessages(chatId: String, currentUserId: String) {
        viewModelScope.launch {
            messageRepository.observeMessages(chatId, currentUserId).collect { messageList ->
                _messages.value = messageList.sortedByDescending { it.timestamp }
            }
        }
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

    fun fetchCurrentUserName(userId: String, onResult: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val name = repo.fetchUsername(userId)
            withContext(Dispatchers.Main) {
                onResult(name)
            }
        }
    }

    fun removeFriendFromDatabase(currentUserId: String, friendId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = friendRepository.removeFriend(currentUserId, friendId)
            if (result.isSuccess) {
                Log.d("ChatViewModel", "Friend removed successfully")
            } else {
                Log.e("ChatViewModel", "Failed to remove friend: ${result.exceptionOrNull()?.message}")
            }
        }
    }

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

    fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        receiverId: String,
        text: String,
        replyToId: String? = null
    ) {
        viewModelScope.launch {
            messageRepository.sendMessage(chatId, senderId, senderName, receiverId, text, replyToId)
        }
    }

    // Edit message
    fun editMessage(chatId: String, messageId: String, newText: String) {
        viewModelScope.launch {
            messageRepository.editMessage(chatId, messageId, newText)
        }
    }

    // Delete message for self
    fun deleteMessageForSelf(chatId: String, messageId: String, userId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessageForSelf(chatId, messageId, userId)
        }
    }

    // Delete message for everyone
    fun deleteMessageForEveryone(chatId: String, messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessageForEveryone(chatId, messageId)
        }
    }

    // Mark messages as seen
    fun markMessagesAsSeen(chatId: String, currentUserId: String, isGroupChat: Boolean) {
        viewModelScope.launch {
            messageRepository.markMessagesAsSeen(chatId, currentUserId, isGroupChat)
        }
    }

    fun observeReceivedRequestsCount(userId: String) {
        viewModelScope.launch {
            friendRepository.observeReceivedRequestsCount(userId).collect { count ->
                _receivedRequestsCount.value = count
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
                val friends = friendRepository.loadFriendsWithDetails(userId)
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

    fun fetchGroupMembers(currentUserId: String, groupId: String) {
        viewModelScope.launch {
            val groups = groupRepository.fetchGroupChats(currentUserId)
            val group = groups.find { it.groupId == groupId }
            _groupMembers.value = group?.members ?: emptyList()
        }
    }

    fun updateGroupChats(groups: List<GroupChat>) {
        _groupChats.value = groups
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
            val messageRepository = MessageRepository()
            val friendRepository = FriendRepository()
            val groupRepository = GroupRepository(AppDatabase.getDatabase(application))

            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(
                application, repo,
                messageRepository,
                groupRepository,
                friendRepository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}