package com.gameside.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.gameside.domain.security.CredentialStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KeystoreCredentialStore @Inject constructor(
    @ApplicationContext context: Context,
) : CredentialStore {
    private val preferences = context.getSharedPreferences("encrypted_provider_credentials", Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    override suspend fun put(identifier: String, credential: String) = withContext(Dispatchers.IO) {
        require(identifier.matches(IDENTIFIER_PATTERN)) { "Invalid credential identifier" }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, getOrCreateKey()) }
        val encrypted = cipher.doFinal(credential.toByteArray(Charsets.UTF_8))
        val encoded = Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
        check(preferences.edit().putString(identifier, encoded).commit()) { "Credential storage failed" }
    }

    override suspend fun get(identifier: String): String? = withContext(Dispatchers.IO) {
        requireValidIdentifier(identifier)
        val encoded = preferences.getString(identifier, null) ?: return@withContext null
        runCatching {
            val payload = Base64.decode(encoded, Base64.NO_WRAP)
            require(payload.size > IV_BYTES)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_BITS, payload.copyOfRange(0, IV_BYTES)))
            }
            cipher.doFinal(payload.copyOfRange(IV_BYTES, payload.size)).toString(Charsets.UTF_8)
        }.getOrElse {
            preferences.edit().remove(identifier).commit()
            null
        }
    }

    override suspend fun remove(identifier: String) = withContext(Dispatchers.IO) {
        requireValidIdentifier(identifier)
        preferences.edit().remove(identifier).commit()
        Unit
    }

    override suspend fun contains(identifier: String): Boolean = withContext(Dispatchers.IO) {
        requireValidIdentifier(identifier)
        preferences.contains(identifier)
    }

    private fun requireValidIdentifier(identifier: String) {
        require(identifier.matches(IDENTIFIER_PATTERN)) { "Invalid credential identifier" }
    }

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "gameside_provider_credentials_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
        val IDENTIFIER_PATTERN = Regex("^[a-z0-9_.-]{1,64}$")
    }
}
