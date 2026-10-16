package com.codingwithtashi.dailyprayer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.ui.theme.CategoryKey
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme

private val RowVerticalPadding = 8.dp

/** Library list row (min 66): tile 44, titles, headphones when audio exists. */
@Composable
fun LibraryRow(
    syllable: String,
    category: CategoryKey,
    tib: String,
    en: String,
    hasAudio: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    Row(
        modifier
            .fillMaxWidth()
            .bottomDivider(c.divider, showDivider)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 66.dp)
            .padding(vertical = RowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PrayerTile(syllable, category, 44.dp, 12.dp, t.tibetanTile14)
        Column(Modifier.weight(1f)) {
            TibetanText(tib, t.tibetanList, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(en, style = t.caption12, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (hasAudio) {
            Icon(painterResource(R.drawable.ic_headphones), stringResource(R.string.cd_has_audio), Modifier.size(18.dp), tint = c.textSubtle)
        }
    }
}

/**
 * Selectable prayer row with a [SquareCheck] (onboarding picks, editor picker). [compact] gives the
 * picker sheet's 60dp / 40dp-tile variant.
 */
@Composable
fun PickRow(
    syllable: String,
    category: CategoryKey,
    tib: String,
    en: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showDivider: Boolean = true,
) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    Row(
        modifier
            .fillMaxWidth()
            .bottomDivider(c.divider, showDivider)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .heightIn(min = if (compact) 60.dp else 62.dp)
            .padding(vertical = RowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 14.dp),
    ) {
        if (compact) PrayerTile(syllable, category, 40.dp, 11.dp, t.tibetanTile13)
        else PrayerTile(syllable, category, 42.dp, 12.dp, t.tibetanTile14)
        Column(Modifier.weight(1f)) {
            TibetanText(tib, if (compact) t.tibetanListSmall else t.tibetanList, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(en, style = t.caption12, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        SquareCheck(checked)
    }
}

/**
 * Routine detail row (min 68): step circle ([numberText] defaults to index + 1), Tibetan title,
 * "[enWithMinutes]" caption, chevron. Announces Done / Not done.
 */
@Composable
fun RoutinePrayerRow(
    index: Int,
    done: Boolean,
    tib: String,
    enWithMinutes: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    numberText: String = (index + 1).toString(),
    showDivider: Boolean = true,
) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    val state = stringResource(if (done) R.string.state_done else R.string.state_not_done)
    Row(
        modifier
            .fillMaxWidth()
            .bottomDivider(c.divider, showDivider)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { stateDescription = state }
            .heightIn(min = 68.dp)
            .padding(vertical = RowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CheckCircle(26.dp, done, number = numberText)
        Column(Modifier.weight(1f)) {
            TibetanText(tib, t.tibetanList, color = if (done) c.textMuted else c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(enWithMinutes, style = t.caption12, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(16.dp), tint = c.chevron)
    }
}

/** Routine editor row (min 60): tile 40, titles, move-up (space kept when hidden) and remove. */
@Composable
fun EditorPrayerRow(
    syllable: String,
    category: CategoryKey,
    tib: String,
    en: String,
    canMoveUp: Boolean,
    onMoveUp: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    Row(
        modifier
            .fillMaxWidth()
            .bottomDivider(c.divider, showDivider)
            .heightIn(min = 60.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PrayerTile(syllable, category, 40.dp, 11.dp, t.tibetanTile13)
        Column(Modifier.weight(1f).padding(vertical = RowVerticalPadding)) {
            TibetanText(tib, t.tibetanListSmall, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(en, style = t.caption12, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (canMoveUp) {
            IconActionButton(R.drawable.ic_arrow_up, stringResource(R.string.cd_move_up), onMoveUp, tint = c.textSubtle, iconSize = 18.dp)
        } else {
            Spacer(Modifier.size(48.dp))
        }
        IconActionButton(R.drawable.ic_minus, stringResource(R.string.cd_remove), onRemove, tint = c.danger, iconSize = 18.dp)
    }
}

/**
 * Saved list row, viewing mode (min 70): tile + titles open the prayer; the play circle (when
 * [hasAudio]) toggles playback.
 */
@Composable
fun SavedRowView(
    syllable: String,
    category: CategoryKey,
    label: String,
    tib: String,
    hasAudio: Boolean,
    isPlaying: Boolean,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    Row(
        modifier
            .fillMaxWidth()
            .bottomDivider(c.divider, showDivider)
            .heightIn(min = 70.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(role = Role.Button, onClick = onOpen)
                .padding(vertical = RowVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PrayerTile(syllable, category, 48.dp, 14.dp, t.tibetanTile15)
            Column(Modifier.weight(1f)) {
                Text(label, style = t.body15Semi, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                TibetanText(tib, t.tibetanCaption, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (hasAudio) PlayCircle(44.dp, isCurrent = false, isPlaying = isPlaying, onClick = onPlayPause)
    }
}

/** Saved list row, editing mode (min 70): rename field under the tile, trash button at the end. */
@Composable
fun SavedRowEdit(
    syllable: String,
    category: CategoryKey,
    name: String,
    onNameChange: (String) -> Unit,
    namePlaceholder: String,
    tib: String,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    Row(
        modifier
            .fillMaxWidth()
            .bottomDivider(c.divider, showDivider)
            .heightIn(min = 70.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box48(onOpen) { PrayerTile(syllable, category, 48.dp, 14.dp, t.tibetanTile15) }
        Column(Modifier.weight(1f).padding(vertical = RowVerticalPadding), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PrayerInput(name, onNameChange, 38.dp, 10.dp, t.input15, placeholder = namePlaceholder, maxLength = 28)
            TibetanText(tib, t.tibetanCaptionSmall, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconActionButton(R.drawable.ic_trash, stringResource(R.string.cd_remove), onRemove, tint = c.danger, iconSize = 20.dp)
    }
}

@Composable
private fun Box48(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.clickable(role = Role.Button, onClick = onClick)) { content() }
}

/**
 * Listen list row (min 68): tile + titles start playback, [DownloadButton] and [PlayCircle] at the
 * end. [meta] is the already-formatted "m:ss · status" caption.
 */
@Composable
fun ListenRow(
    syllable: String,
    category: CategoryKey,
    en: String,
    meta: String,
    downloadState: DownloadUiState,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onDownloadToggle: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    Row(
        modifier
            .fillMaxWidth()
            .bottomDivider(c.divider, showDivider)
            .heightIn(min = 68.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(role = Role.Button, onClick = onPlay)
                .padding(vertical = RowVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrayerTile(syllable, category, 48.dp, 14.dp, t.tibetanTile15)
            Column(Modifier.weight(1f)) {
                Text(en, style = t.body15Semi, color = if (isCurrent) c.accent else c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(meta, style = t.caption12, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        DownloadButton(downloadState, onDownloadToggle)
        PlayCircle(40.dp, isCurrent = isCurrent, isPlaying = isCurrent && isPlaying, onClick = onPlay)
    }
}
