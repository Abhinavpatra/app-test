package com.bloomcycle.app.data.local

import androidx.room3.ColumnTypeConverter
import com.bloomcycle.app.core.time.CycleDate
import com.bloomcycle.app.domain.model.ContentCategory
import com.bloomcycle.app.domain.model.FlowLevel
import com.bloomcycle.app.domain.model.PhaseType
import java.time.Instant
import java.time.LocalDate

/**
 * Dates are stored as ISO-8601 strings ("2026-10-02"), never epoch millis.
 * A LocalDate converted to millis and back can land on the wrong day depending on the
 * device timezone at each end — and a day off silently corrupts every prediction.
 */
class Converters {
    @ColumnTypeConverter
    fun fromCycleDate(value: CycleDate?): String? = value?.toString()

    @ColumnTypeConverter
    fun toCycleDate(value: String?): CycleDate? = value?.let(LocalDate::parse)

    @ColumnTypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @ColumnTypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @ColumnTypeConverter
    fun fromFlowLevel(value: FlowLevel?): String? = value?.name

    @ColumnTypeConverter
    fun toFlowLevel(value: String?): FlowLevel? = value?.let(FlowLevel::valueOf)

    @ColumnTypeConverter
    fun fromContentCategory(value: ContentCategory?): String? = value?.name

    @ColumnTypeConverter
    fun toContentCategory(value: String?): ContentCategory? = value?.let(ContentCategory::valueOf)

    @ColumnTypeConverter
    fun fromPhaseType(value: PhaseType?): String? = value?.name

    @ColumnTypeConverter
    fun toPhaseType(value: String?): PhaseType? = value?.let(PhaseType::valueOf)
}
