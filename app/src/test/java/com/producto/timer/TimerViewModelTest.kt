package com.producto.timer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private var virtualTime = 0L
    private val preferences = FakeTimerPreferences()
    private val notifier = FakeSessionNotifier()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        virtualTime = 0L
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = TimerViewModel(preferences, notifier, now = { virtualTime })

    @Test
    fun initialState_isFocusSessionNotRunning() {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(SessionType.FOCUS, state.sessionType)
        assertEquals(preferences.focusMinutes * 60_000L, state.remainingMillis)
        assertEquals(false, state.isRunning)
        assertEquals(false, state.isFinished)
        assertEquals(0, state.completedFocusSessions)
    }

    @Test
    fun start_countsDownAndFinishesAndNotifies() = runTest {
        val viewModel = createViewModel()

        viewModel.start()
        assertEquals(true, viewModel.uiState.value.isRunning)

        virtualTime += 3_000
        dispatcher.scheduler.advanceTimeBy(3_000)
        dispatcher.scheduler.runCurrent()
        assertEquals(preferences.focusMinutes * 60_000L - 3_000, viewModel.uiState.value.remainingMillis)
        assertEquals(0, notifier.notifyCount)

        virtualTime += preferences.focusMinutes * 60_000L
        dispatcher.scheduler.advanceTimeBy(preferences.focusMinutes * 60_000L)
        dispatcher.scheduler.runCurrent()
        assertEquals(true, viewModel.uiState.value.isFinished)
        assertEquals(false, viewModel.uiState.value.isRunning)
        assertEquals(0L, viewModel.uiState.value.remainingMillis)
        assertEquals(1, notifier.notifyCount)
    }

    @Test
    fun pause_stopsCountdownWithoutResettingRemainingTime() = runTest {
        val viewModel = createViewModel()

        viewModel.start()
        virtualTime += 2_000
        dispatcher.scheduler.advanceTimeBy(2_000)
        dispatcher.scheduler.runCurrent()
        viewModel.pause()

        val remainingAtPause = viewModel.uiState.value.remainingMillis
        assertEquals(false, viewModel.uiState.value.isRunning)

        virtualTime += 5_000
        dispatcher.scheduler.advanceTimeBy(5_000)
        dispatcher.scheduler.runCurrent()
        assertEquals(remainingAtPause, viewModel.uiState.value.remainingMillis)
    }

    @Test
    fun startNextSession_switchesFromFocusToBreakAndRunsAndIncrementsCount() = runTest {
        val viewModel = createViewModel()

        viewModel.startNextSession()

        assertEquals(SessionType.BREAK, viewModel.uiState.value.sessionType)
        assertEquals(preferences.breakMinutes * 60_000L, viewModel.uiState.value.remainingMillis)
        assertEquals(true, viewModel.uiState.value.isRunning)
        assertEquals(false, viewModel.uiState.value.isFinished)
        assertEquals(1, viewModel.uiState.value.completedFocusSessions)
        assertEquals(1, preferences.completedFocusSessions)
    }

    @Test
    fun startNextSession_fromBreak_doesNotIncrementFocusCount() = runTest {
        val viewModel = createViewModel()

        viewModel.startNextSession() // Focus -> Break, count = 1
        viewModel.startNextSession() // Break -> Focus, count unchanged

        assertEquals(SessionType.FOCUS, viewModel.uiState.value.sessionType)
        assertEquals(1, viewModel.uiState.value.completedFocusSessions)
    }

    @Test
    fun updateDurations_whileIdle_refreshesCurrentCountdownImmediately() {
        val viewModel = createViewModel()

        viewModel.updateDurations(focusMinutes = 50, breakMinutes = 15)

        val state = viewModel.uiState.value
        assertEquals(50, state.focusMinutes)
        assertEquals(15, state.breakMinutes)
        assertEquals(50 * 60_000L, state.totalMillis)
        assertEquals(50 * 60_000L, state.remainingMillis)
        assertEquals(50, preferences.focusMinutes)
        assertEquals(15, preferences.breakMinutes)
    }

    @Test
    fun updateDurations_whileRunning_doesNotAlterInProgressCountdown() = runTest {
        val viewModel = createViewModel()

        viewModel.start()
        virtualTime += 2_000
        dispatcher.scheduler.advanceTimeBy(2_000)
        dispatcher.scheduler.runCurrent()
        val remainingBeforeUpdate = viewModel.uiState.value.remainingMillis

        viewModel.updateDurations(focusMinutes = 50, breakMinutes = 15)

        val state = viewModel.uiState.value
        assertEquals(remainingBeforeUpdate, state.remainingMillis)
        assertEquals(50, state.focusMinutes)
        assertEquals(15, state.breakMinutes)
    }
}
