package com.el.konnekt.data

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
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
import com.el.konnekt.data.models.ChatState
import com.el.konnekt.data.models.Friend
import com.el.konnekt.data.models.GroupChat
import com.el.konnekt.data.models.Message
import com.el.konnekt.data.repository.ChatRepository
import com.el.konnekt.data.repository.FriendRepository
import com.el.konnekt.data.repository.GroupRepository
import com.el.konnekt.data.repository.MessageRepository
import com.el.konnekt.ui.activities.KonnektApp.Companion.database
import com.el.konnekt.ui.activities.mainpage.ChatItem
import com.el.konnekt.utils.MessageObfuscator
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.collections.find
import kotlin.collections.map
import java.util.UUID



class ChatViewModel(
    application: Application,
    private val repo: ChatRepository,
    private val messageRepository: MessageRepository,
    private val groupRepository: GroupRepository,
    private val friendRepository: FriendRepository,
    private val database: AppDatabase

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

    private val _cachedFriends = MutableStateFlow<List<Pair<Friend, Map<String, String>>>>(emptyList())
    val cachedFriends: StateFlow<List<Pair<Friend, Map<String, String>>>> = _cachedFriends.asStateFlow()

    private var lastFriendsFetchTime = 0L
    private var lastGroupsFetchTime = 0L
    private val CACHE_VALIDITY = 5 * 60 * 1000

    private val _isLoadingFriends = MutableStateFlow(false)
    private val currentUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val _chatTimestamps = MutableStateFlow<Map<String, Long>>(emptyMap())

    val chatTimestamps: StateFlow<Map<String, Long>> = _chatTimestamps.asStateFlow()
    private val _chatStates = mutableMapOf<String, MutableStateFlow<ChatState>>()
    private val chatListeners = mutableMapOf<String, ValueEventListener>()

    fun getChatState(chatId: String): StateFlow<ChatState> {
        return _chatStates.getOrPut(chatId) {
            MutableStateFlow(ChatState()).also {
                startListeningToChat(chatId)
            }
        }
    }

    private fun startListeningToChat(chatId: String) {
        if (chatListeners.containsKey(chatId)) return

        val db = FirebaseDatabase.getInstance().reference
            .child("chats")
            .child(chatId)
            .child("messages")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val messages = snapshot.children.mapNotNull {
                            it.getValue(Message::class.java)
                        }

                        if (messages.isNotEmpty()) {
                            val lastMsg = messages.last()
                            val deobfuscatedText = MessageObfuscator.deobfuscate(
                                lastMsg.text,
                                chatId
                            )

                            val unreadMessages = messages.count {
                                it.receiverId == currentUserId && !it.seen
                            }

                            _chatStates[chatId]?.value = ChatState(
                                lastMessage = deobfuscatedText,
                                timestamp = lastMsg.timestamp,
                                unreadCount = unreadMessages,
                                hasUnreadMessages = unreadMessages > 0
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Error processing chat $chatId", e)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatViewModel", "Chat listener cancelled: ${error.message}")
            }
        }

        chatListeners[chatId] = listener
        db.addValueEventListener(listener)
    }

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

    fun refreshCombinedChatList(
        currentUserId: String,
        friendList: List<Pair<Friend, Map<String, String>>>,
        searchQuery: String,
        context: Context,
        groupChats: List<GroupChat>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val online = isOnline(context)

            val friendItems = if (online && friendList.isNotEmpty()) {
                friendList.map { (friend, details) ->
                    val chatId = if (currentUserId < friend.friendId)
                        "${currentUserId}_${friend.friendId}" else "${friend.friendId}_${currentUserId}"
//                    val timestamp = repo.fetchLastMessageTimestamp(chatId)
                    val timestamp = _chatTimestamps.value[chatId] ?: repo.fetchLastMessageTimestamp(chatId)

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
                    val timestamp = _chatTimestamps.value[it.groupId] ?: repo.fetchLastMessageTimestamp(it.groupId)
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

    fun updateMessages(chatId: String, newList: List<Message>) {
        viewModelScope.launch {
            _messages.value = newList.sortedByDescending { it.timestamp }
        }
    }

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

    fun editMessage(chatId: String, messageId: String, newText: String) {
        viewModelScope.launch {
            messageRepository.editMessage(chatId, messageId, newText)
        }
    }

    fun deleteMessageForSelf(chatId: String, messageId: String, userId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessageForSelf(chatId, messageId, userId)
        }
    }

    fun deleteMessageForEveryone(chatId: String, messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessageForEveryone(chatId, messageId)
        }
    }

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
        forceRefresh: Boolean = false,
        onFriendsLoaded: (List<Pair<Friend, Map<String, String>>>) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentTime = System.currentTimeMillis()

                // Return cached data if valid and not forcing refresh
                if (!forceRefresh &&
                    _cachedFriends.value.isNotEmpty() &&
                    (currentTime - lastFriendsFetchTime) < CACHE_VALIDITY) {
                    Log.d("ChatViewModel", "Using cached friends data")
                    withContext(Dispatchers.Main) {
                        onFriendsLoaded(_cachedFriends.value)
                    }
                    return@launch
                }

                // Fetch fresh data
                _isLoadingFriends.value = true
                Log.d("ChatViewModel", "Fetching fresh friends data")

                val friends = friendRepository.loadFriendsWithDetails(userId)

                // Update cache
                _cachedFriends.value = friends
                lastFriendsFetchTime = currentTime

                withContext(Dispatchers.Main) {
                    _isLoadingFriends.value = false
                    onFriendsLoaded(friends)
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to load friends: ${e.message}")
                withContext(Dispatchers.Main) {
                    _isLoadingFriends.value = false
                    // Return cached data if available, even if expired
                    if (_cachedFriends.value.isNotEmpty()) {
                        onFriendsLoaded(_cachedFriends.value)
                    } else {
                        onFriendsLoaded(emptyList())
                    }
                }
            }
        }
    }

    // MODIFIED: Add caching logic for groups
    fun loadGroupChats(userId: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                val currentTime = System.currentTimeMillis()

                // Return cached data if valid and not forcing refresh
                if (!forceRefresh &&
                    _groupChats.value.isNotEmpty() &&
                    (currentTime - lastGroupsFetchTime) < CACHE_VALIDITY) {
                    Log.d("ChatViewModel", "Using cached group chats data")
                    return@launch
                }

                Log.d("ChatViewModel", "Fetching fresh group chats data")

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
                    lastGroupsFetchTime = currentTime
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to load group chats: ${e.message}")
            }
        }
    }

    // ADD: Method to manually refresh data
    fun refreshAllData(userId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val friendsDeferred = async(Dispatchers.IO) {
                loadFriendsWithDetails(userId, forceRefresh = true) { }
            }
            val groupsDeferred = async {
                loadGroupChats(userId, forceRefresh = true)
            }

            friendsDeferred.await()
            groupsDeferred.await()

            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    // ADD: Method to clear cache (useful for logout)
    fun clearCache() {
        _cachedFriends.value = emptyList()
        _groupChats.value = emptyList()
        lastFriendsFetchTime = 0L
        lastGroupsFetchTime = 0L
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

        chatListeners.forEach { (chatId, listener) ->
            try {
                FirebaseDatabase.getInstance().reference
                    .child("chats")
                    .child(chatId)
                    .child("messages")
                    .removeEventListener(listener)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error removing listener", e)
            }
        }
        chatListeners.clear()
        _chatStates.clear()
    }
    fun updateChatTimestamp(chatId: String, timestamp: Long) {
        val currentTimestamps = _chatTimestamps.value.toMutableMap()
        currentTimestamps[chatId] = timestamp
        _chatTimestamps.value = currentTimestamps // This creates a new map, triggering observers

        Log.d("ChatViewModel", "Updated timestamp for $chatId: $timestamp")
    }

    fun createGroup(
        groupName: String,
        selectedFriends: List<String>,
        groupImageUri: Uri?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            onError("User not authenticated")
            return
        }

        val groupId = "group_${UUID.randomUUID()}"
        val members = selectedFriends.toMutableList().apply {
            if (!contains(currentUserId)) add(currentUserId)
        }

        viewModelScope.launch {
            try {
                if (groupImageUri != null) {
                    // Upload image first
                    val storageRef = FirebaseStorage.getInstance().reference
                        .child("group_images/$groupId/profile_image.jpg")

                    storageRef.putFile(groupImageUri).await()
                    val downloadUrl = storageRef.downloadUrl.await()

                    // Create group with image
                    createGroupInFirebase(groupId, groupName, members, downloadUrl.toString(), currentUserId)
                    saveGroupToLocalDb(groupId, groupName, members, downloadUrl.toString(), currentUserId)

                } else {
                    // Create group without image
                    createGroupInFirebase(groupId, groupName, members, "", currentUserId)
                    saveGroupToLocalDb(groupId, groupName, members, "", currentUserId)
                }

                loadGroupChats(currentUserId)
                onSuccess()

            } catch (e: Exception) {
                onError("Failed to create group: ${e.message}")
            }
        }
    }

    private suspend fun createGroupInFirebase(
        groupId: String,
        groupName: String,
        members: List<String>,
        groupImageUrl: String,
        adminId: String
    ) {
        val groupData = mapOf(
            "groupName" to groupName,
            "members" to members.associateWith { true },
            "groupImage" to groupImageUrl,
            "adminId" to adminId
        )

        FirebaseDatabase.getInstance().getReference("chats")
            .child(groupId)
            .setValue(groupData)
            .await()
    }

    private suspend fun saveGroupToLocalDb(
        groupId: String,
        groupName: String,
        members: List<String>,
        groupImageUrl: String,
        userId: String
    ) = withContext(Dispatchers.IO) {
        val groupEntity = GroupEntity(
            groupId = groupId,
            userId = userId,
            groupName = groupName,
            groupImageUri = groupImageUrl,
            memberIds = members.joinToString(",")
        )
        database.groupDao().insertGroup(groupEntity)
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
                database
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}