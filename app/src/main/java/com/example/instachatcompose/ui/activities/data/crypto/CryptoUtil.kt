package com.example.instachatcompose.ui.activities.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoUtil {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12 // 96 bits recommended for GCM
    private const val TAG_SIZE = 128 // in bits

    private fun getKeyStore(): KeyStore {
        return KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    fun generateAesKeyIfNeeded(alias: String) {
        val ks = getKeyStore()
        if (!ks.containsAlias(alias)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(alias: String): SecretKey {
        val ks = getKeyStore()
        val entry = ks.getEntry(alias, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    fun encrypt(keyAlias: String, plainText: String): Pair<String, String> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = getSecretKey(keyAlias)

        // Let the system generate IV automatically
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv   // ✅ get the system-generated IV
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val encryptedText = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        val encodedIv = Base64.encodeToString(iv, Base64.NO_WRAP)

        return Pair(encryptedText, encodedIv)
    }

    fun decrypt(keyAlias: String, encryptedText: String, encodedIv: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = getSecretKey(keyAlias)

        val iv = Base64.decode(encodedIv, Base64.NO_WRAP)
        val spec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val decodedBytes = Base64.decode(encryptedText, Base64.NO_WRAP)
        val decryptedBytes = cipher.doFinal(decodedBytes)

        return String(decryptedBytes, Charsets.UTF_8)
    }
}