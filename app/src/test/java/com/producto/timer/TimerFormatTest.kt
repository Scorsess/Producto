package com.producto.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerFormatTest {
    @Test
    fun formatMillis_zero_returnsZeroed() {
        assertEquals("00:00", formatMillis(0))
    }

    @Test
    fun formatMillis_underOneMinute_formatsSeconds() {
        assertEquals("00:45", formatMillis(45_000))
    }

    @Test
    fun formatMillis_overOneMinute_formatsMinutesAndSeconds() {
        assertEquals("25:00", formatMillis(TimerPreferences.DEFAULT_FOCUS_MINUTES * 60_000L))
    }

    @Test
    fun sessionType_next_alternatesFocusAndBreak() {
        assertEquals(SessionType.SHORT_BREAK, SessionType.FOCUS.next())
        assertEquals(SessionType.FOCUS, SessionType.SHORT_BREAK.next())
        assertEquals(SessionType.FOCUS, SessionType.LONG_BREAK.next())
    }

    @Test
    fun durationMillis_derivesFromConfiguredMinutes() {
        val preferences = FakeTimerPreferences(focusMinutes = 50, shortBreakMinutes = 10, longBreakMinutes = 20)

        assertEquals(50 * 60_000L, preferences.durationMillis(SessionType.FOCUS))
        assertEquals(10 * 60_000L, preferences.durationMillis(SessionType.SHORT_BREAK))
        assertEquals(20 * 60_000L, preferences.durationMillis(SessionType.LONG_BREAK))
    }
}
