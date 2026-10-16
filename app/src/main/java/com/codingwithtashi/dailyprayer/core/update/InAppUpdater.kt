package com.codingwithtashi.dailyprayer.core.update

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.codingwithtashi.dailyprayer.core.di.ApplicationScope
import com.codingwithtashi.dailyprayer.core.time.AppClock
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Google Play in-app updates: a flexible download normally, an immediate one for high-priority releases.
 * Every call is a silent no-op when the app was not installed by Play (debug, sideloaded, stage).
 */
@Singleton
class InAppUpdater @Inject constructor(
    private val appUpdateManager: AppUpdateManager,
    private val store: UpdatePromptStore,
    private val clock: AppClock,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val restartChannel = Channel<Unit>(Channel.CONFLATED)

    /** Emits when a flexible update has finished downloading and the user should be offered a restart. */
    val restartPrompts: Flow<Unit> = restartChannel.receiveAsFlow()

    private var checked = false
    private var pendingVersionCode = 0
    private var pendingType = AppUpdateType.FLEXIBLE

    init {
        appUpdateManager.registerListener(
            InstallStateUpdatedListener { state ->
                if (state.installStatus() == InstallStatus.DOWNLOADED) restartChannel.trySend(Unit)
            },
        )
    }

    /** Call once per fresh launch. */
    fun checkForUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        if (checked) return
        checked = true
        scope.launch {
            val info = fetchInfo() ?: return@launch
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                restartChannel.trySend(Unit)
                return@launch
            }
            val declined = store.declined()
            val decision = UpdatePolicy.decide(
                updateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE,
                flexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE),
                immediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE),
                priority = info.updatePriority(),
                availableVersionCode = info.availableVersionCode(),
                declinedVersionCode = declined?.first,
                declinedAtMs = declined?.second,
                nowMs = clock.now().toEpochMilli(),
            )
            val type = when (decision) {
                UpdateDecision.NONE -> return@launch
                UpdateDecision.FLEXIBLE -> AppUpdateType.FLEXIBLE
                UpdateDecision.IMMEDIATE -> AppUpdateType.IMMEDIATE
            }
            pendingVersionCode = info.availableVersionCode()
            pendingType = type
            startFlow(info, type, launcher)
        }
    }

    /** Re-offers a finished download, and resumes an immediate update the user left half-way. */
    fun onResume(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        scope.launch {
            val info = fetchInfo() ?: return@launch
            when {
                info.installStatus() == InstallStatus.DOWNLOADED -> restartChannel.trySend(Unit)
                // The priority guard keeps a running flexible download from being turned into an immediate one.
                info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS &&
                    info.updatePriority() >= UpdatePolicy.HIGH_PRIORITY -> {
                    pendingType = AppUpdateType.IMMEDIATE
                    startFlow(info, AppUpdateType.IMMEDIATE, launcher)
                }
            }
        }
    }

    /** Result of the Play update dialog; a cancelled flexible update snoozes the prompt for that version. */
    fun onFlowResult(resultCode: Int) {
        if (pendingType == AppUpdateType.FLEXIBLE && resultCode == Activity.RESULT_CANCELED) {
            scope.launch { store.recordDecline(pendingVersionCode, clock.now().toEpochMilli()) }
        }
    }

    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    private suspend fun fetchInfo(): AppUpdateInfo? = try {
        appUpdateManager.requestAppUpdateInfo()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null // Always fails when not installed by Play; never worth surfacing.
    }

    private suspend fun startFlow(info: AppUpdateInfo, type: Int, launcher: ActivityResultLauncher<IntentSenderRequest>) {
        try {
            withContext(Dispatchers.Main) {
                appUpdateManager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Play can refuse to start the flow; the next launch asks again.
        }
    }
}
