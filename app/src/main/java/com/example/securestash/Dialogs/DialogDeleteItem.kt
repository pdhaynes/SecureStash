package com.example.securestash.Dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.securestash.Adapters.DeleteItemAdapter
import com.example.securestash.Adapters.DirectoryAdapter
import com.example.securestash.Helpers.UtilityHelper
import com.example.securestash.R
import com.google.android.material.button.MaterialButton
import java.io.File

class DialogDeleteItem(
    context: Context,
    private val directoryAdapter: DirectoryAdapter,
    private val loadDirContents: () -> Unit,
    private val hideSelectionButtons: () -> Unit,
    private val showMainButtons: () -> Unit
) : Dialog(context), View.OnClickListener {
    private lateinit var accept: MaterialButton
    private lateinit var cancel: MaterialButton
    private lateinit var deleteList: MutableList<File>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_delete_item)

        accept = findViewById(R.id.accept_button)
        cancel = findViewById(R.id.cancel_button)
        accept.setOnClickListener(this)
        cancel.setOnClickListener(this)

        deleteList = directoryAdapter.getSelectedItems().map {
                item -> File(item.path)
        }.toMutableList()

        deleteList.filter {
            it.isDirectory
        }.forEach { directory ->
            Log.d("Directory", directory.toString())
            deleteList.addAll(UtilityHelper.recursivelyGrabFileList(directory))
        }

        val deleteTitle: TextView = findViewById(R.id.delete_item_title)
        deleteTitle.text = context.getString(R.string.dialog_change_tag_title, deleteList.count())

        val deleteConfirmation: TextView = findViewById(R.id.delete_confirmation)
        deleteConfirmation.text = context.getString(R.string.dialog_delete_items_confirmation, deleteList.count())

        val deleteView: RecyclerView = findViewById(R.id.delete_list)
        deleteView.adapter = DeleteItemAdapter(context, deleteList)
        deleteView.layoutManager = LinearLayoutManager(context)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.accept_button -> {
                val deleteList = directoryAdapter.getSelectedItems()
                for (item in deleteList) {
                    val position = directoryAdapter.getItemList().indexOf(item)
                    if (position != -1) {
                        if (File(item.path).isFile) {
                            File(item.path).delete()
                        } else {
                            File(item.path).deleteRecursively()
                        }
                        UtilityHelper.removeTag(File(context.cacheDir, "tags.json"), File(item.path))
                        directoryAdapter.getItemList().removeAt(position)
                        directoryAdapter.notifyItemRemoved(position)
                    }
                }
                loadDirContents.invoke()
                directoryAdapter.disableSelectionMode()
                hideSelectionButtons()
                showMainButtons()
                dismiss()
            }
            R.id.cancel_button -> dismiss()
        }
    }
}