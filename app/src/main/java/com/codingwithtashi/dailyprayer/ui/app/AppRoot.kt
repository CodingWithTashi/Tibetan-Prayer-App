package com.codingwithtashi.dailyprayer.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.navigation.compose.rememberNavController
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.data.model.Appearance
import com.codingwithtashi.dailyprayer.navigation.AppNavHost
import com.codingwithtashi.dailyprayer.navigation.Main
import com.codingwithtashi.dailyprayer.navigation.Onboarding
import com.codingwithtashi.dailyprayer.ui.components.AppToastHost
import com.codingwithtashi.dailyprayer.ui.components.LocalToastAnchors
import com.codingwithtashi.dailyprayer.ui.components.LocalToaster
import com.codingwithtashi.dailyprayer.ui.components.ToastAnchors
import com.codingwithtashi.dailyprayer.ui.components.Toaster
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme
import com.codingwithtashi.dailyprayer.ui.theme.SystemBarIcons
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

@Composable
fun AppRoot(
    state: AppRootState.Ready,
    updateRestartPrompts: Flow<Unit> = emptyFlow(),
    onRestartForUpdate: () -> Unit = {},
) {
    val isDark = when (state.appearance) {
        Appearance.SYSTEM -> isSystemInDarkTheme()
        Appearance.LIGHT -> false
        Appearance.DARK -> true
    }
    // Language is derived from the composition's own configuration (PrayerTheme's default) so the
    // typography always matches the resources actually being shown, including right after a per-app
    // locale switch when the stored preference and AppCompat's locale list briefly disagree.
    PrayerTheme(darkTheme = isDark, textScale = state.textSize.scale) {
        val hostState = remember { SnackbarHostState() }
        val accessibility = LocalAccessibilityManager.current
        val toaster = remember(hostState, accessibility) { Toaster(hostState, accessibility) }
        // Fixed at first composition: later onboarded changes must not rebuild the graph.
        val startDestination: Any = remember { if (state.onboarded) Main else Onboarding }
        val navController = rememberNavController()
        SystemBarIcons(darkBackground = isDark)
        val updateReady = stringResource(R.string.update_ready)
        val restartLabel = stringResource(R.string.update_restart)
        val restart by rememberUpdatedState(onRestartForUpdate)
        LaunchedEffect(updateRestartPrompts) {
            updateRestartPrompts.collect {
                launch { toaster.show(updateReady, restartLabel, long = true) { restart() } }
            }
        }
        var rootHeight by remember { mutableIntStateOf(0) }
        CompositionLocalProvider(LocalToaster provides toaster, LocalToastAnchors provides remember { ToastAnchors() }) {
            Box(
                Modifier
                    .fillMaxSize()
                    .onSizeChanged { rootHeight = it.height }
                    .background(PrayerTheme.colors.background)
                    // Exposes testTag values as resource ids so UI-automation flows (Maestro) can select by id.
                    .semantics { testTagsAsResourceId = true },
            ) {
                AppNavHost(startDestination = startDestination, navController = navController)
                AppToastHost(hostState, rootHeight, Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}
