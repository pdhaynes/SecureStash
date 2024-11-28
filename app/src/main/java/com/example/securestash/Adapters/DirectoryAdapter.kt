package com.example.securestash.Adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.securestash.ContentDisplay
import com.example.securestash.DataModels.DirectoryItem
import com.example.securestash.DataModels.ItemType
import com.example.securestash.R

class DirectoryAdapter(private val itemList: List<DirectoryItem>) :
    RecyclerView.Adapter<DirectoryAdapter.DirectoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DirectoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.directory_item, parent, false)
        return DirectoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: DirectoryViewHolder, position: Int) {
        val directoryItem = itemList[position]
        holder.bind(directoryItem)
    }

    override fun getItemCount(): Int = itemList.size

    class DirectoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: DirectoryItem) {
            val itemName: TextView = itemView.findViewById(R.id.tv_item_name)
            val itemPath: TextView = itemView.findViewById(R.id.tv_item_path)
            val itemIcon: ImageView = itemView.findViewById(R.id.iv_item_icon)

            itemName.text = item.name
            itemPath.text = item.name

            itemPath.visibility = View.VISIBLE

            itemIcon.setImageResource(
                when (item.type) {
                    ItemType.DIRECTORY -> R.drawable.ic_settings_24
                    ItemType.IMAGE -> R.drawable.ic_photo_24
                    ItemType.DOCUMENT -> R.drawable.ic_document_24
                    else -> R.drawable.ic_camera_24
                }
            )

            itemView.setOnClickListener {
                Toast.makeText(itemView.context, "Clicked ${item.name}", Toast.LENGTH_SHORT).show()
                val intent = Intent(itemView.context, ContentDisplay::class.java)

                intent.putExtra("ITEM_PATH", item.path)
                intent.putExtra("ITEM_TYPE", item.type.toString())

                itemView.context.startActivity(intent)
            }
        }
    }
}
