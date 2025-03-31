package com.example.instachatcompose.ui.activities.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
}

@Dao
interface FriendDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: FriendEntity)

    @Query("SELECT * FROM friends")
    suspend fun getAllFriends(): List<FriendEntity>

    @Query("DELETE FROM friends WHERE friendId = :friendId")
    suspend fun deleteFriend(friendId: String)
}

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE (senderId = :userId OR receiverId = :userId) ORDER BY timestamp ASC")
    suspend fun getMessagesForUser(userId: String): List<MessageEntity>

    @Query("UPDATE messages SET seen = 1 WHERE messageId = :messageId")
    suspend fun markMessageAsSeen(messageId: String)
}
