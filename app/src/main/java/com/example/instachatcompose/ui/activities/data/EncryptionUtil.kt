package com.example.instachatcompose.ui.activities.data

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtil {
    private const val SECRET_KEY = "MySuperSecretKey" // 16 chars = 128-bit key
    private const val INIT_VECTOR = "RandomInitVector" // 16-byte IV

    private fun getSecretKey(): SecretKey {
        return SecretKeySpec(SECRET_KEY.toByteArray(), "AES")
    }

    fun encrypt(input: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        val iv = IvParameterSpec(INIT_VECTOR.toByteArray())
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), iv)
        val encrypted = cipher.doFinal(input.toByteArray())
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }

    fun decrypt(encrypted: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        val iv = IvParameterSpec(INIT_VECTOR.toByteArray())
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), iv)
        val original = cipher.doFinal(Base64.decode(encrypted, Base64.DEFAULT))
        return String(original)
    }
}
