package com.example.securestash.Helpers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.collection.LruCache
import java.io.File
import javax.crypto.SecretKey

object Cache {
    val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    val cacheSize = maxMemory / 5

    var bitmapCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(cacheSize) {

        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }

    }

    val fileTypeCache = mutableMapOf<String, String>()

    fun saveFileTypeInMemory(filePath: String, fileType: String) {
        fileTypeCache[filePath] = fileType
    }

    fun getFileTypeFromMemory(filePath: String): String? {
        return fileTypeCache[filePath]
    }
    fun retrieveCachedBitmap(key: String): Bitmap? {
        return bitmapCache.get(key) ?: null
    }

    fun buildCache(appFiles: List<File>) {
        for (file in appFiles) {
            val fileType = getFileTypeFromMemory(file.toString())
            val cachedBitmap = retrieveCachedBitmap(file.toString())
            if (fileType == null || cachedBitmap == null) {
                val cryptoHelper = CryptographyHelper()
                val secretKey: SecretKey = cryptoHelper.getSecretKeyFromKeystore(file.name.toString())
                val decodedBytes = cryptoHelper.decodeFile(file, secretKey)
                val decodedFile = decodedBytes.first
                val bitmap = BitmapFactory.decodeByteArray(decodedFile, 0, decodedFile.size)
                if (bitmap != null) {
                    bitmapCache.put(file.toString(), bitmap)
                } else {
                    Log.d("CacheBuilder", "${file.name}'s Bitmap is null or corrupt.")
                }
                val metadata = decodedBytes.second
                val itemType = cryptoHelper.readMetadata(metadata).first
                saveFileTypeInMemory(file.toString(), itemType.toString())
            }
        }
    }
}