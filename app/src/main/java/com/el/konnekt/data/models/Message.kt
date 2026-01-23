package com.el.konnekt.data.models

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val text: String = "",                          // Encrypted text (saved to Firebase)
    val timestamp: Long = 0,
    val seen: Boolean = false,
    val replyTo: String? = null,
    val edited: Boolean = false,
    val deletedFor: Map<String, Boolean>? = null,
    val replyToIv: String? = null,
    val decryptedText: String? = null,              // Decrypted text (runtime only, NOT saved to Firebase)
    val deletedForEveryone: Boolean = false
) {
    /**
     * Returns the text to display in the UI.
     * Uses decryptedText if available, otherwise falls back to text.
     */
    fun getDisplayText(): String = decryptedText ?: text

    /**
     * Checks if this message has been deleted for a specific user.
     */
    fun isDeletedFor(userId: String): Boolean = deletedFor?.get(userId) == true

    /**
     * Creates a copy with decrypted text.
     */
    fun withDecryptedText(decrypted: String): Message = copy(decryptedText = decrypted)
}