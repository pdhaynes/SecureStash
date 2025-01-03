package com.example.securestash.Dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.securestash.Adapters.DirectoryAdapter
import com.example.securestash.Adapters.TagAdapter
import com.example.securestash.Helpers.UtilityHelper
import com.example.securestash.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import yuku.ambilwarna.AmbilWarnaDialog
import java.io.File

class DialogCreateFolder(
    context: Context,
    directoryAdapter: DirectoryAdapter,
    private val loadDirContents: () -> Unit,
    private val currentDirectory: File
) : Dialog(context), View.OnClickListener {
    private lateinit var accept: MaterialButton
    private lateinit var cancel: MaterialButton

    private var newTag: String = ""
    private var userInputFolderName: String = ""

    private var dirAdapter = directoryAdapter
    private var selectedColor = Color.BLUE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_create_folder)

        accept = findViewById(R.id.accept_button)
        cancel = findViewById(R.id.cancel_button)
        accept.setOnClickListener(this)
        cancel.setOnClickListener(this)

        val titleTag: TextView = findViewById(R.id.tag_change_title)
        titleTag.text = "Create Folder"

        val tagFile = File(context.cacheDir, "tags.json")
        val tagList = UtilityHelper.getMostRecentTags(tagFile)

        val previewTag: TextView = findViewById(R.id.preview_tag)
//        previewTag.background.setTint(selectedColor)
//        previewTag.setTextColor(UtilityHelper.getTextColorForBackground(selectedColor))
        val newTagInput: TextInputEditText = findViewById(R.id.user_tag_input)
        newTagInput.doOnTextChanged { text, start, before, count ->
            if (text?.count()!! < 1) {
                previewTag.text = "No tag"
                previewTag.background.setTint(Color.WHITE)
                previewTag.setTextColor(UtilityHelper.getTextColorForBackground(Color.WHITE))
                newTag = ""
            } else {
                previewTag.text = text
                previewTag.background.setTint(selectedColor)
                previewTag.setTextColor(UtilityHelper.getTextColorForBackground(selectedColor))
                newTag = text.toString()
            }
        }

        val newFolderInput: TextInputEditText = findViewById(R.id.user_folder_input)
        newFolderInput.doOnTextChanged { text, start, before, count ->
            if (count > 0) {
                userInputFolderName = text.toString()
            }
        }

        // TODO
        // Check to see if a file path has a tag already, and set all of these items to that tag's
        // information.
        val colorSquare: View = findViewById(R.id.color_square)
        colorSquare.setBackgroundColor(selectedColor) // Blue by default.
        colorSquare.setOnClickListener {
            // Color Picker
            Toast.makeText(context, "Clicked color square", Toast.LENGTH_SHORT).show()
            val colorDialog = AmbilWarnaDialog(context, selectedColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
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

        val lvTags: RecyclerView = findViewById(R.id.tag_list)
        val tagAdapter = TagAdapter(
            tagList,
            updatePreviewTag = { selectedTag ->
                previewTag.text = selectedTag.Name
                previewTag.background.setTint(selectedTag.Color)
                previewTag.setTextColor(UtilityHelper.getTextColorForBackground(selectedTag.Color))

                colorSquare.setBackgroundColor(selectedTag.Color)
                newTagInput.setText(selectedTag.Name)
                selectedColor = selectedTag.Color
            },
        )

        lvTags.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        lvTags.adapter = tagAdapter
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.accept_button -> {
                val inputText = userInputFolderName
                if (inputText == "") {
                    Toast.makeText(context, "Please specify a name for the folder.", Toast.LENGTH_LONG).show()
                    return
                }
                val tagName = newTag
                val newDir = File(currentDirectory, inputText)
                newDir.mkdir()

                if (tagName.isNotEmpty()) {
                    val tagFile: File = File(context.cacheDir, "tags.json")
                    UtilityHelper.addOrUpdateTagForDirectory(tagFile, newDir, selectedColor, tagName)
                }

                loadDirContents()
                dismiss()
            }
            R.id.cancel_button -> dismiss()
        }
    }
}