package com.codingwithtashi.dailyprayer.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme

/**
 * Two-button confirmation dialog themed from the design tokens. [title] and [body] are each optional;
 * [confirmColor] tints the confirm label (accent by default, danger for destructive actions).
 */
@Composable
fun ConfirmDialog(
    title: String?,
    body: String?,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmColor: Color = PrayerTheme.colors.accent,
    titleStyle: TextStyle = PrayerTheme.type.bodyLargeStrong,
) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = title?.let { { AppText(it, style = titleStyle, color = c.ink) } },
        text = body?.let { { AppText(it, style = t.bodyLarge, color = c.ink) } },
        confirmButton = {
            TextButton(onClick = onConfirm) { AppText(confirmLabel, style = t.bodyStrong, color = confirmColor) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { AppText(dismissLabel, style = t.bodyStrong, color = c.textMuted) }
        },
    )
}
