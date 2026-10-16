package com.codingwithtashi.dailyprayer.ui.routine

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.ui.components.ConfirmDialog

/**
 * Shown after saving a reminder when notifications are blocked and Android will no longer show the
 * permission dialog. "Open settings" leads to the app's notification settings.
 */
@Composable
fun NotificationsOffDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmDialog(
        title = stringResource(R.string.notifications_off_title),
        body = stringResource(R.string.notifications_off_body),
        confirmLabel = stringResource(R.string.open_settings),
        dismissLabel = stringResource(R.string.not_now),
        onConfirm = onOpenSettings,
        onDismiss = onDismiss,
    )
}
