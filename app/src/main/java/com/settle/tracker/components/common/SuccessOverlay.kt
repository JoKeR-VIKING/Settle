package com.settle.tracker.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.settle.tracker.ui.theme.AccentButter
import com.settle.tracker.ui.theme.AccentCoral
import com.settle.tracker.ui.theme.AccentLilac
import com.settle.tracker.ui.theme.AccentLime
import com.settle.tracker.ui.theme.BrandBlue
import com.settle.tracker.ui.theme.BrandTeal
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Full-screen success celebration: animated check + confetti burst + message.
 * Auto-dismisses after ~1.4s. Use to reward the user on expense add / settle.
 */
@Composable
fun SuccessOverlay(
    visible: Boolean,
    message: String,
    onDismiss: () -> Unit
) {
    if (visible) {
        LaunchedEffect(Unit) {
            delay(1400)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(250))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Confetti()
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedCheckCircle()
                Spacer(Modifier.height(18.dp))
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(300, delayMillis = 200)) + scaleIn(initialScale = 0.85f),
                    exit = fadeOut() + scaleOut()
                ) {
                    Text(
                        message,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedCheckCircle() {
    var target by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { target = 1f }

    val circleScale by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "circle-scale"
    )
    val checkProgress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(520, delayMillis = 180, easing = FastOutSlowInEasing),
        label = "check-progress"
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(BrandTeal, BrandBlue))),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size((110 * circleScale).dp)) {
            if (circleScale <= 0.01f) return@Canvas

            val w = size.width
            val h = size.height
            val strokeWidth = w * 0.09f
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

            val p1 = Offset(w * 0.28f, h * 0.54f)
            val p2 = Offset(w * 0.46f, h * 0.70f)
            val p3 = Offset(w * 0.74f, h * 0.36f)

            // Animate: first segment reveals 0..0.5, second 0.5..1
            val leg1 = (checkProgress / 0.5f).coerceIn(0f, 1f)
            val leg2 = ((checkProgress - 0.5f) / 0.5f).coerceIn(0f, 1f)

            if (leg1 > 0f) {
                val end1 = Offset(
                    p1.x + (p2.x - p1.x) * leg1,
                    p1.y + (p2.y - p1.y) * leg1
                )
                drawLine(Color.White, p1, end1, strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            if (leg2 > 0f) {
                val end2 = Offset(
                    p2.x + (p3.x - p2.x) * leg2,
                    p2.y + (p3.y - p2.y) * leg2
                )
                drawLine(Color.White, p2, end2, strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun Confetti() {
    val colors = listOf(AccentButter, AccentLime, AccentCoral, AccentLilac, BrandTeal, BrandBlue)
    val particles = remember {
        List(26) { idx ->
            ConfettiParticle(
                angle = Random.nextDouble(-Math.PI, Math.PI).toFloat(),
                distance = Random.nextInt(180, 380).toFloat(),
                spin = Random.nextDouble(-720.0, 720.0).toFloat(),
                color = colors[idx % colors.size],
                sizePx = Random.nextInt(6, 13).toFloat()
            )
        }
    }

    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val steps = 42
        repeat(steps) {
            delay(20L)
            progress = (it + 1f) / steps
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val gravity = progress * progress * 140f
        particles.forEach { p ->
            val dist = p.distance * easeOutCubic(progress)
            val x = cx + cos(p.angle.toDouble()).toFloat() * dist
            val y = cy + sin(p.angle.toDouble()).toFloat() * dist + gravity
            val alpha = (1f - progress * 0.9f).coerceIn(0f, 1f)
            rotate(degrees = p.spin * progress, pivot = Offset(x, y)) {
                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(x - p.sizePx / 2, y - p.sizePx / 2),
                    size = Size(p.sizePx, p.sizePx * 0.5f)
                )
            }
        }
    }
}

private data class ConfettiParticle(
    val angle: Float,
    val distance: Float,
    val spin: Float,
    val color: Color,
    val sizePx: Float
)

private fun easeOutCubic(t: Float): Float {
    val p = (t - 1f)
    return 1f + p * p * p
}
