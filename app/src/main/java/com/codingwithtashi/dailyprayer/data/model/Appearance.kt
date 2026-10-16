package com.codingwithtashi.dailyprayer.data.model

enum class Appearance(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String?): Appearance = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}
