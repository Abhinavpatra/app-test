package com.bloomcycle.app.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bloomcycle.app.domain.insights.CycleLengthSeries
import com.bloomcycle.app.domain.insights.Heatmap
import com.bloomcycle.app.domain.insights.WheelSegment
import com.bloomcycle.app.domain.model.PhaseType
import com.bloomcycle.app.domain.model.SymptomCatalog
import com.bloomcycle.app.ui.phase.PhaseVisual
import com.bloomcycle.app.ui.theme.Bloom
import com.bloomcycle.app.ui.theme.Spacing

/**
 * Four charts, all drawn by hand with Compose Canvas — no chart library, per plan.md §10.
 *
 * The graphics are `clearAndSetSemantics {}`: TalkBack skips the picture and reads the
 * caption underneath, which says the same thing in words. That caption is the accessible
 * text alternative every chart here is required to carry.
 */

private const val ChartHeight = 176

// --- Cycle length: line + predicted band --------------------------------------------------

@Composable
fun CycleLengthChart(series: CycleLengthSeries, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val bandColor = MaterialTheme.colorScheme.primary
    val frameColor = MaterialTheme.colorScheme.outlineVariant

    if (series.points.isEmpty()) {
        ChartPlaceholder(
            text = "Two logged periods are enough to draw a line.",
            modifier = modifier,
        )
        return
    }

    val bandLow = series.bandLow
    val bandHigh = series.bandHigh
    val predicted = series.predicted

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeight.dp)
            .clearAndSetSemantics {},
    ) {
        val padH = 10.dp.toPx()
        val padV = 14.dp.toPx()
        val plotW = size.width - padH * 2
        val plotH = size.height - padV * 2
        val range = (series.highest - series.lowest).coerceAtLeast(1)
        val count = series.points.size

        fun xAt(index: Int): Float = padH + when {
            count == 1 -> plotW / 2f
            else -> plotW * index / (count - 1).toFloat()
        }

        fun yAt(value: Int): Float =
            padV + plotH - (value - series.lowest) * plotH / range

        // Estimated spread, drawn as a band rather than a line: it is a range, not a date.
        if (bandLow != null && bandHigh != null) {
            val top = yAt(bandHigh)
            val bottom = yAt(bandLow)
            drawRect(
                color = bandColor.copy(alpha = 0.12f),
                topLeft = Offset(padH, top),
                size = Size(plotW, (bottom - top).coerceAtLeast(1f)),
            )
        }

        if (predicted != null) {
            val y = yAt(predicted)
            drawLine(
                color = bandColor.copy(alpha = 0.7f),
                start = Offset(padH, y),
                end = Offset(size.width - padH, y),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
            )
        }

        val path = Path()
        series.points.forEachIndexed { index, point ->
            val x = xAt(index)
            val y = yAt(point.length)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )
        series.points.forEachIndexed { index, point ->
            drawCircle(
                color = lineColor,
                radius = 3.5.dp.toPx(),
                center = Offset(xAt(index), yAt(point.length)),
            )
        }

        drawLine(
            color = frameColor,
            start = Offset(padH, size.height - padV),
            end = Offset(size.width - padH, size.height - padV),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

// --- Period durations: bars ---------------------------------------------------------------

@Composable
fun PeriodDurationChart(values: List<Int>, modifier: Modifier = Modifier) {
    val barColor = Bloom.phaseColor(PhaseType.MENSTRUAL)
    val averageColor = MaterialTheme.colorScheme.primary
    val frameColor = MaterialTheme.colorScheme.outlineVariant

    if (values.isEmpty()) {
        ChartPlaceholder(
            text = "Add an end date to a period and its length shows up here.",
            modifier = modifier,
        )
        return
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeight.dp)
            .clearAndSetSemantics {},
    ) {
        val padH = 10.dp.toPx()
        val padV = 14.dp.toPx()
        val plotW = size.width - padH * 2
        val plotH = size.height - padV * 2
        val max = values.max().coerceAtLeast(1)
        val average = values.average().toFloat()

        val slot = plotW / values.size
        val barW = (slot * 0.6f).coerceAtLeast(4f)

        values.forEachIndexed { index, value ->
            val barH = plotH * value / max
            val left = padH + slot * index + (slot - barW) / 2f
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, padV + plotH - barH),
                size = Size(barW, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
            )
        }

        val avgY = padV + plotH - plotH * average / max
        drawLine(
            color = averageColor.copy(alpha = 0.7f),
            start = Offset(padH, avgY),
            end = Offset(size.width - padH, avgY),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
        )
        drawLine(
            color = frameColor,
            start = Offset(padH, size.height - padV),
            end = Offset(size.width - padH, size.height - padV),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

// --- Phase wheel --------------------------------------------------------------------------

@Composable
fun PhaseWheel(segments: List<WheelSegment>, modifier: Modifier = Modifier) {
    if (segments.isEmpty()) {
        ChartPlaceholder(text = "Log a period to see your cycle as a ring.", modifier = modifier)
        return
    }

    val total = segments.sumOf { it.days }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val colors = segments.associate { it.phase to Bloom.phaseColor(it.phase) }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .size(168.dp)
                .clearAndSetSemantics {},
        ) {
            val stroke = 20.dp.toPx()
            val inset = stroke / 2f + 4.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            var start = -90f
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )
            segments.forEach { segment ->
                val sweep = segment.share * 360f
                drawArc(
                    color = colors.getValue(segment.phase),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
                start += sweep
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = total.toString(),
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                text = if (total == 1) "day" else "days",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        segments.forEach { segment ->
            val visual = PhaseVisual.of(segment.phase)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = visual?.glyph ?: "·",
                    style = MaterialTheme.typography.titleSmall,
                    color = Bloom.phaseColor(segment.phase),
                )
                Text(
                    text = " ${segment.days}d",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// --- Symptom heatmap ----------------------------------------------------------------------

private val LabelColumn = 92.dp
private const val CellHeight = 18

@Composable
fun SymptomHeatmapChart(heatmap: Heatmap, modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.primary

    if (heatmap.isEmpty) {
        ChartPlaceholder(
            text = "Log how you feel and it fills in by cycle day.",
            modifier = modifier,
        )
        return
    }

    val rows = heatmap.rows.size
    val columns = heatmap.cycleDays.coerceAtLeast(1)
    val labelEvery = if (columns <= 14) 5 else 10

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Column(
                modifier = Modifier
                    .width(LabelColumn)
                    .height(CellHeight.dp * rows),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                heatmap.rows.forEach { id ->
                    Text(
                        text = SymptomCatalog.label(id),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.height(CellHeight.dp),
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(CellHeight.dp * rows)
                    .clearAndSetSemantics {},
            ) {
                val cellW = size.width / columns
                val gap = 2.dp.toPx()
                heatmap.rows.forEachIndexed { rowIndex, id ->
                    for (column in 0 until columns) {
                        val count = heatmap.counts[id to (column + 1)] ?: continue
                        val alpha = if (heatmap.peak <= 1) {
                            1f
                        } else {
                            0.35f + 0.65f * count / heatmap.peak
                        }
                        drawRoundRect(
                            color = base.copy(alpha = alpha),
                            topLeft = Offset(
                                column * cellW + gap / 2,
                                rowIndex * CellHeight.dp.toPx() + gap / 2,
                            ),
                            size = Size(
                                (cellW - gap).coerceAtLeast(1f),
                                (CellHeight.dp.toPx() - gap),
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                        )
                    }
                }
            }
        }

        Row(modifier = Modifier.padding(start = LabelColumn)) {
            for (column in 0 until columns) {
                val day = column + 1
                val show = day == 1 || day % labelEvery == 0
                Text(
                    text = if (show) day.toString() else "",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Text(
            text = "Cycle day",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = LabelColumn, top = Spacing.xxxs),
        )
    }
}

// --- Shared -------------------------------------------------------------------------------

@Composable
private fun ChartPlaceholder(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ChartHeight.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.md),
        )
    }
}
