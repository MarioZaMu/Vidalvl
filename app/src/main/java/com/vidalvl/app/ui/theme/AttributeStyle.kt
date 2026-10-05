package com.vidalvl.app.ui.theme

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.vidalvl.app.R
import com.vidalvl.app.domain.model.Attribute

@StringRes
fun Attribute.labelRes(): Int = when (this) {
    Attribute.BODY -> R.string.attribute_body
    Attribute.MIND -> R.string.attribute_mind
    Attribute.DISCIPLINE -> R.string.attribute_discipline
    Attribute.SOCIAL -> R.string.attribute_social
}

/** Name shown to the user, in the app language. */
@Composable
fun Attribute.label(): String = stringResource(labelRes())

@DrawableRes
fun Attribute.iconRes(): Int = when (this) {
    Attribute.BODY -> R.drawable.ic_attr_body
    Attribute.MIND -> R.drawable.ic_attr_mind
    Attribute.DISCIPLINE -> R.drawable.ic_attr_discipline
    Attribute.SOCIAL -> R.drawable.ic_attr_social
}

fun AttributeColors.forAttribute(attribute: Attribute): Color = when (attribute) {
    Attribute.BODY -> body
    Attribute.MIND -> mind
    Attribute.DISCIPLINE -> discipline
    Attribute.SOCIAL -> social
}