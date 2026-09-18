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
import com.example.poco.ui.theme.PocoCardBackground
import com.example.poco.ui.theme.PocoGreen
import com.example.poco.ui.theme.PocoNavy
import com.example.poco.ui.theme.PocoNavyAccent
import com.example.poco.ui.theme.PocoTextMuted
import com.example.poco.ui.theme.PocoTextPrimary

enum class TrendPeriod(val label: String) {
    WEEK("1주"), MONTH("1개월"), HALF_YEAR("6개월")
}

/** 1주 · 1개월 · 6개월 장기 추세 그래프가 쓰는 시계열. 실데이터 기준으로 [TrendAggregator]가 채운다. */
data class TrendSeries(
    val xLabels: List<String>,
    val outing: List<Float>,
    val housework: List<Float>
)

/** 실데이터가 없을 때(요청 실패 등)만 쓰는 자리표시 값. 실사용 시엔 항상 GuardianTrendScreen의 hourlyRhythm 파라미터로 대체됨. */
private val defaultHourlyRhythm = List(24) { 0 }

@Composable
fun GuardianTrendScreen(
    selectedTab: GuardianTab,
    onTabSelected: (GuardianTab) -> Unit,
    modifier: Modifier = Modifier,
    hourlyRhythm: List<Int> = defaultHourlyRhythm,
    trendState: TrendUiState = TrendUiState.Loading
) {
    var selectedPeriod by remember { mutableStateOf(TrendPeriod.WEEK) }

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
                    when (trendState) {
                        is TrendUiState.Loading -> {
                            item { TrendStatusCard("장기 추세 데이터를 불러오는 중이에요...") }
                        }
                        is TrendUiState.Error -> {
                            item { TrendStatusCard(trendState.message) }
                        }
                        is TrendUiState.Ready -> {
                            val series = trendState.trendSeriesByPeriod[selectedPeriod]
                            val analysis = trendState.analysisByPeriod[selectedPeriod]
                            item {
                                if (series != null) LongTermTrendCard(period = selectedPeriod, series = series)
                                else TrendStatusCard("아직 기록된 데이터가 없어요")
                            }
                            item {
                                if (analysis != null) LifeRegularityCard(analysis, trendState.sleepState, trendState.dangerState)
                                else TrendStatusCard("아직 기록된 데이터가 없어요")
                            }
                            if (analysis != null) {
                                item {
                                    KeyChangesCard(
                                        keyChanges = analysis.keyChanges,
                                        validDaysCurrent = analysis.validDaysCurrent,
                                        validDaysPrevious = analysis.validDaysPrevious,
                                        dangerState = trendState.dangerState
                                    )
                                }
                                item { LifePatternSummaryCard(analysis.summarySentences) }
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

/** 로딩 · 오류 · "아직 계산 못하는 지표" 공용 안내 카드. 가짜 숫자 대신 상태를 그대로 보여준다. */
@Composable
private fun TrendStatusCard(message: String) {
    PocoCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = message, color = PocoTextMuted, fontSize = 13.sp)
    }
}

/** 규칙성 값 옆에 붙는 상태 라벨. 색상만으로 좋고 나쁨을 구분하지 않도록 항상 텍스트로 표시한다. */
@Composable
private fun regularityLabel(level: RegularityLevel): String = when (level) {
    RegularityLevel.INSUFFICIENT -> "기록 부족"
    RegularityLevel.STABLE -> "비교적 일정"
    RegularityLevel.SOMEWHAT_VARIABLE -> "다소 변동"
    RegularityLevel.HIGHLY_VARIABLE -> "변동이 큼"
}

private fun formatMinutesAsClock(minutes: Double): String {
    val m = ((minutes % 1440) + 1440) % 1440
    val h = (m / 60).toInt()
    val mm = (m % 60).toInt()
    return "%02d:%02d".format(h, mm)
}

/** "생활 규칙성 및 활동 변화" — 식사·수면·짧은 간격의 동일 활동·대화·미디어 활동을 한 카드에 모아 보여준다. */
@Composable
private fun LifeRegularityCard(analysis: TrendAnalysis, sleepState: TrendSourceState, dangerState: TrendSourceState) {
    PocoCard(modifier = Modifier.fillMaxWidth()) {
        CardTitle("생활 규칙성 및 활동 변화")
        Spacer(modifier = Modifier.height(4.dp))
        if (analysis.validDaysCurrent < 4) {
            Text(text = "아직 기록된 데이터가 없어요", color = PocoTextMuted, fontSize = 12.5.sp)
            return@PocoCard
        }
        Spacer(modifier = Modifier.height(10.dp))

        MetricRow("하루 평균 식사 기록", analysis.meal.dailyAverage?.let { "%.1f회".format(it) } ?: "데이터 없음")
        MetricRow("식사 기록 미확인일", "${analysis.meal.daysWithoutMealRecord}일")
        MetricRow("식사 시간 규칙성", regularityLabel(analysis.meal.regularityLevel))

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDividerLine()
        Spacer(modifier = Modifier.height(10.dp))

        when (sleepState) {
            is TrendSourceState.Error -> Text(text = sleepState.message, color = PocoTextMuted, fontSize = 12.5.sp)
            is TrendSourceState.Empty -> {
                MetricRow("평균 수면 시간", "아직 기록된 데이터가 없어요")
            }
            is TrendSourceState.Success -> {
                MetricRow("평균 수면 시간", analysis.sleep.averageDurationMinutes?.let { "%.1f시간".format(it / 60.0) } ?: "데이터 없음")
                MetricRow(
                    "취침 규칙성",
                    if (analysis.sleep.regularityLevel == RegularityLevel.INSUFFICIENT) "수면 기록이 부족해요"
                    else regularityLabel(analysis.sleep.regularityLevel) + (analysis.sleep.averageSleepStartMinutes?.let { " (평균 ${formatMinutesAsClock(it)})" } ?: "")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDividerLine()
        Spacer(modifier = Modifier.height(10.dp))

        MetricRow("짧은 간격의 동일 활동", "${analysis.repeatedActivity.count}회")
        Text(
            text = "짧은 간격에 같은 활동이 다시 기록된 횟수예요. 센서 감지 결과이므로 실제 행동과 다를 수 있어요.",
            color = PocoTextMuted, fontSize = 11.sp, lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDividerLine()
        Spacer(modifier = Modifier.height(10.dp))

        MetricRow("대화·미디어 활동", analysis.media.dailyAverageMinutes?.let { "일평균 %.0f분".format(it) } ?: "데이터 없음")
        Text(
            text = "대화·미디어 관련 소리가 감지된 시간이에요. 실제 인지 활동량과는 차이가 있을 수 있어요.",
            color = PocoTextMuted, fontSize = 11.sp, lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDividerLine()
        Spacer(modifier = Modifier.height(10.dp))

        if (dangerState is TrendSourceState.Error) {
            Text(text = dangerState.message, color = PocoTextMuted, fontSize = 12.5.sp)
        } else {
            MetricRow(
                "위험 감지 횟수",
                "${analysis.danger.totalCount}회" + (analysis.danger.absoluteChange?.let {
                    when {
                        it > 0 -> " (▲${it})"
                        it < 0 -> " (▼${-it})"
                        else -> ""
                    }
                } ?: "")
            )
            Text(
                text = "마이크로 감지된 위험 신호(비명·경적 등)의 횟수예요. 오탐이 섞여 있을 수 있어요.",
                color = PocoTextMuted, fontSize = 11.sp, lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = PocoTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(text = value, color = PocoTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun HorizontalDividerLine() {
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(PocoCardBackground))
}

/** "주요 변화" — 최대 3개. 증가/감소를 색상에만 의존하지 않도록 화살표 기호 + 문장을 함께 쓴다. */
@Composable
private fun KeyChangesCard(
    keyChanges: List<KeyChangeItem>,
    validDaysCurrent: Int,
    validDaysPrevious: Int,
    dangerState: TrendSourceState
) {
    PocoCard(modifier = Modifier.fillMaxWidth()) {
        CardTitle("주요 변화")
        if (dangerState is TrendSourceState.Error) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = dangerState.message, color = PocoTextMuted, fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))
        when {
            validDaysCurrent < 4 || validDaysPrevious < 4 ->
                Text(text = "비교할 기록이 아직 부족해요", color = PocoTextMuted, fontSize = 12.5.sp)
            keyChanges.isEmpty() ->
                Text(text = "이전 기간과 비교해 큰 변화가 확인되지 않았어요.", color = PocoTextMuted, fontSize = 12.5.sp)
            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                keyChanges.forEach { item ->
                    val glyph = when {
                        item.description.contains("늘었") -> "▲"
                        item.description.contains("줄었") -> "▼"
                        else -> "■"
                    }
                    Row {
                        Text(text = glyph, color = PocoTextMuted, fontSize = 13.sp, modifier = Modifier.padding(end = 6.dp))
                        Column {
                            Text(text = item.label, color = PocoTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = item.description, color = PocoTextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/** "생활 패턴 요약" — 규칙 기반 문장(최대 3개) + 비진단 고지. LLM 호출 없음. */
@Composable
private fun LifePatternSummaryCard(sentences: List<String>) {
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
            Text(text = "생활 패턴 요약", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            sentences.forEach { s ->
                Text(text = s, color = Color.White.copy(alpha = 0.92f), fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = TREND_NON_DIAGNOSTIC_NOTICE,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.5.sp,
            lineHeight = 14.sp
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 1200)
@Composable
private fun GuardianTrendScreenPreview() {
    val sampleSeries = TrendSeries(
        xLabels = listOf("월", "화", "수", "목", "금", "토", "일"),
        outing = listOf(1f, 0f, 2f, 1f, 1f, 0f, 1f),
        housework = listOf(3f, 2f, 4f, 3f, 2f, 1f, 3f)
    )
    POCOTheme {
        GuardianTrendScreen(
            selectedTab = GuardianTab.TREND,
            onTabSelected = {},
            hourlyRhythm = listOf(2, 1, 0, 0, 0, 1, 3, 6, 8, 7, 6, 8, 9, 7, 6, 8, 9, 8, 7, 6, 5, 4, 3, 2),
            trendState = TrendUiState.Ready(
                analysisByPeriod = TrendPeriod.entries.associateWith {
                    TrendAggregator.analyze(it, emptyList(), emptyList(), emptyList(), emptyList())
                },
                trendSeriesByPeriod = TrendPeriod.entries.associateWith { sampleSeries },
                sleepState = TrendSourceState.Empty,
                dangerState = TrendSourceState.Empty
            )
        )
    }
}
