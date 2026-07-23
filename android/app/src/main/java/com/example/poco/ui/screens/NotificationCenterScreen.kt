package com.example.poco.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
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
import com.example.poco.ui.components.PocoCard
import com.example.poco.ui.components.PocoTopBar
import com.example.poco.ui.theme.POCOTheme
import com.example.poco.ui.theme.PocoAmber
import com.example.poco.ui.theme.PocoAmberBackground
import com.example.poco.ui.theme.PocoCardBackground
import com.example.poco.ui.theme.PocoGreen
import com.example.poco.ui.theme.PocoRed
import com.example.poco.ui.theme.PocoRedCardBackground
import com.example.poco.ui.theme.PocoTextMuted
import com.example.poco.ui.theme.PocoTextPrimary

enum class AnomalyType(val label: String, val icon: ImageVector, val tint: Color, val background: Color) {
    MEAL_IRREGULAR("식사 시간 불규칙", Icons.Filled.Restaurant, PocoAmber, PocoAmberBackground),
    OUTING_DECREASE("외출 빈도 급감", Icons.Filled.DirectionsWalk, PocoRed, PocoRedCardBackground),
    COGNITIVE_DECREASE("대화·인지 자극 감소", Icons.Filled.Psychology, PocoRed, PocoRedCardBackground)
}

data class AnomalyAlert(
    val time: String,
    val type: AnomalyType,
    val evidence: String
)

data class GeneralNotice(
    val time: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tint: Color
)

private val mockAnomalies = listOf(
    AnomalyAlert(
        time = "오늘 오후 3:42",
        type = AnomalyType.MEAL_IRREGULAR,
        evidence = "3일 연속 오후 3시 이후 지연 · 평균보다 3.2시간 지연"
    ),
    AnomalyAlert(
        time = "오늘 오전 9:10",
        type = AnomalyType.OUTING_DECREASE,
        evidence = "이번 주 외출 1회 · 최근 4주 평균(5회) 대비 80% 감소"
    ),
    AnomalyAlert(
        time = "어제 오후 8:00",
        type = AnomalyType.COGNITIVE_DECREASE,
        evidence = "주간 인지 활동 18분 · 4주 평균(34분) 대비 47% 감소"
    )
)

private val mockGeneralNotices = listOf(
    GeneralNotice("오후 9:00", "일일 요약 리포트 도착", "오늘 하루 활동 요약을 확인해보세요", Icons.Filled.CheckCircle, PocoGreen),
    GeneralNotice("오전 9:05", "정상 활동 감지", "아침 루틴이 평소와 비슷하게 감지됐어요", Icons.Filled.CheckCircle, PocoGreen),
    GeneralNotice("어제 오후 8:40", "배터리 부족 알림", "기기 배터리가 15% 이하로 떨어졌어요", Icons.Filled.BatteryAlert, PocoTextMuted)
)

@Composable
fun NotificationCenterScreen(
    selectedTab: GuardianTab,
    onTabSelected: (GuardianTab) -> Unit,
    modifier: Modifier = Modifier,
    anomalies: List<AnomalyAlert> = mockAnomalies,
    generalNotices: List<GeneralNotice> = mockGeneralNotices
) {
    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f)) {
                PocoTopBar(title = "알림 센터")
                LazyColumnBody(anomalies = anomalies, generalNotices = generalNotices)
            }
            GuardianBottomNav(selectedTab = selectedTab, onTabSelected = onTabSelected)
        }
    }
}

@Composable
private fun LazyColumnBody(anomalies: List<AnomalyAlert>, generalNotices: List<GeneralNotice>) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(title = "이상 탐지", count = anomalies.size, countColor = PocoRed)
        }
        items(anomalies) { anomaly -> AnomalyCard(anomaly) }

        item { Spacer(modifier = Modifier.height(8.dp)) }
        item {
            SectionHeader(title = "일반 알림", count = generalNotices.size, countColor = PocoTextMuted)
        }
        items(generalNotices) { notice -> GeneralNoticeRow(notice) }
        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, countColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, color = PocoTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(countColor.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(text = "$count", color = countColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AnomalyCard(anomaly: AnomalyAlert) {
    PocoCard(modifier = Modifier.fillMaxWidth(), containerColor = anomaly.type.background) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = anomaly.type.icon, contentDescription = null, tint = anomaly.type.tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = anomaly.type.label, color = anomaly.type.tint, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = anomaly.time, color = PocoTextMuted, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = anomaly.evidence, color = PocoTextPrimary, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
    }
}

@Composable
private fun GeneralNoticeRow(notice: GeneralNotice) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PocoCardBackground)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = notice.icon, contentDescription = null, tint = notice.tint, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = notice.title, color = PocoTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = notice.time, color = PocoTextMuted, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = notice.description, color = PocoTextMuted, fontSize = 12.sp)
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
