package com.example.instachatcompose.ui.activities.data.repository

import android.content.Context
import android.util.Log
import com.example.instachatcompose.ui.activities.data.crypto.CryptoUtil
import com.example.instachatcompose.ui.activities.data.Message
import com.example.instachatcompose.ui.activities.data.local.FriendEntity
import com.example.instachatcompose.ui.activities.data.local.GroupEntity
import com.example.instachatcompose.ui.activities.data.local.LocalDataSource
import com.example.instachatcompose.ui.activities.data.local.UserEntity
import com.example.instachatcompose.ui.activities.data.remote.FirebaseDataSource
import com.example.instachatcompose.ui.activities.mainpage.Friend
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.collections.get

class ChatRepository(
    private val firebase: FirebaseDataSource,
    private val local: LocalDataSource,
//    private val cryptoUtil: CryptoUtil
) {

    suspend fun fetchUsername(userId: String): String? = firebase.fetchUsername(userId)

    suspend fun fetchUserProfile(context: Context, userId: String): Pair<String?, String?> =
        firebase.fetchUserProfile(context, userId)

    suspend fun loadFriendsWithDetails(userId: String): List<Pair<Friend, Map<String, String>>> =
        firebase.loadFriendsWithDetails(userId)

    suspend fun fetchGroupMembers(groupId: String): List<String> = firebase.fetchGroupMembers(groupId)

    suspend fun fetchLastMessageTimestamp(chatId: String): Long = firebase.fetchLastMessageTimestamp(chatId)

    // observe messages: repository delegates DataSnapshot -> ViewModel but manages initial load flag
    fun observeMessages(
        chatId: String,
        onNewMessage: (DataSnapshot, Message, Boolean) -> Unit,
        onCancelled: (DatabaseError) -> Unit
    ) {
        var hasLoadedInitial = false
        firebase.addMessageListener(
            chatId = chatId,
            onChildAdded = { snapshot ->
                val msg = snapshot.getValue(Message::class.java)
                if (msg != null) onNewMessage(snapshot, msg, hasLoadedInitial)
            },
            onInitialLoad = { hasLoadedInitial = true },
            onCancelled = { dbError -> onCancelled(dbError) }
        )
    }


    fun removeMessageListener(chatId: String) = firebase.removeMessageListener(chatId)

    fun observeTyping(
        chatId: String,
        receiverId: String,
        onTypingChanged: (Boolean) -> Unit,
        onCancelled: (DatabaseError) -> Unit
    ) {
        firebase.addTypingListener(
            chatId = chatId,
            receiverId = receiverId,
            onDataChange = { value ->
                val isTyping = when (value) {
                    is Boolean -> value
                    is Map<*, *> -> (value["isTyping"] as? Boolean) ?: false
                    else -> false
                }
                onTypingChanged(isTyping)
            },
            onCancelled = { e -> onCancelled(e) }
        )
    }

    fun removeTypingListener(chatId: String, receiverId: String) =
        firebase.removeTypingListener(chatId, receiverId)

    fun sendMessage(chatId: String, message: Message) = firebase.sendMessage(chatId, message)

    fun setTypingStatus(chatId: String, currentUserId: String, isTyping: Boolean) =
        firebase.setTypingStatus(chatId, currentUserId, isTyping)

//    suspend fun decryptMessages(chatId: String, messages: List<Message>): List<Message> =
//        withContext(Dispatchers.Default) {
//            val keyAlias = "chat_$chatId"
//            messages.map { msg ->
//                try {
//                    val decrypted = CryptoUtil.decrypt(keyAlias, msg.text, msg.iv)
//                    msg.copy(decryptedText = decrypted)
//                } catch (e: Exception) {
//                    msg.copy(decryptedText = "[error]")
//                }
//            }
//        }

    suspend fun removeFriend(currentUserId: String, friendId: String) {
        firebase.removeFriendFromFirebase(currentUserId, friendId)
        local.deleteFriend(friendId, currentUserId)
    }

    suspend fun leaveGroup(currentUserId: String, groupId: String) {
        firebase.removeGroupMember(groupId, currentUserId)
        local.deleteGroup(groupId)
    }

    suspend fun loadFriendsFromLocal(userId: String) = local.getFriendsForUser(userId)

    suspend fun saveUserToLocal(user: UserEntity) = local.insertUser(user)

    suspend fun saveFriendEntities(list: List<FriendEntity>) = local.insertFriends(list)

    fun removeAllListeners() = firebase.removeAllListeners()

    suspend fun saveGroupEntities(groups: GroupEntity) = local.insertGroup(groups)

    fun loadGroupsForUser(userId: String): Flow<List<GroupEntity>> =
        local.getGroupsForUser(userId)

    suspend fun fetchUserProfilePic(userId: String): String? {
        return try {
            val snapshot = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("profileImageUri")
                .get()
                .await()

            snapshot.getValue(String::class.java)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error fetching profile image: ${e.message}")
            null
        }
    }
}