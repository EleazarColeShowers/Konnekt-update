package com.el.konnekt.data

import android.content.Context
import android.util.Log
import com.el.konnekt.KonnektApplication
import com.el.konnekt.data.models.Message
import com.el.konnekt.utils.ForegroundNotificationHandler
import com.el.konnekt.utils.MessageObfuscator
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object ForegroundMessageListener {

    private var friendsListener: ValueEventListener? = null
    private val chatListeners = mutableMapOf<String, ChildEventListener>()
    private val groupListeners = mutableMapOf<String, ChildEventListener>()
    private var isInitialized = false

    fun startListening(context: Context, currentUserId: String) {
        if (isInitialized) return
        isInitialized = true

        Log.d("ForegroundListener", "Starting foreground message listener for user: $currentUserId")

        // Listen to friends list
        listenToFriends(context, currentUserId)

        // Listen to groups
        listenToGroups(context, currentUserId)
    }

    private fun listenToFriends(context: Context, currentUserId: String) {
        val friendsRef = FirebaseDatabase.getInstance().reference
            .child("users")
            .child(currentUserId)
            .child("friends")

        friendsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { friendSnapshot ->
                    val friendId = friendSnapshot.child("friendId").getValue(String::class.java)
                    if (friendId != null) {
                        val chatId = if (currentUserId < friendId) {
                            "${currentUserId}_${friendId}"
                        } else {
                            "${friendId}_${currentUserId}"
                        }

                        // Start listening to this chat if not already
                        if (!chatListeners.containsKey(chatId)) {
                            listenToChat(context, chatId, friendId, currentUserId, false)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ForegroundListener", "Error listening to friends: ${error.message}")
            }
        }

        friendsRef.addValueEventListener(friendsListener!!)
    }

    private fun listenToGroups(context: Context, currentUserId: String) {
        val groupsRef = FirebaseDatabase.getInstance().reference.child("chats")

        val groupListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { groupSnapshot ->
                    val groupId = groupSnapshot.key ?: return@forEach
                    if (!groupId.startsWith("group_")) return@forEach

                    val membersSnapshot = groupSnapshot.child("members")
                    val memberIds = membersSnapshot.children.mapNotNull { it.key }

                    if (currentUserId in memberIds) {
                        if (!groupListeners.containsKey(groupId)) {
                            val groupName = groupSnapshot.child("groupName")
                                .getValue(String::class.java) ?: "Unnamed Group"
                            val groupImage = groupSnapshot.child("groupImage")
                                .getValue(String::class.java) ?: ""

                            listenToGroupChat(context, groupId, groupName, groupImage, currentUserId)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ForegroundListener", "Error listening to groups: ${error.message}")
            }
        }

        groupsRef.addValueEventListener(groupListener)
    }

    private fun listenToChat(
        context: Context,
        chatId: String,
        friendId: String,
        currentUserId: String,
        isGroup: Boolean
    ) {
        val messagesRef = FirebaseDatabase.getInstance().reference
            .child("chats")
            .child(chatId)
            .child("messages")

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java) ?: return

                // Only show notification if:
                // 1. Message is for current user
                // 2. App is in foreground
                // 3. User is not currently in this chat
                if (message.receiverId == currentUserId &&
                    KonnektApplication.shouldShowNotification(chatId)) {

                    // Fetch friend details
                    FirebaseDatabase.getInstance().reference
                        .child("users")
                        .child(friendId)
                        .get()
                        .addOnSuccessListener { userSnapshot ->
                            val username = userSnapshot.child("username")
                                .getValue(String::class.java) ?: "Unknown"
                            val profileImage = userSnapshot.child("profileImageUri")
                                .getValue(String::class.java) ?: ""

                            val deobfuscatedText = MessageObfuscator.deobfuscate(message.text, chatId)

                            ForegroundNotificationHandler.showMessageNotification(
                                context = context,
                                senderName = username,
                                messageText = deobfuscatedText,
                                chatId = chatId,
                                senderId = friendId,
                                profileImageUri = profileImage,
                                isGroupChat = false
                            )
                        }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("ForegroundListener", "Error listening to chat $chatId: ${error.message}")
            }
        }

        chatListeners[chatId] = listener
        messagesRef.addChildEventListener(listener)
    }

    private fun listenToGroupChat(
        context: Context,
        groupId: String,
        groupName: String,
        groupImage: String,
        currentUserId: String
    ) {
        val messagesRef = FirebaseDatabase.getInstance().reference
            .child("chats")
            .child(groupId)
            .child("messages")

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java) ?: return

                // Only show notification if:
                // 1. Message is not from current user
                // 2. App is in foreground
                // 3. User is not currently in this group chat
                if (message.senderId != currentUserId &&
                    KonnektApplication.shouldShowNotification(groupId)) {

                    val senderName = message.senderName ?: "Unknown"
                    val deobfuscatedText = MessageObfuscator.deobfuscate(message.text, groupId)

                    ForegroundNotificationHandler.showMessageNotification(
                        context = context,
                        senderName = senderName,
                        messageText = deobfuscatedText,
                        chatId = groupId,
                        senderId = message.senderId,
                        profileImageUri = groupImage,
                        isGroupChat = true,
                        groupName = groupName,
                        groupImageUri = groupImage
                    )
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("ForegroundListener", "Error listening to group $groupId: ${error.message}")
            }
        }

        groupListeners[groupId] = listener
        messagesRef.addChildEventListener(listener)
    }

    fun stopListening() {
        // Remove all listeners
        chatListeners.forEach { (chatId, listener) ->
            FirebaseDatabase.getInstance().reference
                .child("chats")
                .child(chatId)
                .child("messages")
                .removeEventListener(listener)
        }
        chatListeners.clear()

        groupListeners.forEach { (groupId, listener) ->
            FirebaseDatabase.getInstance().reference
                .child("chats")
                .child(groupId)
                .child("messages")
                .removeEventListener(listener)
        }
        groupListeners.clear()

        friendsListener?.let {
            FirebaseDatabase.getInstance().reference
                .child("users")
                .removeEventListener(it)
        }

        isInitialized = false
        Log.d("ForegroundListener", "Stopped all foreground listeners")
    }
}