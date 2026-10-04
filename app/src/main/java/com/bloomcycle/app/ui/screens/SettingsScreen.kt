package com.bloomcycle.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.bloomcycle.app.domain.model.CycleContext
import com.bloomcycle.app.domain.model.TYPICAL_CYCLE_MAX
import com.bloomcycle.app.domain.model.TYPICAL_CYCLE_MIN
import com.bloomcycle.app.domain.model.UserSettings
import com.bloomcycle.app.notifications.BloomNotifications
import com.bloomcycle.app.ui.components.CycleContextRow
import com.bloomcycle.app.ui.components.Gap
import com.bloomcycle.app.ui.components.SectionHeader
import com.bloomcycle.app.ui.components.SoftCard
import com.bloomcycle.app.ui.rememberAppContainer
import com.bloomcycle.app.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Everything the user can change after onboarding, written straight back to the store.
 *
 * Reminder scheduling is *not* done here: this screen only flips flags, and the shell
 * watches them and calls the scheduler. That keeps one place responsible for WorkManager
 * instead of three.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val container = rememberAppContainer()
    val appContext = LocalContext.current
    val scope = rememberCoroutineScope()

    var settings by remember { mutableStateOf<UserSettings?>(null) }
    LaunchedEffect(container) { container.settings.settings.collect { settings = it } }
    val current = settings

    var pickingTime by remember { mutableStateOf(false) }

    // Asked once, from onboarding or here. After that, if it is still off the user has said
    // no (or been denied) and the only route left is the system settings screen, so we stop
    // prompting and just write the flag.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        scope.launch {
            container.settings.update {
                it.copy(remindersEnabled = granted, notificationPermissionAsked = true)
            }
        }
    }

    val setReminders: (Boolean) -> Unit = { enabled ->
        val needsAsk = enabled &&
            Build.VERSION.SDK_INT >= 33 &&
            !BloomNotifications.canPost(appContext) &&
            current?.notificationPermissionAsked != true
        if (needsAsk) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            scope.launch { container.settings.update { it.copy(remindersEnabled = enabled) } }
        }
    }

    Scaffold(
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (current == null) {
                Text(
                    text = "Loading…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xl),
                )
                return@Column
            }

            SectionHeader(eyebrow = "Reminders", title = "A gentle heads-up")

            SoftCard {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Period reminders", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "A note when your period is likely, and the day before.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = current.remindersEnabled,
                            onCheckedChange = setReminders,
                            modifier = Modifier.semantics {
                                contentDescription = "Period reminders, " +
                                    if (current.remindersEnabled) "on" else "off"
                            },
                        )
                    }

                    if (current.remindersEnabled) {
                        TextButton(onClick = { pickingTime = true }) {
                            Text(
                                text = "At " + "%02d:%02d".format(
                                    current.reminderHour,
                                    current.reminderMinute,
                                ),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Daily note", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "One small thing about your cycle, once a day.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = current.dailyNoteEnabled,
                            onCheckedChange = { value ->
                                scope.launch {
                                    container.settings.update { it.copy(dailyNoteEnabled = value) }
                                }
                            },
                            modifier = Modifier.semantics {
                                contentDescription = "Daily note, " +
                                    if (current.dailyNoteEnabled) "on" else "off"
                            },
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mute predictions", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Keep the calendar, silence every date prediction.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = current.predictionsMuted,
                            onCheckedChange = { value ->
                                scope.launch {
                                    container.settings.update { it.copy(predictionsMuted = value) }
                                }
                            },
                            modifier = Modifier.semantics {
                                contentDescription = "Mute predictions, " +
                                    if (current.predictionsMuted) "on" else "off"
                            },
                        )
                    }
                }
            }

            SectionHeader(eyebrow = "Your cycle", title = "What applies to you")

            SoftCard {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        text = "This changes what we are willing to promise. Logging a period " +
                            "always still works.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Gap(Spacing.xxs)
                    CycleContext.entries.forEach { option ->
                        CycleContextRow(
                            option = option,
                            selected = option == current.cycleContext,
                            onSelect = {
                                scope.launch {
                                    container.settings.update { it.copy(cycleContext = option) }
                                }
                            },
                        )
                    }
                }
            }

            SoftCard {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text("Usual cycle length", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${current.typicalCycleLength} days — used until you have logged " +
                            "enough cycles to measure your own. Logged data always wins.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = current.typicalCycleLength.toFloat(),
                        onValueChange = { value ->
                            val chosen = value.toInt().coerceIn(TYPICAL_CYCLE_MIN, TYPICAL_CYCLE_MAX)
                            scope.launch {
                                container.settings.update { it.copy(typicalCycleLength = chosen) }
                            }
                        },
                        valueRange = TYPICAL_CYCLE_MIN.toFloat()..TYPICAL_CYCLE_MAX.toFloat(),
                        steps = TYPICAL_CYCLE_MAX - TYPICAL_CYCLE_MIN - 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Usual cycle length, ${current.typicalCycleLength} days" },
                    )
                }
            }

            SectionHeader(eyebrow = "Privacy", title = "Stays on this device")

            SoftCard {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        text = "Your cycles are encrypted on this phone with a key only you hold. " +
                            "Nothing is uploaded, and there is no account to create or leak. " +
                            "Clearing this app's data removes them for good.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Gap(Spacing.xxxs)
                    Text(
                        text = "Bloom is not medical advice. Predictions are estimates built " +
                            "from what you log, not a diagnosis — talk to a clinician about " +
                            "anything that concerns you.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (pickingTime && current != null) {
        val timeState = rememberTimePickerState(
            initialHour = current.reminderHour,
            initialMinute = current.reminderMinute,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            title = { Text("Reminder time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val hour = timeState.hour
                        val minute = timeState.minute
                        scope.launch {
                            container.settings.update { it.copy(reminderHour = hour, reminderMinute = minute) }
                        }
                        pickingTime = false
                    },
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { pickingTime = false }) { Text("Cancel") }
            },
        )
    }
}
