package org.ledor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.ledor.domain.event.EventType
import org.ledor.ui.theme.KalendarColors
import kotlin.math.cos
import kotlin.math.sin

/** Маген Давид: два равносторонних треугольника в области 24×24 (docs/06-design-system.md §4). */
@Composable
fun MagenDavid(modifier: Modifier = Modifier, color: Color, strokeWidth: Dp = 1.5.dp, filled: Boolean = false) {
    Canvas(modifier) { drawMagenDavid(color, strokeWidth.toPx(), filled) }
}

fun DrawScope.drawMagenDavid(color: Color, strokePx: Float, filled: Boolean = false, center: Offset = this.center, sizePx: Float = size.minDimension) {
    val s = sizePx / 24f
    val ox = center.x - 12f * s
    val oy = center.y - 12f * s
    fun p(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)
    val up = Path().apply { moveTo(p(12f, 2f).x, p(12f, 2f).y); lineTo(p(20.66f, 17f).x, p(20.66f, 17f).y); lineTo(p(3.34f, 17f).x, p(3.34f, 17f).y); close() }
    val down = Path().apply { moveTo(p(12f, 22f).x, p(12f, 22f).y); lineTo(p(3.34f, 7f).x, p(3.34f, 7f).y); lineTo(p(20.66f, 7f).x, p(20.66f, 7f).y); close() }
    val style = if (filled) Fill else Stroke(width = strokePx, join = StrokeJoin.Round)
    drawPath(up, color, style = style)
    drawPath(down, color, style = style)
}

/**
 * Знак LeDor: лист календаря с золотым Маген Давидом на тхелет — та же геометрия, что у иконки приложения
 * (res/drawable/ic_launcher_foreground.xml, область 108, видимая часть 21…87).
 */
@Composable
fun LedorMark(modifier: Modifier = Modifier, size: Dp = 96.dp) {
    Box(modifier.size(size).clip(RoundedCornerShape(size * 0.24f))) {
        Canvas(Modifier.size(size)) {
            val k = this.size.width / 66f
            fun o(x: Float, y: Float) = Offset((x - 21f) * k, (y - 21f) * k)
            drawRect(Brush.linearGradient(listOf(Color(0xFF1450C8), Color(0xFF072C7C)), Offset.Zero, Offset(this.size.width, this.size.height)))
            // лист календаря
            drawRoundRect(Color.White, o(31f, 34f), Size(46f * k, 42f * k), CornerRadius(5f * k))
            drawRoundRect(Color(0xFFDCE6FF), o(31f, 34f), Size(46f * k, 11f * k), CornerRadius(5f * k))
            drawRect(Color(0xFFDCE6FF), o(31f, 40f), Size(46f * k, 5f * k))
            drawRoundRect(Color(0xFF0B3FAE), o(40f, 29.5f), Size(5f * k, 9f * k), CornerRadius(1f * k))
            drawRoundRect(Color(0xFF0B3FAE), o(63f, 29.5f), Size(5f * k, 9f * k), CornerRadius(1f * k))
            // Маген Давид: центр (54; 60,5), радиус 11
            drawMagenDavid(Color(0xFFD4A72C), 2.4f * k, center = o(54f, 60.5f), sizePx = 22f * k / 0.833f)
        }
    }
}

/** Значок типа события в цветном кружке. */
@Composable
fun EventTypeIcon(type: EventType, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val color = KalendarColors.extra.forType(type)
    Box(modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
        EventTypeGlyph(type, color, Modifier.size(size * 0.6f))
    }
}

@Composable
fun EventTypeGlyph(type: EventType, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val u = size.minDimension / 24f
        val st = Stroke(width = 1.8f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun o(x: Float, y: Float) = Offset(x * u, y * u)
        when (type) {
            EventType.BIRTHDAY -> {
                drawRoundRect(color, o(4f, 12f), Size(16f * u, 9f * u), CornerRadius(2f * u), style = st)
                drawLine(color, o(4f, 16f), o(20f, 16f), 1.8f * u)
                for (x in listOf(8f, 12f, 16f)) {
                    drawLine(color, o(x, 12f), o(x, 8f), 1.8f * u, StrokeCap.Round)
                    drawOval(color, o(x - 1.1f, 4f), Size(2.2f * u, 3f * u))
                }
            }
            EventType.YAHRZEIT -> {
                drawRoundRect(color, o(8.5f, 10f), Size(7f * u, 11f * u), CornerRadius(1.5f * u), style = st)
                val flame = Path().apply {
                    moveTo(12f * u, 2.5f * u)
                    cubicTo(15f * u, 5.5f * u, 14.5f * u, 8.5f * u, 12f * u, 8.5f * u)
                    cubicTo(9.5f * u, 8.5f * u, 9f * u, 5.5f * u, 12f * u, 2.5f * u)
                }
                drawPath(flame, color)
            }
            EventType.BAR_MITZVAH -> {
                drawLine(color, o(5f, 3f), o(5f, 21f), 2.2f * u, StrokeCap.Round)
                drawLine(color, o(19f, 3f), o(19f, 21f), 2.2f * u, StrokeCap.Round)
                drawRect(color, o(7f, 5f), Size(10f * u, 14f * u), style = st)
                drawLine(color, o(9.5f, 9f), o(14.5f, 9f), 1.4f * u, StrokeCap.Round)
                drawLine(color, o(9.5f, 12f), o(14.5f, 12f), 1.4f * u, StrokeCap.Round)
                drawLine(color, o(9.5f, 15f), o(13f, 15f), 1.4f * u, StrokeCap.Round)
            }
            EventType.BAT_MITZVAH -> {
                drawLine(color, o(4f, 21f), o(20f, 21f), 1.8f * u, StrokeCap.Round)
                drawRoundRect(color, o(6.5f, 10f), Size(4f * u, 11f * u), CornerRadius(1f * u), style = st)
                drawRoundRect(color, o(13.5f, 10f), Size(4f * u, 11f * u), CornerRadius(1f * u), style = st)
                drawOval(color, o(7.4f, 4f), Size(2.2f * u, 4f * u))
                drawOval(color, o(14.4f, 4f), Size(2.2f * u, 4f * u))
            }
            EventType.WEDDING -> {
                drawCircle(color, 5.5f * u, o(9f, 14f), style = st)
                drawCircle(color, 5.5f * u, o(15f, 14f), style = st)
                val gem = Path().apply { moveTo(9f * u, 3f * u); lineTo(11f * u, 5.5f * u); lineTo(9f * u, 8f * u); lineTo(7f * u, 5.5f * u); close() }
                drawPath(gem, color)
            }
            EventType.OTHER -> {
                val star = Path()
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) 9.5f else 4f
                    val a = Math.toRadians(-90.0 + i * 36.0)
                    val x = 12f + r * cos(a).toFloat()
                    val y = 12.5f + r * sin(a).toFloat()
                    if (i == 0) star.moveTo(x * u, y * u) else star.lineTo(x * u, y * u)
                }
                star.close()
                drawPath(star, color, style = st)
            }
        }
    }
}

/** Солнце/луна для переключателя захода. */
@Composable
fun SunGlyph(color: Color, modifier: Modifier = Modifier, moon: Boolean = false) {
    Canvas(modifier.size(18.dp)) {
        val u = size.minDimension / 24f
        if (moon) {
            val p = Path().apply {
                moveTo(15f * u, 3f * u)
                cubicTo(6f * u, 3f * u, 3f * u, 14f * u, 9f * u, 19f * u)
                cubicTo(13f * u, 22.5f * u, 19.5f * u, 21f * u, 21f * u, 16f * u)
                cubicTo(14f * u, 18f * u, 9f * u, 10f * u, 15f * u, 3f * u)
            }
            drawPath(p, color)
        } else {
            drawCircle(color, 4.5f * u, Offset(12f * u, 12f * u))
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0)
                drawLine(color, Offset((12 + 7 * cos(a)).toFloat() * u, (12 + 7 * sin(a)).toFloat() * u),
                    Offset((12 + 10 * cos(a)).toFloat() * u, (12 + 10 * sin(a)).toFloat() * u), 1.8f * u, StrokeCap.Round)
            }
        }
    }
}
