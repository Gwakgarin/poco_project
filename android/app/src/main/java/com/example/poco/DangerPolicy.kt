package com.example.poco

import com.example.poco.location.HomeState
import java.util.ArrayDeque

/** 서버로 전달할 위험 수준. 확정 위험과 위험 후보를 구분한다. */
enum class DangerLevel {
    DANGER,
    CANDIDATE
}

/** 위험 조건을 만족했을 때 생성되는 판단 결과. */
data class DangerDecision(
    val level: DangerLevel,
    val reason: String
)

/**
 * 소리 분류 결과와 현재 HOME/OUTSIDE 상태를 조합해 위험 알림 여부를 결정한다.
 *
 * scream은 위치와 관계없이 즉시 위험으로 판단하고, car_horn은 OUTSIDE 상태에서
 * [hornWindowMs] 안에 [hornRepeatCount]회 이상 감지됐을 때 위험 후보로 판단한다.
 */
class DangerPolicy(
    private val hornRepeatCount: Int = 3,
    private val hornWindowMs: Long = 30_000L,
    private val screamMinScore: Float = SCREAM_MIN_SCORE,
    private val screamMinDb: Double = SCREAM_MIN_DB
) {
    private val hornDetectedAt = ArrayDeque<Long>()

    /** 위험 조건이 아니면 null, 조건을 만족하면 알림에 사용할 판단 결과를 반환한다. */
    fun evaluate(
        label: String,
        homeState: HomeState,
        detectedAtEpochMs: Long,
        score: Float = 1f,
        segmentDb: Double = Double.POSITIVE_INFINITY
    ): DangerDecision? {
        return when (label.lowercase()) {
            // 분류기 확신도가 낮거나 소리가 작은 scream(큰 소리 오인식 후보)은 위험으로 확정하지 않는다.
            "scream" ->
                if (score >= screamMinScore && segmentDb >= screamMinDb) {
                    DangerDecision(DangerLevel.DANGER, "scream_detected")
                } else {
                    null
                }
            "car_horn" -> evaluateCarHorn(homeState, detectedAtEpochMs)
            else -> null
        }
    }

    /** OUTSIDE 상태의 경적 감지 시각만 누적해 짧은 시간 안의 반복 여부를 확인한다. */
    private fun evaluateCarHorn(homeState: HomeState, detectedAtEpochMs: Long): DangerDecision? {
        if (homeState != HomeState.OUTSIDE) {
            // 집 안이거나 위치 상태가 불명확하면 이전 경적 기록을 위험 판단에 사용하지 않는다.
            hornDetectedAt.clear()
            return null
        }

        // 현재 판단 시간보다 오래된 기록은 반복 횟수에서 제외한다.
        while (hornDetectedAt.isNotEmpty() && detectedAtEpochMs - hornDetectedAt.first() > hornWindowMs) {
            hornDetectedAt.removeFirst()
        }
        hornDetectedAt.addLast(detectedAtEpochMs)
        if (hornDetectedAt.size < hornRepeatCount) return null

        // 한 번 알림을 만든 기록을 다음 알림에 중복 사용하지 않도록 초기화한다.
        hornDetectedAt.clear()
        return DangerDecision(DangerLevel.CANDIDATE, "car_horn_repeated")
    }

    companion object {
        /** 테스트셋 기준 진짜 비명은 0.97 이상, 오탐(phone_alarm)은 0.41 이하. 실기기 로그로 조정할 것. */
        const val SCREAM_MIN_SCORE = 0.7f

        /** scream 확정에 필요한 5초 구간 최대 0.5초 창 dB. 0이면 사실상 비활성, 전시장 실측 후 설정할 것. */
        const val SCREAM_MIN_DB = 0.0
    }
}
