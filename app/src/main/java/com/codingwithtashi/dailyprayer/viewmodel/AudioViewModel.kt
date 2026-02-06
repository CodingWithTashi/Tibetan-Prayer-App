package com.codingwithtashi.dailyprayer.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.codingwithtashi.dailyprayer.AudioPlayerService
import com.codingwithtashi.dailyprayer.dao.PrayerDao
import com.codingwithtashi.dailyprayer.model.Prayer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AudioViewModel @Inject constructor(
    application: Application,
    private val prayerDao: PrayerDao
) : AndroidViewModel(application), AudioPlayerService.PlayerListener {

    companion object {
        private const val TAG = "AudioViewModel"
    }

    private var audioService: AudioPlayerService? = null
    private var isBound = false

    private val _isPlaying = MutableLiveData<Boolean>(false)
    val isPlaying: LiveData<Boolean> = _isPlaying

    private val _currentPrayer = MutableLiveData<Prayer?>()
    val currentPrayer: LiveData<Prayer?> = _currentPrayer

    private val _currentPosition = MutableLiveData<Int>(0)
    val currentPosition: LiveData<Int> = _currentPosition

    private val _duration = MutableLiveData<Int>(0)
    val duration: LiveData<Int> = _duration

    private val _isRepeatMode = MutableLiveData<Boolean>(false)
    val isRepeatMode: LiveData<Boolean> = _isRepeatMode

    private val _playlist = MutableLiveData<List<Prayer>>(emptyList())
    val playlist: LiveData<List<Prayer>> = _playlist

    private val _currentIndex = MutableLiveData<Int>(0)
    val currentIndex: LiveData<Int> = _currentIndex

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Get prayers with audio from database
    val prayersWithAudio: LiveData<List<Prayer>> = prayerDao.getPrayersWithAudio()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service connected")
            val binder = service as AudioPlayerService.AudioBinder
            audioService = binder.getService()
            audioService?.setPlayerListener(this@AudioViewModel)
            isBound = true

            // Update UI with current state
            audioService?.let {
                _isPlaying.value = it.isPlaying()
                _currentPrayer.value = it.getCurrentPrayer()
                _isRepeatMode.value = it.isRepeatEnabled()
                _playlist.value = it.getPlaylist()
                _currentIndex.value = it.getCurrentIndex()

                Log.d(TAG, "Service state - Playing: ${it.isPlaying()}, " +
                        "Current: ${it.getCurrentPrayer()?.title}, " +
                        "Playlist size: ${it.getPlaylist().size}")
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "Service disconnected")
            audioService = null
            isBound = false
        }
    }

    fun bindService(context: Context) {
        Log.d(TAG, "Binding service, currently bound: $isBound")
        if (!isBound) {
            try {
                val intent = Intent(context, AudioPlayerService::class.java)
                context.startService(intent)
                val bindResult = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                Log.d(TAG, "Bind service result: $bindResult")
            } catch (e: Exception) {
                Log.e(TAG, "Error binding service", e)
                _errorMessage.postValue("Failed to start audio service: ${e.message}")
            }
        } else {
            Log.d(TAG, "Service already bound")
        }
    }

    fun unbindService(context: Context) {
        Log.d(TAG, "Unbinding service")
        if (isBound) {
            try {
                audioService?.setPlayerListener(null)
                context.unbindService(serviceConnection)
                isBound = false
                Log.d(TAG, "Service unbound successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error unbinding service", e)
            }
        }
    }

    fun isServiceBound(): Boolean {
        Log.d(TAG, "Service bound check: $isBound")
        return isBound
    }

    fun setPlaylist(prayers: List<Prayer>, startIndex: Int = 0) {
        Log.d(TAG, "Setting playlist with ${prayers.size} prayers, start index: $startIndex")

        if (prayers.isEmpty()) {
            Log.w(TAG, "Attempting to set empty playlist")
            _errorMessage.postValue("No prayers available")
            return
        }

        // Filter prayers with valid audio URLs
        val validPrayers = prayers.filter { it.downloadUrl.isNotEmpty() }
        Log.d(TAG, "Valid prayers with audio: ${validPrayers.size}")

        if (validPrayers.isEmpty()) {
            Log.w(TAG, "No prayers have valid audio URLs")
            _errorMessage.postValue("No audio files available")
            return
        }

        _playlist.value = validPrayers

        if (audioService != null) {
            audioService?.setPlaylist(validPrayers, startIndex)
            Log.d(TAG, "Playlist set in service")
        } else {
            Log.e(TAG, "Audio service is null, cannot set playlist")
            _errorMessage.postValue("Audio service not ready")
        }
    }

    fun playPrayer(prayer: Prayer) {
        Log.d(TAG, "Play prayer: ${prayer.title}, URL: ${prayer.downloadUrl}")
        if (audioService != null) {
            audioService?.playPrayer(prayer)
        } else {
            Log.e(TAG, "Audio service is null")
            _errorMessage.postValue("Audio service not ready")
        }
    }

    fun playPrayerAtIndex(index: Int) {
        Log.d(TAG, "Play prayer at index: $index")
        if (audioService != null) {
            audioService?.playPrayerAtIndex(index)
        } else {
            Log.e(TAG, "Audio service is null")
            _errorMessage.postValue("Audio service not ready")
        }
    }

    fun play() {
        Log.d(TAG, "Play command")
        if (audioService != null) {
            audioService?.play()
        } else {
            Log.e(TAG, "Audio service is null")
            _errorMessage.postValue("Audio service not ready")
        }
    }

    fun pause() {
        Log.d(TAG, "Pause command")
        if (audioService != null) {
            audioService?.pause()
        } else {
            Log.e(TAG, "Audio service is null")
        }
    }

    fun playNext() {
        Log.d(TAG, "Play next command")
        if (audioService != null) {
            audioService?.playNext()
        } else {
            Log.e(TAG, "Audio service is null")
        }
    }

    fun playPrevious() {
        Log.d(TAG, "Play previous command")
        if (audioService != null) {
            audioService?.playPrevious()
        } else {
            Log.e(TAG, "Audio service is null")
        }
    }

    fun toggleRepeat() {
        Log.d(TAG, "Toggle repeat command")
        if (audioService != null) {
            audioService?.toggleRepeat()
        } else {
            Log.e(TAG, "Audio service is null")
        }
    }

    fun seekTo(position: Int) {
        Log.d(TAG, "Seek to: $position")
        if (audioService != null) {
            audioService?.seekTo(position)
        } else {
            Log.e(TAG, "Audio service is null")
        }
    }

    fun stop() {
        Log.d(TAG, "Stop command")
        audioService?.stop()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // PlayerListener callbacks
    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        Log.d(TAG, "Callback - Playback state: $isPlaying")
        _isPlaying.postValue(isPlaying)
    }

    override fun onPrayerChanged(prayer: Prayer) {
        Log.d(TAG, "Callback - Prayer changed: ${prayer.title}")
        _currentPrayer.postValue(prayer)
        audioService?.let {
            _currentIndex.postValue(it.getCurrentIndex())
        }
    }

    override fun onProgressUpdate(currentPosition: Int, duration: Int) {
        _currentPosition.postValue(currentPosition)
        _duration.postValue(duration)
    }

    override fun onRepeatModeChanged(isRepeat: Boolean) {
        Log.d(TAG, "Callback - Repeat mode: $isRepeat")
        _isRepeatMode.postValue(isRepeat)
    }

    override fun onError(message: String) {
        Log.e(TAG, "Callback - Error: $message")
        _errorMessage.postValue(message)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared")
        audioService?.setPlayerListener(null)
    }
}