package com.example.instachatcompose.ui.activities.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val username: String,
    val email: String,
    val bio: String,
    val profileImageUri: String
)

@Entity(
    tableName = "friends",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["userId"],
        childColumns = ["userId"],
        onDelete = CASCADE
    )]
)
data class FriendEntity(
    @PrimaryKey val friendId: String, // The ID of the friend
    val userId: String, // The owner of this friend record
    val username: String,
    val profileImageUri: String,
    val timestamp: Long
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val senderId: String,
    val receiverId: String,
    val text: String,
    val timestamp: Long,
    val seen: Boolean,
    val edited: Boolean
)
