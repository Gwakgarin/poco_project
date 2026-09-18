package com.example.poco.ui.screens

import com.example.poco.BehaviorSessionResponse
import com.example.poco.DangerAlertResponse
import com.example.poco.OutingEventResponse
import com.example.poco.SleepWakeEventResponse

/**
 * 장기 추세 화면의 데이터 소스 하나(수면·위험알림 등)의 조회 상태.
 * "실패해서 0건"과 "성공했는데 실제로 0건"을 구분하기 위한 타입 — emptyList로 뭉개지 않는다.
 */
sealed class TrendSourceState {
    object Success : TrendSourceState()
    object Empty : TrendSourceState()
    data class Error(val message: String) : TrendSourceState()
}

/**
 * 장기 추세 화면 전체 상태.
 * - Loading: 조회 중
 * - Error: 핵심 데이터(behavior_sessions 또는 outing_events) 조회 자체가 실패 — 화면 전체를 오류로 보여준다.
 * - Ready: 핵심 데이터는 있고, 수면·위험알림은 [sleepState]/[dangerState]로 개별 상태를 들고 있다.
 *   sleepState/dangerState 중 하나라도 Error면 [Ready.isPartialSuccess]가 true — 별도의 "PartialSuccess" 분기를
 *   두지 않고 Ready 안에서 계산 프로퍼티로 표현한다(필드가 완전히 같은 상태를 이름만 다르게 중복시키지 않기 위함).
 */
sealed class TrendUiState {
    object Loading : TrendUiState()
    data class Error(val message: String) : TrendUiState()
    data class Ready(
        val analysisByPeriod: Map<TrendPeriod, TrendAnalysis>,
        val trendSeriesByPeriod: Map<TrendPeriod, TrendSeries>,
        val sleepState: TrendSourceState,
        val dangerState: TrendSourceState
    ) : TrendUiState() {
        val isPartialSuccess: Boolean
            get() = sleepState is TrendSourceState.Error || dangerState is TrendSourceState.Error
    }
}

/**
 * API 응답(Result)을 화면 상태로 변환하는 순수 함수. 네트워크 호출을 직접 하지 않으므로
 * Result.success/failure를 합성해 그대로 단위테스트할 수 있다 (TrendUiStateMapperTest 참고).
 */
object TrendUiStateMapper {

    private const val SLEEP_ERROR_MESSAGE = "수면 기록을 불러오지 못했어요"
    private const val DANGER_ERROR_MESSAGE = "위험 알림 기록을 불러오지 못했어요"
    private const val CORE_ERROR_MESSAGE = "데이터를 불러오지 못했어요"

    fun resolve(
        sessionsResult: Result<List<BehaviorSessionResponse>>,
        outingsResult: Result<List<OutingEventResponse>>,
        sleepResult: Result<List<SleepWakeEventResponse>>,
        dangerResult: Result<List<DangerAlertResponse>>,
        nowMillis: Long
    ): TrendUiState {
        if (sessionsResult.isFailure || outingsResult.isFailure) {
            return TrendUiState.Error(CORE_ERROR_MESSAGE)
        }
        val sessions = sessionsResult.getOrDefault(emptyList())
        val outings = outingsResult.getOrDefault(emptyList())
        val sleepEvents = sleepResult.getOrDefault(emptyList())
        val dangerAlerts = dangerResult.getOrDefault(emptyList())

        val sleepState: TrendSourceState = when {
            sleepResult.isFailure -> TrendSourceState.Error(SLEEP_ERROR_MESSAGE)
            sleepEvents.isEmpty() -> TrendSourceState.Empty
            else -> TrendSourceState.Success
        }
        val dangerState: TrendSourceState = when {
            dangerResult.isFailure -> TrendSourceState.Error(DANGER_ERROR_MESSAGE)
            dangerAlerts.isEmpty() -> TrendSourceState.Empty
            else -> TrendSourceState.Success
        }

        val seriesByPeriod = TrendPeriod.entries.associateWith { period ->
            TrendAggregator.aggregate(period, sessions, outings, nowMillis)
        }
        val analysisByPeriod = TrendPeriod.entries.associateWith { period ->
            TrendAggregator.analyze(period, sessions, outings, sleepEvents, dangerAlerts, nowMillis)
        }

        return TrendUiState.Ready(
            analysisByPeriod = analysisByPeriod,
            trendSeriesByPeriod = seriesByPeriod,
            sleepState = sleepState,
            dangerState = dangerState
        )
    }
}
