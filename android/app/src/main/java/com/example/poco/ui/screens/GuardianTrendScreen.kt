package com.example.poco.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poco.ui.components.GuardianBottomNav
import com.example.poco.ui.components.GuardianTab
import com.example.poco.ui.theme.POCOTheme
import com.example.poco.ui.theme.PocoCardBackground
import com.example.poco.ui.theme.PocoDivider
import com.example.poco.ui.theme.PocoGreen
import com.example.poco.ui.theme.PocoNavy
import com.example.poco.ui.theme.PocoTextMuted
import com.example.poco.ui.theme.PocoTextPrimary

private val hourlyRhythm = listOf(
    2, 1, 0, 0, 0, 1, 3, 6, 8, 7, 6, 8, 9, 7, 6, 8, 9, 8, 7, 6, 5, 4, 3, 2
)
private val weeklyTrend = listOf(6, 7, 5, 8, 7, 4, 8)
private data class CognitiveMetric(val label: String, val score: Float, val detail: String)
private val cognitiveMetrics = listOf(
    CognitiveMetric("반응 속도", 0.82f, "양호 · 지난주 대비 +4%"),
    CognitiveMetric("활동 일관성", 0.68f, "보통 · 지난주 대비 -6%"),
    CognitiveMetric("수면 패턴", 0.90f, "양호 · 지난주와 비슷")
)

@Composable
fun GuardianTrendScreen(
    selectedTab: GuardianTab,
    onTabSelected: (GuardianTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(top = 8.dp)) {
                            Text(text = "장기 추세 분석", color = PocoTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text(text = "최근 7일 기준", color = PocoTextMuted, fontSize = 13.sp)
                        }
                    }
                    item { DailyRhythmCard() }
                    item { WeeklyTrendCard() }
                    item { CognitiveMetricsCard() }
                    item { AiSummaryCard() }
                }
            }
            GuardianBottomNav(selectedTab = selectedTab, onTabSelected = onTabSelected)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PocoCardBackground)
            .padding(16.dp)
    ) {
        Text(text = title, color = PocoTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun DailyRhythmCard() {
    SectionCard(title = "24시간 생활 리듬") {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
        ) {
            val barCount = hourlyRhythm.size
            val gap = 3.dp.toPx()
            val barWidth = (size.width - gap * (barCount - 1)) / barCount
            val maxVal = hourlyRhythm.max().toFloat()
            hourlyRhythm.forEachIndexed { index, value ->
                val barHeight = (value / maxVal) * size.height
                drawRoundRect(
                    color = PocoGreen,
                    topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("0시", "6시", "12시", "18시", "24시").forEach {
                Text(text = it, color = PocoTextMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun WeeklyTrendCard() {
    SectionCard(title = "장기 활동 추이") {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
        ) {
            val maxVal = weeklyTrend.max().toFloat()
            val stepX = size.width / (weeklyTrend.size - 1)
            val points = weeklyTrend.mapIndexed { index, value ->
                Offset(index * stepX, size.height - (value / maxVal) * size.height)
            }
            for (i in 0 until points.size - 1) {
                drawLine(
                    color = PocoNavy,
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            points.forEach { point ->
                drawCircle(color = PocoNavy, radius = 4.dp.toPx(), center = point)
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = point)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("월", "화", "수", "목", "금", "토", "일").forEach {
                Text(text = it, color = PocoTextMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun CognitiveMetricsCard() {
    SectionCard(title = "인지 행동 지표") {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            cognitiveMetrics.forEach { metric ->
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = metric.label, color = PocoTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(text = "${(metric.score * 100).toInt()}", color = PocoTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(PocoDivider)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(metric.score)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(PocoGreen)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = metric.detail, color = PocoTextMuted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AiSummaryCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PocoNavy)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "AI 요약 보고서", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "이번 주는 오후 활동량이 지난주보다 다소 줄었지만 전반적인 생활 리듬은 안정적으로 유지되고 있어요. " +
                "화요일 오후에 3시간 이상 움직임이 없었던 점을 제외하면 특이 신호는 없었습니다.",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 1400)
@Composable
private fun GuardianTrendScreenPreview() {
    POCOTheme {
        GuardianTrendScreen(selectedTab = GuardianTab.TREND, onTabSelected = {})
    }
}
