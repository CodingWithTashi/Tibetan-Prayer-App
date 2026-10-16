package com.codingwithtashi.dailyprayer.ui.notifications

import com.codingwithtashi.dailyprayer.core.time.AppClock
import com.codingwithtashi.dailyprayer.data.model.AppNotification
import com.codingwithtashi.dailyprayer.data.repository.NotificationRepository
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

private class FakeNotifications(initial: List<AppNotification>) : NotificationRepository {
    val all = MutableStateFlow(initial)
    override fun observeAll(): Flow<List<AppNotification>> = all
    override fun unreadCount(): Flow<Int> = all.map { l -> l.count { !it.isRead } }
    override suspend fun insert(notification: AppNotification): Long = 0
    override suspend fun delete(id: Int) { all.value = all.value.filterNot { it.id == id } }
    override suspend fun restore(notification: AppNotification) { all.value = (all.value + notification).sortedByDescending { it.receivedAt } }
    override suspend fun markAllRead() { all.value = all.value.map { it.copy(isRead = true) } }
}

private class FixedClock(private val now: Instant) : AppClock {
    override fun now() = now
    override fun zone(): ZoneId = ZoneOffset.UTC
    override fun today(): LocalDate = LocalDate.ofInstant(now, ZoneOffset.UTC)
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {
    private val now = Instant.parse("2026-10-14T15:05:00Z")

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun n(id: Int, at: Instant) = AppNotification(id, "T$id", "C$id", at.toEpochMilli(), null, false)

    @Test fun marksAllReadOnCollection() = runTest(UnconfinedTestDispatcher()) {
        val repo = FakeNotifications(listOf(n(1, now)))
        val vm = NotificationsViewModel(repo, FixedClock(now))
        backgroundScope.launch { vm.state.collect {} }
        assertThat(repo.all.value.all { it.isRead }).isTrue()
    }

    @Test fun timeLabelTodayVersusOtherDay() = runTest(UnconfinedTestDispatcher()) {
        val repo = FakeNotifications(listOf(n(1, now), n(2, Instant.parse("2026-10-02T08:00:00Z"))))
        val vm = NotificationsViewModel(repo, FixedClock(now))
        backgroundScope.launch { vm.state.collect {} }
        val labels = vm.state.value.items!!.map { it.timeLabel }
        assertThat(labels).containsExactly("3:05 PM", "2 Oct").inOrder()
    }

    @Test fun deleteThenUndoRestores() = runTest(UnconfinedTestDispatcher()) {
        val repo = FakeNotifications(listOf(n(1, now)))
        val vm = NotificationsViewModel(repo, FixedClock(now))
        backgroundScope.launch { vm.state.collect {} }
        val item = vm.state.value.items!!.single()
        val events = mutableListOf<NotificationsEvent>()
        backgroundScope.launch { vm.events.collect { events += it } }
        vm.delete(item)
        assertThat(repo.all.value).isEmpty()
        val deleted = events.single() as NotificationsEvent.Deleted
        vm.undoDelete(deleted.notification)
        assertThat(repo.all.value.map { it.id }).containsExactly(1)
    }
}
