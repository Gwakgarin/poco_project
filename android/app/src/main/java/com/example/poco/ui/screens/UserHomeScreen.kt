package com.example.poco.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poco.ui.components.AppBottomNav
import com.example.poco.ui.components.AppTab
import com.example.poco.ui.components.PocoCard
import com.example.poco.ui.components.StatCard
import com.example.poco.ui.theme.POCOTheme
import com.example.poco.ui.theme.PocoCardBackground
import com.example.poco.ui.theme.PocoDivider
import com.example.poco.ui.theme.PocoGlowEnd
import com.example.poco.ui.theme.PocoGlowStart
import com.example.poco.ui.theme.PocoGreenCardBackground
import com.example.poco.ui.theme.PocoGreenDark
import com.example.poco.ui.theme.PocoRed
import com.example.poco.ui.theme.PocoStatusGreen
import com.example.poco.ui.theme.PocoTextMuted
import com.example.poco.ui.theme.PocoTextPrimary
import java.util.Calendar

data class UserHomeUiState(
    val isDetecting: Boolean,
    val statusLabel: String,
    val detectionLabel: String,
    val soundLevelLabel: String,
    val lastCheckedLabel: String,
    val micOn: Boolean,
    val gpsOn: Boolean,
    val batteryPercent: Int,
    val homeStateLabel: String,
    val locationSummary: String,
    val canSaveHome: Boolean,
    val userName: String = "민수"
)

@Composable
fun UserHomeScreen(
    uiState: UserHomeUiState,
    selectedTab: AppTab = AppTab.HOME,
    onSaveCurrentLocationAsHome: () -> Unit = {},
    onTabSelected: (AppTab) -> Unit = {},
    onSosClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            GreetingHeader(userName = uiState.userName, onSosClick = onSosClick)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))

                StatusGlowCircle(
                    text = uiState.statusLabel,
                    isDetecting = uiState.isDetecting,
                    soundLevelLabel = uiState.soundLevelLabel
                )

                Spacer(modifier = Modifier.weight(1f))

                PocoCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                    Text(
                        text = uiState.lastCheckedLabel,
                        color = PocoTextMuted,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = PocoDivider, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))
                    StatusCardRow(uiState = uiState)
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = PocoDivider, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))
                    HomeLocationRow(
                        homeStateLabel = uiState.homeStateLabel,
                        locationSummary = uiState.locationSummary,
                        canSaveHome = uiState.canSaveHome,
                        onSaveCurrentLocationAsHome = onSaveCurrentLocationAsHome
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            AppBottomNav(selectedTab = selectedTab, onTabSelected = onTabSelected)
        }
    }
}

@Composable
private fun GreetingHeader(userName: String, onSosClick: () -> Unit) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 5..10 -> "좋은 아침이에요"
        in 11..17 -> "좋은 오후예요"
        else -> "편안한 저녁 되세요"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${userName}님, $greeting",
            color = PocoTextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(PocoRed)
                .clickable(onClick = onSosClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Filled.ReportProblem, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "SOS", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusGlowCircle(text: String, isDetecting: Boolean, soundLevelLabel: String) {
    val caption = if (isDetecting) "정상적인 생활 소리가\n감지되고 있어요" else "마이크 상태를\n확인해주세요"

    Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        if (isDetecting) {
            ListeningRipple(baseSize = 220.dp, delayMillis = 0)
            ListeningRipple(baseSize = 220.dp, delayMillis = 1400)
        }
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        0f to PocoGlowStart,
                        0.9087f to Color.White,
                        1f to PocoGlowEnd
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = text, color = PocoTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                AudioLevelBars(active = isDetecting)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = caption,
                    color = PocoTextMuted,
                    fontSize = 11.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp,
                    modifier = Modifier.width(130.dp)
                )
            }
        }
        Text(
            text = soundLevelLabel,
            color = PocoTextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun AudioLevelBars(active: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        val periods = listOf(620, 760, 540, 820, 680)
        periods.forEach { period ->
            AudioBar(active = active, periodMillis = period)
        }
    }
}

@Composable
private fun AudioBar(active: Boolean, periodMillis: Int) {
    val transition = rememberInfiniteTransition(label = "audio-bar")
    val heightFraction by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (active) 1f else 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "audioBarHeight"
    )
    Box(
        modifier = Modifier
            .width(3.dp)
            .height(18.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height((18.dp * heightFraction).coerceAtLeast(4.dp))
                .clip(RoundedCornerShape(2.dp))
                .background(if (active) PocoStatusGreen else PocoDivider)
        )
    }
}

@Composable
private fun ListeningRipple(baseSize: androidx.compose.ui.unit.Dp, delayMillis: Int) {
    val transition = rememberInfiniteTransition(label = "listening-ripple")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, delayMillis = delayMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleScale"
    )
    val alpha by transition.animateFloat(
        initialValue = 0.14f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, delayMillis = delayMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleAlpha"
    )
    Box(
        modifier = Modifier
            .size(baseSize)
            .scale(scale)
            .clip(CircleShape)
            .background(PocoStatusGreen.copy(alpha = alpha))
    )
}

@Composable
private fun StatusCardRow(uiState: UserHomeUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(modifier = Modifier.weight(1f), label = "마이크", value = if (uiState.micOn) "ON" else "OFF")
        StatCard(modifier = Modifier.weight(1f), label = "GPS", value = if (uiState.gpsOn) "ON" else "OFF")
        StatCard(modifier = Modifier.weight(1f), label = "배터리", value = "${uiState.batteryPercent}%")
    }
}

@Composable
private fun HomeLocationRow(
    homeStateLabel: String,
    locationSummary: String,
    canSaveHome: Boolean,
    onSaveCurrentLocationAsHome: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = homeStateLabel, color = PocoTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = locationSummary, color = PocoTextMuted, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        HomeSyncChip(
            text = "집으로 설정",
            enabled = canSaveHome,
            onClick = onSaveCurrentLocationAsHome
        )
    }
}

@Composable
private fun HomeSyncChip(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) PocoGreenCardBackground else PocoCardBackground)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (enabled) PocoGreenDark else PocoTextMuted.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun UserHomeScreenPreview() {
    POCOTheme {
        UserHomeScreen(
            uiState = UserHomeUiState(
                isDetecting = true,
                statusLabel = "실제 감지 중",
                detectionLabel = "실시간 소리 감지 중",
                soundLevelLabel = "현재 입력 · 41.2 dB",
                lastCheckedLabel = "마지막 확인 · 오전 9:30",
                micOn = true,
                gpsOn = true,
                batteryPercent = 87,
                homeStateLabel = "집 안",
                locationSummary = "정확도 ±12m · 집에서 8m",
                canSaveHome = true
            )
        )
    }
}
