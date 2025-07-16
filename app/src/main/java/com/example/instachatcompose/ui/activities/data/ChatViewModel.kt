package com.example.instachatcompose.ui.activities.data

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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
    private val _groupChats = MutableStateFlow<List<GroupChat>>(emptyList())
    val groupChats: StateFlow<List<GroupChat>> = _groupChats
    private val _combinedChatList = MutableStateFlow<List<ChatItem>>(emptyList())
    val combinedChatList: StateFlow<List<ChatItem>> = _combinedChatList
    private val appDatabase = AppDatabase.getDatabase(application)
    private val groupDao = appDatabase.groupDao()
    private val _archivedFriends = MutableStateFlow<List<Friend>>(emptyList())
    val archivedFriends: StateFlow<List<Friend>> = _archivedFriends
    private val _archivedGroups = MutableStateFlow<List<GroupChat>>(emptyList())
    val archivedGroups: StateFlow<List<GroupChat>> = _archivedGroups
    private val _archivedItems = MutableStateFlow<List<Any>>(emptyList()) // Unified list
    private val _isArchiveInitialized = MutableStateFlow(false)
    val isArchiveInitialized: StateFlow<Boolean> = _isArchiveInitialized
    private val _isLoadingArchive = MutableStateFlow(true)
    private var requestListener: ChildEventListener? = null
    private val _friendRequestCount = MutableStateFlow(0)
    val friendRequestCount: StateFlow<Int> = _friendRequestCount

    fun listenForFriendRequests(context: Context, userId: String) {
        createNotificationChannel(context)

        val database = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("received_requests")

        requestListener?.let { database.removeEventListener(it) }

        requestListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                handleRequestChange(context, snapshot)
                updateRequestCount(snapshot.ref.parent!!)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                handleRequestChange(context, snapshot)
                updateRequestCount(snapshot.ref.parent!!)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                updateRequestCount(snapshot.ref.parent!!)
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }

        database.addChildEventListener(requestListener as ChildEventListener)
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                updateRequestCount(database)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateRequestCount(database: DatabaseReference) {
        database.get().addOnSuccessListener { snapshot ->
            val pendingCount = snapshot.children.count {
                it.child("status").getValue(String::class.java) == "pending"
            }
            _friendRequestCount.value = pendingCount
        }
    }

    private fun handleRequestChange(context: Context, snapshot: DataSnapshot) {
        val fromId = snapshot.child("from").getValue(String::class.java)
        val status = snapshot.child("status").getValue(String::class.java)
        if (!fromId.isNullOrBlank() && status == "pending") {
            FirebaseDatabase.getInstance()
                .getReference("users").child(fromId).get()
                .addOnSuccessListener { userSnapshot ->
                    val senderName = userSnapshot.child("username").getValue(String::class.java) ?: "Someone"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    ) {
                        NotificationHelper.showNotification(context, "New Friend Request", "$senderName sent you a friend request.")
                    } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        NotificationHelper.showNotification(context, "New Friend Request", "$senderName sent you a friend request.")
                    }
                }
        }
    }

    fun stopListeningForFriendRequests(userId: String) {
        val database = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("received_requests")
        requestListener?.let { database.removeEventListener(it) }
    }

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
            val archivedFriendIds = _archivedFriends.value.map { it.friendId }.toSet()
            val archivedGroupIds = _archivedGroups.value.map { it.groupId }.toSet()
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
            } .filter {
                it.friend.friendId !in archivedFriendIds
            }

            val groupItems = if (isOnline && groupChats.isNotEmpty()) {
                groupChats.mapNotNull { group ->
                    if (currentUserId !in group.members) return@mapNotNull null
                    val timestamp = fetchLastMessageTimestamp(group.groupId)

                    groupDao.insertGroup(
                        GroupEntity(
                            groupId = group.groupId,
                            userId = currentUserId,
                            groupName = group.groupName,
                            groupImageUri = group.groupImage,
                            memberIds = group.members.joinToString(",")
                        )
                    )

                    ChatItem.GroupItem(group, timestamp)
                }
            } else {
                groupDao.getGroupsForUser(currentUserId).first().mapNotNull {
                    val members = it.memberIds.split(",")
                    if (currentUserId !in members) return@mapNotNull null

                    val timestamp = fetchLastMessageTimestamp(it.groupId)

                    ChatItem.GroupItem(
                        GroupChat(
                            groupId = it.groupId,
                            groupName = it.groupName,
                            groupImage = it.groupImageUri ?: "",
                            members = members
                        ),
                        timestamp
                    )
                }
            }.filter {
                it.group.groupName.contains(searchQuery, ignoreCase = true)
            }.filter {
                it.group.groupId !in archivedGroupIds
            }

            _combinedChatList.value = (friendItems + groupItems).sortedByDescending {
                when (it) {
                    is ChatItem.FriendItem -> it.timestamp
                    is ChatItem.GroupItem -> it.timestamp

                }
            }
        }
    }

    fun archiveItem(currentUserId: String, item: Any) {
        _archivedItems.value += item

        val archiveRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(currentUserId)
            .child("archive")

        when (item) {
            is Friend -> {
                val friendMap = mapOf(
                    "friendId" to item.friendId,
                    "timestamp" to item.timestamp
                )
                archiveRef.child("friends").child(item.friendId).setValue(friendMap)
            }

            is GroupChat -> {
                val groupMap = mapOf(
                    "groupId" to item.groupId,
                    "groupName" to item.groupName,
                    "groupImage" to item.groupImage
                )
                archiveRef.child("groups").child(item.groupId).setValue(groupMap)
            }
        }
    }

    fun unarchiveItem(currentUserId: String, item: Any) {
        _archivedItems.update { it - item }

        val archiveRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(currentUserId)
            .child("archive")

        when (item) {
            is Friend -> {
                archiveRef.child("friends").child(item.friendId).removeValue()
            }
            is GroupChat -> {
                archiveRef.child("groups").child(item.groupId).removeValue()
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

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "default_channel",
                "Default Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Used for default notifications"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("NotificationChannel", "Notification channel created")
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

                    val decryptedText = try {
                        MessageCrypto.decrypt(message.text, message.iv)
                    } catch (e: Exception) {
                        "[error decrypting]"
                    }

                    val decryptedMessage = message.copy(text = decryptedText)

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
                                    message = decryptedText
                                )
                            }
                        } else {
                            NotificationHelper.showNotification(
                                context,
                                title = "New message from ${message.senderName}",
                                message = decryptedText
                            )
                        }
                    }

                    messageList.add(0, decryptedMessage)
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

    fun fetchArchivedChats(currentUserId: String) {
        _isArchiveInitialized.value = false // ✅ THIS LINE is missing in your current version
        _isLoadingArchive.value = true
        var friendsDone = false
        var groupsDone = false
        fun checkIfFinished() {
            if (friendsDone && groupsDone) {
                _archivedItems.value = _archivedFriends.value + _archivedGroups.value
                _isArchiveInitialized.value = true
                _isLoadingArchive.value = false
            }
        }

        val archiveRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(currentUserId)
            .child("archive")


        archiveRef.child("friends").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val friends = snapshot.children.mapNotNull { it.getValue(Friend::class.java) }
                _archivedFriends.value = friends
                friendsDone = true
                checkIfFinished()
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        archiveRef.child("groups").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groups = snapshot.children.mapNotNull { it.getValue(GroupChat::class.java) }
                _archivedGroups.value = groups
                groupsDone = true
                checkIfFinished()
            }

            override fun onCancelled(error: DatabaseError) {}
        })

    }


    fun getFriendDetails(friendId: String, onResult: (Map<String, String>) -> Unit) {
        FirebaseDatabase.getInstance().getReference("users")
            .child(friendId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val username = snapshot.child("username").getValue(String::class.java) ?: "Unknown"
                    val profileImageUri = snapshot.child("profileImageUri").getValue(String::class.java) ?: ""
                    onResult(mapOf("username" to username, "profileImageUri" to profileImageUri))
                }

                override fun onCancelled(error: DatabaseError) {
                    onResult(mapOf("username" to "Unknown", "profileImageUri" to ""))
                }
            })
    }
}

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val text: String = "",
    val iv: String = "",
    val replyToIv: String? = null,
    val timestamp: Long = 0,
    val seen: Boolean = false,
    val replyTo: String? = null,
    val edited: Boolean = false,
    val deletedFor: Map<String, Boolean>? = null
)