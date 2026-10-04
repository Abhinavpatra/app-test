package com.bloomcycle.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bloomcycle.app.core.time.CycleDate
import com.bloomcycle.app.domain.cycle.CycleCalculator
import com.bloomcycle.app.domain.home.HomeSummarizer
import com.bloomcycle.app.domain.home.HomeSummary
import com.bloomcycle.app.domain.model.PeriodEvent
import com.bloomcycle.app.domain.model.SymptomCatalog
import com.bloomcycle.app.domain.model.SymptomLog
import com.bloomcycle.app.domain.model.UserSettings
import com.bloomcycle.app.ui.PageScrollSound
import com.bloomcycle.app.ui.components.EmptyState
import com.bloomcycle.app.ui.components.Gap
import com.bloomcycle.app.ui.components.Hairline
import com.bloomcycle.app.ui.components.PrimaryButton
import com.bloomcycle.app.ui.components.SecondaryButton
import com.bloomcycle.app.ui.components.SectionHeader
import com.bloomcycle.app.ui.components.SoftCard
import com.bloomcycle.app.ui.format.formatDateRange
import com.bloomcycle.app.ui.format.formatHeadingDate
import com.bloomcycle.app.ui.phase.PhaseVisual
import com.bloomcycle.app.ui.rememberAppContainer
import com.bloomcycle.app.ui.theme.Bloom
import com.bloomcycle.app.ui.theme.EyebrowStyle
import com.bloomcycle.app.ui.theme.Spacing
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Today. One card that answers "where am I", one that says what is safe to promise, and
 * the two actions the app exists for: log a period, log how you feel.
 */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val container = rememberAppContainer()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val today = remember(container) { container.cycleClock.today() }

    var settings by remember { mutableStateOf<UserSettings?>(null) }
    LaunchedEffect(container) { container.settings.settings.collect { settings = it } }

    val periods by container.cycleRepository
        .observePeriods()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val symptoms by container.cycleRepository
        .observeSymptoms()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val currentSettings = settings
    val summary = remember(periods, today, currentSettings) {
        currentSettings?.let {
            HomeSummarizer.summarize(
                periods = periods,
                today = today,
                context = it.cycleContext,
                assumptions = CycleCalculator.Assumptions(defaultCycleLength = it.typicalCycleLength),
            )
        }
    }

    var periodSheetVisible by remember { mutableStateOf(false) }
    var editingPeriod by remember { mutableStateOf<PeriodEvent?>(null) }
    var symptomSheetVisible by remember { mutableStateOf(false) }

    val savePeriod: (PeriodEvent) -> Unit = { event ->
        scope.launch {
            if (event.id == 0L) {
                container.cycleRepository.logPeriod(event)
                snackbarHostState.showSnackbar("Period logged")
            } else {
                container.cycleRepository.updatePeriod(event)
                snackbarHostState.showSnackbar("Period updated")
            }
            periodSheetVisible = false
            editingPeriod = null
        }
    }

    val deletePeriod: (PeriodEvent) -> Unit = { period ->
        scope.launch {
            container.cycleRepository.deletePeriod(period)
            val result = snackbarHostState.showSnackbar(
                message = "Period deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
            )
            // The row is already gone from the list — restoring it is the undo.
            if (result == SnackbarResult.ActionPerformed) {
                container.cycleRepository.logPeriod(period)
            }
        }
    }

    val saveSymptoms: (List<SymptomLog>) -> Unit = { desired ->
        scope.launch {
            val stored = symptoms.filter { it.date == today }
            stored
                .filter { current -> desired.none { it.symptomId == current.symptomId } }
                .forEach { container.cycleRepository.deleteSymptom(it) }
            desired.forEach { container.cycleRepository.logSymptom(it) }
            symptomSheetVisible = false
            snackbarHostState.showSnackbar(
                if (desired.isEmpty()) "Symptoms cleared" else "Saved for today",
            )
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (summary == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        PageScrollSound(listState)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item(key = "heading") { TodayHeading(today) }
            item(key = "phase") { PhaseCard(summary) }
            summary.caveat?.let { caveat ->
                item(key = "caveat") { CaveatCard(caveat) }
            }
            item(key = "actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    PrimaryButton(
                        text = "Log a period",
                        onClick = {
                            editingPeriod = null
                            periodSheetVisible = true
                        },
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        text = "Symptoms",
                        onClick = { symptomSheetVisible = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            val todaySymptoms = symptoms.filter { it.date == today }
            if (todaySymptoms.isNotEmpty()) {
                item(key = "today-symptoms") { TodaySymptoms(todaySymptoms) }
            }

            item(key = "periods-header") {
                SectionHeader(title = "Your periods", eyebrow = "History")
            }
            if (periods.isEmpty()) {
                item(key = "no-periods") {
                    EmptyState(
                        title = "Nothing logged yet",
                        body = "Log the first day of your last period and the rest falls into place.",
                        glyph = "○",
                    )
                }
            } else {
                items(periods.sortedByDescending { it.startDate }, key = { it.id }) { period ->
                    PeriodRow(
                        period = period,
                        onEdit = {
                            editingPeriod = period
                            periodSheetVisible = true
                        },
                        onDelete = { deletePeriod(period) },
                    )
                }
            }
        }
    }

    if (periodSheetVisible) {
        PeriodLogSheet(
            today = today,
            periods = periods,
            existing = editingPeriod,
            onDismiss = {
                periodSheetVisible = false
                editingPeriod = null
            },
            onSave = savePeriod,
        )
    }
    if (symptomSheetVisible) {
        SymptomLogSheet(
            date = today,
            existing = symptoms.filter { it.date == today },
            onDismiss = { symptomSheetVisible = false },
            onSave = saveSymptoms,
        )
    }
}

@Composable
private fun TodayHeading(today: CycleDate) {
    Text(
        text = formatHeadingDate(today).uppercase(),
        style = EyebrowStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * The card the whole screen is built around. Colour travels with the glyph and the phase
 * word, never on its own, so it still reads in greyscale and through TalkBack.
 */
@Composable
private fun PhaseCard(summary: HomeSummary) {
    val phaseState = summary.phase
    val visual = PhaseVisual.of(phaseState?.phase)
    val tint = Bloom.phaseColor(phaseState?.phase)

    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = visual?.glyph ?: "·",
                style = MaterialTheme.typography.displayMedium,
                color = tint,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.CenterVertically),
            )
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = visual?.label ?: "Today",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = visual?.blurb ?: summary.status.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Gap()
        Hairline()
        Gap(Spacing.sm)

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            Metric(
                label = "Cycle day",
                value = summary.cycleDay?.toString() ?: "—",
                caption = phaseState?.let { "of about ${it.cycleLength} days" }.orEmpty(),
                modifier = Modifier.weight(1f),
            )
            Metric(
                label = "Next period",
                value = summary.countdown ?: "Not yet",
                caption = summary.prediction?.confidence?.label.orEmpty(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Metric(label: String, value: String, caption: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = EyebrowStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xxxs))
        Text(text = value, style = MaterialTheme.typography.titleLarge)
        if (caption.isNotEmpty()) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Shown whenever the user's context means the dates are a guess, not a reading. */
@Composable
private fun CaveatCard(caveat: String) {
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = "A ROUGH GUIDE",
            style = EyebrowStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = caveat,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TodaySymptoms(symptoms: List<SymptomLog>) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "Today", eyebrow = "How you felt")
        Gap(Spacing.xs)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            symptoms.forEach { symptom ->
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Text(
                        text = "${SymptomCatalog.label(symptom.symptomId)} · ${severityWord(symptom.severity)}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(
                            horizontal = Spacing.sm,
                            vertical = Spacing.xs,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodRow(period: PeriodEvent, onEdit: () -> Unit, onDelete: () -> Unit) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatDateRange(period.startDate, period.endDate),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = flowLabel(period),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit this period")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete this period")
            }
        }
    }
}

private fun flowLabel(period: PeriodEvent): String = when {
    period.flow != null -> period.flow.label
    period.isSpottingOnly -> "Spotting"
    else -> "No flow noted"
}
