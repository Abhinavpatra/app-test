package com.bloomcycle.app.domain.model

import com.bloomcycle.app.core.time.CycleDate
import java.time.Instant

enum class FlowLevel(val label: String) {
    SPOTTING("Spotting"),
    LIGHT("Light"),
    MEDIUM("Medium"),
    HEAVY("Heavy"),
}

enum class PhaseType(val label: String, val subtitle: String) {
    MENSTRUAL("Menstrual", "Rest, warmth, and very little pressure"),
    FOLLICULAR("Follicular", "Energy rising — a good week for starting things"),
    OVULATORY("Ovulatory", "Peak energy and sociability for most people"),
    LUTEAL("Luteal", "Turning inward; steady work, gentler plans"),
}

enum class PredictionConfidence(val label: String) {
    LOW("Still learning your rhythm"),
    MEDIUM("A working estimate"),
    HIGH("Based on a consistent pattern"),
}

/**
 * Physiological context that changes whether a cycle prediction means anything at all.
 *
 * Every prediction assumes ovulation happens roughly a fortnight before the next bleed.
 * Hormonal contraception suppresses ovulation outright, and perimenopause and the
 * postpartum period both make cycles genuinely unpredictable — so in those states a
 * confident-looking date is not a reading of the logged data, it is an invention.
 * See plan.md §8.4.
 */
enum class CycleContext(val label: String, val blurb: String) {
    NONE(
        label = "Not applicable",
        blurb = "Dates are read straight from what you have logged.",
    ),
    HORMONAL_CONTRACEPTION(
        label = "Hormonal contraception",
        blurb = "The combined pill, patch, ring, hormonal IUD, implant and injection suppress " +
            "ovulation, so there is no cycle to predict. Bleeding on these is a withdrawal " +
            "bleed, not a period.",
    ),
    PERIMENOPAUSE(
        label = "Perimenopause",
        blurb = "Cycles often lengthen, shorten or skip entirely, so any dates here are a " +
            "rough guide at best.",
    ),
    POSTPARTUM(
        label = "Postpartum or breastfeeding",
        blurb = "Cycles are usually irregular while your body recovers, and may not have " +
            "returned yet.",
    ),
}


enum class SymptomCategory(val label: String) {
    FLOW("Flow"),
    PAIN("Sensations"),
    MOOD("Mood"),
    ENERGY("Energy"),
    BODY("Body"),
    SLEEP("Sleep"),
}

data class SymptomDef(
    val id: String,
    val label: String,
    val category: SymptomCategory,
)

data class PeriodEvent(
    val id: Long = 0,
    val startDate: CycleDate,
    val endDate: CycleDate? = null,
    val flow: FlowLevel? = null,
    val isSpottingOnly: Boolean = false,
    val notes: String? = null,
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH,
)

data class SymptomLog(
    val id: Long = 0,
    val date: CycleDate,
    val symptomId: String,
    val severity: Int = 3,
)

/** A cycle derived from two consecutive logged period starts. Never stored — always computed. */
data class Cycle(
    val index: Int,
    val startDate: CycleDate,
    val endDate: CycleDate?,
    val periodStartDate: CycleDate,
    val periodEndDate: CycleDate?,
    val isOngoing: Boolean,
    /** Days until the next logged period start; null for the most recent cycle. */
    val lengthDays: Int?,
    /** null when the user logged a start date only, so duration is genuinely unknown. */
    val periodDurationDays: Int?,
    /** The gap before this cycle exceeded the logging threshold, so it is not a real cycle. */
    val hasGapBefore: Boolean,
)

data class CyclePrediction(
    val nextPeriodStart: CycleDate,
    val earliestStart: CycleDate,
    val latestStart: CycleDate,
    val ovulationDay: CycleDate,
    val fertileWindowStart: CycleDate,
    val fertileWindowEnd: CycleDate,
    val pmsWindowStart: CycleDate,
    val pmsWindowEnd: CycleDate,
    val confidence: PredictionConfidence,
    val averageCycleLength: Int,
    val averagePeriodDuration: Int,
    val lutealPhaseLength: Int,
    val cyclesUsed: Int,
    val regularityDays: Double,
    val regularityLabel: String,
    val isLate: Boolean,
    val daysUntilNextPeriod: Long,
    /** Why the dates below are caveated; [CycleContext.NONE] when they are not. */
    val context: CycleContext = CycleContext.NONE,
) {
    /** True when the user's context makes these dates a rough guide rather than a reading. */
    val isCaveated: Boolean get() = context != CycleContext.NONE

    /** README's "long term prediction" — projected starts for the coming months. */
    fun projectNext(months: Int, cycleLength: Int = averageCycleLength): List<CycleDate> =
        (1..months).map { nextPeriodStart.plusDays((it * cycleLength).toLong()) }
}

data class PhaseState(
    val phase: PhaseType,
    val cycleDay: Int,
    val dayOfPhase: Int,
    val phaseDaysTotal: Int?,
    val cycle: Cycle?,
    val prediction: CyclePrediction?,
    val today: CycleDate,
) {
    val cycleLength: Int get() = prediction?.averageCycleLength ?: 28
    val daysUntilNext: Long get() = prediction?.daysUntilNextPeriod ?: 0
}

enum class ContentCategory(val label: String) {
    PAIN_RELIEF("Easing discomfort"),
    CYCLE_FACTS("What's typical"),
    MOVEMENT("Moving gently"),
    NUTRITION("Eating well"),
    MIND("Steadiness"),
}

data class ContentItem(
    val id: String,
    val category: ContentCategory,
    val phaseTag: PhaseType? = null,
    val title: String,
    val body: String,
    val sourceTitle: String? = null,
    val sourceUrl: String? = null,
    val isPremium: Boolean = false,
)

data class UserSettings(
    val onboardingComplete: Boolean = false,
    val remindersEnabled: Boolean = true,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val dailyNoteEnabled: Boolean = true,
    /** Set when the user tells us notifications should be quiet (contraception, TTC, etc.). */
    val predictionsMuted: Boolean = false,
    /**
     * What is physiologically going on, from the user's side. Drives whether predictions
     * are shown at all — see [CycleContext]. Independent of [predictionsMuted], which only
     * silences notifications: this one changes what the engine is willing to claim.
     */
    val cycleContext: CycleContext = CycleContext.NONE,
    val chatDisplayName: String = "",
    val notificationPermissionAsked: Boolean = false,
)

enum class PremiumFeature(val title: String, val blurb: String) {
    ANALYSIS("Pattern analysis", "Look back across a year of cycles and see what actually repeats."),
    FERTILITY_WINDOW("Fertility window", "Your most likely fertile days, with an honest confidence range."),
    LONG_HORIZON("Six months ahead", "Projected periods for the next half year, ready to plan around."),
    WORKOUT_PLAN("Movement by phase", "Suggested intensity for training, running and rest, day by day."),
    PERSONALITY_READING("What your cycle says", "A reflective, astrology-flavoured read on your rhythm."),
}

data class EntitlementState(
    val unlocked: Set<PremiumFeature> = emptySet(),
    val isDebugUnlocked: Boolean = false,
) {
    fun isUnlocked(feature: PremiumFeature) = isDebugUnlocked || feature in unlocked
}

enum class ChatScope(val label: String) {
    GLOBAL("Everyone"),
    MY_PHASE("Around my phase"),
}

data class ChatMessage(
    val id: String,
    val text: String,
    val authorName: String,
    val phaseBucket: String,
    val isOwn: Boolean,
    val createdAt: Instant,
)

/**
 * Everything about the sender that leaves the device. Deliberately coarse: the phase
 * bucket is a four-word label, never a date, so a message cannot be used to work out
 * exactly when someone's period was.
 */
data class ChatIdentity(
    val displayName: String,
    val phaseBucket: String,
    val cycleLengthBand: String,
)

object ChatBuckets {
    const val UNKNOWN = "somewhere in between"

    fun forPhase(phase: PhaseType?): String = when (phase) {
        PhaseType.MENSTRUAL -> "on their period"
        PhaseType.FOLLICULAR -> "just after their period"
        PhaseType.OVULATORY -> "near ovulation"
        PhaseType.LUTEAL -> "in the week before"
        null -> UNKNOWN
    }

    fun forCycleLength(averageDays: Int?): String = when {
        averageDays == null -> "still learning"
        averageDays < 24 -> "a shorter cycle"
        averageDays <= 30 -> "a typical cycle"
        else -> "a longer cycle"
    }

    /** Room id for a phase bucket, so two people in the same phase share a room. */
    fun roomFor(phase: PhaseType?, scope: ChatScope): String = when (scope) {
        ChatScope.GLOBAL -> "global"
        ChatScope.MY_PHASE -> "phase-" + (phase?.name?.lowercase() ?: "unknown")
    }
}
