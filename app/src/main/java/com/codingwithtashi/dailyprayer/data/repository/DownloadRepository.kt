package com.codingwithtashi.dailyprayer.data.repository

import com.codingwithtashi.dailyprayer.data.model.DownloadState
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun state(prayerId: Int): Flow<DownloadState>
    fun download(prayerId: Int)
    fun cancel(prayerId: Int)
    suspend fun delete(prayerId: Int)
    fun downloadedCount(): Flow<Int>
    fun totalBytes(): Flow<Long>

    /** "12.3 MB" (one decimal, English digits). */
    fun storageLabel(bytes: Long): String
}
