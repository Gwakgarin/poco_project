package com.example.poco.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poco.ui.components.AppBottomNav
import com.example.poco.ui.components.AppTab
import com.example.poco.ui.components.StatCard
import com.example.poco.ui.theme.POCOTheme
import com.example.poco.ui.theme.PocoGlowEnd
import com.example.poco.ui.theme.PocoGlowStart
import com.example.poco.ui.theme.PocoStatusGreen
import com.example.poco.ui.theme.PocoTextMuted

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
    val canSaveHome: Boolean
)

@Composable
fun UserHomeScreen(
    uiState: UserHomeUiState,
    selectedTab: AppTab = AppTab.HOME,
    onSaveCurrentLocationAsHome: () -> Unit = {},
    onTabSelected: (AppTab) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))

                DetectionStatusRow(
                    isDetecting = uiState.isDetecting,
                    label = uiState.detectionLabel
                )

                Spacer(modifier = Modifier.height(24.dp))

                StatusGlowCircle(text = uiState.statusLabel)

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = uiState.soundLevelLabel,
                    color = PocoTextMuted,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = uiState.lastCheckedLabel,
                    color = PocoTextMuted,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                StatusCardRow(uiState = uiState)

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = uiState.homeStateLabel, color = PocoTextMuted, fontSize = 16.sp)
                Text(text = uiState.locationSummary, color = PocoTextMuted, fontSize = 13.sp)
                TextButton(
                    enabled = uiState.canSaveHome,
                    onClick = onSaveCurrentLocationAsHome
                ) {
                    Text("현재 위치를 집으로 설정")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            AppBottomNav(selectedTab = selectedTab, onTabSelected = onTabSelected)
        }
    }
}

@Composable
private fun DetectionStatusRow(isDetecting: Boolean, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (isDetecting) PocoStatusGreen else PocoTextMuted)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, color = PocoTextMuted, fontSize = 20.sp)
    }
}

@Composable
private fun StatusGlowCircle(text: String) {
    Box(
        modifier = Modifier
            .size(197.dp)
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
        Text(text = text, color = PocoTextMuted, fontSize = 20.sp)
    }
}

@Composable
private fun StatusCardRow(uiState: UserHomeUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 27.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(modifier = Modifier.weight(1f), label = "마이크", value = if (uiState.micOn) "ON" else "OFF")
        StatCard(modifier = Modifier.weight(1f), label = "GPS", value = if (uiState.gpsOn) "ON" else "OFF")
        StatCard(modifier = Modifier.weight(1f), label = "배터리", value = "${uiState.batteryPercent}%")
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
