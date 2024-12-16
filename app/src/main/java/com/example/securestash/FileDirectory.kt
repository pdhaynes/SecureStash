package com.example.securestash

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.securestash.Adapters.DirectoryAdapter
import com.example.securestash.Adapters.DirectoryAdapterListener
import com.example.securestash.DataModels.DirectoryItem
import com.example.securestash.DataModels.ItemType
import com.example.securestash.Helpers.CryptographyHelper
import com.example.securestash.Helpers.UtilityHelper
import com.example.securestash.Interfaces.DirectoryContentLoader
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Text
import yuku.ambilwarna.AmbilWarnaDialog
import java.io.File
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
                uris.forEach { uri ->
//                    UtilityHelper.copyFileToAppDirectory(
//                        uri,
//                        contentResolver = contentResolver,
//                        context = this,
//                        fileType = ItemType.IMAGE,
//                        isLocked = false,
//                        targetDirectory = currentDirectory
//                    )
                    UtilityHelper.queueFileEncodingTask(
                        uri,
                        context = this,
                        fileType = ItemType.DOCUMENT,
                        isLocked = false,
                        targetDirectory = currentDirectory
                    )
                }
                loadDirectoryContents(userSpecifiedDirectory)
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }

    val pickMultipleDocuments = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
//            UtilityHelper.copyFileToAppDirectory(
//                uri,
//                contentResolver = contentResolver,
//                context = this,
//                fileType = ItemType.DOCUMENT,
//                isLocked = false,
//                targetDirectory = currentDirectory
//            )
            UtilityHelper.queueFileEncodingTask(
                uri,
                context = this,
                fileType = ItemType.DOCUMENT,
                isLocked = false,
                targetDirectory = currentDirectory
            )
        }
        loadDirectoryContents(userSpecifiedDirectory)
    }

    private val takePictureForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                loadDirectoryContents(userSpecifiedDirectory)
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
//                loadDirectoryContents(currentDirectory)
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
            layout.addView(userTagName)
            layout.addView(buttonLayout)

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
}