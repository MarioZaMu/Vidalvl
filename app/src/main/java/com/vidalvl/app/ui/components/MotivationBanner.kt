package com.vidalvl.app.ui.components

import androidx.annotation.ArrayRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
import com.vidalvl.app.R
import com.vidalvl.app.domain.model.Goal
import com.vidalvl.app.domain.model.MotivationKind
import com.vidalvl.app.domain.model.MotivationMessage
import com.vidalvl.app.ui.theme.label
import java.util.Locale

/** One encouraging line, with an accent bar on the left. */
@Composable
fun MotivationBanner(message: MotivationMessage, modifier: Modifier = Modifier) {
    val text = motivationText(message)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun motivationText(message: MotivationMessage): String {
    val options = stringArrayResource(arrayFor(message))
    val template = options[message.variant % options.size]
    val locale = Locale.getDefault()
    return when (message.kind) {
        MotivationKind.STREAK -> String.format(locale, template, message.first)
        MotivationKind.PROGRESS -> String.format(locale, template, message.first, message.second)
        MotivationKind.NEGLECTED -> String.format(locale, template, message.attribute?.label().orEmpty())
        else -> template
    }
}

@ArrayRes
private fun arrayFor(message: MotivationMessage): Int = when (message.kind) {
    MotivationKind.ALL_DONE -> R.array.motivation_all_done
    MotivationKind.WELCOME_BACK -> R.array.motivation_welcome_back
    MotivationKind.STREAK -> R.array.motivation_streak
    MotivationKind.PROGRESS -> R.array.motivation_progress
    MotivationKind.NEGLECTED -> R.array.motivation_neglected
    MotivationKind.GENERIC -> R.array.motivation_generic
    MotivationKind.START -> when (message.goal) {
        Goal.FIT -> R.array.motivation_start_fit
        Goal.LEARN -> R.array.motivation_start_learn
        Goal.DISCIPLINE -> R.array.motivation_start_discipline
        Goal.RELATIONSHIPS -> R.array.motivation_start_relationships
        Goal.SLEEP -> R.array.motivation_start_sleep
        Goal.LESS_PHONE -> R.array.motivation_start_less_phone
        Goal.EAT_WELL -> R.array.motivation_start_eat_well
        Goal.STRESS -> R.array.motivation_start_stress
        null -> R.array.motivation_generic
    }
}
