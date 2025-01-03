package com.ph.securestash

import android.app.ActivityManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ph.securestash.DataModels.ItemType
import com.ph.securestash.Helpers.UtilityHelper
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.io.File


class LoadingScreen : AppCompatActivity() {

    private var itemsProgress = 0
    private lateinit var loadingBar: CircularProgressIndicator
    private lateinit var specifiedDirectory: File
    private lateinit var loadingType: String
    private var accountDeleted: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_loading_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loading_page)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loadingBar = findViewById(R.id.loading_spinner)

        val extras = intent.extras ?: return

        val loadType = extras.getString("LOAD_TYPE")
        accountDeleted = extras.getBoolean("ACCOUNT_DELETED")
        when (loadType) {
            "DELETE" -> {
                loadingType = "DELETE"
                loadDeleteScreen(extras)
            }
            "ENCODE" -> {
                loadingType = "ENCODE"
                loadEncodingScreen(extras)
            }
        }
    }

    fun addProgressToLoadingIndicator(addAmount: Int) {
        if (!::loadingBar.isInitialized) {
            Log.e("LoadingScreen", "Loading bar not initialized.")
            return
        }

        runOnUiThread {
            itemsProgress += addAmount
            loadingBar.setProgress(itemsProgress, false)
        }

        if (itemsProgress == loadingBar.max) {
            val tempDirectory = File(filesDir, "Temp").listFiles()
            if (tempDirectory == null || tempDirectory.isEmpty()) {
                // Do nothing!
            } else {
                for (file in tempDirectory) {
                    file.delete()
//                    if (file.delete()) {
//                        Log.d("TempDirCleanup", "Deleted" + file.name + " from temp.")
//                    } else {
//                        Log.w("TempDirCleanup", "Failed to delete " + file.name + "from temp.")
//                    }
                }
            }

            when (loadingType) {
                "DELETE" -> {
                    val fileDirectory = File(filesDir, "Files")
                    if (!fileDirectory.exists()) {
                        fileDirectory.mkdir()
                    }
                    val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                    val tasks = activityManager.appTasks

                    for (appTask in tasks) {
                        val taskInfo = appTask.taskInfo
                        if (taskInfo.baseIntent.component!!.className == "com.ph.securestash.MainActivity") {
                            appTask.finishAndRemoveTask()
                        }
                    }

                    val intent: Intent
                    if (accountDeleted) {
                        intent = Intent(this, MainActivity::class.java)
                    } else {
                        intent = Intent(this, FileDirectory::class.java)
                    }
                    startActivity(intent)
                    finish()
                }
                "ENCODE" -> {
                    val intent = Intent(this, FileDirectory::class.java)
                    intent.putExtra("SPECIFIED_DIR", specifiedDirectory.toString())
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    fun loadEncodingScreen(extras: Bundle) {
        val fileParentDirectory = extras.getString("SPECIFIED_DIR", "null")
        if (!fileParentDirectory.equals("null")) {
            specifiedDirectory = File(fileParentDirectory)
        } else {
            specifiedDirectory = File(filesDir, "Files")
        }

        val targetDirectory = File(filesDir, "Temp").listFiles()

        loadingBar.max = targetDirectory!!.count()

        for (file in targetDirectory) {
            Log.d("ENCODINGSCREEN", file.toString())
            val fileBytes = file.readBytes()
            val typeBytes = fileBytes.sliceArray(0..2)
            val contentBytes = fileBytes.sliceArray(3 until fileBytes.size)
            file.writeBytes(contentBytes)

            Log.d("ENCODINGSCREEN", ItemType.fromByteArray(typeBytes).toString())

            UtilityHelper.queueFileEncodingTask(
                uri = Uri.fromFile(file),
                context = this,
                fileType = ItemType.fromByteArray(typeBytes),
                isLocked = false,
                targetDirectory = specifiedDirectory,
                loadingScreen = this,
                fileName = file.name)
        }
    }

    fun loadDeleteScreen(extras: Bundle) {
        val targetDeletionDirectory = extras.getString("SPECIFIED_DIR", "null")
        if (targetDeletionDirectory == "null") {
            return
        }

        val deleteList = UtilityHelper.recursivelyGrabFileList(File(targetDeletionDirectory))
        loadingBar.max = deleteList.count()

        for (file in deleteList) {
            UtilityHelper.queueFileDeletionTask(
                fileToDelete = file,
                context = baseContext,
                loadingScreen = this
            )
        }
    }
}