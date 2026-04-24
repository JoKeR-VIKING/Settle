package com.settle.tracker.ui.animations

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.bounceClickable(
    enabled: Boolean = true,
    haptic: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "bounce-scale"
    )
    val hapticFeedback = LocalHapticFeedback.current
    this
        .graphicsLayer(scaleX = scale, scaleY = scale)
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled
        ) {
            if (haptic) hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
}

fun Modifier.breathing(
    minScale: Float = 0.96f,
    maxScale: Float = 1.04f,
    durationMs: Int = 2200
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "breathing")
    val s by transition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )
    graphicsLayer(scaleX = s, scaleY = s)
}

fun Modifier.pulse(enabled: Boolean = true): Modifier = composed {
    if (!enabled) return@composed this
    val transition = rememberInfiniteTransition(label = "pulse")
    val s by transition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulse-s"
    )
    graphicsLayer(scaleX = s, scaleY = s)
}

@Composable
fun shimmerBrush(
    baseColor: Color,
    highlightColor: Color
): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val t by transition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmer-t"
    )
    return Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(t - 400f, 0f),
        end   = Offset(t, 300f)
    )
}

@Composable
fun ShimmerBox(
    baseColor: Color,
    highlightColor: Color,
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 16.dp,
    radius: Dp = 8.dp
) {
    val brush = shimmerBrush(baseColor, highlightColor)
    var m: Modifier = modifier
        .height(height)
        .clip(RoundedCornerShape(radius))
        .background(brush)
    if (width != null) m = m.width(width)
    Box(modifier = m) {}
}

@Composable
fun ShimmerCircle(
    size: Dp,
    baseColor: Color,
    highlightColor: Color,
    modifier: Modifier = Modifier
) {
    val brush = shimmerBrush(baseColor, highlightColor)
    Box(
        modifier = modifier
            .size(size)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(brush)
    )
}
