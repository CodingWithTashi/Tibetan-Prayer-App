package com.codingwithtashi.dailyprayer.ui.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingwithtashi.dailyprayer.BuildConfig
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.core.Links
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.core.locale.AppLocale
import com.codingwithtashi.dailyprayer.data.model.AppTextSize
import com.codingwithtashi.dailyprayer.data.model.Appearance
import com.codingwithtashi.dailyprayer.data.prefs.ReaderTextSize
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import com.codingwithtashi.dailyprayer.data.repository.DownloadRepository
import com.codingwithtashi.dailyprayer.reminders.NotificationPermissionPolicy
import com.codingwithtashi.dailyprayer.reminders.ReminderHealth
import com.codingwithtashi.dailyprayer.reminders.ReminderHealthMonitor
import com.codingwithtashi.dailyprayer.reminders.ReminderIssue
import com.codingwithtashi.dailyprayer.reminders.ReminderScheduler
import com.codingwithtashi.dailyprayer.ui.components.avatarInitial
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.codingwithtashi.dailyprayer.core.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope

/** Injectable seam over [AppLocale] so tests don't touch AppCompatDelegate. */
open class LocaleApplier @Inject constructor() {
    open fun apply(language: AppLanguage) = AppLocale.apply(language)
}

/** What the reminder status row shows. */
enum class ReminderStatus { OFF, WORKING, NOTIFICATIONS_OFF, EXACT_ALARMS_OFF }

data class SettingsUiState(
    val name: String = "",
    val initial: String = avatarInitial(null),
    val language: AppLanguage = AppLanguage.EN,
    val appearance: Appearance = Appearance.SYSTEM,
    val appTextSize: AppTextSize = AppTextSize.DEFAULT,
    val textSizeSp: Int = ReaderTextSize.DEFAULT,
    val remindersEnabled: Boolean = true,
    val reminderStatus: ReminderStatus = ReminderStatus.WORKING,
    val keepScreenOn: Boolean = true,
    val downloadedCount: Int = 0,
    val storageLabel: String? = null,
    val aboutOpen: Boolean = false,
    val versionName: String = BuildConfig.VERSION_NAME,
    val showReadingSheet: Boolean = false,
    val goal: Int = 108,
)

sealed interface SettingsEvent {
    data class SetName(val name: String) : SettingsEvent
    data class SetLanguage(val language: AppLanguage) : SettingsEvent
    data class SetAppearance(val appearance: Appearance) : SettingsEvent
    data class SetAppTextSize(val size: AppTextSize) : SettingsEvent
    data object SizeUp : SettingsEvent
    data object SizeDown : SettingsEvent
    data class SetGoal(val goal: Int) : SettingsEvent
    data object OpenReading : SettingsEvent
    data object CloseReading : SettingsEvent
    data object ToggleReminders : SettingsEvent
    data class NotificationPermissionResult(val granted: Boolean) : SettingsEvent
    data object ToggleKeepScreenOn : SettingsEvent
    data object ToggleAbout : SettingsEvent
    data object Share : SettingsEvent
    data object Rate : SettingsEvent
    data object OpenDictionary : SettingsEvent
    data object OpenCalendar : SettingsEvent
    data object OpenGithub : SettingsEvent
    data object OpenInstagram : SettingsEvent
    data object OpenFacebook : SettingsEvent
    data object EmailPrayerRequest : SettingsEvent

    /** Re-reads notification and exact-alarm state; send on every resume. */

    /** The status row was tapped: opens the matching system screen when something needs fixing. */
    data object FixReminderStatus : SettingsEvent

    /**
     * The runtime notification permission is missing after the user turned reminders on.
     * [shouldShowRationale] is Android's answer for whether a request can still be made.
     */
    data class NotificationPermissionMissing(val shouldShowRationale: Boolean) : SettingsEvent
    data object OpenDownloads : SettingsEvent
    data object OpenNotifications : SettingsEvent
    data object Exit : SettingsEvent
}

sealed interface SettingsEffect {
    data object RequestNotificationPermission : SettingsEffect
    /** The screen builds the localized share sentence around [url]. */
    data class ShareLink(val url: String) : SettingsEffect
    data class OpenUrl(val url: String) : SettingsEffect
    data class SendEmail(val to: String) : SettingsEffect
    data object OpenNotificationSettings : SettingsEffect
    data object OpenExactAlarmSettings : SettingsEffect

    /** The runtime permission dialog may be shown. */
    data object LaunchNotificationPermission : SettingsEffect

    /** Notifications are blocked and cannot be requested again: show [res], then open notification settings. */
    data class NotificationsBlocked(@StringRes val res: Int) : SettingsEffect
    data object OpenDownloads : SettingsEffect
    data object OpenNotifications : SettingsEffect
    data object Exit : SettingsEffect
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    private val reminderScheduler: ReminderScheduler,
    private val healthMonitor: ReminderHealthMonitor,
    private val localeApplier: LocaleApplier,
    private val downloads: DownloadRepository,
    private val permissionPolicy: NotificationPermissionPolicy,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val nameOverride = MutableStateFlow<String?>(null)
    private val aboutOpen = MutableStateFlow(false)
    private val showReading = MutableStateFlow(false)
    private var nameJob: Job? = null
    private var pendingName: String? = null

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val state: StateFlow<SettingsUiState> =
        combine(
            prefs.prefs,
            combine(nameOverride, aboutOpen, showReading) { n, a, r -> Triple(n, a, r) },
            combine(downloads.downloadedCount(), downloads.totalBytes()) { count, bytes -> count to bytes },
            healthMonitor.health,
        ) { p, (nameEdit, about, reading), (downloadedCount, bytes), h ->
            val name = nameEdit ?: p.userName
            SettingsUiState(
                name = name,
                initial = avatarInitial(name),
                language = p.appLanguage,
                appearance = p.appearance,
                textSizeSp = p.textSize,
                appTextSize = p.appTextSize,
                remindersEnabled = p.remindersEnabled,
                reminderStatus = reminderStatus(p.remindersEnabled, h),
                keepScreenOn = p.keepScreenOn,
                aboutOpen = about,
                showReadingSheet = reading,
                goal = p.recitationGoal,
                downloadedCount = downloadedCount,
                storageLabel = if (downloadedCount > 0) downloads.storageLabel(bytes) else null,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetName -> setName(event.name)
            is SettingsEvent.SetLanguage -> viewModelScope.launch {
                prefs.setAppLanguage(event.language)
                localeApplier.apply(event.language)
            }
            is SettingsEvent.SetAppearance -> viewModelScope.launch { prefs.setAppearance(event.appearance) }
            is SettingsEvent.SetAppTextSize -> viewModelScope.launch { prefs.setAppTextSize(event.size) }
            SettingsEvent.SizeUp -> viewModelScope.launch {
                prefs.setTextSize(state.value.textSizeSp + ReaderTextSize.STEP)
            }
            SettingsEvent.SizeDown -> viewModelScope.launch {
                prefs.setTextSize(state.value.textSizeSp - ReaderTextSize.STEP)
            }
            is SettingsEvent.SetGoal -> viewModelScope.launch { prefs.setRecitationGoal(event.goal) }
            SettingsEvent.OpenReading -> showReading.value = true
            SettingsEvent.CloseReading -> showReading.value = false
            SettingsEvent.ToggleReminders -> toggleReminders()
            is SettingsEvent.NotificationPermissionResult -> viewModelScope.launch {
                if (!event.granted) {
                    prefs.setRemindersEnabled(false)
                    reminderScheduler.rescheduleAll()
                }
                healthMonitor.refresh()
            }
            SettingsEvent.FixReminderStatus -> when (state.value.reminderStatus) {
                ReminderStatus.NOTIFICATIONS_OFF -> emit(SettingsEffect.OpenNotificationSettings)
                ReminderStatus.EXACT_ALARMS_OFF -> emit(SettingsEffect.OpenExactAlarmSettings)
                ReminderStatus.OFF, ReminderStatus.WORKING -> Unit
            }
            is SettingsEvent.NotificationPermissionMissing -> viewModelScope.launch {
                emit(
                    if (permissionPolicy.claimRequest(event.shouldShowRationale)) SettingsEffect.LaunchNotificationPermission
                    else SettingsEffect.NotificationsBlocked(R.string.reminder_notifications_blocked_toast),
                )
            }
            SettingsEvent.ToggleKeepScreenOn -> viewModelScope.launch { prefs.setKeepScreenOn(!state.value.keepScreenOn) }
            SettingsEvent.ToggleAbout -> aboutOpen.update { !it }
            SettingsEvent.Share -> emit(SettingsEffect.ShareLink(Links.PLAY_STORE_URL))
            SettingsEvent.Rate -> emit(SettingsEffect.OpenUrl(Links.PLAY_STORE_URL))
            SettingsEvent.OpenDictionary -> emit(SettingsEffect.OpenUrl(Links.DICTIONARY_URL))
            SettingsEvent.OpenCalendar -> emit(SettingsEffect.OpenUrl(Links.CALENDAR_URL))
            SettingsEvent.OpenGithub -> emit(SettingsEffect.OpenUrl(Links.GITHUB_URL))
            SettingsEvent.OpenInstagram -> emit(SettingsEffect.OpenUrl(Links.INSTAGRAM_URL))
            SettingsEvent.OpenFacebook -> emit(SettingsEffect.OpenUrl(Links.FACEBOOK_URL))
            SettingsEvent.EmailPrayerRequest -> emit(SettingsEffect.SendEmail(Links.DEV_EMAIL))
            SettingsEvent.OpenDownloads -> emit(SettingsEffect.OpenDownloads)
            SettingsEvent.OpenNotifications -> emit(SettingsEffect.OpenNotifications)
            SettingsEvent.Exit -> emit(SettingsEffect.Exit)
        }
    }

    private fun emit(effect: SettingsEffect) {
        _effects.trySend(effect)
    }

    private fun setName(raw: String) {
        val name = raw.take(MAX_NAME)
        nameOverride.value = name
        nameJob?.cancel()
        pendingName = name.trim()
        nameJob = viewModelScope.launch {
            delay(NAME_DEBOUNCE_MS)
            pendingName = null
            prefs.setUserName(name.trim())
        }
    }

    /** Writes a still-debounced name immediately (called from the screen on ON_STOP). */
    fun flushName() {
        val name = pendingName ?: return
        nameJob?.cancel()
        pendingName = null
        viewModelScope.launch { prefs.setUserName(name) }
    }

    override fun onCleared() {
        val name = pendingName ?: return
        pendingName = null
        // viewModelScope is already cancelled here; hand the last write to the application scope
        // so it survives the ViewModel without blocking the main thread.
        appScope.launch { prefs.setUserName(name) }
    }

    /** Runs in the application scope so leaving the screen cannot cancel the rescheduling. */
    private fun toggleReminders() {
        val enable = !state.value.remindersEnabled
        appScope.launch {
            prefs.setRemindersEnabled(enable)
            reminderScheduler.rescheduleAll()
            if (enable) emit(SettingsEffect.RequestNotificationPermission)
        }
    }

    private fun reminderStatus(remindersEnabled: Boolean, h: ReminderHealth) = when {
        !remindersEnabled -> ReminderStatus.OFF
        h.issue == ReminderIssue.NOTIFICATIONS_OFF -> ReminderStatus.NOTIFICATIONS_OFF
        h.issue == ReminderIssue.EXACT_ALARMS_OFF -> ReminderStatus.EXACT_ALARMS_OFF
        else -> ReminderStatus.WORKING
    }

    companion object {
        const val MAX_NAME = 24
        const val NAME_DEBOUNCE_MS = 400L
    }
}
