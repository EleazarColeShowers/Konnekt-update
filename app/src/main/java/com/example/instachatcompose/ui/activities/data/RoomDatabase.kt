package com.example.instachatcompose.ui.activities.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [UserEntity::class, FriendEntity::class, MessageEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun friendDao(): FriendDao
    abstract fun messageDao(): MessageDao
}
