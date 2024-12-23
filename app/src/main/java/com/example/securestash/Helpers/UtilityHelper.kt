package com.example.securestash.Helpers

import android.content.ContentResolver
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.securestash.DataModels.ItemType
import com.example.securestash.DataModels.Tag
import com.example.securestash.LoadingScreen
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
            try {
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
            } catch (e: Exception) {
                Log.e("URIException", e.message.toString())
                "unknown_file_${System.currentTimeMillis()}"
            }

        } else if (uri.scheme == "file") {
            filename = File(uri.path).name
        }

        return filename ?: "unknown_file_${System.currentTimeMillis()}"
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

    fun queueFileEncodingTask(uri: Uri, context: Context, fileType: ItemType, isLocked: Boolean, targetDirectory: File, loadingScreen: LoadingScreen, fileName: String) {
        val inputData = workDataOf(
            "uri" to uri.toString(),
            "fileType" to fileType.name,
            "isLocked" to isLocked,
            "targetDirectory" to targetDirectory.absolutePath,
            "fileName" to fileName
        )

        val fileEncodingWorkRequest = OneTimeWorkRequestBuilder<FileEncodingWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(fileEncodingWorkRequest)

        WorkManager.getInstance(context)
            .getWorkInfoByIdLiveData(fileEncodingWorkRequest.id)
            .observeForever { workInfo ->
                if (workInfo != null && workInfo.state.isFinished) {
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        val filePath = workInfo.outputData.getString("outputFilePath")
                        val tempFilePath = workInfo.outputData.getString("originalFilePath")
                        if (filePath != null) {
                            val targetDir: File = File(filePath.split("/").dropLast(1).joinToString("/"))
                            loadingScreen.addProgressToLoadingIndicator(1)
                        }
                        if (tempFilePath != null) {
                            Log.d("DELETION", "Deleting file " + tempFilePath)
                            Log.d("DELETION", File(tempFilePath).delete().toString())
                        }
                    } else if (workInfo.state == WorkInfo.State.FAILED) {
                        loadingScreen.addProgressToLoadingIndicator(1)
                    }
                }
            }
    }

    fun queueFileDeletionTask(fileToDelete: File, context: Context, loadingScreen: LoadingScreen) {
        val inputData = Data.Builder()
            .putString("FILE", fileToDelete.toString())
            .build()

        val fileDeletionWorkRequest = OneTimeWorkRequestBuilder<FileDeletionWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(fileDeletionWorkRequest)

        WorkManager.getInstance(context)
            .getWorkInfoByIdLiveData(fileDeletionWorkRequest.id)
            .observeForever { workInfo ->
                if (workInfo != null && workInfo.state.isFinished) {
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        loadingScreen.addProgressToLoadingIndicator(1)
                    } else if (workInfo.state == WorkInfo.State.FAILED) {
                        loadingScreen.addProgressToLoadingIndicator(1)
                    }
                }
            }
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

    fun removeTag(tagFile: File, targetDirectoryPath: File) {
        val tagList = if (tagFile.exists()) {
            val content = tagFile.readText()
            if (content.isNotBlank()) JSONObject(content) else JSONObject()
        } else {
            JSONObject()
        }

        if (tagList.has(targetDirectoryPath.toString())) {
            tagList.remove(targetDirectoryPath.toString())
        }

        tagFile.writeText(tagList.toString())
    }

    fun getMostRecentTags(tagFile: File): List<Tag> {
        val jsonTagList = if (tagFile.exists()) {
            val content = tagFile.readText()
            if (content.isNotBlank()) JSONObject(content) else JSONObject()
        } else {
            JSONObject()
        }

        val tagList = mutableListOf<Tag>()
        for (tagFilePath in jsonTagList.keys()) {
            if (tagList.count() >= 5) {
                return tagList
            }

            val tagObject: JSONObject = jsonTagList[tagFilePath] as JSONObject
            val comparableTag: Tag = Tag(
                Name = tagObject["tag"].toString(),
                Color = tagObject["color"] as Int
            )

            if (!tagList.contains(comparableTag)) {
                tagList.add(comparableTag)
            }
        }

        return tagList
    }

    fun getLuminance(color: Int): Double {
        val red = Color.red(color) / 255.0
        val green = Color.green(color) / 255.0
        val blue = Color.blue(color) / 255.0

        return 0.2126 * red + 0.7152 * green + 0.0722 * blue
    }

    fun getTextColorForBackground(backgroundColor: Int): Int {
        return if (getLuminance(backgroundColor) > 0.5) {
            Color.BLACK
        } else {
            Color.WHITE
        }
    }

    fun recursivelyGrabFileList(root: File): List<File> {
        val fileList = mutableListOf<File>()

        fun traverse(directory: File) {
            if (directory.isDirectory) {
                directory.listFiles()?.forEach { file ->
                    fileList.add(file)
                    if (file.isDirectory) {
                        traverse(file)
                    }
                }
            }
        }

        if (root.exists() && root.isDirectory) {
            traverse(root)
        }

        return fileList
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
            val context = applicationContext
            val fileType = ItemType.valueOf(inputData.getString("fileType") ?: "DEFAULT") // Ensure to pass fileType properly
            val isLocked = inputData.getBoolean("isLocked", false)
            val targetDirectory = File(inputData.getString("targetDirectory") ?: "")
            val fileName = inputData.getString("fileName") ?: "unknown_filename"

            if (uri != null) {
                val newFileName = "${fileName.split(".").first()}_${System.currentTimeMillis()}"

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

                val outputData = workDataOf(
                    "outputFilePath" to outputFile.absolutePath,
                    "originalFilePath" to uri.toString()
                )

                return Result.success(outputData)
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

class FileDeletionWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    override fun doWork(): Result {
        try {
            val fileToDelete = File(inputData.getString("FILE"))
            if (fileToDelete.isFile) {
                fileToDelete.delete()
            } else {
                fileToDelete.deleteRecursively()
            }
            return Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("FileEncodingWorker", "Error encoding file: ${e.message}")
            return Result.failure()
        }
    }
}
