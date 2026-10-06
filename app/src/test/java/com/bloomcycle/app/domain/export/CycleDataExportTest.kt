package com.bloomcycle.app.domain.export

import com.bloomcycle.app.core.time.CycleDate
import com.bloomcycle.app.domain.model.FlowLevel
import com.bloomcycle.app.domain.model.PeriodEvent
import com.bloomcycle.app.domain.model.SymptomLog
import com.bloomcycle.app.domain.model.UserSettings
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleDataExportTest {

    private fun exported(
        periods: List<PeriodEvent> = emptyList(),
        symptoms: List<SymptomLog> = emptyList(),
        settings: UserSettings = UserSettings(),
    ) = buildExportJson(periods, symptoms, settings, "2026-10-06T12:00:00Z", "1.0")

    @Test
    fun `a minimal period exports byte-for-byte as expected`() {
        val json = exported(periods = listOf(PeriodEvent(startDate = CycleDate.of(2026, 10, 1))))

        assertEquals(
            "{\"format\":\"bloom-export-1\"," +
                "\"exportedAt\":\"2026-10-06T12:00:00Z\"," +
                "\"appVersion\":\"1.0\"," +
                "\"periods\":[{\"startDate\":\"2026-10-01\"," +
                "\"endDate\":null,\"flow\":null,\"isSpottingOnly\":false,\"notes\":null," +
                "\"createdAt\":\"1970-01-01T00:00:00Z\"," +
                "\"updatedAt\":\"1970-01-01T00:00:00Z\"}]," +
                "\"symptoms\":[]," +
                "\"settings\":{\"typicalCycleLength\":28,\"cycleContext\":\"NONE\"," +
                "\"reminderHour\":9,\"reminderMinute\":0,\"remindersEnabled\":true," +
                "\"dailyNoteEnabled\":true,\"predictionsMuted\":false}}",
            json,
        )
    }

    @Test
    fun `populated fields export with their values`() {
        val json = exported(
            periods = listOf(
                PeriodEvent(
                    startDate = CycleDate.of(2026, 9, 2),
                    endDate = CycleDate.of(2026, 9, 6),
                    flow = FlowLevel.MEDIUM,
                    isSpottingOnly = true,
                    createdAt = Instant.parse("2026-09-02T08:00:00Z"),
                    updatedAt = Instant.parse("2026-09-06T08:00:00Z"),
                ),
            ),
            symptoms = listOf(
                SymptomLog(date = CycleDate.of(2026, 10, 2), symptomId = "cramps", severity = 4),
            ),
            settings = UserSettings(typicalCycleLength = 29),
        )

        assertTrue(json.contains("\"endDate\":\"2026-09-06\""))
        assertTrue(json.contains("\"flow\":\"MEDIUM\""))
        assertTrue(json.contains("\"isSpottingOnly\":true"))
        assertTrue(json.contains("\"createdAt\":\"2026-09-02T08:00:00Z\""))
        assertTrue(
            json.contains(
                "\"symptoms\":[{\"date\":\"2026-10-02\"," +
                    "\"symptomId\":\"cramps\",\"severity\":4}]",
            ),
        )
        assertTrue(json.contains("\"typicalCycleLength\":29"))
    }

    @Test
    fun `free text survives quotes backslashes newlines and tabs`() {
        val json = exported(
            periods = listOf(
                PeriodEvent(
                    startDate = CycleDate.of(2026, 9, 2),
                    notes = "Felt \"off\" \\ really\nheavy\tfirst day",
                ),
            ),
        )

        assertTrue(
            json.contains("\"notes\":\"Felt \\\"off\\\" \\\\ really\\nheavy\\tfirst day\""),
        )
    }

    @Test
    fun `control characters export as unicode escapes`() {
        val json = exported(
            periods = listOf(
                PeriodEvent(startDate = CycleDate.of(2026, 9, 2), notes = "a\u0001b"),
            ),
        )

        assertTrue(json.contains("\"notes\":\"a\\u0001b\""))
    }
}
