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
import com.example.securestash.DataModels.DirectoryItem
import com.example.securestash.Helpers.UtilityHelper
import com.example.securestash.R
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
        accept.setOnClickListener(this)
        cancel.setOnClickListener(this)

        val titleTag: TextView = findViewById(R.id.tag_change_title)
        titleTag.text = "Change tag for ${selectionList.count()} item(s)."

        val tagFile = File(context.cacheDir, "tags.json")
        val tagList = UtilityHelper.getMostRecentTags(tagFile)

        val previewTag: TextView = findViewById(R.id.preview_tag)
        previewTag.background.setTint(selectedColor)
        previewTag.setTextColor(UtilityHelper.getTextColorForBackground(selectedColor))
        val newTagInput: TextInputEditText = findViewById(R.id.user_tag_input)
        newTagInput.doOnTextChanged { text, start, before, count ->
            if (count < 1) {
                previewTag.text = "Preview"
            } else {
                previewTag.text = text
                newTag = text.toString()
            }
        }

        val colorSquare: View = findViewById(R.id.color_square)
        colorSquare.setBackgroundColor(selectedColor)
        colorSquare.setOnClickListener {
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
            }
            R.id.cancel_button -> dismiss()
        }
    }
}