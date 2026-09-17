package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages secure storage of sensitive credentials (Gemini API key)
 * using Android Keystore with AES-256 GCM encryption.
 *
 * Adheres strictly to security requirements:
 * - Hardware-backed / Keystore-managed encryption
 * - Key never exposed in logs, plaintext storage, or errors
 * - Masked display when loaded
 */
class SecureKeyManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "dr_basta_secure_prefs"
        private const val KEY_ENCRYPTED_API_KEY = "enc_api_key"
        private const val KEY_IV = "enc_iv"
        private const val KEY_SELECTED_MODEL = "selected_model"
        private const val KEYSTORE_ALIAS = "dr_basta_keystore_alias"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128

        const val DEFAULT_MODEL = "gemini-2.5-flash"

        val SUPPORTED_MODELS = listOf(
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "gemini-1.5-flash",
            "gemini-2.0-flash"
        )
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val keyGenSpec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(keyGenSpec)
            return keyGenerator.generateKey()
        }
        return (keyStore.getEntry(KEYSTORE_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    @Synchronized
    fun saveApiKey(rawApiKey: String): Boolean {
        if (rawApiKey.isBlank()) return false
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(rawApiKey.trim().toByteArray(Charsets.UTF_8))

            prefs.edit()
                .putString(KEY_ENCRYPTED_API_KEY, Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
                .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    @Synchronized
    fun getApiKey(): String? {
        val encryptedBase64 = prefs.getString(KEY_ENCRYPTED_API_KEY, null) ?: return null
        val ivBase64 = prefs.getString(KEY_IV, null) ?: return null

        return try {
            val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val secretKey = getOrCreateSecretKey()

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    fun hasApiKey(): Boolean {
        return !prefs.getString(KEY_ENCRYPTED_API_KEY, null).isNullOrBlank()
    }

    fun getMaskedApiKey(): String? {
        val fullKey = getApiKey() ?: return null
        if (fullKey.length <= 8) return "••••••••"
        val start = fullKey.take(4)
        val end = fullKey.takeLast(4)
        return "$start••••••••••••$end"
    }

    @Synchronized
    fun clearApiKey() {
        prefs.edit()
            .remove(KEY_ENCRYPTED_API_KEY)
            .remove(KEY_IV)
            .apply()
    }

    fun getSelectedModel(): String {
        return prefs.getString(KEY_SELECTED_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
    }

    fun saveSelectedModel(model: String) {
        prefs.edit().putString(KEY_SELECTED_MODEL, model).apply()
    }

    fun resetAllSettings() {
        prefs.edit().clear().apply()
    }
}
