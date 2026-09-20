# Producto Architecture & Technical Design

Producto is engineered for high performance, zero distraction, and battery conservation. Built with Kotlin and Jetpack Compose, it adheres to modern Android MVVM best practices.

---

## 1. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                       UI Layer (Compose)                    │
│  - MainActivity.kt (Window insets, Immersive mode, Gestures)│
│  - TimerScreen.kt  (Countdown, Session Ring, Controls)      │
│  - NextcloudTasksDialog.kt (Task List & Credential Setup)   │
└──────────────────────────────┬──────────────────────────────┘
                               │ StateFlow (Unidirectional)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                    ViewModel & State Layer                  │
│  - TimerViewModel.kt (State machine, Ticker coroutine)      │
│  - TimerUiState (Immutable UI snapshot)                     │
└──────────────────────────────┬──────────────────────────────┘
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
┌──────────────────────────────┐    ┌──────────────────────────────┐
│       Persistence Layer      │    │         Network Layer        │
│  - TimerPreferences.kt       │    │  - NextcloudCalDavClient.kt  │
│    (Profiles, Colors, Times) │    │    (OkHttp, CalDAV/RFC 4791) │
│  - NextcloudPreferences.kt   │    │  - CalDavParser.kt           │
│    (Server URL, Token, Task) │    │    (DOM XML & VTODO parser)  │
└──────────────────────────────┘    └──────────────────────────────┘
```

---

## 2. Core Components

### A. State Management (`TimerViewModel`)
* Emits an immutable `TimerUiState` through `StateFlow`.
* Manages session ticking using Kotlin coroutines (`viewModelScope`), ensuring lifecycle-aware backgrounding and rotation survival.
* Decoupled from Android dependencies for unit testability.

### B. Gestures & Navigation
* Vertical drag gestures (`detectDragGestures`) cycle indefinitely between display modes:
  $$\text{Clock} \longleftrightarrow \text{Focus} \longleftrightarrow \text{Long Break} \longleftrightarrow \text{Short Break}$$
* Tap gestures (`detectTapGestures`) toggle Start/Pause or advance sessions when finished.

### C. Immersive AMOLED Design
* Window brightness is managed dynamically via `WindowInsetsControllerCompat`.
* During active countdowns, system bars (status bar and navigation bar) are hidden, and screen brightness is dimmed to 5% to maximize AMOLED power savings.
* Dedicated **Extra Dim** mode drops brightness to 0.5% for night productivity.

### D. Nextcloud CalDAV Subsystem
* `NextcloudConfig`: Normalizes URLs, generates basic auth headers, and resolves collection paths.
* `CalDavParser`: Parses XML Multi-Status responses and RFC 5545 `VTODO` iCalendar streams with line-unfolding, escaping handlers, and date formatters.
* `NextcloudCalDavClient`: Manages network requests via `OkHttp` with connection timeouts and `If-Match` conditional headers.

---

## 3. Key Data Models

* `SessionType`: `FOCUS`, `SHORT_BREAK`, `LONG_BREAK`.
* `TimerProfile`: User-defined presets with custom durations and accent colors.
* `NextcloudTask`: UID, summary, completion state, due date, priority, and calendar metadata.


## 5. Productivity controls

Two lightweight preferences extend the core timer without adding another service dependency. `dailyGoalSessions` stores the user’s daily target (bounded to 1–12) and is surfaced in `TimerUiState` for the header and completion summary. `autoStartNextSession` controls whether the ViewModel starts the next Pomodoro phase after a completed session, following a short 1.2-second handoff. Both values are persisted through `TimerPreferences`, so the experience survives process recreation.
