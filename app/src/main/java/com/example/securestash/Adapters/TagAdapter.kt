package com.example.securestash.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.securestash.DataModels.Tag
import com.example.securestash.Helpers.UtilityHelper
import com.example.securestash.R

class TagAdapter(
    private val tagList: List<Tag>,
    private val updatePreviewTag: (Tag) -> Unit
) : RecyclerView.Adapter<TagAdapter.TagViewHolder>() {

    private val updatePreview = updatePreviewTag

    class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.preview_tag)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.tag_item, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tag = tagList[position]
        holder.textView.text = tag.Name
        holder.textView.background.setTint(tag.Color)
        holder.textView.setTextColor(UtilityHelper.getTextColorForBackground(tag.Color))
        holder.textView.setOnClickListener {
            updatePreview.invoke(tag)
        }
    }

    override fun getItemCount(): Int = tagList.size
}