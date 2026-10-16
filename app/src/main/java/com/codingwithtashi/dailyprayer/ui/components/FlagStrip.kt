package com.codingwithtashi.dailyprayer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme

/** The three flag-strip sizes. */
enum class FlagSize { Large, Small, Tiny }

/** Five coloured flag swatches (decorative, no content description). Large also draws the rotated line. */
@Composable
fun FlagStrip(size: FlagSize, modifier: Modifier = Modifier) {
    val c = PrayerTheme.colors
    val colors = listOf(c.flagBlue, c.flagWhite, c.flagRed, c.flagGreen, c.flagYellow)
    val lineColor = c.textSubtle
    val whiteEdge = if (size == FlagSize.Small) c.borderMuted else c.borderStrong
    val box = when (size) {
        FlagSize.Large -> 180.dp to 40.dp
        FlagSize.Small -> 74.dp to 16.dp
        FlagSize.Tiny -> 8.dp * 5 + 3.dp * 4 to 10.dp
    }
    Canvas(modifier.size(box.first, box.second).clearAndSetSemantics { }) {
        val (sw, sh, gap, x0, y0) = when (size) {
            FlagSize.Large -> floatArrayOf(22.dp.toPx(), 28.dp.toPx(), 8.dp.toPx(), 12.dp.toPx(), 5.dp.toPx())
            FlagSize.Small -> floatArrayOf(10.dp.toPx(), 12.dp.toPx(), 4.dp.toPx(), 4.dp.toPx(), 2.dp.toPx())
            FlagSize.Tiny -> floatArrayOf(8.dp.toPx(), 10.dp.toPx(), 3.dp.toPx(), 0f, 0f)
        }.let { Five(it[0], it[1], it[2], it[3], it[4]) }
        when (size) {
            FlagSize.Large -> {
                val h = 1.5.dp.toPx()
                val top = 6.dp.toPx()
                rotate(-2f, pivot = Offset(this.size.width / 2f, top + h / 2f)) {
                    drawRect(lineColor, Offset(0f, top), Size(this.size.width, h))
                }
            }
            FlagSize.Small -> drawRect(lineColor, Offset(0f, 2.dp.toPx()), Size(this.size.width, 1.dp.toPx()))
            FlagSize.Tiny -> Unit
        }
        colors.forEachIndexed { i, color ->
            val left = x0 + i * (sw + gap)
            drawRect(color, Offset(left, y0), Size(sw, sh))
            if (i == 1) {
                val s = 1.dp.toPx()
                drawRect(whiteEdge, Offset(left + s / 2, y0 + s / 2), Size(sw - s, sh - s), style = Stroke(s))
            }
        }
    }
}

private data class Five(val a: Float, val b: Float, val c: Float, val d: Float, val e: Float)
