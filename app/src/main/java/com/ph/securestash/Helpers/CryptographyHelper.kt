package com.ph.securestash.Helpers

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import com.ph.securestash.DataModels.ItemType
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CryptographyHelper {
    fun encodeFile(context: Context, fileUri: Uri, fileKey: SecretKey, fileType: ItemType, isLocked: Boolean): Pair<ByteArray, ByteArray> {
        var imageBytes: ByteArray? = null

        val encryptCipher = Cipher.getInstance("AES/GCM/NoPadding")

        val inputStream: InputStream? = context.contentResolver.openInputStream(fileUri)
        inputStream?.use { stream ->
            val outputStream = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            imageBytes = outputStream.toByteArray()
        }

        // Should be 4 bytes
        val metadata = prepareMetadata(fileType, isLocked)

        val concatenatedData = metadata + imageBytes!!

        encryptCipher.init(Cipher.ENCRYPT_MODE, fileKey)

        val encryptedData = encryptCipher.doFinal(concatenatedData)

        return Pair(encryptCipher.iv, encryptedData)
    }

    fun saveEncodedFile(file: File, fileData: Pair<ByteArray, ByteArray>) {
        FileOutputStream(file).use { outputStream ->
            outputStream.write(fileData.first)
            outputStream.write(fileData.second)
        }
    }

    fun decodeFile(file: File, secretKey: SecretKey): Pair<ByteArray, ByteArray> {
        val encodedData = ByteArrayOutputStream()
        val readIV = ByteArray(12)
        FileInputStream(file).use { fileInputStream ->
            val ivBytesRead = fileInputStream.read(readIV)
            if (ivBytesRead != readIV.size) {
                throw IOException("Failed to read the entire IV from the file")
            }

            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                encodedData.write(buffer, 0, bytesRead)
            }
        }

        val resultData = encodedData.toByteArray()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, readIV)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val decodedData = cipher.doFinal(resultData)
        val metadataBytes = decodedData.sliceArray(0..3)
        val fileData = decodedData.sliceArray(4 until decodedData.size)
        return Pair(fileData, metadataBytes)
    }

    fun getSecretKeyFromKeystore(fileName: String): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }

        var key = keyStore.getKey(fileName, null) as SecretKey?

        if (key == null) {
            Log.w("SecretKey", "${fileName} not found in keystore.")
            key = generateSecretKey(fileName)
        }

        return key
    }

    fun generateSecretKey(fileName: String): SecretKey {
//        val keyGenerator = KeyGenerator.getInstance("AES")
//
//        keyGenerator.init(256)
//
//        return keyGenerator.generateKey()

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")

        val keyGenSpec = KeyGenParameterSpec.Builder(fileName,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()

        keyGen.init(keyGenSpec)

        return keyGen.generateKey()
    }

    fun prepareMetadata(fileType: ItemType, isLocked: Boolean): ByteArray {
        val fileTypeBytes = fileType.typeBytes
        val isLockedByte = if (isLocked) 1 else 0
        val metadataLength = fileTypeBytes.size + 1
        val metadata = ByteArray(metadataLength)

        System.arraycopy(fileTypeBytes, 0, metadata, 0, fileTypeBytes.size)
        metadata[fileTypeBytes.size] = isLockedByte.toByte()

        return metadata
    }

    fun readMetadata(metadataArray: ByteArray): Pair<ItemType, Boolean> {
        val itemTypeBytes = metadataArray.sliceArray(0..2)
        val lockedByte = metadataArray.last()
        val isLocked = lockedByte == 1.toByte()

        val itemType = ItemType.fromByteArray(itemTypeBytes)
            ?: throw IllegalArgumentException("Item type bytes do not match any known item types.")

        return Pair(itemType, isLocked)
    }
}