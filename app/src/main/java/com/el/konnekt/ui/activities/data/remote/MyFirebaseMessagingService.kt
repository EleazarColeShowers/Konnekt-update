package com.el.konnekt.ui.activities.data.remote

import android.app.NotificationManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.el.konnekt.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val notification = remoteMessage.notification
        if (notification != null) {
            showNotification(notification.title, notification.body)
        }
    }

    private fun showNotification(title: String?, body: String?) {
        val builder = NotificationCompat.Builder(this, "default_channel")
            .setSmallIcon(R.drawable.notificationkonnekt)
            .setContentTitle(title ?: "New message")
            .setContentText(body ?: "")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(0, builder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO: Save this token to your backend
        Log.d("FCM", "New token: $token")
    }
}