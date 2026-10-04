package com.vidalvl.app.domain.model

enum class CreatureStage { HATCHLING, CHILD, YOUTH }

enum class CreatureMood { HAPPY, NEUTRAL, BORED, SAD }

object CreatureRules {
    const val CHILD_LEVEL = 3
    const val YOUTH_LEVEL = 6

    fun stageFor(totalLevel: Int): CreatureStage = when {
        totalLevel >= YOUTH_LEVEL -> CreatureStage.YOUTH
        totalLevel >= CHILD_LEVEL -> CreatureStage.CHILD
        else -> CreatureStage.HATCHLING
    }

    /** Mood from the average glow of the four attributes. */
    fun moodFor(averageGlow: Float): CreatureMood = when {
        averageGlow >= 0.9f -> CreatureMood.HAPPY
        averageGlow >= 0.55f -> CreatureMood.NEUTRAL
        averageGlow >= 0.4f -> CreatureMood.BORED
        else -> CreatureMood.SAD
    }

    /** Attribute strength from 0 to 1: level 1 = 0, level 6 or more = 1. */
    fun strengthOf(level: Int): Float = ((level - 1) / 5f).coerceIn(0f, 1f)
}