package org.ledor.ui.about

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ledor.R
import org.ledor.ui.theme.KalendarColors
import kotlin.math.hypot
import kotlin.random.Random

/** Сколько раз нажать на знак, чтобы открыть пасхалку, и сколько раз попасть по Ецер а-Ра (ABT-6). */
const val EGG_STEPS = 7
/** Подсказка «Ещё 3…» появляется, когда осталось столько нажатий и меньше. */
const val EGG_HINT_FROM = 3

/** Счётчик нажатий на знак: [EGG_STEPS] подряд; пауза дольше [windowMs] — счёт заново. */
class EggTaps(private val windowMs: Long = 2_000) {
    private var count = 0
    private var last = Long.MIN_VALUE / 2

    /** Сколько нажатий осталось после этого; 0 — пасхалка открывается. */
    fun tap(nowMs: Long): Int {
        if (nowMs - last > windowMs) count = 0
        last = nowMs
        count++
        val left = EGG_STEPS - count
        if (left <= 0) count = 0
        return left.coerceAtLeast(0)
    }
}

/**
 * Пасхалка (ABT-6, D-57): Ецер а-Ра шепчет отговорки — «позвонишь завтра». От каждого попадания он сжимается и
 * отскакивает; после седьмого исчезает, и появляется «Кто силён? Тот, кто побеждает своё влечение» (Авот 4:1).
 * Персонаж нарисован здесь же, картинок нет. Работает, только пока открыт.
 */
@Composable
fun YetzerGame(onClose: () -> Unit, random: Random = remember { Random(System.nanoTime()) }) {
    var hits by remember { mutableIntStateOf(0) }
    var spot by remember { mutableStateOf(Offset(0.5f, 0.4f)) }
    val excuses = stringArrayResource(R.array.egg_excuses)
    val haptics = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme
    // Пустой обработчик — чтобы нажатия мимо персонажа не проваливались на экран под пасхалкой.
    Box(Modifier.fillMaxSize().background(colors.background).pointerInput(Unit) { detectTapGestures { } }) {
        if (hits < EGG_STEPS) {
            Surface(
                shape = RoundedCornerShape(50), color = colors.surfaceVariant,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp, start = 24.dp, end = 24.dp),
            ) {
                Text(
                    excuses[hits.coerceAtMost(excuses.lastIndex)], fontStyle = FontStyle.Italic, textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
            BoxWithConstraints(Modifier.fillMaxSize().padding(top = 148.dp, bottom = 64.dp, start = 16.dp, end = 16.dp)) {
                val size = (136 - hits * 12).dp
                val jump = spring<Dp>(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)
                val x by animateDpAsState((maxWidth - size) * spot.x, jump, label = "x")
                val y by animateDpAsState((maxHeight - size) * spot.y, jump, label = "y")
                val bob by rememberInfiniteTransition(label = "bob").animateFloat(
                    -6f, 6f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob",
                )
                val squash = remember { Animatable(1f) }
                LaunchedEffect(hits) {
                    if (hits > 0) { squash.snapTo(0.7f); squash.animateTo(1f, spring(dampingRatio = 0.35f)) }
                }
                val name = stringResource(R.string.egg_yetzer_name)
                Yetzer(
                    size,
                    Modifier.offset { IntOffset(x.roundToPx(), y.roundToPx()) }.size(size)
                        .graphicsLayer { translationY = bob.dp.toPx(); scaleY = squash.value; scaleX = 2f - squash.value }
                        .testTag("yetzer").semantics { contentDescription = name }
                        .pointerInput(Unit) {
                            detectTapGestures {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                hits++
                                spot = nextSpot(spot, random)
                            }
                        },
                )
            }
            Text(
                "$hits / $EGG_STEPS", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
            )
        } else {
            Column(
                Modifier.align(Alignment.Center).padding(32.dp).testTag("egg_quote"),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val hebrew = stringResource(R.string.egg_quote_he)
                val translated = stringResource(R.string.egg_quote)
                // Направление — по тексту: иначе в интерфейсе слева направо точка уезжает в начало фразы.
                Text(hebrew, style = MaterialTheme.typography.headlineSmall.copy(textDirection = TextDirection.Content), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                if (translated != hebrew) Text(translated, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text(stringResource(R.string.egg_quote_source), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.egg_back), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Button(onClick = onClose, modifier = Modifier.testTag("egg_done")) { Text(stringResource(R.string.done)) }
            }
        }
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            Icon(Icons.Filled.Close, stringResource(R.string.close), tint = colors.onBackground)
        }
    }
}

/** Новое место заметно в стороне от прежнего (доли свободного поля). */
private fun nextSpot(from: Offset, random: Random): Offset {
    repeat(20) {
        val to = Offset(random.nextFloat(), random.nextFloat())
        if (hypot(to.x - from.x, to.y - from.y) > 0.35f) return to
    }
    return Offset(1f - from.x, 1f - from.y)
}

/** Ецер а-Ра: тёмный клуб дыма с хвостом, хитрыми глазами и сокращением יצה״ר. Свой персонаж, не копия чужих. */
@Composable
private fun Yetzer(size: Dp, modifier: Modifier = Modifier) {
    val gold = KalendarColors.extra.gold
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val lilac = Color(0xFFB79CFF)
            drawCircle(Brush.radialGradient(listOf(lilac.copy(alpha = 0.45f), Color.Transparent), radius = w * 0.8f), radius = w * 0.8f)
            // Клуб дыма: круглая голова, книзу — хвост, уходящий в сторону.
            val body = Path().apply {
                moveTo(w * 0.50f, h * 0.08f)
                cubicTo(w * 0.85f, h * 0.08f, w * 0.96f, h * 0.40f, w * 0.86f, h * 0.60f)
                cubicTo(w * 0.80f, h * 0.76f, w * 0.62f, h * 0.80f, w * 0.52f, h * 0.88f)
                cubicTo(w * 0.44f, h * 0.94f, w * 0.36f, h * 0.99f, w * 0.20f, h * 0.97f)
                cubicTo(w * 0.31f, h * 0.92f, w * 0.33f, h * 0.84f, w * 0.27f, h * 0.76f)
                cubicTo(w * 0.09f, h * 0.62f, w * 0.05f, h * 0.36f, w * 0.20f, h * 0.20f)
                cubicTo(w * 0.28f, h * 0.11f, w * 0.38f, h * 0.08f, w * 0.50f, h * 0.08f)
                close()
            }
            drawPath(body, Brush.verticalGradient(listOf(Color(0xFF2E2650), Color(0xFF0C0A17))))
            drawPath(body, lilac.copy(alpha = 0.7f), style = Stroke(w * 0.018f))
            // Вихор дыма на макушке.
            val curl = Path().apply {
                moveTo(w * 0.50f, h * 0.10f)
                cubicTo(w * 0.44f, h * -0.07f, w * 0.70f, h * -0.09f, w * 0.63f, h * 0.03f)
            }
            drawPath(curl, Color(0xFF2E2650), style = Stroke(w * 0.07f, cap = StrokeCap.Round))
            drawPath(curl, lilac.copy(alpha = 0.7f), style = Stroke(w * 0.018f, cap = StrokeCap.Round))
            for (side in listOf(-1f, 1f)) {
                val cx = w * (0.5f + side * 0.15f)
                drawOval(Color.White, Offset(cx - w * 0.085f, h * 0.26f), Size(w * 0.17f, h * 0.20f))
                drawCircle(Color(0xFF0C0A17), w * 0.045f, Offset(cx - side * w * 0.025f, h * 0.385f))
                // Брови домиком к переносице — хитрый взгляд.
                drawLine(lilac, Offset(cx + side * w * 0.11f, h * 0.19f), Offset(cx - side * w * 0.09f, h * 0.26f), w * 0.03f, StrokeCap.Round)
            }
            val smirk = Path().apply {
                moveTo(w * 0.40f, h * 0.53f)
                quadraticTo(w * 0.54f, h * 0.61f, w * 0.64f, h * 0.50f)
            }
            drawPath(smirk, lilac, style = Stroke(w * 0.025f, cap = StrokeCap.Round))
        }
        Text(
            stringResource(R.string.egg_yetzer_abbr), color = gold, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.15f).sp,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = size * 0.60f, start = size * 0.06f),
        )
    }
}
