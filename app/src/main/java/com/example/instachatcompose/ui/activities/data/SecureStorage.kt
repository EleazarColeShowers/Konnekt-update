package com.example.instachatcompose.ui.activities.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object SecureStorage {
    fun getSecureSharedPreferences(context: Context): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            "SecurePrefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveUserCredentials(context: Context, email: String, password: String) {
        val sharedPreferences = getSecureSharedPreferences(context)
        sharedPreferences.edit()
            .putString("email", email)
            .putString("password", password)
            .apply()
    }

    fun getUserCredentials(context: Context): Pair<String?, String?> {
        val sharedPreferences = getSecureSharedPreferences(context)
        val email = sharedPreferences.getString("email", null)
        val password = sharedPreferences.getString("password", null)
        return Pair(email, password)
    }
}