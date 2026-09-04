package com.producto.timer

/** In-memory [TimerPreferences] fake so [TimerViewModel] can be unit tested without a Context. */
class FakeTimerPreferences(
    override var focusMinutes: Int = TimerPreferences.DEFAULT_FOCUS_MINUTES,
    override var breakMinutes: Int = TimerPreferences.DEFAULT_BREAK_MINUTES,
    override var completedFocusSessions: Int = 0
) : TimerPreferences
