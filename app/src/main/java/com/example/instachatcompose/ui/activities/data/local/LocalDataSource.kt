package com.example.instachatcompose.ui.activities.data.local

import kotlinx.coroutines.flow.Flow

class LocalDataSource(private val db: AppDatabase) {
    private val userDao = db.userDao()
    private val friendDao = db.friendDao()
    private val groupDao = db.groupDao()

    // ✅ match the DAO
    suspend fun insertGroup(group: GroupEntity) = groupDao.insertGroup(group)

    // ✅ match the DAO return type (Flow<List<GroupEntity>>)
    fun getGroupsForUser(userId: String): Flow<List<GroupEntity>> = groupDao.getGroupsForUser(userId)

    suspend fun getUserById(id: String) = userDao.getUserById(id)
    suspend fun insertUser(user: UserEntity) = userDao.insertUser(user)

    suspend fun getFriendsForUser(userId: String) = friendDao.getFriendsForUser(userId)
    suspend fun insertFriends(list: List<FriendEntity>) = friendDao.insertFriends(list)
    suspend fun deleteFriend(friendId: String, userId: String) = friendDao.deleteFriend(friendId, userId)

    suspend fun deleteGroup(groupId: String) = groupDao.deleteGroup(groupId)
}