package com.producto.timer

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.producto.timer.ui.theme.ProductoTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

/** Brightness fraction applied to the window while a countdown is actively running. */
private const val DIMMED_SCREEN_BRIGHTNESS = 0.05f
private const val EXTRA_DIM_BRIGHTNESS = 0.005f
private val TIMER_BLACK = Color.Black

private enum class DisplayMode {
    CLOCK,
    TIMER
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("TimerApp", "MainActivity.onCreate started")
        try {
            setContent {
                val viewModel: TimerViewModel = viewModel(factory = TimerViewModel.factory(LocalContext.current))
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                
                ProductoTheme(fontFamilyIndex = state.globalFontFamilyIndex) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        TimerScreen(viewModel = viewModel)
                    }
                }
            }
            android.util.Log.d("TimerApp", "MainActivity.setContent finished")
        } catch (e: Exception) {
            android.util.Log.e("TimerApp", "Crash in onCreate", e)
        }
    }
}

/**
 * The main screen of the application.
 *
 * This composable manages the high-level UI state, including:
 * - Switching between [DisplayMode.TIMER] and [DisplayMode.CLOCK].
 * - Handling window immersive mode and brightness (standard dim vs extra dim).
 * - Gesture detection for vertical navigation (swiping) and timer controls (tapping).
 * - Orchestrating settings dialogs for profiles and global app preferences.
 */
@Composable
fun TimerScreen(
    viewModel: TimerViewModel = viewModel(factory = TimerViewModel.factory(LocalContext.current))
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showProfileManager by rememberSaveable { mutableStateOf(false) }
    var showAppSettings by rememberSaveable { mutableStateOf(false) }
    var displayMode by rememberSaveable { mutableStateOf(DisplayMode.TIMER) }
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var isExtraDim by rememberSaveable { mutableStateOf(false) }
    
    val isActiveTimer = displayMode == DisplayMode.TIMER && state.isRunning
    val timerColor = Color(state.globalFontColorArgb.toInt())
    
    val timerFontSize = if (isLandscape) (102 * 0.85f).sp else 102.sp
    val clockFontSize = if (isLandscape) timerFontSize else (102 * 0.75f).sp
    val circleSize = if (isLandscape) (320 * 0.85f).dp else 320.dp
    
    val displayFontFamily = com.producto.timer.ui.theme.getDisplayFontFamily(state.globalFontFamilyIndex)
    val displayFontWeight = com.producto.timer.ui.theme.getDisplayFontWeight(state.globalFontFamilyIndex)

    LaunchedEffect(displayMode) {
        if (displayMode == DisplayMode.CLOCK) {
            while (true) {
                currentTimeMillis = System.currentTimeMillis()
                delay(1000)
            }
        }
    }

    /**
     * Toggles between standard dimming (applied when a timer is running)
     * and a user-triggered "Extra Dim" mode.
     */
    DisposableEffect(isActiveTimer, isExtraDim) {
        val window = (context as? Activity)?.window
        val insetsController = window?.let { WindowInsetsControllerCompat(it, view) }

        fun applyRunningState(isRunning: Boolean, extraDim: Boolean) {
            view.keepScreenOn = isRunning
            window?.attributes = window?.attributes?.apply {
                screenBrightness = when {
                    extraDim -> EXTRA_DIM_BRIGHTNESS
                    isRunning -> DIMMED_SCREEN_BRIGHTNESS
                    else -> WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }
            if (isRunning && window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                insetsController?.hide(WindowInsetsCompat.Type.systemBars())
                insetsController?.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, true)
                insetsController?.show(WindowInsetsCompat.Type.systemBars())
            }
        }

        applyRunningState(isActiveTimer, isExtraDim)

        onDispose {
            view.keepScreenOn = false
            window?.attributes = window?.attributes?.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, true)
            }
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TIMER_BLACK)
            .pointerInput(displayMode, state.sessionType) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (kotlin.math.abs(dragAmount.y) > 60) {
                            if (dragAmount.y > 0) {
                                // Swipe Down: Move Forward
                                // Order: Clock -> Focus -> Long Break -> Short Break -> Clock
                                when {
                                    displayMode == DisplayMode.CLOCK -> {
                                        displayMode = DisplayMode.TIMER
                                        viewModel.setSessionType(SessionType.FOCUS)
                                    }
                                    state.sessionType == SessionType.FOCUS -> {
                                        viewModel.setSessionType(SessionType.LONG_BREAK)
                                    }
                                    state.sessionType == SessionType.LONG_BREAK -> {
                                        viewModel.setSessionType(SessionType.SHORT_BREAK)
                                    }
                                    state.sessionType == SessionType.SHORT_BREAK -> {
                                        displayMode = DisplayMode.CLOCK
                                    }
                                }
                            } else {
                                // Swipe Up: Move Backward
                                when {
                                    displayMode == DisplayMode.CLOCK -> {
                                        displayMode = DisplayMode.TIMER
                                        viewModel.setSessionType(SessionType.SHORT_BREAK)
                                    }
                                    state.sessionType == SessionType.SHORT_BREAK -> {
                                        viewModel.setSessionType(SessionType.LONG_BREAK)
                                    }
                                    state.sessionType == SessionType.LONG_BREAK -> {
                                        viewModel.setSessionType(SessionType.FOCUS)
                                    }
                                    state.sessionType == SessionType.FOCUS -> {
                                        displayMode = DisplayMode.CLOCK
                                    }
                                }
                            }
                        }
                    }
                )
            }
            .pointerInput(displayMode, state.isFinished) {
                detectTapGestures(
                    onTap = {
                        if (displayMode == DisplayMode.TIMER) {
                            if (state.isFinished) viewModel.startNextSession() else viewModel.togglePause()
                        }
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ModeSelector(
                label = "CLOCK",
                selected = displayMode == DisplayMode.CLOCK,
                color = timerColor,
                onClick = { displayMode = DisplayMode.CLOCK }
            )
            ModeSelector(
                label = "TIMER",
                selected = displayMode == DisplayMode.TIMER,
                color = timerColor,
                onClick = { displayMode = DisplayMode.TIMER }
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (displayMode == DisplayMode.CLOCK) {
                Text(
                    text = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        .format(Date(currentTimeMillis)),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = clockFontSize,
                        fontFamily = displayFontFamily,
                        fontWeight = displayFontWeight
                    ),
                    color = timerColor
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SessionTypeSelector(
                        label = "Focus",
                        selected = state.sessionType == SessionType.FOCUS,
                        color = timerColor,
                        onClick = { viewModel.setSessionType(SessionType.FOCUS) }
                    )
                    SessionTypeSelector(
                        label = "Short Break",
                        selected = state.sessionType == SessionType.SHORT_BREAK,
                        color = timerColor,
                        onClick = { viewModel.setSessionType(SessionType.SHORT_BREAK) }
                    )
                    SessionTypeSelector(
                        label = "Long Break",
                        selected = state.sessionType == SessionType.LONG_BREAK,
                        color = timerColor,
                        onClick = { viewModel.setSessionType(SessionType.LONG_BREAK) }
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Box(contentAlignment = Alignment.Center) {
                    val progress = if (state.totalMillis > 0) {
                        state.remainingMillis.toFloat() / state.totalMillis
                    } else 0f
                    
                    Canvas(modifier = Modifier.size(circleSize)) {
                        drawArc(
                            color = timerColor.copy(alpha = 0.2f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = timerColor,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.sessionType.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = timerColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = formatMillis(state.remainingMillis),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = timerFontSize,
                                fontFamily = displayFontFamily,
                                fontWeight = displayFontWeight
                            ),
                            color = timerColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
                Text(
                    text = when {
                        state.isFinished -> "Tap to start ${state.sessionType.next().label}"
                        state.isRunning -> "Tap to pause"
                        else -> "Tap to start"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = timerColor.copy(alpha = 0.8f)
                )
                if (!state.isRunning) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Focus sessions completed: ${state.completedFocusSessions}",
                        style = MaterialTheme.typography.bodySmall,
                        color = timerColor.copy(alpha = 0.7f)
                    )
                }
            }
        }

        if (!state.isRunning) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                IconButton(onClick = { showAppSettings = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Global Settings", tint = timerColor)
                }
                if (displayMode == DisplayMode.TIMER) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\u2699",
                        style = MaterialTheme.typography.headlineSmall,
                        color = timerColor,
                        modifier = Modifier.clickable { showSettings = true }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Profiles",
                        style = MaterialTheme.typography.labelSmall,
                        color = timerColor,
                        modifier = Modifier.clickable { showProfileManager = true }
                    )
                }
            }
        }
        
        // Extra Dim Button
        IconButton(
            onClick = { isExtraDim = !isExtraDim },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text(
                text = "\u2600", // Sun symbol
                fontSize = 24.sp,
                color = if (isExtraDim) timerColor else timerColor.copy(alpha = 0.4f)
            )
        }
    }

    if (showAppSettings) {
        AppSettingsDialog(
            currentFontFamilyIndex = state.globalFontFamilyIndex,
            currentFontColorArgb = state.globalFontColorArgb,
            onDismiss = { showAppSettings = false },
            onSave = { fontIndex, colorArgb ->
                viewModel.updateGlobalSettings(fontIndex, colorArgb)
                showAppSettings = false
            }
        )
    }

    if (showSettings) {
        val currentProfile = state.profiles.find { it.id == state.currentProfileId }
        if (currentProfile != null) {
            ProfileSettingsDialog(
                profile = currentProfile,
                onDismiss = { showSettings = false },
                onSave = { updatedProfile ->
                    viewModel.updateProfile(updatedProfile)
                    showSettings = false
                }
            )
        }
    }

    if (showProfileManager) {
        ProfileManagerDialog(
            profiles = state.profiles,
            currentProfileId = state.currentProfileId,
            onDismiss = { showProfileManager = false },
            onSelect = { id ->
                viewModel.switchProfile(id)
                showProfileManager = false
            },
            onAdd = { name -> viewModel.addProfile(name) },
            onDelete = { id -> viewModel.deleteProfile(id) }
        )
    }
}

@Composable
private fun ModeSelector(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = color.copy(alpha = if (selected) 1f else 0.45f),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun SessionTypeSelector(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color.copy(alpha = if (selected) 1f else 0.45f),
        modifier = Modifier
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = color.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileSettingsDialog(
    profile: TimerProfile,
    onDismiss: () -> Unit,
    onSave: (TimerProfile) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var focusText by remember { mutableStateOf(profile.focusMinutes.toString()) }
    var shortBreakText by remember { mutableStateOf(profile.shortBreakMinutes.toString()) }
    var longBreakText by remember { mutableStateOf(profile.longBreakMinutes.toString()) }
    var selectedColor by remember { mutableStateOf(Color(profile.colorArgb)) }

    val focusMinutes = focusText.toIntOrNull()
    val shortMinutes = shortBreakText.toIntOrNull()
    val longMinutes = longBreakText.toIntOrNull()
    val minutesRange = TimerPreferences.MIN_MINUTES..TimerPreferences.MAX_MINUTES
    val isValid = focusMinutes != null && focusMinutes in minutesRange &&
            shortMinutes != null && shortMinutes in minutesRange &&
            longMinutes != null && longMinutes in minutesRange &&
            name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profile settings") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = focusText,
                    onValueChange = { focusText = it },
                    label = { Text("Focus minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = shortBreakText,
                    onValueChange = { shortBreakText = it },
                    label = { Text("Short break minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = longBreakText,
                    onValueChange = { longBreakText = it },
                    label = { Text("Long break minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Theme Color", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                
                val colors = listOf(
                    Color(0xFF00E676), Color(0xFF2979FF), Color(0xFFFF5252),
                    Color(0xFFFFD740), Color(0xFFE040FB), Color(0xFF1DE9B6),
                    Color(0xFFFFFFFF), Color(0xFFFF9100)
                )
                
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(color, CircleShape)
                                .border(
                                    width = if (selectedColor == color) 2.dp else 0.dp,
                                    color = Color.Black,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        profile.copy(
                            name = name,
                            focusMinutes = focusMinutes!!,
                            shortBreakMinutes = shortMinutes!!,
                            longBreakMinutes = longMinutes!!,
                            colorArgb = selectedColor.toArgb().toLong()
                        )
                    )
                },
                enabled = isValid
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ProfileManagerDialog(
    profiles: List<TimerProfile>,
    currentProfileId: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onAdd: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var newProfileName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Profiles") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    profiles.forEach { profile ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(profile.id) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = profile.name,
                                color = if (profile.id == currentProfileId) 
                                    Color(profile.colorArgb) else MaterialTheme.colorScheme.onSurface
                            )
                            if (profile.id != "default") {
                                IconButton(onClick = { onDelete(profile.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        label = { Text("New Profile Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newProfileName.isNotBlank()) {
                                onAdd(newProfileName)
                                newProfileName = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Profile")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppSettingsDialog(
    currentFontFamilyIndex: Int,
    currentFontColorArgb: Long,
    onDismiss: () -> Unit,
    onSave: (Int, Long) -> Unit
) {
    var selectedFontIndex by remember { mutableStateOf(currentFontFamilyIndex) }
    var selectedColor by remember { mutableStateOf(Color(currentFontColorArgb.toInt())) }
    var showFontOptions by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("App Settings") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                TextButton(
                    onClick = { showFontOptions = !showFontOptions },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showFontOptions) "Hide Font Styles" else "Font Styles")
                }
                
                if (showFontOptions) {
                    Spacer(modifier = Modifier.height(8.dp))
                    com.producto.timer.ui.theme.FontNames.forEachIndexed { index, name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFontIndex = index }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = selectedFontIndex == index,
                                onClick = { selectedFontIndex = index }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = name,
                                fontFamily = com.producto.timer.ui.theme.getDisplayFontFamily(index),
                                fontWeight = com.producto.timer.ui.theme.getDisplayFontWeight(index),
                                fontSize = 24.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Global Font Color", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                
                val colors = listOf(
                    Color(0xFF00E676), Color(0xFF2979FF), Color(0xFFFF5252),
                    Color(0xFFFFD740), Color(0xFFE040FB), Color(0xFF1DE9B6),
                    Color(0xFFFFFFFF), Color(0xFFFF9100), Color(0xFFE91E63),
                    Color(0xFF9C27B0)
                )
                
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(color, CircleShape)
                                .border(
                                    width = if (selectedColor == color) 2.dp else 0.dp,
                                    color = Color.Black,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(selectedFontIndex, selectedColor.toArgb().toLong())
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun TimerScreenPreview() {
    ProductoTheme {
        TimerScreen()
    }
}
