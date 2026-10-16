package com.codingwithtashi.dailyprayer.ui.today

import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.data.model.Routine
import com.codingwithtashi.dailyprayer.data.model.RoutineCompletion
import com.codingwithtashi.dailyprayer.data.prefs.UserPrefs
import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {
    private val today = LocalDate.of(2026, 10, 14) // Wednesday
    private val prefs = FakePrefs(UserPrefs(userName = " tashi "))
    private val prayers = FakePrayerRepo((1..6).map { testPrayer(it, titleEn = if (it == 1) "The Heart Sutra" else "Prayer $it") })
    private val routines = FakeRoutineRepo()
    private val notifications = FakeNotificationRepo()
    private val clock = FixedClock(today)

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm() = TodayViewModel(prefs, routines, prayers, notifications, clock).also { it.applyLocale = {} }

    private fun routine(id: Long, vararg prayerIds: Int) = Routine(
        id, "Morning $id", 390, DayOfWeek.entries.toSet(), true,
        prayerIds.map { pid -> prayers.all.value.first { it.id == pid } }, 0L,
    )

    private fun completion(rid: Long, pid: Int) = RoutineCompletion(rid, pid, today, 0L)

    private fun TestScope.state(vm: TodayViewModel): TodayUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
        return vm.uiState.value
    }

    @Test fun noRoutinesGivesAddRoutineCta() = runTest {
        val s = state(vm())
        assertThat(s.cta).isEqualTo(TodayCta.AddRoutine)
        assertThat(s.routines).isEmpty()
        assertThat(s.practiceCount).isNull()
        assertThat(s.greetingName).isEqualTo("tashi")
        assertThat(s.initial).isEqualTo("T")
    }

    @Test fun incompleteRoutineGivesStartCtaWithNextPrayer() = runTest {
        routines.routines.value = listOf(routine(1, 1, 2, 3))
        routines.completions.value = listOf(completion(1, 1))
        val s = state(vm())
        assertThat(s.cta).isEqualTo(TodayCta.Start(1, "Morning 1", 2))
        assertThat(s.routines.single().subtitle).isEqualTo(RoutineSubtitle.Names("The Heart Sutra · Prayer 2 · Prayer 3 (1/3)"))
        assertThat(s.practiceCount).isEqualTo("0 / 1")
    }

    @Test fun noProgressSubtitleHasNoCount() = runTest {
        routines.routines.value = listOf(routine(1, 2, 3))
        val s = state(vm())
        assertThat(s.routines.single().subtitle).isEqualTo(RoutineSubtitle.Names("Prayer 2 · Prayer 3"))
    }

    @Test fun allCompleteGivesAllDoneAndCompleteSubtitle() = runTest {
        routines.routines.value = listOf(routine(1, 1, 2))
        routines.completions.value = listOf(completion(1, 1), completion(1, 2))
        val s = state(vm())
        assertThat(s.cta).isEqualTo(TodayCta.AllDone)
        assertThat(s.routines.single().subtitle).isEqualTo(RoutineSubtitle.Complete)
        assertThat(s.routines.single().done).isTrue()
        assertThat(s.practiceCount).isEqualTo("1 / 1")
    }

    @Test fun ctaPicksFirstIncompleteRoutine() = runTest {
        routines.routines.value = listOf(routine(1, 1), routine(2, 2, 3))
        routines.completions.value = listOf(completion(1, 1))
        val s = state(vm())
        assertThat(s.cta).isEqualTo(TodayCta.Start(2, "Morning 2", 2))
        assertThat(s.practiceCount).isEqualTo("1 / 2")
    }

    @Test fun continueCardOnlyBetweenThresholds() = runTest {
        val vm = vm()
        val s0 = state(vm)
        assertThat(s0.continueReading).isNull()
        prayers.continueReading.value = testPrayer(1, progress = 0.03f)
        assertThat(vm.uiState.value.continueReading).isNull()
        prayers.continueReading.value = testPrayer(1, progress = 0.5f)
        assertThat(vm.uiState.value.continueReading?.pct).isEqualTo("50%")
        prayers.continueReading.value = testPrayer(1, progress = 0.97f)
        assertThat(vm.uiState.value.continueReading).isNull()
    }

    @Test fun savedTilesCappedAtFourWithLabelRule() = runTest {
        prayers.all.value = listOf(
            testPrayer(1, titleEn = "The Heart Sutra of Wisdom", isSaved = true),
            testPrayer(2, titleEn = "Green Tara", isSaved = true, savedLabel = "My Tara"),
            testPrayer(3, isSaved = true), testPrayer(4, isSaved = true), testPrayer(5, isSaved = true),
            testPrayer(6),
        )
        val s = state(vm())
        assertThat(s.hasSaved).isTrue()
        assertThat(s.savedTiles).hasSize(4)
        assertThat(s.savedTiles.map { it.label }.take(2)).containsExactly("Heart Sutra", "My Tara").inOrder()
    }

    @Test fun noSavedMeansEmpty() = runTest {
        val s = state(vm())
        assertThat(s.hasSaved).isFalse()
        assertThat(s.savedTiles).isEmpty()
    }

    @Test fun tibetanDigitsInBo() = runTest {
        prefs.state.value = UserPrefs(appLanguage = AppLanguage.BO)
        routines.routines.value = listOf(routine(1, 1, 2, 3))
        routines.completions.value = listOf(completion(1, 1))
        routines.dates.value = setOf(today, today.minusDays(1))
        prayers.continueReading.value = testPrayer(1, progress = 0.5f)
        val s = state(vm())
        assertThat(s.streak).isEqualTo("༢")
        assertThat(s.practiceCount).isEqualTo("༠ / ༡")
        assertThat((s.routines.single().subtitle as RoutineSubtitle.Names).text).endsWith("(༡/༣)")
        assertThat(s.continueReading?.pct).isEqualTo("༥༠%")
        assertThat(s.routines.single().timeLabel).isEqualTo("6:30 AM")
    }

    @Test fun weekRowAndStreak() = runTest {
        routines.dates.value = setOf(today, today.minusDays(1))
        val s = state(vm())
        assertThat(s.streak).isEqualTo("2")
        assertThat(s.week.map { it.filled }).containsExactly(false, true, true, false, false, false, false).inOrder()
        assertThat(s.week.single { it.isToday }.dayOfWeek).isEqualTo(DayOfWeek.WEDNESDAY)
    }

    @Test fun refreshRollsOverToNewDay() = runTest {
        routines.routines.value = listOf(routine(1, 1))
        routines.completions.value = listOf(completion(1, 1))
        val vm = vm()
        assertThat(state(vm).routines.single().done).isTrue()
        clock.date = today.plusDays(1)
        vm.refresh()
        assertThat(vm.uiState.value.date).isEqualTo(today.plusDays(1))
        assertThat(vm.uiState.value.routines.single().done).isFalse()
    }

    @Test fun toggleLanguageSwitchesPrefs() = runTest {
        val vm = vm()
        state(vm)
        vm.onEvent(TodayEvent.ToggleLanguage)
        assertThat(prefs.state.value.appLanguage).isEqualTo(AppLanguage.BO)
    }

    @Test fun unreadCount() = runTest {
        notifications.unread.value = 3
        assertThat(state(vm()).unreadNotifications).isEqualTo(3)
    }
}
