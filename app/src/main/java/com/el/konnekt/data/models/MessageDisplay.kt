package com.el.konnekt.data.models

data class MessageDisplay(
    val id: String,
    val senderId: String,
    val senderName: String,
    val receiverId: String,
    val originalText: String, // encrypted
    val displayText: String,  // decrypted - DONE IN REPOSITORY
    val timestamp: Long,
    val seen: Boolean,
    val replyTo: String? = null,
    val edited: Boolean = false
)