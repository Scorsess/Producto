package com.producto.timer

import android.content.Context
import androidx.core.content.edit

/**
 * Persistent storage for session durations, multiple profiles, and global UI preferences.
 *
 * This implementation uses [android.content.SharedPreferences] to store:
 * - Specific durations for each [SessionType].
 * - A collection of user-defined [TimerProfile]s.
 * - Global aesthetic choices like [globalFontFamilyIndex] and [globalFontColorArgb].
 */
interface TimerPreferences {
    var focusMinutes: Int
    var shortBreakMinutes: Int
    var longBreakMinutes: Int
    var completedFocusSessions: Int
    var timerColorArgb: Long

    var currentProfileId: String
    var globalFontFamilyIndex: Int
    var globalFontColorArgb: Long
    fun getProfiles(): List<TimerProfile>
    fun saveProfile(profile: TimerProfile)
    fun deleteProfile(id: String)

    companion object {
        const val DEFAULT_FOCUS_MINUTES = 25
        const val DEFAULT_SHORT_BREAK_MINUTES = 5
        const val DEFAULT_LONG_BREAK_MINUTES = 15
        const val DEFAULT_COLOR = 0xFF00E676L
        const val MIN_MINUTES = 1
        const val MAX_MINUTES = 180
    }
}

/** The countdown length for [session], derived from the configured minutes. */
fun TimerPreferences.durationMillis(session: SessionType): Long {
    val minutes = when (session) {
        SessionType.FOCUS -> focusMinutes
        SessionType.SHORT_BREAK -> shortBreakMinutes
        SessionType.LONG_BREAK -> longBreakMinutes
    }
    return minutes * 60_000L
}

/** [TimerPreferences] backed by [android.content.SharedPreferences]. */
class AndroidTimerPreferences(context: Context) : TimerPreferences {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override var focusMinutes: Int
        get() = prefs.getInt(KEY_FOCUS_MINUTES, TimerPreferences.DEFAULT_FOCUS_MINUTES)
        set(value) = prefs.edit { putInt(KEY_FOCUS_MINUTES, value) }

    override var shortBreakMinutes: Int
        get() = prefs.getInt(KEY_SHORT_BREAK_MINUTES, TimerPreferences.DEFAULT_SHORT_BREAK_MINUTES)
        set(value) = prefs.edit { putInt(KEY_SHORT_BREAK_MINUTES, value) }

    override var longBreakMinutes: Int
        get() = prefs.getInt(KEY_LONG_BREAK_MINUTES, TimerPreferences.DEFAULT_LONG_BREAK_MINUTES)
        set(value) = prefs.edit { putInt(KEY_LONG_BREAK_MINUTES, value) }

    override var completedFocusSessions: Int
        get() = prefs.getInt(KEY_COMPLETED_SESSIONS, 0)
        set(value) = prefs.edit { putInt(KEY_COMPLETED_SESSIONS, value) }

    override var timerColorArgb: Long
        get() = try {
            prefs.getLong(KEY_TIMER_COLOR, TimerPreferences.DEFAULT_COLOR)
        } catch (e: Exception) {
            TimerPreferences.DEFAULT_COLOR
        }
        set(value) = prefs.edit { putLong(KEY_TIMER_COLOR, value) }

    override var currentProfileId: String
        get() = prefs.getString(KEY_CURRENT_PROFILE_ID, "default") ?: "default"
        set(value) = prefs.edit { putString(KEY_CURRENT_PROFILE_ID, value) }

    override var globalFontFamilyIndex: Int
        get() = prefs.getInt(KEY_FONT_FAMILY, 0)
        set(value) = prefs.edit { putInt(KEY_FONT_FAMILY, value) }

    override var globalFontColorArgb: Long
        get() = prefs.getLong(KEY_FONT_COLOR, TimerPreferences.DEFAULT_COLOR)
        set(value) = prefs.edit { putLong(KEY_FONT_COLOR, value) }

    override fun getProfiles(): List<TimerProfile> {
        return try {
            val ids = prefs.getStringSet(KEY_PROFILE_IDS, setOf("default")) ?: setOf("default")
            ids.map { id ->
                TimerProfile(
                    id = id,
                    name = prefs.getString("profile_${id}_name", if (id == "default") "Classic Pomodoro" else "Timer $id") ?: "Timer $id",
                    focusMinutes = prefs.getInt("profile_${id}_focus", TimerPreferences.DEFAULT_FOCUS_MINUTES),
                    shortBreakMinutes = prefs.getInt("profile_${id}_short_break", TimerPreferences.DEFAULT_SHORT_BREAK_MINUTES),
                    longBreakMinutes = prefs.getInt("profile_${id}_long_break", TimerPreferences.DEFAULT_LONG_BREAK_MINUTES),
                    colorArgb = prefs.getLong("profile_${id}_color", TimerPreferences.DEFAULT_COLOR)
                )
            }
        } catch (e: Exception) {
            listOf(
                TimerProfile(
                    id = "default",
                    name = "Classic Pomodoro",
                    focusMinutes = TimerPreferences.DEFAULT_FOCUS_MINUTES,
                    shortBreakMinutes = TimerPreferences.DEFAULT_SHORT_BREAK_MINUTES,
                    longBreakMinutes = TimerPreferences.DEFAULT_LONG_BREAK_MINUTES,
                    colorArgb = TimerPreferences.DEFAULT_COLOR
                )
            )
        }
    }

    override fun saveProfile(profile: TimerProfile) {
        val ids = (prefs.getStringSet(KEY_PROFILE_IDS, emptySet()) ?: emptySet()).toMutableSet()
        ids.add(profile.id)
        prefs.edit {
            putStringSet(KEY_PROFILE_IDS, ids)
            putString("profile_${profile.id}_name", profile.name)
            putInt("profile_${profile.id}_focus", profile.focusMinutes)
            putInt("profile_${profile.id}_short_break", profile.shortBreakMinutes)
            putInt("profile_${profile.id}_long_break", profile.longBreakMinutes)
            putLong("profile_${profile.id}_color", profile.colorArgb)
        }
    }

    override fun deleteProfile(id: String) {
        if (id == "default") return
        val ids = (prefs.getStringSet(KEY_PROFILE_IDS, emptySet()) ?: emptySet()).toMutableSet()
        ids.remove(id)
        prefs.edit {
            putStringSet(KEY_PROFILE_IDS, ids)
            remove("profile_${id}_name")
            remove("profile_${id}_focus")
            remove("profile_${id}_short_break")
            remove("profile_${id}_long_break")
            remove("profile_${id}_color")
        }
    }

    private companion object {
        const val PREFS_NAME = "producto_timer_prefs"
        const val KEY_FOCUS_MINUTES = "focus_minutes"
        const val KEY_SHORT_BREAK_MINUTES = "short_break_minutes"
        const val KEY_LONG_BREAK_MINUTES = "long_break_minutes"
        const val KEY_COMPLETED_SESSIONS = "completed_focus_sessions"
        const val KEY_TIMER_COLOR = "timer_color"
        const val KEY_CURRENT_PROFILE_ID = "current_profile_id"
        const val KEY_PROFILE_IDS = "profile_ids"
        const val KEY_FONT_FAMILY = "global_font_family"
        const val KEY_FONT_COLOR = "global_font_color"
    }
}
