package com.codingwithtashi.dailyprayer.data.model

enum class DownloadError { OFFLINE, NETWORK, DISK, CANCELLED, UNKNOWN }

sealed interface DownloadState {
    data object NotDownloaded : DownloadState
    data class Downloading(val progress: Float, val bytes: Long, val total: Long) : DownloadState
    data object Downloaded : DownloadState
    data class Failed(val error: DownloadError) : DownloadState
}
