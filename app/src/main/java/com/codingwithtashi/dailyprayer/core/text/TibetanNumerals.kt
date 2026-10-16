package com.codingwithtashi.dailyprayer.core.text

import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import kotlin.math.roundToInt

/** Converts Western digits to Tibetan digits and formats counts per the app language. */
object TibetanNumerals {
    private const val ZERO = '༠'

    fun toTibetan(n: Int): String = toTibetan(n.toString())

    fun toTibetan(n: Long): String = toTibetan(n.toString())

    fun toTibetan(digits: String): String = buildString(digits.length) {
        for (c in digits) append(if (c in '0'..'9') ZERO + (c - '0') else c)
    }

    fun formatCount(n: Int, language: AppLanguage): String =
        if (language.usesTibetanDigits) toTibetan(n) else n.toString()

    fun formatPercent(fraction: Float, language: AppLanguage): String {
        val pct = (fraction * 100f).roundToInt()
        return formatCount(pct, language) + "%"
    }

    /** Only the number; the "min" word comes from resources. */
    fun formatMinutes(n: Int, language: AppLanguage): String = formatCount(n, language)
}
