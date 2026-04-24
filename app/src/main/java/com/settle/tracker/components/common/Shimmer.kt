package com.settle.tracker.components.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun ExpenseListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 5
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(itemCount) {
            SkeletonExpenseCard()
        }
    }
}

@Composable
fun ProfileSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ShimmerBox(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape),
                shape = CircleShape
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.45f).height(18.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.55f).height(14.dp))
            }
        }

        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
        )

        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
        )
    }
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp)
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-offset"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ),
        start = Offset(shimmerOffset - 250f, shimmerOffset - 250f),
        end = Offset(shimmerOffset, shimmerOffset)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

@Composable
private fun SkeletonExpenseCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ShimmerBox(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape),
                shape = CircleShape
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.65f).height(18.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth(0.42f).height(14.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ShimmerBox(modifier = Modifier.size(width = 72.dp, height = 18.dp))
                ShimmerBox(modifier = Modifier.size(width = 54.dp, height = 14.dp))
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
    }
}
