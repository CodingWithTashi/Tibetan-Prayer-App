package com.codingwithtashi.dailyprayer.navigation

import com.codingwithtashi.dailyprayer.core.notification.DeepLink
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Carries deep links from the Activity (intents) to the nav host. The last link is replayed so one
 * that arrives before the host is composed (cold start) is not lost; the host calls [consumed]
 * once handled so it is not replayed after a configuration change.
 */
@Singleton
class NavigationEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<DeepLink>(replay = 1, extraBufferCapacity = 8)
    val events: SharedFlow<DeepLink> = _events.asSharedFlow()

    fun emit(link: DeepLink) {
        _events.tryEmit(link)
    }

    fun consumed() {
        _events.resetReplayCache()
    }
}

/** Exposes the singleton [NavigationEventBus] to composables. */
@dagger.hilt.android.lifecycle.HiltViewModel
class NavigationEventsViewModel @Inject constructor(val bus: NavigationEventBus) : androidx.lifecycle.ViewModel()
