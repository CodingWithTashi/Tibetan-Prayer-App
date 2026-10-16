package com.codingwithtashi.dailyprayer.ui.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.core.time.DateFormats
import com.codingwithtashi.dailyprayer.ui.components.Avatar
import com.codingwithtashi.dailyprayer.ui.components.BellButton
import com.codingwithtashi.dailyprayer.ui.components.CheckCircle
import com.codingwithtashi.dailyprayer.ui.components.DashedBox
import com.codingwithtashi.dailyprayer.ui.components.FlagSize
import com.codingwithtashi.dailyprayer.ui.components.FlagStrip
import com.codingwithtashi.dailyprayer.ui.components.PillButton
import com.codingwithtashi.dailyprayer.ui.components.PillVariant
import com.codingwithtashi.dailyprayer.ui.components.PrayerTile
import com.codingwithtashi.dailyprayer.ui.components.SectionLabel
import com.codingwithtashi.dailyprayer.ui.components.contentWidth
import com.codingwithtashi.dailyprayer.ui.components.rememberIsCompactWidth
import com.codingwithtashi.dailyprayer.ui.theme.CategoryKey
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay

@Composable
fun TodayTab(
    listState: LazyListState,
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenRoutine: (Long) -> Unit,
    onNewRoutine: () -> Unit,
    onOpenReader: (prayerId: Int, routineId: Long?) -> Unit,
    onSeeAllSaved: () -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    // Midnight rollover while the app stays in the foreground.
    LaunchedEffect(state.date) {
        val zone = ZoneId.systemDefault()
        val now = java.time.ZonedDateTime.now(zone)
        val untilMidnight = java.time.Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(zone)).toMillis()
        delay(untilMidnight + 500)
        viewModel.refresh()
    }
    TodayContent(
        state = state,
        listState = listState,
        modifier = modifier,
        onEvent = { e ->
            when (e) {
                TodayEvent.OpenSettings -> onOpenSettings()
                TodayEvent.OpenNotifications -> onOpenNotifications()
                is TodayEvent.OpenRoutine -> onOpenRoutine(e.id)
                TodayEvent.NewRoutine -> onNewRoutine()
                is TodayEvent.StartRoutine -> onOpenReader(e.nextPrayerId, e.routineId)
                is TodayEvent.OpenReader -> onOpenReader(e.prayerId, null)
                TodayEvent.SeeAllSaved -> onSeeAllSaved()
                TodayEvent.ToggleLanguage -> viewModel.onEvent(e)
            }
        },
    )
}

@Composable
internal fun TodayContent(
    state: TodayUiState,
    listState: LazyListState,
    onEvent: (TodayEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = PrayerTheme.colors
    Box(modifier.background(c.background), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            state = listState,
            modifier = Modifier.contentWidth().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "header") { Header(state, onEvent) }
            item(key = "week") { WeekRow(state) }
            item(key = "practice") { PracticeCard(state, onEvent) }
            state.continueReading?.let { cr ->
                item(key = "continue") { ContinueCard(cr) { onEvent(TodayEvent.OpenReader(cr.prayer.id)) } }
            }
            item(key = "saved") { SavedSection(state, onEvent) }
        }
    }
}

@Composable
private fun Header(state: TodayUiState, onEvent: (TodayEvent) -> Unit) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    val compactHeight = !currentWindowAdaptiveInfo().windowSizeClass
        .isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
    val dateText = DateFormats.dateLabel(
        state.date,
        state.language,
        stringArrayResource(R.array.weekday_names).toList(),
        stringArrayResource(R.array.month_names).toList(),
    )
    val greetingText = state.greetingName?.let { stringResource(R.string.greet_name, it) } ?: stringResource(R.string.greet)
    val greetingStyle = if (compactHeight) t.greeting.copy(fontSize = 24.sp, lineHeight = 28.sp) else t.greeting
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f).padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FlagStrip(FlagSize.Small)
            Text(greetingText, style = greetingStyle, color = c.accent)
            Text(
                text = if (state.language == AppLanguage.EN) dateText.uppercase() else dateText,
                style = t.caption12Medium.copy(letterSpacing = if (state.language == AppLanguage.EN) 0.06.em else t.caption12Medium.letterSpacing),
                color = c.textMuted,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            BellButton(unread = state.unreadNotifications > 0, onClick = { onEvent(TodayEvent.OpenNotifications) })
            LangPill(state.language) { onEvent(TodayEvent.ToggleLanguage) }
            val settingsCd = stringResource(R.string.cd_settings)
            Box(
                Modifier
                    .minimumInteractiveComponentSize()
                    .semantics { contentDescription = settingsCd }
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { onEvent(TodayEvent.OpenSettings) },
                contentAlignment = Alignment.Center,
            ) { Avatar(40.dp, state.initial) }
        }
    }
}

/** Shows the language you would switch TO: "EN" while in Tibetan, "བོད" while in English. */
@Composable
private fun LangPill(language: AppLanguage, onClick: () -> Unit) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    val cd = stringResource(R.string.cd_switch_language)
    val shape = RoundedCornerShape(17.dp)
    Box(
        Modifier
            .minimumInteractiveComponentSize()
            .semantics { contentDescription = cd },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .height(34.dp)
                .clip(shape)
                .border(1.dp, c.border, shape)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (language == AppLanguage.BO) {
                Text("EN", style = t.body13Semi, color = c.ink, maxLines = 1)
            } else {
                Text("བོད", style = t.tibUiPill, color = c.ink, maxLines = 1)
            }
        }
    }
}

@Composable
private fun WeekRow(state: TodayUiState) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    val letters = stringArrayResource(R.array.week_days)
    val names = stringArrayResource(R.array.weekday_names)
    val done = stringResource(R.string.state_done)
    val notDone = stringResource(R.string.state_not_done)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            state.week.forEach { day ->
                val i = day.dayOfWeek.value - 1
                Column(
                    Modifier
                        .widthIn(min = 26.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription = names[i]
                            stateDescription = if (day.filled) done else notDone
                        },
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val ring = when {
                        day.filled -> Modifier.background(c.accent, CircleShape)
                        day.isToday -> Modifier.border(2.dp, c.accent, CircleShape)
                        else -> Modifier.border(1.dp, c.borderMuted, CircleShape)
                    }
                    Box(Modifier.size(26.dp).then(ring), contentAlignment = Alignment.Center) {
                        if (day.filled) {
                            Icon(painterResource(R.drawable.ic_check), null, Modifier.size(11.dp), tint = c.onAccent)
                        }
                    }
                    Text(
                        letters[i],
                        style = t.weekDay(state.language),
                        color = if (day.isToday) c.accent else c.textMuted,
                        maxLines = 1,
                    )
                }
            }
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(state.streak, style = t.statNumber, color = c.ink, maxLines = 1)
            Text(stringResource(R.string.day_streak), style = t.caption11, color = c.textMuted, maxLines = 1)
        }
    }
}

@Composable
private fun PracticeCard(state: TodayUiState, onEvent: (TodayEvent) -> Unit) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.surface, shape)
            .border(1.dp, c.border, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(stringResource(R.string.todays_practice), style = t.label11)
            state.practiceCount?.let { Text(it, style = t.label11, color = c.accent, maxLines = 1) }
        }
        if (state.routines.isEmpty()) {
            Text(
                stringResource(R.string.no_routines),
                style = t.body14,
                color = c.textMuted,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
        } else {
            state.routines.forEach { r -> TodayRoutineRow(r) { onEvent(TodayEvent.OpenRoutine(r.id)) } }
        }
        when (val cta = state.cta) {
            is TodayCta.Start -> PillButton(
                stringResource(R.string.start_routine, cta.name),
                { onEvent(TodayEvent.StartRoutine(cta.routineId, cta.nextPrayerId)) },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                height = 48.dp,
            )
            TodayCta.AllDone -> PillButton(
                stringResource(R.string.all_done),
                onClick = {},
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                variant = PillVariant.Tonal,
                height = 48.dp,
                interactive = false,
            )
            TodayCta.AddRoutine -> PillButton(
                stringResource(R.string.add_routine),
                { onEvent(TodayEvent.NewRoutine) },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                height = 48.dp,
            )
        }
        if (state.cta != TodayCta.AddRoutine) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PillButton(
                    stringResource(R.string.add_routine),
                    onClick = { onEvent(TodayEvent.NewRoutine) },
                    variant = PillVariant.Text,
                    height = 40.dp,
                    leadingIcon = R.drawable.ic_plus,
                    leadingIconSize = 14.dp,
                    textStyle = t.body13Semi,
                )
            }
        }
    }
}

@Composable
private fun TodayRoutineRow(routine: TodayRoutine, onClick: () -> Unit) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    val state = stringResource(if (routine.done) R.string.state_done else R.string.state_not_done)
    val subtitle = when (val s = routine.subtitle) {
        RoutineSubtitle.Complete -> stringResource(R.string.complete)
        is RoutineSubtitle.Names -> s.text
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { stateDescription = state },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CheckCircle(24.dp, routine.done)
        Column(Modifier.weight(1f)) {
            Text(routine.name, style = t.body15Semi, color = if (routine.done) c.textMuted else c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = t.caption12, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(routine.timeLabel, style = t.body13Medium, color = c.textMuted, maxLines = 1)
        Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(16.dp), tint = c.textSubtle)
    }
}

@Composable
private fun ContinueCard(cr: ContinueReading, onClick: () -> Unit) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    val shape = RoundedCornerShape(18.dp)
    val p = cr.prayer
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surfaceWarm)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PrayerTile(p.syllable, CategoryKey.valueOf(p.category.name), 44.dp, 12.dp, t.tibetanTile14)
        Column(Modifier.weight(1f)) {
            SectionLabel(stringResource(R.string.continue_reading), style = t.label11Narrow, color = c.accent)
            Text(p.titleEn, style = t.body15Semi, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(cr.pct, style = t.body13Semi, color = c.accent, maxLines = 1)
    }
}

@Composable
private fun SavedSection(state: TodayUiState, onEvent: (TodayEvent) -> Unit) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    val width = currentWindowAdaptiveInfo().windowSizeClass
    val columns = when {
        rememberIsCompactWidth() -> 4
        width.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> 8
        else -> 6
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.tab_saved), style = t.sectionTitle, color = c.ink)
            Box(
                Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clickable(role = Role.Button) { onEvent(TodayEvent.SeeAllSaved) },
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(stringResource(R.string.see_all), style = t.body13Semi, color = c.accent, maxLines = 1)
            }
        }
        if (state.savedTiles.isEmpty()) {
            DashedBox(Modifier.fillMaxWidth(), radius = 16.dp, contentPadding = PaddingValues(16.dp)) {
                Text(stringResource(R.string.saved_empty), style = t.body14, color = c.textMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                state.savedTiles.forEach { tile ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable(role = Role.Button) { onEvent(TodayEvent.OpenReader(tile.prayer.id)) },
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        PrayerTile(
                            tile.prayer.syllable,
                            CategoryKey.valueOf(tile.prayer.category.name),
                            size = null,
                            radius = 16.dp,
                            style = t.tibetanTile19,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        )
                        Text(tile.label, style = t.caption12Medium, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                repeat((columns - state.savedTiles.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
