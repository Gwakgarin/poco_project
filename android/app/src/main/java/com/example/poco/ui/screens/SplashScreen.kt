package com.example.poco.ui.screens

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poco.ui.components.PocoLogoBadge
import com.example.poco.ui.theme.POCOTheme
import com.example.poco.ui.theme.PocoGlowStart
import com.example.poco.ui.theme.PocoGreen
import com.example.poco.ui.theme.PocoGreenDeep
import com.example.poco.ui.theme.PocoNearBlack
import kotlinx.coroutines.delay

private const val SPLASH_DURATION_MS = 1400L

@Composable
fun SplashScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MS)
        onTimeout()
    }

    val transition = rememberInfiniteTransition(label = "splash-pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(modifier = modifier.fillMaxSize(), color = PocoNearBlack) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    // 글로우를 로고 크기에 맞춰 로컬로 배경 처리 — 화면 전체 기준으로 그리면
                    // 텍스트까지 포함한 Column 전체가 중앙 정렬되면서 글로우 중심과 로고 위치가 어긋난다.
                    Box(
                        modifier = Modifier
                            .size(420.dp)
                            .background(
                                Brush.radialGradient(
                                    0f to PocoGlowStart,
                                    0.22f to PocoGreen,
                                    0.5f to PocoGreenDeep,
                                    1f to PocoNearBlack
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = pulseAlpha))
                    )
                    PocoLogoBadge(size = 92.dp, badgeColor = Color.White, markColor = PocoGreen)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "POCO",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.4).sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "소리로 지키는 가족의 하루",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun SplashScreenPreview() {
    POCOTheme {
        SplashScreen(onTimeout = {})
    }
}
