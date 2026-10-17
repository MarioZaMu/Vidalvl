package com.vidalvl.app.domain.model

enum class Gender { MALE, FEMALE }

/** Sprite layers in draw order (back to front). */
enum class AvatarLayer { CLOAK_BACK, HAIR_BACK, BODY, EYES, OUTFIT, HAIR_FRONT, HAT, GLASSES, CLOAK }

/** Everything needed to draw the base avatar. [hairColor] is an ARGB int. */
data class AvatarConfig(
    val gender: Gender = Gender.MALE,
    val skinTone: Int = 1,
    val hairStyle: Int = 1,
    val hairColor: Int = DEFAULT_HAIR_COLOR,
    val eyeStyle: Int = 1,
    /** Player name shown in the headers. Blank until the user chooses one. */
    val name: String = ""
) {
    companion object {
        const val MAX_NAME_LENGTH = 16
        const val SKIN_TONES = 5
        const val HAIR_STYLES = 33
        const val EYE_STYLES = 10
        const val DEFAULT_HAIR_COLOR = 0xFF6B4423.toInt()

        val HAIR_COLORS = listOf(
            0xFF1C1C1C.toInt(), // black
            0xFF6B4423.toInt(), // brown
            0xFFD9A441.toInt(), // blond
            0xFFB5361F.toInt(), // red
            0xFFE8E8E8.toInt(), // white
            0xFF3E7BD9.toInt()  // blue
        )
    }
}
