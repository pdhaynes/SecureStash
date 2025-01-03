package com.example.securestash

import android.content.ClipData.Item
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.example.securestash.Adapters.DirectoryAdapter
import com.example.securestash.Adapters.DirectoryAdapterListener
import com.example.securestash.DataModels.DirectoryItem
import com.example.securestash.DataModels.ItemType
import com.example.securestash.Dialogs.DialogChangeTag
import com.example.securestash.Dialogs.DialogCreateFolder
import com.example.securestash.Helpers.Cache
import com.example.securestash.Helpers.Config
import com.example.securestash.Helpers.CryptographyHelper
import com.example.securestash.Helpers.UtilityHelper
import com.example.securestash.Interfaces.DirectoryContentLoader
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import yuku.ambilwarna.AmbilWarnaDialog
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.crypto.SecretKey

class FileDirectory : AppCompatActivity(), DirectoryContentLoader, DirectoryAdapterListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: LayoutManager
    private lateinit var directoryAdapter: DirectoryAdapter
    private val cryptoHelper: CryptographyHelper = CryptographyHelper()

    private lateinit var mainFab: ExtendedFloatingActionButton
    private lateinit var backFab: FloatingActionButton
    private lateinit var filterFab: FloatingActionButton

    private lateinit var uploadFileFab: FloatingActionButton
    private lateinit var uploadImageFab: FloatingActionButton
    private lateinit var takePhotoFab: FloatingActionButton
    private lateinit var addFolderFab: FloatingActionButton
    private lateinit var backDirectoryFab: FloatingActionButton

    private lateinit var cancelSelectionFab: FloatingActionButton
    private lateinit var trashSelectionFab: FloatingActionButton
    private lateinit var changeSelectionTagFab: FloatingActionButton
    private lateinit var moveSelectionFab: FloatingActionButton

    var userSpecifiedDirectory: File? = null
    private lateinit var currentDirectory: File

    var dataList = mutableListOf<DirectoryItem>()

    val pickMultipleMedia =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
            if (uris.isNotEmpty()) {

                // TODO()
                // Implement option to lock files.
                uris.forEach { uri ->
                    val mimeType = contentResolver.getType(uri)
                    Log.d("PhotoPicker", mimeType.toString())

                    val isVideo = mimeType?.startsWith("video") == true
                    val itemType: ItemType = if (isVideo) {
                        ItemType.VIDEO
                    } else {
                        ItemType.IMAGE
                    }

                    var imageBytes: ByteArray? = null
                    val inputStream: InputStream? = baseContext.contentResolver.openInputStream(uri)
                    inputStream?.use { stream ->
                        val outputStream = ByteArrayOutputStream()
                        val buffer = ByteArray(1024)
                        var bytesRead: Int
                        while (stream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }
                        imageBytes = outputStream.toByteArray()
                    }

                    val tempFileDirectory = File(filesDir, "Temp")
                    if (!tempFileDirectory.exists()) {
                        tempFileDirectory.mkdir()
                    }

                    val tempFile = File(tempFileDirectory, UtilityHelper.getFileNameFromUri(
                        contentResolver = baseContext.contentResolver,
                        uri = uri
                    ))

                    val combinedBytes = itemType.typeBytes + imageBytes!!
                    FileOutputStream(tempFile).use { outputStream ->
                        outputStream.write(combinedBytes)
                    }
                }

                showLoadingScreen()

            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }

    val pickMultipleDocuments = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            Log.d("PhotoPicker", "Number of items selected: ${uris.size}")

            // TODO()
            // Implement option to lock files.

            // Android's Security checks don't like when URIs are selected in this scope and then
            // passed to the loading screen scope, so I might just call
            uris.forEach { uri ->
                var imageBytes: ByteArray? = null
                val inputStream: InputStream? = baseContext.contentResolver.openInputStream(uri)
                inputStream?.use { stream ->
                    val outputStream = ByteArrayOutputStream()
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    while (stream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    imageBytes = outputStream.toByteArray()
                }

                val tempFileDirectory = File(filesDir, "Temp")
                if (!tempFileDirectory.exists()) {
                    tempFileDirectory.mkdir()
                }

                val tempFile = File(tempFileDirectory, UtilityHelper.getFileNameFromUri(
                    contentResolver = baseContext.contentResolver,
                    uri = uri
                ))

                FileOutputStream(tempFile).use { outputStream ->
                    outputStream.write(imageBytes)
                }
            }

            showLoadingScreen()

        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

    private val takePictureForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
//                loadDirectoryContents(userSpecifiedDirectory)
            }
        }

    // TODO
    // In order to improve load times, gonna make all decoding happen
    // in cache. ? I dont know if passing the image bytes in to the DirectoryItem
    // data class is the move and it might actually be hindering load times.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_directory)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val window = this.window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = this.getColor(R.color.brandeisblue)
        window.navigationBarColor = this.getColor(R.color.paledogwood)

        val specifiedDir = intent.getStringExtra("SPECIFIED_DIR")
        userSpecifiedDirectory = if (specifiedDir != null) {
            currentDirectory = File(specifiedDir)
            File(specifiedDir)
        } else {
            currentDirectory = File(filesDir, "Files")
            null
        }

        val settingsButton: MaterialButton = findViewById(R.id.settings)
        settingsButton.setOnClickListener {
            val intent = Intent(baseContext, Settings::class.java)
            startActivity(intent)
        }

        // Main Buttons
        mainFab = findViewById(R.id.main_fab)
        backFab = findViewById(R.id.back_fab)
        filterFab = findViewById(R.id.directory_filter_fab)

        // Upload Buttons
        uploadFileFab = findViewById(R.id.upload_file_fab)
        uploadImageFab = findViewById(R.id.upload_image_fab)
        takePhotoFab = findViewById(R.id.take_photo_fab)
        addFolderFab = findViewById(R.id.create_folder_fab)
        backDirectoryFab = findViewById(R.id.directory_back_fab)

        // Selection buttons
        cancelSelectionFab = findViewById(R.id.selection_cancel)
        trashSelectionFab = findViewById(R.id.selection_trash)
        changeSelectionTagFab = findViewById(R.id.selection_change_tag)
        moveSelectionFab = findViewById(R.id.selection_move)

        recyclerView = findViewById(R.id.directory_recycler_view)

        val layoutSwitch: MaterialSwitch = findViewById(R.id.layout_switch)

        if (Config.load(cacheDir).has("LAYOUT")) {
            layoutManager = when (Config.load(cacheDir).getString("LAYOUT")) {
                "LINEAR" -> {
                    directoryAdapter = DirectoryAdapter(dataList, currentDirectory, false)
                    LinearLayoutManager(this)
                }
                "GRID" -> {
                    layoutSwitch.isChecked = true
                    val columns = if (Config.load(cacheDir).has("DIR_COLUMN_COUNT")) {
                        Config.load(cacheDir).getInt("DIR_COLUMN_COUNT")
                    } else {
                        Config.update(cacheDir, "DIR_COLUMN_COUNT", "3")
                        3
                    }
                    directoryAdapter = DirectoryAdapter(dataList, currentDirectory, true)
                    GridLayoutManager(this, columns)
                }
                else -> {
                    Config.update(cacheDir, "LAYOUT", "LINEAR")
                    directoryAdapter = DirectoryAdapter(dataList, currentDirectory, false)
                    LinearLayoutManager(this)
                }
            }
        } else {
            layoutManager = LinearLayoutManager(this)
            directoryAdapter = DirectoryAdapter(dataList, currentDirectory, false)
            Config.update(cacheDir, "LAYOUT", "LINEAR")
        }

        recyclerView.layoutManager = layoutManager

        recyclerView.adapter = directoryAdapter

        layoutSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                recyclerView.layoutManager = GridLayoutManager(this, 3)
                directoryAdapter.updateLayout(true)
                Config.update(cacheDir, "LAYOUT", "GRID")
            } else {
                recyclerView.layoutManager = LinearLayoutManager(this)
                directoryAdapter.updateLayout(false)
                Config.update(cacheDir, "LAYOUT", "LINEAR")
            }
        }

        val scrollIndicator = findViewById<ImageView>(R.id.scroll_indicator)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                val itemCount = recyclerView.adapter?.itemCount ?: 0

                if (lastVisibleItemPosition < itemCount - 1) {
                    scrollIndicator.visibility = View.VISIBLE
                } else {
                    scrollIndicator.visibility = View.GONE
                }
            }
        })

        scrollIndicator.setOnClickListener {
            recyclerView.smoothScrollToPosition(recyclerView.adapter?.itemCount ?: 0)
        }

        // region Button Click Logic

        mainFab.setOnClickListener {
            hideMainButtons()
            showUploadButtons()
        }

        backFab.setOnClickListener {
            showMainButtons()
            hideUploadButtons()
        }

        uploadImageFab.setOnClickListener {
            pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        }

        uploadFileFab.setOnClickListener {
            pickMultipleDocuments.launch(arrayOf("application/pdf", "text/plain"))
        }

        takePhotoFab.setOnClickListener {
            val intent = Intent(this, TakePicture::class.java)
            intent.putExtra("ITEM_PATH", currentDirectory.toString())
            Log.d("TakePictureItemPath", currentDirectory.toString())
            takePictureForResult.launch(intent)
        }

        addFolderFab.setOnClickListener {
            val customDialog = DialogCreateFolder(
                this,
                directoryAdapter,
                currentDirectory = currentDirectory,
                loadDirContents = {
                loadDirectoryContents(currentDirectory)
            })

            customDialog.show()
        }

        cancelSelectionFab.setOnClickListener {
            directoryAdapter.disableSelectionMode()
            hideSelectionButtons()
            showMainButtons()
        }

        trashSelectionFab.setOnClickListener {
            val deleteList = directoryAdapter.getSelectedItems()
            if (deleteList.count() < 1) {
                Toast.makeText(baseContext, "Please select at least 1 item.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val builder = AlertDialog.Builder(this)

            builder.setTitle("Confirm Deletion of ${deleteList.count()} items.")

            val contentLayout = LinearLayout(this)
            contentLayout.orientation = LinearLayout.VERTICAL

            val textView = TextView(this)
            textView.text = "Are you sure you want to delete these ${deleteList.count()} items?"

            val itemListTextView = TextView(this)
            itemListTextView.textSize = 10f
            itemListTextView.text = deleteList.joinToString(separator = "\n") { item ->
                "- ${item.name}"
            }
            contentLayout.addView(textView)
            contentLayout.addView(itemListTextView)
            builder.setView(contentLayout)

            builder.setPositiveButton("Delete") { dialog, _ ->
                for (item in deleteList) {
                    val position = directoryAdapter.getItemList().indexOf(item)
                    if (position != -1) {
                        if (File(item.path).isFile) {
                            File(item.path).delete()
                        } else {
                            File(item.path).deleteRecursively()
                        }
                        UtilityHelper.removeTag(File(cacheDir, "tags.json"), File(item.path))
                        directoryAdapter.getItemList().removeAt(position) // Remove from the adapter's list
                        directoryAdapter.notifyItemRemoved(position)
                    }

                }
                directoryAdapter.disableSelectionMode()
                hideSelectionButtons()
                showMainButtons()
            }

            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }

            builder.show()
        }

        changeSelectionTagFab.setOnClickListener {
            val selectedList = directoryAdapter.getSelectedItems()
            if (selectedList.count() < 1) {
                Toast.makeText(baseContext, "Please select at least 1 item.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
           val customDialog = DialogChangeTag(
               this,
               directoryAdapter,
               loadDirContents = {
                   loadDirectoryContents(currentDirectory)
               },
               hideSelectionButtons = {
                   hideSelectionButtons()
               },
               showMainButtons = {
                   showMainButtons()
               },
           )
            customDialog.show()
        }

        if (currentDirectory == File(filesDir, "Files")) {
            backDirectoryFab.hide()
        }

        backDirectoryFab.setOnClickListener {
            finish()
        }

        // endregion

        if (LAST_ITEM_COUNT != currentDirectory.listFiles().count()) {
            loadDirectoryContents(userSpecifiedDirectory)
        }
    }

    fun showUploadButtons() {
        backFab.show()
        uploadFileFab.show()
        uploadImageFab.show()
        takePhotoFab.show()
        addFolderFab.show()
    }

    fun hideUploadButtons() {
        backFab.hide()
        uploadFileFab.hide()
        uploadImageFab.hide()
        takePhotoFab.hide()
        addFolderFab.hide()
    }

    fun showMainButtons() {
        mainFab.show()
        if (currentDirectory != File(filesDir, "Files")) {
            backDirectoryFab.show()
        }
//        filterFab.show()
    }

    fun hideMainButtons() {
        mainFab.hide()
        backDirectoryFab.hide()
//        filterFab.hide()
    }

    fun showSelectionButtons() {
        cancelSelectionFab.show()
        trashSelectionFab.show()
        changeSelectionTagFab.show()
        moveSelectionFab.show()
    }

    fun hideSelectionButtons() {
        cancelSelectionFab.hide()
        trashSelectionFab.hide()
        changeSelectionTagFab.hide()
        moveSelectionFab.hide()
    }

    override fun onEnableSelectionMode() {
        directoryAdapter.enableSelectionMode()
        hideUploadButtons()
        hideMainButtons()
        showSelectionButtons()
    }

    @Deprecated("Deprecated")
    override fun onBackPressed() {
        if (directoryAdapter.isSelectionMode) {
            directoryAdapter.disableSelectionMode()
            hideSelectionButtons()
            showMainButtons()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        directoryAdapter.disableSelectionMode()
        hideUploadButtons()
        hideSelectionButtons()
        showMainButtons()
    }

    override fun onResume() {
        super.onResume()
        if (LAST_ITEM_COUNT != currentDirectory.listFiles().count()) {
            loadDirectoryContents(currentDirectory)
        }
    }

    override fun loadDirectoryContents(selectedDirectory: File?) {
        val fileDirectory = selectedDirectory ?: File(filesDir, "Files")
        val directoryList = fileDirectory.listFiles()?.toList() ?: emptyList()

        currentDirectory = fileDirectory

        dataList.clear()
        directoryAdapter.notifyDataSetChanged()

        LAST_ITEM_COUNT = directoryList.count()

        for (item in directoryList) {
            var metadata: Pair<ItemType, Boolean>? = null
            val cachedItemType = Cache.getFileTypeFromMemory(item.path)
            val itemType = if (cachedItemType != null) {
                ItemType.fromName(cachedItemType) ?: ItemType.UNKNOWN
            } else if (item.isDirectory) {
                ItemType.DIRECTORY
            } else {
                val secretKey: SecretKey = cryptoHelper.getSecretKeyFromKeystore(item.name)
                val decodedData = cryptoHelper.decodeFile(item, secretKey)
                metadata = cryptoHelper.readMetadata(decodedData.second)
                metadata.first
            }

            val locked = metadata?.second ?: false

            val directoryItem = DirectoryItem(
                name = item.name,
                path = item.path,
                type = itemType,
                locked = locked
            )

            dataList.add(directoryItem)
            directoryAdapter.notifyItemInserted(dataList.size - 1)
        }

        val noItemsView: RelativeLayout = findViewById(R.id.no_items_view)
        val noItemsText: TextView = findViewById(R.id.no_items_text)
        if (dataList.isEmpty()) {
            noItemsText.setTextColor(Color.BLACK)
            noItemsView.visibility = View.VISIBLE
        } else {
            noItemsView.visibility = View.GONE
        }
    }

    fun showLoadingScreen() {
        val intent = Intent(this, LoadingScreen::class.java)
        intent.putExtra("SPECIFIED_DIR", currentDirectory.toString())
        intent.putExtra("LOAD_TYPE", "ENCODE")

        startActivity(intent)
        finish()
    }

    companion object {
        var LAST_ITEM_COUNT = 0
    }
}