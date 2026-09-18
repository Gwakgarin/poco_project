package com.example.poco.ui.screens

/**
 * 장기 추세 화면(1주 · 1개월 · 6개월)이 쓰는 분석 결과 데이터 클래스.
 * 전부 [TrendAggregator]가 실제 behavior_sessions / outing_events / sleep_wake_events /
 * danger_alerts로 계산한다. 여기 있는 값 중 어떤 것도 의학적 판단(인지저하 의심 등)을 담지 않는다 —
 * "기록된 데이터를 비교한 결과"만 표현한다.
 */

/** 기간 대비 변화 상태. NORMAL이 아니면 changePercent는 항상 null. */
enum class ChangeStatus { NORMAL, NO_PREVIOUS_DATA, INSUFFICIENT_RECORD }

data class ChangeInfo(
    val status: ChangeStatus,
    val changePercent: Double? = null
)

enum class RegularityLevel { INSUFFICIENT, STABLE, SOMEWHAT_VARIABLE, HIGHLY_VARIABLE }

data class OutingStats(
    val pairedTripCount: Int,
    val dailyAverage: Double?,
    val daysWithOuting: Int,
    val outingDayRatioPercent: Double?,
    val averageDurationMinutes: Double?,
    val change: ChangeInfo
)

data class HouseworkStats(
    val totalCount: Int,
    val laundryCount: Int,
    val dishwashingCount: Int,
    val cleaningCount: Int,
    val dailyAverage: Double?,
    val daysWithActivity: Int,
    val change: ChangeInfo
)

data class MealStats(
    val totalCount: Int,
    val dailyAverage: Double?,
    val daysWithoutMealRecord: Int,
    val startTimeStdDevMinutes: Double?,
    val regularityLevel: RegularityLevel
)

data class SleepStats(
    val averageSleepStartMinutes: Double?,
    val averageWakeMinutes: Double?,
    val averageDurationMinutes: Double?,
    val sleepStartStdDevMinutes: Double?,
    val wakeStdDevMinutes: Double?,
    val regularityLevel: RegularityLevel,
    val durationChange: ChangeInfo,
    val validPairCount: Int
)

data class RepeatedActivityStats(val count: Int)

data class MediaActivityStats(
    val sessionCount: Int,
    val totalDurationMinutes: Double,
    val dailyAverageMinutes: Double?,
    val daysWithActivity: Int,
    val change: ChangeInfo
)

data class DangerStats(
    val totalCount: Int,
    val dangerCount: Int,
    val candidateCount: Int,
    val countByLabel: Map<String, Int>,
    val absoluteChange: Int?
)

data class KeyChangeItem(val label: String, val description: String)

/** 유효 기록일이 4일 미만이면(현재 또는 이전) 대부분의 비교·요약 문구가 "기록 부족"으로 대체된다. */
data class TrendAnalysis(
    val period: TrendPeriod,
    val validDaysCurrent: Int,
    val validDaysPrevious: Int,
    val outing: OutingStats,
    val housework: HouseworkStats,
    val meal: MealStats,
    val sleep: SleepStats,
    val repeatedActivity: RepeatedActivityStats,
    val media: MediaActivityStats,
    val danger: DangerStats,
    val keyChanges: List<KeyChangeItem>,
    val summarySentences: List<String>
)

/** "앱에 기록된 생활 데이터를 비교한 결과이며, 건강 상태나 질환을 진단하지 않습니다." — 화면 하단 고지 문구. 상수로 관리해 문구가 흩어지지 않게 한다. */
const val TREND_NON_DIAGNOSTIC_NOTICE =
    "앱에 기록된 생활 데이터를 비교한 결과이며, 건강 상태나 질환을 진단하지 않습니다."
