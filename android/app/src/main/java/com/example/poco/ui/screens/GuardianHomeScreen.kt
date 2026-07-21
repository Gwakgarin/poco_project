package com.example.poco.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poco.ui.components.GuardianBottomNav
import com.example.poco.ui.components.GuardianTab
import com.example.poco.ui.components.StatCard
import com.example.poco.ui.theme.POCOTheme
import com.example.poco.ui.theme.PocoAmber
import com.example.poco.ui.theme.PocoAmberBackground
import com.example.poco.ui.theme.PocoNavy
import com.example.poco.ui.theme.PocoTextMuted
import com.example.poco.ui.theme.PocoTextPrimary

@Composable
fun GuardianHomeScreen(
    selectedTab: GuardianTab,
    onTabSelected: (GuardianTab) -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f)) {
                GuardianHeader()

                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(PocoAmberBackground)
                            .clickable(onClick = onOpenNotifications)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Filled.WarningAmber, contentDescription = null, tint = PocoAmber)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "주의", color = PocoAmber, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "평소보다 활동이 적어요 · 마지막 활동 3시간 전",
                                color = PocoTextPrimary,
                                fontSize = 13.sp
                            )
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = PocoTextMuted)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = "실시간 상태", color = PocoTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(modifier = Modifier.weight(1f), label = "마이크", value = "ON")
                        StatCard(modifier = Modifier.weight(1f), label = "GPS", value = "ON")
                        StatCard(modifier = Modifier.weight(1f), label = "배터리", value = "62%")
                    }
                }
            }
            GuardianBottomNav(selectedTab = selectedTab, onTabSelected = onTabSelected)
        }
    }
}

@Composable
private fun GuardianHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PocoNavy)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Text(text = "보호자 모드", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "김민수님 모니터링 중", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun GuardianHomeScreenPreview() {
    POCOTheme {
        GuardianHomeScreen(selectedTab = GuardianTab.HOME, onTabSelected = {}, onOpenNotifications = {})
    }
}
