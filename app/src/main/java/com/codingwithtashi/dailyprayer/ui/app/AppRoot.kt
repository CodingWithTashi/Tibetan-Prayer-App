package com.codingwithtashi.dailyprayer.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.navigation.compose.rememberNavController
import com.codingwithtashi.dailyprayer.data.model.Appearance
import com.codingwithtashi.dailyprayer.navigation.AppNavHost
import com.codingwithtashi.dailyprayer.navigation.Main
import com.codingwithtashi.dailyprayer.navigation.Onboarding
import com.codingwithtashi.dailyprayer.ui.components.AppToastHost
import com.codingwithtashi.dailyprayer.ui.components.LocalToaster
import com.codingwithtashi.dailyprayer.ui.components.Toaster
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme
import com.codingwithtashi.dailyprayer.ui.theme.SystemBarIcons

@Composable
fun AppRoot(state: AppRootState.Ready) {
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
        val toaster = remember(hostState) { Toaster(hostState) }
        // Fixed at first composition: later onboarded changes must not rebuild the graph.
        val startDestination: Any = remember { if (state.onboarded) Main else Onboarding }
        val navController = rememberNavController()
        SystemBarIcons(darkBackground = isDark)
        CompositionLocalProvider(LocalToaster provides toaster) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(PrayerTheme.colors.background)
                    // Exposes testTag values as resource ids so UI-automation flows (Maestro) can select by id.
                    .semantics { testTagsAsResourceId = true },
            ) {
                AppNavHost(startDestination = startDestination, navController = navController)
                AppToastHost(hostState, Modifier.align(Alignment.TopCenter))
            }
        }
    }
}
