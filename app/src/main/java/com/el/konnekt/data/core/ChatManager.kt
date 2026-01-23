package com.el.konnekt.data.core

import com.el.konnekt.data.ChatViewModel

object ChatManager {
    private var hasStartedListening = false

    fun startListeningForMessages(
        chatId: String,
        currentUserId: String,
        chatViewModel: ChatViewModel
    ) {
        if (!hasStartedListening) {
            hasStartedListening = true
            chatViewModel.observeMessages(chatId, currentUserId)
        }
    }

    fun stopListeningForMessages(
        chatId: String,
        chatViewModel: ChatViewModel
    ) {
        if (hasStartedListening) {
            hasStartedListening = false
            chatViewModel.stopObservingMessages(chatId)
        }
    }

    fun reset() {
        hasStartedListening = false
    }
}