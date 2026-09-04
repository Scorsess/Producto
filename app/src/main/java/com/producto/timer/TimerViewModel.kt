package com.producto.timer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Snapshot of the active countdown. [isFinished] means the current session's time is up and is
 * waiting for the user to tap to advance to the next session.
 */
data class TimerUiState(
    val sessionType: SessionType = SessionType.FOCUS,
    val focusMinutes: Int = TimerPreferences.DEFAULT_FOCUS_MINUTES,
    val shortBreakMinutes: Int = TimerPreferences.DEFAULT_SHORT_BREAK_MINUTES,
    val longBreakMinutes: Int = TimerPreferences.DEFAULT_LONG_BREAK_MINUTES,
    val timerColorArgb: Long = TimerPreferences.DEFAULT_COLOR,
    val totalMillis: Long = TimerPreferences.DEFAULT_FOCUS_MINUTES * 60_000L,
    val remainingMillis: Long = totalMillis,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val completedFocusSessions: Int = 0,
    val profiles: List<TimerProfile> = emptyList(),
    val currentProfileId: String = "default",
    val globalFontFamilyIndex: Int = 0,
    val globalFontColorArgb: Long = TimerPreferences.DEFAULT_COLOR
)

private const val TICK_MILLIS = 1000L

/**
 * Drives a single Focus/Break countdown cycle.
 */
/**
 * Drives the logic for focus/break cycles and manages timer profiles.
 *
 * Responsibilities:
 * - Maintains the countdown state (running, paused, finished).
 * - Handles transitions between [SessionType] (Focus, Short Break, Long Break).
 * - Manages multiple [TimerProfile] configurations.
 * - Provides global app settings (fonts and colors) via [TimerPreferences].
 */
class TimerViewModel(
    private val preferences: TimerPreferences,
    private val notifier: SessionNotifier,
    private val now: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(buildFreshState(SessionType.FOCUS))
    val uiState: StateFlow<TimerUiState> = _uiState

    private var tickerJob: Job? = null

    init {
        android.util.Log.d("TimerApp", "TimerViewModel initialized")
    }

    private fun buildFreshState(session: SessionType): TimerUiState {
        android.util.Log.d("TimerApp", "buildFreshState for $session")
        val duration = preferences.durationMillis(session)
        val profiles = preferences.getProfiles()
        return TimerUiState(
            sessionType = session,
            focusMinutes = preferences.focusMinutes,
            shortBreakMinutes = preferences.shortBreakMinutes,
            longBreakMinutes = preferences.longBreakMinutes,
            timerColorArgb = preferences.timerColorArgb,
            totalMillis = duration,
            remainingMillis = duration,
            completedFocusSessions = preferences.completedFocusSessions,
            profiles = profiles,
            currentProfileId = preferences.currentProfileId,
            globalFontFamilyIndex = preferences.globalFontFamilyIndex,
            globalFontColorArgb = preferences.globalFontColorArgb
        )
    }

    /** Starts or resumes the countdown for the current session. No-op if already running. */
    fun start() {
        if (_uiState.value.isRunning || _uiState.value.isFinished) return
        _uiState.update { it.copy(isRunning = true) }
        tickerJob = viewModelScope.launch {
            val startMillis = now()
            val initialRemaining = _uiState.value.remainingMillis
            
            while (_uiState.value.remainingMillis > 0) {
                delay(TICK_MILLIS)
                val elapsed = now() - startMillis
                _uiState.update { 
                    it.copy(remainingMillis = (initialRemaining - elapsed).coerceAtLeast(0)) 
                }
            }
            _uiState.update { it.copy(isRunning = false, isFinished = true) }
            notifier.notifySessionComplete()
        }
    }

    /** Pauses the countdown, keeping the remaining time. */
    fun pause() {
        tickerJob?.cancel()
        _uiState.update { it.copy(isRunning = false) }
    }

    /** Toggles between running and paused; ignored once the session has finished. */
    fun togglePause() {
        if (_uiState.value.isFinished) return
        if (_uiState.value.isRunning) pause() else start()
    }

    /** Advances to the next session and immediately starts its countdown. */
    fun startNextSession() {
        tickerJob?.cancel()
        val finishedSession = _uiState.value.sessionType
        if (finishedSession == SessionType.FOCUS) {
            preferences.completedFocusSessions += 1
        }
        _uiState.update { buildFreshState(finishedSession.next()) }
        start()
    }

    /** Manually switches to a specific session type. */
    fun setSessionType(sessionType: SessionType) {
        tickerJob?.cancel()
        _uiState.update { buildFreshState(sessionType) }
    }

    /** Updates the current profile's settings. */
    fun updateProfile(profile: TimerProfile) {
        preferences.saveProfile(profile)
        if (preferences.currentProfileId == profile.id) {
            preferences.focusMinutes = profile.focusMinutes
            preferences.shortBreakMinutes = profile.shortBreakMinutes
            preferences.longBreakMinutes = profile.longBreakMinutes
            preferences.timerColorArgb = profile.colorArgb
            
            _uiState.update { state ->
                val untouched = !state.isRunning && !state.isFinished && state.remainingMillis == state.totalMillis
                if (untouched) {
                    buildFreshState(state.sessionType)
                } else {
                    state.copy(
                        focusMinutes = profile.focusMinutes,
                        shortBreakMinutes = profile.shortBreakMinutes,
                        longBreakMinutes = profile.longBreakMinutes,
                        timerColorArgb = profile.colorArgb,
                        profiles = preferences.getProfiles()
                    )
                }
            }
        } else {
            _uiState.update { it.copy(profiles = preferences.getProfiles()) }
        }
    }

    /** Legacy support for duration updates. */
    fun updateDurations(focusMinutes: Int, breakMinutes: Int) {
        val profiles = preferences.getProfiles()
        val currentProfile = profiles.find { it.id == preferences.currentProfileId } ?: profiles.first()
        updateProfile(currentProfile.copy(focusMinutes = focusMinutes, shortBreakMinutes = breakMinutes))
    }

    /** Switches to a different timer profile. */
    fun switchProfile(profileId: String) {
        tickerJob?.cancel()
        preferences.currentProfileId = profileId
        val profiles = preferences.getProfiles()
        val profile = profiles.find { it.id == profileId } ?: return
        
        preferences.focusMinutes = profile.focusMinutes
        preferences.shortBreakMinutes = profile.shortBreakMinutes
        preferences.longBreakMinutes = profile.longBreakMinutes
        preferences.timerColorArgb = profile.colorArgb
        
        _uiState.update { buildFreshState(it.sessionType) }
    }

    /** Adds a new timer profile. */
    fun addProfile(name: String) {
        val newProfile = TimerProfile(
            id = UUID.randomUUID().toString(),
            name = name,
            focusMinutes = TimerPreferences.DEFAULT_FOCUS_MINUTES,
            shortBreakMinutes = TimerPreferences.DEFAULT_SHORT_BREAK_MINUTES,
            longBreakMinutes = TimerPreferences.DEFAULT_LONG_BREAK_MINUTES,
            colorArgb = TimerPreferences.DEFAULT_COLOR
        )
        preferences.saveProfile(newProfile)
        _uiState.update { it.copy(profiles = preferences.getProfiles()) }
    }

    /** Deletes a timer profile. */
    fun deleteProfile(profileId: String) {
        if (profileId == "default") return
        preferences.deleteProfile(profileId)
        if (preferences.currentProfileId == profileId) {
            switchProfile("default")
        } else {
            _uiState.update { it.copy(profiles = preferences.getProfiles()) }
        }
    }

    /** Updates global app settings (font, global color). */
    fun updateGlobalSettings(fontFamilyIndex: Int, fontColorArgb: Long) {
        preferences.globalFontFamilyIndex = fontFamilyIndex
        preferences.globalFontColorArgb = fontColorArgb
        _uiState.update { 
            it.copy(
                globalFontFamilyIndex = fontFamilyIndex,
                globalFontColorArgb = fontColorArgb
            )
        }
    }

    override fun onCleared() {
        tickerJob?.cancel()
    }

    companion object {
        /** Builds a [ViewModelProvider.Factory] wiring up the Android-backed preferences/notifier. */
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContext = context.applicationContext
                TimerViewModel(
                    preferences = AndroidTimerPreferences(appContext),
                    notifier = AndroidSessionNotifier(appContext)
                )
            }
        }
    }
}
