package com.vidalvl.app.ui.creature

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import com.vidalvl.app.domain.model.CreatureMood
import com.vidalvl.app.domain.model.CreatureStage

/** Everything the drawing needs. Strengths go from 0 to 1. */
data class CreatureLook(
    val stage: CreatureStage,
    val mood: CreatureMood,
    val glow: Float,
    val body: Float,
    val mind: Float,
    val discipline: Float,
    val social: Float,
    val auraColor: Color,
    val sparkleColor: Color
)

@Composable
fun DragonView(look: CreatureLook, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "dragonBreath")
    val periodMs = when (look.mood) {
        CreatureMood.HAPPY -> 1800
        CreatureMood.NEUTRAL -> 2600
        CreatureMood.BORED -> 3600
        CreatureMood.SAD -> 4200
    }
    val breath = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(periodMs, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "breath"
    )
    val palette = remember(look.glow) { paletteFor(look.glow) }
    val spec = remember(look.stage) { specFor(look.stage) }

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .alpha((0.65f + 0.35f * look.glow).coerceIn(0f, 1f))
    ) {
        drawDragon(look, spec, palette, breath.value)
    }
}

// ---------- Stage shapes and colors ----------

private class StageSpec(
    val shell: Boolean,
    val headRx: Float, val headRy: Float, val headY: Float,
    val bodyY: Float, val bodyRx: Float, val bodyRy: Float,
    val wing: Float, val horn: Float, val snout: Float,
    val tail: Float, val spines: Int,
    val eyeRx: Float, val eyeRy: Float, val eyeOffset: Float
)

private fun specFor(stage: CreatureStage): StageSpec = when (stage) {
    CreatureStage.HATCHLING -> StageSpec(
        shell = true,
        headRx = 25f, headRy = 21f, headY = 46f,
        bodyY = 72f, bodyRx = 17f, bodyRy = 16f,
        wing = 0.35f, horn = 0f, snout = 0.3f,
        tail = 0.7f, spines = 0,
        eyeRx = 5.6f, eyeRy = 6.6f, eyeOffset = 0.38f
    )
    CreatureStage.CHILD -> StageSpec(
        shell = false,
        headRx = 21f, headRy = 18f, headY = 34f,
        bodyY = 65f, bodyRx = 17f, bodyRy = 22f,
        wing = 0.7f, horn = 8f, snout = 0.45f,
        tail = 1f, spines = 3,
        eyeRx = 4.6f, eyeRy = 5.6f, eyeOffset = 0.1f
    )
    CreatureStage.YOUTH -> StageSpec(
        shell = false,
        headRx = 19f, headRy = 16f, headY = 28f,
        bodyY = 63f, bodyRx = 21f, bodyRy = 25f,
        wing = 1.05f, horn = 11f, snout = 0.65f,
        tail = 1.2f, spines = 5,
        eyeRx = 4.2f, eyeRy = 4.8f, eyeOffset = 0.05f
    )
}

private class Palette(
    val bodyTop: Color, val bodyBottom: Color, val rim: Color,
    val crimson: Color, val crimsonDark: Color,
    val belly: Color, val bellyMuted: Color,
    val shellLight: Color, val shellDark: Color, val shellEdge: Color,
    val iris: Color, val eyeBack: Color, val mouth: Color
)

private fun Color.mute(amount: Float): Color {
    val gray = red * 0.3f + green * 0.59f + blue * 0.11f
    return lerp(Color(gray, gray, gray, alpha), this, amount)
}

/** Colors lose saturation as the glow drops (a sad dragon looks washed out). */
private fun paletteFor(glow: Float): Palette {
    val amount = 0.3f + 0.7f * glow.coerceIn(0f, 1f)
    fun c(argb: Long) = Color(argb).mute(amount)
    return Palette(
        bodyTop = c(0xFF2F2E3D), bodyBottom = c(0xFF16151C), rim = c(0xFF53506A),
        crimson = c(0xFFB51A2B), crimsonDark = c(0xFF6E1224),
        belly = c(0xFF9B1C2F), bellyMuted = c(0xFF4C4B5A),
        shellLight = c(0xFFF6EBD3), shellDark = c(0xFFD2BE97), shellEdge = c(0xFF9C8660),
        iris = c(0xFFD22B40), eyeBack = c(0xFF0C0B10), mouth = c(0xFFE0475B)
    )
}

private class Geo(val headY: Float, val bodyRx: Float, val bodyRy: Float)

// ---------- Drawing (everything uses a 100 x 100 coordinate space) ----------

private fun DrawScope.drawDragon(look: CreatureLook, s: StageSpec, p: Palette, breath: Float) {
    val unit = size.minDimension / 100f
    val headDy = when (look.mood) {
        CreatureMood.SAD -> 3.5f
        CreatureMood.BORED -> 1.8f
        else -> 0f
    }
    val g = Geo(
        headY = s.headY + headDy,
        bodyRx = s.bodyRx * (0.92f + 0.2f * look.body),
        bodyRy = s.bodyRy * (0.95f + 0.1f * look.body)
    )

    scale(scale = unit, pivot = Offset.Zero) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(look.auraColor.copy(alpha = 0.5f * look.glow), Color.Transparent),
                center = Offset(50f, 55f),
                radius = 50f
            ),
            radius = 50f,
            center = Offset(50f, 55f)
        )
        drawOval(Color.Black.copy(alpha = 0.3f), Offset(27f, 90f), Size(46f, 7f))

        val lift = (if (look.mood == CreatureMood.HAPPY) 2.4f else 1.2f) * breath
        translate(top = -lift) {
            drawTail(look, s, p, g)
            drawWings(look, s, p, g, breath)
            drawBody(s, p, g)
            drawHead(look, s, p, g, breath)
        }
        drawSparkles(look, breath)
    }
}

private fun DrawScope.spike(base: Offset, dir: Offset, width: Float, height: Float, color: Color) {
    val perp = Offset(-dir.y, dir.x)
    val path = Path().apply {
        moveTo(base.x + perp.x * width / 2f, base.y + perp.y * width / 2f)
        lineTo(base.x + dir.x * height, base.y + dir.y * height)
        lineTo(base.x - perp.x * width / 2f, base.y - perp.y * width / 2f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawTail(look: CreatureLook, s: StageSpec, p: Palette, g: Geo) {
    val len = s.tail
    val tx = 50f + g.bodyRx * 0.6f
    val ty = s.bodyY + g.bodyRy * 0.7f
    val width = 5f + 1.2f * len
    val tail = Path().apply {
        moveTo(tx, ty)
        cubicTo(tx + 16f * len, ty + 10f, tx + 30f * len, ty + 2f, tx + 24f * len, ty - 18f * len)
    }
    drawPath(tail, p.rim, style = Stroke(width = width + 1.2f, cap = StrokeCap.Round))
    drawPath(tail, p.bodyBottom, style = Stroke(width = width, cap = StrokeCap.Round))

    if (s.spines > 0) {
        val measure = PathMeasure()
        measure.setPath(tail, false)
        val total = measure.length
        for (i in 1..s.spines) {
            val f = 0.15f + 0.7f * (i - 1) / (s.spines - 1).toFloat()
            val pos = measure.getPosition(total * f)
            val tan = measure.getTangent(total * f)
            val normal = Offset(tan.y, -tan.x)
            val k = 1f - 0.45f * f
            spike(
                base = pos + normal * (width / 2f - 0.6f),
                dir = normal,
                width = 3.4f * k,
                height = (3f + 3f * look.discipline) * k,
                color = p.crimson
            )
        }
        spike(measure.getPosition(total), measure.getTangent(total), width + 1.2f, 5f, p.crimson)
    }
}

private fun DrawScope.drawWings(look: CreatureLook, s: StageSpec, p: Palette, g: Geo, breath: Float) {
    val w = s.wing * (0.85f + 0.3f * look.social)
    val droop = when (look.mood) {
        CreatureMood.SAD -> 14f
        CreatureMood.BORED -> 7f
        else -> 0f
    }
    val degrees = droop - 4f + 8f * breath
    val ax = 50f + g.bodyRx * 0.55f
    val ay = s.bodyY - g.bodyRy * 0.35f

    drawWing(ax, ay, w, degrees, p)
    scale(scaleX = -1f, scaleY = 1f, pivot = Offset(50f, 0f)) {
        drawWing(ax, ay, w, degrees, p)
    }
}

private fun DrawScope.drawWing(ax: Float, ay: Float, w: Float, degrees: Float, p: Palette) {
    rotate(degrees = degrees, pivot = Offset(ax, ay)) {
        val membrane = Path().apply {
            moveTo(ax, ay)
            quadraticTo(ax + 10f * w, ay - 20f * w, ax + 30f * w, ay - 24f * w)
            quadraticTo(ax + 24f * w, ay - 12f * w, ax + 29f * w, ay - 3f * w)
            quadraticTo(ax + 19f * w, ay - 4f * w, ax + 21f * w, ay + 9f * w)
            quadraticTo(ax + 12f * w, ay + 3f * w, ax, ay + 7f * w)
            close()
        }
        drawPath(membrane, p.crimson)
        drawPath(membrane, p.crimsonDark, style = Stroke(width = 0.9f))
        val origin = Offset(ax, ay)
        drawLine(p.bodyBottom, origin, Offset(ax + 30f * w, ay - 24f * w), 1.1f, StrokeCap.Round)
        drawLine(p.bodyBottom, origin, Offset(ax + 29f * w, ay - 3f * w), 1.1f, StrokeCap.Round)
        drawLine(p.bodyBottom, origin, Offset(ax + 21f * w, ay + 9f * w), 1.1f, StrokeCap.Round)
    }
}

private fun DrawScope.drawBody(s: StageSpec, p: Palette, g: Geo) {
    val cy = s.bodyY

    // Feet
    for (side in listOf(-1f, 1f)) {
        val fx = 50f + side * g.bodyRx * 0.55f
        drawOval(p.bodyBottom, Offset(fx - 8f, 85.5f), Size(16f, 9f))
        drawOval(p.rim, Offset(fx - 8f, 85.5f), Size(16f, 9f), style = Stroke(0.7f))
        for (i in -1..1) drawCircle(p.crimson, 0.9f, Offset(fx + i * 3.6f, 93.2f))
    }

    // Torso
    val topLeft = Offset(50f - g.bodyRx, cy - g.bodyRy)
    val bodySize = Size(g.bodyRx * 2f, g.bodyRy * 2f)
    drawOval(
        Brush.verticalGradient(
            listOf(p.bodyTop, p.bodyBottom),
            startY = cy - g.bodyRy,
            endY = cy + g.bodyRy
        ),
        topLeft,
        bodySize
    )
    drawOval(p.rim, topLeft, bodySize, style = Stroke(0.8f))

    // Belly with scale lines
    val brx = g.bodyRx * 0.62f
    val bry = g.bodyRy * 0.8f
    val bcy = cy + g.bodyRy * 0.12f
    drawOval(
        if (s.shell) p.bellyMuted else p.belly,
        Offset(50f - brx, bcy - bry),
        Size(brx * 2f, bry * 2f)
    )
    val lineColor = if (s.shell) p.bodyBottom else p.crimsonDark
    for (i in 0..3) {
        val y = bcy - bry * 0.55f + i * bry * 0.38f
        drawArc(
            lineColor, 20f, 140f, false,
            Offset(50f - brx * 0.8f, y - 2.2f), Size(brx * 1.6f, 4.4f),
            style = Stroke(0.8f)
        )
    }

    // Arms
    for (side in listOf(-1f, 1f)) {
        val ax = 50f + side * g.bodyRx * 0.95f
        val ay = cy + g.bodyRy * 0.05f
        rotate(degrees = -side * 18f, pivot = Offset(ax, ay)) {
            drawOval(p.bodyBottom, Offset(ax - 4.5f, ay - 8f), Size(9f, 16f))
            drawOval(p.rim, Offset(ax - 4.5f, ay - 8f), Size(9f, 16f), style = Stroke(0.7f))
        }
    }
}

private fun DrawScope.drawHead(look: CreatureLook, s: StageSpec, p: Palette, g: Geo, breath: Float) {
    val hy = g.headY
    val headTopLeft = Offset(50f - s.headRx, hy - s.headRy)
    val headSize = Size(s.headRx * 2f, s.headRy * 2f)

    // Horns and crest (behind the head)
    if (s.horn > 0f) {
        val hl = s.horn * (0.8f + 0.4f * look.discipline)
        val bx = 50f + s.headRx * 0.5f
        val by = hy - s.headRy * 0.78f
        val horn = Path().apply {
            moveTo(bx - 3f, by + 2f)
            quadraticTo(bx + 1f, by - hl * 0.6f, bx + hl * 0.55f, by - hl)
            quadraticTo(bx + 5.5f, by - hl * 0.25f, bx + 3.5f, by + 2f)
            close()
        }
        drawPath(horn, p.crimsonDark)
        scale(scaleX = -1f, scaleY = 1f, pivot = Offset(50f, 0f)) {
            drawPath(horn, p.crimsonDark)
        }
    }
    val crest = when {
        s.spines >= 5 -> 3
        s.spines >= 3 -> 1
        else -> 0
    }
    for (i in 0 until crest) {
        val x = 50f + (i - (crest - 1) / 2f) * 5.5f
        spike(
            Offset(x, hy - s.headRy + 1.5f), Offset(0f, -1f),
            3.8f, 3.2f + 3.5f * look.discipline, p.crimson
        )
    }

    // Head
    drawOval(
        Brush.verticalGradient(
            listOf(p.bodyTop, p.bodyBottom),
            startY = hy - s.headRy,
            endY = hy + s.headRy
        ),
        headTopLeft,
        headSize
    )
    drawOval(p.rim, headTopLeft, headSize, style = Stroke(0.8f))

    // Snout
    val snoutRx = s.headRx * (0.30f + 0.3f * s.snout)
    val snoutRy = s.headRy * (0.25f + 0.2f * s.snout)
    val snoutY = hy + s.headRy * 0.55f
    val snoutTopLeft = Offset(50f - snoutRx, snoutY - snoutRy)
    val snoutSize = Size(snoutRx * 2f, snoutRy * 2f)
    drawOval(p.bodyTop, snoutTopLeft, snoutSize)
    drawOval(p.rim, snoutTopLeft, snoutSize, style = Stroke(0.7f))
    drawCircle(p.bodyBottom, 0.8f, Offset(50f - 2.6f, snoutY - snoutRy * 0.15f))
    drawCircle(p.bodyBottom, 0.8f, Offset(50f + 2.6f, snoutY - snoutRy * 0.15f))

    // Eyes, brows and cheeks
    val eyeY = hy + s.headRy * s.eyeOffset
    val eyeDx = s.headRx * 0.45f
    for (side in listOf(-1f, 1f)) {
        val ex = 50f + side * eyeDx
        val rx = s.eyeRx
        val ry = if (look.mood == CreatureMood.HAPPY) s.eyeRy * 1.05f else s.eyeRy

        drawOval(p.eyeBack, Offset(ex - rx, eyeY - ry), Size(rx * 2f, ry * 2f))
        drawOval(p.iris, Offset(ex - rx * 0.8f, eyeY - ry * 0.75f), Size(rx * 1.6f, ry * 1.55f))
        drawOval(p.eyeBack, Offset(ex - rx * 0.38f, eyeY - ry * 0.4f), Size(rx * 0.76f, ry * 0.9f))
        drawCircle(Color.White, rx * 0.26f, Offset(ex - rx * 0.28f, eyeY - ry * 0.32f))
        if (look.mood == CreatureMood.HAPPY || look.mood == CreatureMood.SAD) {
            drawCircle(
                Color.White.copy(alpha = 0.85f), rx * 0.14f,
                Offset(ex + rx * 0.3f, eyeY + ry * 0.3f)
            )
        }

        if (look.mood == CreatureMood.BORED) {
            // Heavy eyelid
            drawArc(
                p.bodyTop, 180f, 180f, true,
                Offset(ex - rx * 1.15f, eyeY - ry * 1.15f), Size(rx * 2.3f, ry * 2.3f)
            )
            drawLine(
                p.rim, Offset(ex - rx * 1.1f, eyeY), Offset(ex + rx * 1.1f, eyeY),
                0.9f, StrokeCap.Round
            )
        }

        if (!s.shell) {
            val browY = eyeY - ry - 2.2f
            val outer = ex + side * rx * 1.1f
            val inner = ex - side * rx * 1.1f
            when (look.mood) {
                CreatureMood.HAPPY -> drawArc(
                    p.crimson, 205f, 130f, false,
                    Offset(ex - rx * 1.1f, browY - 2.2f), Size(rx * 2.2f, 4.4f),
                    style = Stroke(1.2f, cap = StrokeCap.Round)
                )
                CreatureMood.NEUTRAL -> drawLine(
                    p.crimson, Offset(inner, browY), Offset(outer, browY - 0.6f),
                    1.2f, StrokeCap.Round
                )
                CreatureMood.BORED -> drawLine(
                    p.crimson, Offset(inner, browY + 1.2f), Offset(outer, browY + 1.2f),
                    1.2f, StrokeCap.Round
                )
                CreatureMood.SAD -> drawLine(
                    p.crimson, Offset(outer, browY + 1.2f), Offset(inner, browY - 2f),
                    1.2f, StrokeCap.Round
                )
            }
        }

        if (look.mood == CreatureMood.HAPPY) {
            drawCircle(
                p.crimson.copy(alpha = 0.4f), 2.4f,
                Offset(ex + side * rx * 1.0f, eyeY + ry + 1.6f)
            )
        }
        if (look.mood == CreatureMood.SAD && side < 0f) {
            drawCircle(
                Color(0xFF9AD8F5).copy(alpha = 0.9f - 0.5f * breath), 1.5f,
                Offset(ex - rx * 0.3f, eyeY + ry + 1.2f + 4.5f * breath)
            )
        }
    }

    // Mouth
    val mx = 50f
    val my = snoutY + snoutRy * 0.4f
    val stroke = Stroke(width = 1.1f, cap = StrokeCap.Round)
    when (look.mood) {
        CreatureMood.HAPPY -> drawArc(
            p.mouth, 15f, 150f, false, Offset(mx - 4.5f, my - 3.5f), Size(9f, 7f), style = stroke
        )
        CreatureMood.NEUTRAL -> drawArc(
            p.mouth, 30f, 120f, false, Offset(mx - 3f, my - 2f), Size(6f, 4f), style = stroke
        )
        CreatureMood.BORED -> drawLine(
            p.mouth, Offset(mx - 2.8f, my), Offset(mx + 2.8f, my), 1.1f, StrokeCap.Round
        )
        CreatureMood.SAD -> drawArc(
            p.mouth, 200f, 140f, false, Offset(mx - 3.5f, my - 0.5f), Size(7f, 5f), style = stroke
        )
    }

    // Eggshell on the head (hatchling only)
    if (s.shell) {
        val rx = s.headRx
        val ry = s.headRy
        val shell = Path().apply {
            moveTo(50f + rx * 0.98f, hy - ry * 0.1f)
            cubicTo(
                50f + rx * 0.98f, hy - ry * 1.6f,
                50f - rx * 0.98f, hy - ry * 1.6f,
                50f - rx * 0.98f, hy - ry * 0.1f
            )
            lineTo(50f - rx * 0.7f, hy + ry * 0.12f)
            lineTo(50f - rx * 0.4f, hy - ry * 0.1f)
            lineTo(50f - rx * 0.12f, hy + ry * 0.14f)
            lineTo(50f + rx * 0.2f, hy - ry * 0.1f)
            lineTo(50f + rx * 0.5f, hy + ry * 0.12f)
            lineTo(50f + rx * 0.75f, hy - ry * 0.08f)
            close()
        }
        drawPath(
            shell,
            Brush.verticalGradient(
                listOf(p.shellLight, p.shellDark),
                startY = hy - ry * 1.3f,
                endY = hy + ry * 0.1f
            )
        )
        drawPath(shell, p.shellEdge, style = Stroke(width = 0.8f))
        val crack = Path().apply {
            moveTo(50f - rx * 0.1f, hy - ry * 1.2f)
            lineTo(50f + rx * 0.06f, hy - ry * 0.98f)
            lineTo(50f - rx * 0.05f, hy - ry * 0.82f)
            lineTo(50f + rx * 0.08f, hy - ry * 0.6f)
        }
        drawPath(crack, p.shellEdge, style = Stroke(width = 0.7f, cap = StrokeCap.Round))
    }
}

/** Sparkles around the dragon: the stronger the Mind attribute, the more of them. */
private fun DrawScope.drawSparkles(look: CreatureLook, breath: Float) {
    val count = (look.mind * 3f).toInt() + if (look.mind > 0.15f) 1 else 0
    val spots = listOf(Offset(18f, 22f), Offset(83f, 26f), Offset(13f, 54f), Offset(87f, 52f))
    for (i in 0 until count.coerceAtMost(spots.size)) {
        val twinkle = if (i % 2 == 0) breath else 1f - breath
        val c = spots[i]
        val r = 2.6f + 1.6f * twinkle
        val star = Path().apply {
            moveTo(c.x, c.y - r)
            lineTo(c.x + r * 0.28f, c.y - r * 0.28f)
            lineTo(c.x + r, c.y)
            lineTo(c.x + r * 0.28f, c.y + r * 0.28f)
            lineTo(c.x, c.y + r)
            lineTo(c.x - r * 0.28f, c.y + r * 0.28f)
            lineTo(c.x - r, c.y)
            lineTo(c.x - r * 0.28f, c.y - r * 0.28f)
            close()
        }
        drawPath(star, look.sparkleColor.copy(alpha = 0.35f + 0.65f * twinkle))
    }
}