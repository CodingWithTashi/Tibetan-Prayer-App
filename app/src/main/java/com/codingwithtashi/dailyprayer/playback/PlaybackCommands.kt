package com.codingwithtashi.dailyprayer.playback

import android.os.Bundle
import androidx.media3.session.SessionCommand

object PlaybackCommands {
    const val CYCLE_REPEAT_MODE = "com.codingwithtashi.dailyprayer.CYCLE_REPEAT_MODE"
    const val SET_REPEAT_MODE = "com.codingwithtashi.dailyprayer.SET_REPEAT_MODE"

    const val KEY_REPEAT_MODE = "repeat_mode"
    const val KEY_REPEATS_LEFT = "repeats_left"
    const val KEY_ERROR = "error"
    const val KEY_PRAYER_ID = "prayer_id"
    const val KEY_ROUTINE_ID = "routine_id"
    const val KEY_CONTEXT_LABEL = "context_label"
    const val KEY_SOURCE = "source"

    val cycleRepeat = SessionCommand(CYCLE_REPEAT_MODE, Bundle.EMPTY)
    val setRepeat = SessionCommand(SET_REPEAT_MODE, Bundle.EMPTY)
}
