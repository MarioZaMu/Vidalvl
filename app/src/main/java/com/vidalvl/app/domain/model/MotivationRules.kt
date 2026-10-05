package com.vidalvl.app.domain.model

enum class MotivationKind { ALL_DONE, WELCOME_BACK, STREAK, PROGRESS, NEGLECTED, START, GENERIC }

/**
 * Which message to show. The text lives in string resources; [variant] only picks among the
 * alternatives of a kind, and the numbers or the attribute fill the blanks of the sentence.
 */
data class MotivationMessage(
    val kind: MotivationKind,
    val variant: Int,
    val goal: Goal? = null,
    val attribute: Attribute? = null,
    val first: Int = 0,
    val second: Int = 0
)

data class MotivationInput(
    val dailyDone: Int,
    val dailyTotal: Int,
    val streak: Int,
    /** Days since the last completed mission; null if there never was one. */
    val daysSinceLast: Int?,
    val goals: Set<Goal>,
    /** Attributes the player practiced before but not lately, most neglected first. */
    val neglected: List<Attribute>,
    /** Changes the variant (and the goal) from one day to the next. */
    val seed: Long
)

/** Picks an encouraging message from what is happening today. It never blames: absence is met with a welcome. */
object MotivationRules {
    private val STREAK_MILESTONES = setOf(3, 5, 7, 14, 21, 30, 50, 100)
    const val AWAY_DAYS = 3

    /** Below the neutral 0.6 of an untrained attribute: it was practiced, then left alone. */
    const val NEGLECTED_GLOW = 0.55f

    /**
     * Attributes worth a gentle nudge, most neglected first. When the player chose goals, only the
     * attributes those goals lean on are mentioned, so the message stays about what they care about.
     */
    fun neglected(glows: Map<Attribute, Float>, goals: Set<Goal>): List<Attribute> {
        val wanted = goals.map { it.attribute }.toSet()
        return glows.entries
            .filter { it.value < NEGLECTED_GLOW && (wanted.isEmpty() || it.key in wanted) }
            .sortedBy { it.value }
            .map { it.key }
    }

    fun pick(input: MotivationInput): MotivationMessage {
        val variant = Math.floorMod(input.seed, 1_000_000L).toInt()
        val done = input.dailyDone
        val total = input.dailyTotal

        return when {
            total > 0 && done == total ->
                MotivationMessage(MotivationKind.ALL_DONE, variant)

            done == 0 && (input.daysSinceLast ?: 0) >= AWAY_DAYS ->
                MotivationMessage(MotivationKind.WELCOME_BACK, variant)

            done > 0 && input.streak in STREAK_MILESTONES ->
                MotivationMessage(MotivationKind.STREAK, variant, first = input.streak)

            done > 0 ->
                MotivationMessage(MotivationKind.PROGRESS, variant, first = done, second = total)

            input.neglected.isNotEmpty() ->
                MotivationMessage(MotivationKind.NEGLECTED, variant, attribute = input.neglected.first())

            input.goals.isNotEmpty() -> {
                val goals = input.goals.toList()
                MotivationMessage(MotivationKind.START, variant, goal = goals[variant % goals.size])
            }

            else -> MotivationMessage(MotivationKind.GENERIC, variant)
        }
    }
}
