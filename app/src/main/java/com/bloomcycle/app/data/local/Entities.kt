package com.bloomcycle.app.data.local

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.bloomcycle.app.core.time.CycleDate
import com.bloomcycle.app.domain.model.FlowLevel
import com.bloomcycle.app.domain.model.PeriodEvent
import com.bloomcycle.app.domain.model.SymptomLog
import java.time.Instant

@Entity(
    tableName = "period_events",
    indices = [Index(value = ["startDate"], unique = true)],
)
data class PeriodEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: CycleDate,
    val endDate: CycleDate?,
    val flow: FlowLevel?,
    val isSpottingOnly: Boolean,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "symptom_logs",
    indices = [Index(value = ["date"])],
)
data class SymptomLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: CycleDate,
    val symptomId: String,
    val severity: Int,
)

// --- mappers -----------------------------------------------------------------------------

fun PeriodEventEntity.toDomain() = PeriodEvent(
    id = id,
    startDate = startDate,
    endDate = endDate,
    flow = flow,
    isSpottingOnly = isSpottingOnly,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PeriodEvent.toEntity() = PeriodEventEntity(
    id = id,
    startDate = startDate,
    endDate = endDate,
    flow = flow,
    isSpottingOnly = isSpottingOnly,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun SymptomLogEntity.toDomain() = SymptomLog(
    id = id,
    date = date,
    symptomId = symptomId,
    severity = severity,
)

fun SymptomLog.toEntity() = SymptomLogEntity(
    id = id,
    date = date,
    symptomId = symptomId,
    severity = severity,
)
