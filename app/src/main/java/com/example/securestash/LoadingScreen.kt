package com.example.securestash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.securestash.DataModels.ItemType
import com.example.securestash.Helpers.UtilityHelper
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.io.File


class LoadingScreen : AppCompatActivity() {

    private var itemsProgress = 0
    private lateinit var loadingBar: CircularProgressIndicator
    private lateinit var specifiedDirectory: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_loading_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loading_page)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val fileType: ItemType

        val extras = intent.extras
        if (extras != null) {
            val fileParentDirectory = extras.getString("SPECIFIED_DIR", "null")
            if (!fileParentDirectory.equals("null")) {
                specifiedDirectory = File(fileParentDirectory)
            } else {
                specifiedDirectory = File(filesDir, "Files")
            }

            val itemTypeName = extras.getString("ITEM_TYPE", "null")
            val itemType = ItemType.fromName(itemTypeName)
            if (itemType != null) {
                fileType = ItemType.fromName(itemTypeName)!!
            } else {
                fileType = ItemType.DOCUMENT
            }
        } else {
            throw Exception("Intent extras not provided.")
        }

        val targetDirectory = File(filesDir, "Temp").listFiles()

        loadingBar = findViewById(R.id.loading_spinner)
        loadingBar.max = targetDirectory!!.count()

        for (file in targetDirectory) {
            UtilityHelper.queueFileEncodingTask(
                uri = Uri.fromFile(file),
                context = this,
                fileType = fileType,
                isLocked = false,
                targetDirectory = specifiedDirectory,
                loadingScreen = this,
                fileName = file.name)
        }
    }

    fun addProgressToLoadingIndicator(addAmount: Int) {
        runOnUiThread {
            itemsProgress += addAmount
            loadingBar.setProgress(itemsProgress, true)
        }
        if (itemsProgress == loadingBar.max) {
            val tempDirectory = File(filesDir, "Temp").listFiles()
            for (file in tempDirectory) {
                if (file.delete()) {
                    Log.d("TempDirCleanup", "Deleted" + file.name + " from temp.")
                } else {
                    Log.w("TempDirCleanup", "Failed to delete " + file.name + "from temp.")
                }
            }
            val intent = Intent(this, FileDirectory::class.java)
            intent.putExtra("SPECIFIED_DIR", specifiedDirectory.toString())
            startActivity(intent)
            finish()
        }
    }
}