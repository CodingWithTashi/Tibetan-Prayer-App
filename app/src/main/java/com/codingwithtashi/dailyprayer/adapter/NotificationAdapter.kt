package com.codingwithtashi.dailyprayer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codingwithtashi.dailyprayer.R

class NotificationAdapter(private val items: MutableList<String>) : RecyclerView.Adapter<NotificationAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(parent)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun addItem(name: String) {
        items.add(name)
        notifyItemInserted(items.size)
    }

    fun removeAt(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    class VH(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.notification_single_item, parent, false)) {
        fun bind(name: String) = with(itemView) {
            val txtTitle = itemView.findViewById<TextView>(R.id.txtTitle)
            txtTitle.text = name
        }
    }
}