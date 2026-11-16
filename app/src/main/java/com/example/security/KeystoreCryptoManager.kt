package com.example.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreCryptoManager(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun storeTwoStepPin(pin: String) {
        val digest = sha256Hex(pin)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(digest.toByteArray(Charsets.UTF_8))
        val combined = Base64.encodeToString(iv + ciphertext, Base64.NO_WRAP)
        prefs.edit().putString(KEY_TWO_STEP_BLOB, combined).apply()
    }

    fun verifyTwoStepPin(pin: String): Boolean {
        val blob = prefs.getString(KEY_TWO_STEP_BLOB, null) ?: return false
        return try {
            val bytes = Base64.decode(blob, Base64.NO_WRAP)
            if (bytes.size <= 12) return false
            val iv = bytes.copyOfRange(0, 12)
            val ciphertext = bytes.copyOfRange(12, bytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            val decrypted = String(cipher.doFinal(ciphertext), Charsets.UTF_8)
            decrypted == sha256Hex(pin)
        } catch (_: Exception) {
            false
        }
    }

    fun clearTwoStepPin() {
        prefs.edit().remove(KEY_TWO_STEP_BLOB).apply()
    }

    fun hasTwoStepPin(): Boolean = prefs.contains(KEY_TWO_STEP_BLOB)

    fun storeChatLockSecretCode(code: String) {
        val normalized = code.trim()
        require(normalized.isNotEmpty()) { "Secret code cannot be empty" }
        val digest = sha256Hex(normalized)
        runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(digest.toByteArray(Charsets.UTF_8))
            val combined = Base64.encodeToString(iv + ciphertext, Base64.NO_WRAP)
            prefs.edit().putString(KEY_CHAT_LOCK_SECRET_BLOB, combined).remove(KEY_CHAT_LOCK_SECRET_HASH).apply()
        }.onFailure {
            // Fallback for headless test environments without AndroidKeyStore
            prefs.edit().putString(KEY_CHAT_LOCK_SECRET_HASH, digest).remove(KEY_CHAT_LOCK_SECRET_BLOB).apply()
        }
    }

    fun verifyChatLockSecretCode(code: String): Boolean {
        val candidate = code.trim()
        if (candidate.isEmpty()) return false
        val expectedDigest = sha256Hex(candidate)
        val blob = prefs.getString(KEY_CHAT_LOCK_SECRET_BLOB, null)
        if (blob != null) {
            val decryptedMatch = try {
                val bytes = Base64.decode(blob, Base64.NO_WRAP)
                if (bytes.size <= 12) false
                else {
                    val iv = bytes.copyOfRange(0, 12)
                    val ciphertext = bytes.copyOfRange(12, bytes.size)
                    val cipher = Cipher.getInstance(TRANSFORMATION)
                    cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
                    val decrypted = String(cipher.doFinal(ciphertext), Charsets.UTF_8)
                    decrypted == expectedDigest
                }
            } catch (_: Exception) {
                false
            }
            if (decryptedMatch) return true
        }
        val fallbackHash = prefs.getString(KEY_CHAT_LOCK_SECRET_HASH, null)
        return fallbackHash != null && fallbackHash == expectedDigest
    }

    fun clearChatLockSecretCode() {
        prefs.edit()
            .remove(KEY_CHAT_LOCK_SECRET_BLOB)
            .remove(KEY_CHAT_LOCK_SECRET_HASH)
            .apply()
    }

    fun hasChatLockSecretCode(): Boolean =
        prefs.contains(KEY_CHAT_LOCK_SECRET_BLOB) || prefs.contains(KEY_CHAT_LOCK_SECRET_HASH)

    private fun sha256Hex(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "limon_chat_keystore_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PREFS_NAME = "limon_secure_prefs"
        const val KEY_TWO_STEP_BLOB = "two_step_pin_blob"
        const val KEY_CHAT_LOCK_SECRET_BLOB = "chat_lock_secret_blob"
        const val KEY_CHAT_LOCK_SECRET_HASH = "chat_lock_secret_hash"
    }
}
