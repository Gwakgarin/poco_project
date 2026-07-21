package com.example.poco.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poco.ui.components.GuardianBottomNav
import com.example.poco.ui.components.GuardianTab
import com.example.poco.ui.theme.POCOTheme
import com.example.poco.ui.theme.PocoAmber
import com.example.poco.ui.theme.PocoCardBackground
import com.example.poco.ui.theme.PocoGreen
import com.example.poco.ui.theme.PocoRed
import com.example.poco.ui.theme.PocoTextMuted
import com.example.poco.ui.theme.PocoTextPrimary

enum class AlertSeverity { INFO, WARNING, CRITICAL }

data class NotificationItem(
    val time: String,
    val title: String,
    val description: String,
    val severity: AlertSeverity
)

private val mockNotifications = listOf(
    NotificationItem("오후 2:10", "활동 감소 감지", "평소보다 3시간 이상 움직임이 없어요", AlertSeverity.WARNING),
    NotificationItem("오전 11:32", "긴급 알림 해제", "SOS 알림이 사용자에 의해 해제되었습니다", AlertSeverity.CRITICAL),
    NotificationItem("오전 9:05", "정상 활동 감지", "아침 루틴이 평소와 비슷하게 감지됐어요", AlertSeverity.INFO),
    NotificationItem("어제 오후 8:40", "배터리 부족", "기기 배터리가 15% 이하로 떨어졌어요", AlertSeverity.WARNING)
)

@Composable
fun NotificationCenterScreen(
    selectedTab: GuardianTab,
    onTabSelected: (GuardianTab) -> Unit,
    modifier: Modifier = Modifier,
    items: List<NotificationItem> = mockNotifications
) {
    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "알림센터",
                    color = PocoTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 24.dp)
                        .padding(top = 16.dp, bottom = 8.dp)
                )
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items) { item -> NotificationRow(item) }
                }
            }
            GuardianBottomNav(selectedTab = selectedTab, onTabSelected = onTabSelected)
        }
    }
}

@Composable
private fun NotificationRow(item: NotificationItem) {
    val (icon, tint) = when (item.severity) {
        AlertSeverity.INFO -> Icons.Filled.CheckCircle to PocoGreen
        AlertSeverity.WARNING -> Icons.Filled.WarningAmber to PocoAmber
        AlertSeverity.CRITICAL -> Icons.Filled.ReportProblem to PocoRed
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PocoCardBackground)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = item.title, color = PocoTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = item.time, color = PocoTextMuted, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = item.description, color = PocoTextMuted, fontSize = 13.sp)
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun NotificationCenterScreenPreview() {
    POCOTheme {
        NotificationCenterScreen(selectedTab = GuardianTab.ALERTS, onTabSelected = {})
    }
}
