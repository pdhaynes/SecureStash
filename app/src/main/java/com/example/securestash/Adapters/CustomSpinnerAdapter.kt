package com.example.securestash.Adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.example.securestash.R

class CustomSpinnerAdapter(private val context: Context, private val items: Array<String>) : BaseAdapter() {
    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.spinner_item, parent, false)
        val textView = view.findViewById<TextView>(R.id.spinner_text)
        textView.text = items[position]

        val arrowView = view.findViewById<ImageView>(R.id.spinner_arrow)
        if (items.size == 1) {
            arrowView.visibility = View.GONE
        }

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.spinner_item, parent, false)

        val textView = view.findViewById<TextView>(R.id.spinner_text)
        val arrowView = view.findViewById<ImageView>(R.id.spinner_arrow)
        val spinner_background = view.findViewById<RelativeLayout>(R.id.spinner_background)

        spinner_background.setBackgroundColor(context.getColor(R.color.isabelline))
        textView.text = items[position]
        arrowView.visibility = View.GONE

        return view
    }
}
