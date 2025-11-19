package com.el.konnekt.ui.activities.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoUtil {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_SIZE = 128 // in bits

    private fun getKeyStore(): KeyStore {
        return KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    // ----------------------------
    // AES key generation and use
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

    fun generateExportableAesKey(): ByteArray {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        return keyGen.generateKey().encoded
    }


    private fun getSecretKey(alias: String): SecretKey {
        val ks = getKeyStore()
        val entry = ks.getEntry(alias, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    fun getAesKeyBytes(alias: String): ByteArray {
        val secretKey = getSecretKey(alias)
        val keyBytes = secretKey.encoded
        return keyBytes ?: throw IllegalStateException("Unable to extract AES key bytes from Android Keystore")
    }


    fun encrypt(keyAlias: String, plainText: String): Pair<String, String> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = getSecretKey(keyAlias)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
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

    // ----------------------------
    // RSA key generation and retrieval
    fun generateRsaKeyPairIfNeeded(alias: String) {
        val ks = getKeyStore()
        if (!ks.containsAlias(alias)) {
            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .build()
            keyPairGenerator.initialize(spec)
            keyPairGenerator.generateKeyPair()
        }
    }

    fun getPublicKeyBase64(alias: String): String {
        val keyStore = getKeyStore()
        val certificate = keyStore.getCertificate(alias)
        val publicKey = certificate.publicKey.encoded
        return Base64.encodeToString(publicKey, Base64.NO_WRAP)
    }

    // ----------------------------
    // 🔐 Encrypt AES key with receiver’s RSA public key
    fun encryptAesKeyWithRsa(publicKeyBytes: ByteArray, aesKey: ByteArray): ByteArray {
        val keyFactory = KeyFactory.getInstance("RSA")
        val publicKeySpec = X509EncodedKeySpec(publicKeyBytes)
        val publicKey = keyFactory.generatePublic(publicKeySpec)

        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(aesKey)
    }

    // 🔓 Decrypt AES key using current user's RSA private key
    fun decryptAesKeyWithRsa(alias: String, encryptedAesKeyBase64: String): ByteArray {
        val keyStore = getKeyStore()
        val privateKeyEntry = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
            ?: throw IllegalStateException("No RSA key pair found for alias: $alias")

        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.DECRYPT_MODE, privateKeyEntry.privateKey)
        val encryptedBytes = Base64.decode(encryptedAesKeyBase64, Base64.NO_WRAP)
        return cipher.doFinal(encryptedBytes)
    }
}
