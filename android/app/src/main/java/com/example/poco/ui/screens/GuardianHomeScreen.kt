package com.example.poco.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
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
import com.example.poco.ui.components.PocoCard
import com.example.poco.ui.components.StatCard
import com.example.poco.ui.theme.POCOTheme
import com.example.poco.ui.theme.PocoAmber
import com.example.poco.ui.theme.PocoAmberBackground
import com.example.poco.ui.theme.PocoDivider
import com.example.poco.ui.theme.PocoGreen
import com.example.poco.ui.theme.PocoNavy
import com.example.poco.ui.theme.PocoRed
import com.example.poco.ui.theme.PocoTextMuted
import com.example.poco.ui.theme.PocoTextPrimary

private data class ActivitySummaryStat(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val valueColor: Color
)

@Composable
fun GuardianHomeScreen(
    selectedTab: GuardianTab,
    onTabSelected: (GuardianTab) -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier,
    monitoredUserLabel: String = "연동된 사용자",
    mealCountLabel: String = "-",
    outingLabel: String = "-",
    cognitiveDurationLabel: String = "-",
    micLabel: String = "-",
    gpsLabel: String = "-",
    batteryLabel: String = "-",
    latestAlert: TimelineEntry? = null,
    sleepDurationLabel: String = "-",
    wakeTimelineEntry: TimelineEntry = TimelineEntry("-", "기상 정보 없음"),
    recentTimeline: List<TimelineEntry> = emptyList()
) {
    val homeActivitySummary = listOf(
        ActivitySummaryStat(Icons.Filled.Restaurant, "식사 횟수", mealCountLabel, PocoTextPrimary),
        ActivitySummaryStat(Icons.Filled.DirectionsWalk, "외출 여부", outingLabel, PocoGreen),
        ActivitySummaryStat(Icons.Filled.Psychology, "인지 활동 시간", cognitiveDurationLabel, PocoTextPrimary),
        ActivitySummaryStat(Icons.Filled.Bedtime, "수면 시간", sleepDurationLabel, PocoTextPrimary)
    )
    val homeTimeline = (listOf(wakeTimelineEntry) + recentTimeline).ifEmpty { listOf(TimelineEntry("-", "아직 기록이 없어요")) }

    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item { GuardianHeader(monitoredUserLabel = monitoredUserLabel) }
                    if (latestAlert != null) {
                        item {
                            PocoCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onOpenNotifications),
                                containerColor = PocoAmberBackground
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Filled.WarningAmber, contentDescription = null, tint = PocoAmber)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "주의", color = PocoAmber, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${latestAlert.label} · ${latestAlert.time}",
                                            color = PocoTextPrimary,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = PocoTextMuted)
                                }
                            }
                        }
                    }
                    item {
                        Column {
                            SectionLabel("오늘의 활동 요약")
                            Spacer(modifier = Modifier.height(12.dp))
                            PocoCard(modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        ActivitySummaryCell(homeActivitySummary[0])
                                        ActivitySummaryCell(homeActivitySummary[1])
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        ActivitySummaryCell(homeActivitySummary[2])
                                        ActivitySummaryCell(homeActivitySummary[3])
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Column {
                            SectionLabel("실시간 상태")
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatCard(modifier = Modifier.weight(1f), label = "마이크", value = micLabel)
                                StatCard(modifier = Modifier.weight(1f), label = "GPS", value = gpsLabel)
                                StatCard(modifier = Modifier.weight(1f), label = "배터리", value = batteryLabel)
                            }
                        }
                    }
                    item {
                        Column {
                            SectionLabel("오늘의 일간 타임라인")
                            Spacer(modifier = Modifier.height(12.dp))
                            PocoCard(modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    homeTimeline.forEachIndexed { index, entry ->
                                        HomeTimelineRow(entry, isLast = index == homeTimeline.lastIndex)
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                }
            }
            GuardianBottomNav(selectedTab = selectedTab, onTabSelected = onTabSelected)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, color = PocoTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun ActivitySummaryCell(stat: ActivitySummaryStat) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PocoCardBackgroundTint(stat.valueColor)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = stat.icon, contentDescription = null, tint = stat.valueColor, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = stat.label, color = PocoTextMuted, fontSize = 12.sp)
            Text(text = stat.value, color = stat.valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PocoCardBackgroundTint(base: Color): Color {
    return if (base == PocoTextPrimary) Color(0xFFEDEEF1) else base.copy(alpha = 0.14f)
}

@Composable
private fun HomeTimelineRow(entry: TimelineEntry, isLast: Boolean) {
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
                        .height(32.dp)
                        .background(PocoDivider)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 18.dp)) {
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

@Composable
private fun GuardianHeader(monitoredUserLabel: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
            .background(PocoNavy)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 22.dp)
    ) {
        Text(text = "보호자 모드", color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "${monitoredUserLabel} 모니터링 중", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun GuardianHomeScreenPreview() {
    POCOTheme {
        GuardianHomeScreen(
            selectedTab = GuardianTab.HOME,
            onTabSelected = {},
            onOpenNotifications = {},
            monitoredUserLabel = "김민수님",
            mealCountLabel = "3회",
            outingLabel = "다녀옴",
            cognitiveDurationLabel = "42분",
            micLabel = "ON",
            gpsLabel = "ON",
            batteryLabel = "62%",
            sleepDurationLabel = "7시간 20분",
            wakeTimelineEntry = TimelineEntry("오전 7:15", "기상 · 활동 시작")
        )
    }
}
