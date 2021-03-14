package com.codingwithtashi.dailyprayer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.model.PrayerNotification

class NotificationAdapter(private val items: MutableList<PrayerNotification>) : RecyclerView.Adapter<NotificationAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(parent)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun addItem(prayerNotification: PrayerNotification) {
        items.add(prayerNotification)
        notifyItemInserted(items.size)
    }

    fun removeAt(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    class VH(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.notification_single_item, parent, false)) {
        fun bind(prayerNotification: PrayerNotification) = with(itemView) {
            val txtTitle = itemView.findViewById<TextView>(R.id.txtTitle)
            val desc = itemView.findViewById<TextView>(R.id.description)
            val time = itemView.findViewById<TextView>(R.id.notification_time)
            txtTitle.text = prayerNotification.title
            desc.text = prayerNotification.content
            time.text = prayerNotification.time
        }
    }
}