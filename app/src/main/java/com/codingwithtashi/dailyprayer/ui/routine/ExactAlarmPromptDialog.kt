package com.codingwithtashi.dailyprayer.ui.routine

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.ui.components.ConfirmDialog

/**
 * Explanation before sending the user to the "Alarms & reminders" settings screen.
 * Reusable (the onboarding finish step shows it too); shown on every reminder save while exact alarms are not allowed.
 */
@Composable
fun ExactAlarmPromptDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmDialog(
        title = null,
        body = stringResource(R.string.exact_alarm_rationale),
        confirmLabel = stringResource(R.string.open_settings),
        dismissLabel = stringResource(R.string.cancel),
        onConfirm = onOpenSettings,
        onDismiss = onDismiss,
    )
}
