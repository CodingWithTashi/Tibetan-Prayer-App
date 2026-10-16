package com.codingwithtashi.dailyprayer.ui.preview

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.data.model.AppTextSize
import com.codingwithtashi.dailyprayer.ui.components.AppCard
import com.codingwithtashi.dailyprayer.ui.components.AppText
import com.codingwithtashi.dailyprayer.ui.components.AppearanceKind
import com.codingwithtashi.dailyprayer.ui.components.AppearanceOptionCard
import com.codingwithtashi.dailyprayer.ui.components.Avatar
import com.codingwithtashi.dailyprayer.ui.components.BellButton
import com.codingwithtashi.dailyprayer.ui.components.CategoryChip
import com.codingwithtashi.dailyprayer.ui.components.CheckCircle
import com.codingwithtashi.dailyprayer.ui.components.CircleButton
import com.codingwithtashi.dailyprayer.ui.components.ConicProgressRing
import com.codingwithtashi.dailyprayer.ui.components.CounterBar
import com.codingwithtashi.dailyprayer.ui.components.DashedBox
import com.codingwithtashi.dailyprayer.ui.components.DayChip
import com.codingwithtashi.dailyprayer.ui.components.DownloadButton
import com.codingwithtashi.dailyprayer.ui.components.DownloadUiState
import com.codingwithtashi.dailyprayer.ui.components.EditorPrayerRow
import com.codingwithtashi.dailyprayer.ui.components.EmptyState
import com.codingwithtashi.dailyprayer.ui.components.FilterChipPill
import com.codingwithtashi.dailyprayer.ui.components.FlagSize
import com.codingwithtashi.dailyprayer.ui.components.FlagStrip
import com.codingwithtashi.dailyprayer.ui.components.GoalPill
import com.codingwithtashi.dailyprayer.ui.components.IconActionButton
import com.codingwithtashi.dailyprayer.ui.components.LanguageOption
import com.codingwithtashi.dailyprayer.ui.components.LibraryRow
import com.codingwithtashi.dailyprayer.ui.components.ListenRow
import com.codingwithtashi.dailyprayer.ui.components.MiniPlayer
import com.codingwithtashi.dailyprayer.ui.components.Overline
import com.codingwithtashi.dailyprayer.ui.components.PickRow
import com.codingwithtashi.dailyprayer.ui.components.PillButton
import com.codingwithtashi.dailyprayer.ui.components.PillVariant
import com.codingwithtashi.dailyprayer.ui.components.PlayCircle
import com.codingwithtashi.dailyprayer.ui.components.PrayerBottomBar
import com.codingwithtashi.dailyprayer.ui.components.PrayerInput
import com.codingwithtashi.dailyprayer.ui.components.PrayerNavRail
import com.codingwithtashi.dailyprayer.ui.components.PrayerTile
import com.codingwithtashi.dailyprayer.ui.components.PrayerToast
import com.codingwithtashi.dailyprayer.ui.components.ProgressBarThin
import com.codingwithtashi.dailyprayer.ui.components.RoutinePrayerRow
import com.codingwithtashi.dailyprayer.ui.components.SavedRowEdit
import com.codingwithtashi.dailyprayer.ui.components.SavedRowView
import com.codingwithtashi.dailyprayer.ui.components.ScreenTitle
import com.codingwithtashi.dailyprayer.ui.components.SectionLabel
import com.codingwithtashi.dailyprayer.ui.components.SegmentedControl
import com.codingwithtashi.dailyprayer.ui.components.SettingsRow
import com.codingwithtashi.dailyprayer.ui.components.SettingsTrailing
import com.codingwithtashi.dailyprayer.ui.components.SheetHandle
import com.codingwithtashi.dailyprayer.ui.components.SmallPill
import com.codingwithtashi.dailyprayer.ui.components.SquareCheck
import com.codingwithtashi.dailyprayer.ui.components.TabSpec
import com.codingwithtashi.dailyprayer.ui.components.TibetanText
import com.codingwithtashi.dailyprayer.ui.components.TimeChip
import com.codingwithtashi.dailyprayer.ui.components.ToggleSwitch
import com.codingwithtashi.dailyprayer.ui.components.TopBar
import com.codingwithtashi.dailyprayer.ui.components.avatarInitial
import com.codingwithtashi.dailyprayer.ui.components.contentWidth
import com.codingwithtashi.dailyprayer.ui.theme.CategoryKey
import com.codingwithtashi.dailyprayer.ui.theme.Jomolhari
import com.codingwithtashi.dailyprayer.ui.theme.PrayerDimens
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme
import com.codingwithtashi.dailyprayer.ui.theme.category

private val tabs = listOf(
    TabSpec(R.string.tab_today, R.drawable.ic_sunrise),
    TabSpec(R.string.tab_library, R.drawable.ic_library),
    TabSpec(R.string.tab_saved, R.drawable.ic_heart),
    TabSpec(R.string.tab_listen, R.drawable.ic_headphones),
)

@Composable
private fun GallerySection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(title)
        content()
    }
}

/** Every component in the current theme (no outer chrome); used twice by [ComponentGallery]. */
@Composable
private fun GalleryBody() {
    val c = PrayerTheme.colors
    var checked by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var text by remember { mutableStateOf("") }
    val p = PreviewData.prayers
    val noop = {}
    Column(
        Modifier.background(c.background).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ScreenTitle("Library")
        GallerySection("Tiles, flags, avatar") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                p.forEach { PrayerTile(it.syllable, it.category, 44.dp, 12.dp, PrayerTheme.type.tileGlyphSmall) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                p.forEach { PrayerTile(it.syllable, it.category, 44.dp, 12.dp, PrayerTheme.type.tileGlyphSmall, darkVariant = true) }
            }
            FlagStrip(FlagSize.Large)
            FlagStrip(FlagSize.Small)
            FlagStrip(FlagSize.Tiny)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(40.dp, avatarInitial("tashi"))
                Avatar(56.dp, avatarInitial(null))
                BellButton(unread = true, onClick = noop)
                BellButton(unread = false, onClick = noop)
            }
        }
        GallerySection("Buttons") {
            PillButton("Start · Morning practice", noop, Modifier.fillMaxWidth(), height = 54.dp)
            PillButton("Tonal", noop, variant = PillVariant.Tonal)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("Edit", noop, variant = PillVariant.Outlined(), height = 36.dp)
                PillButton("Listen", noop, variant = PillVariant.Outlined(1.5.dp), height = 52.dp, leadingIcon = R.drawable.ic_play, leadingIconSize = 14.dp)
                PillButton("Text", noop, variant = PillVariant.Text, height = 40.dp)
                PillButton("Disabled", noop, enabled = false, height = 40.dp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconActionButton(R.drawable.ic_heart, "Save", noop)
                IconActionButton(R.drawable.ic_heart_filled, "Unsave", noop, tint = c.accent)
                IconActionButton("Reading settings", noop) { AppText(stringResource(R.string.reader_settings_glyph), style = PrayerTheme.type.aaGlyph, color = c.ink) }
                CircleButton(46.dp, "Minus", noop, border = androidx.compose.foundation.BorderStroke(1.dp, c.border)) {
                    androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_minus), null, Modifier.size(18.dp), tint = c.ink)
                }
                PlayCircle(44.dp, isCurrent = false, isPlaying = false, onClick = noop)
                PlayCircle(40.dp, isCurrent = true, isPlaying = true, onClick = noop)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SmallPill(noop, "Speed") { AppText("1.0×", style = PrayerTheme.type.bodySmallStrong, color = c.ink) }
                DownloadButton(DownloadUiState.NotDownloaded, noop)
                DownloadButton(DownloadUiState.Downloading(0.42f), noop)
                DownloadButton(DownloadUiState.Downloaded, noop)
                TimeChip("6:30 AM", noop)
            }
        }
        GallerySection("Selection controls") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CheckCircle(PrayerDimens.checkCircleSmallMin, true)
                CheckCircle(PrayerDimens.checkCircleSmallMin, false)
                CheckCircle(PrayerDimens.checkCircleLargeMin, false, number = "2")
                SquareCheck(true)
                SquareCheck(false)
                ToggleSwitch(checked, { checked = it })
                ToggleSwitch(false, { })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                CategoryChip("All", true, noop)
                CategoryChip("Daily", false, noop, dot = c.category(CategoryKey.DAILY).dot)
                FilterChipPill("Downloaded · 3", false, noop)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                PreviewData.weekDays.forEachIndexed { i, d -> DayChip(d, i % 2 == 0, noop, d) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("21", "108", "1k", "10k").forEachIndexed { i, g -> GoalPill(g, i == 1, noop, Modifier.weight(1f)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LanguageOption("English", true, noop, PrayerTheme.type.leadStrong, Modifier.weight(1f))
                LanguageOption("བོད་ཡིག", false, noop, PrayerTheme.type.leadStrong, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppearanceOptionCard(AppearanceKind.System, "System", true, noop, Modifier.weight(1f))
                AppearanceOptionCard(AppearanceKind.Light, "Light", false, noop, Modifier.weight(1f))
                AppearanceOptionCard(AppearanceKind.Dark, "Dark", false, noop, Modifier.weight(1f))
            }
            var seg by remember { mutableIntStateOf(0) }
            val type = PrayerTheme.type
            SegmentedControl(listOf("English", "བོད་ཡིག"), seg, { seg = it }, { type.leadStrong })
        }
        GallerySection("Text, bars, inputs") {
            Overline("Playing from")
            TibetanText("རྒྱུན་ཆགས་གསུམ་པ།", PrayerTheme.type.tibetanHero, color = c.accent)
            AppText("21 / 108", style = PrayerTheme.type.counter, maxLines = 1)
            ProgressBarThin(0.4f, height = 6.dp)
            ProgressBarThin(0.7f, height = 2.dp)
            ConicProgressRing(0.6f)
            SheetHandle(Modifier.align(Alignment.CenterHorizontally))
            PrayerInput(text, { text = it }, PrayerDimens.inputLarge, 16.dp, PrayerTheme.type.inputLarge, placeholder = "Your name", maxLength = 24)
            DashedBox(Modifier.fillMaxWidth().heightIn(min = PrayerDimens.dashedBoxMin), radius = 14.dp) {
                AppText("Add prayers", style = PrayerTheme.type.bodyStrong, color = c.accent)
            }
            EmptyState(R.drawable.ic_heart, "Nothing saved yet", button = "Browse library" to noop)
        }
        GallerySection("Top bar and rows") {
            Box(Modifier.background(c.surface)) {
                TopBar(onBack = noop, center = { AppText("Settings", style = PrayerTheme.type.bodyLargeStrong, color = c.ink) }, actions = {
                    IconActionButton(R.drawable.ic_heart, "Save", noop)
                })
            }
            Column(Modifier.contentWidth()) {
                LibraryRow(p[0].syllable, p[0].category, p[0].tib, p[0].en, true, noop)
                PickRow(p[1].syllable, p[1].category, p[1].tib, p[1].en, checked = true, onToggle = noop)
                PickRow(p[2].syllable, p[2].category, p[2].tib, p[2].en, checked = false, onToggle = noop, compact = true)
                RoutinePrayerRow(0, true, p[3].tib, "${p[3].en} · 12 min", noop)
                RoutinePrayerRow(1, false, p[4].tib, "${p[4].en} · 5 min", noop)
                EditorPrayerRow(p[0].syllable, p[0].category, p[0].tib, p[0].en, canMoveUp = false, onMoveUp = noop, onRemove = noop)
                EditorPrayerRow(p[1].syllable, p[1].category, p[1].tib, p[1].en, canMoveUp = true, onMoveUp = noop, onRemove = noop)
                SavedRowView(p[3].syllable, p[3].category, "Tara", p[3].tib, hasAudio = true, isPlaying = false, onOpen = noop, onPlayPause = noop)
                SavedRowEdit(p[3].syllable, p[3].category, "Tara", { }, p[3].en, p[3].tib, noop, noop)
                ListenRow(p[3].syllable, p[3].category, p[3].en, "3:42 · Streaming", DownloadUiState.NotDownloaded, false, false, noop, noop)
                ListenRow(p[1].syllable, p[1].category, p[1].en, "5:10 · Downloaded", DownloadUiState.Downloaded, true, true, noop, noop, showDivider = false)
            }
        }
        GallerySection("Settings") {
            Column(
                Modifier.fillMaxWidth().background(c.surface, RoundedCornerShape(18.dp)).padding(horizontal = 16.dp),
            ) {
                SettingsRow(R.drawable.ic_text_size, "Reading", sub = "Text size 18", trailing = SettingsTrailing.Chevron, onClick = noop)
                SettingsRow(R.drawable.ic_bell, "Reminders", sub = "Daily at your set time", trailing = SettingsTrailing.Switch(true) {})
                SettingsRow(R.drawable.ic_download, "Downloaded", trailing = SettingsTrailing.Value("3"), onClick = noop)
                SettingsRow(R.drawable.ic_exit, "Exit", tint = c.danger, labelColor = c.danger, onClick = noop, showDivider = false)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppCard("ཚིག", "Tibetan Dictionary", "Look up any word", c.appDictBg, c.appDictTile, c.appDictSub, noop, Modifier.weight(1f))
                AppCard("ལོ", "Tibetan Calendar", "Holy days & dates", c.appCalBg, c.appCalTile, c.appCalSub, noop, Modifier.weight(1f))
            }
        }
        GallerySection("Player, counter, toast") {
            MiniPlayer(PreviewData.miniPlayer, noop, noop, noop)
            CounterBar("21", "108", 0.2f, noop, noop, noop, resetEnabled = true)
            PrayerToast(object : SnackbarData {
                override val visuals = object : SnackbarVisuals {
                    override val message = "Removed from saved"
                    override val actionLabel = "Undo"
                    override val withDismissAction = false
                    override val duration = androidx.compose.material3.SnackbarDuration.Short
                }
                override fun performAction() = Unit
                override fun dismiss() = Unit
            })
        }
        GallerySection("Navigation") {
            Box(Modifier.fillMaxWidth()) { PrayerBottomBar(tabs, selectedTab, { selectedTab = it }) }
            Box(Modifier.height(280.dp)) { PrayerNavRail(tabs, selectedTab, { selectedTab = it }) }
        }
    }
}

/** One row per [PrayerTypography] token: its name and a sample line (Tibetan for Jomolhari tokens). */
@Composable
private fun TypeScaleSection() {
    val c = PrayerTheme.colors
    val type = PrayerTheme.type
    GallerySection("Type scale") {
        type.all.forEach { (name, style) ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AppText("$name · ${style.fontSize.value.toInt()}sp", style = type.caption, color = c.textMuted)
                val sample = if (style.fontFamily == Jomolhari) "བཀྲ་ཤིས་བདེ་ལེགས། ༡༢༣" else "Daily prayer 108"
                AppText(sample, style = style, color = c.ink)
            }
        }
    }
}

/**
 * Gallery of every shared component, light block above dark block. The type scale is shown once,
 * above the light block. [textScale] is the app-wide Text size multiplier.
 */
@Composable
fun ComponentGallery(modifier: Modifier = Modifier, textScale: Float = 1f) {
    Column(modifier) {
        PrayerTheme(darkTheme = false, textScale = textScale) {
            Column(Modifier.background(PrayerTheme.colors.background).padding(horizontal = 20.dp, vertical = 16.dp)) { TypeScaleSection() }
            GalleryBody()
        }
        PrayerTheme(darkTheme = true, textScale = textScale) { GalleryBody() }
    }
}

// Canvases are tall because the type scale and the larger text sizes lengthen the gallery.
@Preview(name = "light", widthDp = 360, heightDp = 10000, showBackground = true)
@Composable
private fun GalleryLightPreview() = ComponentGallery()

@Preview(name = "dark", widthDp = 360, heightDp = 10000, uiMode = UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun GalleryDarkPreview() = ComponentGallery()

@Preview(name = "bo", widthDp = 360, heightDp = 10000, locale = "bo", showBackground = true)
@Composable
private fun GalleryBoPreview() = ComponentGallery()

@Preview(name = "text size Small", widthDp = 360, heightDp = 10000, showBackground = true)
@Composable
private fun GalleryTextSmallPreview() = ComponentGallery(textScale = AppTextSize.SMALL.scale)

@Preview(name = "text size Largest", widthDp = 360, heightDp = 13000, showBackground = true)
@Composable
private fun GalleryTextLargestPreview() = ComponentGallery(textScale = AppTextSize.LARGEST.scale)

@Preview(name = "fontScale1.3", widthDp = 360, heightDp = 12000, fontScale = 1.3f, showBackground = true)
@Composable
private fun GalleryFontScalePreview() = ComponentGallery()

@Preview(name = "fontScale1.5", widthDp = 360, heightDp = 14000, fontScale = 1.5f, showBackground = true)
@Composable
private fun GalleryFontScale15Preview() = ComponentGallery()
