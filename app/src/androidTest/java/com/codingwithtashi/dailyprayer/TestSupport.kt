package com.codingwithtashi.dailyprayer

import com.codingwithtashi.dailyprayer.core.init.AppInitializer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

/** Runs the app initialiser (HiltTestApplication does not) and waits until seeding finished. */
suspend fun AppInitializer.startAndAwait() {
    start()
    withTimeout(20_000) { ready.first { it } }
}
