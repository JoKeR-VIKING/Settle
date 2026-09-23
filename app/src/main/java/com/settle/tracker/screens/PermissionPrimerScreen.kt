package com.settle.tracker.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.settle.tracker.ui.theme.BrandBlue
import com.settle.tracker.ui.theme.BrandTeal
import com.settle.tracker.utils.SettlePermission
import kotlinx.coroutines.launch

private data class PrimerCard(
    val icon: ImageVector,
    val headline: String,
    val body: String,
    val permissions: List<SettlePermission>
)

private fun buildPrimerCards(pending: List<SettlePermission>): List<PrimerCard> {
    val cards = mutableListOf<PrimerCard>()
    val remaining = pending.toMutableList()

    // SMS read + receive are really one feature to the user — merge into one card.
    val sms = remaining.filter {
        it == SettlePermission.ReadSms || it == SettlePermission.ReceiveSms
    }
    if (sms.isNotEmpty()) {
        cards += PrimerCard(
            icon = Icons.Filled.Sms,
            headline = "Auto-detect bank SMS",
            body = "Settle reads bank-format texts on-device to pre-fill expenses instantly. Nothing is ever uploaded.",
            permissions = sms
        )
        remaining.removeAll(sms)
    }

    remaining.forEach { permission ->
        cards += PrimerCard(
            icon = permission.icon,
            headline = permission.label,
            body = permission.why,
            permissions = listOf(permission)
        )
    }

    return cards
}

/**
 * Full-screen, shown-once-per-permission-set primer. Appears the first time the
 * app is opened after install, and again after an update introduces a
 * permission the user hasn't been shown before — [pendingPermissions] is
 * exactly that diff, computed by the caller from [com.settle.tracker.utils.SettlePrefs].
 *
 * Purely educational: tapping "Allow" fires the real system prompt right there,
 * but skipping is always safe — the on-demand per-feature prompts still ask
 * when a skipped feature is actually used.
 */
@Composable
fun PermissionPrimerScreen(
    pendingPermissions: List<SettlePermission>,
    onFinished: () -> Unit
) {
    val cards = remember(pendingPermissions) { buildPrimerCards(pendingPermissions) }
    val totalPages = cards.size + 2 // welcome + one per card + done
    val pagerState = rememberPagerState(pageCount = { totalPages })
    val scope = rememberCoroutineScope()

    fun goNext() {
        scope.launch {
            if (pagerState.currentPage < totalPages - 1) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { goNext() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (pagerState.currentPage < totalPages - 1) {
                    TextButton(onClick = onFinished) {
                        Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> PrimerInfoPage(
                        icon = Icons.Filled.PrivacyTip,
                        headline = "Before you start",
                        body = "A quick look at what Settle can do for you — and exactly what it needs to do " +
                            "it. No spam, no surprise pop-ups, ever."
                    )
                    totalPages - 1 -> PrimerInfoPage(
                        icon = Icons.Filled.LockOpen,
                        headline = "You're all set",
                        body = "Skipped something? No problem — Settle will only ask again the moment you " +
                            "actually use that feature."
                    )
                    else -> {
                        val card = cards[page - 1]
                        PermissionCardPage(card)
                    }
                }
            }

            PagerDots(total = totalPages, current = pagerState.currentPage)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (pagerState.currentPage) {
                    0 -> {
                        Button(
                            onClick = { goNext() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("Let's go", fontWeight = FontWeight.SemiBold) }
                    }
                    totalPages - 1 -> {
                        Button(
                            onClick = onFinished,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("Get started", fontWeight = FontWeight.SemiBold) }
                    }
                    else -> {
                        val card = cards[pagerState.currentPage - 1]
                        Button(
                            onClick = { launcher.launch(card.permissions.map { it.androidKey }.toTypedArray()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("Allow", fontWeight = FontWeight.SemiBold) }

                        TextButton(
                            onClick = { goNext() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Not now", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimerInfoPage(icon: ImageVector, headline: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedMedallion(icon = icon)
        Spacer(Modifier.height(28.dp))
        Text(
            headline,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PermissionCardPage(card: PrimerCard) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedMedallion(icon = card.icon)
        Spacer(Modifier.height(28.dp))
        Text(
            card.headline,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            card.body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.PrivacyTip,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Stays on your device. Change anytime in Settings.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnimatedMedallion(icon: ImageVector) {
    val infinite = rememberInfiniteTransition(label = "medallion")
    val scale by infinite.animateFloat(
        initialValue = 0.94f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "medallion-scale"
    )
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.25f, targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "medallion-glow"
    )

    Box(contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .size(140.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(BrandTeal.copy(alpha = glowAlpha), BrandTeal.copy(alpha = 0f))
                ),
                radius = size.minDimension / 2f
            )
        }
        Box(
            modifier = Modifier
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .size(96.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(BrandTeal, BrandBlue))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
private fun PagerDots(total: Int, current: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (i == current) 9.dp else 7.dp)
                    .clip(CircleShape)
                    .background(
                        if (i == current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}
