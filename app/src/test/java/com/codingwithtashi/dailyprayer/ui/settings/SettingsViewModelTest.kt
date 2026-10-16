package com.codingwithtashi.dailyprayer.ui.settings

import com.codingwithtashi.dailyprayer.data.model.AppTextSize
import com.codingwithtashi.dailyprayer.data.model.Appearance
import com.codingwithtashi.dailyprayer.core.Links
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.reminders.NotificationPermissionPolicy
import com.codingwithtashi.dailyprayer.reminders.ReminderHealth
import com.codingwithtashi.dailyprayer.ui.FakePrefs
import com.codingwithtashi.dailyprayer.ui.FakeReminderHealthMonitor
import com.codingwithtashi.dailyprayer.ui.RecordingScheduler
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

private class FakeLocale : LocaleApplier() {
    val applied = mutableListOf<AppLanguage>()
    override fun apply(language: AppLanguage) { applied += language }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var prefs: FakePrefs
    private lateinit var scheduler: RecordingScheduler
    private lateinit var locale: FakeLocale
    private lateinit var vm: SettingsViewModel
    private lateinit var downloads: FakeDownloadRepository
    private lateinit var health: FakeReminderHealthMonitor

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        prefs = FakePrefs(); scheduler = RecordingScheduler(); locale = FakeLocale()
        downloads = FakeDownloadRepository(); health = FakeReminderHealthMonitor()
        vm = SettingsViewModel(prefs, scheduler, health, locale, downloads, NotificationPermissionPolicy(prefs), CoroutineScope(dispatcher + SupervisorJob()))
    }

    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.send(e: SettingsEvent) { vm.onEvent(e); advanceUntilIdle() }

    @Test fun nameIsClampedTo24AndPersistedDebounced() = runTest(dispatcher) {
        send(SettingsEvent.SetName("a".repeat(40)))
        assertThat(vm.state.value.name).hasLength(24)
        assertThat(vm.state.value.initial).isEqualTo("A")
        vm.onEvent(SettingsEvent.SetName("Tashi"))
        advanceTimeBy(100)
        assertThat(prefs.state.value.userName).isEqualTo("a".repeat(24).trim())
        advanceUntilIdle()
        assertThat(prefs.state.value.userName).isEqualTo("Tashi")
    }

    @Test fun textSizeStaysWithin18To36InSteps() = runTest(dispatcher) {
        repeat(10) { send(SettingsEvent.SizeUp) }
        assertThat(prefs.state.value.textSize).isEqualTo(36)
        send(SettingsEvent.SizeDown)
        assertThat(prefs.state.value.textSize).isEqualTo(34)
        repeat(20) { send(SettingsEvent.SizeDown) }
        assertThat(prefs.state.value.textSize).isEqualTo(18)
    }

    @Test fun appTextSizePersists() = runTest(dispatcher) {
        send(SettingsEvent.SetAppTextSize(AppTextSize.LARGE))
        assertThat(prefs.state.value.appTextSize).isEqualTo(AppTextSize.LARGE)
        assertThat(vm.state.value.appTextSize).isEqualTo(AppTextSize.LARGE)
    }

    @Test fun goalAndAppearancePersist() = runTest(dispatcher) {
        send(SettingsEvent.SetGoal(21))
        send(SettingsEvent.SetAppearance(Appearance.DARK))
        assertThat(prefs.state.value.recitationGoal).isEqualTo(21)
        assertThat(prefs.state.value.appearance).isEqualTo(Appearance.DARK)
        assertThat(vm.state.value.appearance).isEqualTo(Appearance.DARK)
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
        assertThat(scheduler.rescheduledAll).isEqualTo(2)
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
        send(SettingsEvent.OpenGithub)
        val got = List(4) { vm.effects.first() }
        assertThat(got).containsExactly(
            SettingsEffect.ShareLink(Links.PLAY_STORE_URL),
            SettingsEffect.SendEmail(Links.DEV_EMAIL),
            SettingsEffect.OpenUrl(Links.DICTIONARY_URL),
            SettingsEffect.OpenUrl(Links.GITHUB_URL),
        ).inOrder()
    }

    @Test fun deniedPermissionTurnsRemindersOff() = runTest(dispatcher) {
        send(SettingsEvent.NotificationPermissionResult(false))
        assertThat(prefs.state.value.remindersEnabled).isFalse()
    }

    @Test fun downloadedCountAndStorageComeFromDownloadRepository() = runTest(dispatcher) {
        advanceUntilIdle()
        assertThat(vm.state.value.downloadedCount).isEqualTo(0)
        assertThat(vm.state.value.storageLabel).isNull()
        downloads.count.value = 3
        downloads.bytes.value = 12_300_000L
        advanceUntilIdle()
        assertThat(vm.state.value.downloadedCount).isEqualTo(3)
        assertThat(vm.state.value.storageLabel).isEqualTo("12.3 MB")
    }

    @Test fun statusMapsOffWorkingNotificationsAndExact() = runTest(dispatcher) {
        assertThat(vm.state.value.reminderStatus).isEqualTo(ReminderStatus.WORKING)
        health.state.value = ReminderHealth(false, false); advanceUntilIdle()
        assertThat(vm.state.value.reminderStatus).isEqualTo(ReminderStatus.NOTIFICATIONS_OFF)
        health.state.value = ReminderHealth(true, false); advanceUntilIdle()
        assertThat(vm.state.value.reminderStatus).isEqualTo(ReminderStatus.EXACT_ALARMS_OFF)
        send(SettingsEvent.ToggleReminders)
        assertThat(vm.state.value.reminderStatus).isEqualTo(ReminderStatus.OFF)
    }

    @Test fun fixStatusOpensMatchingSettings() = runTest(dispatcher) {
        health.state.value = ReminderHealth(false, true); advanceUntilIdle()
        send(SettingsEvent.FixReminderStatus)
        assertThat(vm.effects.first()).isEqualTo(SettingsEffect.OpenNotificationSettings)
        health.state.value = ReminderHealth(true, false); advanceUntilIdle()
        send(SettingsEvent.FixReminderStatus)
        assertThat(vm.effects.first()).isEqualTo(SettingsEffect.OpenExactAlarmSettings)
    }

    @Test fun fixStatusDoesNothingWhenWorkingOrOff() = runTest(dispatcher) {
        send(SettingsEvent.FixReminderStatus)
        send(SettingsEvent.Rate)
        assertThat(vm.effects.first()).isEqualTo(SettingsEffect.OpenUrl(Links.PLAY_STORE_URL))
    }

    @Test fun exactAlarmGrantDoesNotReschedule() = runTest(dispatcher) {
        // SystemEventsReceiver owns the reschedule on the permission-state broadcast.
        health.state.value = ReminderHealth(true, false); advanceUntilIdle()
        val before = scheduler.rescheduledAll
        health.state.value = ReminderHealth(true, true); advanceUntilIdle()
        assertThat(scheduler.rescheduledAll).isEqualTo(before)
        assertThat(vm.state.value.reminderStatus).isEqualTo(ReminderStatus.WORKING)
    }

    @Test fun switchReschedulesOnApplicationScope() = runTest(dispatcher) {
        val appDispatcher = StandardTestDispatcher(kotlinx.coroutines.test.TestCoroutineScheduler())
        val isolated = SettingsViewModel(prefs, scheduler, health, locale, downloads, NotificationPermissionPolicy(prefs), CoroutineScope(appDispatcher + SupervisorJob()))
        isolated.onEvent(SettingsEvent.ToggleReminders)
        advanceUntilIdle()
        assertThat(scheduler.rescheduledAll).isEqualTo(0)
        appDispatcher.scheduler.advanceUntilIdle()
        assertThat(scheduler.rescheduledAll).isEqualTo(1)
        assertThat(prefs.state.value.remindersEnabled).isFalse()
    }

    @Test fun permissionMissingAndRequestableLaunchesPrompt() = runTest(dispatcher) {
        send(SettingsEvent.NotificationPermissionMissing(shouldShowRationale = false))
        assertThat(vm.effects.first()).isEqualTo(SettingsEffect.LaunchNotificationPermission)
        // Marked when the dialog is launched, not when its result comes back.
        assertThat(prefs.state.value.notificationPermissionAsked).isTrue()
    }

    @Test fun permissionMissingAfterPermanentDenialOpensSettingsWithMessage() = runTest(dispatcher) {
        prefs.setNotificationPermissionAsked(true)
        send(SettingsEvent.NotificationPermissionMissing(shouldShowRationale = false))
        assertThat(vm.effects.first())
            .isEqualTo(SettingsEffect.NotificationsBlocked(R.string.reminder_notifications_blocked_toast))
        assertThat(prefs.state.value.remindersEnabled).isTrue()
    }

    @Test fun rationaleAllowsRequestEvenAfterAsking() = runTest(dispatcher) {
        prefs.setNotificationPermissionAsked(true)
        send(SettingsEvent.NotificationPermissionMissing(shouldShowRationale = true))
        assertThat(vm.effects.first()).isEqualTo(SettingsEffect.LaunchNotificationPermission)
    }
}
