package com.codingwithtashi.dailyprayer

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import com.codingwithtashi.dailyprayer.core.notification.DeepLinks
import com.codingwithtashi.dailyprayer.core.update.InAppUpdater
import com.codingwithtashi.dailyprayer.data.model.Appearance
import com.codingwithtashi.dailyprayer.navigation.NavigationEventBus
import com.codingwithtashi.dailyprayer.playback.PlayerController
import com.codingwithtashi.dailyprayer.reminders.ReminderHealthMonitor
import com.codingwithtashi.dailyprayer.ui.app.AppRoot
import com.codingwithtashi.dailyprayer.ui.app.AppRootState
import com.codingwithtashi.dailyprayer.ui.app.AppRootViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: AppRootViewModel by viewModels()

    @Inject lateinit var navigationEvents: NavigationEventBus
    @Inject lateinit var playerController: PlayerController
    @Inject lateinit var reminderHealth: ReminderHealthMonitor
    @Inject lateinit var inAppUpdater: InAppUpdater

    private val updateLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        inAppUpdater.onFlowResult(it.resultCode)
    }

    override fun onStart() {
        super.onStart()
        playerController.connect()
    }

    override fun onResume() {
        super.onResume()
        // Returning from system settings lands here, so a fix made there clears the reminder warnings.
        reminderHealth.refresh()
        inAppUpdater.onResume(updateLauncher)
    }

    override fun onStop() {
        playerController.release()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { viewModel.uiState.value is AppRootState.Loading }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )
        if (savedInstanceState == null) {
            dispatchDeepLink(intent)
            inAppUpdater.checkForUpdate(updateLauncher)
        }
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            when (val s = state) {
                AppRootState.Loading -> Box(Modifier.fillMaxSize().background(colorResource(R.color.window_background)))
                is AppRootState.Ready -> {
                    LaunchedEffect(s.appearance) {
                        AppCompatDelegate.setDefaultNightMode(
                            when (s.appearance) {
                                Appearance.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                                Appearance.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                                Appearance.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                            },
                        )
                    }
                    AppRoot(s, updateRestartPrompts = inAppUpdater.restartPrompts, onRestartForUpdate = inAppUpdater::completeUpdate)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        dispatchDeepLink(intent)
    }

    private fun dispatchDeepLink(intent: Intent?) {
        if (intent == null || DeepLinks.isReplay(intent)) return
        val link = DeepLinks.parse(intent) ?: return
        navigationEvents.emit(link)
        setIntent(DeepLinks.stripped(intent))
    }
}
