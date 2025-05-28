package com.example.instachatcompose.ui.activities.data

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.instachatcompose.ui.activities.mainpage.ChatItem
import com.example.instachatcompose.ui.activities.mainpage.Friend
import com.example.instachatcompose.ui.activities.mainpage.GroupChat
import com.example.instachatcompose.ui.activities.mainpage.fetchGroupChats
import com.example.instachatcompose.ui.activities.mainpage.fetchLastMessageTimestamp
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.google.firebase.Firebase
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(application: Application) : AndroidViewModel(application)  {
    private val db = Firebase.database.reference
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages
    private val _isFriendTyping = MutableStateFlow(false)
    val isFriendTyping: StateFlow<Boolean> = _isFriendTyping
    private var chatListener: ValueEventListener? = null
    private var typingListener: ValueEventListener? = null
    private val _currentUserName = MutableStateFlow<String?>(null)
    val currentUserName: StateFlow<String?> = _currentUserName
    private val _groupMembers = MutableStateFlow<List<String>>(emptyList())
    val groupMembers: StateFlow<List<String>> = _groupMembers
    private val _groupChats = MutableStateFlow<List<GroupChat>>(emptyList())
    val groupChats: StateFlow<List<GroupChat>> = _groupChats
    private val _combinedChatList = MutableStateFlow<List<ChatItem>>(emptyList())
    val combinedChatList: StateFlow<List<ChatItem>> = _combinedChatList
    private val appDatabase = AppDatabase.getDatabase(application)
    private val groupDao = appDatabase.groupDao()

    fun refreshCombinedChatList(
        currentUserId: String,
        friendList: List<Pair<Friend, Map<String, String>>>,
        searchQuery: String,
        context: Context,
        groupChats: List<GroupChat>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val userDao = db.userDao()
            val friendDao = db.friendDao()
            val groupDao = db.groupDao()

            val isOnline = try {
                val socket = java.net.Socket()
                socket.connect(java.net.InetSocketAddress("8.8.8.8", 53), 1500)
                socket.close()
                true
            } catch (e: Exception) {
                false
            }

            val friendItems = if (isOnline && friendList.isNotEmpty()) {
                friendList.map { (friend, details) ->
                    val chatId = if (currentUserId < friend.friendId) {
                        "${currentUserId}_${friend.friendId}"
                    } else {
                        "${friend.friendId}_${currentUserId}"
                    }
                    val timestamp = fetchLastMessageTimestamp(chatId)

                    val friendAsUser = UserEntity(
                        userId = friend.friendId,
                        username = details["username"] ?: "",
                        email = "",
                        bio = "",
                        profileImageUri = details["profileImageUri"] ?: ""
                    )

                    if (userDao.getUserById(friend.friendId) == null) {
                        userDao.insertUser(friendAsUser)
                    }

                    friendDao.insertFriends(
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
                friendDao.getFriendsForUser(currentUserId).map {
                    val details = mapOf(
                        "username" to it.username,
                        "profileImageUri" to it.profileImageUri
                    )
                    ChatItem.FriendItem(Friend(it.friendId), details, it.timestamp)
                }
            }.filter {
                it.details["username"]?.contains(searchQuery, ignoreCase = true) ?: false
            }

            val groupItems = if (isOnline && groupChats.isNotEmpty()) {
                groupChats.map {
                    val timestamp = fetchLastMessageTimestamp(it.groupId)

                    groupDao.insertGroup(
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
                groupDao.getGroupsForUser(currentUserId).first().map {
                    val timestamp = fetchLastMessageTimestamp(it.groupId)
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


    fun loadGroupChats(currentUserId: String) {
        viewModelScope.launch {
            val groups = fetchGroupChats(currentUserId)
            _groupChats.value = groups.asReversed()
        }
    }

    fun fetchCurrentUserName(userId: String, onResult: (String?) -> Unit) {
        val userRef = db.child("users").child(userId).child("username")
        userRef.get().addOnSuccessListener { snapshot ->
            val name = snapshot.getValue(String::class.java)
            onResult(name)
        }.addOnFailureListener { error ->
            Log.e("ChatVM", "Failed to fetch username: ${error.message}")
            onResult(null)
        }
    }

    fun fetchGroupMembers(groupId: String) {
        val fullGroupId = "group_$groupId"
        val membersRef = db.child("chats").child(fullGroupId).child("members")
        membersRef.get().addOnSuccessListener { snapshot ->
            val memberNames = snapshot.children.mapNotNull {
                it.child("name").getValue(String::class.java)
            }
            _groupMembers.value = memberNames
        }.addOnFailureListener { error ->
            Log.e("ChatVM", "Failed to fetch group members: ${error.message}")
        }
    }

    fun observeMessages(
        context: Context,
        chatId: String,
        currentUserId: String,
        isChatOpen: Boolean,
        requestNotificationPermission: () -> Unit
    ) {
        val messagesRef = db.child("chats").child(chatId).child("messages")
        chatListener?.let { messagesRef.removeEventListener(it) }

        val messageList = mutableListOf<Message>()
        var hasLoadedInitialMessages = false

        chatListener = object : ChildEventListener, ValueEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)

                if (message != null && message.deletedFor?.containsKey(currentUserId) != true) {
                    if (message.receiverId == currentUserId && !message.seen && isChatOpen) {
                        snapshot.ref.child("seen").setValue(true)
                    }

                    if (hasLoadedInitialMessages &&
                        message.senderId != currentUserId &&
                        message.receiverId == currentUserId
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                requestNotificationPermission()
                            } else {
                                NotificationHelper.showNotification(
                                    context,
                                    title = "New message from ${message.senderName}",
                                    message = message.text
                                )
                            }
                        } else {
                            NotificationHelper.showNotification(
                                context,
                                title = "New message from ${message.senderName}",
                                message = message.text
                            )
                        }
                    }
                    messageList.add(0, message)
                    _messages.value = messageList.sortedByDescending { it.timestamp }
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onDataChange(snapshot: DataSnapshot) {
                hasLoadedInitialMessages = true
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatVM", "ChildEventListener cancelled: ${error.message}")
            }
        }
        messagesRef.addChildEventListener(chatListener as ChildEventListener)
        messagesRef.addListenerForSingleValueEvent(chatListener as ValueEventListener)
    }



    fun observeTyping(chatId: String, receiverId: String) {
        val typingRef = db.child("chats").child(chatId).child("typing").child(receiverId)
        typingListener?.let { typingRef.removeEventListener(it) }
        typingListener = typingRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.value
                _isFriendTyping.value = when (value) {
                    is Boolean -> value
                    is Map<*, *> -> value["isTyping"] as? Boolean ?: false
                    else -> false
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatVM", "Typing listener failed: ${error.message}")
            }
        })
    }

    fun sendMessage(chatId: String, message: Message) {
        val messagesRef = db.child("chats").child(chatId).child("messages")
        val newMessageRef = messagesRef.push()
        newMessageRef.setValue(message)
    }

    fun setTypingStatus(chatId: String, currentUserId: String, isTyping: Boolean) {
        val typingRef = db.child("chats").child(chatId).child("typing").child(currentUserId)
        typingRef.setValue(isTyping)
    }

    override fun onCleared() {
        super.onCleared()
        chatListener?.let {
            db.removeEventListener(it)
        }
        typingListener?.let {
            db.removeEventListener(it)
        }
    }
    
    fun removeFriendFromDatabase(currentUserId: String, friendId: String,friendDao: FriendDao) {
        val db = Firebase.database.reference
        db.child("users").child(currentUserId).child("friends")
            .orderByChild("friendId").equalTo(friendId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        child.ref.removeValue()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error removing friend: ${error.message}")
                }
            })

        db.child("users").child(friendId).child("friends")
            .orderByChild("friendId").equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        child.ref.removeValue()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error removing friend: ${error.message}")
                }
            })
        CoroutineScope(Dispatchers.IO).launch {
            friendDao.deleteFriend(friendId, currentUserId)
        }

    }
    fun loadFriendsWithDetails(userId: String, onFriendsLoaded: (List<Pair<Friend, Map<String, String>>>) -> Unit) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        val friendsRef = usersRef.child(userId).child("friends")

        friendsRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val detailTasks = snapshot.children.map { friendSnapshot ->
                    val friendId = friendSnapshot.child("friendId").getValue(String::class.java) ?: ""
                    val timestamp = friendSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                    val friend = Friend(friendId, timestamp)

                    usersRef.child(friendId).get().continueWith { task ->
                        val userSnapshot = task.result
                        val details = if (userSnapshot.exists()) {
                            mapOf(
                                "username" to (userSnapshot.child("username").getValue(String::class.java) ?: "Unknown"),
                                "profileImageUri" to (userSnapshot.child("profileImageUri").getValue(String::class.java) ?: "")
                            )
                        } else {
                            mapOf("username" to "Unknown", "profileImageUri" to "")
                        }
                        Pair(friend, details)
                    }
                }

                Tasks.whenAllSuccess<Pair<Friend, Map<String, String>>>(detailTasks)
                    .addOnSuccessListener { friendDetailsList ->
                        onFriendsLoaded(friendDetailsList)
                    }
            } else {
                onFriendsLoaded(emptyList())
            }
        }.addOnFailureListener { exception ->
            Log.e("Firebase", "Error loading friends: ${exception.message}")
            onFriendsLoaded(emptyList())
        }
    }

    fun fetchUserProfile(context: Context, userId: String, onResult: (String?, String?) -> Unit) {
        val database = Firebase.database.reference
        val userRef = database.child("users").child(userId)
        val db = AppDatabase.getDatabase(context)
        val userDao = db.userDao()

        CoroutineScope(Dispatchers.IO).launch {
            val localUser = userDao.getUserById(userId)

            if (localUser != null) {
                withContext(Dispatchers.Main) {
                    onResult(localUser.username, localUser.profileImageUri)
                }
            }

            userRef.get()
                .addOnSuccessListener { dataSnapshot ->
                    val username = dataSnapshot.child("username").getValue(String::class.java)
                    val imageUrl = dataSnapshot.child("profileImageUri").getValue(String::class.java)

                    if (username != null || imageUrl != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            userDao.insertUser(
                                UserEntity(
                                    userId = userId,
                                    username = username ?: "",
                                    email = "",  // Not fetched here
                                    bio = "",    // Not fetched here
                                    profileImageUri = imageUrl ?: ""
                                )
                            )
                            withContext(Dispatchers.Main) {
                                onResult(username, imageUrl)
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    if (localUser == null) {
                        onResult(null, null)
                    }
                }
        }
    }
    fun leaveGroup(currentUserId: String, groupId: String) {
        db.child("chats").child("group_$groupId").child("members").child(currentUserId).removeValue()
        viewModelScope.launch(Dispatchers.IO) {
            groupDao.deleteGroup(groupId)
            Log.d("RoomDB", "Group $groupId removed from local database.")
        }
    }

    fun removeGroupChat(groupId: String) {
        _groupChats.value = _groupChats.value.filterNot { it.groupId == groupId }
    }

}

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Long = 0,
    val seen: Boolean = false,
    val replyTo: String? = null,
    val edited: Boolean = false,
    val deletedFor: Map<String, Boolean>? = null
)