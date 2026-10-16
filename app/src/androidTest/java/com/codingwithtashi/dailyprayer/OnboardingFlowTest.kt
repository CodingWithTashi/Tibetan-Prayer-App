package com.codingwithtashi.dailyprayer

import android.Manifest
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import com.codingwithtashi.dailyprayer.core.init.AppInitializer
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import com.codingwithtashi.dailyprayer.data.repository.RoutineRepository
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@HiltAndroidTest
class OnboardingFlowTest {
    private val hilt = HiltAndroidRule(this)
    private val compose = createEmptyComposeRule()
    private val permission = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule val chain: RuleChain = RuleChain.outerRule(hilt).around(permission).around(compose)

    @Inject lateinit var prefs: UserPreferencesRepository
    @Inject lateinit var routines: RoutineRepository
    @Inject lateinit var initializer: AppInitializer

    private fun str(id: Int, vararg args: Any) =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(id, *args)

    @Before fun setUp() {
        hilt.inject()
        runBlocking {
            initializer.startAndAwait()
            routines.getRoutines().forEach { routines.delete(it.id) }
            prefs.setOnboarded(false)
        }
    }

    @After fun tearDown() = runBlocking {
        routines.getRoutines().forEach { routines.delete(it.id) }
        prefs.setOnboarded(true)
    }

    @Test fun onboardingCreatesRoutineAndReachesToday() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithContentDescription(str(R.string.cd_step_of, 1, 3)).assertExists()
            compose.onNodeWithText(str(R.string.continue_)).performClick()

            compose.waitUntil(5_000) {
                compose.onAllNodesWithText(str(R.string.skip)).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription(str(R.string.cd_step_of, 2, 3)).assertExists()
            compose.onNodeWithText(str(R.string.skip)).performClick()

            compose.waitUntil(5_000) {
                compose.onAllNodesWithText(str(R.string.ob_start)).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription(str(R.string.cd_step_of, 3, 3)).assertExists()
            compose.onNodeWithText(str(R.string.ob_start)).performClick()

            // Optional one-time exact-alarm explanation (depends on the device's special-access state).
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText(str(R.string.tab_library)).fetchSemanticsNodes().isNotEmpty() ||
                    compose.onAllNodesWithText(str(R.string.open_settings)).fetchSemanticsNodes().isNotEmpty()
            }
            if (compose.onAllNodesWithText(str(R.string.open_settings)).fetchSemanticsNodes().isNotEmpty()) {
                compose.onNodeWithText(str(R.string.cancel)).performClick()
            }
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText(str(R.string.tab_library)).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(str(R.string.tab_today)).assertExists()

            val saved = runBlocking { routines.getRoutines() }
            assertThat(saved).hasSize(1)
            assertThat(saved.single().prayers).isNotEmpty()
            assertThat(runBlocking { prefs.snapshot().onboarded }).isTrue()
        }
    }
}
