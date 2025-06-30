package com.example.instachatcompose.ui.activities.data

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec

object MessageCrypto {
    fun encrypt(text: String): Pair<String, String> {
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.ENCRYPT_MODE, KeystoreHelper.getSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(text.toByteArray())

        val encryptedText = Base64.encodeToString(encrypted, Base64.DEFAULT)
        val ivString = Base64.encodeToString(iv, Base64.DEFAULT)

        return encryptedText to ivString
    }

    fun decrypt(encryptedText: String, ivString: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        val iv = IvParameterSpec(Base64.decode(ivString, Base64.DEFAULT))
        cipher.init(Cipher.DECRYPT_MODE, KeystoreHelper.getSecretKey(), iv)
        val decrypted = cipher.doFinal(Base64.decode(encryptedText, Base64.DEFAULT))
        return String(decrypted)
    }
}
