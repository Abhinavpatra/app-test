package com.bloomcycle.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.bloomcycle.app.core.time.CycleDate
import com.bloomcycle.app.domain.model.SymptomCatalog
import com.bloomcycle.app.domain.model.SymptomCategory
import com.bloomcycle.app.domain.model.SymptomLog
import com.bloomcycle.app.ui.components.Gap
import com.bloomcycle.app.ui.components.Hairline
import com.bloomcycle.app.ui.components.PrimaryButton
import com.bloomcycle.app.ui.components.SecondaryButton
import com.bloomcycle.app.ui.components.SectionHeader
import com.bloomcycle.app.ui.format.formatDate
import com.bloomcycle.app.ui.theme.Spacing
import kotlin.math.roundToInt

/**
 * Spoken severity, so a slider reads as "Strong" rather than "3 of 5". Shared with the
 * Today card, which has to label symptoms the same way.
 */
internal fun severityWord(severity: Int): String =
    listOf("Mild", "Noticed", "Moderate", "Strong", "Severe")[(severity.coerceIn(1, 5)) - 1]

/**
 * Multi-select symptom entry for one day. Emits the complete desired list rather than
 * individual add/remove calls, so the caller can reconcile against what is already stored
 * and the sheet never has to know how the repository works.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomLogSheet(
    date: CycleDate,
    existing: List<SymptomLog>,
    onDismiss: () -> Unit,
    onSave: (List<SymptomLog>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selected = remember {
        mutableStateMapOf<String, Int>().apply {
            existing.forEach { put(it.symptomId, it.severity.coerceIn(1, 5)) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen)
                .padding(bottom = Spacing.lg)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SectionHeader(title = "How are you feeling?", eyebrow = formatDate(date))
            Text(
                text = "Tap everything that applies. Nothing is required.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Gap(Spacing.xxs)

            SymptomCategory.entries.forEach { category ->
                val definitions = SymptomCatalog.forCategory(category)
                if (definitions.isEmpty()) return@forEach

                Text(
                    text = category.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    definitions.forEach { definition ->
                        FilterChip(
                            selected = selected.containsKey(definition.id),
                            onClick = {
                                if (selected.containsKey(definition.id)) {
                                    selected.remove(definition.id)
                                } else {
                                    selected[definition.id] = 3
                                }
                            },
                            label = { Text(definition.label) },
                        )
                    }
                }
            }

            if (selected.isNotEmpty()) {
                Gap(Spacing.xs)
                Hairline()
                Gap(Spacing.xs)
                SectionHeader(title = "How strong?", eyebrow = "Selected")

                SymptomCatalog.values
                    .filter { selected.containsKey(it.id) }
                    .forEach { definition ->
                        val severity = selected.getValue(definition.id)
                        val word = severityWord(severity)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    contentDescription = "${definition.label}, $word"
                                },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(definition.label, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = word,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Slider(
                                value = severity.toFloat(),
                                onValueChange = { selected[definition.id] = it.roundToInt() },
                                valueRange = 1f..5f,
                                steps = 3,
                            )
                        }
                    }
            }

            Gap(Spacing.xs)
            PrimaryButton(
                text = "Save",
                onClick = {
                    onSave(
                        SymptomCatalog.values
                            .filter { selected.containsKey(it.id) }
                            .map { definition ->
                                SymptomLog(
                                    id = existing.find { it.symptomId == definition.id }?.id ?: 0L,
                                    date = date,
                                    symptomId = definition.id,
                                    severity = selected.getValue(definition.id),
                                )
                            },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
