package com.codingwithtashi.dailyprayer.ui.preview

import com.codingwithtashi.dailyprayer.ui.components.MiniPlayerUi
import com.codingwithtashi.dailyprayer.ui.theme.CategoryKey

/** Fixture prayer used by previews only. */
internal data class PreviewPrayer(
    val syllable: String,
    val category: CategoryKey,
    val tib: String,
    val en: String,
    val hasAudio: Boolean,
)

/** Preview fixtures: one prayer per category (titles from the catalogue table in the plan). */
internal object PreviewData {
    val prayers = listOf(
        PreviewPrayer("རྒྱུན", CategoryKey.DAILY, "རྒྱུན་ཆགས་གསུམ་པ།", "Three Continuous Practices", true),
        PreviewPrayer("ཤེས", CategoryKey.SUTRA, "ཤེས་རབ་སྙིང་པོ།", "The Heart Sutra", true),
        PreviewPrayer("ཞབས", CategoryKey.LIFE, "ཞབས་བརྟན།", "Long-life Prayer for His Holiness", false),
        PreviewPrayer("སྒྲོལ", CategoryKey.PRAISE, "སྒྲོལ་མ།", "21 Praises to Tara", true),
        PreviewPrayer("བདེན", CategoryKey.ASPIRE, "བདེན་ཚིག་སྨོན་ལམ།", "Words of Truth", true),
        PreviewPrayer("ཁ", CategoryKey.OTHER, "ཁ་འདོན།", "Other Prayer", false),
    )

    val miniPlayer = MiniPlayerUi(
        syllable = "སྒྲོལ",
        title = "21 Praises to Tara",
        subtitle = "Verse 4 / 21",
        isPlaying = true,
        progress = 0.35f,
    )

    val weekDays = listOf("M", "T", "W", "T", "F", "S", "S")
}
