# Producto (Timer)

A professional-grade, distraction-free Android productivity timer built with Kotlin and Jetpack Compose. Designed for the Pomodoro technique and beyond, Producto combines extreme minimal aesthetics with powerful customization.

## Key Features

### 1. Immersive Productivity
- **Distraction-Free UI**: High-contrast interface (customizable colors on pitch black).
- **Immersive Mode**: Automatically dims the screen and hides system navigation/status bars during active countdowns to help you stay in the flow.
- **Extra Dim Mode**: A dedicated button (Sun icon) to drop screen brightness to 0.5% for night use or ultimate battery saving.

### 2. Versatile Session Management
- **Multi-Session Support**: Seamlessly switch between **Focus**, **Short Break**, and **Long Break**.
- **Visual Progress Circle**: A dynamic ring around the timer that empties proportionally as time passes.
- **Infinite Swipe Navigation**: Use vertical swipe gestures to cycle indefinitely through modes:
  `Clock ↔ Focus ↔ Long Break ↔ Short Break`
- **Session Tracking**: Automatically counts and persists your completed focus sessions.

### 3. Deep Customization (Timer Profiles)
- **Multiple Profiles**: Create, manage, and switch between different timer configurations (e.g., "Classic Pomodoro", "Deep Work", "Quick Sprints").
- **Independent Durations**: Each profile stores its own unique times for Focus and both Break types.
- **Profile Colors**: Assign a unique color to each profile to visually identify your current mode.

### 4. Global Typography & Aesthetics
- **Special Clock Fonts**: Choose from 10 unique display styles (Digital Mono, Cyber Black, Futuristic, etc.) for the timer and clock digits.
- **Responsive Layout**: Font sizes automatically adjust for optimal readability in both Portrait and Landscape orientations.
- **Custom Font Colors**: Set a global color scheme that applies across all screens and modes.

## Controls & Gestures

| Action | Result |
| :--- | :--- |
| **Tap Timer** | Toggle Start/Pause or Advance to next session if finished. |
| **Swipe Up** | Cycle backward through: Clock ← Focus ← Long Break ← Short Break. |
| **Swipe Down** | Cycle forward through: Clock → Focus → Long Break → Short Break. |
| **Gear Icon** | Open **App Settings** (Global Fonts and Colors). |
| **Tool Icon** | Open **Profile Settings** (Durations and Profile Colors). |
| **Sun Icon** | Toggle **Extra Dim** mode (0.5% brightness). |

## Technical Architecture

Producto follows modern Android development standards:
- **Language**: Kotlin 2.2.10
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM with `StateFlow` for unidirectional data flow.
- **Persistence**: Enhanced `SharedPreferences` implementation with robust error handling and multi-profile support.
- **Lifecycle Awareness**: Timer ticking and window management are fully lifecycle-aware, surviving rotations and backgrounding.

## Project Structure

```text
app/src/main/java/com/producto/timer/
├── ui/theme/           # Typography (Type.kt), Color, and Theme definitions.
├── MainActivity.kt     # UI orchestration, Gestures, and Immersive Mode logic.
├── TimerViewModel.kt   # State management, Timer logic, and Profile handling.
├── Timer.kt            # Data models (TimerProfile, SessionType).
├── TimerPreferences.kt # Multi-profile storage and global settings persistence.
└── SessionNotifier.kt  # Audio/Haptic feedback engine.
```

## Requirements

- Android Studio Koala (2024.1.1) or newer.
- Android SDK 34 (Compile Sdk).
- Min SDK 24 (Android 7.0 Nougat).
- Java 17.

## Build & Run

1. Clone the repository.
2. Open in Android Studio.
3. Run `./gradlew assembleDebug` to build the APK.
4. Deploy to your device using the **Run** button or `Shift + F10`.

---
*Producto - Stay focused, stay productive.*
