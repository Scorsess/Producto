package com.producto.timer.nextcloud

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.producto.timer.TimerUiState
import com.producto.timer.TimerViewModel

@Composable
fun NextcloudTasksDialog(
    state: TimerUiState,
    viewModel: TimerViewModel,
    timerColor: Color,
    onDismiss: () -> Unit
) {
    var isEditingAccount by remember { mutableStateOf(state.nextcloudConfig == null) }
    var serverUrlInput by remember { mutableStateOf(state.nextcloudConfig?.serverUrl.orEmpty()) }
    var usernameInput by remember { mutableStateOf(state.nextcloudConfig?.username.orEmpty()) }
    var passwordInput by remember { mutableStateOf(state.nextcloudConfig?.appPassword.orEmpty()) }

    val darkBackground = Color(0xFF121212)
    val cardBackground = Color(0xFF1E1E1E)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = darkBackground,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Nextcloud Tasks",
                        style = MaterialTheme.typography.titleLarge,
                        color = timerColor,
                        fontWeight = FontWeight.Bold
                    )
                    if (state.isNextcloudLoading) {
                        Spacer(modifier = Modifier.width(10.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = timerColor,
                            strokeWidth = 2.dp
                        )
                    }
                }
                Row {
                    if (state.nextcloudConfig != null) {
                        IconButton(onClick = { viewModel.fetchNextcloudTasks() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = timerColor)
                        }
                        IconButton(onClick = { isEditingAccount = !isEditingAccount }) {
                            Icon(Icons.Default.Settings, contentDescription = "Account Settings", tint = timerColor)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = timerColor.copy(alpha = 0.7f))
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Status / Error Messages
                state.nextcloudError?.let { err ->
                    Text(
                        text = err,
                        color = Color(0xFFEF5350),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }

                state.nextcloudSuccessMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = Color(0xFF66BB6A),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }

                if (isEditingAccount) {
                    // Account setup form
                    Text(
                        text = if (state.nextcloudConfig == null) "Connect to your Nextcloud server:" else "Nextcloud Account Settings:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = serverUrlInput,
                        onValueChange = { serverUrlInput = it },
                        label = { Text("Server URL (e.g. https://cloud.example.com)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = timerColor,
                            focusedLabelColor = timerColor,
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Username") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = timerColor,
                            focusedLabelColor = timerColor,
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("App Password / Token") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = timerColor,
                            focusedLabelColor = timerColor,
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Tip: Create a dedicated token in Nextcloud: Settings → Security → Devices & sessions → Create new app password.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (state.nextcloudConfig != null) {
                            TextButton(
                                onClick = {
                                    viewModel.disconnectNextcloud()
                                    serverUrlInput = ""
                                    usernameInput = ""
                                    passwordInput = ""
                                }
                            ) {
                                Text("Disconnect", color = Color(0xFFEF5350))
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Button(
                            onClick = {
                                if (serverUrlInput.isNotBlank() && usernameInput.isNotBlank() && passwordInput.isNotBlank()) {
                                    viewModel.connectNextcloud(serverUrlInput, usernameInput, passwordInput)
                                    isEditingAccount = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = timerColor)
                        ) {
                            Text("Connect", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Task list view
                    if (state.nextcloudTasks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (state.isNextcloudLoading) "Syncing open tasks..." else "No open tasks found in Nextcloud.",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        Text(
                            text = "Select a task to attach to your focus timer:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 340.dp)
                        ) {
                            items(state.nextcloudTasks, key = { it.uid }) { task ->
                                val isSelected = state.activeTask?.uid == task.uid

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) timerColor else Color.White.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .background(
                                            color = if (isSelected) timerColor.copy(alpha = 0.12f) else cardBackground,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            if (isSelected) {
                                                viewModel.selectActiveTask(null)
                                            } else {
                                                viewModel.selectActiveTask(task)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Checkbox / Mark complete button
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .border(
                                                width = 1.5.dp,
                                                color = if (isSelected) timerColor else Color.White.copy(alpha = 0.4f),
                                                shape = CircleShape
                                            )
                                            .clickable { viewModel.completeTask(task) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Empty circle for user to tap to complete
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.summary,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) timerColor else Color.White,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (task.dueDate != null || task.calendarName != null) {
                                            Row(modifier = Modifier.padding(top = 2.dp)) {
                                                task.calendarName?.let { cal ->
                                                    Text(
                                                        text = cal,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White.copy(alpha = 0.4f)
                                                    )
                                                }
                                                if (task.calendarName != null && task.dueDate != null) {
                                                    Text(
                                                        text = " • ",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White.copy(alpha = 0.4f)
                                                    )
                                                }
                                                task.dueDate?.let { due ->
                                                    Text(
                                                        text = "Due: $due",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFFFFB74D)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Active",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = timerColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isEditingAccount && state.activeTask != null) {
                TextButton(onClick = { viewModel.selectActiveTask(null) }) {
                    Text("Clear Active Task", color = Color.White.copy(alpha = 0.6f))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = timerColor)
            }
        }
    )
}
