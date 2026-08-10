package com.example.poco.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.poco.ui.theme.PocoCardBackground
import com.example.poco.ui.theme.PocoDivider
import com.example.poco.ui.theme.PocoGreen
import com.example.poco.ui.theme.PocoNavy
import com.example.poco.ui.theme.PocoNavyAccent
import com.example.poco.ui.theme.PocoRed
import com.example.poco.ui.theme.PocoTextMuted
import com.example.poco.ui.theme.PocoTextPrimary

enum class TrendPeriod(val label: String) {
    WEEK("1주"), MONTH("1개월"), HALF_YEAR("6개월")
}

private data class TrendSeries(
    val xLabels: List<String>,
    val outing: List<Float>,
    val housework: List<Float>
)

private val trendSeriesByPeriod = mapOf(
    TrendPeriod.WEEK to TrendSeries(
        xLabels = listOf("월", "화", "수", "목", "금", "토", "일"),
        outing = listOf(1f, 0f, 2f, 1f, 1f, 0f, 1f),
        housework = listOf(3f, 2f, 4f, 3f, 2f, 1f, 3f)
    ),
    TrendPeriod.MONTH to TrendSeries(
        xLabels = listOf("1주", "2주", "3주", "4주"),
        outing = listOf(5f, 4f, 3f, 2f),
        housework = listOf(14f, 12f, 10f, 9f)
    ),
    TrendPeriod.HALF_YEAR to TrendSeries(
        xLabels = listOf("2월", "3월", "4월", "5월", "6월", "7월"),
        outing = listOf(22f, 20f, 18f, 15f, 12f, 9f),
        housework = listOf(60f, 58f, 50f, 45f, 40f, 35f)
    )
)

private data class CognitiveBarMetric(val label: String, val value: Float, val max: Float, val unit: String)

private val cognitiveMetricsByPeriod = mapOf(
    TrendPeriod.WEEK to listOf(
        CognitiveBarMetric("반복 행동 횟수", 6f, 20f, "회/주"),
        CognitiveBarMetric("불규칙 식사 오차율", 32f, 100f, "%"),
        CognitiveBarMetric("낮 시간 무활동 비율", 18f, 100f, "%"),
        CognitiveBarMetric("인지 자극 활동 감소 수준", 15f, 100f, "%")
    ),
    TrendPeriod.MONTH to listOf(
        CognitiveBarMetric("반복 행동 횟수", 9f, 20f, "회/주"),
        CognitiveBarMetric("불규칙 식사 오차율", 41f, 100f, "%"),
        CognitiveBarMetric("낮 시간 무활동 비율", 27f, 100f, "%"),
        CognitiveBarMetric("인지 자극 활동 감소 수준", 33f, 100f, "%")
    ),
    TrendPeriod.HALF_YEAR to listOf(
        CognitiveBarMetric("반복 행동 횟수", 14f, 20f, "회/주"),
        CognitiveBarMetric("불규칙 식사 오차율", 55f, 100f, "%"),
        CognitiveBarMetric("낮 시간 무활동 비율", 38f, 100f, "%"),
        CognitiveBarMetric("인지 자극 활동 감소 수준", 47f, 100f, "%")
    )
)

private val aiSummaryByPeriod = mapOf(
    TrendPeriod.WEEK to "이번 주는 오후 활동량이 지난주보다 다소 줄었지만 전반적인 생활 리듬은 안정적으로 유지되고 있어요. " +
        "화요일 오후에 3시간 이상 움직임이 없었던 점을 제외하면 특이 신호는 없었습니다.",
    TrendPeriod.MONTH to "최근 1개월간 외출 빈도 43% 감소와 반복 행동 증가가 관찰됩니다. " +
        "이는 사회적 고립 및 인지 자극 부족의 징후일 수 있습니다.",
    TrendPeriod.HALF_YEAR to "최근 6개월간 외출 횟수와 가사 활동량이 꾸준히 감소하는 추세이며, " +
        "인지 자극 활동 감소 수준도 함께 높아지고 있어 정기적인 대면 진료 상담을 권장드립니다."
)

private data class KeyMetricChange(val label: String, val delta: String, val isWorsening: Boolean)

private val keyMetricChangesByPeriod = mapOf(
    TrendPeriod.WEEK to listOf(
        KeyMetricChange("무활동 지속 시간", "+1건 (3시간 이상)", true),
        KeyMetricChange("수면 패턴", "지난주와 비슷", false)
    ),
    TrendPeriod.MONTH to listOf(
        KeyMetricChange("외출 빈도", "-43% (4주 평균 대비)", true),
        KeyMetricChange("반복 행동", "+3회 (4주 평균 대비)", true),
        KeyMetricChange("인지 자극 활동", "-47%", true)
    ),
    TrendPeriod.HALF_YEAR to listOf(
        KeyMetricChange("외출 횟수", "-59% (2월 대비)", true),
        KeyMetricChange("가사 활동량", "-42% (2월 대비)", true),
        KeyMetricChange("인지 자극 활동 감소 수준", "+32%p", true)
    )
)

/** 실데이터가 없을 때(요청 실패 등)만 쓰는 자리표시 값. 실사용 시엔 항상 GuardianTrendScreen의 hourlyRhythm 파라미터로 대체됨. */
private val defaultHourlyRhythm = List(24) { 0 }

@Composable
fun GuardianTrendScreen(
    selectedTab: GuardianTab,
    onTabSelected: (GuardianTab) -> Unit,
    modifier: Modifier = Modifier,
    hourlyRhythm: List<Int> = defaultHourlyRhythm
) {
    var selectedPeriod by remember { mutableStateOf(TrendPeriod.WEEK) }
    val series = trendSeriesByPeriod.getValue(selectedPeriod)
    val cognitiveMetrics = cognitiveMetricsByPeriod.getValue(selectedPeriod)

    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f)) {
                PocoTopBar(title = "장기 추세 분석")
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        PeriodToggleRow(selected = selectedPeriod, onSelect = { selectedPeriod = it })
                    }
                    item { DailyRhythmCard(hourlyRhythm = hourlyRhythm) }
                    item { LongTermTrendCard(period = selectedPeriod, series = series) }
                    item { CognitiveMetricsCard(metrics = cognitiveMetrics) }
                    item {
                        AiSummaryCard(
                            summaryText = aiSummaryByPeriod.getValue(selectedPeriod),
                            keyChanges = keyMetricChangesByPeriod.getValue(selectedPeriod)
                        )
                    }
                    item { Spacer(modifier = Modifier.height(4.dp)) }
                }
            }
            GuardianBottomNav(selectedTab = selectedTab, onTabSelected = onTabSelected)
        }
    }
}

@Composable
private fun PeriodToggleRow(selected: TrendPeriod, onSelect: (TrendPeriod) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PocoCardBackground)
            .padding(4.dp)
    ) {
        TrendPeriod.entries.forEach { period ->
            val isSelected = period == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isSelected) Color.White else Color.Transparent)
                    .clickable { onSelect(period) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period.label,
                    color = if (isSelected) PocoNavy else PocoTextMuted,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CardTitle(text: String) {
    Text(text = text, color = PocoTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun DailyRhythmCard(hourlyRhythm: List<Int>) {
    PocoCard(modifier = Modifier.fillMaxWidth()) {
        CardTitle("24시간 생활 리듬 · 오늘 기준")
        Spacer(modifier = Modifier.height(12.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
        ) {
            val barCount = hourlyRhythm.size
            val gap = 3.dp.toPx()
            val barWidth = (size.width - gap * (barCount - 1)) / barCount
            val maxVal = (hourlyRhythm.maxOrNull() ?: 0).coerceAtLeast(1).toFloat()
            hourlyRhythm.forEachIndexed { index, value ->
                val barHeight = (value / maxVal) * size.height
                drawRoundRect(
                    color = PocoGreen,
                    topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
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
private fun LongTermTrendCard(period: TrendPeriod, series: TrendSeries) {
    PocoCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardTitle("장기 활동 추이")
            Spacer(modifier = Modifier.weight(1f))
            LegendDot(color = PocoGreen, label = "외출 횟수")
            Spacer(modifier = Modifier.width(10.dp))
            LegendDot(color = PocoNavyAccent, label = "가사 활동량")
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "${period.label} 기준", color = PocoTextMuted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            val maxVal = maxOf(series.outing.max(), series.housework.max())
            fun points(data: List<Float>): List<Offset> {
                val stepX = if (data.size > 1) size.width / (data.size - 1) else 0f
                return data.mapIndexed { index, value ->
                    Offset(index * stepX, size.height - (value / maxVal) * size.height)
                }
            }
            fun drawSeries(data: List<Float>, color: Color) {
                val pts = points(data)
                for (i in 0 until pts.size - 1) {
                    drawLine(color = color, start = pts[i], end = pts[i + 1], strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                }
                pts.forEach { point ->
                    drawCircle(color = color, radius = 4.dp.toPx(), center = point)
                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = point)
                }
            }
            drawSeries(series.housework, PocoNavyAccent)
            drawSeries(series.outing, PocoGreen)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            series.xLabels.forEach {
                Text(text = it, color = PocoTextMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = label, color = PocoTextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun CognitiveMetricsCard(metrics: List<CognitiveBarMetric>) {
    PocoCard(modifier = Modifier.fillMaxWidth()) {
        CardTitle("인지 행동 지표")
        Spacer(modifier = Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            metrics.forEach { metric ->
                val ratio = (metric.value / metric.max).coerceIn(0f, 1f)
                val barColor = when {
                    ratio >= 0.45f -> PocoRed
                    ratio >= 0.25f -> PocoAmber
                    else -> PocoGreen
                }
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = metric.label, color = PocoTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = "${if (metric.value % 1f == 0f) metric.value.toInt() else metric.value}${metric.unit}",
                            color = barColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PocoDivider)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(barColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiSummaryCard(summaryText: String, keyChanges: List<KeyMetricChange>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(PocoNavy)
            .padding(18.dp)
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
            text = summaryText,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 13.sp,
            lineHeight = 19.sp
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "주요 변화 지표 요약", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            keyChanges.forEach { change ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = change.label, color = Color.White.copy(alpha = 0.85f), fontSize = 12.5.sp)
                    Text(
                        text = change.delta,
                        color = if (change.isWorsening) Color(0xFFFF9B9B) else Color.White.copy(alpha = 0.85f),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .clickable { /* TODO: PDF 내보내기 — 다음 단계에서 연동 예정 */ }
                .padding(vertical = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Filled.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "진료 자료 PDF 내보내기", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 1500)
@Composable
private fun GuardianTrendScreenPreview() {
    POCOTheme {
        GuardianTrendScreen(
            selectedTab = GuardianTab.TREND,
            onTabSelected = {},
            hourlyRhythm = listOf(2, 1, 0, 0, 0, 1, 3, 6, 8, 7, 6, 8, 9, 7, 6, 8, 9, 8, 7, 6, 5, 4, 3, 2)
        )
    }
}
