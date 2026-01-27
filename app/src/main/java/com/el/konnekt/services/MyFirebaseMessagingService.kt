package com.el.konnekt.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.el.konnekt.R
import com.el.konnekt.ui.activities.message.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        Log.d("FCMService", "Service created")

        // Get and save FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCMService", "Current FCM token: $token")

                // Save to Firebase
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                userId?.let {
                    FirebaseDatabase.getInstance().reference
                        .child("users")
                        .child(it)
                        .child("fcmToken")
                        .setValue(token)
                        .addOnSuccessListener {
                            Log.d("FCMService", "✅ Token saved successfully")
                        }
                        .addOnFailureListener { e ->
                            Log.e("FCMService", "❌ Failed to save token: ${e.message}")
                        }
                }
            } else {
                Log.e("FCMService", "Failed to get token: ${task.exception}")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCMService", "📱 Message received from: ${remoteMessage.from}")
        Log.d("FCMService", "📱 Data payload: ${remoteMessage.data}")
        Log.d("FCMService", "📱 Notification: ${remoteMessage.notification?.title}")

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            Log.w("FCMService", "User not logged in, ignoring notification")
            return
        }

        // Get data from Cloud Function
        val chatId = remoteMessage.data["chatId"] ?: ""
        val senderId = remoteMessage.data["senderId"] ?: ""
        val senderName = remoteMessage.data["senderName"] ?: "Unknown"
        val isGroupChat = remoteMessage.data["isGroupChat"]?.toBoolean() ?: false

        // Get notification content
        val title = remoteMessage.notification?.title ?: senderName
        val body = remoteMessage.notification?.body ?: ""

        Log.d("FCMService", "Showing notification for chat: $chatId")

        // Show notification
        showNotification(
            title = title,
            message = body,
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            currentUserId = currentUserId,
            isGroupChat = isGroupChat
        )
    }

    private fun showNotification(
        title: String,
        message: String,
        chatId: String,
        senderId: String,
        senderName: String,
        currentUserId: String,
        isGroupChat: Boolean
    ) {
        // Create intent to open chat
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("chatId", chatId)
            putExtra("friendId", senderId)
            putExtra("username", senderName)
            putExtra("currentUserId", currentUserId)
            putExtra("isGroupChat", isGroupChat)
            if (isGroupChat) {
                putExtra("groupId", chatId.removePrefix("group_"))
                putExtra("groupName", senderName)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            chatId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(this, "konnekt_messages")
            .setSmallIcon(R.drawable.konnekt)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)

        notificationManager.notify(chatId.hashCode(), builder.build())

        Log.d("FCMService", "✅ Notification shown successfully")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCMService", "🔄 New FCM token: $token")

        // Save token to Firebase
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            FirebaseDatabase.getInstance().reference
                .child("users")
                .child(it)
                .child("fcmToken")
                .setValue(token)
                .addOnSuccessListener {
                    Log.d("FCMService", "✅ New token saved successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("FCMService", "❌ Failed to save new token: ${e.message}")
                }
        }
    }
}