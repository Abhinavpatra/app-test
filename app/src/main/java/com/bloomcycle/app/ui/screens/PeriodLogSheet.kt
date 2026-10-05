package com.bloomcycle.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bloomcycle.app.core.time.CycleDate
import java.time.Instant
import com.bloomcycle.app.domain.model.FlowLevel
import com.bloomcycle.app.domain.model.PeriodEvent
import com.bloomcycle.app.domain.validation.PeriodEntryRules
import com.bloomcycle.app.ui.components.DateField
import com.bloomcycle.app.ui.components.Gap
import com.bloomcycle.app.ui.components.PrimaryButton
import com.bloomcycle.app.ui.components.SecondaryButton
import com.bloomcycle.app.ui.components.SectionHeader
import com.bloomcycle.app.ui.format.formatDate
import com.bloomcycle.app.ui.format.toCycleDate
import com.bloomcycle.app.ui.format.toPickerMillis
import com.bloomcycle.app.ui.theme.Spacing

private enum class DatePick { NONE, START, END }

/**
 * The one way to add or change a period. Used for both: passing an existing event prefills
 * the fields, and saving is a single callback so the screen owns the write and the undo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodLogSheet(
    today: CycleDate,
    periods: List<PeriodEvent>,
    onDismiss: () -> Unit,
    onSave: (PeriodEvent) -> Unit,
    existing: PeriodEvent? = null,
    initialStartDate: CycleDate = today,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var startDate by remember { mutableStateOf(existing?.startDate ?: initialStartDate) }
    var endDate by remember { mutableStateOf(existing?.endDate) }
    var flow by remember { mutableStateOf(existing?.flow) }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var picking by remember { mutableStateOf(DatePick.NONE) }

    val error = PeriodEntryRules.validate(
        startDate = startDate,
        endDate = endDate,
        today = today,
        existing = periods,
        editingId = existing?.id ?: 0L,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.screen)
                .padding(bottom = Spacing.lg)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SectionHeader(
                title = if (existing == null) "Log a period" else "Edit period",
                eyebrow = if (existing == null) "New entry" else "Editing",
            )
            Gap(Spacing.xxs)

            DateField(
                label = "Started",
                value = startDate,
                placeholder = null,
                onClick = { picking = DatePick.START },
            )
            DateField(
                label = "Ended",
                value = endDate,
                placeholder = "Still going",
                onClick = { picking = DatePick.END },
            )
            if (endDate != null) {
                TextButton(onClick = { endDate = null }, modifier = Modifier.align(Alignment.End)) {
                    Text("Clear end date")
                }
            }

            Text("How heavy was it?", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                FlowLevel.entries.forEach { level ->
                    FilterChip(
                        selected = flow == level,
                        onClick = { flow = if (flow == level) null else level },
                        label = { Text(level.label) },
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notes (optional)") },
                singleLine = true,
            )

            if (error != null) {
                Text(
                    text = error.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Gap(Spacing.xxs)
            PrimaryButton(
                text = "Save",
                onClick = {
                    onSave(
                        PeriodEvent(
                            id = existing?.id ?: 0L,
                            startDate = startDate,
                            endDate = endDate,
                            flow = flow,
                            isSpottingOnly = flow == FlowLevel.SPOTTING,
                            notes = notes.trim().ifEmpty { null },
                            createdAt = existing?.createdAt ?: Instant.EPOCH,
                            updatedAt = existing?.updatedAt ?: Instant.EPOCH,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = error == null,
            )
            SecondaryButton(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (picking != DatePick.NONE) {
        val initial = when (picking) {
            DatePick.START -> startDate
            DatePick.END -> endDate ?: startDate
            DatePick.NONE -> startDate
        }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initial.toPickerMillis())
        DatePickerDialog(
            onDismissRequest = { picking = DatePick.NONE },
            confirmButton = {
                TextButton(
                    onClick = {
                        val chosen = pickerState.selectedDateMillis?.toCycleDate() ?: initial
                        when (picking) {
                            DatePick.START -> startDate = chosen
                            DatePick.END -> endDate = chosen
                            DatePick.NONE -> Unit
                        }
                        picking = DatePick.NONE
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { picking = DatePick.NONE }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
