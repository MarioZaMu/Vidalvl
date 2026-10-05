package com.vidalvl.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** One axis of the radar. [value] goes from 0 to 1 (share of the chart's scale). */
data class RadarEntry(
    val levelText: String,
    @DrawableRes val iconRes: Int,
    val color: Color,
    val value: Float
)

private const val MIN_VISIBLE_VALUE = 0.12f

/**
 * Radar chart for the attributes. Axes go clockwise from the top (up to four are placed
 * with labels: top, right, bottom, left). The caller describes it for accessibility.
 */
@Composable
fun RadarChart(entries: List<RadarEntry>, modifier: Modifier = Modifier) {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }

    val gridColor = MaterialTheme.colorScheme.outline
    val fillColor = MaterialTheme.colorScheme.primary

    Box(modifier = modifier.aspectRatio(1f).clearAndSetSemantics { }) {
        Canvas(modifier = Modifier.fillMaxSize().padding(28.dp)) {
            val count = entries.size
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2

            fun point(index: Int, fraction: Float): Offset {
                val angle = Math.toRadians(index * 360.0 / count - 90.0)
                return Offset(
                    center.x + (radius * fraction * cos(angle)).toFloat(),
                    center.y + (radius * fraction * sin(angle)).toFloat()
                )
            }

            fun polygon(fractions: List<Float>) = Path().apply {
                fractions.forEachIndexed { i, f ->
                    val p = point(i, f)
                    if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                }
                close()
            }

            // Grid rings and axes.
            listOf(1f / 3, 2f / 3, 1f).forEach { ring ->
                drawPath(polygon(List(count) { ring }), gridColor, style = Stroke(1.dp.toPx()))
            }
            repeat(count) { drawLine(gridColor, center, point(it, 1f), 1.dp.toPx()) }

            // Data.
            val shape = polygon(entries.map { it.value.coerceIn(MIN_VISIBLE_VALUE, 1f) * grow.value })
            drawPath(shape, fillColor.copy(alpha = 0.28f))
            drawPath(shape, fillColor, style = Stroke(2.dp.toPx()))
            entries.forEachIndexed { i, entry ->
                drawVertex(point(i, entry.value.coerceIn(MIN_VISIBLE_VALUE, 1f) * grow.value), entry.color)
            }
        }

        val alignments = listOf(
            Alignment.TopCenter, Alignment.CenterEnd, Alignment.BottomCenter, Alignment.CenterStart
        )
        entries.forEachIndexed { index, entry ->
            Column(
                modifier = Modifier.align(alignments[index % alignments.size]),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(entry.iconRes),
                    contentDescription = null,
                    tint = entry.color,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = entry.levelText,
                    style = MaterialTheme.typography.labelSmall,
                    color = entry.color
                )
            }
        }
    }
}

private fun DrawScope.drawVertex(at: Offset, color: Color) {
    drawCircle(color, radius = 4.dp.toPx(), center = at)
}
