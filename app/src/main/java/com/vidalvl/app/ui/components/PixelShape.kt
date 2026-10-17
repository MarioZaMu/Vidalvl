package com.vidalvl.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Corners cut as a staircase instead of a curve, to match the pixel-art avatar.
 * With [steps] = 2 the top row of each corner loses 2 units and the next row loses 1.
 */
class PixelCornerShape(private val unit: Dp = 3.dp, private val steps: Int = 2) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        // Whole screen pixels keep the stair edges crisp.
        val unitPx = max(1f, with(density) { unit.toPx() }.roundToInt().toFloat())
        val points = pixelCornerPoints(size.width, size.height, unitPx, steps)
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Vertices of a rectangle [w] x [h] whose four corners are cut in [steps] stairs of [unit] pixels.
 * The path starts on the top edge and runs clockwise. The unit shrinks on tiny boxes so corners never overlap.
 */
internal fun pixelCornerPoints(w: Float, h: Float, unit: Float, steps: Int): List<Offset> {
    val k = max(1, steps)
    val u = min(unit, min(w, h) / (2f * k))
    // Top-right corner, relative to the corner itself.
    val base = buildList {
        add(Offset(-k * u, 0f))
        for (r in 0 until k) {
            add(Offset(-(k - r) * u, (r + 1) * u))
            add(Offset(-(k - r - 1) * u, (r + 1) * u))
        }
    }
    val corners = listOf(Offset(w, 0f), Offset(w, h), Offset(0f, h), Offset(0f, 0f))
    val result = mutableListOf(Offset(k * u, 0f))
    corners.forEachIndexed { turn, corner ->
        base.forEach { p ->
            var x = p.x
            var y = p.y
            repeat(turn) {
                val nx = -y
                y = x
                x = nx
            }
            result.add(Offset(corner.x + x, corner.y + y))
        }
    }
    return result
}

/**
 * A hard, blur-free shadow: the same silhouette drawn [offset] down and to the right.
 * The offset is reserved as padding, so the content keeps its size and nothing gets clipped.
 */
fun Modifier.pixelShadow(shape: Shape, color: Color, offset: Dp = 3.dp): Modifier =
    this
        .drawBehind {
            val o = offset.toPx()
            val outline = shape.createOutline(Size(size.width - o, size.height - o), layoutDirection, this)
            translate(o, o) { drawOutline(outline, color) }
        }
        .padding(end = offset, bottom = offset)
