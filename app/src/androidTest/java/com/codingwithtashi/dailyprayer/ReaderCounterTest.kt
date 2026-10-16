package com.codingwithtashi.dailyprayer

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.codingwithtashi.dailyprayer.core.init.AppInitializer
import com.codingwithtashi.dailyprayer.data.model.Prayer
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@HiltAndroidTest
class ReaderCounterTest {
    private val hilt = HiltAndroidRule(this)
    private val compose = createEmptyComposeRule()

    @get:Rule val chain: RuleChain = RuleChain.outerRule(hilt).around(compose)

    @Inject lateinit var prefs: UserPreferencesRepository
    @Inject lateinit var prayers: PrayerRepository
    @Inject lateinit var initializer: AppInitializer

    private lateinit var prayer: Prayer

    private fun str(id: Int) = ApplicationProvider.getApplicationContext<android.content.Context>().getString(id)

    @Before fun setUp() {
        hilt.inject()
        runBlocking {
            initializer.startAndAwait()
            prefs.setOnboarded(true)
            prefs.setAppLanguage(com.codingwithtashi.dailyprayer.core.locale.AppLanguage.EN)
            prayer = prayers.observeAll().first().first()
            prayers.setCount(prayer.id, 0)
        }
    }

    @After fun tearDown() = runBlocking { prayers.setCount(prayer.id, 0) }

    @Test fun incrementThreeTimesThenReset() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText(str(R.string.tab_library)).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(str(R.string.tab_library)).performClick()

            // Library rows show the Tibetan title; the first row is the first prayer by sort order.
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText(prayer.title, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onAllNodesWithText(prayer.title, useUnmergedTree = true)[0].performClick()

            val inc = str(R.string.cd_increment)
            compose.waitUntil(10_000) {
                compose.onAllNodesWithContentDescription(inc).fetchSemanticsNodes().isNotEmpty()
            }
            repeat(3) { compose.onNodeWithContentDescription(inc).performClick() }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText("3", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
            assertThat(runBlocking { prayers.get(prayer.id)!!.count }).isEqualTo(3)

            compose.onNodeWithContentDescription(str(R.string.cd_reset_count)).performClick()
            compose.onNodeWithText(str(R.string.reset)).performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText("0", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
            assertThat(runBlocking { prayers.get(prayer.id)!!.count }).isEqualTo(0)
        }
    }
}
