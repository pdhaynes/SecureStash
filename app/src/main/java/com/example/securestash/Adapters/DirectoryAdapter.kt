package com.example.securestash.Adapters

import android.content.ClipData.Item
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.securestash.ContentDisplay
import com.example.securestash.DataModels.DirectoryItem
import com.example.securestash.DataModels.ItemType
import com.example.securestash.FileDirectory
import com.example.securestash.Helpers.Cache
import com.example.securestash.Helpers.CryptographyHelper
import com.example.securestash.Helpers.UtilityHelper
import com.example.securestash.R
import org.json.JSONObject
import java.io.File

class DirectoryAdapter(
    private val itemList: MutableList<DirectoryItem>,
    private val currentDirectory: File,
    private var isGrid: Boolean
) : RecyclerView.Adapter<DirectoryAdapter.DirectoryViewHolder>() {

    private val selectedItems = mutableListOf<DirectoryItem>()

    var isSelectionMode = false

    fun updateLayout(isGrid: Boolean) {
        this.isGrid = isGrid
    }

    override fun getItemViewType(position: Int): Int {
        return if (isGrid) VIEW_TYPE_GRID else VIEW_TYPE_LINEAR
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DirectoryViewHolder {
        val layoutId = when (viewType) {
            VIEW_TYPE_GRID -> R.layout.grid_directory_item
            VIEW_TYPE_LINEAR -> R.layout.linear_directory_item
            else -> throw IllegalArgumentException("Invalid view type")
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
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
            val itemName: TextView = itemView.findViewById(R.id.name)
            val itemTag: TextView = itemView.findViewById(R.id.tag)
            val itemIcon: ImageView = itemView.findViewById(R.id.image)
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
                if (content.isNotBlank()) JSONObject(content) else JSONObject()
            } else {
                JSONObject()
            }

            if (tagList.has(itemPath)) {
                val tagInfo = tagList.getJSONObject(itemPath)
                if (tagInfo.has("tag")) {
                    val tagName = tagInfo.getString("tag")

                    val tagColor = tagInfo.getInt("color")
                    val textColor = UtilityHelper.getTextColorForBackground(tagColor)

                    itemTag.text = tagName
                    itemTag.backgroundTintList = ColorStateList.valueOf(tagColor)
                    itemTag.setTextColor(textColor)

                    itemTag.visibility = View.VISIBLE
                }

            }

            var bitmap: Bitmap? = null
            val cachedBitmap = Cache.retrieveCachedBitmap(item.path)
            val cachedItemType = Cache.getFileTypeFromMemory(item.path)
            if (isGrid) {
                if (cachedItemType != null) {
                    val itemType = ItemType.fromName(cachedItemType)
                    if (itemType == ItemType.IMAGE || itemType == ItemType.DOCUMENT) {
                        bitmap = if (cachedBitmap != null) {
                            cachedBitmap
                        } else {
                            val cryptoHelper = CryptographyHelper()
                            val secretKey = cryptoHelper.getSecretKeyFromKeystore(item.name)
                            val decodedBytes = cryptoHelper.decodeFile(File(item.path), secretKey).first

                            val decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            Bitmap.createScaledBitmap(decodedBitmap, 120, 120, true)
                        }
                    }
                } else {
                    if (File(itemPath).isFile) {
                        val cryptoHelper = CryptographyHelper()
                        val secretKey = cryptoHelper.getSecretKeyFromKeystore(item.name)
                        val decodedBytes = cryptoHelper.decodeFile(File(item.path), secretKey)
                        val imageBytes = decodedBytes.first
                        val fileType = cryptoHelper.readMetadata(decodedBytes.second).first
                        Cache.saveFileTypeInMemory(itemPath, fileType.toString())
                        val itemType: ItemType = ItemType.fromName(fileType.toString())
                        bitmap = if (itemType == ItemType.IMAGE || itemType == ItemType.DOCUMENT) {
                            val decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            Bitmap.createScaledBitmap(decodedBitmap, 120, 120, true)
                        } else {
                            null
                        }
                    }
                }

                if (bitmap != null) {
                    itemIcon.setImageBitmap(bitmap)
                    Cache.bitmapCache.put(item.path, bitmap)
                    itemIcon.clearColorFilter()
                } else {
                    itemIcon.setImageResource(
                        when (item.type) {
                            ItemType.DIRECTORY -> R.drawable.ic_folder_24
                            ItemType.IMAGE -> R.drawable.ic_photo_24
                            ItemType.DOCUMENT -> R.drawable.ic_document_24
                            ItemType.VIDEO -> R.drawable.ic_video_camera_24
                            ItemType.AUDIO -> R.drawable.ic_microphone_24
                            else -> R.drawable.ic_question_mark_24
                        }
                    )
                    itemIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.spacecadet), PorterDuff.Mode.SRC_IN)
                }
            } else {
                itemIcon.setImageResource(
                    when (item.type) {
                        ItemType.DIRECTORY -> R.drawable.ic_folder_24
                        ItemType.IMAGE -> R.drawable.ic_photo_24
                        ItemType.DOCUMENT -> R.drawable.ic_document_24
                        ItemType.VIDEO -> R.drawable.ic_video_camera_24
                        ItemType.AUDIO -> R.drawable.ic_microphone_24
                        else -> R.drawable.ic_question_mark_24
                    }
                )
                itemIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.spacecadet), PorterDuff.Mode.SRC_IN)
            }

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


    companion object {
        private const val VIEW_TYPE_GRID = 1
        private const val VIEW_TYPE_LINEAR = 2
    }
}




interface DirectoryAdapterListener {
    fun onEnableSelectionMode()
}
