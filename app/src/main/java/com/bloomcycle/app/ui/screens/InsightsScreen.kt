package com.bloomcycle.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bloomcycle.app.R
import com.bloomcycle.app.domain.cycle.CycleCalculator
import com.bloomcycle.app.domain.insights.CycleLengthSeries
import com.bloomcycle.app.domain.insights.Heatmap
import com.bloomcycle.app.domain.insights.InsightsAnalysis
import com.bloomcycle.app.domain.insights.InsightsReport
import com.bloomcycle.app.domain.insights.Trend
import com.bloomcycle.app.domain.insights.WheelSegment
import com.bloomcycle.app.domain.insights.cycleLengthSeries
import com.bloomcycle.app.domain.insights.periodDurationSeries
import com.bloomcycle.app.domain.insights.phaseWheel
import com.bloomcycle.app.domain.insights.symptomHeatmap
import com.bloomcycle.app.domain.model.CycleContext
import com.bloomcycle.app.domain.model.CyclePrediction
import com.bloomcycle.app.domain.model.PremiumFeature
import com.bloomcycle.app.domain.model.UserSettings
import com.bloomcycle.app.core.time.CycleDate
import com.bloomcycle.app.ui.LocalNavController
import com.bloomcycle.app.ui.charts.CycleLengthChart
import com.bloomcycle.app.ui.charts.PeriodDurationChart
import com.bloomcycle.app.ui.charts.PhaseWheel
import com.bloomcycle.app.ui.charts.SymptomHeatmapChart
import com.bloomcycle.app.ui.components.Gap
import com.bloomcycle.app.ui.components.PremiumBadge
import com.bloomcycle.app.ui.components.PremiumGate
import com.bloomcycle.app.ui.components.SectionHeader
import com.bloomcycle.app.ui.components.SoftCard
import com.bloomcycle.app.ui.format.formatDate
import com.bloomcycle.app.ui.format.formatHeadingDate
import com.bloomcycle.app.ui.navigation.BloomDestination
import com.bloomcycle.app.ui.phase.PhaseVisual
import com.bloomcycle.app.ui.rememberAppContainer
import com.bloomcycle.app.ui.theme.ArticleStyle
import com.bloomcycle.app.ui.theme.EyebrowStyle
import com.bloomcycle.app.ui.theme.Spacing
import com.bloomcycle.app.ui.theme.Bloom
import com.bloomcycle.app.domain.model.PhaseType
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collect

/**
 * Phase 7. Charts are hand-drawn Compose Canvas — no chart library — and every one carries a
 * sentence underneath saying the same thing in words: that sentence is the accessible
 * alternative, and for the two advanced readings it is also the free-tier preview.
 *
 * Free tier keeps the factual charts (lengths, durations, symptoms, consistency). The
 * interpretive layer — the ring and the written reading — sits behind [PremiumFeature.ANALYSIS].
 */
@Composable
fun InsightsScreen(modifier: Modifier = Modifier) {
    val container = rememberAppContainer()
    val today = remember(container) { container.cycleClock.today() }

    var settings by remember { mutableStateOf<UserSettings?>(null) }
    LaunchedEffect(container) { container.settings.settings.collect { settings = it } }

    val periods by container.cycleRepository
        .observePeriods()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val symptoms by container.cycleRepository
        .observeSymptoms()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val entitlements by container.entitlements
        .state
        .collectAsStateWithLifecycle(initialValue = null)

    val current = settings
    val cycles = remember(periods) { CycleCalculator.buildCycles(periods) }
    val assumptions = remember(current) {
        CycleCalculator.Assumptions(
            defaultCycleLength = current?.typicalCycleLength
                ?: CycleCalculator.DEFAULT_CYCLE_LENGTH,
        )
    }
    val prediction = remember(periods, today, current) {
        current?.let { CycleCalculator.predict(cycles, today, it.cycleContext, assumptions) }
    }
    val report = remember(periods, symptoms, current) {
        InsightsAnalysis.analyze(
            periods = periods,
            symptoms = symptoms,
            context = current?.cycleContext ?: CycleContext.NONE,
            assumptions = assumptions,
        )
    }
    val lengths = remember(cycles, prediction) { cycleLengthSeries(cycles, prediction) }
    val durations = remember(cycles) { periodDurationSeries(cycles) }
    val heatmap = remember(symptoms, cycles) { symptomHeatmap(symptoms, cycles) }
    val wheel = remember(cycles, prediction) { phaseWheel(cycles, prediction) }

    val analysisUnlocked = entitlements?.isUnlocked(PremiumFeature.ANALYSIS) == true
    val longHorizonUnlocked = entitlements?.isUnlocked(PremiumFeature.LONG_HORIZON) == true
    // Locked charts open the paywall (Phase 9) instead of unlocking themselves, so the
    // price, the wording and the "tracking stays free" line live in exactly one place.
    val navController = LocalNavController.current
    val openPaywall: () -> Unit = { navController?.navigate(BloomDestination.PAYWALL) }

    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Gap(Spacing.xs)
            SectionHeader(title = "Patterns over time", eyebrow = "Insights")
            Text(
                text = formatHeadingDate(today),
                style = EyebrowStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!report.hasHistory) {
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = report.status.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Gap(Spacing.xs)
                    Text(
                        text = "Log two periods and this screen starts drawing what you have " +
                            "given it. Nothing is estimated until there is something to " +
                            "estimate from.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                ChartCard(
                    title = "Cycle length",
                    eyebrow = "History",
                    caption = lengthCaption(report, lengths),
                ) {
                    CycleLengthChart(series = lengths)
                }

                ConsistencyCard(report = report)

                ChartCard(
                    title = "Period durations",
                    caption = durationCaption(durations),
                ) {
                    PeriodDurationChart(values = durations)
                }

                ChartCard(
                    title = "Symptoms by cycle day",
                    eyebrow = "What tends to show up when",
                    caption = heatmapCaption(heatmap),
                ) {
                    SymptomHeatmapChart(heatmap = heatmap)
                }

                ChartCard(
                    title = "Your cycle as a ring",
                    caption = wheelCaption(wheel),
                    locked = !analysisUnlocked,
                ) {
                    PremiumGate(
                        feature = PremiumFeature.ANALYSIS,
                        unlocked = analysisUnlocked,
                        onOpenPaywall = openPaywall,
                    ) {
                        PhaseWheel(segments = wheel)
                    }
                }

                ChartCard(
                    title = "What your pattern says",
                    caption = if (analysisUnlocked) {
                        report.summary
                    } else {
                        teaserLine(report.summary)
                    },
                    locked = !analysisUnlocked,
                ) {
                    PremiumGate(
                        feature = PremiumFeature.ANALYSIS,
                        unlocked = analysisUnlocked,
                        onOpenPaywall = openPaywall,
                    ) {
                        Text(text = report.summary, style = ArticleStyle)
                    }
                }

                prediction?.let { predicted ->
                    ChartCard(
                        title = "Six months ahead",
                        eyebrow = "Projection",
                        caption = if (longHorizonUnlocked) {
                            projectionCaption(predicted)
                        } else {
                            "Projected period starts for the next six months, spaced by your " +
                                "own average."
                        },
                        locked = !longHorizonUnlocked,
                    ) {
                        PremiumGate(
                            feature = PremiumFeature.LONG_HORIZON,
                            unlocked = longHorizonUnlocked,
                            onOpenPaywall = openPaywall,
                        ) {
                            ProjectionList(
                                dates = predicted.projectNext(months = 6),
                                cycleLength = predicted.averageCycleLength,
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.disclaimer_health),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// --- Cards --------------------------------------------------------------------------------

@Composable
private fun ChartCard(
    title: String,
    caption: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    locked: Boolean = false,
    content: @Composable () -> Unit,
) {
    SoftCard(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = title,
            eyebrow = eyebrow,
            trailing = if (locked) {
                { PremiumBadge() }
            } else {
                null
            },
        )
        Gap(Spacing.sm)
        content()
        Gap(Spacing.sm)
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The number people actually want, with the honest label beside it. */
@Composable
private fun ConsistencyCard(report: InsightsReport) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "Consistency", eyebrow = "Score")
        Gap(Spacing.sm)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = report.consistencyScore.toString(),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Light,
                color = Bloom.phaseColor(PhaseType.FOLLICULAR),
            )
            Spacer(Modifier.width(Spacing.sm))
            Column {
                Text(
                    text = report.regularityLabel,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = when (report.trend) {
                        Trend.UNKNOWN -> "Log a few more cycles to see which way they drift."
                        else -> "${report.trend.label}${trendSuffix(report.trendDays)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Gap(Spacing.sm)
        Text(
            text = consistencyCaption(report),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun trendSuffix(days: Int): String = if (days == 0) "" else " — about $days days"

private fun consistencyCaption(report: InsightsReport): String = when {
    report.consistencyScore == 0 -> "Not enough cycles yet to score: three is the minimum."
    report.consistencyScore >= 80 -> "Your cycle lengths sit close together."
    report.consistencyScore >= 50 -> "Your cycle lengths move around a little."
    else -> "Your cycle lengths vary a lot right now."
}

// --- Captions: the text alternative under every chart --------------------------------------

private fun lengthCaption(report: InsightsReport, series: CycleLengthSeries): String {
    if (!report.hasHistory) return report.status.orEmpty()
    val head = "Your last ${report.cycleCount} cycles averaged ${report.averageLength} days, " +
        "running from ${report.shortest} to ${report.longest} days."
    val band = if (series.predicted != null && series.bandLow != null && series.bandHigh != null) {
        " Next about ${series.predicted} days, somewhere between ${series.bandLow} and " +
            "${series.bandHigh}."
    } else {
        ""
    }
    return head + band
}

private fun durationCaption(values: List<Int>): String = when {
    values.isEmpty() -> "No period has an end date logged yet."
    else -> "Periods ran ${values.min()} to ${values.max()} days, usually " +
        "${values.average().roundToInt()} days."
}

private fun heatmapCaption(heatmap: Heatmap): String {
    if (heatmap.isEmpty) return "Nothing logged to place on the grid yet."

    val perDay = HashMap<Int, Int>()
    heatmap.counts.forEach { (key, count) ->
        perDay[key.second] = (perDay[key.second] ?: 0) + count
    }
    val topDay = perDay.maxByOrNull { it.value }?.key
    val kinds = heatmap.rows.size
    return "${heatmap.totalLogged} symptom logs across $kinds " +
        (if (kinds == 1) "kind" else "kinds") + ", most often on cycle day $topDay. " +
        "Columns are days of the cycle, rows are what you logged."
}

private fun wheelCaption(segments: List<WheelSegment>): String {
    if (segments.isEmpty()) return "Log a period and the ring splits into your four phases."
    return segments.joinToString(" · ") { segment ->
        "${PhaseVisual.of(segment.phase)?.label ?: segment.phase.label} ${segment.days}d"
    }
}

/** The free-tier preview: one sentence, never the whole reading. */
private fun teaserLine(summary: String): String {
    val sentence = summary.substringBefore(". ").trim()
    return if (sentence.isEmpty()) summary else "$sentence."
}

// --- Six months ahead (Phase 8, PremiumFeature.LONG_HORIZON) ------------------------------

private fun projectionCaption(prediction: CyclePrediction): String {
    val dates = prediction.projectNext(months = 6)
    if (dates.isEmpty()) return ""
    return "Six starts, ${formatDate(dates.first())} through ${formatDate(dates.last())}, " +
        "spaced by your ${prediction.averageCycleLength}-day average. Estimates, not dates — " +
        "log a period and they move."
}

@Composable
private fun ProjectionList(dates: List<CycleDate>, cycleLength: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        dates.forEachIndexed { index, date ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (index == 0) "Next" else "+${cycleLength}d",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(56.dp),
                )
                Text(
                    text = formatDate(date),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
