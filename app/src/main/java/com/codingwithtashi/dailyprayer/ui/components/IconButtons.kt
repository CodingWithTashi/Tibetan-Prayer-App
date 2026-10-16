package com.codingwithtashi.dailyprayer.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.core.text.TibetanNumerals
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme

/** 48x48 icon button with an unbounded 22dp ripple; [contentDescription] is mandatory. */
@Composable
fun IconActionButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = PrayerTheme.colors.ink,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp,
    enabled: Boolean = true,
) {
    IconActionButton(contentDescription, onClick, modifier, size, enabled) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Icon button variant with a custom [content] slot (e.g. the "Aa" glyph). */
@Composable
fun IconActionButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .semantics { this.contentDescription = contentDescription }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 22.dp),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Circular button (counter +/-, text-size controls); at least 48dp touch target. */
@Composable
fun CircleButton(
    size: Dp,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    border: BorderStroke? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .then(if (border != null) Modifier.border(border, CircleShape) else Modifier)
            .semantics { this.contentDescription = contentDescription }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Notifications bell (40dp circle, 1dp border) with an 8dp accent dot when [unread]. */
@Composable
fun BellButton(unread: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = PrayerTheme.colors
    Box(modifier.minimumInteractiveComponentSize()) {
        CircleButton(
            size = 40.dp,
            contentDescription = stringResource(R.string.cd_notifications),
            onClick = onClick,
            border = BorderStroke(1.dp, c.border),
        ) {
            Icon(painterResource(R.drawable.ic_bell), contentDescription = null, tint = c.ink, modifier = Modifier.size(20.dp))
        }
        if (unread) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
                    .size(8.dp)
                    .background(c.accent, CircleShape),
            )
        }
    }
}

/** Round play/pause button: accent when [isCurrent], surfaceWarm otherwise; icon shows pause while [isPlaying]. */
@Composable
fun PlayCircle(
    size: Dp,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = PrayerTheme.colors
    val iconSize = if (size >= 44.dp) 16.dp else 15.dp
    CircleButton(
        size = size,
        contentDescription = stringResource(if (isPlaying) R.string.cd_pause else R.string.cd_play),
        onClick = onClick,
        modifier = modifier,
        containerColor = if (isCurrent) c.accent else c.surfaceWarm,
    ) {
        Icon(
            painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
            contentDescription = null,
            tint = if (isCurrent) c.onAccent else c.accent,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** Download state of one prayer's audio. */
sealed interface DownloadUiState {
    data object NotDownloaded : DownloadUiState
    data class Downloading(val progress: Float) : DownloadUiState
    data object Downloaded : DownloadUiState
}

/** 48dp download control: arrow, progress ring with stop square, or green check. */
@Composable
fun DownloadButton(state: DownloadUiState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = PrayerTheme.colors
    val cd = stringResource(
        when (state) {
            DownloadUiState.NotDownloaded -> R.string.cd_download
            is DownloadUiState.Downloading -> R.string.cd_cancel_download
            DownloadUiState.Downloaded -> R.string.cd_remove_download
        },
    )
    val stateText = when (state) {
        is DownloadUiState.Downloading -> stringResource(
            R.string.state_downloading_pct,
            TibetanNumerals.formatCount((state.progress * 100).toInt(), PrayerTheme.language),
        )
        DownloadUiState.Downloaded -> stringResource(R.string.downloaded)
        DownloadUiState.NotDownloaded -> null
    }
    Box(
        modifier = modifier
            .size(48.dp)
            .semantics {
                contentDescription = cd
                if (stateText != null) stateDescription = stateText
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 22.dp),
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            DownloadUiState.NotDownloaded ->
                Icon(painterResource(R.drawable.ic_download), null, Modifier.size(18.dp), tint = c.accent)
            is DownloadUiState.Downloading -> {
                ConicProgressRing(progress = state.progress, size = 28.dp, stroke = 3.dp)
                Icon(painterResource(R.drawable.ic_stop_small), null, Modifier.size(24.dp), tint = c.accent)
            }
            DownloadUiState.Downloaded ->
                Icon(painterResource(R.drawable.ic_check), null, Modifier.size(18.dp), tint = c.success)
        }
    }
}
