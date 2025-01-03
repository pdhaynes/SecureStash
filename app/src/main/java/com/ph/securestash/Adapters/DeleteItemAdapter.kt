package com.ph.securestash.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ph.securestash.Helpers.UtilityHelper
import com.ph.securestash.R
import java.io.File

class DeleteItemAdapter(
    private val context: Context,
    private val selectedItems: List<File>
) : RecyclerView.Adapter<DeleteItemAdapter.DeleteItemViewHolder>() {

    class DeleteItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemName: TextView = view.findViewById(R.id.name)
        val itemPath: TextView = view.findViewById(R.id.path)
        val itemTag: TextView = view.findViewById(R.id.tag)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeleteItemViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.delete_item, parent, false)
        return DeleteItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeleteItemViewHolder, position: Int) {
        val item = selectedItems[position]
        val itemTag = UtilityHelper.getFileTag(
            tagFile = File(context.cacheDir, "tags.json"),
            targetDirectoryPath = File(item.path)
        )

        val basePath = File(context.filesDir, "Files").absolutePath
        holder.itemName.text = item.name

        if (item.path != basePath + "/" + item.name) {
            holder.itemPath.text = "..${item.path.removePrefix(basePath)}"
            holder.itemPath.visibility = View.VISIBLE
        }

        if (itemTag != null) {
            holder.itemTag.text = itemTag.Name
            holder.itemTag.background.setTint(itemTag.Color)
            holder.itemTag.setTextColor(UtilityHelper.getTextColorForBackground(itemTag.Color))
            holder.itemTag.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int = selectedItems.size
}
