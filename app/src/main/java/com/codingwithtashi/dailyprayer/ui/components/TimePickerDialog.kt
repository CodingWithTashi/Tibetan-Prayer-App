package com.codingwithtashi.dailyprayer.ui.components

import android.text.format.DateFormat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme

/**
 * Time picker dialog (M3 [TimePicker] in an [AlertDialog]) themed from the design tokens. Time is
 * exchanged as minutes after midnight; 24-hour mode follows the system setting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimePickerDialog(
    initialMinutes: Int,
    onConfirm: (minutes: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = PrayerTheme.colors
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = (initialMinutes / 60).coerceIn(0, 23),
        initialMinute = (initialMinutes % 60).coerceIn(0, 59),
        is24Hour = DateFormat.is24HourFormat(context),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
                AppText(stringResource(R.string.done), style = PrayerTheme.type.bodyStrong, color = c.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                AppText(stringResource(R.string.cancel), style = PrayerTheme.type.bodyStrong, color = c.textMuted)
            }
        },
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = c.surfaceWarm,
                    clockDialSelectedContentColor = c.onAccentSolid,
                    clockDialUnselectedContentColor = c.ink,
                    selectorColor = c.accentSolid,
                    containerColor = c.surface,
                    periodSelectorBorderColor = c.border,
                    periodSelectorSelectedContainerColor = c.accentSolid,
                    periodSelectorUnselectedContainerColor = c.surface,
                    periodSelectorSelectedContentColor = c.onAccentSolid,
                    periodSelectorUnselectedContentColor = c.textMuted,
                    timeSelectorSelectedContainerColor = c.accentSolid,
                    timeSelectorUnselectedContainerColor = c.surfaceWarm,
                    timeSelectorSelectedContentColor = c.onAccentSolid,
                    timeSelectorUnselectedContentColor = c.ink,
                ),
            )
        },
    )
}
