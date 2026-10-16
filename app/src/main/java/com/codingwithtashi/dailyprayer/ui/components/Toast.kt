package com.codingwithtashi.dailyprayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme

/** Custom snackbar content: darkCard, r16, min height 50, bodyMedium text and a gold Undo action. */
@Composable
internal fun PrayerToast(data: SnackbarData, modifier: Modifier = Modifier) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp)
            .shadow(12.dp, shape, ambientColor = c.ink, spotColor = c.ink)
            .background(c.darkCard, shape)
            .border(1.dp, c.darkCardBorder, shape)
            .heightIn(min = 50.dp)
            .padding(horizontal = 16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppText(data.visuals.message, style = t.bodyMedium, color = c.darkCardText, modifier = Modifier.weight(1f).padding(vertical = 12.dp))
        val action = data.visuals.actionLabel
        if (action != null) {
            AppText(
                action,
                style = t.bodyStrong,
                color = c.gold,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .clickable(role = Role.Button) { data.performAction() }
                    .padding(horizontal = 4.dp)
                    .semantics { },
            )
        }
    }
}

/**
 * Top-anchored toast host. Place it in a Box aligned TopCenter above the screen content; it adds
 * the status-bar inset plus 8dp and 16dp side margins.
 */
@Composable
fun AppToastHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 8.dp),
    ) { data -> PrayerToast(data) }
}

/** Shows toasts (with an optional Undo) on a [SnackbarHostState]. */
class Toaster(private val host: SnackbarHostState) {
    /** Shows [message]; when [undoLabel] is set and tapped, [onUndo] runs. Suspends until the toast closes. */
    suspend fun show(message: String, undoLabel: String? = null, onUndo: (suspend () -> Unit)? = null) {
        host.currentSnackbarData?.dismiss()
        val result = host.showSnackbar(
            message = message,
            actionLabel = undoLabel,
            withDismissAction = false,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) onUndo?.invoke()
    }
}

/** App-level [Toaster]; defaults to one that shows nothing until the scaffold provides the real one. */
val LocalToaster = staticCompositionLocalOf { Toaster(SnackbarHostState()) }
