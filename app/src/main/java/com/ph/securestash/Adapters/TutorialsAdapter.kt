package com.ph.securestash.Adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.ph.securestash.DataModels.Tutorial
import com.ph.securestash.Introduction
import com.ph.securestash.R

class TutorialsAdapter(private val tutorialsList: List<Tutorial>) : RecyclerView.Adapter<TutorialsAdapter.TutorialViewHolder>() {

    class TutorialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tutorialTitle: MaterialTextView = itemView.findViewById(R.id.tutorial_title)
        val image: ImageView = itemView.findViewById(R.id.image)
        val card: CardView = itemView.findViewById(R.id.card)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TutorialViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.grid_tutorial_item, parent, false)
        return TutorialViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TutorialViewHolder, position: Int) {
        val tutorial = tutorialsList[position]
        holder.tutorialTitle.text = tutorial.title
        holder.image.setImageResource(tutorial.image)
        holder.card.setOnClickListener {
            val tutorialIntent = Intent(holder.itemView.context, Introduction::class.java)
            holder.itemView.context.startActivity(tutorialIntent)
        }
    }

    override fun getItemCount(): Int {
        return tutorialsList.size
    }
}
