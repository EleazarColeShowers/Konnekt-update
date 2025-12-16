package com.el.konnekt.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriends(friends: List<FriendEntity>)

    @Query("SELECT * FROM friends WHERE userId = :userId")
    suspend fun getFriendsForUser(userId: String): List<FriendEntity>

    @Query("SELECT * FROM friends WHERE userId = :userId AND friendId = :friendId LIMIT 1")
    suspend fun getFriendByUserAndFriendId(
        userId: String,
        friendId: String
    ): FriendEntity?

    @Query("SELECT * FROM friends WHERE userId = :currentUserId")
    fun observeFriendsForUser(currentUserId: String): Flow<List<FriendEntity>>

    @Query("DELETE FROM friends WHERE friendId = :friendId AND userId = :userId")
    suspend fun deleteFriend(friendId: String, userId: String)
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

@Dao
interface GroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Query("SELECT * FROM group_table")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("DELETE FROM group_table WHERE groupId = :groupId")
    suspend fun deleteGroup(groupId: String)

    @Query("SELECT * FROM group_table WHERE userId = :userId")
    fun getGroupsForUser(userId: String): Flow<List<GroupEntity>>

}
