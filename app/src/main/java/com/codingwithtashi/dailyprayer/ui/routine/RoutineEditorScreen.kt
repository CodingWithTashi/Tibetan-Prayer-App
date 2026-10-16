package com.codingwithtashi.dailyprayer.ui.routine

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.core.text.TibetanNumerals
import com.codingwithtashi.dailyprayer.core.time.DateFormats
import com.codingwithtashi.dailyprayer.reminders.ExactAlarmPermission
import com.codingwithtashi.dailyprayer.ui.components.DashedBox
import com.codingwithtashi.dailyprayer.ui.components.DayChip
import com.codingwithtashi.dailyprayer.ui.components.EditorPrayerRow
import com.codingwithtashi.dailyprayer.ui.components.LocalToaster
import com.codingwithtashi.dailyprayer.ui.components.PickRow
import com.codingwithtashi.dailyprayer.ui.components.PillButton
import com.codingwithtashi.dailyprayer.ui.components.PillVariant
import com.codingwithtashi.dailyprayer.ui.components.PrayerInput
import com.codingwithtashi.dailyprayer.ui.components.PrayerTimePickerDialog
import com.codingwithtashi.dailyprayer.ui.components.SectionLabel
import com.codingwithtashi.dailyprayer.ui.components.SheetHandle
import com.codingwithtashi.dailyprayer.ui.components.TimeChip
import com.codingwithtashi.dailyprayer.ui.components.ToggleSwitch
import com.codingwithtashi.dailyprayer.ui.components.contentWidth
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Composable
fun RoutineEditorScreen(
    routineId: Long?,
    onClose: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: RoutineEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val toaster = LocalToaster.current
    // Toasts outlive this screen (Saved / Deleted pop it immediately).
    val toastScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
    val scope = rememberCoroutineScope()

    fun afterPermissions() {
        scope.launch {
            if (!ExactAlarmPermission.canScheduleExact(context) && viewModel.promptExactAlarmOnce()) return@launch
            onClose()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { afterPermissions() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is RoutineEditorEffect.Toast -> {
                    val msg = resources.getString(effect.res, *effect.args.toTypedArray())
                    toastScope.launch { toaster.show(msg) }
                }
                is RoutineEditorEffect.Saved -> {
                    if (!effect.reminderOn) {
                        onClose()
                    } else {
                        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
                            viewModel.shouldAskNotificationPermission()
                        if (needsPermission) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else afterPermissions()
                    }
                }
                is RoutineEditorEffect.Deleted -> {
                    val msg = resources.getString(R.string.routine_deleted)
                    val undo = resources.getString(R.string.undo)
                    toastScope.launch { toaster.show(msg, undo) { viewModel.undoDelete(effect.routine) } }
                    onDeleted()
                }
                RoutineEditorEffect.Close -> onClose()
            }
        }
    }

    BackHandler { viewModel.onEvent(RoutineEditorEvent.Cancel) }

    RoutineEditorContent(state = state, onEvent = viewModel::onEvent)

    if (state.showExactAlarmPrompt) {
        ExactAlarmPromptDialog(
            onOpenSettings = {
                viewModel.dismissExactAlarmPrompt()
                ExactAlarmPermission.requestIntent(context)?.let {
                    try { context.startActivity(it) } catch (_: ActivityNotFoundException) { }
                }
                onClose()
            },
            onDismiss = {
                viewModel.dismissExactAlarmPrompt()
                onClose()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorContent(state: RoutineEditorUiState, onEvent: (RoutineEditorEvent) -> Unit) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    val lang = PrayerTheme.language
    Column(Modifier.fillMaxSize().background(c.background)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(52.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PillButton(
                stringResource(R.string.cancel), { onEvent(RoutineEditorEvent.Cancel) },
                variant = PillVariant.Text, height = 44.dp, horizontalPadding = 8.dp,
                textStyle = t.body15Semi, contentColor = c.textMuted,
            )
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(if (state.isNew) R.string.new_routine else R.string.edit_routine),
                    style = t.body15Semi, color = c.ink, maxLines = 1,
                )
            }
            PillButton(
                stringResource(R.string.save), { onEvent(RoutineEditorEvent.Save) },
                variant = PillVariant.Filled, height = 36.dp, horizontalPadding = 16.dp, radius = 18.dp,
                textStyle = t.body14Semi,
                containerColor = if (state.canSave) Color.Unspecified else c.border,
                contentColor = if (state.canSave) Color.Unspecified else c.textSubtle,
            )
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().imePadding(),
            contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "name") {
                Column(Modifier.contentWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(stringResource(R.string.name))
                    PrayerInput(
                        value = state.name,
                        onValueChange = { onEvent(RoutineEditorEvent.SetName(it)) },
                        height = 52.dp, radius = 14.dp, textStyle = t.input16,
                        placeholder = stringResource(R.string.routine_ph),
                        maxLength = RoutineEditorUiState.MAX_NAME,
                    )
                }
            }
            item(key = "card") { ScheduleCard(state, onEvent) }
            item(key = "prayers") {
                Column(Modifier.contentWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel(stringResource(R.string.prayers_count, TibetanNumerals.formatCount(state.prayers.size, lang)))
                    Column {
                        state.prayers.forEachIndexed { i, p ->
                            EditorPrayerRow(
                                syllable = p.syllable, category = p.category, tib = p.tib, en = p.en,
                                canMoveUp = i > 0,
                                onMoveUp = { onEvent(RoutineEditorEvent.MoveUp(i)) },
                                onRemove = { onEvent(RoutineEditorEvent.Remove(p.id)) },
                            )
                        }
                    }
                    DashedBox(
                        Modifier.fillMaxWidth().height(50.dp).clickable(role = Role.Button) { onEvent(RoutineEditorEvent.OpenPicker) },
                        radius = 14.dp,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(painterResource(R.drawable.ic_plus), null, Modifier.size(16.dp), tint = c.accent)
                            Text(stringResource(R.string.add_prayers), style = t.body14Semi, color = c.accent)
                        }
                    }
                    if (!state.isNew) {
                        PillButton(
                            stringResource(R.string.delete_routine), { onEvent(RoutineEditorEvent.Delete) },
                            modifier = Modifier.fillMaxWidth(), variant = PillVariant.Text, height = 48.dp,
                            textStyle = t.body14Semi, contentColor = c.danger,
                        )
                    }
                }
            }
        }
    }

    if (state.showTimePicker) {
        PrayerTimePickerDialog(
            initialMinutes = state.timeMinutes,
            onConfirm = { onEvent(RoutineEditorEvent.SetTime(it)) },
            onDismiss = { onEvent(RoutineEditorEvent.CloseTimePicker) },
        )
    }
    if (state.showPicker) PrayerPickerSheet(state, onEvent)
    if (state.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(RoutineEditorEvent.DismissDiscard) },
            containerColor = c.surface,
            title = { Text(stringResource(R.string.discard_changes_q), style = t.sheetTitle, color = c.ink) },
            confirmButton = {
                TextButton({ onEvent(RoutineEditorEvent.ConfirmDiscard) }) {
                    Text(stringResource(R.string.discard), style = t.body14Semi, color = c.danger)
                }
            },
            dismissButton = {
                TextButton({ onEvent(RoutineEditorEvent.DismissDiscard) }) {
                    Text(stringResource(R.string.cancel), style = t.body14Semi, color = c.textMuted)
                }
            },
        )
    }
}

@Composable
private fun ScheduleCard(state: RoutineEditorUiState, onEvent: (RoutineEditorEvent) -> Unit) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    val letters = stringArrayResource(R.array.week_days)
    val names = stringArrayResource(R.array.weekday_names)
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.contentWidth().background(c.surface, shape).border(1.dp, c.border, shape).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.time), style = t.body15Semi, color = c.ink, modifier = Modifier.weight(1f))
            TimeChip(DateFormats.time12(state.timeMinutes, PrayerTheme.language), { onEvent(RoutineEditorEvent.OpenTimePicker) })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            state.days.forEachIndexed { i, on ->
                DayChip(letters[i], on, { onEvent(RoutineEditorEvent.ToggleDay(i)) }, names.getOrElse(i) { letters[i] })
            }
        }
        HorizontalDivider(color = c.border)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 40.dp)
                .toggleable(state.reminder, role = Role.Switch, onValueChange = { onEvent(RoutineEditorEvent.ToggleReminder) }),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.reminder), style = t.body15Semi, color = c.ink)
                Text(stringResource(R.string.reminder_sub), style = t.caption12, color = c.textMuted)
            }
            ToggleSwitch(state.reminder, null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrayerPickerSheet(state: RoutineEditorUiState, onEvent: (RoutineEditorEvent) -> Unit) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onEvent(RoutineEditorEvent.ClosePicker) }
    }
    val picked = state.pickedIds
    ModalBottomSheet(
        onDismissRequest = { onEvent(RoutineEditorEvent.ClosePicker) },
        sheetState = sheetState,
        containerColor = c.surface,
        dragHandle = { SheetHandle() },
    ) {
        Column(Modifier.heightIn(max = 620.dp)) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.add_prayers), style = t.sheetTitle, color = c.ink, modifier = Modifier.weight(1f))
                PillButton(
                    stringResource(R.string.done), { close() },
                    height = 36.dp, horizontalPadding = 16.dp, radius = 18.dp, textStyle = t.body14Semi,
                )
            }
            LazyColumn(Modifier.weight(1f, fill = false), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp)) {
                items(state.allPrayers, key = { it.id }) { p ->
                    PickRow(
                        syllable = p.syllable, category = p.category, tib = p.tib, en = p.en,
                        checked = p.id in picked, onToggle = { onEvent(RoutineEditorEvent.TogglePick(p.id)) },
                        compact = true,
                    )
                }
            }
        }
    }
}
