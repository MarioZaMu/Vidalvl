package com.vidalvl.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.vidalvl.app.domain.model.Attribute

/** Spanish name shown to the user. */
fun Attribute.label(): String = when (this) {
    Attribute.BODY -> "Cuerpo"
    Attribute.MIND -> "Mente"
    Attribute.DISCIPLINE -> "Disciplina"
    Attribute.SOCIAL -> "Social"
}

fun AttributeColors.forAttribute(attribute: Attribute): Color = when (attribute) {
    Attribute.BODY -> body
    Attribute.MIND -> mind
    Attribute.DISCIPLINE -> discipline
    Attribute.SOCIAL -> social
}