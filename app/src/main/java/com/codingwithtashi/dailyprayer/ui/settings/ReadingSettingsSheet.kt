package com.codingwithtashi.dailyprayer.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.core.text.TibetanNumerals
import com.codingwithtashi.dailyprayer.data.model.Appearance
import com.codingwithtashi.dailyprayer.ui.components.AppearanceKind
import com.codingwithtashi.dailyprayer.ui.components.AppearanceOptionCard
import com.codingwithtashi.dailyprayer.ui.components.CircleButton
import com.codingwithtashi.dailyprayer.ui.components.GoalPill
import com.codingwithtashi.dailyprayer.ui.components.ProgressBarThin
import com.codingwithtashi.dailyprayer.ui.components.SectionLabel
import com.codingwithtashi.dailyprayer.ui.components.SheetHandle
import com.codingwithtashi.dailyprayer.ui.theme.LocalAppLanguage
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme

const val MIN_TEXT_SIZE = 16
const val MAX_TEXT_SIZE = 32
const val TEXT_SIZE_STEP = 2

data class ReadingSettingsState(val textSizeSp: Int, val appearance: Appearance, val goal: Int)

/** Stateless reading settings sheet (text size, appearance, recitation goal); reused by the Reader. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsSheet(
    state: ReadingSettingsState,
    onSizeDown: () -> Unit,
    onSizeUp: () -> Unit,
    onAppearance: (Appearance) -> Unit,
    onGoal: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    val language = LocalAppLanguage.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
        scrimColor = c.scrim.copy(alpha = 0.45f),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = { SheetHandle() },
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 22.dp, end = 22.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(stringResource(R.string.reading), style = t.sheetTitle, color = c.ink)
            val sizeLabel = TibetanNumerals.formatCount(state.textSizeSp, language)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                CircleButton(
                    size = 46.dp,
                    contentDescription = stringResource(R.string.cd_decrease_text),
                    onClick = onSizeDown,
                    enabled = state.textSizeSp > MIN_TEXT_SIZE,
                    border = BorderStroke(1.dp, c.border),
                ) { Text("ཀ", style = t.tibetanTile16, color = c.ink) }
                ProgressBarThin(
                    progress = ((state.textSizeSp - MIN_TEXT_SIZE).toFloat() / (MAX_TEXT_SIZE - MIN_TEXT_SIZE)).coerceIn(0f, 1f),
                    modifier = Modifier.weight(1f).semantics { stateDescription = sizeLabel },
                    height = 4.dp,
                    trackColor = c.border,
                    fillColor = c.accent,
                    announceProgress = false,
                )
                CircleButton(
                    size = 46.dp,
                    contentDescription = stringResource(R.string.cd_increase_text),
                    onClick = onSizeUp,
                    enabled = state.textSizeSp < MAX_TEXT_SIZE,
                    border = BorderStroke(1.dp, c.border),
                ) { Text("ཀ", style = t.tibetanTile16.copy(fontSize = 26.sp), color = c.ink) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppearanceOptionCard(AppearanceKind.System, stringResource(R.string.system_default), state.appearance == Appearance.SYSTEM, { onAppearance(Appearance.SYSTEM) }, Modifier.weight(1f))
                AppearanceOptionCard(AppearanceKind.Light, stringResource(R.string.light), state.appearance == Appearance.LIGHT, { onAppearance(Appearance.LIGHT) }, Modifier.weight(1f))
                AppearanceOptionCard(AppearanceKind.Dark, stringResource(R.string.dark), state.appearance == Appearance.DARK, { onAppearance(Appearance.DARK) }, Modifier.weight(1f))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.recitation_goal))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(21 to TibetanNumerals.formatCount(21, language), 108 to TibetanNumerals.formatCount(108, language), 1000 to "1k", 10000 to "10k")
                        .forEach { (goal, label) ->
                            GoalPill(label, state.goal == goal, { onGoal(goal) }, Modifier.weight(1f))
                        }
                }
            }
        }
    }
}
