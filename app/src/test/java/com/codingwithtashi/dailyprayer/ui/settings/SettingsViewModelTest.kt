package com.codingwithtashi.dailyprayer.ui.settings

import com.codingwithtashi.dailyprayer.core.Links
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.data.model.Appearance
import com.codingwithtashi.dailyprayer.data.model.Routine
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import com.codingwithtashi.dailyprayer.data.prefs.UserPrefs
import com.codingwithtashi.dailyprayer.reminders.ReminderScheduler
import com.codingwithtashi.dailyprayer.ui.FakeDownloadRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

private class FakePrefs : UserPreferencesRepository {
    val state = MutableStateFlow(UserPrefs())
    override val prefs = state
    override suspend fun snapshot() = state.value
    override suspend fun setOnboarded(value: Boolean) = state.update { it.copy(onboarded = value) }
    override suspend fun setUserName(name: String) = state.update { it.copy(userName = name) }
    override suspend fun setAppLanguage(language: AppLanguage) = state.update { it.copy(appLanguage = language) }
    override suspend fun setAppearance(appearance: Appearance) = state.update { it.copy(appearance = appearance) }
    override suspend fun setTextSize(size: Int) = state.update { it.copy(textSize = size) }
    override suspend fun setRecitationGoal(goal: Int) = state.update { it.copy(recitationGoal = goal) }
    override suspend fun setRemindersEnabled(enabled: Boolean) = state.update { it.copy(remindersEnabled = enabled) }
    override suspend fun setKeepScreenOn(enabled: Boolean) = state.update { it.copy(keepScreenOn = enabled) }
    override suspend fun setLastReadPrayerId(id: Int?) = state.update { it.copy(lastReadPrayerId = id) }
    override suspend fun setLegacyCleanupDone(done: Boolean) = state.update { it.copy(legacyCleanupDone = done) }
    override suspend fun setExactAlarmPromptShown(shown: Boolean) = state.update { it.copy(exactAlarmPromptShown = shown) }
    override suspend fun setNotificationPermissionAsked(asked: Boolean) = state.update { it.copy(notificationPermissionAsked = asked) }
    override suspend fun setWelcomeNotificationInserted(inserted: Boolean) = state.update { it.copy(welcomeNotificationInserted = inserted) }
    private fun MutableStateFlow<UserPrefs>.update(f: (UserPrefs) -> UserPrefs) { value = f(value) }
}

private class FakeScheduler : ReminderScheduler {
    var rescheduled = 0
    override suspend fun schedule(routine: Routine) = Unit
    override fun cancel(routineId: Long) = Unit
    override suspend fun rescheduleAll() { rescheduled++ }
}

private class FakeLocale : LocaleApplier() {
    val applied = mutableListOf<AppLanguage>()
    override fun apply(language: AppLanguage) { applied += language }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var prefs: FakePrefs
    private lateinit var scheduler: FakeScheduler
    private lateinit var locale: FakeLocale
    private lateinit var vm: SettingsViewModel
    private lateinit var downloads: FakeDownloadRepository

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        prefs = FakePrefs(); scheduler = FakeScheduler(); locale = FakeLocale()
        downloads = FakeDownloadRepository()
        vm = SettingsViewModel(prefs, scheduler, locale, downloads, CoroutineScope(dispatcher + SupervisorJob()))
    }

    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.send(e: SettingsEvent) { vm.onEvent(e); advanceUntilIdle() }

    @Test fun nameIsClampedTo24AndPersistedDebounced() = runTest(dispatcher) {
        send(SettingsEvent.SetName("a".repeat(40)))
        assertThat(vm.uiState.value.name).hasLength(24)
        assertThat(vm.uiState.value.initial).isEqualTo("A")
        vm.onEvent(SettingsEvent.SetName("Tashi"))
        advanceTimeBy(100)
        assertThat(prefs.state.value.userName).isEqualTo("a".repeat(24).trim())
        advanceUntilIdle()
        assertThat(prefs.state.value.userName).isEqualTo("Tashi")
    }

    @Test fun textSizeStaysWithin16To32InSteps() = runTest(dispatcher) {
        repeat(10) { send(SettingsEvent.SizeUp) }
        assertThat(prefs.state.value.textSize).isEqualTo(32)
        send(SettingsEvent.SizeDown)
        assertThat(prefs.state.value.textSize).isEqualTo(30)
        repeat(20) { send(SettingsEvent.SizeDown) }
        assertThat(prefs.state.value.textSize).isEqualTo(16)
    }

    @Test fun goalAndAppearancePersist() = runTest(dispatcher) {
        send(SettingsEvent.SetGoal(21))
        send(SettingsEvent.SetAppearance(Appearance.DARK))
        assertThat(prefs.state.value.recitationGoal).isEqualTo(21)
        assertThat(prefs.state.value.appearance).isEqualTo(Appearance.DARK)
        assertThat(vm.uiState.value.appearance).isEqualTo(Appearance.DARK)
    }

    @Test fun languagePersistsAndAppliesLocale() = runTest(dispatcher) {
        send(SettingsEvent.SetLanguage(AppLanguage.BO))
        assertThat(prefs.state.value.appLanguage).isEqualTo(AppLanguage.BO)
        assertThat(locale.applied).containsExactly(AppLanguage.BO)
    }

    @Test fun remindersPermissionEffectOnlyWhenTurningOn() = runTest(dispatcher) {
        // default is ON -> first toggle turns OFF
        send(SettingsEvent.ToggleReminders)
        assertThat(prefs.state.value.remindersEnabled).isFalse()
        send(SettingsEvent.ToggleReminders)
        assertThat(prefs.state.value.remindersEnabled).isTrue()
        assertThat(scheduler.rescheduled).isEqualTo(2)
        assertThat(vm.effects.first()).isEqualTo(SettingsEffect.RequestNotificationPermission)
    }

    @Test fun turningRemindersOffEmitsNoEffect() = runTest(dispatcher) {
        send(SettingsEvent.ToggleReminders)
        send(SettingsEvent.Rate)
        assertThat(vm.effects.first()).isEqualTo(SettingsEffect.OpenUrl(Links.PLAY_STORE_URL))
    }

    @Test fun linkEffectsCarryPayloads() = runTest(dispatcher) {
        send(SettingsEvent.Share)
        send(SettingsEvent.EmailPrayerRequest)
        send(SettingsEvent.OpenDictionary)
        send(SettingsEvent.RemindersHelp)
        val got = List(4) { vm.effects.first() }
        assertThat(got).containsExactly(
            SettingsEffect.ShareLink(Links.PLAY_STORE_URL),
            SettingsEffect.SendEmail(Links.DEV_EMAIL, "Prayer Request"),
            SettingsEffect.OpenUrl(Links.DICTIONARY_URL),
            SettingsEffect.OpenAppDetailsSettings,
        ).inOrder()
    }

    @Test fun deniedPermissionTurnsRemindersOff() = runTest(dispatcher) {
        send(SettingsEvent.NotificationPermissionResult(false))
        assertThat(prefs.state.value.remindersEnabled).isFalse()
        assertThat(prefs.state.value.notificationPermissionAsked).isTrue()
    }

    @Test fun downloadedCountAndStorageComeFromDownloadRepository() = runTest(dispatcher) {
        advanceUntilIdle()
        assertThat(vm.uiState.value.downloadedCount).isEqualTo(0)
        assertThat(vm.uiState.value.storageLabel).isNull()
        downloads.count.value = 3
        downloads.bytes.value = 12_300_000L
        advanceUntilIdle()
        assertThat(vm.uiState.value.downloadedCount).isEqualTo(3)
        assertThat(vm.uiState.value.storageLabel).isEqualTo("12.3 MB")
    }
}
