package com.codingwithtashi.dailyprayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.codingwithtashi.dailyprayer.model.Prayer
import com.codingwithtashi.dailyprayer.ui.activity.MainActivity
import java.io.File
import java.io.IOException

class AudioPlayerService : Service() {

    companion object {
        const val TAG = "AudioPlayerService"
        const val CHANNEL_ID = "prayer_audio_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.codingwithtashi.dailyprayer.ACTION_PLAY"
        const val ACTION_PAUSE = "com.codingwithtashi.dailyprayer.ACTION_PAUSE"
        const val ACTION_NEXT = "com.codingwithtashi.dailyprayer.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.codingwithtashi.dailyprayer.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.codingwithtashi.dailyprayer.ACTION_STOP"
        const val ACTION_REPEAT = "com.codingwithtashi.dailyprayer.ACTION_REPEAT"
    }

    private val binder = AudioBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var currentPrayer: Prayer? = null
    private var playlistPrayers: List<Prayer> = emptyList()
    private var currentIndex: Int = 0
    private var isRepeatMode: Boolean = false

    private var playerListener: PlayerListener? = null

    // Handler for progress updates
    private val progressHandler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    interface PlayerListener {
        fun onPlaybackStateChanged(isPlaying: Boolean)
        fun onPrayerChanged(prayer: Prayer)
        fun onProgressUpdate(currentPosition: Int, duration: Int)
        fun onRepeatModeChanged(isRepeat: Boolean)
        fun onError(message: String)
    }

    inner class AudioBinder : Binder() {
        fun getService(): AudioPlayerService = this@AudioPlayerService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "Service created")
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "Service bound")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        when (intent?.action) {
            ACTION_PLAY -> play()
            ACTION_PAUSE -> pause()
            ACTION_NEXT -> playNext()
            ACTION_PREVIOUS -> playPrevious()
            ACTION_STOP -> stop()
            ACTION_REPEAT -> toggleRepeat()
        }
        return START_NOT_STICKY
    }

    fun setPlayerListener(listener: PlayerListener?) {
        this.playerListener = listener
        Log.d(TAG, "Player listener set: ${listener != null}")
    }

    fun setPlaylist(prayers: List<Prayer>, startIndex: Int = 0) {
        this.playlistPrayers = prayers.filter { !it.downloadUrl.isNullOrEmpty() }
        this.currentIndex = if (startIndex in playlistPrayers.indices) startIndex else 0
        Log.d(TAG, "Playlist set with ${playlistPrayers.size} prayers, starting at index $currentIndex")

        // Auto-play first prayer if available
        if (playlistPrayers.isNotEmpty() && currentPrayer == null) {
            playPrayer(playlistPrayers[currentIndex])
        }
    }

    fun playPrayer(prayer: Prayer) {
        Log.d(TAG, "Playing prayer: ${prayer.title}")
        currentPrayer = prayer

        // Update index in playlist
        val index = playlistPrayers.indexOfFirst { it.id == prayer.id }
        if (index != -1) {
            currentIndex = index
        }

        // Check if audio is cached locally
        val cachedFile = getCachedAudioFile(prayer)
        if (cachedFile != null && cachedFile.exists()) {
            Log.d(TAG, "Playing from cache: ${cachedFile.absolutePath}")
            prepareAndPlay(cachedFile.absolutePath)
        } else if (!prayer.downloadUrl.isNullOrEmpty()) {
            Log.d(TAG, "Playing from URL: ${prayer.downloadUrl}")
            prepareAndPlay(prayer.downloadUrl!!)
        } else {
            val error = "No audio available for this prayer"
            Log.e(TAG, error)
            playerListener?.onError(error)
            return
        }

        updateNotification()
        playerListener?.onPrayerChanged(prayer)
    }

    fun playPrayerAtIndex(index: Int) {
        Log.d(TAG, "Playing prayer at index: $index")
        if (index in playlistPrayers.indices) {
            currentIndex = index
            playPrayer(playlistPrayers[index])
        } else {
            Log.e(TAG, "Invalid index: $index (playlist size: ${playlistPrayers.size})")
        }
    }

    private fun getCachedAudioFile(prayer: Prayer): File? {
        val cacheDir = File(applicationContext.cacheDir, "prayer_audio")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        // Generate filename from prayer ID
        val fileName = "prayer_${prayer.id}.${getAudioExtension(prayer.downloadUrl)}"
        val cachedFile = File(cacheDir, fileName)

        return if (cachedFile.exists() && cachedFile.length() > 0) {
            Log.d(TAG, "Found cached file: ${cachedFile.absolutePath}")
            cachedFile
        } else {
            null
        }
    }

    private fun getAudioExtension(url: String?): String {
        return when {
            url?.contains(".mp3") == true -> "mp3"
            url?.contains(".m4a") == true -> "m4a"
            url?.contains(".wav") == true -> "wav"
            url?.contains(".ogg") == true -> "ogg"
            else -> "mp3"
        }
    }

    private fun prepareAndPlay(source: String) {
        try {
            // Stop progress updates
            stopProgressUpdates()

            // Release previous player
            mediaPlayer?.release()

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                setOnPreparedListener {
                    Log.d(TAG, "MediaPlayer prepared, duration: ${it.duration}ms")
                    start()
                    playerListener?.onPlaybackStateChanged(true)
                    playerListener?.onProgressUpdate(0, it.duration)
                    startProgressUpdates()
                }

                setOnCompletionListener {
                    Log.d(TAG, "Playback completed")
                    onPlaybackCompleted()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    playerListener?.onError("Error playing audio (code: $what)")
                    stopProgressUpdates()
                    true
                }

                setDataSource(source)
                prepareAsync()
            }

            Log.d(TAG, "MediaPlayer preparing from source: $source")

        } catch (e: IOException) {
            Log.e(TAG, "Error preparing media player", e)
            playerListener?.onError("Error loading audio: ${e.message}")
            stopProgressUpdates()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error preparing media player", e)
            playerListener?.onError("Unexpected error: ${e.message}")
            stopProgressUpdates()
        }
    }

    private fun onPlaybackCompleted() {
        if (isRepeatMode) {
            Log.d(TAG, "Repeat mode active, replaying")
            seekTo(0)
            play()
        } else if (currentIndex < playlistPrayers.size - 1) {
            Log.d(TAG, "Playing next prayer")
            playNext()
        } else {
            Log.d(TAG, "Reached end of playlist")
            playerListener?.onPlaybackStateChanged(false)
            stopProgressUpdates()
        }
    }

    fun play() {
        if (mediaPlayer == null && currentPrayer != null) {
            Log.d(TAG, "Recreating player for current prayer")
            playPrayer(currentPrayer!!)
        } else if (mediaPlayer?.isPlaying == false) {
            Log.d(TAG, "Resuming playback")
            mediaPlayer?.start()
            playerListener?.onPlaybackStateChanged(true)
            updateNotification()
            startProgressUpdates()
        } else {
            Log.d(TAG, "Already playing")
        }
    }

    fun pause() {
        if (mediaPlayer?.isPlaying == true) {
            Log.d(TAG, "Pausing playback")
            mediaPlayer?.pause()
            playerListener?.onPlaybackStateChanged(false)
            updateNotification()
            stopProgressUpdates()
        } else {
            Log.d(TAG, "Not playing, cannot pause")
        }
    }

    fun stop() {
        Log.d(TAG, "Stopping service")
        stopProgressUpdates()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        playerListener?.onPlaybackStateChanged(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    fun playNext() {
        if (playlistPrayers.isEmpty()) {
            Log.w(TAG, "Cannot play next: playlist is empty")
            return
        }

        currentIndex = (currentIndex + 1) % playlistPrayers.size
        Log.d(TAG, "Playing next: index $currentIndex")
        playPrayer(playlistPrayers[currentIndex])
    }

    fun playPrevious() {
        if (playlistPrayers.isEmpty()) {
            Log.w(TAG, "Cannot play previous: playlist is empty")
            return
        }

        // If more than 3 seconds in, restart current prayer
        if (getCurrentPosition() > 3000) {
            Log.d(TAG, "Restarting current prayer")
            seekTo(0)
            play()
            return
        }

        currentIndex = if (currentIndex - 1 < 0) {
            playlistPrayers.size - 1
        } else {
            currentIndex - 1
        }
        Log.d(TAG, "Playing previous: index $currentIndex")
        playPrayer(playlistPrayers[currentIndex])
    }

    fun toggleRepeat() {
        isRepeatMode = !isRepeatMode
        Log.d(TAG, "Repeat mode: $isRepeatMode")
        playerListener?.onRepeatModeChanged(isRepeatMode)
        updateNotification()
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
        Log.d(TAG, "Seeked to position: $position")
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false

    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0

    fun getDuration(): Int = mediaPlayer?.duration ?: 0

    fun getCurrentPrayer(): Prayer? = currentPrayer

    fun isRepeatEnabled(): Boolean = isRepeatMode

    fun getPlaylist(): List<Prayer> = playlistPrayers

    fun getCurrentIndex(): Int = currentIndex

    private fun startProgressUpdates() {
        stopProgressUpdates() // Stop any existing updates

        progressRunnable = object : Runnable {
            override fun run() {
                if (mediaPlayer?.isPlaying == true) {
                    try {
                        val currentPos = getCurrentPosition()
                        val duration = getDuration()
                        playerListener?.onProgressUpdate(currentPos, duration)
                        progressHandler.postDelayed(this, 500) // Update every 500ms
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating progress", e)
                    }
                }
            }
        }
        progressHandler.post(progressRunnable!!)
        Log.d(TAG, "Started progress updates")
    }

    private fun stopProgressUpdates() {
        progressRunnable?.let {
            progressHandler.removeCallbacks(it)
            progressRunnable = null
        }
        Log.d(TAG, "Stopped progress updates")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Prayer Audio Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for prayer audio playback"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    private fun updateNotification() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        openAppIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP

        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseAction = if (isPlaying()) {
            NotificationCompat.Action(
                R.drawable.ic_pause,
                "Pause",
                createPendingIntent(ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                R.drawable.ic_play,
                "Play",
                createPendingIntent(ACTION_PLAY)
            )
        }

        val repeatIcon = if (isRepeatMode) {
            R.drawable.ic_repeat_on
        } else {
            R.drawable.ic_repeat
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentPrayer?.title ?: "Prayer Audio")
            .setContentText(if (isPlaying()) "Playing" else "Paused")
            .setSmallIcon(R.drawable.prayer)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_previous, "Previous", createPendingIntent(ACTION_PREVIOUS))
            .addAction(playPauseAction)
            .addAction(R.drawable.ic_next, "Next", createPendingIntent(ACTION_NEXT))
            .addAction(repeatIcon, "Repeat", createPendingIntent(ACTION_REPEAT))
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isPlaying())
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, AudioPlayerService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        stopProgressUpdates()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "Task removed - continuing playback")
        // Don't stop the service when app is removed from recents
        // Only stop if user explicitly stops playback
        super.onTaskRemoved(rootIntent)
    }
}