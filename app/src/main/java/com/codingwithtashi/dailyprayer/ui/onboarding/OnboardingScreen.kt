package com.codingwithtashi.dailyprayer.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass
import com.codingwithtashi.dailyprayer.R
import androidx.compose.ui.platform.LocalContext
import android.content.ActivityNotFoundException
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.core.time.DateFormats
import com.codingwithtashi.dailyprayer.reminders.ExactAlarmPermission
import com.codingwithtashi.dailyprayer.ui.routine.ExactAlarmPromptDialog
import com.codingwithtashi.dailyprayer.ui.components.FlagSize
import com.codingwithtashi.dailyprayer.ui.components.FlagStrip
import com.codingwithtashi.dailyprayer.ui.components.IconActionButton
import com.codingwithtashi.dailyprayer.ui.components.LanguageOption
import com.codingwithtashi.dailyprayer.ui.components.LocalToaster
import com.codingwithtashi.dailyprayer.ui.components.PickRow
import com.codingwithtashi.dailyprayer.ui.components.PillButton
import com.codingwithtashi.dailyprayer.ui.components.PillVariant
import com.codingwithtashi.dailyprayer.ui.components.PrayerInput
import com.codingwithtashi.dailyprayer.ui.components.PrayerTimePickerDialog
import com.codingwithtashi.dailyprayer.ui.components.SectionLabel
import com.codingwithtashi.dailyprayer.ui.components.TimeChip
import com.codingwithtashi.dailyprayer.ui.components.TibetanText
import com.codingwithtashi.dailyprayer.ui.components.contentWidth
import com.codingwithtashi.dailyprayer.ui.theme.CategoryKey
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme
import com.codingwithtashi.dailyprayer.ui.theme.motionDuration
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    val finished by rememberUpdatedState(onFinished)
    val context = LocalContext.current
    var permissionPending by remember { mutableStateOf(false) }
    var showExactAlarm by remember { mutableStateOf(false) }

    // Flow: notification permission -> (optional, once) exact-alarm explanation -> Main.
    fun continueAfterPermission() {
        scope.launch {
            if (viewModel.routineCreated && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !ExactAlarmPermission.canScheduleExact(context) && viewModel.promptExactAlarmOnce()
            ) {
                showExactAlarm = true
            } else {
                finished()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onPermissionAsked()
        continueAfterPermission()
    }

    val pickMessage = stringResource(R.string.pick_at_least_one)
    val welcomeBare = stringResource(R.string.welcome)
    val morningPractice = stringResource(R.string.morning_practice)
    var welcomeName by remember { mutableStateOf<String?>(null) }
    val welcomeMessage = welcomeName?.let { if (it.isBlank()) welcomeBare else stringResource(R.string.welcome_name, it) }
    LaunchedEffect(welcomeMessage) {
        welcomeMessage?.let { msg ->
            welcomeName = null
            scope.launch { toaster.show(msg) }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.receiveAsFlow().collect { effect ->
            when (effect) {
                OnboardingEffect.PickAtLeastOne -> scope.launch { toaster.show(pickMessage) }
                OnboardingEffect.RequestNotificationPermission -> permissionPending = true
                is OnboardingEffect.Welcome -> welcomeName = effect.name
                OnboardingEffect.Finished ->
                    if (permissionPending && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionPending = false
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        continueAfterPermission()
                    }
            }
        }
    }

    BackHandler(enabled = state.step > 0) { viewModel.onEvent(OnboardingEvent.Back) }

    OnboardingContent(state, morningPractice, viewModel::onEvent)

    if (showExactAlarm) {
        ExactAlarmPromptDialog(
            onOpenSettings = {
                showExactAlarm = false
                ExactAlarmPermission.requestIntent(context)?.let {
                    try { context.startActivity(it) } catch (_: ActivityNotFoundException) { }
                }
                finished()
            },
            onDismiss = {
                showExactAlarm = false
                finished()
            },
        )
    }
}

@Composable
internal fun OnboardingContent(
    state: OnboardingUiState,
    routineName: String,
    onEvent: (OnboardingEvent) -> Unit,
) {
    val c = PrayerTheme.colors
    val reduceMotion = PrayerTheme.reduceMotion
    Box(Modifier.fillMaxSize().background(c.background), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .fillMaxHeight()
                .contentWidth(480.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Header(state.step, onBack = { onEvent(OnboardingEvent.Back) })
            AnimatedContent(
                targetState = state.step,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                transitionSpec = {
                    if (reduceMotion) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(tween(250)) { it / 4 * dir } + fadeIn(tween(250))) togetherWith
                            (slideOutHorizontally(tween(250)) { -it / 4 * dir } + fadeOut(tween(150)))
                    }
                },
                label = "onboardingStep",
            ) { step ->
                when (step) {
                    0 -> StepLanguage(state, onEvent)
                    1 -> StepName(state, onEvent)
                    else -> StepRoutine(state, routineName, onEvent)
                }
            }
        }
    }
    if (state.showTimePicker) {
        PrayerTimePickerDialog(
            initialMinutes = state.timeMinutes,
            onConfirm = { onEvent(OnboardingEvent.SetTime(it)) },
            onDismiss = { onEvent(OnboardingEvent.DismissTimePicker) },
        )
    }
}

@Composable
private fun Header(step: Int, onBack: () -> Unit) {
    val c = PrayerTheme.colors
    val stepDescription = stringResource(R.string.cd_step_of, step + 1, 3)
    Row(
        Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (step > 0) {
            IconActionButton(R.drawable.ic_back, stringResource(R.string.cd_back), onBack)
        } else {
            Spacer(Modifier.size(48.dp))
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.clearAndSetSemantics { contentDescription = stepDescription },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) { i ->
                val w by animateDpAsState(if (i == step) 22.dp else 6.dp, tween(motionDuration(200)), label = "dot")
                Box(
                    Modifier
                        .width(w)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (i <= step) c.accent else c.borderMuted),
                )
            }
        }
    }
}

@Composable
private fun rememberCompactHeight(): Boolean =
    !currentWindowAdaptiveInfo().windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)

@Composable
private fun titleStyle() =
    if (PrayerTheme.language == AppLanguage.BO) PrayerTheme.type.tibetanTitle30 else PrayerTheme.type.displayMedium

@Composable
private fun StepLanguage(state: OnboardingUiState, onEvent: (OnboardingEvent) -> Unit) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    val compactHeight = rememberCompactHeight()
    Column(
        Modifier
            .fillMaxSize()
            .then(if (compactHeight) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Column(
            (if (compactHeight) Modifier.padding(vertical = 16.dp) else Modifier.weight(1f)).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.Start,
        ) {
            FlagStrip(FlagSize.Large)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TibetanText("ཉིན་རེའི་ཁ་འདོན།", t.tibetanHero, color = c.accent)
                Text("Daily Prayer", style = t.displayLarge, color = c.ink)
                Text(stringResource(R.string.ob_intro), style = t.body16, color = c.textMuted, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(stringResource(R.string.app_language))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LanguageOption(
                    label = stringResource(R.string.lang_english),
                    selected = state.language == AppLanguage.EN,
                    onClick = { onEvent(OnboardingEvent.SetLanguage(AppLanguage.EN)) },
                    textStyle = t.body16Semi,
                    modifier = Modifier.weight(1f),
                )
                LanguageOption(
                    label = stringResource(R.string.lang_tibetan),
                    selected = state.language == AppLanguage.BO,
                    onClick = { onEvent(OnboardingEvent.SetLanguage(AppLanguage.BO)) },
                    textStyle = t.tibUiLangOption,
                    modifier = Modifier.weight(1f),
                )
            }
            PillButton(
                stringResource(R.string.continue_),
                { onEvent(OnboardingEvent.Next) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                height = 54.dp,
            )
        }
    }
}

@Composable
private fun StepName(state: OnboardingUiState, onEvent: (OnboardingEvent) -> Unit) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(R.string.ob_name_q), style = titleStyle(), color = c.ink)
        Text(stringResource(R.string.ob_name_sub), style = t.body15, color = c.textMuted)
        PrayerInput(
            value = state.name,
            onValueChange = { onEvent(OnboardingEvent.SetName(it)) },
            height = 56.dp,
            radius = 16.dp,
            textStyle = t.input18,
            placeholder = stringResource(R.string.name_ph),
            maxLength = 24,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onEvent(OnboardingEvent.Next) }),
            autofocus = true,
        )
        Spacer(Modifier.weight(1f))
        PillButton(stringResource(R.string.continue_), { onEvent(OnboardingEvent.Next) }, modifier = Modifier.fillMaxWidth(), height = 54.dp)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PillButton(
                stringResource(R.string.skip),
                onClick = {
                    onEvent(OnboardingEvent.SetName(""))
                    onEvent(OnboardingEvent.Next)
                },
                variant = PillVariant.Text,
                height = 40.dp,
                textStyle = t.body14Semi,
                contentColor = c.textMuted,
            )
        }
    }
}

@Composable
private fun StepRoutine(state: OnboardingUiState, routineName: String, onEvent: (OnboardingEvent) -> Unit) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.ob_routine_q), style = titleStyle(), color = c.ink)
            Text(stringResource(R.string.ob_routine_sub), style = t.body15, color = c.textMuted)
        }
        val shape = RoundedCornerShape(16.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(c.surface, shape)
                .border(1.dp, c.border, shape)
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(painterResource(R.drawable.ic_bell), null, Modifier.size(20.dp), tint = c.accent)
            Text(stringResource(R.string.remind_at), style = t.body15Semi, color = c.ink, modifier = Modifier.weight(1f))
            TimeChip(DateFormats.time12(state.timeMinutes, state.language), onClick = { onEvent(OnboardingEvent.OpenTimePicker) })
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(state.candidates, key = { it.id }) { p ->
                val slug = p.slug.orEmpty()
                PickRow(
                    syllable = p.syllable,
                    category = CategoryKey.valueOf(p.category.name),
                    tib = p.title,
                    en = p.titleEn,
                    checked = slug in state.picks,
                    onToggle = { onEvent(OnboardingEvent.TogglePick(slug)) },
                    showDivider = p != state.candidates.last(),
                )
            }
        }
        PillButton(
            stringResource(R.string.ob_start),
            { onEvent(OnboardingEvent.Finish(true, routineName)) },
            modifier = Modifier.fillMaxWidth(),
            height = 54.dp,
        )
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PillButton(
                stringResource(R.string.ob_later),
                onClick = { onEvent(OnboardingEvent.Finish(false)) },
                variant = PillVariant.Text,
                height = 36.dp,
                textStyle = t.body14Semi,
                contentColor = c.textMuted,
            )
        }
    }
}
