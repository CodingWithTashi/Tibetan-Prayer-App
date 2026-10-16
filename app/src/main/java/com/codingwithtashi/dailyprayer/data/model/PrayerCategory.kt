package com.codingwithtashi.dailyprayer.data.model

enum class PrayerCategory(val key: String) {
    DAILY("daily"),
    SUTRA("sutra"),
    LIFE("life"),
    PRAISE("praise"),
    ASPIRE("aspire"),
    OTHER("other");

    companion object {
        fun fromKey(key: String?): PrayerCategory = entries.firstOrNull { it.key == key } ?: OTHER
    }
}
