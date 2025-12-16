package com.el.konnekt.data.remote

import android.content.Context
import android.util.Log
import com.el.konnekt.data.Message
import com.el.konnekt.data.local.AppDatabase
import com.el.konnekt.data.local.UserEntity
import com.el.konnekt.ui.activities.mainpage.Friend
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class FirebaseDataSource(
    private val db: DatabaseReference = Firebase.database.reference
) {
    private val childListeners = mutableMapOf<String, ChildEventListener>()
    private val typingListeners = mutableMapOf<String, ValueEventListener>()

    // ----- simple fetchers -----
    suspend fun fetchUsername(userId: String): String? = withContext(Dispatchers.IO) {
        db.child("users").child(userId).child("username").get().await()
            .getValue(String::class.java)
    }

    suspend fun fetchGroupMembers(groupId: String): List<String> = withContext(Dispatchers.IO) {
        val snapshot = db.child("chats").child("group_$groupId").child("members").get().await()
        snapshot.children.mapNotNull { it.child("name").getValue(String::class.java) }
    }

    suspend fun fetchLastMessageTimestamp(chatId: String): Long = withContext(Dispatchers.IO) {
        val snapshot = db.child("chats").child(chatId).child("messages")
            .orderByChild("timestamp").limitToLast(1).get().await()
        snapshot.children.firstOrNull()?.child("timestamp")?.getValue(Long::class.java) ?: 0L
    }


    suspend fun fetchUserProfile(context: Context, userId: String): Pair<String?, String?> {
        val userRef = db.child("users").child(userId)
        val dbLocal = AppDatabase.Companion.getDatabase(context)
        val userDao = dbLocal.userDao()

        return suspendCancellableCoroutine { cont ->
            CoroutineScope(Dispatchers.IO).launch {
                val localUser = userDao.getUserById(userId)
                if (localUser != null) {
                    cont.resume(Pair(localUser.username, localUser.profileImageUri))
                }

                userRef.get()
                    .addOnSuccessListener { snapshot ->
                        val username = snapshot.child("username").getValue(String::class.java)
                        val imageUrl =
                            snapshot.child("profileImageUri").getValue(String::class.java)

                        if (username != null || imageUrl != null) {
                            CoroutineScope(Dispatchers.IO).launch {
                                userDao.insertUser(
                                    UserEntity(
                                        userId = userId,
                                        username = username ?: "",
                                        email = "",
                                        bio = "",
                                        profileImageUri = imageUrl ?: ""
                                    )
                                )
                                cont.resume(Pair(username, imageUrl))
                            }
                        } else cont.resume(Pair(null, null))
                    }
                    .addOnFailureListener { cont.resume(Pair(null, null)) }
            }
        }
    }

    suspend fun loadFriendsWithDetails(userId: String): List<Pair<Friend, Map<String, String>>> {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
        val friendsRef = usersRef.child(userId).child("friends")

        return suspendCancellableCoroutine { cont ->
            friendsRef.get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.exists()) {
                        cont.resume(emptyList())
                        return@addOnSuccessListener
                    }

                    val detailTasks = snapshot.children.map { friendSnapshot ->
                        val friendId =
                            friendSnapshot.child("friendId").getValue(String::class.java) ?: ""
                        val timestamp =
                            friendSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                        val friend = Friend(friendId, timestamp)

                        usersRef.child(friendId).get().continueWith { task ->
                            val userSnap = task.result
                            val details = if (userSnap.exists()) {
                                mapOf(
                                    "username" to (userSnap.child("username")
                                        .getValue(String::class.java) ?: "Unknown"),
                                    "profileImageUri" to (userSnap.child("profileImageUri")
                                        .getValue(String::class.java) ?: "")
                                )
                            } else {
                                mapOf("username" to "Unknown", "profileImageUri" to "")
                            }
                            Pair(friend, details)
                        }
                    }

                    Tasks.whenAllSuccess<Pair<Friend, Map<String, String>>>(detailTasks)
                        .addOnSuccessListener { result -> cont.resume(result) }
                        .addOnFailureListener { cont.resume(emptyList()) }
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseDataSource", "loadFriendsWithDetails: ${e.message}")
                    cont.resume(emptyList())
                }
        }
    }

    // ----- message listeners (pass DataSnapshot back) -----
    fun addMessageListener(
        chatId: String,
        onChildAdded: (DataSnapshot) -> Unit,
        onInitialLoad: () -> Unit,
        onCancelled: (DatabaseError) -> Unit
    ) {
        val ref = db.child("chats").child(chatId).child("messages")

        childListeners[chatId]?.let { prev -> ref.removeEventListener(prev) }

        val childListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                onChildAdded(snapshot)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                onCancelled(error)
            }
        }

        childListeners[chatId] = childListener
        ref.addChildEventListener(childListener)

        // notify when initial snapshot has loaded (so caller can treat later adds as new)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) { onInitialLoad() }
            override fun onCancelled(error: DatabaseError) { onCancelled(error) }
        })
    }

    fun removeMessageListener(chatId: String) {
        childListeners.remove(chatId)?.let { listener ->
            db.child("chats").child(chatId).child("messages").removeEventListener(listener)
        }
    }

    // ----- typing listeners -----
    fun addTypingListener(
        chatId: String,
        receiverId: String,
        onDataChange: (Any?) -> Unit,
        onCancelled: (DatabaseError) -> Unit
    ) {
        val key = "$chatId:$receiverId"
        val ref = db.child("chats").child(chatId).child("typing").child(receiverId)
        typingListeners[key]?.let { ref.removeEventListener(it) }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onDataChange(snapshot.value)
            }
            override fun onCancelled(error: DatabaseError) {
                onCancelled(error)
            }
        }
        typingListeners[key] = listener
        ref.addValueEventListener(listener)
    }

    fun removeTypingListener(chatId: String, receiverId: String) {
        val key = "$chatId:$receiverId"
        typingListeners.remove(key)?.let { listener ->
            db.child("chats").child(chatId).child("typing").child(receiverId)
                .removeEventListener(listener)
        }
    }

    // ----- send / set typing -----
    fun sendMessage(chatId: String, message: Message) {
        db.child("chats").child(chatId).child("messages").push().setValue(message)
    }

    fun setTypingStatus(chatId: String, currentUserId: String, isTyping: Boolean) {
        db.child("chats").child(chatId).child("typing").child(currentUserId).setValue(isTyping)
    }

    // ----- remove friend / leave group -----
    fun removeFriendFromFirebase(currentUserId: String, friendId: String) {
        listOf(currentUserId to friendId, friendId to currentUserId).forEach { (u1, u2) ->
            db.child("users").child(u1).child("friends")
                .orderByChild("friendId").equalTo(u2)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (child in snapshot.children) child.ref.removeValue()
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FirebaseDataSource", "Error removing friend: ${error.message}")
                    }
                })
        }
    }

    fun removeGroupMember(groupId: String, userId: String) {
        db.child("chats").child("group_$groupId").child("members").child(userId).removeValue()
    }

    fun removeAllListeners() {
        // remove any listeners we still hold references to
        childListeners.forEach { (chatId, listener) ->
            db.child("chats").child(chatId).child("messages").removeEventListener(listener)
        }
        childListeners.clear()

        typingListeners.forEach { (key, listener) ->
            val parts = key.split(":")
            if (parts.size == 2) {
                val chatId = parts[0]
                val receiverId = parts[1]
                db.child("chats").child(chatId).child("typing").child(receiverId)
                    .removeEventListener(listener)
            }
        }
        typingListeners.clear()
    }
}