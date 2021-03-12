package com.codingwithtashi.dailyprayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.model.Prayer
import com.codingwithtashi.dailyprayer.viewmodel.HomeViewModel
import com.google.android.material.textview.MaterialTextView

/**
 * Created by kunchok on 10/03/2021
 */
class PrayerListAdapter(var prayerList: ArrayList<Prayer>,var listener: ItemClickListener) : RecyclerView.Adapter<PrayerListAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.single_list_item,parent,false);
        return ViewHolder(view);
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sno = position+1;
        holder.title.text ="$sno. ";
        holder.content.text = prayerList[position].title;
        holder.itemView.setOnClickListener {
            listener.onItemClick(prayer = prayerList[position])
            Navigation.findNavController(holder.itemView).navigate(R.id.action_homeFragment_to_detailFragment);
        }
    }

    override fun getItemCount(): Int {
        return prayerList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var title: MaterialTextView = itemView.findViewById(R.id.sno);
        var content: MaterialTextView = itemView.findViewById(R.id.content);

    }

}
interface ItemClickListener{
    fun onItemClick(prayer:Prayer);
}