package com.example.securestash.Helpers

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.KeyStore


class CredentialManager {
    private val KEY_ALIAS = "credential_manager"
    private val KEYSTORE = "AndroidKeyStore"

    init {
        if (!isKeyExist(KEY_ALIAS)) {
            generateKey()
        }
    }

    private fun isKeyExist(alias: String): Boolean {
        val keyStore = KeyStore.getInstance(KEYSTORE)
        keyStore.load(null)
        return keyStore.containsAlias(alias)
    }

    private fun generateKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    fun encryptData(data: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = getSecretKey()

        cipher.init(Cipher.ENCRYPT_MODE, key)

        val encryption = cipher.doFinal(data.toByteArray())

        val iv = cipher.iv

        val ivAndEncrypted = iv + encryption

        return Base64.encodeToString(ivAndEncrypted, Base64.DEFAULT)
    }


    fun decryptData(encryptedData: String): String {
        val decodedData = Base64.decode(encryptedData, Base64.DEFAULT)

        if (decodedData.size < 12) {
            throw IllegalArgumentException("Invalid encrypted data: IV and encrypted content missing or corrupted.")
        }

        val iv = decodedData.copyOfRange(0, 12)
        val encryptedContent = decodedData.copyOfRange(12, decodedData.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = getSecretKey()

        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        val decryptedData = cipher.doFinal(encryptedContent)

        return String(decryptedData, Charsets.UTF_8)
    }


    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE)
        keyStore.load(null)
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

}