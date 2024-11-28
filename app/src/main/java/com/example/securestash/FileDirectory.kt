package com.example.securestash

import android.content.ClipData.Item
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.securestash.Adapters.DirectoryAdapter
import com.example.securestash.DataModels.DirectoryItem
import com.example.securestash.DataModels.ItemType
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDragHandleView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.FileOutputStream

class FileDirectory : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var directoryAdapter: DirectoryAdapter

    var dataList = mutableListOf<DirectoryItem>()

    val pickMultipleMedia =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
            if (uris.isNotEmpty()) {
                Log.d("PhotoPicker", "Number of items selected: ${uris.size}")

                uris.forEach { uri ->
                    copyFileToAppDirectory(uri)
                }
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }

    val pickMultipleDocuments = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris?.forEach { uri ->
            Log.d("Picked Document", uri.toString())
            copyFileToAppDirectory(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContentView(R.layout.activity_file_directory)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = this.resources.getColor(R.color.brandeisblue)
        window.navigationBarColor = this.resources.getColor(R.color.rosybrown)

        val mainFab: ExtendedFloatingActionButton = findViewById(R.id.main_fab)
        val backFab: FloatingActionButton = findViewById(R.id.back_fab)
        val uploadFileFab: FloatingActionButton = findViewById(R.id.upload_file_fab)
        val uploadImageFab: FloatingActionButton = findViewById(R.id.upload_image_fab)
        val takePhotoFab: FloatingActionButton = findViewById(R.id.take_photo_fab)

        // region Button Click Logic

        mainFab.setOnClickListener {
            mainFab.hide()
            backFab.show()
            uploadFileFab.show()
            uploadImageFab.show()
            takePhotoFab.show()
        }

        backFab.setOnClickListener {
            mainFab.show()
            backFab.hide()
            uploadFileFab.hide()
            uploadImageFab.hide()
            takePhotoFab.hide()
        }

        uploadImageFab.setOnClickListener {
            pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        }

        uploadFileFab.setOnClickListener {
            pickMultipleDocuments.launch(arrayOf("application/pdf", "text/plain"))
        }

        // endregion

        recyclerView = findViewById(R.id.directory_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        directoryAdapter = DirectoryAdapter(dataList)
        recyclerView.adapter = directoryAdapter

        loadDirectoryContents()
    }



    private fun getFileNameFromUri(uri: Uri): String {
        var fileName = "file_${System.currentTimeMillis()}"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                fileName = it.getString(nameIndex)
            }
        }
        return fileName
    }


    private fun copyFileToAppDirectory(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)

            if (inputStream != null) {
                val fileName = getFileNameFromUri(uri)

                val outputFile = File(filesDir, fileName)

                val outputStream = FileOutputStream(outputFile)

                inputStream.copyTo(outputStream)

                inputStream.close()
                outputStream.close()

                Log.d("FileCopy", "File copied to: ${outputFile.absolutePath}")
                loadDirectoryContents()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("FileCopyError", "Error copying file: ${e.message}")
        }
    }

    private fun loadDirectoryContents() {
        val directoryList = filesDir.listFiles()?.toList() ?: emptyList()

        dataList.clear()

        for (item in directoryList) {
            // TODO
            // Implement proprietary file storage to check item type
            val type: ItemType
            if (item.isDirectory) {
                type = ItemType.DIRECTORY
            } else {
                type = when (item.name.split(".").last()) {
                    "png" -> ItemType.IMAGE
                    "jpg" -> ItemType.IMAGE
                    "jpeg" -> ItemType.IMAGE
                    "pdf" -> ItemType.DOCUMENT
                    else -> ItemType.DOCUMENT
                }
            }

            // TODO
            // Implement way to check if file is locked
            val locked = false

            val directoryItem = DirectoryItem(
                name = item.name,
                path = item.path,
                type = type,
                locked = locked
            )

            dataList.add(directoryItem)
        }

        directoryAdapter.notifyDataSetChanged()
    }
}