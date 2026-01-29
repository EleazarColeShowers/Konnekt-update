package com.el.konnekt.data.models

data class ChatState(
    val lastMessage: String? = null,
    val timestamp: Long? = null,
    val unreadCount: Int = 0,
    val hasUnreadMessages: Boolean = false
)
