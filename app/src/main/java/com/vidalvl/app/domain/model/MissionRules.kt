package com.vidalvl.app.domain.model

/** Limits and cleaning for missions created or edited by the user. */
object MissionRules {
    const val MAX_TITLE_LENGTH = 40
    const val MAX_DESCRIPTION_LENGTH = 80

    fun isValidTitle(raw: String): Boolean {
        val title = raw.trim()
        return title.isNotEmpty() && title.length <= MAX_TITLE_LENGTH
    }

    fun cleanTitle(raw: String): String = raw.trim().take(MAX_TITLE_LENGTH)

    /** Blank descriptions are stored as null. */
    fun cleanDescription(raw: String): String? =
        raw.trim().take(MAX_DESCRIPTION_LENGTH).ifEmpty { null }
}