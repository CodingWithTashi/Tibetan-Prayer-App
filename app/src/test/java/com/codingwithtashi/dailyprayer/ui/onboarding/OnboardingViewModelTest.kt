package com.codingwithtashi.dailyprayer.ui.onboarding

import androidx.lifecycle.SavedStateHandle
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.data.seed.OnboardingDefaults
import com.codingwithtashi.dailyprayer.ui.today.FakePrayerRepo
import com.codingwithtashi.dailyprayer.ui.today.FakePrefs
import com.codingwithtashi.dailyprayer.ui.today.FakeRoutineRepo
import com.codingwithtashi.dailyprayer.ui.today.testPrayer
import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    // Catalogue order differs from candidate order on purpose; id = reverse index.
    private val prayers = FakePrayerRepo(
        OnboardingDefaults.CANDIDATE_SLUGS.reversed().mapIndexed { i, slug -> testPrayer(100 + i, slug = slug) } +
            testPrayer(1, slug = "other"),
    )
    private val routines = FakeRoutineRepo()
    private val prefs = FakePrefs()
    private val handle = SavedStateHandle()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm() = OnboardingViewModel(handle, prefs, prayers, routines).also {
        it.applyLocale = {}
        it.sdkInt = 34
    }

    private fun TestScope.started(vm: OnboardingViewModel): OnboardingViewModel {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
        return vm
    }

    private fun OnboardingViewModel.drain(): List<OnboardingEffect> {
        val out = mutableListOf<OnboardingEffect>()
        while (true) out += effects.tryReceive().getOrNull() ?: break
        return out
    }

    @Test fun defaults() = runTest {
        val vm = started(vm())
        val s = vm.uiState.value
        assertThat(s.step).isEqualTo(0)
        assertThat(s.timeMinutes).isEqualTo(390)
        assertThat(s.picks).containsExactlyElementsIn(OnboardingDefaults.DEFAULT_PICK_SLUGS)
        assertThat(s.candidates.map { it.slug }).containsExactlyElementsIn(OnboardingDefaults.CANDIDATE_SLUGS).inOrder()
    }

    @Test fun stepNavigationIsClamped() = runTest {
        val vm = started(vm())
        vm.onEvent(OnboardingEvent.Back)
        assertThat(vm.uiState.value.step).isEqualTo(0)
        repeat(5) { vm.onEvent(OnboardingEvent.Next) }
        assertThat(vm.uiState.value.step).isEqualTo(2)
        vm.onEvent(OnboardingEvent.Back)
        assertThat(vm.uiState.value.step).isEqualTo(1)
    }

    @Test fun nameIsLimitedTo24() = runTest {
        val vm = started(vm())
        vm.onEvent(OnboardingEvent.SetName("x".repeat(40)))
        assertThat(vm.uiState.value.name).hasLength(24)
    }

    @Test fun togglePick() = runTest {
        val vm = started(vm())
        vm.onEvent(OnboardingEvent.TogglePick("dolma"))
        assertThat(vm.uiState.value.picks).contains("dolma")
        vm.onEvent(OnboardingEvent.TogglePick("dolma"))
        assertThat(vm.uiState.value.picks).doesNotContain("dolma")
    }

    @Test fun setLanguageUpdatesPrefsAndAppliesLocale() = runTest {
        val vm = started(vm())
        var applied: AppLanguage? = null
        vm.applyLocale = { applied = it }
        vm.onEvent(OnboardingEvent.SetLanguage(AppLanguage.BO))
        assertThat(applied).isEqualTo(AppLanguage.BO)
        assertThat(vm.uiState.value.language).isEqualTo(AppLanguage.BO)
    }

    @Test fun stateSurvivesRecreationThroughSavedState() = runTest {
        val first = started(vm())
        first.onEvent(OnboardingEvent.Next)
        first.onEvent(OnboardingEvent.SetName("Pema"))
        first.onEvent(OnboardingEvent.SetTime(450))
        val second = started(vm())
        assertThat(second.uiState.value.step).isEqualTo(1)
        assertThat(second.uiState.value.name).isEqualTo("Pema")
        assertThat(second.uiState.value.timeMinutes).isEqualTo(450)
    }

    @Test fun finishWithNoPicksToastsAndCreatesNothing() = runTest {
        val vm = started(vm())
        OnboardingDefaults.DEFAULT_PICK_SLUGS.forEach { vm.onEvent(OnboardingEvent.TogglePick(it)) }
        vm.onEvent(OnboardingEvent.Finish(true, "Morning practice"))
        assertThat(vm.drain()).containsExactly(OnboardingEffect.PickAtLeastOne)
        assertThat(routines.saved).isEmpty()
        assertThat(prefs.state.value.onboarded).isFalse()
    }

    @Test fun finishCreatesRoutineInCandidateOrder() = runTest {
        val vm = started(vm())
        // default picks are the first three candidates; add the last one
        vm.onEvent(OnboardingEvent.TogglePick("ngowa_monlam_one"))
        vm.onEvent(OnboardingEvent.SetName("  Pema "))
        vm.onEvent(OnboardingEvent.SetTime(420))
        vm.onEvent(OnboardingEvent.Finish(true, "Morning practice"))
        val draft = routines.saved.single()
        val idBySlug = prayers.all.value.associate { it.slug to it.id }
        assertThat(draft.prayerIds).containsExactly(
            idBySlug["gyun_chak_sum"], idBySlug["kyab_do_sam_kay"], idBySlug["sherab_nyingpo"], idBySlug["ngowa_monlam_one"],
        ).inOrder()
        assertThat(draft.name).isEqualTo("Morning practice")
        assertThat(draft.reminderEnabled).isTrue()
        assertThat(draft.days).isEqualTo(DayOfWeek.entries.toSet())
        assertThat(draft.timeMinutes).isEqualTo(420)
        assertThat(draft.id).isNull()
        assertThat(prefs.state.value.userName).isEqualTo("Pema")
        assertThat(prefs.state.value.onboarded).isTrue()
        assertThat(vm.drain()).containsExactly(
            OnboardingEffect.RequestNotificationPermission, OnboardingEffect.Welcome("Pema"), OnboardingEffect.Finished,
        ).inOrder()
    }

    @Test fun permissionNotRequestedBelowApi33() = runTest {
        val vm = started(vm()).also { it.sdkInt = 32 }
        vm.onEvent(OnboardingEvent.Finish(true, "Morning practice"))
        assertThat(vm.drain()).containsExactly(OnboardingEffect.Welcome(""), OnboardingEffect.Finished).inOrder()
    }

    @Test fun finishWithoutRoutineOnlyOnboards() = runTest {
        val vm = started(vm())
        vm.onEvent(OnboardingEvent.SetName("Pema"))
        vm.onEvent(OnboardingEvent.Finish(false))
        assertThat(routines.saved).isEmpty()
        assertThat(prefs.state.value.onboarded).isTrue()
        assertThat(prefs.state.value.userName).isEqualTo("Pema")
        assertThat(vm.drain()).containsExactly(OnboardingEffect.Welcome("Pema"), OnboardingEffect.Finished).inOrder()
    }
}
