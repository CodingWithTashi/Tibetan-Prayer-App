package com.codingwithtashi.dailyprayer.ui.today

import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.core.text.PrayerText
import com.codingwithtashi.dailyprayer.core.time.AppClock
import com.codingwithtashi.dailyprayer.data.model.AppNotification
import com.codingwithtashi.dailyprayer.data.model.Appearance
import com.codingwithtashi.dailyprayer.data.model.Prayer
import com.codingwithtashi.dailyprayer.data.model.PrayerCategory
import com.codingwithtashi.dailyprayer.data.model.Routine
import com.codingwithtashi.dailyprayer.data.model.RoutineCompletion
import com.codingwithtashi.dailyprayer.data.model.RoutineDraft
import com.codingwithtashi.dailyprayer.data.prefs.UserPrefs
import com.codingwithtashi.dailyprayer.data.prefs.UserPreferencesRepository
import com.codingwithtashi.dailyprayer.data.repository.NotificationRepository
import com.codingwithtashi.dailyprayer.data.repository.PrayerRepository
import com.codingwithtashi.dailyprayer.data.repository.RoutineRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

fun testPrayer(
    id: Int,
    slug: String? = "slug$id",
    titleEn: String = "Prayer $id",
    savedLabel: String? = null,
    isSaved: Boolean = false,
    progress: Float = 0f,
) = Prayer(
    id = id, slug = slug, title = "བོད$id", titleEn = titleEn, content = "", category = PrayerCategory.DAILY,
    sortOrder = id, audioUrl = "", hasAudio = false, isDownloaded = false, audioPath = null, audioDurationMs = null,
    isSaved = isSaved, savedLabel = savedLabel, savedAt = null, count = 0, lastReadProgress = progress,
    lastReadAt = null, syllable = "བ",
)

class FakePrefs(initial: UserPrefs = UserPrefs()) : UserPreferencesRepository {
    val state = MutableStateFlow(initial)
    override val prefs: Flow<UserPrefs> = state
    override suspend fun snapshot() = state.value
    override suspend fun setOnboarded(value: Boolean) { state.value = state.value.copy(onboarded = value) }
    override suspend fun setUserName(name: String) { state.value = state.value.copy(userName = name) }
    override suspend fun setAppLanguage(language: AppLanguage) { state.value = state.value.copy(appLanguage = language) }
    override suspend fun setAppearance(appearance: Appearance) { state.value = state.value.copy(appearance = appearance) }
    override suspend fun setTextSize(size: Int) { state.value = state.value.copy(textSize = size) }
    override suspend fun setRecitationGoal(goal: Int) { state.value = state.value.copy(recitationGoal = goal) }
    override suspend fun setRemindersEnabled(enabled: Boolean) { state.value = state.value.copy(remindersEnabled = enabled) }
    override suspend fun setKeepScreenOn(enabled: Boolean) { state.value = state.value.copy(keepScreenOn = enabled) }
    override suspend fun setLastReadPrayerId(id: Int?) { state.value = state.value.copy(lastReadPrayerId = id) }
    override suspend fun setLegacyCleanupDone(done: Boolean) { state.value = state.value.copy(legacyCleanupDone = done) }
    override suspend fun setExactAlarmPromptShown(shown: Boolean) { state.value = state.value.copy(exactAlarmPromptShown = shown) }
    override suspend fun setNotificationPermissionAsked(asked: Boolean) { state.value = state.value.copy(notificationPermissionAsked = asked) }
    override suspend fun setWelcomeNotificationInserted(inserted: Boolean) { state.value = state.value.copy(welcomeNotificationInserted = inserted) }
}

class FakePrayerRepo(all: List<Prayer> = emptyList()) : PrayerRepository {
    val all = MutableStateFlow(all)
    val continueReading = MutableStateFlow<Prayer?>(null)
    override fun observeAll() = all
    override fun observeSaved() = all.map { list -> list.filter { it.isSaved } }
    override fun observeWithAudio() = all
    override fun observe(id: Int) = all.map { l -> l.firstOrNull { it.id == id } }
    override suspend fun get(id: Int) = all.value.firstOrNull { it.id == id }
    override suspend fun getByIds(ids: List<Int>) = ids.mapNotNull { get(it) }
    override fun observeContinueReading(): Flow<Prayer?> = continueReading
    override suspend fun setSaved(id: Int, saved: Boolean) = Unit
    override suspend fun setSavedLabel(id: Int, label: String?) = Unit
    override suspend fun setCount(id: Int, count: Int) = Unit
    override suspend fun incrementCount(id: Int) = 0
    override suspend fun decrementCount(id: Int) = 0
    override suspend fun setReadProgress(id: Int, progress: Float) = Unit
    override suspend fun text(id: Int): PrayerText = error("unused")
    override suspend fun updateAudioDuration(id: Int, ms: Long) = Unit
    override suspend fun upsertRemote(title: String, content: String, audioUrl: String?, titleEn: String?, category: String?) = 0
}

class FakeRoutineRepo : RoutineRepository {
    val routines = MutableStateFlow<List<Routine>>(emptyList())
    val completions = MutableStateFlow<List<RoutineCompletion>>(emptyList())
    val dates = MutableStateFlow<Set<LocalDate>>(emptySet())
    val saved = mutableListOf<RoutineDraft>()
    override fun observeRoutines() = routines
    override fun observeRoutine(id: Long) = routines.map { l -> l.firstOrNull { it.id == id } }
    override suspend fun getRoutine(id: Long) = routines.value.firstOrNull { it.id == id }
    override suspend fun getRoutines() = routines.value
    override suspend fun save(draft: RoutineDraft): Long { saved += draft; return saved.size.toLong() }
    override suspend fun delete(id: Long) = Unit
    override suspend fun restore(routine: Routine) = Unit
    override fun observeCompletions(date: LocalDate) = completions.map { l -> l.filter { it.date == date } }
    override fun observeCompletionDates(): Flow<Set<LocalDate>> = dates
    override suspend fun markDone(routineId: Long, prayerId: Int, date: LocalDate?) = Unit
}

class FakeNotificationRepo(unread: Int = 0) : NotificationRepository {
    val unread = MutableStateFlow(unread)
    override fun observeAll(): Flow<List<AppNotification>> = MutableStateFlow(emptyList())
    override fun unreadCount(): Flow<Int> = unread
    override suspend fun insert(notification: AppNotification) = 0L
    override suspend fun delete(id: Int) = Unit
    override suspend fun restore(notification: AppNotification) = Unit
    override suspend fun markAllRead() = Unit
}

class FixedClock(var date: LocalDate) : AppClock {
    override fun now(): Instant = date.atStartOfDay(zone()).toInstant()
    override fun today(): LocalDate = date
    override fun zone(): ZoneId = ZoneId.of("UTC")
}
