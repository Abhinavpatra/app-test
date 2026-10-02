package com.bloomcycle.app.data.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Holds the SQLCipher passphrase, wrapped by a non-exportable Android Keystore key.
 *
 * Hierarchy:
 *   Android Keystore (TEE-backed, never leaves the device)
 *     └─ AES-256-GCM "bloom.db.wrap"
 *          └─ wraps the 32-byte random SQLCipher passphrase
 *               └─ stored as Base64 ciphertext in ordinary SharedPreferences
 *
 * The ciphertext alone is useless without the Keystore key, which is why it does not
 * need to be encrypted a second time — and why it must never be backed up (see
 * backup_rules.xml: a restored ciphertext with no key is an unopenable database).
 *
 * The passphrase is generated randomly rather than derived from user input on purpose:
 * a forgotten passphrase would mean unrecoverable loss of someone's period history,
 * with no reset flow possible.
 */
class PassphraseProvider(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Returns the database passphrase, creating it on first use. Caller owns the array. */
    fun getOrCreate(): ByteArray {
        prefs.getString(KEY_WRAPPED, null)?.let { wrapped ->
            return unwrap(wrapped)
        }
        val fresh = ByteArray(PASSPHRASE_BYTES).also { java.security.SecureRandom().nextBytes(it) }
        prefs.edit().putString(KEY_WRAPPED, wrap(fresh)).apply()
        return fresh
    }

    // --- Keystore operations ----------------------------------------------------------------

    private fun wrap(plain: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plain)
        val combined = ByteArray(iv.size + ciphertext.size)
        iv.copyInto(combined, 0)
        ciphertext.copyInto(combined, iv.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun unwrap(encoded: String): ByteArray {
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        // Android Keystore GCM uses a 12-byte IV; take it from the envelope rather than
        // hardcoding so a future provider change cannot silently corrupt the payload.
        val ivLength = combined.size - GCM_TAG_BITS / 8
        val iv = combined.copyOfRange(0, ivLength)
        val ciphertext = combined.copyOfRange(ivLength, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, wrappingKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun wrappingKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) createWrappingKey()
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    /**
     * No user authentication is required on this key — requiring a unlock would make the
     * database unreadable while the screen is locked, and background reminder workers
     * read it too.
     */
    private fun createWrappingKey() {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build(),
        )
        generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "bloom.db.wrap"
        const val PREFS_NAME = "bloom_crypto"
        const val KEY_WRAPPED = "wrapped_passphrase"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PASSPHRASE_BYTES = 32
        const val KEY_SIZE_BITS = 256
        const val GCM_TAG_BITS = 128
    }
}
