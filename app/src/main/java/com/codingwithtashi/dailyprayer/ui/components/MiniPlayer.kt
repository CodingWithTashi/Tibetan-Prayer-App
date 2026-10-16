package com.codingwithtashi.dailyprayer.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.ui.theme.CategoryKey
import com.codingwithtashi.dailyprayer.ui.theme.NowPlayingPalette
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme

/** UI state of the mini player. [subtitle] is the preformatted "Verse i / n" line. */
@Immutable
data class MiniPlayerUi(
    val syllable: String,
    val title: String,
    val subtitle: String,
    val isPlaying: Boolean,
    val progress: Float,
)

/**
 * Mini player card (h62, r16, darkCard): the tile and titles open Now Playing; play/pause and
 * close are separate buttons; a 2dp gold progress bar sits at the bottom. Margin 10/10/8 is built in.
 */
@Composable
fun MiniPlayer(
    state: MiniPlayerUi,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    val shape = RoundedCornerShape(16.dp)
    val openDescription = "${stringResource(R.string.now_playing)}, ${state.title}, ${state.subtitle}"
    Box(
        modifier
            .padding(start = 10.dp, end = 10.dp, bottom = 8.dp)
            .fillMaxWidth()
            .height(62.dp)
            .clip(shape)
            .background(c.darkCard)
            .border(1.dp, c.darkCardBorder, shape),
    ) {
        Row(
            Modifier.fillMaxWidth().fillMaxHeight().padding(start = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(role = Role.Button, onClick = onOpen)
                    .semantics(mergeDescendants = true) { contentDescription = openDescription },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PrayerTile(
                    state.syllable, CategoryKey.DAILY, 42.dp, 10.dp, t.tibetanTile13,
                    containerColor = c.accentSolid, contentColor = NowPlayingPalette.miniSyllable,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(state.title, style = t.body14Semi, color = c.darkCardText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(state.subtitle, style = t.caption12, color = NowPlayingPalette.miniSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            IconActionButton(
                icon = if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                contentDescription = stringResource(if (state.isPlaying) R.string.cd_pause else R.string.cd_play),
                onClick = onPlayPause,
                tint = c.darkCardText,
                iconSize = 20.dp,
            )
            IconActionButton(R.drawable.ic_close, stringResource(R.string.cd_close), onClose, tint = NowPlayingPalette.close, iconSize = 16.dp)
        }
        ProgressBarThin(
            progress = state.progress,
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 10.dp).fillMaxWidth(),
            height = 2.dp,
            trackColor = NowPlayingPalette.miniTrack,
            fillColor = c.gold,
            radius = 1.dp,
            announceProgress = false,
        )
    }
}
