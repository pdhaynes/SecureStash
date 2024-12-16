package com.example.securestash.Helpers

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.securestash.DataModels.ItemType
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object UtilityHelper {
    private val cryptoHelper: CryptographyHelper = CryptographyHelper()

    fun getFileNameFromUri(contentResolver: ContentResolver, uri: Uri): String {
        var filename: String? = null

        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DISPLAY_NAME), null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val target = it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                    filename = if (target != -1) {
                        it.getString(target)
                    } else {
                        "file_name"
                    }
                }
            }
        } else if (uri.scheme == "file") {
            filename = File(uri.path).name
        }

        return filename ?: "file_name"
    }

    fun renameDuplicateFile(file: File): File {
        val dateFormat = SimpleDateFormat("MMddyyhhmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        val newName = File(file.nameWithoutExtension + timestamp + "." + file.extension)
        Log.d("RDF", "Renamed ${file.name} to ${newName}")
        return newName
    }

    fun copyFileToAppDirectory(uri: Uri, contentResolver: ContentResolver, context: Context, fileType: ItemType, isLocked: Boolean, targetDirectory: File) {
        try {
            val originalFileName = getFileNameFromUri(contentResolver, uri)

            val newFileName = "${originalFileName.split(".").first()}_${System.currentTimeMillis()}"  // Adjust the extension accordingly

            val outputFile = File(targetDirectory, newFileName)

            val secretKey = cryptoHelper.generateSecretKey(newFileName)
            
            val encryptResult = cryptoHelper.encodeFile(
                context = context,
                fileUri = uri,
                fileKey = secretKey,
                fileType = fileType,
                isLocked = isLocked
            )

            cryptoHelper.saveEncodedFile(
                file = outputFile,
                fileData = encryptResult
            )

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("FileCopyError", "Error copying file: ${e.message}")
        }
    }

    fun queueFileEncodingTask(uri: Uri, context: Context, fileType: ItemType, isLocked: Boolean, targetDirectory: File) {
        val inputData = workDataOf(
            "uri" to uri.toString(),
            "fileType" to fileType.name,
            "isLocked" to isLocked,
            "targetDirectory" to targetDirectory.absolutePath
        )

        val fileEncodingWorkRequest = OneTimeWorkRequestBuilder<FileEncodingWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(fileEncodingWorkRequest)
    }

    fun addOrUpdateTagForDirectory(tagFile: File, targetDirectoryPath: File, color: Int, tagName: String) {
        try {
            val tagList = if (tagFile.exists()) {
                val content = tagFile.readText()
                if (content.isNotBlank()) JSONObject(content) else JSONObject()
            } else {
                JSONObject()
            }

            val tagObj = JSONObject().apply {
                put("tag", tagName)
                put("color", color)
            }

            tagList.put(targetDirectoryPath.toString(), tagObj)

            tagFile.writeText(tagList.toString())
        } catch (e: Exception) {
            Log.e("TagHandler", "Error updating tag file: ${e.message}", e)
        }
    }
}

class FileEncodingWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    private val cryptoHelper = CryptographyHelper()

    override fun doWork(): Result {
        try {
            val uri = inputData.getString("uri")?.let { Uri.parse(it) }
            val contentResolver = applicationContext.contentResolver
            val context = applicationContext
            val fileType = ItemType.valueOf(inputData.getString("fileType") ?: "DEFAULT") // Ensure to pass fileType properly
            val isLocked = inputData.getBoolean("isLocked", false)
            val targetDirectory = File(inputData.getString("targetDirectory") ?: "")

            if (uri != null) {
                val originalFileName = UtilityHelper.getFileNameFromUri(contentResolver, uri)
                val newFileName = "${originalFileName.split(".").first()}_${System.currentTimeMillis()}"

                val outputFile = File(targetDirectory, newFileName)

                val secretKey = cryptoHelper.generateSecretKey(newFileName)
                val encryptResult = cryptoHelper.encodeFile(
                    context = context,
                    fileUri = uri,
                    fileKey = secretKey,
                    fileType = fileType,
                    isLocked = isLocked
                )

                cryptoHelper.saveEncodedFile(
                    file = outputFile,
                    fileData = encryptResult
                )

                return Result.success()
            } else {
                return Result.failure()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("FileEncodingWorker", "Error encoding file: ${e.message}")
            return Result.failure()
        }
    }
}
