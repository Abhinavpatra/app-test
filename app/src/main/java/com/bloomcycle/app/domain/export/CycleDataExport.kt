package com.bloomcycle.app.domain.export

import com.bloomcycle.app.domain.model.PeriodEvent
import com.bloomcycle.app.domain.model.SymptomLog
import com.bloomcycle.app.domain.model.UserSettings

/**
 * Everything the user gave the app, as one JSON document for the Settings export.
 *
 * Deliberately hand-rolled rather than kotlinx-serialization: the shape is small, flat
 * and versioned (`format`), and this keeps a user-data path free of reflection and
 * codegen surprises. Chat is excluded on purpose — posted messages belong to shared
 * rooms on the server, not to this device's export.
 */
fun buildExportJson(
    periods: List<PeriodEvent>,
    symptoms: List<SymptomLog>,
    settings: UserSettings,
    exportedAt: String,
    appVersion: String,
): String = buildString {
    append("{")
    stringField("format", "bloom-export-1")
    stringField("exportedAt", exportedAt)
    stringField("appVersion", appVersion)
    append("\"periods\":[")
    append(
        periods.joinToString(",") { event ->
            jsonObject(
                "\"startDate\":\"${event.startDate}\"",
                "\"endDate\":${event.endDate?.let { "\"$it\"" } ?: "null"}",
                "\"flow\":${event.flow?.name?.let { "\"$it\"" } ?: "null"}",
                "\"isSpottingOnly\":${event.isSpottingOnly}",
                "\"notes\":${event.notes?.let { "\"${it.escapeJson()}\"" } ?: "null"}",
                "\"createdAt\":\"${event.createdAt}\"",
                "\"updatedAt\":\"${event.updatedAt}\"",
            )
        },
    )
    append("],")
    append("\"symptoms\":[")
    append(
        symptoms.joinToString(",") { log ->
            jsonObject(
                "\"date\":\"${log.date}\"",
                "\"symptomId\":\"${log.symptomId.escapeJson()}\"",
                "\"severity\":${log.severity}",
            )
        },
    )
    append("],")
    append("\"settings\":")
    append(
        jsonObject(
            "\"typicalCycleLength\":${settings.typicalCycleLength}",
            "\"cycleContext\":\"${settings.cycleContext.name}\"",
            "\"reminderHour\":${settings.reminderHour}",
            "\"reminderMinute\":${settings.reminderMinute}",
            "\"remindersEnabled\":${settings.remindersEnabled}",
            "\"dailyNoteEnabled\":${settings.dailyNoteEnabled}",
            "\"predictionsMuted\":${settings.predictionsMuted}",
        ),
    )
    append("}")
}

private fun jsonObject(vararg fields: String): String = fields.joinToString(",", "{", "}")

private fun StringBuilder.stringField(name: String, value: String) {
    append("\"$name\":\"${value.escapeJson()}\",")
}

private fun String.escapeJson(): String = buildString {
    for (char in this@escapeJson) {
        when (char) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (char < ' ') append("\\u%04x".format(char.code)) else append(char)
        }
    }
}
