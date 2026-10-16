package com.codingwithtashi.dailyprayer.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.navigation.MainTab
import com.codingwithtashi.dailyprayer.ui.components.MiniPlayerUi
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Regression: a min-height mini player whose rows filled the height took the whole screen. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w390dp-h844dp")
class MainScaffoldLayoutTest {
    @get:Rule val compose = createComposeRule()

    private fun show(textScale: Float) = compose.setContent {
        PrayerTheme(darkTheme = true, language = AppLanguage.EN, reduceMotion = true, textScale = textScale) {
            MainScaffoldContent(
                tab = MainTab.LISTEN,
                miniPlayer = MiniPlayerUi("རྒྱུན", "Three Continuous Practices", "Verse 1 / 24", isPlaying = false, progress = 0.1f),
                onSelectTab = {},
                onMiniPlayerOpen = {},
                onMiniPlayerPlayPause = {},
                onMiniPlayerClose = {},
                tabContent = { _, _, modifier -> Box(modifier.testTag(TAB_CONTENT)) },
            )
        }
    }

    private fun assertLayout(maxMiniHeight: Int) {
        val mini = compose.onNodeWithTag("mini_player").getBoundsInRoot()
        assertThat(mini.bottom - mini.top).isLessThan(maxMiniHeight.dp)
        compose.onNodeWithTag(TAB_CONTENT).assertIsDisplayed()
        val content = compose.onNodeWithTag(TAB_CONTENT).getBoundsInRoot()
        assertThat(content.bottom - content.top).isGreaterThan(400.dp)
        compose.onNodeWithText("Listen").assertIsDisplayed()
        compose.onNodeWithText("Today").assertIsDisplayed()
    }

    @Test fun miniPlayerStaysACardAndKeepsTabContentAndBottomBar() {
        show(textScale = 1f)
        assertLayout(maxMiniHeight = 100)
    }

    @Test fun largestTextSizeStillLeavesRoomForTheTab() {
        show(textScale = 1.3f)
        assertLayout(maxMiniHeight = 130)
    }

    private companion object {
        const val TAB_CONTENT = "tab_content"
    }
}
