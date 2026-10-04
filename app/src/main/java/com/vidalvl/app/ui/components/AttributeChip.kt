package com.vidalvl.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Compact attribute summary. It dims when the attribute has been neglected (low glow). */
@Composable
fun AttributeChip(
    label: String,
    level: Int,
    progress: Float,
    glow: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedGlow by animateFloatAsState(targetValue = glow, label = "chipGlow")

    Surface(
        modifier = modifier.alpha(0.55f + 0.45f * animatedGlow),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, color.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                maxLines = 1
            )
            Text(
                text = "Nv $level",
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                color = color
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(color)
                )
            }
        }
    }
}