package com.el.konnekt.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.el.konnekt.R
import com.el.konnekt.ui.activities.konnekt.Konnekt
import com.el.konnekt.ui.activities.message.ChatActivity

object ForegroundNotificationHandler {

    private const val FRIEND_REQUEST_CHANNEL_ID = "friend_request_foreground"
    private const val MESSAGE_CHANNEL_ID = "message_foreground"
    private const val FRIEND_REQUEST_NOTIFICATION_ID = 1001
    private const val MESSAGE_NOTIFICATION_BASE_ID = 2000

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val friendRequestChannel = NotificationChannel(
                FRIEND_REQUEST_CHANNEL_ID,
                "Friend Requests",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new friend requests"
                enableVibration(true)
            }

            val messageChannel = NotificationChannel(
                MESSAGE_CHANNEL_ID,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new messages"
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(friendRequestChannel)
            notificationManager.createNotificationChannel(messageChannel)
        }
    }

    fun showFriendRequestNotification(
        context: Context,
        username: String,
        fromUserId: String
    ) {
        val intent = Intent(context, Konnekt::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, FRIEND_REQUEST_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Make sure this icon exists
            .setContentTitle("New Friend Request")
            .setContentText("$username sent you a friend request")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(FRIEND_REQUEST_NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                // Permission not granted
            }
        }
    }

    fun showMessageNotification(
        context: Context,
        senderName: String,
        messageText: String,
        chatId: String,
        senderId: String,
        profileImageUri: String,
        isGroupChat: Boolean = false,
        groupName: String? = null,
        groupImageUri: String? = null
    ) {
        val intent = Intent(context, ChatActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("chatId", chatId)
            putExtra("currentUserId", senderId)
            putExtra("username", if (isGroupChat) groupName else senderName)
            putExtra("profileImageUri", if (isGroupChat) groupImageUri else profileImageUri)
            putExtra("isGroupChat", isGroupChat)
            if (isGroupChat) {
                putExtra("groupId", chatId.removePrefix("group_"))
                putExtra("groupName", groupName)
                putExtra("groupImageUri", groupImageUri)
            } else {
                putExtra("friendId", senderId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            chatId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayText = if (isGroupChat) {
            "$senderName: $messageText"
        } else {
            messageText
        }

        val notification = NotificationCompat.Builder(context, MESSAGE_CHANNEL_ID)
            .setSmallIcon(R.drawable.konnekt)
            .setContentTitle(if (isGroupChat) groupName else senderName)
            .setContentText(displayText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setGroup(chatId)
            .build()

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(MESSAGE_NOTIFICATION_BASE_ID + chatId.hashCode(), notification)
            } catch (e: SecurityException) {
                // Permission not granted
            }
        }
    }

    fun cancelMessageNotification(context: Context, chatId: String) {
        with(NotificationManagerCompat.from(context)) {
            cancel(MESSAGE_NOTIFICATION_BASE_ID + chatId.hashCode())
        }
    }

    fun cancelFriendRequestNotification(context: Context) {
        with(NotificationManagerCompat.from(context)) {
            cancel(FRIEND_REQUEST_NOTIFICATION_ID)
        }
    }
}