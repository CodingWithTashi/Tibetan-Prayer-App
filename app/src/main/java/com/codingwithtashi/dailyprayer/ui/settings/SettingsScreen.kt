package com.codingwithtashi.dailyprayer.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codingwithtashi.dailyprayer.R
import com.codingwithtashi.dailyprayer.core.Links
import com.codingwithtashi.dailyprayer.core.locale.AppLanguage
import com.codingwithtashi.dailyprayer.core.text.TibetanNumerals
import com.codingwithtashi.dailyprayer.data.model.AppTextSize
import com.codingwithtashi.dailyprayer.data.model.Appearance
import com.codingwithtashi.dailyprayer.reminders.ExactAlarmPermission
import com.codingwithtashi.dailyprayer.reminders.NotificationAccess
import com.codingwithtashi.dailyprayer.ui.components.AppCard
import com.codingwithtashi.dailyprayer.ui.components.AppText
import com.codingwithtashi.dailyprayer.ui.components.Avatar
import com.codingwithtashi.dailyprayer.ui.components.FlagSize
import com.codingwithtashi.dailyprayer.ui.components.FlagStrip
import com.codingwithtashi.dailyprayer.ui.components.LocalToaster
import com.codingwithtashi.dailyprayer.ui.components.PrayerInput
import com.codingwithtashi.dailyprayer.ui.components.SectionLabel
import com.codingwithtashi.dailyprayer.ui.components.SegmentedControl
import com.codingwithtashi.dailyprayer.ui.components.SettingsRow
import com.codingwithtashi.dailyprayer.ui.components.SettingsTrailing
import com.codingwithtashi.dailyprayer.ui.components.rememberSafeLauncher
import com.codingwithtashi.dailyprayer.ui.components.TopBar
import com.codingwithtashi.dailyprayer.ui.components.contentWidth
import com.codingwithtashi.dailyprayer.ui.theme.PrayerDimens
import com.codingwithtashi.dailyprayer.ui.theme.PrayerTheme
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenNotifications: () -> Unit,
    onExit: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.flushName() }
    val context = LocalContext.current
    val resources = LocalResources.current
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onEvent(SettingsEvent.NotificationPermissionResult(it))
    }
    val emailSubject = stringResource(R.string.email_subject_prayer_request)
    val shareTemplate = stringResource(R.string.share_text, "{url}")

    val launch = rememberSafeLauncher()

    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.RequestNotificationPermission ->
                    if (!NotificationAccess.canPost(context)) {
                        viewModel.onEvent(SettingsEvent.NotificationPermissionMissing(NotificationAccess.shouldShowRationale(context)))
                    }
                SettingsEffect.LaunchNotificationPermission ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                is SettingsEffect.NotificationsBlocked -> {
                    scope.launch { toaster.show(resources.getString(effect.res)) }
                    launch(NotificationAccess.settingsIntent(context))
                }
                is SettingsEffect.ShareLink -> launch(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareTemplate.replace("{url}", effect.url)),
                        null,
                    ),
                )
                is SettingsEffect.OpenUrl -> launch(Intent(Intent.ACTION_VIEW, effect.url.toUri()))
                is SettingsEffect.SendEmail -> launch(
                    Intent(Intent.ACTION_SENDTO, "mailto:${effect.to}".toUri()).putExtra(Intent.EXTRA_SUBJECT, emailSubject),
                )
                SettingsEffect.OpenNotificationSettings -> launch(NotificationAccess.settingsIntent(context))
                SettingsEffect.OpenExactAlarmSettings -> ExactAlarmPermission.requestIntent(context)?.let(launch)
                SettingsEffect.OpenDownloads -> onOpenDownloads()
                SettingsEffect.OpenNotifications -> onOpenNotifications()
                SettingsEffect.Exit -> onExit()
            }
        }
    }

    SettingsContent(state, onBack, viewModel::onEvent)

    if (state.showReadingSheet) {
        ReadingSettingsSheet(
            state = ReadingSettingsState(state.textSizeSp, state.appearance, state.goal),
            onSizeDown = { viewModel.onEvent(SettingsEvent.SizeDown) },
            onSizeUp = { viewModel.onEvent(SettingsEvent.SizeUp) },
            onAppearance = { viewModel.onEvent(SettingsEvent.SetAppearance(it)) },
            onGoal = { viewModel.onEvent(SettingsEvent.SetGoal(it)) },
            onDismiss = { viewModel.onEvent(SettingsEvent.CloseReading) },
        )
    }
}

@Composable
fun SettingsContent(state: SettingsUiState, onBack: () -> Unit, onEvent: (SettingsEvent) -> Unit) {
    val c = PrayerTheme.colors
    val t = PrayerTheme.type
    val cardShape = RoundedCornerShape(18.dp)
    val cardMod = Modifier.fillMaxWidth().background(c.surface, cardShape).border(1.dp, c.border, cardShape).padding(horizontal = 16.dp)
    Column(Modifier.fillMaxSize().background(c.background)) {
        TopBar(onBack = onBack, center = {
            AppText(stringResource(R.string.settings), style = t.bodyLargeStrong, color = c.ink, modifier = Modifier.padding(end = 48.dp))
        })
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            LazyColumn(
                Modifier.contentWidth().imePadding(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Avatar(56.dp, state.initial)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SectionLabel(stringResource(R.string.your_name))
                            PrayerInput(
                                value = state.name,
                                onValueChange = { onEvent(SettingsEvent.SetName(it)) },
                                height = PrayerDimens.inputDefault,
                                radius = 12.dp,
                                textStyle = t.input,
                                placeholder = stringResource(R.string.name_ph),
                                maxLength = SettingsViewModel.MAX_NAME,
                            )
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(stringResource(R.string.language))
                        SegmentedControl(
                            options = listOf("English", "བོད་ཡིག"),
                            selectedIndex = if (state.language == AppLanguage.BO) 1 else 0,
                            onSelect = { onEvent(SettingsEvent.SetLanguage(if (it == 1) AppLanguage.BO else AppLanguage.EN)) },
                            textStyleFor = { t.leadStrong },
                        )
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(stringResource(R.string.appearance))
                        SegmentedControl(
                            options = listOf(stringResource(R.string.system_default), stringResource(R.string.light), stringResource(R.string.dark)),
                            selectedIndex = Appearance.entries.indexOf(state.appearance),
                            onSelect = { onEvent(SettingsEvent.SetAppearance(Appearance.entries[it])) },
                            textStyleFor = { t.bodyStrong },
                        )
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(stringResource(R.string.text_size))
                        val sizeNames = AppTextSize.entries.map { option ->
                            stringResource(
                                when (option) {
                                    AppTextSize.SMALL -> R.string.text_size_small
                                    AppTextSize.DEFAULT -> R.string.text_size_default
                                    AppTextSize.LARGE -> R.string.text_size_large
                                    AppTextSize.LARGEST -> R.string.text_size_largest
                                },
                            )
                        }
                        val glyph = stringResource(if (state.language == AppLanguage.BO) R.string.text_size_glyph_tibetan else R.string.text_size_glyph_latin)
                        SegmentedControl(
                            options = AppTextSize.entries.map { glyph },
                            selectedIndex = AppTextSize.entries.indexOf(state.appTextSize),
                            onSelect = { onEvent(SettingsEvent.SetAppTextSize(AppTextSize.entries[it])) },
                            textStyleFor = { t.textSizeSample(AppTextSize.entries[it]) },
                            contentDescriptionFor = { sizeNames[it] },
                        )
                        AppText(sizeNames[AppTextSize.entries.indexOf(state.appTextSize)], style = t.caption, color = c.textMuted)
                    }
                }
                item {
                    val size = TibetanNumerals.formatCount(state.textSizeSp, state.language)
                    val mode = when (state.appearance) {
                        Appearance.SYSTEM -> ""
                        Appearance.LIGHT -> " · " + stringResource(R.string.light)
                        Appearance.DARK -> " · " + stringResource(R.string.dark)
                    }
                    Column(cardMod) {
                        SettingsRow(R.drawable.ic_text_size, stringResource(R.string.reading), sub = stringResource(R.string.text_size_n, size) + mode,
                            trailing = SettingsTrailing.Chevron, onClick = { onEvent(SettingsEvent.OpenReading) })
                        SettingsRow(R.drawable.ic_bell, stringResource(R.string.reminders), sub = stringResource(R.string.reminders_sub),
                            trailing = SettingsTrailing.Switch(state.remindersEnabled) { onEvent(SettingsEvent.ToggleReminders) })
                        val actionable = state.reminderStatus == ReminderStatus.NOTIFICATIONS_OFF || state.reminderStatus == ReminderStatus.EXACT_ALARMS_OFF
                        SettingsRow(R.drawable.ic_info, stringResource(R.string.reminder_status), sub = reminderStatusText(state.reminderStatus),
                            trailing = if (actionable) SettingsTrailing.Chevron else SettingsTrailing.None,
                            onClick = if (actionable) ({ onEvent(SettingsEvent.FixReminderStatus) }) else null)
                        SettingsRow(R.drawable.ic_screen, stringResource(R.string.keep_screen_on),
                            trailing = SettingsTrailing.Switch(state.keepScreenOn) { onEvent(SettingsEvent.ToggleKeepScreenOn) })
                        SettingsRow(R.drawable.ic_download, stringResource(R.string.downloaded),
                            sub = stringResource(R.string.downloaded_sub, TibetanNumerals.formatCount(state.downloadedCount, state.language), state.storageLabel ?: "0 MB"),
                            trailing = SettingsTrailing.Chevron, onClick = { onEvent(SettingsEvent.OpenDownloads) }, showDivider = false)
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(stringResource(R.string.our_apps))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AppCard("ཚིག", stringResource(R.string.app_dictionary), stringResource(R.string.app_dictionary_sub),
                                c.appDictBg, c.appDictTile, c.appDictSub, { onEvent(SettingsEvent.OpenDictionary) }, Modifier.weight(1f))
                            AppCard("ལོ", stringResource(R.string.app_calendar), stringResource(R.string.app_calendar_sub),
                                c.appCalBg, c.appCalTile, c.appCalSub, { onEvent(SettingsEvent.OpenCalendar) }, Modifier.weight(1f))
                        }
                    }
                }
                item {
                    Column(cardMod) {
                        SettingsRow(R.drawable.ic_share, stringResource(R.string.share_app), trailing = SettingsTrailing.Chevron, onClick = { onEvent(SettingsEvent.Share) })
                        SettingsRow(R.drawable.ic_star, stringResource(R.string.rate_app), trailing = SettingsTrailing.Chevron, onClick = { onEvent(SettingsEvent.Rate) })
                        SettingsRow(R.drawable.ic_info, stringResource(R.string.about), trailing = SettingsTrailing.Chevron,
                            onClick = { onEvent(SettingsEvent.ToggleAbout) }, showDivider = !state.aboutOpen)
                        if (state.aboutOpen) {
                            Column(Modifier.padding(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppText(stringResource(R.string.about_text), style = t.bodySmall, color = c.textMuted)
                                AppText(stringResource(R.string.fonts_credit), style = t.bodySmall, color = c.textSubtle)
                            }
                        }
                        SettingsRow(R.drawable.ic_mail, stringResource(R.string.contact_email), sub = Links.DEV_EMAIL,
                            trailing = SettingsTrailing.Chevron, onClick = { onEvent(SettingsEvent.EmailPrayerRequest) })
                        SettingsRow(R.drawable.ic_link, stringResource(R.string.contact_github), trailing = SettingsTrailing.Chevron, onClick = { onEvent(SettingsEvent.OpenGithub) })
                        SettingsRow(R.drawable.ic_link, stringResource(R.string.contact_instagram), trailing = SettingsTrailing.Chevron, onClick = { onEvent(SettingsEvent.OpenInstagram) })
                        SettingsRow(R.drawable.ic_link, stringResource(R.string.contact_facebook), trailing = SettingsTrailing.Chevron, onClick = { onEvent(SettingsEvent.OpenFacebook) })
                        SettingsRow(R.drawable.ic_exit, stringResource(R.string.exit), tint = c.danger, labelColor = c.danger,
                            onClick = { onEvent(SettingsEvent.Exit) }, showDivider = false)
                    }
                }
                item {
                    Column(Modifier.fillMaxWidth().navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlagStrip(FlagSize.Tiny)
                        AppText(stringResource(R.string.version_line, state.versionName), style = t.bodySmallMedium, color = c.textSubtle)
                    }
                }
            }
        }
    }
}


@Composable
private fun reminderStatusText(status: ReminderStatus) = stringResource(
    when (status) {
        ReminderStatus.OFF -> R.string.reminder_status_off
        ReminderStatus.WORKING -> R.string.reminder_status_working
        ReminderStatus.NOTIFICATIONS_OFF -> R.string.reminder_status_notifications_off
        ReminderStatus.EXACT_ALARMS_OFF -> R.string.reminder_status_exact_off
    },
)
