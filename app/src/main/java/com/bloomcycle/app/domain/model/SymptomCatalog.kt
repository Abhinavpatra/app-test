package com.bloomcycle.app.domain.model

/** A fixed, curated catalog. Not a DB table — these ids are referenced by symptom logs. */
object SymptomCatalog {
    private val all = listOf(
        SymptomDef("cramps", "Cramps", SymptomCategory.PAIN),
        SymptomDef("backache", "Lower back ache", SymptomCategory.PAIN),
        SymptomDef("headache", "Headache", SymptomCategory.PAIN),
        SymptomDef("mid_cycle_pain", "One-sided twinge", SymptomCategory.PAIN),
        SymptomDef("bloating", "Bloating", SymptomCategory.BODY),
        SymptomDef("tender_chest", "Tender chest", SymptomCategory.BODY),
        SymptomDef("nausea", "Nausea", SymptomCategory.BODY),
        SymptomDef("acne", "Skin flare-up", SymptomCategory.BODY),
        SymptomDef("low_energy", "Low energy", SymptomCategory.ENERGY),
        SymptomDef("high_energy", "High energy", SymptomCategory.ENERGY),
        SymptomDef("focused", "Sharp focus", SymptomCategory.ENERGY),
        SymptomDef("irritable", "Irritable", SymptomCategory.MOOD),
        SymptomDef("low_mood", "Low mood", SymptomCategory.MOOD),
        SymptomDef("anxious", "Anxious", SymptomCategory.MOOD),
        SymptomDef("calm", "Calm", SymptomCategory.MOOD),
        SymptomDef("happy", "Content", SymptomCategory.MOOD),
        SymptomDef("sociable", "Wanting company", SymptomCategory.MOOD),
        SymptomDef("withdrawn", "Wanting space", SymptomCategory.MOOD),
        SymptomDef("cravings", "Cravings", SymptomCategory.BODY),
        SymptomDef("insomnia", "Trouble sleeping", SymptomCategory.SLEEP),
        SymptomDef("restless_sleep", "Restless sleep", SymptomCategory.SLEEP),
        SymptomDef("spotting", "Spotting", SymptomCategory.FLOW),
        SymptomDef("clots", "Clots", SymptomCategory.FLOW),
        SymptomDef("heavy_flow", "Heavy flow", SymptomCategory.FLOW),
        SymptomDef("light_flow", "Light flow", SymptomCategory.FLOW),
    )

    val byId: Map<String, SymptomDef> = all.associateBy { it.id }
    val values: List<SymptomDef> = all

    fun label(id: String): String = byId[id]?.label ?: id

    fun forCategory(category: SymptomCategory): List<SymptomDef> =
        all.filter { it.category == category }
}
