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
import androidx.compose.material.icons.filled.ReportProblem
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
import com.example.poco.ui.theme.PocoDivider
import com.example.poco.ui.theme.PocoGreen
import com.example.poco.ui.theme.PocoRed
import com.example.poco.ui.theme.PocoRedCardBackground
import com.example.poco.ui.theme.PocoTextMuted
import com.example.poco.ui.theme.PocoTextPrimary

data class TimelineEntry(
    val time: String,
    val label: String,
    val isRisk: Boolean = false
)

private val mockTimeline = listOf(
    TimelineEntry("오전 7:20", "기상 · 활동 시작"),
    TimelineEntry("오전 8:10", "식사 소리 감지"),
    TimelineEntry("오전 11:40", "외출 감지 (GPS 이동)"),
    TimelineEntry("오후 2:05", "3시간 이상 활동 없음", isRisk = true),
    TimelineEntry("오후 3:30", "정상 활동 재개")
)

@Composable
fun GuardianDailyMonitoringScreen(
    selectedTab: GuardianTab,
    onTabSelected: (GuardianTab) -> Unit,
    modifier: Modifier = Modifier,
    timeline: List<TimelineEntry> = mockTimeline,
    sleepDurationLabel: String = "-"
) {
    val riskCount = timeline.count { it.isRisk }

    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 24.dp)
                        .padding(top = 16.dp, bottom = 8.dp)
                ) {
                    Text(text = "오늘의 모니터링", color = PocoTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(text = "2026년 7월 21일 화요일", color = PocoTextMuted, fontSize = 13.sp)
                }

                if (riskCount > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PocoRedCardBackground)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Filled.ReportProblem, contentDescription = null, tint = PocoRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "오늘 위험 신호 ${riskCount}건이 감지됐어요", color = PocoRed, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(modifier = Modifier.weight(1f), label = "총 활동", value = "18회")
                    StatCard(modifier = Modifier.weight(1f), label = "수면 시간", value = sleepDurationLabel)
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "이상 신호",
                        value = "${riskCount}건",
                        valueColor = if (riskCount > 0) PocoRed else PocoGreen
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "타임라인",
                    color = PocoTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(timeline) { entry -> TimelineRow(entry, isLast = entry == timeline.last()) }
                }
            }
            GuardianBottomNav(selectedTab = selectedTab, onTabSelected = onTabSelected)
        }
    }
}

@Composable
private fun TimelineRow(entry: TimelineEntry, isLast: Boolean) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(16.dp)) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (entry.isRisk) PocoRed else PocoGreen)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(PocoDivider)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.padding(bottom = 20.dp)) {
            Text(text = entry.time, color = PocoTextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = entry.label,
                color = if (entry.isRisk) PocoRed else PocoTextPrimary,
                fontSize = 14.sp,
                fontWeight = if (entry.isRisk) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun GuardianDailyMonitoringScreenPreview() {
    POCOTheme {
        GuardianDailyMonitoringScreen(selectedTab = GuardianTab.TIMELINE, onTabSelected = {})
    }
}
