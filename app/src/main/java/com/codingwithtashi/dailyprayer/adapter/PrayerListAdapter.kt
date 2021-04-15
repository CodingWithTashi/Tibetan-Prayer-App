package com.codingwithtashi.dailyprayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.model.Prayer
import com.codingwithtashi.dailyprayer.utils.FragmentType
import com.google.android.material.textview.MaterialTextView

/**
 * Created by kunchok on 10/03/2021
 */
class PrayerListAdapter(var listener: ItemClickListener,var type: String) : ListAdapter<Prayer,PrayerListAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.single_list_item,parent,false);
        return ViewHolder(view);
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem,listener,type);
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var title: MaterialTextView = itemView.findViewById(R.id.sno);
        var content: MaterialTextView = itemView.findViewById(R.id.content);

        fun bind(
            prayer: Prayer?,
            listener: ItemClickListener,
            type: String
        ) {
            title.text ="༄༅། ";
            content.text = prayer?.title;
            itemView.setOnClickListener {
                if (prayer != null) {
                    listener.onItemClick(prayer = prayer)
                }
                if(type == FragmentType.HOME_FRAGMENT)
                    Navigation.findNavController(itemView).navigate(R.id.action_homeFragment_to_detailFragment)
                else
                    Navigation.findNavController(itemView).navigate(R.id.action_routineFragment_to_detailFragment)

            }
        }


    }

    class DiffCallback : DiffUtil.ItemCallback<Prayer>() {
        override fun areItemsTheSame(oldItem: Prayer, newItem: Prayer) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Prayer, newItem: Prayer) =
            oldItem == newItem
    }

}
interface ItemClickListener{
    fun onItemClick(prayer:Prayer);
}