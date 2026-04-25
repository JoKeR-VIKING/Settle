package com.settle.tracker.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.settle.tracker.ui.animations.ShimmerBox
import com.settle.tracker.ui.animations.ShimmerCircle

@Composable
fun ExpenseRowSkeleton(modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val hl = MaterialTheme.colorScheme.surface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ShimmerCircle(size = 40.dp, baseColor = base, highlightColor = hl)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShimmerBox(baseColor = base, highlightColor = hl, width = 160.dp, height = 14.dp)
            ShimmerBox(baseColor = base, highlightColor = hl, width = 220.dp, height = 12.dp)
        }
    }
}

@Composable
fun ExpenseListSkeleton(count: Int = 6, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(Modifier.height(12.dp))
        repeat(count) {
            ExpenseRowSkeleton()
        }
    }
}

@Composable
fun GroupRowSkeleton(modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val hl = MaterialTheme.colorScheme.surface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        ShimmerCircle(size = 28.dp, baseColor = base, highlightColor = hl)
        ShimmerBox(baseColor = base, highlightColor = hl, width = 140.dp, height = 14.dp)
        Spacer(Modifier.fillMaxWidth(0f))
    }
}

@Composable
fun AnalyticsCardSkeleton(modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val hl = MaterialTheme.colorScheme.surface
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShimmerBox(baseColor = base, highlightColor = hl, width = 180.dp, height = 18.dp)
        ShimmerBox(baseColor = base, highlightColor = hl, height = 180.dp)
    }
}
