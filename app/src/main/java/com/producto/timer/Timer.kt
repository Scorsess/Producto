package com.producto.timer

/** The phases of a productivity cycle. */
enum class SessionType(val label: String) {
    FOCUS(label = "Focus"),
    SHORT_BREAK(label = "Short Break"),
    LONG_BREAK(label = "Long Break");

    /** The session that follows this one when a countdown completes. */
    fun next(): SessionType = when (this) {
        FOCUS -> SHORT_BREAK
        SHORT_BREAK -> FOCUS
        LONG_BREAK -> FOCUS
    }
}

/** A configuration profile for the timer. */
data class TimerProfile(
    val id: String,
    val name: String,
    val focusMinutes: Int,
    val shortBreakMinutes: Int,
    val longBreakMinutes: Int,
    val colorArgb: Long
)

/** Formats a millisecond duration as MM:SS (Pomodoro sessions never reach an hour). */
fun formatMillis(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
