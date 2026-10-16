package com.codingwithtashi.dailyprayer.playback

enum class RepeatMode {
    OFF, THREE, SEVEN, INFINITE;

    /** Cycles OFF -> THREE -> SEVEN -> INFINITE -> OFF. */
    fun next(): RepeatMode = entries[(ordinal + 1) % entries.size]
}
