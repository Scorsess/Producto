package com.producto.timer

import com.producto.timer.nextcloud.NextcloudTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private var virtualTime = 0L
    private val preferences = FakeTimerPreferences()
    private val notifier = FakeSessionNotifier()
    private val nextcloudPrefs = FakeNextcloudPreferences()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        virtualTime = 0L
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = TimerViewModel(
        preferences = preferences,
        notifier = notifier,
        nextcloudPrefs = nextcloudPrefs,
        now = { virtualTime }
    )

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

        assertEquals(0L, viewModel.uiState.value.remainingMillis)
        assertEquals(false, viewModel.uiState.value.isRunning)
        assertEquals(true, viewModel.uiState.value.isFinished)
        assertEquals(1, notifier.notifyCount)
    }

    @Test
    fun pause_stopsCountdownPreservingRemainingTime() = runTest {
        val viewModel = createViewModel()

        viewModel.start()
        virtualTime += 5_000
        dispatcher.scheduler.advanceTimeBy(5_000)
        dispatcher.scheduler.runCurrent()

        val remainingAtPause = viewModel.uiState.value.remainingMillis
        viewModel.pause()
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

        assertEquals(SessionType.SHORT_BREAK, viewModel.uiState.value.sessionType)
        assertEquals(preferences.shortBreakMinutes * 60_000L, viewModel.uiState.value.remainingMillis)
        assertEquals(true, viewModel.uiState.value.isRunning)
        assertEquals(false, viewModel.uiState.value.isFinished)
        assertEquals(1, viewModel.uiState.value.completedFocusSessions)
        assertEquals(1, preferences.completedFocusSessions)
    }

    @Test
    fun startNextSession_fromBreak_doesNotIncrementFocusCount() = runTest {
        val viewModel = createViewModel()

        viewModel.startNextSession() // Focus -> Short Break, count = 1
        viewModel.startNextSession() // Short Break -> Focus, count unchanged

        assertEquals(SessionType.FOCUS, viewModel.uiState.value.sessionType)
        assertEquals(1, viewModel.uiState.value.completedFocusSessions)
    }

    @Test
    fun selectActiveTask_updatesStateAndPreferences() {
        val viewModel = createViewModel()
        val task = NextcloudTask(uid = "123", summary = "Nextcloud Task 1")

        viewModel.selectActiveTask(task)
        assertEquals(task, viewModel.uiState.value.activeTask)
        assertEquals("123", nextcloudPrefs.activeTaskUid)
        assertEquals("Nextcloud Task 1", nextcloudPrefs.activeTaskSummary)

        viewModel.selectActiveTask(null)
        assertNull(viewModel.uiState.value.activeTask)
        assertNull(nextcloudPrefs.activeTaskUid)
        assertNull(nextcloudPrefs.activeTaskSummary)
    }

    @Test
    fun disconnectNextcloud_resetsTasksAndActiveTask() {
        val viewModel = createViewModel()
        val task = NextcloudTask(uid = "123", summary = "Nextcloud Task 1")
        viewModel.selectActiveTask(task)

        viewModel.disconnectNextcloud()
        assertNull(viewModel.uiState.value.nextcloudConfig)
        assertEquals(0, viewModel.uiState.value.nextcloudTasks.size)
        assertNull(viewModel.uiState.value.activeTask)
    }
}
