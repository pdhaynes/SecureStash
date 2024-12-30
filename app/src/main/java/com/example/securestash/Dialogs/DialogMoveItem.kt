package com.example.securestash.Dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import com.example.securestash.Adapters.CustomSpinnerAdapter
import com.example.securestash.Adapters.DirectoryAdapter
import com.example.securestash.Helpers.UtilityHelper
import com.example.securestash.R
import com.google.android.material.button.MaterialButton
import java.io.File

class DialogMoveItem(
    context: Context,
    private val currentPath: String,
    private val directoryAdapter: DirectoryAdapter,
    private val loadDirContents: () -> Unit,
    private val hideSelectionButtons: () -> Unit,
    private val showMainButtons: () -> Unit,
) : Dialog(context), View.OnClickListener {
    private lateinit var accept: MaterialButton
    private lateinit var cancel: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_move_item)

        accept = findViewById(R.id.accept_button)
        cancel = findViewById(R.id.cancel_button)
        accept.setOnClickListener(this)
        cancel.setOnClickListener(this)

        val basePath = File(context.filesDir, "Files").absolutePath

        val directoryList = UtilityHelper.recursivelyGrabFileList(File(context.filesDir, "Files"))
            .filter { it.isDirectory && it.absolutePath != currentPath }
            .map { file -> file.absolutePath.removePrefix("$basePath/") }
            .toMutableList()

        Log.d("ASDF", File(currentPath, "Files").toString())
        Log.d("ASDF", context.filesDir.toString())
        Log.d("ASDF", (currentPath != File(context.filesDir, "Files").toString()).toString())

        if (currentPath != File(context.filesDir, "Files").toString()) {
            directoryList.add(0, "Home Directory")
        }

        val movementSelect: Spinner = findViewById(R.id.dropdown_select)

        val adapter = CustomSpinnerAdapter(context, directoryList.toTypedArray())

        movementSelect.adapter = adapter

        movementSelect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedOption = directoryList[position]
                Toast.makeText(context, "Selected: $selectedOption", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }

    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.accept_button -> {
                val basePath = File(context.filesDir, "Files").absolutePath
                val movementSelect: Spinner = findViewById(R.id.dropdown_select)

                val selectedPath = movementSelect.selectedItem?.toString()
                if (selectedPath.isNullOrEmpty()) {
                    Toast.makeText(context, "No destination selected!", Toast.LENGTH_SHORT).show()
                    return
                }

                val targetDirectory = if (selectedPath == "Home Directory") {
                    File(context.filesDir, "Files")
                } else {
                    File(basePath, selectedPath)
                }

                if (!targetDirectory.exists()) {
                    Toast.makeText(context, "Target directory does not exist!", Toast.LENGTH_SHORT).show()
                    return
                }

                val filesToMove = directoryAdapter.getSelectedItems()
                if (filesToMove.isEmpty()) {
                    Toast.makeText(context, "No files to move!", Toast.LENGTH_SHORT).show()
                    return
                }

                filesToMove.forEach { file ->
                    val destinationFile = File(targetDirectory, file.name)
                    val targetFile = File(file.path)
                    if (targetFile.renameTo(destinationFile)) {
                        Log.d("MoveFile", "Moved file to $destinationFile")
                        targetFile.delete()
                    } else {
                        Toast.makeText(context, "Failed to move: ${file.name}", Toast.LENGTH_SHORT).show()
                    }
                }

                Toast.makeText(context, "Files moved successfully!", Toast.LENGTH_SHORT).show()
                loadDirContents.invoke()
                directoryAdapter.disableSelectionMode()
                hideSelectionButtons.invoke()
                showMainButtons.invoke()
                dismiss()
            }
            R.id.cancel_button -> dismiss()
        }
    }
}