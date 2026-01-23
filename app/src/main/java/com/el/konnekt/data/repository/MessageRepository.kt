package com.el.konnekt.data.repository

import android.util.Log
import com.el.konnekt.data.models.Message
import com.el.konnekt.utils.MessageObfuscator
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MessageRepository {
    private val database = FirebaseDatabase.getInstance().reference

    suspend fun sendMessage(
        chatId: String,  // "userId1_userId2" or "group_12345"
        senderId: String,
        senderName: String,
        receiverId: String,
        text: String,
        replyToId: String? = null
    ) {
        try {
            val messageId = database.child("chats").child(chatId).child("messages").push().key ?: return
            val timestamp = System.currentTimeMillis()

            // CRITICAL: Determine encryption key
            // For groups: chatId comes as "group_12345", use "12345" for encryption
            // For 1-on-1: chatId is "userId1_userId2", use as-is
            val encryptionKey = if (chatId.startsWith("group_")) {
                chatId.removePrefix("group_")
            } else {
                chatId
            }

            Log.d("MessageRepository", "Sending message - chatId: $chatId, encryptionKey: $encryptionKey")

            // Encrypt with the key
            val encryptedText = MessageObfuscator.obfuscate(text, encryptionKey)

            val message = Message(
                id = messageId,
                senderId = senderId,
                senderName = senderName,
                receiverId = receiverId,
                text = encryptedText,
                timestamp = timestamp,
                replyTo = replyToId,
                seen = false,
                edited = false
            )

            // CRITICAL: Firebase path uses chatId as-is (with "group_" prefix for groups)
            database.child("chats")
                .child(chatId)
                .child("messages")
                .child(messageId)
                .setValue(message)
                .await()

            Log.d("MessageRepository", "Message sent successfully")
        } catch (e: Exception) {
            Log.e("MessageRepository", "Send message failed: ${e.message}", e)
            throw e
        }
    }

    suspend fun editMessage(chatId: String, messageId: String, newText: String) {
        try {
            // Determine encryption key same way
            val encryptionKey = if (chatId.startsWith("group_")) {
                chatId.removePrefix("group_")
            } else {
                chatId
            }

            val encryptedText = MessageObfuscator.obfuscate(newText, encryptionKey)

            // Firebase path uses chatId as-is
            database.child("chats")
                .child(chatId)
                .child("messages")
                .child(messageId)
                .updateChildren(mapOf(
                    "text" to encryptedText,
                    "edited" to true
                ))
                .await()
        } catch (e: Exception) {
            Log.e("MessageRepository", "Edit message failed: ${e.message}", e)
        }
    }

    fun observeMessages(chatId: String, currentUserId: String): Flow<List<Message>> = callbackFlow {
        val messagesRef = database.child("chats").child(chatId).child("messages")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { messageSnapshot ->
                    try {
                        val message = messageSnapshot.getValue(Message::class.java)

                        // Filter out messages deleted for this user
                        if (message != null) {
                            val deletedForUsers = messageSnapshot.child("deletedFor")
                                .children.mapNotNull { it.key }

                            if (currentUserId !in deletedForUsers && !message.deletedForEveryone) {
                                message
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("MessageRepository", "Error parsing message: ${e.message}")
                        null
                    }
                }
                trySend(messages.sortedBy { it.timestamp })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MessageRepository", "Observe messages cancelled: ${error.message}")
                close(error.toException())
            }
        }

        messagesRef.addValueEventListener(listener)
        awaitClose { messagesRef.removeEventListener(listener) }
    }

    suspend fun deleteMessageForSelf(chatId: String, messageId: String, userId: String) {
        try {
            database.child("chats")
                .child(chatId)
                .child("messages")
                .child(messageId)
                .child("deletedFor")
                .child(userId)
                .setValue(true)
                .await()
        } catch (e: Exception) {
            Log.e("MessageRepository", "Delete for self failed: ${e.message}", e)
        }
    }

    suspend fun deleteMessageForEveryone(chatId: String, messageId: String) {
        try {
            database.child("chats")
                .child(chatId)
                .child("messages")
                .child(messageId)
                .updateChildren(mapOf(
                    "deletedForEveryone" to true,
                    "text" to "This message was deleted"
                ))
                .await()
        } catch (e: Exception) {
            Log.e("MessageRepository", "Delete for everyone failed: ${e.message}", e)
        }
    }

    suspend fun markMessagesAsSeen(chatId: String, currentUserId: String, isGroupChat: Boolean) {
        try {
            val messagesRef = database.child("chats").child(chatId).child("messages")
            val snapshot = messagesRef.get().await()

            snapshot.children.forEach { messageSnapshot ->
                val message = messageSnapshot.getValue(Message::class.java)
                if (message != null && !message.seen) {
                    // For 1-on-1: mark as seen if I'm the receiver
                    // For groups: mark as seen if I'm not the sender
                    val shouldMarkSeen = if (isGroupChat) {
                        message.senderId != currentUserId
                    } else {
                        message.receiverId == currentUserId
                    }

                    if (shouldMarkSeen) {
                        messageSnapshot.ref.child("seen").setValue(true)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MessageRepository", "Mark messages as seen failed: ${e.message}", e)
        }
    }
}