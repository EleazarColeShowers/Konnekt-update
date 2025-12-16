package com.el.konnekt.data.core

import android.Manifest
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import com.el.konnekt.data.ChatViewModel

object ChatManager {
    private var hasStartedListening = false
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    fun startListeningForMessages(
        context: Context,
        chatId: String,
        currentUserId: String,
        chatViewModel: ChatViewModel
    ) {
        if (!hasStartedListening) {
            hasStartedListening = true
            chatViewModel.observeMessages(context, chatId, currentUserId, isChatOpen = false,
                requestNotificationPermission = {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                })
        }
    }
}