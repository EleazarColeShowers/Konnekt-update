package com.el.konnekt.utils

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object MessageObfuscator {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/ECB/PKCS5Padding"

    /**
     * Generate a consistent key for a chat based on participant IDs
     * Same participants = same key (so all devices can decrypt)
     */
    private fun getChatKey(chatId: String): SecretKeySpec {
        // Use chat ID as seed - consistent across all devices
        val keyBytes = chatId.toByteArray()

        // Hash to get 256-bit key
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(keyBytes)

        // Use first 16 bytes for AES-128
        return SecretKeySpec(hash.copyOf(16), ALGORITHM)
    }

    /**
     * Obfuscate message text
     * Returns Base64 encoded encrypted text
     */
    fun obfuscate(plainText: String, chatId: String): String {
        return try {
            val key = getChatKey(chatId)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        } catch (e: Exception) {
            // If encryption fails, return original (better than losing message)
            plainText
        }
    }

    /**
     * De-obfuscate message text
     * Returns original plain text
     */
    fun deobfuscate(obfuscatedText: String, chatId: String): String {
        return try {
            val key = getChatKey(chatId)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key)

            val decoded = Base64.decode(obfuscatedText, Base64.NO_WRAP)
            val decrypted = cipher.doFinal(decoded)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            // If decryption fails, might be unencrypted old message
            obfuscatedText
        }
    }
}