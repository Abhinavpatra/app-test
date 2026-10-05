package com.bloomcycle.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bloomcycle.app.R
import com.bloomcycle.app.core.time.CycleDate
import com.bloomcycle.app.domain.calendar.CalendarModel
import com.bloomcycle.app.domain.calendar.DayMark
import com.bloomcycle.app.domain.calendar.DayMarks
import com.bloomcycle.app.domain.cycle.CycleCalculator
import com.bloomcycle.app.domain.model.PeriodEvent
import com.bloomcycle.app.domain.model.PhaseType
import com.bloomcycle.app.domain.model.SymptomCatalog
import com.bloomcycle.app.domain.model.SymptomLog
import com.bloomcycle.app.domain.model.UserSettings
import com.bloomcycle.app.ui.components.Gap
import com.bloomcycle.app.ui.components.Hairline
import com.bloomcycle.app.ui.components.PhaseChip
import com.bloomcycle.app.ui.components.PrimaryButton
import com.bloomcycle.app.ui.components.SecondaryButton
import com.bloomcycle.app.ui.components.SectionHeader
import com.bloomcycle.app.ui.components.SoftCard
import com.bloomcycle.app.ui.format.formatDate
import com.bloomcycle.app.ui.format.formatHeadingDate
import com.bloomcycle.app.ui.format.speakDate
import com.bloomcycle.app.ui.phase.PhaseVisual
import com.bloomcycle.app.ui.rememberAppContainer
import com.bloomcycle.app.ui.theme.Bloom
import com.bloomcycle.app.ui.theme.EyebrowStyle
import com.bloomcycle.app.ui.theme.Spacing
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * The month view. One definition of a day ([CalendarModel]) feeds the grid, the legend and
 * the day sheet, so the shapes drawn here and the words shown when you tap one always agree.
 *
 * Nothing in this screen is colour-only: a logged day is a filled disc, a predicted day is a
 * ring, a fertile day carries dots, today is outlined, and an unusual cycle length gets a
 * corner dot. The legend says all of it in words, and so does the day sheet.
 */
@Composable
fun CalendarScreen(modifier: Modifier = Modifier) {
    val container = rememberAppContainer()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
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
    val cycles = remember(periods) { CycleCalculator.buildCycles(periods) }
    val assumptions = remember(currentSettings) {
        CycleCalculator.Assumptions(
            defaultCycleLength = currentSettings?.typicalCycleLength
                ?: CycleCalculator.DEFAULT_CYCLE_LENGTH,
        )
    }
    val prediction = remember(periods, today, currentSettings) {
        currentSettings?.let {
            CycleCalculator.predict(cycles, today, it.cycleContext, assumptions)
        }
    }

    val firstDay = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }
    val months = remember(today) { CalendarModel.monthsAround(today, back = 13, forward = 13) }
    val pagerState = rememberPagerState(initialPage = months.indexOf(YearMonth.from(today))) {
        months.size
    }
    val currentMonth = months.getOrNull(pagerState.currentPage) ?: YearMonth.from(today)

    val monthMarks = remember(currentMonth, periods, cycles, prediction, today, firstDay) {
        CalendarModel.monthMarks(currentMonth, periods, cycles, prediction, today, firstDay)
    }
    val yearMarks = remember(currentMonth.year, periods, cycles, prediction, today) {
        CalendarModel.monthsMarkedIn(currentMonth.year, periods, cycles, prediction, today)
    }

    var selectedDay by remember { mutableStateOf<CycleDate?>(null) }
    var periodSheetDay by remember { mutableStateOf<CycleDate?>(null) }
    var editingPeriod by remember { mutableStateOf<PeriodEvent?>(null) }
    var symptomSheetDay by remember { mutableStateOf<CycleDate?>(null) }
    var legendVisible by remember { mutableStateOf(false) }

    val savePeriod: (PeriodEvent) -> Unit = { event ->
        scope.launch {
            if (event.id == 0L) {
                container.cycleRepository.logPeriod(event)
                snackbarHostState.showSnackbar("Period logged")
            } else {
                container.cycleRepository.updatePeriod(event)
                snackbarHostState.showSnackbar("Period updated")
            }
            periodSheetDay = null
            editingPeriod = null
        }
    }

    val saveSymptoms: (List<SymptomLog>) -> Unit = { desired ->
        scope.launch {
            val date = symptomSheetDay ?: today
            val stored = symptoms.filter { it.date == date }
            stored
                .filter { current -> desired.none { it.symptomId == current.symptomId } }
                .forEach { container.cycleRepository.deleteSymptom(it) }
            desired.forEach { container.cycleRepository.logSymptom(it) }
            symptomSheetDay = null
            snackbarHostState.showSnackbar(
                if (desired.isEmpty()) "Symptoms cleared" else "Saved for ${formatDate(date)}",
            )
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
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
            SectionHeader(
                title = monthTitle(currentMonth),
                eyebrow = "Calendar",
                trailing = {
                    Row {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        (pagerState.currentPage - 1).coerceAtLeast(0),
                                    )
                                }
                            },
                        ) {
                            Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous month")
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        (pagerState.currentPage + 1)
                                            .coerceAtMost(months.lastIndex),
                                    )
                                }
                            },
                        ) {
                            Icon(Icons.Outlined.ChevronRight, contentDescription = "Next month")
                        }
                    }
                },
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cellWidth = maxWidth / CELLS_WIDE
                val cellHeight = cellWidth + Spacing.xxs
                Column {
                    WeekdayRow(firstDay = firstDay)
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(cellHeight * WEEKS_SHOWN),
                        verticalAlignment = Alignment.Top,
                    ) { page ->
                        // Derived from the page, not from the settled month, so the
                        // neighbour you are swiping towards is already drawn correctly.
                        val month = months[page]
                        val pageMatrix = remember(month, firstDay) {
                            val cells = CalendarModel.monthMatrix(month, firstDay)
                            cells + List(CELLS_WIDE * WEEKS_SHOWN - cells.size) { null }
                        }
                        val pageMarks = remember(month, periods, cycles, prediction, today, firstDay) {
                            CalendarModel.monthMarks(month, periods, cycles, prediction, today, firstDay)
                        }
                        MonthGrid(
                            matrix = pageMatrix,
                            marks = pageMarks,
                            onSelect = { selectedDay = it },
                        )
                    }
                }
            }

            YearStrip(
                year = currentMonth.year,
                currentMonth = currentMonth,
                marked = yearMarks,
                onMonth = { month ->
                    val index = months.indexOf(month)
                    if (index >= 0) scope.launch { pagerState.animateScrollToPage(index) }
                },
            )

            if (periods.isEmpty()) {
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Nothing logged yet", style = MaterialTheme.typography.titleMedium)
                    Gap(Spacing.xs)
                    Text(
                        text = "Log the first day of your last period and the months below fill " +
                            "in with what you logged and what is likely to come.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            LegendCard(visible = legendVisible, onToggle = { legendVisible = !legendVisible })

            Text(
                text = stringResource(R.string.disclaimer_health),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    selectedDay?.let { day ->
        DayDetailSheet(
            date = day,
            today = today,
            marks = monthMarks[day] ?: CalendarModel.dayMarks(day, periods, cycles, prediction, today),
            symptoms = symptoms.filter { it.date == day },
            covering = periods.firstOrNull { CalendarModel.covers(it, day, today) },
            onDismiss = { selectedDay = null },
            onLogPeriod = {
                selectedDay = null
                periodSheetDay = it
            },
            onEditPeriod = { period ->
                selectedDay = null
                editingPeriod = period
                periodSheetDay = period.startDate
            },
            onLogSymptoms = {
                selectedDay = null
                symptomSheetDay = it
            },
        )
    }

    periodSheetDay?.let { day ->
        PeriodLogSheet(
            today = today,
            periods = periods,
            existing = editingPeriod,
            initialStartDate = day,
            onDismiss = {
                periodSheetDay = null
                editingPeriod = null
            },
            onSave = savePeriod,
        )
    }

    symptomSheetDay?.let { day ->
        SymptomLogSheet(
            date = day,
            existing = symptoms.filter { it.date == day },
            onDismiss = { symptomSheetDay = null },
            onSave = saveSymptoms,
        )
    }
}

private const val CELLS_WIDE = 7
private const val WEEKS_SHOWN = 6

// --- Grid ---------------------------------------------------------------------------------

@Composable
private fun WeekdayRow(firstDay: DayOfWeek, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        repeat(CELLS_WIDE) { offset ->
            val day = firstDay.plus(offset.toLong())
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MonthGrid(
    matrix: List<CycleDate?>,
    marks: Map<CycleDate, DayMarks>,
    onSelect: (CycleDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        matrix.chunked(CELLS_WIDE).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                week.forEach { date ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (date != null) {
                            DayCell(
                                marks = marks[date] ?: DayMarks(date),
                                onClick = { onSelect(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * One day. Precedence is shape-first: a filled disc beats a ring, a ring beats nothing, and
 * the extra signals (fertile dots, today's frame, the corner dot) sit around them.
 */
@Composable
private fun DayCell(marks: DayMarks, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.small
    val logged = marks.loggedPeriod
    val loggedColor = Bloom.phaseColor(PhaseType.MENSTRUAL)
    val predictedColor = MaterialTheme.colorScheme.primary
    val fertileColor = Bloom.phaseColor(PhaseType.FOLLICULAR)

    val description = buildString {
        append(speakDate(marks.date))
        DayMark.entries.filter { marks.carries(it) }.forEach {
            append(", ")
            append(it.spoken())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(if (logged) loggedColor else Color.Transparent, shape)
            .then(
                if (marks.today) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f), shape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                if (!logged && marks.predictedPeriod) {
                    Box(
                        modifier = Modifier
                            .size(NumberSize)
                            .border(1.5.dp, predictedColor, CircleShape),
                    )
                }
                Text(
                    text = marks.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (logged || marks.predictedPeriod) FontWeight.Bold else null,
                    color = if (logged) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
            if (marks.fertileWindow) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .background(fertileColor, CircleShape),
                        )
                    }
                }
            }
        }

        if (marks.outsideTypicalRange) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .size(5.dp)
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape),
            )
        }
    }
}

private val NumberSize = 30.dp

// --- Year overview ------------------------------------------------------------------------

@Composable
private fun YearStrip(
    year: Int,
    currentMonth: YearMonth,
    marked: Map<YearMonth, Set<DayMark>>,
    onMonth: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.sm),
    ) {
        Text(
            text = "$year AT A GLANCE",
            style = EyebrowStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Gap(Spacing.xs)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
        ) {
            (1..12).forEach { monthValue ->
                val month = YearMonth.of(year, monthValue)
                val isCurrent = month == currentMonth
                val marks = marked[month].orEmpty()
                Surface(
                    onClick = { onMonth(month) },
                    shape = MaterialTheme.shapes.small,
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    border = if (isCurrent) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        null
                    },
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = month.month
                                .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                                .uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            YearDot(
                                filled = DayMark.LOGGED_PERIOD in marks,
                                color = Bloom.phaseColor(PhaseType.MENSTRUAL),
                            )
                            YearDot(
                                filled = DayMark.PREDICTED_PERIOD in marks,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            YearDot(
                                filled = DayMark.FERTILE_WINDOW in marks,
                                color = Bloom.phaseColor(PhaseType.FOLLICULAR),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YearDot(filled: Boolean, color: Color) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .then(
                if (filled) {
                    Modifier.background(color, CircleShape)
                } else {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                },
            ),
    )
}

// --- Legend -------------------------------------------------------------------------------

@Composable
private fun LegendCard(visible: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    SoftCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Why am I seeing this?",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onToggle) {
                Text(if (visible) "Hide" else "Explain")
            }
        }

        AnimatedVisibility(visible = visible) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Gap(Spacing.xs)
                DayMark.entries.forEach { mark ->
                    Row(verticalAlignment = Alignment.Top) {
                        MarkSample(
                            mark = mark,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        Column {
                            Text(mark.title(), style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = mark.explanation(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The legend's sample swatch — the same shapes the grid draws, at a fixed size. */
@Composable
private fun MarkSample(mark: DayMark, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (mark) {
            DayMark.LOGGED_PERIOD -> Box(
                modifier = Modifier
                    .size(NumberSize)
                    .background(Bloom.phaseColor(PhaseType.MENSTRUAL), CircleShape),
            )

            DayMark.PREDICTED_PERIOD -> Box(
                modifier = Modifier
                    .size(NumberSize)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
            )

            DayMark.FERTILE_WINDOW -> Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(Bloom.phaseColor(PhaseType.FOLLICULAR), CircleShape),
                    )
                }
            }

            DayMark.TODAY -> Box(
                modifier = Modifier
                    .size(NumberSize)
                    .border(
                        1.5.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        RoundedCornerShape(8.dp),
                    ),
            )

            DayMark.OUTSIDE_TYPICAL_RANGE -> Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape),
            )
        }
    }
}

private fun DayMark.title(): String = when (this) {
    DayMark.LOGGED_PERIOD -> "Period days you logged"
    DayMark.PREDICTED_PERIOD -> "Estimated arrival window"
    DayMark.FERTILE_WINDOW -> "Fertile window estimate"
    DayMark.TODAY -> "Today"
    DayMark.OUTSIDE_TYPICAL_RANGE -> "A cycle outside the usual band"
}

private fun DayMark.explanation(): String = when (this) {
    DayMark.LOGGED_PERIOD ->
        "A filled disc is a day you logged as bleeding, including a period still going."

    DayMark.PREDICTED_PERIOD ->
        "A ring marks the window your recent cycles point to — earliest through latest. " +
            "It is an estimate, never a date."

    DayMark.FERTILE_WINDOW ->
        "Three dots mark the most fertile days of the estimate. Useful to know; it is not " +
            "contraception and not a pregnancy test."

    DayMark.TODAY ->
        "The outlined square is the day you are looking at."

    DayMark.OUTSIDE_TYPICAL_RANGE ->
        "A small dot means that cycle ran shorter than 21 or longer than 35 days. Common, " +
            "worth noticing, nothing alarming."
}

private fun DayMark.spoken(): String = when (this) {
    DayMark.LOGGED_PERIOD -> "a period day you logged"
    DayMark.PREDICTED_PERIOD -> "inside the estimated arrival window"
    DayMark.FERTILE_WINDOW -> "in the fertile window estimate"
    DayMark.TODAY -> "today"
    DayMark.OUTSIDE_TYPICAL_RANGE -> "in a cycle outside the typical range"
}

// --- Day detail sheet ---------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailSheet(
    date: CycleDate,
    today: CycleDate,
    marks: DayMarks,
    symptoms: List<SymptomLog>,
    covering: PeriodEvent?,
    onDismiss: () -> Unit,
    onLogPeriod: (CycleDate) -> Unit,
    onEditPeriod: (PeriodEvent) -> Unit,
    onLogSymptoms: (CycleDate) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val carried = DayMark.entries.filter { marks.carries(it) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.screen)
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SectionHeader(title = formatHeadingDate(date), eyebrow = "Day detail")

            if (carried.isEmpty() && marks.phase == null && symptoms.isEmpty()) {
                Text(
                    text = "Nothing logged or expected on this day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            carried.forEach { mark ->
                Row(verticalAlignment = Alignment.Top) {
                    MarkSample(mark = mark, modifier = Modifier.padding(top = 2.dp))
                    Spacer(Modifier.width(Spacing.sm))
                    Column {
                        Text(mark.title(), style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = mark.explanation(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (marks.phase != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PhaseChip(phase = marks.phase)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = PhaseVisual.of(marks.phase)?.blurb.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (symptoms.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    symptoms.forEach { log ->
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                text = SymptomCatalog.label(log.symptomId),
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

            Hairline()

            if (date.isAfter(today)) {
                Text(
                    text = "That day has not happened yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (covering != null) {
                SecondaryButton(
                    text = "Edit the period covering this day",
                    onClick = { onEditPeriod(covering) },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                PrimaryButton(
                    text = "Log a period on this day",
                    onClick = { onLogPeriod(date) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SecondaryButton(
                text = "How I felt on this day",
                onClick = { onLogSymptoms(date) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun monthTitle(month: YearMonth): String =
    month.month
        .getDisplayName(TextStyle.FULL, Locale.getDefault())
        .replaceFirstChar { it.uppercaseChar() } + " " + month.year
