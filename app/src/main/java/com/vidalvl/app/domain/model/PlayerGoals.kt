package com.vidalvl.app.domain.model

/** Why the user came to the app. Each goal leans on one attribute; the AI uses them to suggest missions. */
enum class Goal(val attribute: Attribute) {
    FIT(Attribute.BODY),
    LEARN(Attribute.MIND),
    DISCIPLINE(Attribute.DISCIPLINE),
    RELATIONSHIPS(Attribute.SOCIAL),
    SLEEP(Attribute.BODY),
    LESS_PHONE(Attribute.DISCIPLINE),
    EAT_WELL(Attribute.BODY),
    STRESS(Attribute.MIND)
}

/** What the user chose on the welcome screen. [dailyMinutes] is the time they can give each day. */
data class PlayerGoals(
    val goals: Set<Goal>,
    val dailyMinutes: Int
)

object GoalRules {
    const val MAX_GOALS = 3
    val MINUTE_OPTIONS = listOf(5, 15, 30)
    const val DEFAULT_MINUTES = 15

    /** Adds or removes [goal]. A fourth goal is ignored: three is the most the user can pick. */
    fun toggle(selected: Set<Goal>, goal: Goal): Set<Goal> = when {
        goal in selected -> selected - goal
        selected.size < MAX_GOALS -> selected + goal
        else -> selected
    }

    fun canContinue(selected: Set<Goal>): Boolean = selected.isNotEmpty()

    /** Goals are stored as comma-separated names. Unknown names (from a future version) are dropped. */
    fun encode(goals: Set<Goal>): String = goals.joinToString(",") { it.name }

    fun decode(raw: String): Set<Goal> = raw.split(",")
        .mapNotNull { name -> Goal.entries.firstOrNull { it.name == name.trim() } }
        .toCollection(LinkedHashSet())
}
