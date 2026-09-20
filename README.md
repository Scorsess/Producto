# Producto (Timer)

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A professional-grade, distraction-free Android productivity timer built with **Kotlin** and **Jetpack Compose**. Designed for the Pomodoro technique and deep work, Producto combines an extreme minimalist AMOLED pitch-black aesthetic with powerful profile management and **direct Nextcloud Tasks integration**.

## Productive by design

Producto is a focused Pomodoro workspace rather than a generic countdown. The refreshed experience uses a calm dark-green visual system, clearer session hierarchy, and glanceable progress so the next action is always obvious.

### New in this branch

- **Daily focus goal:** Set a 1–12 session target in App Settings. Your completed-versus-target count remains visible in the main header and below the timer.
- **Auto-start next session:** Enable hands-free transitions from a completed focus session into its next break (and onward), with a short 1.2-second handoff. Disable it any time when you want a deliberate pause.
- **Refined interaction design:** The main screen now has stronger hierarchy, branded context, more readable status copy, and a stable Producto palette that does not change unexpectedly with device wallpaper colors.

---

## Key Features

### 1. Immersive Productivity & Battery Conservation
* **Pure Black AMOLED Aesthetic**: High-contrast, pitch-black (`#000000`) background with customizable accent colors to minimize power draw and eliminate visual clutter.
* **Auto-Dimming Immersive Mode**: System navigation and status bars automatically hide during active countdowns, dimming the screen to 5% brightness to keep you in flow.
* **Extra Dim Mode**: One-tap toggle (Sun icon `☀`) drops screen brightness to 0.5% for night sessions and maximum battery longevity.

### 2. Nextcloud Tasks Integration (Direct CalDAV)
* **Direct Standalone Sync**: Connect directly to your Nextcloud server without third-party intermediary apps or cloud relays.
* **CalDAV / VTODO Compliant**: Discovers task calendar collections and retrieves all pending/open to-do items.
* **Active Focus Target**: Select any task to attach it directly to your timer session (`🎯 Task Name`).
* **Instant Task Completion**: Check off tasks directly from the app, or with one tap immediately after completing a focus session.
* *See the [Nextcloud Integration Guide](docs/NEXTCLOUD_INTEGRATION.md) for full setup instructions.*

### 3. Versatile Session Management
* **Multi-Phase Cycles**: Seamlessly switch between **Focus**, **Short Break**, and **Long Break**.
* **Visual Progress Circle**: A smooth, dynamic ring that unrolls proportionally as time elapses.
* **Infinite Vertical Swipe Navigation**: Cycle continuously through modes:
  $$\text{Clock} \longleftrightarrow \text{Focus} \longleftrightarrow \text{Long Break} \longleftrightarrow \text{Short Break}$$
* **Session Tracking**: Tracks and persists completed focus sessions across app launches.
* **Daily Focus Goal**: Set a personal target of 1–12 focus sessions and track progress at a glance.
* **Auto-start Next Session**: Optionally move into the next focus/break phase automatically after a short handoff.

### 4. Customization & Timer Profiles
* **Multi-Profile Support**: Create, switch, and delete customized profiles (e.g., *"Classic Pomodoro"*, *"Deep Work"*, *"Sprint"*).
* **Independent Durations**: Configure independent focus and break durations for every profile.
* **Unique Profile Colors**: Assign custom accent colors to each profile for immediate visual identification.

### 5. Typography & Global Aesthetics
* **10 Display Fonts**: Choose from 10 distinct clock typography styles (Digital Mono, Cyber Black, Futuristic, etc.).
* **Responsive Layouts**: Dynamic layout and font scaling optimized for both Portrait and Landscape orientations.
* **Global Color Schemes**: Set a global accent palette across all UI components.

---

## Controls & Gestures

| Action | Result |
| :--- | :--- |
| **Tap Timer / Ring** | Toggle Start/Pause or advance to next session if countdown is finished. |
| **Swipe Down** | Cycle forward: `Clock → Focus → Long Break → Short Break → Clock`. |
| **Swipe Up** | Cycle backward: `Clock ← Focus ← Long Break ← Short Break ← Clock`. |
| **Tasks Button (`☑`)** | Open Nextcloud Tasks dialog (connect account, view tasks, select active goal). |
| **Gear Icon (`⚙`)** | Open **App Settings** (font styles, color picker, daily goal, and auto-start). |
| **Tools Icon (`🛠`)** | Open **Profile Settings** (Durations and profile colors). |
| **Profiles Label** | Open **Profile Manager** (Create, select, or delete profiles). |
| **Sun Icon (`☀`)** | Toggle **Extra Dim** mode (0.5% screen brightness). |

---

## Technical Architecture

Producto follows modern Android development standards:
* **Language**: Kotlin 2.2.10
* **UI Toolkit**: Jetpack Compose with Material 3
* **Architecture**: MVVM with `StateFlow` and unidirectional data flow
* **Networking**: OkHttp 4.12.0 for direct CalDAV communication over HTTPS
* **Parsing**: Robust XML DOM and RFC 5545 iCalendar `VTODO` parser with line-unfolding
* **Persistence**: SharedPreferences with support for multiple timer profiles and Nextcloud credentials
* **Lifecycle Awareness**: Timer ticking and window management are lifecycle-safe and survive screen rotations and backgrounding.

*Read the [Architecture Documentation](docs/ARCHITECTURE.md) for deeper design details.*

---

## Project Structure

```text
app/src/main/java/com/producto/timer/
├── MainActivity.kt            # Compose UI orchestration, gestures & immersive mode
├── TimerViewModel.kt          # State machine, coroutine ticker, profile & task coordination
├── Timer.kt                   # Core data models (TimerProfile, SessionType)
├── TimerPreferences.kt        # Multi-profile & app settings persistence
├── SessionNotifier.kt         # Audio/Haptic vibration engine
├── nextcloud/                 # Nextcloud CalDAV integration subsystem
│   ├── NextcloudTask.kt       # VTODO task entity
│   ├── NextcloudConfig.kt     # Server URL normalizer & Basic Auth generator
│   ├── CalDavParser.kt        # XML Multistatus & RFC 5545 iCalendar parser
│   ├── NextcloudCalDavClient.kt # Asynchronous OkHttp CalDAV API client
│   ├── NextcloudPreferences.kt # Server credentials & active task storage
│   └── NextcloudTasksDialog.kt # Task selection, completion & connection UI
└── ui/theme/                  # Typography (Type.kt), Colors, and Compose Theme
```

---

## Requirements

* Android SDK 34 (Compile & Target SDK)
* Min SDK: Android 24 (Android 7.0 Nougat)
* JDK 17+ / OpenJDK 21
* Android Studio Koala (2024.1.1) or newer

---

## Build & Install

### Command Line (Gradle)

```zsh
# 1. Build Debug APK
./gradlew assembleDebug

# 2. Install to connected device via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 3. Launch the application
adb shell am start -n com.producto.timer/.MainActivity
```

### Running Unit Tests

```zsh
./gradlew testDebugUnitTest
```

---

## Documentation

* [Nextcloud Tasks Integration Guide](docs/NEXTCLOUD_INTEGRATION.md)
* [Architecture & Technical Design](docs/ARCHITECTURE.md)

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
