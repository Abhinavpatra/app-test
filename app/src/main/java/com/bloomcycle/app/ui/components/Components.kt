package com.bloomcycle.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bloomcycle.app.domain.model.PhaseType
import com.bloomcycle.app.ui.phase.PhaseVisual
import com.bloomcycle.app.ui.theme.Bloom
import com.bloomcycle.app.ui.theme.EyebrowStyle
import com.bloomcycle.app.ui.theme.Spacing

/**
 * The five components everything else is assembled from. Keeping them here is what stops
 * each new screen from quietly inventing its own card corner, chip height or heading size.
 */

// --- SoftCard ---------------------------------------------------------------------------

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Spacing.md),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = border,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun SoftCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(Spacing.md),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

// --- PhaseChip --------------------------------------------------------------------------

/**
 * Colour never travels alone: the chip always carries the phase word beside its glyph, so
 * it survives greyscale printing, colour-vision deficiency and TalkBack.
 */
@Composable
fun PhaseChip(
    phase: PhaseType?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val visual = PhaseVisual.of(phase)
    val tint = Bloom.phaseColor(phase)
    val label = visual?.label ?: "No phase yet"

    Surface(
        modifier = modifier.semantics { contentDescription = "Phase: $label" },
        shape = MaterialTheme.shapes.extraLarge,
        color = tint.copy(alpha = 0.14f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) Spacing.xs else Spacing.sm,
                vertical = if (compact) Spacing.xxs else Spacing.xs,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = visual?.glyph ?: "·",
                style = MaterialTheme.typography.titleMedium,
                color = tint,
            )
            Spacer(Modifier.width(Spacing.xs))
            Text(
                text = label,
                style = if (compact) {
                    MaterialTheme.typography.labelSmall
                } else {
                    MaterialTheme.typography.labelLarge
                },
            )
        }
    }
}

// --- PrimaryButton ----------------------------------------------------------------------

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 52.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xs),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

/** The quiet counterpart to [PrimaryButton] — same height, so pairing them in a row lines up. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 52.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xs),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

// --- SectionHeader ----------------------------------------------------------------------

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            if (eyebrow != null) {
                Text(
                    text = eyebrow.uppercase(),
                    style = EyebrowStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.xxs))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(Spacing.sm))
            trailing()
        }
    }
}

// --- EmptyState -------------------------------------------------------------------------

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    glyph: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (glyph != null) {
            Text(
                text = glyph,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.md))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (action != null) {
            Spacer(Modifier.height(Spacing.lg))
            action()
        }
    }
}

// --- Divider ----------------------------------------------------------------------------

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/** Vertical gap sized from the shared scale. */
@Composable
fun Gap(size: Dp = Spacing.md) {
    Spacer(Modifier.height(size))
}

// --- CycleContextRow --------------------------------------------------------------------

/**
 * The picker for [com.bloomcycle.app.domain.model.CycleContext] — onboarding and settings
 * ask the same question, so they share the row rather than drifting apart.
 *
 * Colour never carries the choice alone: the row shows a radio, the label and, when
 * selected, the one line that says what picking it changes.
 */
@Composable
fun CycleContextRow(
    option: com.bloomcycle.app.domain.model.CycleContext,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onSelect,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            androidx.compose.material3.RadioButton(selected = selected, onClick = onSelect)
            Column {
                Text(option.label, style = MaterialTheme.typography.titleMedium)
                if (selected) {
                    Gap(Spacing.xxxs)
                    Text(
                        text = option.blurb,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
