package com.codingwithtashi.dailyprayer.ui.notifications

import com.codingwithtashi.dailyprayer.data.model.AppNotification
import com.codingwithtashi.dailyprayer.ui.FakeClock
import com.codingwithtashi.dailyprayer.ui.FakeNotificationRepo
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {
    private val now = Instant.parse("2026-10-14T15:05:00Z")

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun n(id: Int, at: Instant) = AppNotification(id, "T$id", "C$id", at.toEpochMilli(), null, false)

    @Test fun marksAllReadOnCollection() = runTest(UnconfinedTestDispatcher()) {
        val repo = FakeNotificationRepo(listOf(n(1, now)))
        val vm = NotificationsViewModel(repo, FakeClock(now))
        backgroundScope.launch { vm.state.collect {} }
        assertThat(repo.all.value.all { it.isRead }).isTrue()
    }

    @Test fun timeLabelTodayVersusOtherDay() = runTest(UnconfinedTestDispatcher()) {
        val repo = FakeNotificationRepo(listOf(n(1, now), n(2, Instant.parse("2026-10-02T08:00:00Z"))))
        val vm = NotificationsViewModel(repo, FakeClock(now))
        backgroundScope.launch { vm.state.collect {} }
        val labels = vm.state.value.items!!.map { it.timeLabel }
        assertThat(labels).containsExactly("3:05 PM", "2 Oct").inOrder()
    }

    @Test fun deleteThenUndoRestores() = runTest(UnconfinedTestDispatcher()) {
        val repo = FakeNotificationRepo(listOf(n(1, now)))
        val vm = NotificationsViewModel(repo, FakeClock(now))
        backgroundScope.launch { vm.state.collect {} }
        val item = vm.state.value.items!!.single()
        val effects = mutableListOf<NotificationsEffect>()
        backgroundScope.launch { vm.effects.collect { effects += it } }
        vm.onEvent(NotificationsEvent.Delete(item))
        assertThat(repo.all.value).isEmpty()
        val deleted = effects.single() as NotificationsEffect.Deleted
        vm.onEvent(NotificationsEvent.UndoDelete(deleted.notification))
        assertThat(repo.all.value.map { it.id }).containsExactly(1)
    }
}
