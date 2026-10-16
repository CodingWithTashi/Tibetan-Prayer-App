package com.codingwithtashi.dailyprayer.ui.components

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.core.startActivitySafely
import kotlinx.coroutines.launch

/** Returns a launcher that starts an intent and shows the "no app available" toast when nothing can handle it. */
@Composable
fun rememberSafeLauncher(): (Intent) -> Unit {
    val context = LocalContext.current
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    val noApp = stringResource(R.string.no_app_available)
    return remember(context, toaster, scope, noApp) {
        { intent -> if (!context.startActivitySafely(intent)) scope.launch { toaster.show(noApp) } }
    }
}
