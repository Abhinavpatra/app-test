package com.bloomcycle.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import com.bloomcycle.app.domain.model.CycleContext
import com.bloomcycle.app.domain.model.PeriodEvent
import com.bloomcycle.app.domain.model.TYPICAL_CYCLE_MAX
import com.bloomcycle.app.domain.model.TYPICAL_CYCLE_MIN
import com.bloomcycle.app.notifications.BloomNotifications
import com.bloomcycle.app.ui.components.DateField
import com.bloomcycle.app.ui.components.CycleContextRow
import com.bloomcycle.app.ui.components.Gap
import com.bloomcycle.app.ui.components.PrimaryButton
import com.bloomcycle.app.ui.components.SecondaryButton
import com.bloomcycle.app.ui.format.toCycleDate
import com.bloomcycle.app.ui.format.toPickerMillis
import com.bloomcycle.app.ui.rememberAppContainer
import com.bloomcycle.app.ui.theme.EyebrowStyle
import com.bloomcycle.app.ui.theme.Spacing
import java.time.LocalDate
import kotlinx.coroutines.launch

private enum class OnboardingStep { WELCOME, BIRTH, LAST_PERIOD, TYPICAL_LENGTH, CONTEXT, NOTIFICATIONS }

private enum class OnboardingPick { NONE, BIRTH, LAST_PERIOD }

/**
 * Six screens, one decision each, all of it optional except finishing.
 *
 * Deliberately lives outside the navigation suite: onboarding is not a place the user can
 * wander back to, so it renders instead of the shell rather than as a route in it.
 * Reminder scheduling is *not* done here — the shell watches the settings and schedules.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = rememberAppContainer()
    val scope = rememberCoroutineScope()
    val today = remember(container) { container.cycleClock.today() }
    val appContext = LocalContext.current

    val steps = OnboardingStep.entries
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[stepIndex]

    var birthDateIso by rememberSaveable { mutableStateOf<String?>(null) }
    var lastPeriodIso by rememberSaveable { mutableStateOf<String?>(null) }
    var typicalLength by rememberSaveable { mutableIntStateOf(28) }
    var contextName by rememberSaveable { mutableStateOf(CycleContext.NONE.name) }
    var picking by rememberSaveable { mutableStateOf(OnboardingPick.NONE.name) }
    val context = CycleContext.entries.firstOrNull { it.name == contextName } ?: CycleContext.NONE
    val pick = OnboardingPick.entries.firstOrNull { it.name == picking } ?: OnboardingPick.NONE

    val finish: (Boolean) -> Unit = { wantsReminders ->
        scope.launch {
            container.settings.update { current ->
                current.copy(
                    onboardingComplete = true,
                    birthDate = birthDateIso?.let(::parseIso),
                    typicalCycleLength = typicalLength,
                    cycleContext = context,
                    remindersEnabled = wantsReminders,
                    notificationPermissionAsked = true,
                )
            }
            lastPeriodIso?.let { iso ->
                parseIso(iso)?.let { start ->
                    container.cycleRepository.logPeriod(PeriodEvent(startDate = start))
                }
            }
            onFinished()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // Ask regardless of the answer: the user said they wanted reminders, and Android
        // will show the system prompt again if they later flip it back on in Settings.
        finish(true)
    }

    val turnOnReminders: () -> Unit = {
        val needsAsk = Build.VERSION.SDK_INT >= 33 && !BloomNotifications.canPost(appContext)
        if (needsAsk) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            finish(true)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screen, vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (stepIndex > 0) {
                        TextButton(onClick = { stepIndex-- }) { Text("Back") }
                    }
                    Text(
                        text = "Step ${stepIndex + 1} of ${steps.size}",
                        style = EyebrowStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .weight(1f),
                        textAlign = TextAlign.End,
                    )
                }
                LinearProgressIndicator(
                    progress = { (stepIndex + 1) / steps.size.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screen),
                )
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.screen),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    when (step) {
                        OnboardingStep.WELCOME -> PrimaryButton(
                            text = "Let's begin",
                            onClick = { stepIndex++ },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        OnboardingStep.NOTIFICATIONS -> {
                            SecondaryButton(
                                text = "Maybe later",
                                onClick = { finish(false) },
                                modifier = Modifier.weight(1f),
                            )
                            PrimaryButton(
                                text = "Turn on reminders",
                                onClick = turnOnReminders,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        else -> PrimaryButton(
                            text = if (stepIndex == steps.lastIndex) "Finish" else "Continue",
                            onClick = { stepIndex++ },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = Spacing.screen)
                .verticalScroll(rememberScrollState())
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            when (step) {
                OnboardingStep.WELCOME -> {
                    Text(
                        text = "BLOOM",
                        style = EyebrowStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Gap(Spacing.lg)
                    Text(
                        text = "◐",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Gap(Spacing.md)
                    StepCopy(
                        title = "A quiet place for your cycle",
                        body = "Log a period in a few seconds, see where you are today, and keep it " +
                            "on this device — encrypted. No account, no feed, no streaks.",
                    )
                }

                OnboardingStep.BIRTH -> {
                    StepCopy(
                        title = "When were you born?",
                        body = "Optional — it helps us use language that fits your stage of life. " +
                            "We never share it.",
                    )
                    Gap(Spacing.xs)
                    DateField(
                        label = "Date of birth",
                        value = birthDateIso?.let(::parseIso),
                        onClick = { picking = OnboardingPick.BIRTH.name },
                    )
                    if (birthDateIso != null) {
                        TextButton(onClick = { birthDateIso = null }) { Text("Clear") }
                    }
                    Text(
                        text = "You can skip this.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OnboardingStep.LAST_PERIOD -> {
                    StepCopy(
                        title = "When did your last period start?",
                        body = "That is day one. Skip it if you are not sure — you can log it later " +
                            "in one tap.",
                    )
                    Gap(Spacing.xs)
                    DateField(
                        label = "Last period started",
                        value = lastPeriodIso?.let(::parseIso),
                        onClick = { picking = OnboardingPick.LAST_PERIOD.name },
                    )
                    if (lastPeriodIso != null) {
                        TextButton(onClick = { lastPeriodIso = null }) { Text("Clear") }
                    }
                }

                OnboardingStep.TYPICAL_LENGTH -> {
                    StepCopy(
                        title = "How long is a usual cycle?",
                        body = "From the first day of one period to the first day of the next. " +
                            "We only use this until you have logged enough to measure it yourself.",
                    )
                    Gap(Spacing.xs)
                    Text(
                        text = "$typicalLength days",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SliderRow(
                        value = typicalLength.toFloat(),
                        onValueChange = { typicalLength = it.toInt() },
                        range = TYPICAL_CYCLE_MIN.toFloat()..TYPICAL_CYCLE_MAX.toFloat(),
                        steps = TYPICAL_CYCLE_MAX - TYPICAL_CYCLE_MIN - 1,
                        contentDescription = "Usual cycle length, $typicalLength days",
                    )
                    Text(
                        text = "Most people land between 21 and 35 days. Both ends of this " +
                            "slider are normal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OnboardingStep.CONTEXT -> {
                    StepCopy(
                        title = "Anything we should know?",
                        body = "This changes what we are willing to promise. Pick whatever fits.",
                    )
                    Gap(Spacing.xs)
                    CycleContext.entries.forEach { option ->
                        CycleContextRow(
                            option = option,
                            selected = option == context,
                            onSelect = { contextName = option.name },
                        )
                    }
                }

                OnboardingStep.NOTIFICATIONS -> {
                    StepCopy(
                        title = "A gentle heads-up",
                        body = "At most a couple of reminders a month: when your period is likely, " +
                            "and a small check-in before it. Nothing that nags.",
                    )
                    Gap(Spacing.xs)
                    Text(
                        text = "You can turn this off any time in Settings, and we will not ask again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (pick != OnboardingPick.NONE) {
                val initial = when (pick) {
                    OnboardingPick.BIRTH -> birthDateIso?.let(::parseIso)
                    OnboardingPick.LAST_PERIOD -> lastPeriodIso?.let(::parseIso)
                    OnboardingPick.NONE -> null
                } ?: today
                val pickerState = rememberDatePickerState(
                    initialSelectedDateMillis = initial.toPickerMillis(),
                )
                DatePickerDialog(
                    onDismissRequest = { picking = OnboardingPick.NONE.name },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val chosen = pickerState.selectedDateMillis?.toCycleDate()
                                when (pick) {
                                    OnboardingPick.BIRTH -> birthDateIso = chosen?.toString()
                                    OnboardingPick.LAST_PERIOD -> lastPeriodIso = chosen?.toString()
                                    OnboardingPick.NONE -> Unit
                                }
                                picking = OnboardingPick.NONE.name
                            },
                        ) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { picking = OnboardingPick.NONE.name }) { Text("Cancel") }
                    },
                ) {
                    DatePicker(state = pickerState)
                }
            }
        }
    }
}

@Composable
private fun StepCopy(title: String, body: String) {
    Text(text = title, style = MaterialTheme.typography.headlineSmall)
    Gap(Spacing.xs)
    Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SliderRow(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    contentDescription: String,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = range,
        steps = steps,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.contentDescription = contentDescription },
    )
}

private fun parseIso(raw: String): LocalDate? = runCatching { LocalDate.parse(raw) }.getOrNull()
