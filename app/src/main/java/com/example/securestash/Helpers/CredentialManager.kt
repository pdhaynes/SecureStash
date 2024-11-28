package com.example.securestash.Helpers

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.KeyStore
import java.security.SecureRandom


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
        // Decode the Base64 encoded string to get the byte array
        val decodedData = Base64.decode(encryptedData, Base64.DEFAULT)

        // Ensure the decoded data is long enough to contain the IV (12 bytes for GCM) and the encrypted content
        if (decodedData.size < 12) {
            throw IllegalArgumentException("Invalid encrypted data: IV and encrypted content missing or corrupted.")
        }

        // Extract the IV (first 12 bytes) and the encrypted content (remaining bytes)
        val iv = decodedData.copyOfRange(0, 12) // GCM IV is typically 12 bytes
        val encryptedContent = decodedData.copyOfRange(12, decodedData.size)

        // Initialize the cipher for decryption
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val key = getSecretKey()  // Get the key from Keystore

        // Set up the GCMParameterSpec with the IV and an authentication tag length of 128 bits
        val spec = GCMParameterSpec(128, iv)  // 128-bit authentication tag length
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        // Decrypt the content
        val decryptedData = cipher.doFinal(encryptedContent)

        // Convert decrypted byte array to String
        return String(decryptedData, Charsets.UTF_8)
    }


    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE)
        keyStore.load(null)
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

}