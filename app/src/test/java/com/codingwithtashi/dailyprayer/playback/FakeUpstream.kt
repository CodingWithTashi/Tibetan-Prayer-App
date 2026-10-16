package com.codingwithtashi.dailyprayer.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/** Serves [data] for any Uri, honouring position/length, and counts opens and bytes served. */
@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
class FakeUpstream(
    val data: ByteArray,
    private val failure: IOException? = null,
    private val readDelayMs: Long = 0,
) : DataSource.Factory {
    val opens = AtomicInteger()
    val bytesServed = AtomicLong()

    override fun createDataSource(): DataSource = object : BaseDataSource(false) {
        private var uri: Uri? = null
        private var pos = 0
        private var end = 0

        override fun open(dataSpec: DataSpec): Long {
            opens.incrementAndGet()
            failure?.let { throw it }
            uri = dataSpec.uri
            pos = dataSpec.position.toInt()
            end = if (dataSpec.length == C.LENGTH_UNSET.toLong()) data.size
            else minOf(data.size.toLong(), dataSpec.position + dataSpec.length).toInt()
            return (end - pos).toLong()
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (length == 0) return 0
            if (pos >= end) return C.RESULT_END_OF_INPUT
            if (readDelayMs > 0) Thread.sleep(readDelayMs)
            val n = minOf(length, minOf(end - pos, 4096))
            System.arraycopy(data, pos, buffer, offset, n)
            pos += n
            bytesServed.addAndGet(n.toLong())
            return n
        }

        override fun getUri(): Uri? = uri
        override fun close() { uri = null }
    }
}
