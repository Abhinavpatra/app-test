package com.bloomcycle.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bloomcycle.app.domain.cycle.CycleCalculator
import com.bloomcycle.app.domain.cycle.PhaseResolver
import com.bloomcycle.app.domain.insight.Astrology
import com.bloomcycle.app.domain.insight.Biorhythm
import com.bloomcycle.app.domain.insight.InclinationProfile
import com.bloomcycle.app.domain.insights.FertilityReadout
import com.bloomcycle.app.domain.model.EntitlementState
import com.bloomcycle.app.domain.model.PremiumFeature
import com.bloomcycle.app.domain.model.UserSettings
import com.bloomcycle.app.domain.wellness.WorkoutAdvisor
import com.bloomcycle.app.ui.LocalNavController
import com.bloomcycle.app.ui.components.DateField
import com.bloomcycle.app.ui.components.Gap
import com.bloomcycle.app.ui.components.Hairline
import com.bloomcycle.app.ui.components.PremiumGate
import com.bloomcycle.app.ui.components.SectionHeader
import com.bloomcycle.app.ui.components.SoftCard
import com.bloomcycle.app.ui.format.formatDateRange
import com.bloomcycle.app.ui.format.toCycleDate
import com.bloomcycle.app.ui.format.toPickerMillis
import com.bloomcycle.app.ui.navigation.BloomDestination
import com.bloomcycle.app.ui.rememberAppContainer
import com.bloomcycle.app.ui.theme.Spacing
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Phase 10 — the three premium analyses, one screen.
 *
 * Every section sits behind [PremiumGate] for its own feature, so a locked reading is a
 * teaser with a one-tap route to the paywall rather than a blank space. The hard rules
 * plan.md sets for this phase are structural, not copy-paste:
 *
 *  - the fertile window always carries its uncertainty statement and the three disclaimers
 *    (estimate / not contraception / not a pregnancy test) — see [FertilityReadout];
 *  - the astrology cards always render [Astrology.ENTERTAINMENT_LABEL] at the bottom, in
 *    the card, where it cannot be dismissed or scrolled past (risk R1);
 *  - the inclination read is its own section with its own card, never merged into the
 *    astrological one;
 *  - the birth date is read from and written straight back to local settings — nothing here
 *    has a network path to send it down.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val container = rememberAppContainer()
    val scope = rememberCoroutineScope()
    val today = remember(container) { container.cycleClock.today() }

    var settings by remember { mutableStateOf<UserSettings?>(null) }
    LaunchedEffect(container) { container.settings.settings.collect { settings = it } }
    val current = settings

    val entitlements by container.entitlements.state
        .collectAsStateWithLifecycle(initialValue = EntitlementState())
    val periods by container.cycleRepository
        .observePeriods()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val cycles = remember(periods) { CycleCalculator.buildCycles(periods) }
    val prediction = remember(periods, today, current) {
        current?.let { CycleCalculator.predict(cycles, today, it.cycleContext) }
    }

    val navController = LocalNavController.current
    val openPaywall: () -> Unit = { navController?.navigate(BloomDestination.PAYWALL) }

    var pickingBirth by remember { mutableStateOf(false) }

    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.Start)
                    .defaultMinSize(minHeight = 48.dp)
                    .semantics { contentDescription = "Back" },
            ) {
                Text("Back")
            }

            SectionHeader(eyebrow = "Premium", title = "Readings")
            Text(
                text = "Four optional readings. Everything below is computed here, on this " +
                    "device, from what you log and what you tell us.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            DateField(
                label = "Date of birth",
                value = current?.birthDate,
                onClick = { pickingBirth = true },
                placeholder = "Not set — tap to add",
            )
            Text(
                text = "Used by the moon sign and inclination readings only. It stays in this " +
                    "app's local store and is never sent anywhere.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionHeader(eyebrow = "Prediction", title = "Fertility window")
            PremiumGate(
                feature = PremiumFeature.FERTILITY_WINDOW,
                unlocked = entitlements.isUnlocked(PremiumFeature.FERTILITY_WINDOW),
                onOpenPaywall = openPaywall,
            ) {
                val predicted = prediction
                if (predicted == null) {
                    ReadingHint(
                        title = "No window yet",
                        body = "Log two periods and the estimate appears here, with its " +
                            "uncertainty spelled out.",
                    )
                } else {
                    val readout = remember(predicted) { FertilityReadout.of(predicted) }
                    SoftCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = formatDateRange(readout.windowStart, readout.windowEnd),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Gap(Spacing.xs)
                        readout.contextLine?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Gap(Spacing.xxxs)
                        }
                        Text(text = readout.uncertainty, style = MaterialTheme.typography.bodyMedium)
                        Gap(Spacing.xs)
                        Text(
                            text = readout.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Gap(Spacing.sm)
                        Hairline()
                        Gap(Spacing.xs)
                        Text(
                            text = readout.disclaimer,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            SectionHeader(eyebrow = "Movement", title = "By phase")
            PremiumGate(
                feature = PremiumFeature.WORKOUT_PLAN,
                unlocked = entitlements.isUnlocked(PremiumFeature.WORKOUT_PLAN),
                onOpenPaywall = openPaywall,
            ) {
                val phase = remember(cycles, prediction, today) {
                    PhaseResolver.resolve(today, cycles, prediction)?.phase
                }
                val advice = remember(phase) { WorkoutAdvisor.forPhase(phase) }
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = advice.summary, style = MaterialTheme.typography.titleMedium)
                    Gap(Spacing.xxxs)
                    Text(
                        text = advice.suggestion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Gap(Spacing.sm)
                    Hairline()
                    Gap(Spacing.xs)
                    Text(
                        text = WorkoutAdvisor.LISTEN_NOTE,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SectionHeader(eyebrow = "Reflection", title = "Natal moon")
            PremiumGate(
                feature = PremiumFeature.PERSONALITY_READING,
                unlocked = entitlements.isUnlocked(PremiumFeature.PERSONALITY_READING),
                onOpenPaywall = openPaywall,
            ) {
                val birth = current?.birthDate
                if (birth == null) {
                    ReadingHint(
                        title = "Add your date of birth",
                        body = "Use the field above and this reading appears. It stays on this " +
                            "device.",
                    )
                } else {
                    val sign = remember(birth) { Astrology.natalMoonSign(birth) }
                    val rhythm = remember(birth, today) { Astrology.biorhythm(birth, today) }
                    SoftCard(modifier = Modifier.fillMaxWidth()) {
                        Text(text = sign.label, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = sign.dates,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Gap(Spacing.sm)
                        BiorhythmRow("Physical", rhythm.physical, rhythm)
                        BiorhythmRow("Emotional", rhythm.emotional, rhythm)
                        BiorhythmRow("Intellectual", rhythm.intellectual, rhythm)
                        Gap(Spacing.sm)
                        Hairline()
                        Gap(Spacing.xs)
                        Text(
                            text = Astrology.ENTERTAINMENT_LABEL,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            SectionHeader(eyebrow = "Reflection", title = "Inclination")
            PremiumGate(
                feature = PremiumFeature.PERSONALITY_READING,
                unlocked = entitlements.isUnlocked(PremiumFeature.PERSONALITY_READING),
                onOpenPaywall = openPaywall,
            ) {
                val birth = current?.birthDate
                if (birth == null) {
                    ReadingHint(
                        title = "Add your date of birth",
                        body = "One line for study focus, one for movement focus — kept apart " +
                            "from the reading above.",
                    )
                } else {
                    val profile = remember(birth) { InclinationProfile.of(birth) }
                    SoftCard(modifier = Modifier.fillMaxWidth()) {
                        Text(text = profile.inclination.label, style = MaterialTheme.typography.titleMedium)
                        Gap(Spacing.xxxs)
                        Text(
                            text = profile.focus,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Gap(Spacing.sm)
                        Text("Study focus", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = profile.studyFocus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Gap(Spacing.xs)
                        Text("Movement focus", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = profile.athleticFocus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Gap(Spacing.sm)
                        Hairline()
                        Gap(Spacing.xs)
                        Text(
                            text = Astrology.ENTERTAINMENT_LABEL,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (pickingBirth) {
        val initial = current?.birthDate ?: today
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initial.toPickerMillis())
        DatePickerDialog(
            onDismissRequest = { pickingBirth = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.toCycleDate()?.let { chosen ->
                            scope.launch { container.settings.update { it.copy(birthDate = chosen) } }
                        }
                        pickingBirth = false
                    },
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { pickingBirth = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun BiorhythmRow(label: String, value: Int, rhythm: Biorhythm) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "$value% ${rhythm.word(value)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReadingHint(title: String, body: String) {
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Gap(Spacing.xxxs)
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
