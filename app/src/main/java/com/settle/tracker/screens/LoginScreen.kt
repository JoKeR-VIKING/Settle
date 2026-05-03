package com.settle.tracker.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseUser
import com.settle.tracker.GoogleAuthClient
import com.settle.tracker.R
import com.settle.tracker.ui.animations.CoinLoader
import com.settle.tracker.ui.animations.breathing
import com.settle.tracker.ui.animations.bounceClickable
import com.settle.tracker.ui.theme.BrandBlue
import com.settle.tracker.ui.theme.BrandBlueDeep
import com.settle.tracker.ui.theme.BrandTeal
import com.settle.tracker.ui.theme.BrandTealDeep
import com.settle.tracker.utils.SettleLinks
import com.settle.tracker.utils.openUrl
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    googleAuthClient: GoogleAuthClient,
    onLoginSuccess: (FirebaseUser) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { contentVisible = true }

    // Animated blob background: slow drifting color washes using brand palette
    val transition = rememberInfiniteTransition(label = "bg")
    val t by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(7000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "bg-t"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Brand gradient wash
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BrandTeal.copy(alpha = 0.30f + 0.10f * t),
                            Color.Transparent
                        ),
                        center = Offset(400f + t * 200f, 400f),
                        radius = 900f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BrandBlue.copy(alpha = 0.25f + 0.10f * (1 - t)),
                            Color.Transparent
                        ),
                        center = Offset(1000f - t * 300f, 1500f),
                        radius = 1000f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(1.dp))

            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(600)) + slideInVertically(
                    animationSpec = tween(600, easing = FastOutSlowInEasing),
                    initialOffsetY = { -it / 3 }
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Settle Logo",
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(percent = 30))
                            .breathing(minScale = 0.97f, maxScale = 1.05f),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(Modifier.height(22.dp))

                    Text(
                        "Settle",
                        style = MaterialTheme.typography.headlineLarge,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        "Split smart. Settle fast.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(700, delayMillis = 200)) + slideInVertically(
                    animationSpec = tween(700, easing = FastOutSlowInEasing),
                    initialOffsetY = { it / 2 }
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CoinLoader(message = "Signing you in…")
                    }

                    Button(
                        onClick = {
                            isLoading = true
                            scope.launch {
                                val loginSuccess = googleAuthClient.signIn()
                                isLoading = false
                                if (loginSuccess) {
                                    googleAuthClient.getSignedInUser()?.let(onLoginSuccess)
                                } else {
                                    Toast.makeText(context, "Sign in failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(0.9f),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(BrandTealDeep, BrandBlueDeep)
                                    ),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.layout.Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.google_logo),
                                        contentDescription = "Google Logo",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Continue with Google",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.4.sp
                                )
                            }
                        }
                    }

                    Text(
                        "By continuing you agree to our Terms",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .bounceClickable { context.openUrl(SettleLinks.PRIVACY_POLICY) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "View Privacy Policy",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
