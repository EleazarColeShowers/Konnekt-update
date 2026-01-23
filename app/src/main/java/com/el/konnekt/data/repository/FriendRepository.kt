package com.el.konnekt.data.repository

import android.util.Log
import com.el.konnekt.data.models.Friend
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FriendRepository {
    private val database = FirebaseDatabase.getInstance().reference

    suspend fun loadFriendsWithDetails(
        userId: String
    ): List<Pair<Friend, Map<String, String>>> {
        return try {
            Log.d("FriendRepository", "Loading friends for user: $userId")

            val friendsSnapshot = database.child("users").child(userId)
                .child("friends").get().await()

            Log.d("FriendRepository", "Friends snapshot exists: ${friendsSnapshot.exists()}")
            Log.d("FriendRepository", "Friends count: ${friendsSnapshot.childrenCount}")

            val friendsList = mutableListOf<Pair<Friend, Map<String, String>>>()

            for (friendSnapshot in friendsSnapshot.children) {
                val friendKey = friendSnapshot.key ?: continue
                Log.d("FriendRepository", "Processing friend key: $friendKey")

                // The structure is: friends/[key]/friendId and friends/[key]/timestamp
                val friendId = friendSnapshot.child("friendId").getValue(String::class.java)
                val timestamp = friendSnapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()

                if (friendId == null) {
                    Log.w("FriendRepository", "Friend ID is null for key: $friendKey, skipping")
                    continue
                }

                Log.d("FriendRepository", "Friend ID: $friendId, Timestamp: $timestamp")

                val friend = Friend(friendId = friendId, timestamp = timestamp)

                // Fetch user details
                try {
                    val userSnapshot = database.child("users").child(friendId).get().await()

                    if (userSnapshot.exists()) {
                        val username = userSnapshot.child("username").getValue(String::class.java) ?: "Unknown"
                        val profileImageUri = userSnapshot.child("profileImageUri").getValue(String::class.java) ?: ""

                        Log.d("FriendRepository", "Fetched details for $friendId - username: $username")

                        val details = mapOf(
                            "username" to username,
                            "profileImageUri" to profileImageUri
                        )

                        friendsList.add(Pair(friend, details))
                    } else {
                        Log.w("FriendRepository", "User snapshot doesn't exist for $friendId")
                        friendsList.add(Pair(friend, mapOf(
                            "username" to "Unknown",
                            "profileImageUri" to ""
                        )))
                    }
                } catch (e: Exception) {
                    Log.e("FriendRepository", "Error fetching user details for $friendId: ${e.message}", e)
                    friendsList.add(Pair(friend, mapOf(
                        "username" to "Unknown",
                        "profileImageUri" to ""
                    )))
                }
            }

            Log.d("FriendRepository", "Successfully loaded ${friendsList.size} friends")
            friendsList.sortedByDescending { it.first.timestamp }
        } catch (e: Exception) {
            Log.e("FriendRepository", "Error loading friends: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun removeFriend(userId: String, friendId: String): Result<Unit> {
        return try {
            database.child("users").child(userId).child("friends")
                .child(friendId).removeValue().await()
            database.child("users").child(friendId).child("friends")
                .child(userId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FriendRepository", "Remove friend failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun observeReceivedRequestsCount(userId: String): Flow<Int> = callbackFlow {
        val requestsRef = database.child("users").child(userId).child("receivedRequests")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.childrenCount.toInt())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FriendRepository", "Observe requests error: ${error.message}")
                close(error.toException())
            }
        }

        requestsRef.addValueEventListener(listener)
        awaitClose { requestsRef.removeEventListener(listener) }
    }

    suspend fun getFriends(userId: String): Result<List<Friend>> {
        return try {
            val friendsSnapshot = database.child("users").child(userId)
                .child("friends").get().await()

            val friends = friendsSnapshot.children.mapNotNull { snapshot ->
                val friendId = snapshot.child("friendId").getValue(String::class.java) ?: return@mapNotNull null
                val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                Friend(friendId = friendId, timestamp = timestamp)
            }

            Result.success(friends)
        } catch (e: Exception) {
            Log.e("FriendRepository", "Get friends failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}