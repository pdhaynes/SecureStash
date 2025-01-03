package com.ph.securestash.Dialogs

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
import com.ph.securestash.Adapters.DirectoryAdapter
import com.ph.securestash.Adapters.TagAdapter
import com.ph.securestash.DataModels.DirectoryItem
import com.ph.securestash.Helpers.UtilityHelper
import com.ph.securestash.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import yuku.ambilwarna.AmbilWarnaDialog
import java.io.File

class DialogChangeTag(
    context: Context,
    directoryAdapter: DirectoryAdapter,
    private val loadDirContents: () -> Unit,
    private val hideSelectionButtons: () -> Unit,
    private val showMainButtons: () -> Unit
) : Dialog(context), View.OnClickListener {
    private lateinit var accept: MaterialButton
    private lateinit var cancel: MaterialButton
    private lateinit var remove: MaterialButton

    private lateinit var newTag: String

    private var dirAdapter = directoryAdapter
    private var selectedColor = Color.BLUE
    private var selectionList: List<DirectoryItem> = dirAdapter.getSelectedItems()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_tag_changer)

        accept = findViewById(R.id.accept_button)
        cancel = findViewById(R.id.cancel_button)
        remove = findViewById(R.id.remove_tag)

        accept.setOnClickListener(this)
        cancel.setOnClickListener(this)
        remove.setOnClickListener(this)

        val titleTag: TextView = findViewById(R.id.tag_change_title)
        titleTag.text = context.getString(R.string.dialog_change_tag_title, selectionList.count())

        val tagFile = File(context.cacheDir, "tags.json")
        val tagList = UtilityHelper.getMostRecentTags(tagFile)

        val tagsPresent: Boolean = selectionList.filter { item ->
            UtilityHelper.getFileTag(tagFile, File(item.path)) != null
        }.isNotEmpty()

        if (tagsPresent) {
            remove.setTextColor(context.getColor(R.color.unblue))
            remove.isEnabled = true
        } else {
            remove.setTextColor(context.getColor(android.R.color.darker_gray))
            remove.isEnabled = false
        }

        val previewTag: TextView = findViewById(R.id.preview_tag)
        previewTag.background.setTint(selectedColor)
        previewTag.setTextColor(UtilityHelper.getTextColorForBackground(selectedColor))
        val newTagInput: TextInputEditText = findViewById(R.id.user_tag_input)
        newTagInput.doOnTextChanged { text, _, _, _ ->
            if (text.toString().isEmpty()) {
                previewTag.text = context.getString(R.string.tag_preview)
            } else {
                previewTag.text = text
                newTag = text.toString()
            }
        }

        val colorSquare: View = findViewById(R.id.color_square)
        colorSquare.setBackgroundColor(selectedColor)
        colorSquare.setOnClickListener {
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
            }
        )

        lvTags.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        lvTags.adapter = tagAdapter

        val noTags: TextView = findViewById(R.id.no_tags_text)
        if (tagList.isEmpty()) {
            noTags.visibility = View.VISIBLE
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.accept_button -> {
                if (::newTag.isInitialized){
                    val tagName = newTag
                    if (tagName.isNotEmpty()) {
                        val tagFile = File(context.cacheDir, "tags.json")
                        for (item in selectionList) {
                            UtilityHelper.addOrUpdateTagForDirectory(tagFile, File(item.path), selectedColor, tagName)
                        }
                        loadDirContents.invoke()
                        dismiss()
                    } else {
                        Toast.makeText(context, "Tag name cannot be empty!", Toast.LENGTH_SHORT).show()
                    }
                    dirAdapter.disableSelectionMode()
                    hideSelectionButtons()
                    showMainButtons()
                } else {
                    Toast.makeText(context, "Please enter a name for the tag.", Toast.LENGTH_SHORT).show()
                }
            }
            R.id.remove_tag -> {
                if (remove.isEnabled) {
                    val tagFile = File(context.cacheDir, "tags.json")
                    for (item in selectionList) {
                        UtilityHelper.removeTag(
                            tagFile = tagFile,
                            targetDirectoryPath = File(item.path)
                        )}
                    loadDirContents.invoke()
                    dirAdapter.disableSelectionMode()
                    hideSelectionButtons()
                    showMainButtons()
                    dismiss()
                } else {
                    Toast.makeText(context, "Selected files have no tags to remove.", Toast.LENGTH_SHORT).show()
                }
            }
            R.id.cancel_button -> dismiss()
        }
    }
}