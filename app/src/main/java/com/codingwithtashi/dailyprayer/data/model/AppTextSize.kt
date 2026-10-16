package com.codingwithtashi.dailyprayer.data.model

/**
 * App-wide Text size setting. [scale] multiplies every font size and line height in the UI
 * (on top of the system font scale). [SMALL] is roughly the pre-3.0 look.
 */
enum class AppTextSize(val scale: Float) {
    SMALL(0.85f),
    DEFAULT(1f),
    LARGE(1.15f),
    LARGEST(1.3f),
    ;

    companion object {
        /** Parses a stored enum name; unknown or missing values fall back to [DEFAULT]. */
        fun fromName(name: String?): AppTextSize = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
