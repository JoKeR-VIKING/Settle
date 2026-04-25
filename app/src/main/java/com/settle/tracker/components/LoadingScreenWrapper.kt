package com.settle.tracker.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.settle.tracker.ui.animations.CoinLoader
import kotlinx.coroutines.delay

/**
 * Full-app loading overlay with brand CoinLoader and cycling tips
 * (perceived-performance trick during long API calls).
 */
@Composable
fun LoadingScreenWrapper(
    isLoading: Boolean,
    message: String = "Loading…",
    content: @Composable () -> Unit
) {
    // Cycle helpful tips every ~2.4s while loading
    val tips = remember {
        listOf(
            "Split smart. Settle fast.",
            "Every paisa tracked.",
            "Did you know you can split unequally?",
            "Your groups sync across devices.",
            "Recurring expenses never slip.",
        )
    }
    var tipIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(isLoading) {
        if (!isLoading) return@LaunchedEffect
        while (true) {
            delay(2400)
            tipIndex = (tipIndex + 1) % tips.size
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f))
                    .pointerInput(Unit) { detectTapGestures { } },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 28.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CoinLoader(message = message)

                    AnimatedContent(
                        targetState = tips[tipIndex],
                        transitionSpec = {
                            (fadeIn(tween(450)) togetherWith fadeOut(tween(350)))
                        },
                        label = "tip-cycle"
                    ) { tip ->
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
