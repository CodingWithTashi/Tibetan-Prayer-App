package com.codingwithtashi.dailyprayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.databinding.ItemPlaylistBinding
import com.codingwithtashi.dailyprayer.model.Prayer

class AudioPlaylistAdapter(
    private val onPrayerClick: (Prayer, Int) -> Unit,
    private val onDownloadClick: (Prayer) -> Unit,
    private val isCachedChecker: (Prayer) -> Boolean
) : ListAdapter<Prayer, AudioPlaylistAdapter.PrayerViewHolder>(PrayerDiffCallback()) {

    private var currentPlayingId: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrayerViewHolder {
        val binding = ItemPlaylistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PrayerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PrayerViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    fun setCurrentPlaying(prayerId: Int) {
        val oldId = currentPlayingId
        currentPlayingId = prayerId

        // Refresh only affected items
        currentList.forEachIndexed { index, prayer ->
            if (prayer.id == prayerId || prayer.id == oldId) {
                notifyItemChanged(index)
            }
        }
    }

    inner class PrayerViewHolder(
        private val binding: ItemPlaylistBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(prayer: Prayer, position: Int) {
            binding.txtPrayerTitle.text = prayer.title

            val isPlaying = prayer.id == currentPlayingId
            val isCached = isCachedChecker(prayer)

            // Background color for playing item
            if (isPlaying) {
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.playing_background)
                )
                binding.imgPlayingIndicator.visibility = View.VISIBLE
            } else {
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, android.R.color.transparent)
                )
                binding.imgPlayingIndicator.visibility = View.GONE
            }

            // Show cached indicator or download button
            if (isCached) {
                binding.imgDownload.setImageResource(R.drawable.ic_downloaded)
                binding.imgDownload.visibility = View.VISIBLE
            } else {
                binding.imgDownload.setImageResource(R.drawable.ic_download)
                binding.imgDownload.visibility = View.VISIBLE
            }

            // Click listeners
            binding.root.setOnClickListener {
                onPrayerClick(prayer, position)
            }

            binding.imgDownload.setOnClickListener {
                if (!isCached) {
                    onDownloadClick(prayer)
                }
            }
        }
    }

    private class PrayerDiffCallback : DiffUtil.ItemCallback<Prayer>() {
        override fun areItemsTheSame(oldItem: Prayer, newItem: Prayer): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Prayer, newItem: Prayer): Boolean {
            return oldItem == newItem
        }
    }
}