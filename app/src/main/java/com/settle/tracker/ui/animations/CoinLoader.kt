package com.settle.tracker.ui.animations

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.settle.tracker.ui.theme.AccentButter
import com.settle.tracker.ui.theme.BrandBlue
import com.settle.tracker.ui.theme.BrandTeal
import kotlin.math.sin

/**
 * A "Lottie-style" loader drawn entirely in Compose — no third-party deps.
 * A spinning brand-gradient coin over 3 bouncing dots.
 */
@Composable
fun CoinLoader(
    size: Dp = 64.dp,
    message: String? = null,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "coin")
    val rotation by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "rot"
    )
    val flip by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing)),
        label = "flip"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer(rotationZ = rotation, rotationY = flip),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val r = this.size.minDimension / 2f
                // coin gradient disc
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(BrandTeal, BrandBlue),
                        start = Offset(0f, 0f),
                        end = Offset(this.size.width, this.size.height)
                    ),
                    radius = r
                )
                // inner ring
                drawCircle(
                    color = AccentButter.copy(alpha = 0.9f),
                    radius = r * 0.62f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.08f)
                )
                // glyph dot (S-like accent)
                drawCircle(
                    color = Color.White.copy(alpha = 0.95f),
                    radius = r * 0.18f,
                    center = Offset(this.size.width / 2f, this.size.height / 2f)
                )
            }
        }

        BouncingDots()

        if (!message.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun BouncingDots(
    dotColor: Color = BrandTeal,
) {
    val t = rememberInfiniteTransition(label = "dots")
    val p by t.animateFloat(
        initialValue = 0f, targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            tween(1000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "dots-p"
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(14.dp)
    ) {
        repeat(3) { i ->
            val phase = p + i * 0.6f
            val y = (sin(phase.toDouble()) * 6).toFloat().coerceAtLeast(0f)
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer(translationY = -y)
            ) {
                Canvas(modifier = Modifier.size(8.dp)) {
                    drawCircle(color = dotColor)
                }
            }
        }
    }
    Spacer(Modifier.width(0.dp))
}
