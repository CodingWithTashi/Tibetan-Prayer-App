# Testing

Four layers: JVM unit tests, Maestro UI flows, instrumented (on-device) tests, and a manual release checklist.
All commands run from the repository root with JDK 17:

```
export JAVA_HOME=/Users/kunchoktashi/Library/Java/JavaVirtualMachines/ms-17.0.15/Contents/Home
```

Use `sh ./gradlew ...` (the wrapper is not executable). Run Gradle tasks serially.
The stage flavor (`com.codingwithtashi.dailyprayer.stage`) is the only app tests touch; the production id `com.codingwithtashi.dailyprayer` must never be used by tests.

## 1. Unit tests

291 `@Test` methods in 41 test classes under `app/src/test/java/com/codingwithtashi/dailyprayer/` (TST below).

| Package (under TST) | Tests | Files and purpose |
|---|---:|---|
| (root) | 1 | `SmokeTest` - the test setup itself works |
| `core/locale` | 2 | `LanguageSyncTest` - app language and per-app locale stay in sync |
| `core/notification` | 5 | `DeepLinksTest` - notification and FCM deep-link parsing/building |
| `core/text` | 21 | `TibetanNumeralsTest` (Tibetan digits formatting), `TibetanScriptTest` (script detection), `VerseSplitterTest` (verse splitting) |
| `core/time` | 5 | `DateFormatsTest` - 12-hour times and dates in English and Tibetan |
| `data/download` | 11 | `AudioFileStoreTest` (file store), `DownloadRepositoryImplTest` (download lifecycle) |
| `data/prefs` | 3 | `UserPreferencesRepositoryImplTest` - DataStore preferences |
| `data/repository` | 14 | `PrayerRepositoryImplTest`, `RoutineRepositoryImplTest` - Room-backed repositories |
| `data/seed` | 10 | `PrayerCatalogTest` (catalog integrity), `PrayerSeederTest` (seeding and updates) |
| `domain` | 12 | `RoutineProgressCalculatorTest`, `StreakCalculatorTest` |
| `messaging` | 4 | `FcmMessageHandlerTest` - FCM payload handling |
| `playback` | 11 | `PlaybackLogicTest` (queue/seek logic), `RepeatPolicyTest` (repeat x3/x7/infinite) |
| `reminders` | 56 | `DaysMaskTest`, `NextTriggerCalculatorTest`, `NotificationPermissionPolicyTest`, `ReminderHandlerTest` (alarm fires, re-arms, inbox), `ReminderHealthTest` (status: notifications/exact alarm), `ReminderSchedulerImplTest` (arm/cancel/Doze-safe fallback), `SystemEventsHandlerTest` (boot, time zone, package replaced), `DataStoreReminderStateStoreTest` |
| `ui` | 1 | `UiTextImportGuardTest` - fails if UI code imports `material3.Text` instead of `AppText` |
| `ui/components` | 7 | `TibetanAwareTextTest` |
| `ui/library` | 3 | `LibraryViewModelTest` |
| `ui/listen` | 7 | `ListenViewModelTest` |
| `ui/notifications` | 3 | `NotificationsViewModelTest` |
| `ui/onboarding` | 11 | `OnboardingViewModelTest` |
| `ui/player` | 13 | `NowPlayingViewModelTest` |
| `ui/reader` | 17 | `ReaderViewModelTest` |
| `ui/routine` | 34 | `RoutineDetailViewModelTest` (14), `RoutineEditorViewModelTest` (20) |
| `ui/saved` | 6 | `SavedViewModelTest` |
| `ui/settings` | 18 | `SettingsViewModelTest` |
| `ui/theme` | 3 | `PrayerTypographyTest` |
| `ui/today` | 13 | `TodayViewModelTest` |
| **Total** | **291** | |

Non-test helpers: `ui/TestFakes.kt`, `ui/AudioFakes.kt` (shared fakes), `data/TestDb.kt` (in-memory Room helpers). Re-count with
`grep -rc '@Test' app/src/test | awk -F: '{s+=$2} END {print s}'`.

### Run

```
sh ./gradlew :app:testStageDebugUnitTest                          # all
sh ./gradlew :app:testStageDebugUnitTest --tests '*.ReminderHandlerTest'      # one class
sh ./gradlew :app:testStageDebugUnitTest --tests '*.ReminderHandlerTest.someTestName'   # one method
```

Reports: `app/build/reports/tests/testStageDebugUnitTest/index.html`.

### Conventions

- Shared fakes live in `ui/TestFakes.kt` and `ui/AudioFakes.kt`; reuse them instead of building new mocks per test. Database tests use `data/TestDb.kt`.
- Robolectric runs at `sdk=35` (`app/src/test/resources/robolectric.properties`). Use `@RunWith(RobolectricTestRunner::class)` only when Android classes are needed.
- MockK needs `-Djdk.attach.allowAttachSelf=true`; it is already set in `testOptions` in `app/build.gradle.kts`.
- Flows are asserted with Turbine (`flow.test { ... }`).
- Coroutine tests use `runTest` plus `advanceUntilIdle()`, with the standard test dispatcher set as Main.
- ViewModels follow one pattern: `state: StateFlow`, `onEvent(...)`, `effects: Flow`. Test through those three.
- `UiTextImportGuardTest` enforces that UI code uses `AppText`, never `androidx.compose.material3.Text`.
- Say "Tibetan digits" / "English digits" in names and comments.

### Add a test

1. Create `XTest.kt` in the matching package under TST.
2. Reuse `TestFakes`/`AudioFakes`; for Room use `TestDb`.
3. Name tests as behavior (`` `fires and re-arms the next day` ``).
4. Run it alone with `--tests '*.XTest'`, then the full suite, then update the table above.

## 2. Maestro UI flows

Flows live in `.maestro/` and target the stage app (`appId: com.codingwithtashi.dailyprayer.stage`). They select by visible text and content description; a few stable `testTag`s are exposed as ids (`testTagsAsResourceId` is enabled once on the root in `AppRoot.kt`): `counter_increment`, `reader_done`, `library_search`, `mini_player`.

| Flow | What it checks | Network |
|---|---|---|
| `onboarding_en.yaml` | clear state, English onboarding, Today visible | no |
| `onboarding_bo.yaml` | Tibetan onboarding, Tibetan tab labels | no |
| `routine_reader_done_next.yaml` | start routine, counter taps, Done / Next through to Finish, completion toast | no |
| `library_search.yaml` | English and Tibetan queries, empty state, clear | no |
| `save_unsave_undo.yaml` | save, unsave, Undo, appears in Saved | no |
| `listen_stream_download.yaml` | stream, mini player, Now Playing, download, downloaded state, remove | yes (tag `network`) |
| `now_playing_controls.yaml` | speed, repeat, play/pause, queue, close | yes (tag `network`) |
| `settings_appearance_language_textsize.yaml` | Dark, text size Largest/Default, Tibetan and back | no |
| `reminder_editor_save.yaml` | edit routine days, save, optional exact-alarm dialog | no |

Shared steps: `.maestro/common/onboard_en.yaml` (clear state, grant notifications, English onboarding with the default routine). Every flow that needs an onboarded app calls it, so each flow starts from cleared app data.

### Install

```
curl -fsSL "https://get.maestro.mobile.dev" | bash      # installs to ~/.maestro/bin
~/.maestro/bin/maestro --version
```

### Run

```
sh ./gradlew :app:installStageDebug                     # build and install the stage debug APK
maestro test .maestro/onboarding_en.yaml                # one flow
maestro test .maestro/                                  # all top-level flows
maestro test --exclude-tags=network .maestro/           # offline
~/.maestro/bin/maestro check-syntax .maestro/<flow>.yaml
```

### Notes

- Use USB adb. Wireless adb stalled and replayed taps in earlier runs.
- Flows clear app data (`clearState: true`) and pre-grant notifications with `launchApp: permissions`. They wipe routines, saved items and downloads of the stage app only.
- The exact-alarm explanation dialog appears after onboarding/save only when "Alarms & reminders" is not allowed; flows handle it with an optional tap. Allowing it up front removes the branch: `adb shell appops set com.codingwithtashi.dailyprayer.stage SCHEDULE_EXACT_ALARM allow`.
- Device language: flows assume English system UI for system dialogs; app language is set by the flows themselves.
- Listen flows need internet and stream real audio; run them on Wi-Fi.
- Tibetan text input (`library_search.yaml`) relies on Maestro unicode input; if it fails, check the Maestro version and the keyboard.
- Screenshots and logs on failure: `~/.maestro/tests/`.

## 3. Instrumented / integration tests

```
sh ./gradlew :app:connectedStageDebugAndroidTest
```

Sources: `app/src/androidTest/java/com/codingwithtashi/dailyprayer/` (Hilt runner `HiltTestRunner.kt`, helpers in `TestSupport.kt`).

- `OnboardingFlowTest` - `onboardingCreatesRoutineAndReachesToday` walks onboarding end to end with real Hilt/Room and reaches Today. It deletes all routines of the stage app in `setUp` and `tearDown`: do not run it on a device where the stage app holds routines you want to keep.
- `ReaderCounterTest` - starts onboarded (via preferences), opens the first prayer from Library, taps the counter three times, checks the stored count is 3, resets, checks it is 0.

Device prerequisites: USB adb, one device attached, screen unlocked, notifications allowed for the stage app, exact alarms allowed:

```
adb shell appops set com.codingwithtashi.dailyprayer.stage SCHEDULE_EXACT_ALARM allow
adb shell pm grant com.codingwithtashi.dailyprayer.stage android.permission.POST_NOTIFICATIONS
```

## 4. Manual release checklist

Package id for all commands: `com.codingwithtashi.dailyprayer.stage` (`$PKG`).

### R8 stage build walkthrough

```
sh ./gradlew :app:assembleStageRelease
adb install -r app/build/outputs/apk/stage/release/*.apk
```

Smoke test on the minified build (no crashes, no missing resources):

- [ ] Onboarding in English; routine created.
- [ ] Reader: counter, goal, text size sheet (Aa), save/unsave.
- [ ] Listen: stream a prayer.
- [ ] Download a prayer, then play it in airplane mode.
- [ ] Now Playing: speed, repeat, queue; lock-screen / notification controls work (play/pause, next).
- [ ] Reminder: create one 2 minutes ahead, notification fires, tap opens the routine.

### Reminder fire and tap script

Create a routine whose reminder is 2 minutes ahead, then:

1. Cold start: `adb shell cmd alarm dump | grep -A3 dailyprayer.stage` shows the alarm; `adb shell am force-stop $PKG` (or `am kill`); turn the screen off; the notification appears (`adb shell dumpsys notification | grep -A8 $PKG`); tapping it opens the routine detail.
2. Warm start: repeat with the app in the foreground, then in the background.
3. Doze: before the trigger time `adb shell dumpsys deviceidle force-idle`; the reminder still fires. Restore with `adb shell dumpsys deviceidle unforce`.
4. Exact alarms revoked: `adb shell appops set $PKG SCHEDULE_EXACT_ALARM deny`. The alarm dump shows an allow-while-idle alarm, it fires in Doze, and Settings shows "Times may drift · Allow exact time". Restore with `... allow`.
5. Notifications revoked: `adb shell pm revoke $PKG android.permission.POST_NOTIFICATIONS`. Settings and routine detail show the warnings, the inbox still records the reminder, the alarm is re-armed. Restore with `pm grant`.
6. Reboot: `adb reboot`, unlock; the alarm dump shows the reminder re-armed.
7. Global switch: turn Reminders off in Settings, confirm alarms are gone from the dump, turn on, confirm re-armed.

### FCM

Firebase console, Cloud Messaging, send a test message to the stage app token (token from the debug log) and a topic message; confirm it shows in Notifications inside the app and the tap deep link opens the right screen.

### Appearance, language, scale

- [ ] Dark pass: Settings, Appearance, Dark; walk every tab, reader, Now Playing, settings.
- [ ] Tibetan (bo) pass: Settings, Language, Tibetan; check that Tibetan stacks are not clipped and Tibetan digits show only for counts.
- [ ] System font scale: `adb shell settings put system font_scale 1.3`, then `1.5`; nothing clips. Restore: `adb shell settings put system font_scale 1.0`.
- [ ] In-app Text size Largest (Settings); nothing clips, then back to Default.

### Release prerequisites

- [ ] Crashlytics enabled in the Firebase console.
- [ ] All Tibetan strings marked `(AI) review` in `values-bo/strings.xml` reviewed by a Tibetan speaker and markers removed, plus the English titles in `PrayerCatalog.kt`.
- [ ] `versionCode` in `app/build.gradle.kts` (currently 28) is higher than the latest on Play.
- [ ] `GOOGLE_SERVICES_JSON` repository secret present for CI.
- [ ] `sh ./gradlew :app:assembleStageDebug :app:testStageDebugUnitTest :app:lintStageDebug :app:assembleStageRelease :app:assembleProductionRelease` is green.

## 5. CI

`.github/workflows/main.yml` runs on push and pull request to `master`:

```
assembleStageDebug testStageDebugUnitTest lintStageDebug
```

It needs the repository secret `GOOGLE_SERVICES_JSON` (base64 of `google-services.json`); without it the build fails at the Google services step. CI does not run Maestro or instrumented tests; those are run locally on a device before release.

## Verified runs

| Flow / suite | Device | Date | Result |
|---|---|---|---|
| | | | |
