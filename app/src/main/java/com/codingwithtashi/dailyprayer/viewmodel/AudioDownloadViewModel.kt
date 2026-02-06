package com.codingwithtashi.dailyprayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.codingwithtashi.dailyprayer.dao.PrayerDao
import com.codingwithtashi.dailyprayer.model.Prayer
import com.codingwithtashi.dailyprayer.utils.AudioCacheManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioDownloadViewModel @Inject constructor(
    application: Application,
    private val audioCacheManager: AudioCacheManager,
    private val prayerDao: PrayerDao
) : AndroidViewModel(application) {

    private val _downloadProgress = MutableLiveData<DownloadProgress>()
    val downloadProgress: LiveData<DownloadProgress> = _downloadProgress

    private val _cacheSize = MutableLiveData<String>()
    val cacheSize: LiveData<String> = _cacheSize

    init {
        updateCacheSize()
    }

    fun downloadPrayerAudio(prayer: Prayer) {
        if (prayer.downloadUrl.isNullOrEmpty()) {
            _downloadProgress.value = DownloadProgress(
                prayerId = prayer.id!!,
                status = DownloadStatus.FAILED,
                message = "No audio URL available"
            )
            return
        }

        if (audioCacheManager.isCached(prayer.id!!, prayer.downloadUrl)) {
            _downloadProgress.value = DownloadProgress(
                prayerId = prayer.id!!,
                status = DownloadStatus.COMPLETED,
                message = "Already downloaded"
            )
            return
        }

        viewModelScope.launch {
            _downloadProgress.value = DownloadProgress(
                prayerId = prayer.id!!,
                status = DownloadStatus.DOWNLOADING,
                message = "Downloading ${prayer.title}..."
            )

            val result = audioCacheManager.downloadAndCache(prayer.id!!, prayer.downloadUrl!!)

            if (result.isSuccess) {
                _downloadProgress.value = DownloadProgress(
                    prayerId = prayer.id!!,
                    status = DownloadStatus.COMPLETED,
                    message = "Downloaded successfully"
                )
            } else {
                _downloadProgress.value = DownloadProgress(
                    prayerId = prayer.id!!,
                    status = DownloadStatus.FAILED,
                    message = "Download failed: ${result.exceptionOrNull()?.message}"
                )
            }

            updateCacheSize()
        }
    }

    fun downloadAllPrayers() {
        viewModelScope.launch {
            val prayers = prayerDao.getPrayersWithAudioSync()

            prayers.forEach { prayer ->
                if (!prayer.downloadUrl.isNullOrEmpty() &&
                    !audioCacheManager.isCached(prayer.id!!, prayer.downloadUrl)) {
                    downloadPrayerAudio(prayer)
                }
            }
        }
    }

    fun deleteCachedAudio(prayer: Prayer) {
        viewModelScope.launch {
            val deleted = audioCacheManager.deleteCachedFile(prayer.id!!, prayer.downloadUrl)
            if (deleted) {
                _downloadProgress.value = DownloadProgress(
                    prayerId = prayer.id!!,
                    status = DownloadStatus.DELETED,
                    message = "Cache cleared"
                )
            }
            updateCacheSize()
        }
    }

    fun clearAllCache() {
        viewModelScope.launch {
            val cleared = audioCacheManager.clearAllCache()
            if (cleared) {
                _downloadProgress.value = DownloadProgress(
                    prayerId = -1,
                    status = DownloadStatus.DELETED,
                    message = "All cache cleared"
                )
            }
            updateCacheSize()
        }
    }

    fun isCached(prayer: Prayer): Boolean {
        return audioCacheManager.isCached(prayer.id!!, prayer.downloadUrl)
    }

    private fun updateCacheSize() {
        _cacheSize.value = audioCacheManager.getCacheSizeFormatted()
    }

    data class DownloadProgress(
        val prayerId: Int,
        val status: DownloadStatus,
        val message: String
    )

    enum class DownloadStatus {
        IDLE,
        DOWNLOADING,
        COMPLETED,
        FAILED,
        DELETED
    }
}