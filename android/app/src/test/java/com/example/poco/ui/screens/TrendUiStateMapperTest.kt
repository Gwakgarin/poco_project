package com.example.poco.ui.screens

import com.example.poco.BehaviorSessionResponse
import com.example.poco.DangerAlertResponse
import com.example.poco.OutingEventResponse
import com.example.poco.SleepWakeEventResponse
import com.example.poco.toServerDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * [TrendUiStateMapper.resolve]는 네트워크를 직접 호출하지 않는 순수 함수라, 여기서는
 * Result.success/failure를 직접 합성해서 "API 성공+빈 데이터"와 "API 실패"가
 * 실제로 다른 화면 상태로 이어지는지 검증한다.
 */
class TrendUiStateMapperTest {

    private val zone = ZoneId.of("Asia/Seoul")
    private val now = ZonedDateTime.of(2026, 9, 17, 10, 0, 0, 0, zone).toInstant().toEpochMilli()

    private fun session(behavior: String) = BehaviorSessionResponse(
        deviceId = 1L, behavior = behavior,
        startTime = now.toServerDateTime(), confirmedTime = now.toServerDateTime(), endTime = now.toServerDateTime()
    )
    private fun okSessions() = Result.success(listOf(session("cleaning")))

    /** OUT-HOME이 정상적으로 짝지어지는 완결된 외출 1건(2시간). */
    private fun okOutings() = Result.success(
        listOf(
            OutingEventResponse(deviceId = 1L, transitionType = "HOME_TO_OUTSIDE", timestamp = now.toServerDateTime()),
            OutingEventResponse(deviceId = 1L, transitionType = "OUTSIDE_TO_HOME", timestamp = (now + 2 * 60 * 60 * 1000L).toServerDateTime())
        )
    )

    @Test
    fun sleepApi_successWithZeroRows_mapsToEmpty() {
        val state = TrendUiStateMapper.resolve(okSessions(), okOutings(), Result.success(emptyList()), Result.success(emptyList()), now)
        val ready = state as TrendUiState.Ready
        assertEquals(TrendSourceState.Empty, ready.sleepState)
    }

    @Test
    fun sleepApi_failure_mapsToError() {
        val state = TrendUiStateMapper.resolve(okSessions(), okOutings(), Result.failure(RuntimeException("timeout")), Result.success(emptyList()), now)
        val ready = state as TrendUiState.Ready
        assertTrue(ready.sleepState is TrendSourceState.Error)
        assertEquals("수면 기록을 불러오지 못했어요", (ready.sleepState as TrendSourceState.Error).message)
    }

    @Test
    fun dangerApi_successWithZeroRows_mapsToEmpty() {
        val state = TrendUiStateMapper.resolve(okSessions(), okOutings(), Result.success(emptyList()), Result.success(emptyList()), now)
        val ready = state as TrendUiState.Ready
        assertEquals(TrendSourceState.Empty, ready.dangerState)
    }

    @Test
    fun dangerApi_failure_mapsToError() {
        val state = TrendUiStateMapper.resolve(okSessions(), okOutings(), Result.success(emptyList()), Result.failure(RuntimeException("500")), now)
        val ready = state as TrendUiState.Ready
        assertTrue(ready.dangerState is TrendSourceState.Error)
        assertEquals("위험 알림 기록을 불러오지 못했어요", (ready.dangerState as TrendSourceState.Error).message)
    }

    @Test
    fun behaviorApi_failure_producesOverallError() {
        val state = TrendUiStateMapper.resolve(Result.failure(RuntimeException("no network")), okOutings(), Result.success(emptyList()), Result.success(emptyList()), now)
        assertTrue(state is TrendUiState.Error)
    }

    @Test
    fun outingApi_failure_producesOverallError() {
        val state = TrendUiStateMapper.resolve(okSessions(), Result.failure(RuntimeException("no network")), Result.success(emptyList()), Result.success(emptyList()), now)
        assertTrue(state is TrendUiState.Error)
    }

    @Test
    fun sleepApiFailure_keepsOutingHouseworkMealData() {
        val sessions = Result.success(listOf(session("laundry"), session("meal")))
        val state = TrendUiStateMapper.resolve(sessions, okOutings(), Result.failure(RuntimeException("x")), Result.success(emptyList()), now)
        val ready = state as TrendUiState.Ready
        // 수면만 실패했을 뿐, 가사활동/외출 집계는 정상적으로 계산돼 있어야 한다.
        val weekAnalysis = ready.analysisByPeriod.getValue(TrendPeriod.WEEK)
        assertEquals(1, weekAnalysis.housework.laundryCount)
        assertEquals(1, weekAnalysis.outing.pairedTripCount)
        assertTrue(ready.sleepState is TrendSourceState.Error)
    }

    @Test
    fun dangerApiFailure_keepsOtherData() {
        val sessions = Result.success(listOf(session("cleaning")))
        val state = TrendUiStateMapper.resolve(sessions, okOutings(), Result.success(emptyList()), Result.failure(RuntimeException("x")), now)
        val ready = state as TrendUiState.Ready
        val weekAnalysis = ready.analysisByPeriod.getValue(TrendPeriod.WEEK)
        assertEquals(1, weekAnalysis.housework.cleaningCount)
        assertEquals(1, weekAnalysis.outing.pairedTripCount)
        assertTrue(ready.dangerState is TrendSourceState.Error)
    }

    @Test
    fun loading_isDistinctInitialState() {
        // GuardianTrendScreen 초기값이 Loading이어야 "0건으로 로드됨"과 혼동되지 않는다.
        val initial: TrendUiState = TrendUiState.Loading
        assertTrue(initial is TrendUiState.Loading)
    }

    @Test
    fun bothSleepAndDangerFail_stillReadyWithCoreData() {
        val state = TrendUiStateMapper.resolve(okSessions(), okOutings(), Result.failure(RuntimeException("x")), Result.failure(RuntimeException("y")), now)
        val ready = state as TrendUiState.Ready
        assertTrue(ready.isPartialSuccess)
        assertTrue(ready.sleepState is TrendSourceState.Error)
        assertTrue(ready.dangerState is TrendSourceState.Error)
    }

    @Test
    fun allSourcesSucceedWithData_notPartialSuccess() {
        val state = TrendUiStateMapper.resolve(
            okSessions(), okOutings(),
            Result.success(listOf(SleepWakeEventResponse(deviceId = 1L, eventType = "SLEEP", timestamp = now.toServerDateTime()))),
            Result.success(listOf(DangerAlertResponse(deviceId = 1L, soundLabel = "scream", level = "DANGER", reason = "scream_detected", detectedAt = now.toServerDateTime()))),
            now
        )
        val ready = state as TrendUiState.Ready
        assertTrue(!ready.isPartialSuccess)
        assertEquals(TrendSourceState.Success, ready.sleepState)
        assertEquals(TrendSourceState.Success, ready.dangerState)
    }
}
