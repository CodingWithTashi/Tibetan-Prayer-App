package com.codingwithtashi.dailyprayer.ui.preview

import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.data.model.Prayer
import com.codingwithtashi.dailyprayer.data.model.PrayerCategory
import com.codingwithtashi.dailyprayer.ui.components.DownloadUiState
import com.codingwithtashi.dailyprayer.ui.components.MiniPlayerUi
import com.codingwithtashi.dailyprayer.ui.library.LibraryItem
import com.codingwithtashi.dailyprayer.ui.library.LibraryUiState
import com.codingwithtashi.dailyprayer.ui.listen.ListenItem
import com.codingwithtashi.dailyprayer.ui.listen.ListenUiState
import com.codingwithtashi.dailyprayer.ui.listen.MetaKind
import com.codingwithtashi.dailyprayer.ui.saved.SavedItem
import com.codingwithtashi.dailyprayer.ui.saved.SavedUiState
import com.codingwithtashi.dailyprayer.ui.theme.CategoryKey
import com.codingwithtashi.dailyprayer.ui.today.ContinueReading
import com.codingwithtashi.dailyprayer.ui.today.RoutineSubtitle
import com.codingwithtashi.dailyprayer.ui.today.SavedTile
import com.codingwithtashi.dailyprayer.ui.today.TodayCta
import com.codingwithtashi.dailyprayer.ui.today.TodayRoutine
import com.codingwithtashi.dailyprayer.ui.today.TodayUiState
import com.codingwithtashi.dailyprayer.ui.today.WeekDay
import java.time.DayOfWeek
import java.time.LocalDate

/** Fixture prayer used by previews only. */
internal data class PreviewPrayer(
    val syllable: String,
    val category: CategoryKey,
    val tib: String,
    val en: String,
    val hasAudio: Boolean,
)

/** Preview fixtures: one prayer per category. */
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

    private fun domainPrayer(p: PreviewPrayer, id: Int, saved: Boolean = false) = Prayer(
        id = id, slug = null, title = p.tib, titleEn = p.en, content = "", category = PrayerCategory.valueOf(p.category.name),
        sortOrder = id, audioUrl = "", hasAudio = p.hasAudio, isDownloaded = false, audioPath = null, audioDurationMs = null,
        isSaved = saved, savedLabel = null, savedAt = null, count = 0, lastReadProgress = 0.4f, lastReadAt = null,
        syllable = p.syllable,
    )

    /** Today tab state: a week with a streak, two routines, a continue card and saved tiles. */
    fun today(language: AppLanguage = AppLanguage.EN) = TodayUiState(
        language = language,
        greetingName = "Tashi",
        initial = "T",
        date = LocalDate.of(2026, 6, 15),
        week = DayOfWeek.entries.mapIndexed { i, d -> WeekDay(d, filled = i < 3, isToday = i == 3) },
        streak = "3",
        routines = listOf(
            TodayRoutine(1, "Morning practice", "6:30 AM", RoutineSubtitle.Names("Three Continuous Practices · The Heart Sutra (1/2)"), done = false),
            TodayRoutine(2, "Evening", "8:00 PM", RoutineSubtitle.Complete, done = true),
        ),
        practiceCount = "1 of 2",
        cta = TodayCta.Start(1, "Morning practice", 2),
        continueReading = ContinueReading(domainPrayer(prayers[3], 4), "40%"),
        savedTiles = prayers.take(4).mapIndexed { i, p -> SavedTile(domainPrayer(p, i + 1, saved = true), p.en.removePrefix("The ")) },
        hasSaved = true,
        unreadNotifications = 1,
    )

    fun library() = LibraryUiState(
        items = prayers.mapIndexed { i, p -> LibraryItem(i + 1, p.syllable, p.category, p.en, p.tib, p.hasAudio) },
    )

    fun saved() = SavedUiState(
        items = prayers.take(4).mapIndexed { i, p ->
            SavedItem(i + 1, p.syllable, p.category, p.en, p.tib, p.hasAudio, isPlaying = i == 1, draftLabel = "", titleEn = p.en)
        },
    )

    fun listen(language: AppLanguage = AppLanguage.EN) = ListenUiState(
        items = prayers.filter { it.hasAudio }.mapIndexed { i, p ->
            ListenItem(
                id = i + 1, syllable = p.syllable, category = p.category, en = p.en, tib = p.tib,
                durationLabel = "5:1$i", metaKind = if (i == 0) MetaKind.DOWNLOADED else MetaKind.TIBETAN, percentLabel = null,
                download = if (i == 0) DownloadUiState.Downloaded else DownloadUiState.NotDownloaded,
                isCurrent = i == 1, isPlaying = i == 1,
            )
        },
        downloadedCount = 1,
        storageLabel = "12 MB",
        language = language,
    )
}

