package com.producto.timer

/** In-memory [TimerPreferences] fake so [TimerViewModel] can be unit tested without a Context. */
class FakeTimerPreferences(
    override var focusMinutes: Int = TimerPreferences.DEFAULT_FOCUS_MINUTES,
    override var shortBreakMinutes: Int = TimerPreferences.DEFAULT_SHORT_BREAK_MINUTES,
    override var longBreakMinutes: Int = TimerPreferences.DEFAULT_LONG_BREAK_MINUTES,
    override var completedFocusSessions: Int = 0,
    override var timerColorArgb: Long = TimerPreferences.DEFAULT_COLOR,
    override var currentProfileId: String = "default",
    override var globalFontFamilyIndex: Int = 0,
    override var globalFontColorArgb: Long = TimerPreferences.DEFAULT_COLOR
) : TimerPreferences {

    private val profiles = mutableListOf(
        TimerProfile(
            id = "default",
            name = "Classic",
            focusMinutes = focusMinutes,
            shortBreakMinutes = shortBreakMinutes,
            longBreakMinutes = longBreakMinutes,
            colorArgb = timerColorArgb
        )
    )

    override fun getProfiles(): List<TimerProfile> = profiles.toList()

    override fun saveProfile(profile: TimerProfile) {
        val index = profiles.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            profiles[index] = profile
        } else {
            profiles.add(profile)
        }
    }

    override fun deleteProfile(id: String) {
        profiles.removeAll { it.id == id }
    }
}
