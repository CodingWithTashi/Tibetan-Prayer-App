package com.codingwithtashi.dailyprayer.ui

import com.codingwithtashi.dailyprayer.core.error.ErrorReporter
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.core.text.PrayerText
import com.codingwithtashi.dailyprayer.core.time.AppClock
import com.codingwithtashi.dailyprayer.data.model.AppNotification
import com.codingwithtashi.dailyprayer.data.model.AppTextSize
import com.codingwithtashi.dailyprayer.data.model.Appearance
import com.codingwithtashi.dailyprayer.data.model.Prayer
import com.codingwithtashi.dailyprayer.data.model.PrayerCategory
import com.codingwithtashi.dailyprayer.data.model.Routine
import com.codingwithtashi.dailyprayer.data.model.RoutineCompletion
import com.codingwithtashi.dailyprayer.data.model.RoutineDraft
import com.codingwithtashi.dailyprayer.data.prefs.ReaderTextSize
import com.codingwithtashi.dailyprayer.data.prefs.UserPrefs
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import com.codingwithtashi.dailyprayer.data.repository.NotificationRepository
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.codingwithtashi.dailyprayer.data.repository.RoutineRepository
import com.codingwithtashi.dailyprayer.reminders.ArmedAlarm
import com.codingwithtashi.dailyprayer.reminders.ReminderHealth
import com.codingwithtashi.dailyprayer.reminders.ReminderHealthMonitor
import com.codingwithtashi.dailyprayer.reminders.ReminderScheduler
import com.codingwithtashi.dailyprayer.reminders.ReminderStateStore
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

// ---- Fixtures ----

fun testPrayer(
    id: Int,
    titleEn: String = "Prayer $id",
    title: String = "བོད$id",
    category: PrayerCategory = PrayerCategory.DAILY,
    isSaved: Boolean = false,
    savedLabel: String? = null,
    progress: Float = 0f,
    hasAudio: Boolean = false,
    count: Int = 0,
    slug: String? = "slug$id",
    syllable: String = "བ",
) = Prayer(
    id = id, slug = slug, title = title, titleEn = titleEn, content = "", category = category,
    sortOrder = id, audioUrl = if (hasAudio) "https://x/$id.mp3" else "", hasAudio = hasAudio, isDownloaded = false,
    audioPath = null, audioDurationMs = null, isSaved = isSaved, savedLabel = savedLabel, savedAt = null, count = count,
    lastReadProgress = progress, lastReadAt = null, syllable = syllable,
)

fun testRoutine(
    id: Long = 1,
    name: String = "Morning",
    time: Int = 600,
    days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    reminder: Boolean = true,
    prayers: List<Prayer> = listOf(testPrayer(1), testPrayer(2), testPrayer(3)),
) = Routine(id, name, time, days, reminder, prayers, createdAt = 0L)

// ---- Preferences ----

/** In-memory [UserPreferencesRepository]; [lastReadSets] counts [setLastReadPrayerId] calls. */
class FakePrefs(initial: UserPrefs = UserPrefs()) : UserPreferencesRepository {
    val state = MutableStateFlow(initial)
    var lastReadSets = 0
    override val prefs: Flow<UserPrefs> = state
    override suspend fun snapshot() = state.value
    override suspend fun setOnboarded(value: Boolean) { state.value = state.value.copy(onboarded = value) }
    override suspend fun setUserName(name: String) { state.value = state.value.copy(userName = name) }
    override suspend fun setAppLanguage(language: AppLanguage) { state.value = state.value.copy(appLanguage = language) }
    override suspend fun setAppearance(appearance: Appearance) { state.value = state.value.copy(appearance = appearance) }
    override suspend fun setAppTextSize(size: AppTextSize) { state.value = state.value.copy(appTextSize = size) }
    override suspend fun setTextSize(size: Int) { state.value = state.value.copy(textSize = ReaderTextSize.clamp(size)) }
    override suspend fun setRecitationGoal(goal: Int) { state.value = state.value.copy(recitationGoal = goal) }
    override suspend fun setRemindersEnabled(enabled: Boolean) { state.value = state.value.copy(remindersEnabled = enabled) }
    override suspend fun setKeepScreenOn(enabled: Boolean) { state.value = state.value.copy(keepScreenOn = enabled) }
    override suspend fun setLastReadPrayerId(id: Int?) { lastReadSets++; state.value = state.value.copy(lastReadPrayerId = id) }
    override suspend fun setLegacyCleanupDone(done: Boolean) { state.value = state.value.copy(legacyCleanupDone = done) }
    override suspend fun setNotificationPermissionAsked(asked: Boolean) { state.value = state.value.copy(notificationPermissionAsked = asked) }
    override suspend fun setWelcomeNotificationInserted(inserted: Boolean) { state.value = state.value.copy(welcomeNotificationInserted = inserted) }
}

/** In-memory [PrayerRepository]; mutations edit [all]; [progress] records [setReadProgress] calls. */
class FakePrayerRepo(initial: List<Prayer> = emptyList()) : PrayerRepository {
    val all = MutableStateFlow(initial)
    val continueReading = MutableStateFlow<Prayer?>(null)
    val progress = mutableListOf<Float>()
    var text: PrayerText = PrayerText(listOf("v"), listOf("p1", "p2"), 60, 3)
    private fun edit(id: Int, f: (Prayer) -> Prayer) { all.value = all.value.map { if (it.id == id) f(it) else it } }
    override fun observeAll() = all
    override fun observeSaved() = all.map { list -> list.filter { it.isSaved } }
    override fun observeWithAudio() = all.map { list -> list.filter { it.hasAudio } }
    override fun observe(id: Int) = all.map { l -> l.firstOrNull { it.id == id } }
    override suspend fun get(id: Int) = all.value.firstOrNull { it.id == id }
    override suspend fun getByIds(ids: List<Int>) = ids.mapNotNull { get(it) }
    override fun observeContinueReading(): Flow<Prayer?> = continueReading
    override suspend fun setSaved(id: Int, saved: Boolean) = edit(id) { it.copy(isSaved = saved) }
    override suspend fun setSavedLabel(id: Int, label: String?) = edit(id) { it.copy(savedLabel = label) }
    override suspend fun setCount(id: Int, count: Int) = edit(id) { it.copy(count = count) }
    override suspend fun incrementCount(id: Int): Int { edit(id) { it.copy(count = it.count + 1) }; return get(id)?.count ?: 0 }
    override suspend fun decrementCount(id: Int): Int { edit(id) { it.copy(count = maxOf(0, it.count - 1)) }; return get(id)?.count ?: 0 }
    override suspend fun setReadProgress(id: Int, progress: Float) { this.progress += progress }
    override suspend fun text(id: Int): PrayerText = text
    override suspend fun updateAudioDuration(id: Int, ms: Long) = edit(id) { it.copy(audioDurationMs = ms) }
    override suspend fun setDownloaded(id: Int, path: String, durationMs: Long?) =
        edit(id) { it.copy(isDownloaded = true, audioPath = path, audioDurationMs = durationMs ?: it.audioDurationMs) }
    override suspend fun clearDownloaded(id: Int) = edit(id) { it.copy(isDownloaded = false, audioPath = null) }
    override suspend fun upsertRemote(title: String, content: String, audioUrl: String?, titleEn: String?, category: String?) = 0
}

/** In-memory [RoutineRepository]; [saved] records drafts, [done] records [markDone] calls. */
class FakeRoutineRepo(initial: List<Routine> = emptyList()) : RoutineRepository {
    val routines = MutableStateFlow(initial)
    val completions = MutableStateFlow<List<RoutineCompletion>>(emptyList())
    val dates = MutableStateFlow<Set<LocalDate>>(emptySet())
    val saved = mutableListOf<RoutineDraft>()
    val done = mutableListOf<Pair<Long, Int>>()
    override fun observeRoutines() = routines
    override fun observeRoutine(id: Long) = routines.map { l -> l.firstOrNull { it.id == id } }
    override suspend fun getRoutine(id: Long) = routines.value.firstOrNull { it.id == id }
    override suspend fun getRoutines() = routines.value
    override suspend fun save(draft: RoutineDraft): Long { saved += draft; return saved.size.toLong() }
    override suspend fun delete(id: Long) { routines.value = routines.value.filterNot { it.id == id } }
    override suspend fun restore(routine: Routine) { routines.value = routines.value + routine }
    override fun observeCompletions(date: LocalDate) = completions.map { l -> l.filter { it.date == date } }
    override fun observeCompletionDates(): Flow<Set<LocalDate>> = dates
    override suspend fun markDone(routineId: Long, prayerId: Int, date: LocalDate?) { done += routineId to prayerId }
}

/** In-memory [NotificationRepository]; [unread] is settable and tracks [all] on every change. */
class FakeNotificationRepo(initial: List<AppNotification> = emptyList()) : NotificationRepository {
    val all = MutableStateFlow(initial)
    val unread = MutableStateFlow(initial.count { !it.isRead })
    val inserted = mutableListOf<AppNotification>()
    var insertFailure: Throwable? = null
    private fun set(list: List<AppNotification>) { all.value = list; unread.value = list.count { !it.isRead } }
    override fun observeAll(): Flow<List<AppNotification>> = all
    override fun unreadCount(): Flow<Int> = unread
    override suspend fun insert(notification: AppNotification): Long {
        insertFailure?.let { throw it }
        inserted += notification
        set(all.value + notification)
        return 0L
    }
    override suspend fun delete(id: Int) = set(all.value.filterNot { it.id == id })
    override suspend fun restore(notification: AppNotification) = set((all.value + notification).sortedByDescending { it.receivedAt })
    override suspend fun markAllRead() = set(all.value.map { it.copy(isRead = true) })
}

// ---- Time, scheduling, reporting ----

/** Mutable [AppClock]: set [instant] (or [date], interpreted at start of day in [zone]). */
class FakeClock(
    var instant: Instant = Instant.parse("2026-03-04T08:00:00Z"),
    private val zone: ZoneId = ZoneId.of("UTC"),
) : AppClock {
    constructor(date: LocalDate, zone: ZoneId = ZoneId.of("UTC")) : this(date.atStartOfDay(zone).toInstant(), zone)

    var date: LocalDate
        get() = today()
        set(value) { instant = value.atStartOfDay(zone).toInstant() }

    override fun now(): Instant = instant
    override fun today(): LocalDate = LocalDate.ofInstant(instant, zone)
    override fun zone(): ZoneId = zone
}

class RecordingScheduler : ReminderScheduler {
    val scheduled = mutableListOf<Long>()
    val cancelled = mutableListOf<Long>()
    var rescheduledAll = 0
    var ensured = 0
    /** Every call in order, e.g. "schedule:1", "cancel:1". */
    val calls = mutableListOf<String>()
    override suspend fun schedule(routine: Routine) { scheduled += routine.id; calls += "schedule:${routine.id}" }
    override suspend fun cancel(routineId: Long) { cancelled += routineId; calls += "cancel:$routineId" }
    override suspend fun rescheduleAll() { rescheduledAll++ }
    override suspend fun ensureScheduled() { ensured++ }
}

/** In-memory [ReminderStateStore]. */
class FakeReminderStateStore : ReminderStateStore {
    val armed = mutableMapOf<Long, ArmedAlarm>()
    override suspend fun get(routineId: Long) = armed[routineId]
    override suspend fun getAll(): Map<Long, ArmedAlarm> = armed.toMap()
    override suspend fun put(routineId: Long, alarm: ArmedAlarm) { armed[routineId] = alarm }
    override suspend fun remove(routineId: Long) { armed.remove(routineId) }
}

class RecordingErrorReporter : ErrorReporter {
    val errors = mutableListOf<Throwable>()
    override fun record(t: Throwable) { errors += t }
}

/** [ReminderHealthMonitor] whose [state] tests set directly; [refreshes] counts [refresh] calls. */
class FakeReminderHealthMonitor(
    initial: ReminderHealth = ReminderHealth(notificationsUsable = true, exactAlarmsAllowed = true),
) : ReminderHealthMonitor {
    val state = MutableStateFlow(initial)
    var refreshes = 0
    override val health: StateFlow<ReminderHealth> = state
    override fun refresh() { refreshes++ }
}
