package com.example.instachatcompose.ui.activities.data

import android.util.Log
import com.example.instachatcompose.ui.activities.mainpage.Friend
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FriendRepository(
    private val friendDao: FriendDao
) {
    private val db = Firebase.database.reference

    suspend fun fetchFriendsFromFirebase(currentUserId: String): List<FriendEntity> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = db.child("friends").child(currentUserId).get().await()
                val friends = snapshot.children.mapNotNull {
                    val friend = it.getValue(Friend::class.java)
                    friend?.let { f ->
                        FriendEntity(
                            friendId = f.friendId,
                            username = it.child("username").getValue(String::class.java) ?: "",
                            profileImageUri = it.child("profileImageUri").getValue(String::class.java) ?: "",
                            timestamp = f.timestamp
                        )
                    }
                }
                // Save fetched friends to Room DB
                friendDao.insertFriend(friends)
                friends
            } catch (e: Exception) {
                Log.e("FriendRepository", "Error fetching friends", e)
                emptyList()
            }
        }
    }

    suspend fun getFriends(currentUserId: String): List<FriendEntity> {
        return withContext(Dispatchers.IO) {
            val isOnline = isNetworkAvailable()
            if (isOnline) {
                fetchFriendsFromFirebase(currentUserId)
            } else {
                friendDao.getAllFriends()
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        // Implement a function to check network status
        return true // Placeholder
    }
}
