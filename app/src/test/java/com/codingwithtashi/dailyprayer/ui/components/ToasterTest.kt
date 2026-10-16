package com.codingwithtashi.dailyprayer.ui.components

import androidx.compose.material3.SnackbarHostState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ToasterTest {
    private val host = SnackbarHostState()
    private val toaster = Toaster(host)

    @Test fun `plain toast disappears after two seconds`() = runTest {
        launch { toaster.show("hi") }
        runCurrent()
        assertThat(host.currentSnackbarData).isNotNull()
        advanceTimeBy(Toaster.PLAIN_MS - 1)
        runCurrent()
        assertThat(host.currentSnackbarData).isNotNull()
        advanceTimeBy(2)
        runCurrent()
        assertThat(host.currentSnackbarData).isNull()
    }

    @Test fun `toast with action lasts three and a half seconds`() = runTest {
        launch { toaster.show("hi", "Undo") {} }
        runCurrent()
        advanceTimeBy(Toaster.PLAIN_MS + 1)
        runCurrent()
        assertThat(host.currentSnackbarData).isNotNull()
        advanceTimeBy(Toaster.WITH_ACTION_MS)
        runCurrent()
        assertThat(host.currentSnackbarData).isNull()
    }

    @Test fun `undo runs only when the action is performed`() = runTest {
        var undone = 0
        launch { toaster.show("hi", "Undo") { undone++ } }
        runCurrent()
        host.currentSnackbarData!!.performAction()
        runCurrent()
        assertThat(undone).isEqualTo(1)
    }

    @Test fun `timeout and dismissal do not run undo`() = runTest {
        var undone = 0
        launch { toaster.show("a", "Undo") { undone++ } }
        runCurrent()
        advanceTimeBy(Toaster.WITH_ACTION_MS + 1)
        runCurrent()
        launch { toaster.show("b", "Undo") { undone++ } }
        runCurrent()
        host.currentSnackbarData!!.dismiss()
        runCurrent()
        assertThat(undone).isEqualTo(0)
        assertThat(host.currentSnackbarData).isNull()
    }
}
