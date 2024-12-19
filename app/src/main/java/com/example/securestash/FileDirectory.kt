package com.example.securestash

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.securestash.Adapters.DirectoryAdapter
import com.example.securestash.Adapters.DirectoryAdapterListener
import com.example.securestash.DataModels.DirectoryItem
import com.example.securestash.DataModels.ItemType
import com.example.securestash.DataModels.Tag
import com.example.securestash.Helpers.CryptographyHelper
import com.example.securestash.Helpers.UtilityHelper
import com.example.securestash.Interfaces.DirectoryContentLoader
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import yuku.ambilwarna.AmbilWarnaDialog
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.crypto.SecretKey

class FileDirectory : AppCompatActivity(), DirectoryContentLoader, DirectoryAdapterListener {

    private lateinit var recyclerView: RecyclerView
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


    var userSpecifiedDirectory: File? = null
    private lateinit var currentDirectory: File

    var dataList = mutableListOf<DirectoryItem>()

    val pickMultipleMedia =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
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

                    val tempFileDirectory: File = File(filesDir, "Temp")
                    if (!tempFileDirectory.exists()) {
                        tempFileDirectory.mkdir()
                    }

                    val tempFile: File = File(tempFileDirectory, UtilityHelper.getFileNameFromUri(
                        contentResolver = baseContext.contentResolver,
                        uri = uri
                    ))

                    FileOutputStream(tempFile).use { outputStream ->
                        outputStream.write(imageBytes)
                    }
                }

                showLoadingScreen(ItemType.IMAGE)

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

                val tempFileDirectory: File = File(filesDir, "Temp")
                if (!tempFileDirectory.exists()) {
                    tempFileDirectory.mkdir()
                }

                val tempFile: File = File(tempFileDirectory, UtilityHelper.getFileNameFromUri(
                    contentResolver = baseContext.contentResolver,
                    uri = uri
                ))

                FileOutputStream(tempFile).use { outputStream ->
                    outputStream.write(imageBytes)
                }
            }

            showLoadingScreen(ItemType.DOCUMENT)

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
        window.navigationBarColor = this.resources.getColor(R.color.paledogwood)

        val specifiedDir = intent.getStringExtra("SPECIFIED_DIR")
        userSpecifiedDirectory = if (specifiedDir != null) {
            currentDirectory = File(specifiedDir)
            File(specifiedDir)
        } else {
            currentDirectory = File(filesDir, "Files")
            null
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

        recyclerView = findViewById(R.id.directory_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        directoryAdapter = DirectoryAdapter(dataList, currentDirectory)
        recyclerView.adapter = directoryAdapter

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
            Toast.makeText(this, "Clicked add folder", Toast.LENGTH_SHORT).show()
            showDialog()
        }

        cancelSelectionFab.setOnClickListener {
            directoryAdapter.disableSelectionMode()
            hideSelectionButtons()
            showMainButtons()
        }

        trashSelectionFab.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val deleteList = directoryAdapter.getSelectedItems()

            builder.setTitle("Confirm Deletion of ${deleteList.count()} items.")

            val contentLayout = LinearLayout(this)
            contentLayout.orientation = LinearLayout.VERTICAL

            val textView: TextView = TextView(this)
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
                        File(item.path).delete()
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

            val builder = AlertDialog.Builder(this)

            builder.setTitle("Change Tags for ${selectedList.count()} items.")

            val userTagName = EditText(this).apply {
                hint = "Tag name"
                inputType = InputType.TYPE_CLASS_TEXT
            }

            var selectedColor = Color.RED

            val buttonLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(0, 20, 0, 0)
            }

            val colorPickerButton = MaterialButton(this).apply {
                text = "Choose Tag Color"
                height = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    48f,
                    resources.displayMetrics
                ).toInt()
            }

            val colorSquare = View(this).apply {
                val squareSize = resources.getDimensionPixelSize(R.dimen.color_square_size)
                layoutParams = LinearLayout.LayoutParams(squareSize, squareSize).apply {
                    setMargins(20, 0, 0, 0)
                }
                setBackgroundColor(selectedColor)
            }

            val tagPreviewLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
            }

            val previewTitle: TextView = TextView(this).apply {
                text = "Tag preview:"
            }

            val previewTagLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(2, 2, 2, 2)
                }
                setPadding(2, 2, 2, 2)
            }

            val previewTag = TextView(this).apply {
                text = "Preview"
                textSize = 16f

                setTextColor(UtilityHelper.getTextColorForBackground(selectedColor))
                ellipsize = TextUtils.TruncateAt.END
                isSingleLine = true

                val padding = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    6f,
                    resources.displayMetrics
                ).toInt()
                setPadding(padding, padding, padding, padding)

                val drawable = ContextCompat.getDrawable(context, R.drawable.rounded_background)?.mutate()
                drawable?.setTint(selectedColor)
                background = drawable
            }

            userTagName.doOnTextChanged { text, start, before, count ->
                if (count == 0) {
                    previewTag.text = "Preview"
                } else {
                    previewTag.text = text
                }
            }

            colorPickerButton.setOnClickListener {
                val colorDialog = AmbilWarnaDialog(this, selectedColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
                    override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                        selectedColor = color
                        colorSquare.setBackgroundColor(color)
                        previewTag.background.setTint(color)
                        previewTag.setTextColor(UtilityHelper.getTextColorForBackground(color))
                    }

                    override fun onCancel(dialog: AmbilWarnaDialog?) {}
                })
                colorDialog.show()
            }

            buttonLayout.addView(colorPickerButton)
            buttonLayout.addView(colorSquare)

            val recentTagsLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
            }

            val recentTitle: TextView = TextView(this).apply {
                text = "Recent tags:"
            }

            recentTagsLayout.addView(recentTitle)

            tagPreviewLayout.addView(previewTitle)
            previewTagLayout.addView(previewTag)
            tagPreviewLayout.addView(previewTagLayout)

            val listOfRecentTags: List<Tag> = UtilityHelper.getMostRecentTags(File(cacheDir, "tags.json"))
            listOfRecentTags.forEach { tag ->
                val singleTagLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(2, 2, 2, 2)
                    }
                    setPadding(2, 2, 2, 2)
                }

                val tagName = TextView(this).apply {
                    text = tag.Name
                    textSize = 16f

                    setTextColor(UtilityHelper.getTextColorForBackground(tag.Color))
                    ellipsize = TextUtils.TruncateAt.END
                    isSingleLine = true

                    val padding = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        6f,
                        resources.displayMetrics
                    ).toInt()
                    setPadding(padding, padding, padding, padding)

                    val drawable = ContextCompat.getDrawable(context, R.drawable.rounded_background)?.mutate()
                    drawable?.setTint(tag.Color)
                    background = drawable
                }

                singleTagLayout.addView(tagName)
                recentTagsLayout.addView(singleTagLayout)

                tagName.setOnClickListener {
                    userTagName.setText(tag.Name)
                    selectedColor = tag.Color
                    previewTag.text = tag.Name
                    previewTag.background.setTint(tag.Color)
                    colorSquare.setBackgroundColor(tag.Color)
                }
            }

            val tagLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(recentTagsLayout, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(tagPreviewLayout, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            }

            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 20, 50, 20)
                addView(userTagName)
                addView(tagLayout)
                addView(buttonLayout)
            }

            builder.setView(layout)

            builder.setPositiveButton("OK") { dialog, _ ->
                val tagName = userTagName.text.toString()
                if (tagName.isNotEmpty()) {
                    val tagFile: File = File(cacheDir, "tags.json")
                    for (item in selectedList) {
                        UtilityHelper.addOrUpdateTagForDirectory(tagFile, File(item.path), selectedColor, tagName)
                    }
                    selectedList.clear()
                    loadDirectoryContents(userSpecifiedDirectory)
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Tag name cannot be empty!", Toast.LENGTH_SHORT).show()
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

        if (currentDirectory == File(filesDir, "Files")) {
            backDirectoryFab.hide()
        }

        backDirectoryFab.setOnClickListener {
            finish()
        }

        // endregion

        loadDirectoryContents(userSpecifiedDirectory)
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
        filterFab.show()
    }

    fun hideMainButtons() {
        mainFab.hide()
        backDirectoryFab.hide()
        filterFab.hide()
    }

    fun showSelectionButtons() {
        cancelSelectionFab.show()
        trashSelectionFab.show()
        changeSelectionTagFab.show()
    }

    fun hideSelectionButtons() {
        cancelSelectionFab.hide()
        trashSelectionFab.hide()
        changeSelectionTagFab.hide()
    }

    override fun onEnableSelectionMode() {
        directoryAdapter.enableSelectionMode()
        hideUploadButtons()
        hideMainButtons()
        showSelectionButtons()
    }

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

    private fun showDialog() {
        val builder = AlertDialog.Builder(this)

        builder.setTitle("Folder Name")

        val userFolderName = EditText(this)
        userFolderName.hint = "Folder name"
        userFolderName.inputType = InputType.TYPE_CLASS_TEXT

        val userTagName = EditText(this)
        userTagName.hint = "Tag name"
        userTagName.inputType = InputType.TYPE_CLASS_TEXT


        var selectedColor = Color.WHITE

        val buttonLayout = LinearLayout(this)
        buttonLayout.orientation = LinearLayout.HORIZONTAL
        buttonLayout.setPadding(0, 20, 0, 0)

        val colorPickerButton = Button(this)
        colorPickerButton.text = "Choose Tag Color"

        val colorSquare = View(this)
        val squareSize = resources.getDimensionPixelSize(R.dimen.color_square_size)
        val squareParams = LinearLayout.LayoutParams(squareSize, squareSize)
        squareParams.setMargins(20, 0, 0, 0)
        colorSquare.layoutParams = squareParams
        colorSquare.setBackgroundColor(selectedColor)

        colorPickerButton.setOnClickListener {
            val colorDialog = AmbilWarnaDialog(this, selectedColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
                override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                    selectedColor = color
                    colorSquare.setBackgroundColor(color)
                }

                override fun onCancel(dialog: AmbilWarnaDialog?) {
                }
            })
            colorDialog.show()
        }

        buttonLayout.addView(colorPickerButton)
        buttonLayout.addView(colorSquare)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 20)
        layout.addView(userFolderName)
        layout.addView(userTagName)
        layout.addView(buttonLayout)

        builder.setView(layout)

        builder.setPositiveButton("OK") { dialog, _ ->
            val inputText = userFolderName.text.toString()
            val tagName = userTagName.text.toString()
            if (inputText.isNotEmpty()) {
                val newDir = File(currentDirectory, inputText)
                newDir.mkdir()

                if (tagName.isNotEmpty()) {
                    val tagFile: File = File(cacheDir, "tags.json")
                    UtilityHelper.addOrUpdateTagForDirectory(tagFile, newDir, selectedColor, tagName)
                }

                loadDirectoryContents(userSpecifiedDirectory)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Folder name cannot be empty!", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    override fun loadDirectoryContents(selectedDirectory: File?) {
        val fileDirectory = selectedDirectory ?: File(filesDir, "Files")
        val directoryList = fileDirectory.listFiles()?.toList() ?: emptyList()

        currentDirectory = fileDirectory

        dataList.clear()

        for (item in directoryList) {

            var metadata: Pair<ItemType, Boolean>? = null
            val type: ItemType = if (item.isDirectory) {
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
                type = type,
                locked = locked
            )

            dataList.add(directoryItem)
        }

        val noItemsView: RelativeLayout = findViewById(R.id.no_items_view)
        val noItemsText: TextView = findViewById(R.id.no_items_text)
        if (dataList.isEmpty()) {
            noItemsText.setTextColor(Color.BLACK)
            noItemsView.visibility = View.VISIBLE
        } else {
            noItemsView.visibility = View.GONE
        }

        directoryAdapter.notifyDataSetChanged()
    }

    fun showLoadingScreen(itemType: ItemType) {
        val intent = Intent(this, LoadingScreen::class.java)
        intent.putExtra("SPECIFIED_DIR", currentDirectory.toString())
        intent.putExtra("ITEM_TYPE", itemType.name)

        startActivity(intent)
        finish()
    }
}