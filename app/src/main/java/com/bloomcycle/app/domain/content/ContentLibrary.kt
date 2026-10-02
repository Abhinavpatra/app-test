package com.bloomcycle.app.domain.content

import com.bloomcycle.app.domain.model.ContentCategory
import com.bloomcycle.app.domain.model.ContentItem
import com.bloomcycle.app.domain.model.PhaseType

/**
 * Curated, cited guidance. Kept as compiled Kotlin rather than a JSON seed inside the
 * encrypted database: this is the one piece of content that should survive losing the
 * Keystore key, and static text has nothing to gain from being encrypted.
 *
 * Sources are reputable public-health bodies and are named on every card. Bloom makes no
 * claims beyond what those sources actually say.
 */
object ContentLibrary {

    val items: List<ContentItem> = listOf(
        ContentItem(
            id = "how-long",
            category = ContentCategory.CYCLE_FACTS,
            title = "How long does a period usually last?",
            body = "Most periods last between 3 and 7 days, with around 5 being typical. " +
                "Bleeding is usually heaviest in the first two days. Anyone whose periods " +
                "routinely last longer than a week is within normal variation for some people " +
                "but it is worth mentioning at an appointment.",
            sourceTitle = "NHS — Periods",
            sourceUrl = "https://www.nhs.uk/conditions/periods/",
        ),
        ContentItem(
            id = "cycle-length",
            category = ContentCategory.CYCLE_FACTS,
            title = "What counts as a normal cycle length",
            body = "Cycles between 21 and 35 days are considered typical for adults, with 28 " +
                "days as the average. Cycle length naturally varies, and it is common for two " +
                "cycles in a row to differ by several days.",
            sourceTitle = "NHS — Periods",
            sourceUrl = "https://www.nhs.uk/conditions/periods/",
        ),
        ContentItem(
            id = "cycle-variation",
            category = ContentCategory.CYCLE_FACTS,
            title = "Why the first years are the most unpredictable",
            body = "Cycles often take a year or two to settle after periods begin, and they " +
                "can become less predictable again in the years before menopause. Stress, " +
                "travel, illness and changes in weight all shift timing too.",
            sourceTitle = "NHS — Periods",
            sourceUrl = "https://www.nhs.uk/conditions/periods/",
        ),
        ContentItem(
            id = "heavy-bleeding",
            category = ContentCategory.CYCLE_FACTS,
            title = "When bleeding counts as heavy",
            body = "Soaking through a pad or tampon every hour for two or more consecutive " +
                "hours, passing clots larger than about 2.5cm, or bleeding for more than 7 " +
                "days are all reasons to have it assessed. Effective treatment exists.",
            sourceTitle = "NICE guideline NG92 — Heavy menstrual bleeding",
            sourceUrl = "https://www.nice.org.uk/guidance/ng92",
        ),
        ContentItem(
            id = "why-cramps",
            category = ContentCategory.PAIN_RELIEF,
            title = "Why cramps happen",
            body = "The uterus contracts to shed its lining, and substances called prostaglandins " +
                "drive both those contractions and the inflammation around them. Higher " +
                "prostaglandin levels are linked to more painful periods.",
            sourceTitle = "NHS — Period pain",
            sourceUrl = "https://www.nhs.uk/conditions/period-pain/",
        ),
        ContentItem(
            id = "heat",
            category = ContentCategory.PAIN_RELIEF,
            title = "Heat is genuinely one of the better options",
            body = "A heat pad or warm water bottle on the lower abdomen relaxes the muscle and " +
                "takes the edge off for many people. Around 15 to 20 minutes at a time is a " +
                "comfortable starting point — hot enough to soothe, never hot enough to sting.",
            sourceTitle = "NHS — Period pain",
            sourceUrl = "https://www.nhs.uk/conditions/period-pain/",
        ),
        ContentItem(
            id = "movement",
            category = ContentCategory.PAIN_RELIEF,
            phaseTag = PhaseType.MENSTRUAL,
            title = "Gentle movement often helps more than stillness",
            body = "Walking, stretching and easy yoga increase circulation and can ease cramps " +
                "for a lot of people. Nothing here needs to be a workout — moving comfortably " +
                "counts, and stopping the moment something hurts always counts as good judgement.",
            sourceTitle = "NHS — Period pain",
            sourceUrl = "https://www.nhs.uk/conditions/period-pain/",
        ),
        ContentItem(
            id = "painkillers",
            category = ContentCategory.PAIN_RELIEF,
            title = "About painkillers",
            body = "Anti-inflammatory painkillers such as ibuprofen work against prostaglandins " +
                "themselves, so they tend to help more than paracetamol for cramps — and they " +
                "work best taken early, before pain peaks. Not everyone can take them; a " +
                "pharmacist can confirm what is right for you.",
            sourceTitle = "NHS — Period pain",
            sourceUrl = "https://www.nhs.uk/conditions/period-pain/",
        ),
        ContentItem(
            id = "when-to-ask",
            category = ContentCategory.PAIN_RELIEF,
            title = "When to actually ask for help",
            body = "Pain that stops you working or sleeping, pain that suddenly worsens, or " +
                "pain that no longer responds to what used to help are all worth an " +
                "appointment. Painful periods are common — but treatable is not the same as " +
                "something you simply have to endure.",
            sourceTitle = "NICE guideline NG92",
            sourceUrl = "https://www.nice.org.uk/guidance/ng92",
        ),
        ContentItem(
            id = "pms",
            category = ContentCategory.MIND,
            phaseTag = PhaseType.LUTEAL,
            title = "PMS is real and very common",
            body = "Around three in four people experience some premenstrual symptoms. Low mood, " +
                "irritability, bloating and tiredness in the week before a period are driven by " +
                "hormone changes, not by weakness or a bad attitude.",
            sourceTitle = "NHS — Premenstrual syndrome",
            sourceUrl = "https://www.nhs.uk/conditions/premenstrual-syndrome/",
        ),
        ContentItem(
            id = "luteal-temperature",
            category = ContentCategory.CYCLE_FACTS,
            phaseTag = PhaseType.LUTEAL,
            title = "Why the second half of the cycle feels different",
            body = "After ovulation, progesterone rises and body temperature shifts by a fraction " +
                "of a degree. That same shift is why the days before a period often come with " +
                "more fatigue and a stronger preference for quieter plans.",
            sourceTitle = "NHS — Periods",
            sourceUrl = "https://www.nhs.uk/conditions/periods/",
        ),
        ContentItem(
            id = "iron",
            category = ContentCategory.NUTRITION,
            phaseTag = PhaseType.MENSTRUAL,
            title = "Replacing what is lost",
            body = "Iron-rich foods — red meat, lentils, beans, fortified cereals, dark leafy " +
                "greens — help replenish stores, and pairing them with vitamin C improves " +
                "absorption. If heavy bleeding is a regular feature of your periods, it is " +
                "worth having iron levels checked rather than self-supplementing.",
            sourceTitle = "NHS — Periods",
            sourceUrl = "https://www.nhs.uk/conditions/periods/",
        ),
        ContentItem(
            id = "sleep",
            category = ContentCategory.MIND,
            title = "Sleep when the cycle says so",
            body = "The rise in temperature and progesterone in the luteal phase genuinely makes " +
                "sleep lighter for some people. A cooler room and a consistent wind-down tend " +
                "to do more than trying harder at it.",
            sourceTitle = "NHS — Premenstrual syndrome",
            sourceUrl = "https://www.nhs.uk/conditions/premenstrual-syndrome/",
        ),
        ContentItem(
            id = "fertility-basics",
            category = ContentCategory.CYCLE_FACTS,
            title = "How the fertile window works",
            body = "An egg survives roughly 24 hours after ovulation, while sperm can live up to " +
                "about five days inside the body. That is why the fertile window spans several " +
                "days before ovulation rather than only the day itself. Ovulation timing varies " +
                "considerably between cycles and between people.",
            sourceTitle = "NHS — Periods and fertility",
            sourceUrl = "https://www.nhs.uk/conditions/periods/",
        ),
        ContentItem(
            id = "ovulation-timing",
            category = ContentCategory.CYCLE_FACTS,
            title = "The 'day 14' rule is an average, not a rule",
            body = "Textbooks often describe ovulation on day 14 because that is the midpoint of " +
                "a 28-day cycle. In practice ovulation can land anywhere across a wide range, " +
                "and it shifts from cycle to cycle — which is exactly why calendar-only " +
                "prediction is an estimate and not a method.",
            sourceTitle = "NHS — Periods and fertility",
            sourceUrl = "https://www.nhs.uk/conditions/periods/",
        ),
        ContentItem(
            id = "confidence-note",
            category = ContentCategory.CYCLE_FACTS,
            title = "What a prediction can and cannot tell you",
            body = "Bloom works from an average of your own recent cycles plus a spread that " +
                "widens as your timing becomes less consistent. It describes a pattern in data " +
                "you entered. It cannot confirm ovulation, cannot diagnose anything, and should " +
                "never be relied on for contraception.",
        ),
    )

    private val byId = items.associateBy { it.id }

    fun get(id: String): ContentItem? = byId[id]
}
