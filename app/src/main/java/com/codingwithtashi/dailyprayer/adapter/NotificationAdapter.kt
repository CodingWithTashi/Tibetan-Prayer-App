package com.codingwithtashi.dailyprayer.adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.model.PrayerNotification

class NotificationAdapter(private val items: ArrayList<PrayerNotification>) : RecyclerView.Adapter<NotificationAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(parent)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size


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
            val moreText = itemView.findViewById<TextView>(R.id.more_text)
            txtTitle.text = prayerNotification.title
            desc.text = prayerNotification.content
            time.text = prayerNotification.time
            if(!prayerNotification.link.isNullOrEmpty()){
                moreText.visibility = View.VISIBLE
                moreText.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(prayerNotification.link))
                    context.startActivity(intent)
                }
            }
        }
    }
}