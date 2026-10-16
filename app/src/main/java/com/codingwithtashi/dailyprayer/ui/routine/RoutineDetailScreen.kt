package com.codingwithtashi.dailyprayer.ui.routine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.codingwithtashi.dailyprayer.reminders.ExactAlarmPermission
import com.codingwithtashi.dailyprayer.reminders.NotificationAccess
import com.codingwithtashi.dailyprayer.reminders.ReminderIssue
import com.codingwithtashi.dailyprayer.ui.components.rememberSafeLauncher
import com.codingwithtashi.dailyprayer.ui.components.AppText
import com.codingwithtashi.dailyprayer.playback.PlaybackError
import com.codingwithtashi.dailyprayer.ui.components.LocalToaster
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.core.text.TibetanNumerals
import com.codingwithtashi.dailyprayer.core.time.DateFormats
import com.codingwithtashi.dailyprayer.domain.StartLabel
import com.codingwithtashi.dailyprayer.ui.components.PillButton
import com.codingwithtashi.dailyprayer.ui.components.PillVariant
import com.codingwithtashi.dailyprayer.ui.components.ProgressBarThin
import com.codingwithtashi.dailyprayer.ui.components.RoutinePrayerRow
import com.codingwithtashi.dailyprayer.ui.components.TopBar
import com.codingwithtashi.dailyprayer.ui.components.contentWidth
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme

@Composable
fun RoutineDetailScreen(
    routineId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenReader: (prayerId: Int, routineId: Long) -> Unit,
    onOpenPlayer: () -> Unit,
    viewModel: RoutineDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toaster = LocalToaster.current
    val offlineMessage = stringResource(R.string.offline_cannot_stream)
    val errorMessage = stringResource(R.string.playback_error)
    val context = LocalContext.current
    val launch = rememberSafeLauncher()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onEvent(RoutineDetailEvent.RefreshDate) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                RoutineDetailEffect.OpenPlayer -> onOpenPlayer()
                is RoutineDetailEffect.PlaybackFailed ->
                    toaster.show(if (effect.error == PlaybackError.OFFLINE) offlineMessage else errorMessage)
            }
        }
    }
    LaunchedEffect(state.status) { if (state.status == RoutineDetailStatus.GONE) onBack() }
    RoutineDetailContent(
        state = state,
        onBack = onBack,
        onEdit = { onEdit(routineId) },
        onOpenPrayer = { onOpenReader(it, routineId) },
        onStart = { state.startTarget?.let { onOpenReader(it, routineId) } },
        onFixReminder = {
            val intent = when (state.reminderIssue) {
                ReminderIssue.NOTIFICATIONS_OFF -> NotificationAccess.settingsIntent(context)
                ReminderIssue.EXACT_ALARMS_OFF -> ExactAlarmPermission.requestIntent(context)
                null -> null
            }
            if (intent != null) launch(intent)
        },
        onListen = { viewModel.onEvent(RoutineDetailEvent.Listen) },
    )
}

@Composable
fun RoutineDetailContent(
    state: RoutineDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenPrayer: (Int) -> Unit,
    onStart: () -> Unit,
    onListen: () -> Unit,
    onFixReminder: () -> Unit = {},
) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    val lang = PrayerTheme.language
    Column(Modifier.fillMaxSize().background(c.background)) {
        TopBar(
            onBack = onBack,
            actions = {
                PillButton(
                    text = stringResource(R.string.edit),
                    onClick = onEdit,
                    variant = PillVariant.Outlined(),
                    height = 36.dp,
                    horizontalPadding = 14.dp,
                    modifier = Modifier.padding(end = 8.dp),
                    textStyle = t.bodyStrong,
                    contentColor = c.ink,
                )
            },
        )
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "head") {
                Column(Modifier.contentWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppText(state.name, style = t.headline, color = c.ink)
                    AppText(routineMeta(state), style = t.bodyMedium, color = c.textMuted)
                }
            }
            if (state.globalRemindersOff || state.reminderIssue != null) {
                item(key = "reminderNotice") { ReminderNoticeCard(state.reminderIssue, onFixReminder, Modifier.contentWidth()) }
            }
            item(key = "progress") {
                ProgressBarThin(state.progress.fraction, Modifier.contentWidth(), height = 6.dp, radius = 3.dp)
            }
            itemsIndexed(state.prayers, key = { _, p -> p.id }) { i, p ->
                RoutinePrayerRow(
                    index = p.index,
                    done = p.done,
                    tib = p.tib,
                    enWithMinutes = "${p.titleEn} · " + stringResource(R.string.minutes_short, TibetanNumerals.formatCount(p.estMinutes, lang)),
                    onClick = { onOpenPrayer(p.id) },
                    modifier = Modifier.contentWidth(),
                    numberText = TibetanNumerals.formatCount(p.index + 1, lang),
                    showDivider = true,
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(c.surface)
                .drawBehind {
                    drawRect(c.border, Offset.Zero, Size(size.width, 1.dp.toPx()))
                }
                .navigationBarsPadding()
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.contentWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                PillButton(
                    text = startLabelText(state),
                    onClick = onStart,
                    modifier = Modifier.weight(1f),
                    height = 52.dp,
                    enabled = state.startTarget != null,
                )
                if (state.hasAudio) {
                    PillButton(
                        text = stringResource(R.string.tab_listen),
                        onClick = onListen,
                        variant = PillVariant.Outlined(1.5.dp),
                        height = 52.dp,
                        horizontalPadding = 20.dp,
                        leadingIcon = R.drawable.ic_play,
                        leadingIconSize = 14.dp,
                    )
                }
            }
        }
    }
}

/** Compact warning under the meta line, with a button that opens the fixing settings screen. */
@Composable
private fun ReminderNoticeCard(issue: ReminderIssue?, onFix: () -> Unit, modifier: Modifier = Modifier) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    val shape = RoundedCornerShape(14.dp)
    val message = stringResource(
        when (issue) {
            null -> R.string.reminders_off_in_settings
            ReminderIssue.NOTIFICATIONS_OFF -> R.string.reminder_warn_notifications_off
            ReminderIssue.EXACT_ALARMS_OFF -> R.string.reminder_warn_exact_off
        },
    )
    Row(
        modifier.fillMaxWidth().background(c.surface, shape).border(1.dp, c.border, shape).padding(start = 14.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AppText(message, style = t.bodyMedium, color = c.ink, modifier = Modifier.weight(1f))
        if (issue != null) {
            PillButton(
                text = stringResource(if (issue == ReminderIssue.NOTIFICATIONS_OFF) R.string.reminder_fix_turn_on else R.string.reminder_fix_allow),
                onClick = onFix,
                variant = PillVariant.Outlined(),
                height = 48.dp,
                horizontalPadding = 16.dp,
                textStyle = t.bodyStrong,
                contentColor = c.accent,
            )
        }
    }
}

@Composable
private fun startLabelText(state: RoutineDetailUiState): String {
    val lang = PrayerTheme.language
    return when (state.startLabel) {
        StartLabel.START_READING -> stringResource(R.string.start_reading)
        StartLabel.READ_AGAIN -> stringResource(R.string.read_again)
        StartLabel.CONTINUE -> stringResource(
            R.string.continue_k_of_n,
            TibetanNumerals.formatCount(state.progress.doneCount + 1, lang),
            TibetanNumerals.formatCount(state.progress.total, lang),
        )
    }
}

/** "6:30 AM · Every day · Reminder on". */
@Composable
internal fun routineMeta(state: RoutineDetailUiState): String {
    val letters = stringArrayResource(R.array.week_days)
    val everyDay = stringResource(R.string.every_day)
    val reminderOn = stringResource(R.string.reminder_on)
    return buildRoutineMeta(
        time = DateFormats.time12(state.timeMinutes, PrayerTheme.language),
        days = if (state.everyDay) everyDay else state.days.sorted().joinToString(" ") { letters[it.value - 1] },
        reminderOn = if (state.reminderOn) reminderOn else null,
    )
}

internal fun buildRoutineMeta(time: String, days: String, reminderOn: String?): String =
    listOfNotNull(time, days, reminderOn).joinToString(" · ")
