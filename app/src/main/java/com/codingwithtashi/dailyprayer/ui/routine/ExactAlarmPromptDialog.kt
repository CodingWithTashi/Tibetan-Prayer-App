package com.codingwithtashi.dailyprayer.ui.routine

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme

/**
 * One-time explanation before sending the user to the "Alarms & reminders" settings screen.
 * Reusable (the onboarding finish step shows it too); the caller owns the "shown once" flag.
 */
@Composable
fun ExactAlarmPromptDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        text = { Text(stringResource(R.string.exact_alarm_rationale), style = t.body15, color = c.ink) },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.open_settings), style = t.body14Semi, color = c.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), style = t.body14Semi, color = c.textMuted)
            }
        },
    )
}
