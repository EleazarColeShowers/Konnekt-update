package com.el.konnekt.data.repository

import android.net.Uri
import android.util.Log
import com.el.konnekt.data.local.AppDatabase
import com.el.konnekt.data.local.GroupEntity
import com.el.konnekt.data.models.GroupChat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GroupRepository(private val database: AppDatabase) {
    private val firebaseDb = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

    // Function 2: fetchGroupChats moved here
    suspend fun fetchGroupChats(currentUserId: String): List<GroupChat> {
        return try {
            val snapshot = firebaseDb.child("chats").get().await()
            val groupChats = mutableListOf<GroupChat>()

            for (groupSnapshot in snapshot.children) {
                val key = groupSnapshot.key ?: continue
                if (!key.startsWith("group_")) continue

                val membersSnapshot = groupSnapshot.child("members")
                val memberIds = membersSnapshot.children.mapNotNull { it.key }

                if (currentUserId in memberIds) {
                    val groupName = groupSnapshot.child("groupName").getValue(String::class.java) ?: "Unnamed Group"
                    val groupId = key.removePrefix("group_")
                    val groupImage = groupSnapshot.child("groupImage").getValue(String::class.java) ?: ""

                    groupChats.add(
                        GroupChat(
                            groupId = groupId,
                            groupName = groupName,
                            members = memberIds,
                            groupImage = groupImage
                        )
                    )
                }
            }
            groupChats
        } catch (e: Exception) {
            Log.e("GroupRepository", "Error fetching groups: ${e.message}")
            emptyList()
        }
    }

    // Function 3: Real-time group sync moved here
    fun observeUserGroups(userId: String): Flow<List<GroupChat>> = callbackFlow {
        val groupsRef = firebaseDb.child("chats")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userGroups = mutableListOf<GroupChat>()

                for (groupSnapshot in snapshot.children) {
                    val groupId = groupSnapshot.key ?: continue
                    if (!groupId.startsWith("group_")) continue

                    val membersSnapshot = groupSnapshot.child("members")
                    val memberIds = membersSnapshot.children.mapNotNull { it.key }

                    if (userId in memberIds) {
                        val groupName = groupSnapshot.child("groupName")
                            .getValue(String::class.java) ?: "Unnamed Group"
                        val groupImage = groupSnapshot.child("groupImage")
                            .getValue(String::class.java) ?: ""

                        userGroups.add(
                            GroupChat(
                                groupId = groupId.removePrefix("group_"),
                                groupName = groupName,
                                members = memberIds,
                                groupImage = groupImage
                            )
                        )
                    }
                }

                trySend(userGroups)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GroupRepository", "Error: ${error.message}")
                close(error.toException())
            }
        }

        groupsRef.addValueEventListener(listener)
        awaitClose { groupsRef.removeEventListener(listener) }
    }

    // Function 12: Group creation moved here
    suspend fun createGroup(
        groupId: String,
        groupName: String,
        members: List<String>,
        adminId: String,
        groupImageUri: Uri?
    ): Result<String> {
        return try {
            val imageUrl = groupImageUri?.let { uploadGroupImage(groupId, it) } ?: ""

            val groupData = mapOf(
                "groupName" to groupName,
                "members" to members.associateWith { true },
                "groupImage" to imageUrl,
                "adminId" to adminId
            )

            firebaseDb.child("chats").child(groupId).setValue(groupData).await()

            // Save to local DB
            val groupEntity = GroupEntity(
                groupId = groupId,
                userId = adminId,
                groupName = groupName,
                groupImageUri = imageUrl,
                memberIds = members.joinToString(",")
            )
            database.groupDao().insertGroup(groupEntity)

            Result.success(groupId)
        } catch (e: Exception) {
            Log.e("GroupRepository", "Create group failed: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun uploadGroupImage(groupId: String, imageUri: Uri): String {
        val storageRef = storage.child("group_images/$groupId/profile_image.jpg")
        storageRef.putFile(imageUri).await()
        return storageRef.downloadUrl.await().toString()
    }

    // Function 14: Leave group moved here
    suspend fun leaveGroup(userId: String, groupId: String): Result<Unit> {
        return try {
            firebaseDb.child("chats").child("group_$groupId")
                .child("members").child(userId).removeValue().await()
            database.groupDao().deleteGroup(groupId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}