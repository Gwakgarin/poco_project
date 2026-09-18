package com.example.poco.ui.screens

import kotlin.math.abs

/**
 * "주요 변화"(최대 3개) 선택과 "생활 패턴 요약" 문장을 규칙 기반으로 만든다. LLM을 쓰지 않는다.
 * 여기서 만드는 문장은 사실만 담고, 의학적 해석("인지 저하 의심" 등)은 절대 포함하지 않는다.
 */
object TrendSummaryGenerator {

    private const val CHANGE_PERCENT_THRESHOLD = 20.0

    /** 하나의 "주요 변화" 후보. changePercent가 null이면(NORMAL이 아니면) 항상 후보에서 제외된다. */
    data class KeyChangeCandidate(
        val label: String,
        val changeInfo: ChangeInfo,
        val absoluteChangeMagnitude: Double,
        val minAbsoluteChange: Double,
        val describe: (ChangeInfo) -> String
    )

    /** 금지된 의료·진단성 표현. UI·요약 문장 어디에도 나오면 안 된다 — 테스트에서 이 목록으로 검사한다. */
    val FORBIDDEN_PHRASES = listOf(
        "인지 기능 저하 의심", "사회적 고립 의심", "치매 증상 악화",
        "진료 상담 권장", "건강 상태 위험", "즉각적인 조치 필요"
    )

    fun selectKeyChanges(candidates: List<KeyChangeCandidate>): List<KeyChangeItem> {
        return candidates
            .filter { it.changeInfo.status == ChangeStatus.NORMAL }
            .filter { abs(it.changeInfo.changePercent ?: 0.0) >= CHANGE_PERCENT_THRESHOLD }
            .filter { it.absoluteChangeMagnitude >= it.minAbsoluteChange }
            .sortedByDescending { abs(it.changeInfo.changePercent ?: 0.0) }
            .take(3)
            .map { KeyChangeItem(it.label, it.describe(it.changeInfo)) }
    }

    /** [TrendAggregator.analyze]가 실제 값으로 후보 목록을 만들 때 쓰는 오버로드. */
    fun selectKeyChanges(
        period: TrendPeriod,
        validDaysCurrent: Int,
        validDaysPrevious: Int,
        outing: OutingStats,
        housework: HouseworkStats,
        meal: MealStats,
        sleep: SleepStats,
        media: MediaActivityStats,
        danger: DangerStats
    ): List<KeyChangeItem> {
        // 1개월·6개월은 기간 길이를 그대로 확대하지 않고 "주 평균 등가"로 정규화해서 비교한다.
        val weeklyEquivalentFactor = when (period) {
            TrendPeriod.WEEK -> 1.0
            TrendPeriod.MONTH -> 7.0
            TrendPeriod.HALF_YEAR -> 7.0
        }

        val candidates = mutableListOf<KeyChangeCandidate>()

        candidates += KeyChangeCandidate(
            label = "외출",
            changeInfo = outing.change,
            absoluteChangeMagnitude = (outing.dailyAverage ?: 0.0) * weeklyEquivalentFactor,
            minAbsoluteChange = 2.0
        ) { describeDirection(it, "외출 활동이 늘었어요", "외출 활동이 비슷해요", "외출 활동이 줄었어요") }

        candidates += KeyChangeCandidate(
            label = "가사활동",
            changeInfo = housework.change,
            absoluteChangeMagnitude = (housework.dailyAverage ?: 0.0) * weeklyEquivalentFactor,
            minAbsoluteChange = 3.0
        ) { describeDirection(it, "가사활동이 늘었어요", "가사활동이 비슷해요", "가사활동이 줄었어요") }

        candidates += KeyChangeCandidate(
            label = "대화·미디어 활동",
            changeInfo = media.change,
            absoluteChangeMagnitude = media.dailyAverageMinutes ?: 0.0,
            minAbsoluteChange = 30.0
        ) { describeDirection(it, "대화·미디어 활동 시간이 늘었어요", "대화·미디어 활동 시간이 비슷해요", "대화·미디어 활동 시간이 줄었어요") }

        candidates += KeyChangeCandidate(
            label = "평균 수면 시간",
            changeInfo = sleep.durationChange,
            absoluteChangeMagnitude = abs(minutesDelta(sleep.durationChange, sleep.averageDurationMinutes)),
            minAbsoluteChange = 60.0
        ) { describeDirection(it, "평균 수면 시간이 늘었어요", "평균 수면 시간이 비슷해요", "평균 수면 시간이 줄었어요") }

        if (danger.absoluteChange != null && validDaysCurrent >= 4 && validDaysPrevious >= 4 && abs(danger.absoluteChange) >= 2) {
            val direction = if (danger.absoluteChange > 0) "늘었어요" else "줄었어요"
            candidates += KeyChangeCandidate(
                label = "위험 알림",
                changeInfo = ChangeInfo(ChangeStatus.NORMAL, 100.0), // 퍼센트 임계값 통과용 — 위험 알림은 절대 건수 기준
                absoluteChangeMagnitude = 999.0,
                minAbsoluteChange = 0.0
            ) { "위험 후보 알림이 이전 기간보다 ${abs(danger.absoluteChange)}회 $direction" }
        }

        return selectKeyChanges(candidates)
    }

    private fun minutesDelta(change: ChangeInfo, current: Double?): Double {
        if (change.status != ChangeStatus.NORMAL || current == null) return 0.0
        val percent = change.changePercent ?: return 0.0
        val previous = current / (1 + percent / 100.0)
        return current - previous
    }

    private fun describeDirection(change: ChangeInfo, up: String, similar: String, down: String): String {
        val percent = change.changePercent ?: 0.0
        val phrase = when {
            percent >= 20.0 -> up
            percent <= -20.0 -> down
            else -> similar
        }
        return "이전 기간보다 $phrase"
    }

    /** 데이터 충분성 → 주요 변화 결과 순으로 최대 3문장. 어떤 경우에도 의학적 해석을 담지 않는다. */
    fun buildSummary(validDaysCurrent: Int, validDaysPrevious: Int, keyChanges: List<KeyChangeItem>): List<String> {
        if (validDaysCurrent < 4 || validDaysPrevious < 4) {
            return listOf("비교할 기록이 아직 부족해요. 활동이 더 기록되면 이전 기간과 비교해 보여드릴게요.")
        }
        if (keyChanges.isEmpty()) {
            return listOf("이전 기간과 비교해 큰 변화가 확인되지 않았어요.")
        }
        return keyChanges.take(3).map { it.description }
    }
}
