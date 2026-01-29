package com.el.konnekt.data

import android.content.Context
import android.util.Log
import com.el.konnekt.KonnektApplication
import com.el.konnekt.utils.ForegroundNotificationHandler
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase

object ForegroundFriendRequestListener {

    private var listener: ChildEventListener? = null
    private var isInitialized = false

    fun startListening(context: Context, currentUserId: String) {
        if (isInitialized) return
        isInitialized = true

        Log.d("FriendRequestListener", "Starting friend request listener for user: $currentUserId")

        val requestsRef = FirebaseDatabase.getInstance().reference
            .child("users")
            .child(currentUserId)
            .child("received_requests")

        listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (KonnektApplication.isAppInForeground) {
                    val fromUserId = snapshot.child("from").getValue(String::class.java) ?: return

                    FirebaseDatabase.getInstance().reference
                        .child("users")
                        .child(fromUserId)
                        .get()
                        .addOnSuccessListener { userSnapshot ->
                            val username = userSnapshot.child("username")
                                .getValue(String::class.java) ?: "Unknown User"

                            ForegroundNotificationHandler.showFriendRequestNotification(
                                context = context,
                                username = username,
                                fromUserId = fromUserId
                            )
                        }
                        .addOnFailureListener { exception ->
                            Log.e("FriendRequestListener", "Failed to fetch user details", exception)
                        }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {
                Log.e("FriendRequestListener", "Error listening to friend requests: ${error.message}")
            }
        }

        requestsRef.addChildEventListener(listener!!)
    }

    fun stopListening(currentUserId: String) {
        listener?.let {
            FirebaseDatabase.getInstance().reference
                .child("users")
                .child(currentUserId)
                .child("received_requests")
                .removeEventListener(it)
        }
        listener = null
        isInitialized = false
        Log.d("FriendRequestListener", "Stopped friend request listener")
    }
}