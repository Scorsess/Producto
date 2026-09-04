# Copilot Instructions — Producto (Timer)

Single-module Android app (Kotlin + Jetpack Compose): a Pomodoro-style
Focus/Break productivity timer, similar in spirit to the Goodtime app.
Application ID / package: `com.producto.timer`. The Gradle module is `:app`
(root project name is `Producto`, directory name is `Timer`).

## Environment notes (read before assuming tools are missing)

- The Gradle wrapper (`gradlew`/`gradlew.bat`, `gradle/wrapper/`) **is checked
  in** — use it directly, don't regenerate it. It currently pins Gradle 8.13
  (`gradle/wrapper/gradle-wrapper.properties`).
- This dev machine has no standalone JDK/Gradle on `PATH` (only a legacy
  Java 8 at `C:\Program Files (x86)\Common Files\Oracle\Java\java8path`, which
  is too old for AGP). **Android Studio is installed at
  `C:\Program Files\Android\Android Studio`** and bundles both a compatible
  JDK and the Android SDK. When running Gradle from a shell (not from within
  Android Studio), set `JAVA_HOME` to Android Studio's bundled JBR first:
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
  ./gradlew.bat assembleDebug
  ```
- Node.js (LTS) is installed system-wide (via winget) so the `mobile-mcp` MCP
  server (`@mobilenext/mobile-mcp`, configured user-wide in
  `~/.copilot/mcp-config.json`) can drive a connected Android
  device/emulator via `npx`. There is no Android emulator/AVD or physical
  device configured in this environment by default — `connectedAndroidTest`
  and on-device manual testing require one to be created/attached first
  (Android Studio's Device Manager, or `mobile-mcp`).

## Build, test, lint

- Build debug APK: `./gradlew assembleDebug`
- Install on a connected device/emulator: `./gradlew installDebug`
- Run all JVM unit tests: `./gradlew test`
- Run a single unit test class: `./gradlew test --tests "com.producto.timer.TimerViewModelTest"`
- Run a single unit test method: `./gradlew test --tests "com.producto.timer.TimerFormatTest.formatMillis_zero_returnsZeroed"`
- Run instrumented tests (requires a connected device/emulator): `./gradlew connectedAndroidTest`
- Lint: `./gradlew lint`

Dependency versions are centralized in `gradle/libs.versions.toml` (Gradle
version catalog) and referenced from `app/build.gradle.kts` via `libs.*` —
add new dependencies there rather than hardcoding coordinates in module
build files.

## Architecture

Pomodoro-style Focus/Break timer (Goodtime-like): full-screen, dims the
display and hides system bars while a countdown is running, plays a
sound/haptic cue when it finishes, and waits for a tap to advance to the
next session once time is up.

Android-framework-dependent code (anything touching `Context`,
`SharedPreferences`, `AudioManager`/`Vibrator`, `Activity`/`Window`/`View`) is
kept behind small interfaces (`TimerPreferences`, `SessionNotifier`) so
`TimerViewModel` itself has no Android dependency and can be unit tested with
plain in-memory fakes — no Robolectric/instrumentation needed.

- `app/src/main/java/com/producto/timer/Timer.kt` — pure, non-Compose domain
  logic: `SessionType` enum (`FOCUS`/`BREAK` labels, `next()`) and
  `formatMillis(Long): String` (MM:SS).
- `app/src/main/java/com/producto/timer/TimerPreferences.kt` — `TimerPreferences`
  interface (configurable `focusMinutes`/`breakMinutes`, persisted
  `completedFocusSessions`) plus the `durationMillis(SessionType)` extension
  and the real `AndroidTimerPreferences` (`SharedPreferences`-backed) impl.
- `app/src/main/java/com/producto/timer/SessionNotifier.kt` — `SessionNotifier`
  interface (`notifySessionComplete()`) plus `AndroidSessionNotifier`, which
  checks `AudioManager.ringerMode` and only plays a `ToneGenerator` beep +
  vibrates on `RINGER_MODE_NORMAL`, only vibrates on `RINGER_MODE_VIBRATE`,
  and stays silent on `RINGER_MODE_SILENT` — this is how sound/haptics respect
  the system mute/vibrate/sound setting. Requires the `VIBRATE` manifest
  permission.
- `app/src/main/java/com/producto/timer/TimerViewModel.kt` — `TimerViewModel`
  takes a `TimerPreferences` and `SessionNotifier` via constructor injection,
  owns the countdown as a `StateFlow<TimerUiState>`, and drives it with a
  `viewModelScope` coroutine ticking every second (`start`/`pause`/
  `togglePause`/`startNextSession`/`updateDurations`). Survives configuration
  changes; state persists across screen rotation. `TimerViewModel.factory(context)`
  builds the production `ViewModelProvider.Factory` wiring up the
  Android-backed implementations — this is the only place `Context` enters
  the ViewModel.
- `app/src/main/java/com/producto/timer/MainActivity.kt` — single Activity,
  hosts `TimerScreen`, created via
  `viewModel(factory = TimerViewModel.factory(LocalContext.current))`. Collects
  `TimerViewModel.uiState` via `collectAsStateWithLifecycle`. A
  `DisposableEffect` keyed on `isRunning` dims the window
  (`WindowManager.LayoutParams.screenBrightness`), sets `keepScreenOn`, and
  hides/shows system bars via `WindowInsetsControllerCompat`. Tapping anywhere
  toggles pause/resume while running, or starts the next session (Focus ->
  Break -> Focus -> ...) once finished. The settings gear and the
  "Focus sessions completed" counter only render while paused/idle/finished,
  keeping the running view distraction-free; the gear opens
  `DurationSettingsDialog`, which validates minutes are within
  `TimerPreferences.MIN_MINUTES..MAX_MINUTES` before calling
  `viewModel.updateDurations(...)`.
- `app/src/main/java/com/producto/timer/ui/theme/` — Material3 theme
  (`Theme.kt`, `Color.kt`, `Type.kt`), supports Android 12+ dynamic color.
- `app/src/test/` — JVM unit tests (fast, no emulator, no Android dependency):
  `TimerFormatTest` covers `formatMillis`/`SessionType`/`durationMillis`;
  `TimerViewModelTest` drives the countdown with a `StandardTestDispatcher`
  (`kotlinx-coroutines-test`) via `Dispatchers.setMain`/`advanceTimeBy`
  instead of real delays, using `FakeTimerPreferences`/`FakeSessionNotifier`
  test doubles (also in `src/test`) instead of the Android-backed impls.
  `app/src/androidTest/` — instrumented tests requiring a device/emulator.
  When adding new Android-framework-dependent logic, prefer putting it behind
  an interface with a fake, the way `TimerPreferences`/`SessionNotifier` are,
  so it stays testable from `src/test`.

## Conventions

- Kotlin only; UI is Compose-first (no XML layouts).
- New Gradle dependencies go in `gradle/libs.versions.toml`, then referenced
  via the `libs` accessor.
- Package root for all app code is `com.producto.timer`; all `.kt` files
  live flat under `app/src/main/java/com/producto/timer/` except the theme
  package (`ui/theme/`) — there's no other sub-package structure yet.
- **Android-framework-dependent code goes behind an interface with a fake**,
  not directly into the ViewModel — see `TimerPreferences`/`SessionNotifier`
  above. This is what keeps `TimerViewModel` unit-testable from `src/test`
  without Robolectric or instrumentation. Follow this pattern for any new
  Android API surface (notifications, alarms, sensors, etc.).
- ViewModels are constructed via a `companion object factory(context)` that
  returns a `ViewModelProvider.Factory` (see `TimerViewModel.factory`), wired
  up at the call site with
  `viewModel(factory = TimerViewModel.factory(LocalContext.current))` — don't
  use `AndroidViewModel`/no-arg `viewModel()`, since that would reintroduce a
  direct Context dependency into the ViewModel and make it harder to fake in
  tests.
- Coroutine-based countdowns use `viewModelScope.launch { while (...) { delay(1000); ... } }`
  ticking a `StateFlow`, not `CountDownTimer` — this is what lets
  `TimerViewModelTest` drive time deterministically with
  `StandardTestDispatcher`/`advanceTimeBy` instead of real 1-second delays.
- Full-screen tap-target UX: the whole screen is one `pointerInput`/
  `detectTapGestures` surface (pause/resume or advance-to-next-session).
  Any control that must not trigger that background tap (e.g. the settings
  gear) must be a separate `clickable` Composable layered on top in the same
  `Box` — Compose's pointer-input consumption naturally prevents the tap
  from also reaching the background gesture detector, so no manual
  workaround is needed, but don't add new full-bleed backgrounds that would
  shadow it.
- Chrome (settings gear, "Focus sessions completed" counter) is only shown
  while **not** running (paused/idle/finished), to keep the active countdown
  distraction-free, matching Goodtime's minimal-while-running UX. Follow
  this precedent for any future controls.
- Vector launcher icon (`res/mipmap-anydpi-v26/ic_launcher(.xml|_round.xml)`,
  `res/drawable/ic_launcher_foreground.xml`, `res/values/ic_launcher_background.xml`)
  is required for `processDebugResources`/`processReleaseResources` to
  succeed — don't remove it without replacing it, or the build will fail
  with `AAPT: error: resource mipmap/ic_launcher ... not found`.

## Known gotchas

- `AndroidManifest.xml` declares `<uses-permission android:name="android.permission.VIBRATE" />`,
  required by `AndroidSessionNotifier`'s haptic feedback. Keep it if that
  class keeps using `Vibrator`/`VibrationEffect`.
- `AndroidSessionNotifier` branches on `AudioManager.ringerMode`
  (`RINGER_MODE_NORMAL` -> tone + vibrate, `RINGER_MODE_VIBRATE` -> vibrate
  only, `RINGER_MODE_SILENT` -> nothing) — this is intentional and mirrors
  system mute/vibrate/sound behavior; don't "fix" it to always play sound.
- `TimerViewModel.updateDurations` only snaps the *current* countdown to a
  new duration if it's untouched (`!isRunning && !isFinished && remainingMillis == totalMillis`);
  otherwise the new duration applies starting the *next* session. This is
  deliberate so an in-progress countdown is never abruptly shortened/extended
  out from under the user.
