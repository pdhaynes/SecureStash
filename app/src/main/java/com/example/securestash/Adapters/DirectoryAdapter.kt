package com.example.securestash.Adapters

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.securestash.ContentDisplay
import com.example.securestash.DataModels.DirectoryItem
import com.example.securestash.DataModels.ItemType
import com.example.securestash.FileDirectory
import com.example.securestash.R
import org.json.JSONObject
import java.io.File

class DirectoryAdapter(
    private val itemList: MutableList<DirectoryItem>,
    private val currentDirectory: File,
) : RecyclerView.Adapter<DirectoryAdapter.DirectoryViewHolder>() {

    private val selectedItems = mutableListOf<DirectoryItem>()

    var isSelectionMode = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DirectoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.directory_item, parent, false)
        return DirectoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: DirectoryViewHolder, position: Int) {
        val directoryItem = itemList[position]
        holder.bind(directoryItem, currentDirectory)
    }

    override fun getItemCount(): Int = itemList.size

    fun getItemList(): MutableList<DirectoryItem> {
        return itemList
    }

    fun getSelectedItems(): MutableList<DirectoryItem> {
        return selectedItems
    }

    fun getSelectedItemCount(): Int {
        return selectedItems.count()
    }

    fun enableSelectionMode() {
        isSelectionMode = true
        notifyDataSetChanged() // Refresh the list
    }

    fun disableSelectionMode() {
        isSelectionMode = false
        notifyDataSetChanged() // Refresh the list
    }

    inner class DirectoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(item: DirectoryItem,
                 currentDirectory: File,
        ) {
            val itemName: TextView = itemView.findViewById(R.id.tv_item_name)
            val itemTag: TextView = itemView.findViewById(R.id.tv_item_tag)
            val itemIcon: ImageView = itemView.findViewById(R.id.iv_item_icon)
            val divider: View = itemView.findViewById(R.id.divider)
            val checkbox: CheckBox = itemView.findViewById(R.id.cb_item_select)

            itemTag.text = ""
            itemTag.visibility = View.GONE

            checkbox.isChecked = false

            itemName.text = item.name
            val itemPath = item.path
            val tagFile: File = File(itemView.context.cacheDir, "tags.json")
            val tagList = if (tagFile.exists()) {
                val content = tagFile.readText()
                JSONObject(content)
            } else {
                JSONObject()
            }

            if (tagList.has(itemPath)) {
                val tagInfo = tagList.getJSONObject(itemPath)
                if (tagInfo.has("tag")) {
                    val tagName = tagInfo.getString("tag")

                    val tagColor = tagInfo.getInt("color")
                    val textColor = getTextColorForBackground(tagColor)

                    itemTag.text = tagName
                    itemTag.backgroundTintList = ColorStateList.valueOf(tagColor)
                    itemTag.setTextColor(textColor)

                    itemTag.visibility = View.VISIBLE
                }

            }

            itemIcon.setImageResource(
                when (item.type) {
                    ItemType.DIRECTORY -> R.drawable.ic_folder_24
                    ItemType.IMAGE -> R.drawable.ic_photo_24
                    ItemType.DOCUMENT -> R.drawable.ic_document_24
                    else -> R.drawable.ic_camera_24
                }
            )

            if (isSelectionMode) {
                divider.visibility = View.VISIBLE
                checkbox.visibility = View.VISIBLE
            } else {
                divider.visibility = View.GONE
                checkbox.visibility = View.GONE
                checkbox.isChecked = false
                selectedItems.clear()
            }

            checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked && !selectedItems.contains(item)) {
                    selectedItems.add(item)
                } else if (selectedItems.contains(item) && !isChecked) {
                    selectedItems.remove(item)
                } else {
                    selectedItems.remove(item)
                }
            }

            itemView.setOnClickListener {
                if (!isSelectionMode) {
                    if (item.type == ItemType.DIRECTORY) {
                        val intent = Intent(itemView.context, FileDirectory::class.java)
                        intent.putExtra("SPECIFIED_DIR", File(currentDirectory, item.name).toString())
                        itemView.context.startActivity(intent)
                    } else {
                        val intent = Intent(itemView.context, ContentDisplay::class.java)
                        intent.putExtra("ITEM_PATH", item.path)
                        intent.putExtra("ITEM_TYPE", item.type.toString())
                        itemView.context.startActivity(intent)
                    }
                }
                else {
                    checkbox.isChecked = !checkbox.isChecked
                }
            }

            itemView.setOnLongClickListener {
                if (!isSelectionMode) {
                    (itemView.context as? DirectoryAdapterListener)?.onEnableSelectionMode()
                }
                true
            }
        }
    }
}

fun getLuminance(color: Int): Double {
    val red = Color.red(color) / 255.0
    val green = Color.green(color) / 255.0
    val blue = Color.blue(color) / 255.0

    return 0.2126 * red + 0.7152 * green + 0.0722 * blue
}

fun getTextColorForBackground(backgroundColor: Int): Int {
    return if (getLuminance(backgroundColor) > 0.5) {
        Color.BLACK
    } else {
        Color.WHITE
    }
}

interface DirectoryAdapterListener {
    fun onEnableSelectionMode()
}
