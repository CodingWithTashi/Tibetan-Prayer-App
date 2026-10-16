package com.codingwithtashi.dailyprayer.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codingwithtashi.dailyprayer.core.time.AppClock
import com.codingwithtashi.dailyprayer.data.model.AppNotification
import com.codingwithtashi.dailyprayer.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationItem(
    val notification: AppNotification,
    val timeLabel: String,
) {
    val id: Int get() = notification.id
}

/** `null` until the first emission so the screen can avoid flashing the empty state. */
data class NotificationsUiState(val items: List<NotificationItem>? = null)

/** User actions on the Notifications screen. */
sealed interface NotificationsEvent {
    data class Delete(val item: NotificationItem) : NotificationsEvent
    data class UndoDelete(val notification: AppNotification) : NotificationsEvent
}

/** One-shot effects the Notifications screen shows. */
sealed interface NotificationsEffect {
    /** Show the "deleted" toast; Undo sends [NotificationsEvent.UndoDelete]. */
    data class Deleted(val notification: AppNotification) : NotificationsEffect
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val clock: AppClock,
) : ViewModel() {

    private val effectChannel = Channel<NotificationsEffect>(Channel.BUFFERED)
    val effects: Flow<NotificationsEffect> = effectChannel.receiveAsFlow()

    val state: StateFlow<NotificationsUiState> = repository.observeAll()
        .onEach { list -> if (list.any { !it.isRead }) repository.markAllRead() }
        .map { list -> NotificationsUiState(list.map { NotificationItem(it, timeLabel(it.receivedAt)) }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationsUiState())

    fun onEvent(event: NotificationsEvent) {
        when (event) {
            is NotificationsEvent.Delete -> delete(event.item)
            is NotificationsEvent.UndoDelete -> undoDelete(event.notification)
        }
    }

    private fun delete(item: NotificationItem) {
        viewModelScope.launch {
            repository.delete(item.id)
            effectChannel.send(NotificationsEffect.Deleted(item.notification))
        }
    }

    private fun undoDelete(notification: AppNotification) {
        viewModelScope.launch { repository.restore(notification) }
    }

    /** "h:mm a" for today, "d MMM" otherwise; English digits regardless of language. */
    internal fun timeLabel(receivedAt: Long): String {
        val zoned = Instant.ofEpochMilli(receivedAt).atZone(clock.zone())
        return if (zoned.toLocalDate() == clock.today()) TIME.format(zoned).uppercase(Locale.UK) else DATE.format(zoned)
    }

    private companion object {
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.UK)
        val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.UK)
    }
}
