package com.example.poco.ui.screens

import com.example.poco.BehaviorSessionResponse
import com.example.poco.DangerAlertResponse
import com.example.poco.OutingEventResponse
import com.example.poco.SleepWakeEventResponse
import com.example.poco.toServerDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class TrendAggregatorTest {

    private val zone: ZoneId = ZoneId.of("Asia/Seoul")

    // 2026-09-17 10:00 Asia/Seoul 고정 — 월/연 경계에서 멀지 않아 계산이 애매해지지 않음.
    private val nowZdt: ZonedDateTime = ZonedDateTime.of(2026, 9, 17, 10, 0, 0, 0, zone)
    private val now = nowZdt.toInstant().toEpochMilli()

    private fun at(daysAgo: Long, hour: Int, minute: Int): ZonedDateTime =
        nowZdt.minusDays(daysAgo).withHour(hour).withMinute(minute).withSecond(0).withNano(0)

    private fun ms(z: ZonedDateTime): Long = z.toInstant().toEpochMilli()

    private fun session(behavior: String, start: ZonedDateTime, durationMinutes: Long = 5): BehaviorSessionResponse {
        val s = ms(start)
        val e = s + durationMinutes * 60_000
        return BehaviorSessionResponse(deviceId = 1L, behavior = behavior, startTime = s.toServerDateTime(), confirmedTime = s.toServerDateTime(), endTime = e.toServerDateTime())
    }

    private fun outing(type: String, at: ZonedDateTime) = OutingEventResponse(deviceId = 1L, transitionType = type, timestamp = ms(at).toServerDateTime())
    private fun sleepWake(type: String, at: ZonedDateTime) = SleepWakeEventResponse(deviceId = 1L, eventType = type, timestamp = ms(at).toServerDateTime())
    private fun danger(level: String, label: String, at: ZonedDateTime) = DangerAlertResponse(deviceId = 1L, soundLabel = label, level = level, reason = "x", detectedAt = ms(at).toServerDateTime())

    /** 현재 주(daysAgo 1~6)와 직전 주(daysAgo 8~13)에 매일 활동을 채워, 유효기록일 조건(>=4일 양쪽)을 쉽게 만족시키는 배경 데이터. */
    private fun richBackground(): Triple<List<BehaviorSessionResponse>, List<OutingEventResponse>, List<SleepWakeEventResponse>> {
        val sessions = mutableListOf<BehaviorSessionResponse>()
        val outings = mutableListOf<OutingEventResponse>()
        val sleeps = mutableListOf<SleepWakeEventResponse>()
        for (d in listOf(1L, 2L, 3L, 4L, 5L, 6L, 8L, 9L, 10L, 11L, 12L, 13L)) {
            sessions.add(session("cleaning", at(d, 10, 0)))
            outings.add(outing("HOME_TO_OUTSIDE", at(d, 9, 0)))
            outings.add(outing("OUTSIDE_TO_HOME", at(d, 10, 0)))
            sleeps.add(sleepWake("SLEEP", at(d + 1, 23, 0)))
            sleeps.add(sleepWake("WAKE", at(d, 7, 0)))
        }
        return Triple(sessions, outings, sleeps)
    }

    // ===================== [기간] =====================

    @Test
    fun period_week_currentAndPreviousBoundaries() {
        val (cur, prev) = TrendAggregator.currentAndPreviousWindows(TrendPeriod.WEEK, now)
        assertEquals(ms(at(6, 0, 0)), cur.first)
        assertEquals(ms(at(-1, 0, 0)), cur.last + 1) // curEnd = 내일 자정
        assertEquals(ms(at(13, 0, 0)), prev.first)
        assertEquals(cur.first, prev.last + 1)
    }

    @Test
    fun period_month_currentAndPreviousBoundaries() {
        val (cur, prev) = TrendAggregator.currentAndPreviousWindows(TrendPeriod.MONTH, now)
        assertEquals(ms(at(27, 0, 0)), cur.first)
        assertEquals(28 * 24 * 60 * 60 * 1000L, cur.last + 1 - cur.first)
        assertEquals(cur.first, prev.last + 1)
        assertEquals(28 * 24 * 60 * 60 * 1000L, prev.last + 1 - prev.first)
    }

    @Test
    fun period_halfYear_crossesYearBoundary() {
        val (cur, prev) = TrendAggregator.currentAndPreviousWindows(TrendPeriod.HALF_YEAR, now)
        // 현재: 2026-04-01 ~ 2026-10-01, 이전: 2025-10-01 ~ 2026-04-01 (연도 경계를 넘어감)
        val curStart = ZonedDateTime.of(2026, 4, 1, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        val curEnd = ZonedDateTime.of(2026, 10, 1, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        val prevStart = ZonedDateTime.of(2025, 10, 1, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(curStart, cur.first)
        assertEquals(curEnd, cur.last + 1)
        assertEquals(prevStart, prev.first)
        assertEquals(curStart, prev.last + 1)
    }

    @Test
    fun period_asiaSeoulDayBoundary_separatesConsecutiveMinutes() {
        val justBeforeMidnight = session("cleaning", at(2, 23, 59))
        val justAfterMidnight = session("cleaning", at(1, 0, 1))
        val window = TrendAggregator.currentAndPreviousWindows(TrendPeriod.WEEK, now).first
        val validDays = TrendAggregator.countValidDays(window, listOf(justBeforeMidnight, justAfterMidnight), emptyList(), emptyList(), emptyList())
        assertEquals(2, validDays) // 1분 차이지만 자정을 넘어서 서로 다른 유효 기록일 2개
    }

    // ===================== [변화율] =====================

    @Test
    fun change_normalPercent() {
        val info = TrendAggregator.changeInfo(current = 12.0, previous = 10.0, validDaysCurrent = 5, validDaysPrevious = 5)
        assertEquals(ChangeStatus.NORMAL, info.status)
        assertEquals(20.0, info.changePercent!!, 0.001)
    }

    @Test
    fun change_previousZero_returnsNoPreviousData() {
        val info = TrendAggregator.changeInfo(current = 3.0, previous = 0.0, validDaysCurrent = 5, validDaysPrevious = 5)
        assertEquals(ChangeStatus.NO_PREVIOUS_DATA, info.status)
        assertNull(info.changePercent)
    }

    @Test
    fun change_insufficientValidDays() {
        val info = TrendAggregator.changeInfo(current = 3.0, previous = 2.0, validDaysCurrent = 2, validDaysPrevious = 5)
        assertEquals(ChangeStatus.INSUFFICIENT_RECORD, info.status)
        assertNull(info.changePercent)
    }

    @Test
    fun change_neverProducesNaNOrInfinite() {
        val cases = listOf(0.0 to 0.0, 100.0 to 0.0, 0.0 to 100.0, -5.0 to 5.0)
        cases.forEach { (c, p) ->
            val info = TrendAggregator.changeInfo(c, p, 5, 5)
            info.changePercent?.let {
                assertFalse("NaN 이면 안됨: $c/$p", it.isNaN())
                assertFalse("Infinite 면 안됨: $c/$p", it.isInfinite())
            }
        }
    }

    // ===================== [외출] =====================

    @Test
    fun outing_normalPairCountedWithDuration() {
        val outings = listOf(
            outing("HOME_TO_OUTSIDE", at(2, 9, 0)),
            outing("OUTSIDE_TO_HOME", at(2, 11, 0))
        )
        val analysis = TrendAggregator.analyze(TrendPeriod.WEEK, emptyList(), outings, emptyList(), emptyList(), now)
        assertEquals(1, analysis.outing.pairedTripCount)
        assertEquals(120.0, analysis.outing.averageDurationMinutes!!, 0.001)
    }

    @Test
    fun outing_unmatchedEventsExcluded() {
        val outings = listOf(
            outing("OUTSIDE_TO_HOME", at(4, 15, 0)), // 앞서는 OUT이 전혀 없는 고아 HOME -> 버려짐
            outing("HOME_TO_OUTSIDE", at(2, 9, 0)) // 이후 HOME이 전혀 없는 고아 OUT(진행중 외출) -> 버려짐
        )
        val analysis = TrendAggregator.analyze(TrendPeriod.WEEK, emptyList(), outings, emptyList(), emptyList(), now)
        assertEquals(0, analysis.outing.pairedTripCount)
    }

    @Test
    fun outing_duplicateOutKeepsEarliest() {
        val outings = listOf(
            outing("HOME_TO_OUTSIDE", at(2, 9, 0)),
            outing("HOME_TO_OUTSIDE", at(2, 9, 30)), // 중복 OUT — 무시돼야 함
            outing("OUTSIDE_TO_HOME", at(2, 11, 0))
        )
        val analysis = TrendAggregator.analyze(TrendPeriod.WEEK, emptyList(), outings, emptyList(), emptyList(), now)
        assertEquals(1, analysis.outing.pairedTripCount)
        assertEquals(120.0, analysis.outing.averageDurationMinutes!!, 0.001) // 09:00 기준으로 계산돼야 함(09:30 아님)
    }

    @Test
    fun outing_pairCrossingMidnightDurationCorrect() {
        val outings = listOf(
            outing("HOME_TO_OUTSIDE", at(3, 23, 0)),
            outing("OUTSIDE_TO_HOME", at(2, 1, 0)) // 다음날 새벽 귀가
        )
        val analysis = TrendAggregator.analyze(TrendPeriod.WEEK, emptyList(), outings, emptyList(), emptyList(), now)
        assertEquals(1, analysis.outing.pairedTripCount)
        assertEquals(120.0, analysis.outing.averageDurationMinutes!!, 0.001)
    }

    // ===================== [가사활동] =====================

    @Test
    fun housework_countsByBehaviorType() {
        val sessions = listOf(
            session("laundry", at(2, 10, 0)),
            session("dishwashing", at(2, 15, 0)),
            session("cleaning", at(2, 18, 0)),
            session("meal", at(2, 12, 0)) // 가사활동 아님 — 제외돼야 함
        )
        val analysis = TrendAggregator.analyze(TrendPeriod.WEEK, sessions, emptyList(), emptyList(), emptyList(), now)
        assertEquals(3, analysis.housework.totalCount)
        assertEquals(1, analysis.housework.laundryCount)
        assertEquals(1, analysis.housework.dishwashingCount)
        assertEquals(1, analysis.housework.cleaningCount)
    }

    @Test
    fun housework_mergedWithin10Minutes() {
        val first = session("cleaning", at(2, 10, 0), durationMinutes = 5) // 10:00~10:05
        val second = session("cleaning", at(2, 10, 10), durationMinutes = 5) // 10:10 시작 (종료 후 5분 뒤)
        val merged = TrendAggregator.mergeSessions(listOf(first, second))
        assertEquals(1, merged.size)
    }

    @Test
    fun housework_notMergedBeyond10Minutes() {
        val first = session("cleaning", at(2, 10, 0), durationMinutes = 5) // ~10:05 종료
        val second = session("cleaning", at(2, 10, 16), durationMinutes = 5) // 11분 뒤 시작
        val merged = TrendAggregator.mergeSessions(listOf(first, second))
        assertEquals(2, merged.size)
    }

    @Test
    fun housework_overlappingSessionsMerged() {
        val first = session("cleaning", at(2, 10, 0), durationMinutes = 20) // 10:00~10:20
        val second = session("cleaning", at(2, 10, 10), durationMinutes = 20) // 10:10 시작(겹침)
        val merged = TrendAggregator.mergeSessions(listOf(first, second))
        assertEquals(1, merged.size)
        assertEquals(ms(at(2, 10, 30)), merged[0].second) // 더 늦게 끝나는 쪽 종료시각 채택
    }

    // ===================== [식사] =====================

    @Test
    fun meal_dailyAverageComputed() {
        val sessions = listOf(1L, 2L, 3L, 4L).map { session("meal", at(it, 8, 0)) }
        val analysis = TrendAggregator.analyze(TrendPeriod.WEEK, sessions, emptyList(), emptyList(), emptyList(), now)
        assertEquals(4, analysis.meal.totalCount)
        assertNotNull(analysis.meal.dailyAverage)
    }

    @Test
    fun meal_daysWithoutMealRecordCounted() {
        // 현재 주(7일: daysAgo 0~6)에서 식사 기록이 있는 날은 2일뿐
        val sessions = listOf(session("meal", at(1, 8, 0)), session("meal", at(2, 8, 0)))
        val analysis = TrendAggregator.analyze(TrendPeriod.WEEK, sessions, emptyList(), emptyList(), emptyList(), now)
        val (currentWindow, _) = TrendAggregator.currentAndPreviousWindows(TrendPeriod.WEEK, now)
        val validDays = TrendAggregator.countValidDays(currentWindow, sessions, emptyList(), emptyList(), emptyList())
        assertEquals(validDays - 2, analysis.meal.daysWithoutMealRecord)
    }

    @Test
    fun meal_usesOnlyFirstMealOfDay() {
        val sameDayTwoMeals = listOf(
            session("meal", at(2, 8, 0)),
            session("meal", at(2, 19, 0))
        )
        // 다른 3일치를 더해 표준편차 계산에 필요한 4일 이상을 채움 (모두 08:00 근처로 일정하게)
        val others = listOf(3L, 4L, 5L).map { session("meal", at(it, 8, 0)) }
        val analysis = TrendAggregator.analyze(TrendPeriod.WEEK, sameDayTwoMeals + others, emptyList(), emptyList(), emptyList(), now)
        // 하루에 08:00, 19:00 두 끼가 있어도 규칙성 계산엔 08:00(첫 끼)만 들어가므로 표준편차가 매우 작아야 함(19:00이 섞이면 커짐)
        assertTrue(analysis.meal.startTimeStdDevMinutes!! < 30.0)
    }

    @Test
    fun meal_stdDevComputedWhenEnoughData() {
        val sessions = listOf(
            session("meal", at(1, 8, 0)),
            session("meal", at(2, 8, 30)),
            session("meal", at(3, 7, 45)),
            session("meal", at(4, 8, 10))
        )
        val analysis = TrendAggregator.analyze(TrendPeriod.WEEK, sessions, emptyList(), emptyList(), emptyList(), now)
        assertEquals(RegularityLevel.STABLE, analysis.meal.regularityLevel)
        assertNotNull(analysis.meal.startTimeStdDevMinutes)
    }

    @Test
    fun meal_overnightBoundaryDoesNotDistortAverage() {
        // 첫 끼 시각이 23:30 / 00:30 처럼 자정을 걸치는 경우에도 표준편차가 크게 왜곡되면 안 됨
        val sessions = listOf(
            session("meal", at(1, 23, 30)),
            session("meal", at(2, 0, 30)),
            session("meal", at(3, 23, 45)),
            session("meal", at(4, 0, 15))
        )
        val analysis = TrendAggregator.analyze(TrendPeriod.WEEK, sessions, emptyList(), emptyList(), emptyList(), now)
        // 보정 없이 그냥 평균 내면 표준편차가 몇 백 분 단위로 튀지만, 자정 보정을 하면 1시간 이내여야 함
        assertTrue("자정 보정이 안 되면 표준편차가 비정상적으로 커짐: ${analysis.meal.startTimeStdDevMinutes}", analysis.meal.startTimeStdDevMinutes!! < 60.0)
    }

    @Test
    fun meal_under4Days_insufficientRegularity() {
        val sessions = listOf(session("meal", at(1, 8, 0)), session("meal", at(2, 8, 0)))
        val analysis = TrendAggregator.analyze(TrendPeriod.WEEK, sessions, emptyList(), emptyList(), emptyList(), now)
        assertEquals(RegularityLevel.INSUFFICIENT, analysis.meal.regularityLevel)
        assertNull(analysis.meal.startTimeStdDevMinutes)
    }

    // ===================== [수면] =====================

    @Test
    fun sleep_normalPairCounted() {
        val events = listOf(sleepWake("SLEEP", at(2, 23, 0)), sleepWake("WAKE", at(1, 7, 0)))
        val analysis = TrendAggregator.analyze(TrendPeriod.WEEK, emptyList(), emptyList(), events, emptyList(), now)
        assertEquals(1, analysis.sleep.validPairCount)
        assertEquals(480.0, analysis.sleep.averageDurationMinutes!!, 0.001)
    }

    @Test
    fun sleep_shorterThan2HoursExcluded() {
        val events = listOf(sleepWake("SLEEP", at(2, 23, 0)), sleepWake("WAKE", at(2, 23, 30)))
        val analysis = TrendAggregator.analyze(TrendPeriod.WEEK, emptyList(), emptyList(), events, emptyList(), now)
        assertEquals(0, analysis.sleep.validPairCount)
    }

    @Test
    fun sleep_longerThan16HoursExcluded() {
        val events = listOf(sleepWake("SLEEP", at(3, 20, 0)), sleepWake("WAKE", at(2, 14, 0))) // 18시간
        val analysis = TrendAggregator.analyze(TrendPeriod.WEEK, emptyList(), emptyList(), events, emptyList(), now)
        assertEquals(0, analysis.sleep.validPairCount)
    }

    @Test
    fun sleep_overnightCrossingHandled() {
        val events = listOf(sleepWake("SLEEP", at(2, 23, 30)), sleepWake("WAKE", at(1, 7, 0)))
        val analysis = TrendAggregator.analyze(TrendPeriod.WEEK, emptyList(), emptyList(), events, emptyList(), now)
        assertEquals(1, analysis.sleep.validPairCount)
        assertTrue(analysis.sleep.averageSleepStartMinutes!! > 1400.0) // 23:30 -> 보정 후 1410분 근처
    }

    @Test
    fun sleep_startStdDevComputed() {
        val events = listOf(1L, 2L, 3L, 4L).flatMap { d -> listOf(sleepWake("SLEEP", at(d + 1, 23, 0)), sleepWake("WAKE", at(d, 7, 0))) }
        val analysis = TrendAggregator.analyze(TrendPeriod.WEEK, emptyList(), emptyList(), events, emptyList(), now)
        assertEquals(RegularityLevel.STABLE, analysis.sleep.regularityLevel)
        assertNotNull(analysis.sleep.sleepStartStdDevMinutes)
    }

    @Test
    fun sleep_under4Pairs_insufficientRegularity() {
        val events = listOf(sleepWake("SLEEP", at(2, 23, 0)), sleepWake("WAKE", at(1, 7, 0)))
        val analysis = TrendAggregator.analyze(TrendPeriod.WEEK, emptyList(), emptyList(), events, emptyList(), now)
        assertEquals(RegularityLevel.INSUFFICIENT, analysis.sleep.regularityLevel)
    }

    // ===================== [짧은 간격의 동일 활동] =====================

    @Test
    fun repeated_within60MinutesCounted() {
        val first = session("dishwashing", at(2, 10, 0), durationMinutes = 5) // ~10:05 종료
        val second = session("dishwashing", at(2, 10, 35), durationMinutes = 5) // 30분 뒤 시작(10분 초과, 60분 이내)
        val analysis = TrendAggregator.analyze(TrendPeriod.WEEK, listOf(first, second), emptyList(), emptyList(), emptyList(), now)
        assertEquals(1, analysis.repeatedActivity.count)
    }

    @Test
    fun repeated_differentBehaviorNotCounted() {
        val first = session("dishwashing", at(2, 10, 0), durationMinutes = 5)
        val second = session("laundry", at(2, 10, 35), durationMinutes = 5) // 다른 behavior
        val analysis = TrendAggregator.analyze(TrendPeriod.WEEK, listOf(first, second), emptyList(), emptyList(), emptyList(), now)
        assertEquals(0, analysis.repeatedActivity.count)
    }

    @Test
    fun repeated_alreadyMergedNotDoubleCounted() {
        val a = session("cleaning", at(2, 10, 0), durationMinutes = 5)   // 10:00~10:05
        val b = session("cleaning", at(2, 10, 10), durationMinutes = 5) // 5분 뒤 -> a와 병합됨
        val c = session("cleaning", at(2, 10, 45), durationMinutes = 5) // 병합된 세션 종료(10:15) 후 30분 뒤 -> 반복 1회만 카운트
        val analysis = TrendAggregator.analyze(TrendPeriod.WEEK, listOf(a, b, c), emptyList(), emptyList(), emptyList(), now)
        assertEquals(1, analysis.repeatedActivity.count)
    }

    // ===================== [요약 / 주요 변화] =====================

    @Test
    fun keyChanges_maxThreeItems() {
        val candidates = (1..5).map { i ->
            TrendSummaryGenerator.KeyChangeCandidate(
                label = "지표$i",
                changeInfo = ChangeInfo(ChangeStatus.NORMAL, 50.0),
                absoluteChangeMagnitude = 100.0,
                minAbsoluteChange = 0.0
            ) { "변화 $i" }
        }
        val result = TrendSummaryGenerator.selectKeyChanges(candidates)
        assertEquals(3, result.size)
    }

    @Test
    fun keyChanges_excludesBelowMinimumAbsoluteChange() {
        val candidates = listOf(
            TrendSummaryGenerator.KeyChangeCandidate("작은 변화", ChangeInfo(ChangeStatus.NORMAL, 50.0), absoluteChangeMagnitude = 0.5, minAbsoluteChange = 2.0) { "무시돼야 함" }
        )
        assertTrue(TrendSummaryGenerator.selectKeyChanges(candidates).isEmpty())
    }

    @Test
    fun keyChanges_excludesBelowPercentThreshold() {
        val candidates = listOf(
            TrendSummaryGenerator.KeyChangeCandidate("작은 퍼센트", ChangeInfo(ChangeStatus.NORMAL, 5.0), absoluteChangeMagnitude = 100.0, minAbsoluteChange = 0.0) { "무시돼야 함" }
        )
        assertTrue(TrendSummaryGenerator.selectKeyChanges(candidates).isEmpty())
    }

    @Test
    fun summary_noSignificantChange_fallbackMessage() {
        val summary = TrendSummaryGenerator.buildSummary(validDaysCurrent = 5, validDaysPrevious = 5, keyChanges = emptyList())
        assertEquals(listOf("이전 기간과 비교해 큰 변화가 확인되지 않았어요."), summary)
    }

    @Test
    fun summary_insufficientData_fallbackMessage() {
        val summary = TrendSummaryGenerator.buildSummary(validDaysCurrent = 2, validDaysPrevious = 5, keyChanges = emptyList())
        assertEquals(1, summary.size)
        assertTrue(summary[0].contains("비교할 기록이 아직 부족"))
    }

    @Test
    fun summary_neverContainsForbiddenMedicalPhrases() {
        val (sessions, outings, sleeps) = richBackground()
        val analysis = TrendAggregator.analyze(TrendPeriod.WEEK, sessions, outings, sleeps, emptyList(), now)
        val allText = analysis.summarySentences.joinToString(" ") + analysis.keyChanges.joinToString(" ") { it.description }
        TrendSummaryGenerator.FORBIDDEN_PHRASES.forEach { phrase ->
            assertFalse("금지된 표현이 포함됨: $phrase", allText.contains(phrase))
        }
    }

    @Test
    fun analyze_richBackground_producesValidDaysAndDoesNotCrash() {
        val (sessions, outings, sleeps) = richBackground()
        val analysis = TrendAggregator.analyze(TrendPeriod.WEEK, sessions, outings, sleeps, emptyList(), now)
        assertTrue(analysis.validDaysCurrent >= 4)
        assertTrue(analysis.validDaysPrevious >= 4)
    }

    // ===================== 기존 "장기 활동 추이" 차트 집계 (유지) =====================

    private val sessionsLegacy = listOf(
        session("cleaning", at(1, 10, 0)),
        session("laundry", at(20, 10, 0)),
        session("dishwashing", at(150, 10, 0)),
        session("meal", at(1, 12, 0))
    )
    private val outingsLegacy = listOf(
        outing("HOME_TO_OUTSIDE", at(1, 9, 0)),
        outing("OUTSIDE_TO_HOME", at(1, 11, 0)),
        outing("HOME_TO_OUTSIDE", at(20, 9, 0)),
        outing("HOME_TO_OUTSIDE", at(150, 9, 0))
    )

    @Test
    fun legacyChart_week_onlyCountsLast7Days() {
        val result = TrendAggregator.aggregate(TrendPeriod.WEEK, sessionsLegacy, outingsLegacy, now)
        assertEquals(7, result.xLabels.size)
        assertEquals(1f, result.housework.sum())
        assertEquals(1f, result.outing.sum())
    }

    @Test
    fun legacyChart_month_countsLast28Days() {
        val result = TrendAggregator.aggregate(TrendPeriod.MONTH, sessionsLegacy, outingsLegacy, now)
        assertEquals(4, result.xLabels.size)
        assertEquals(2f, result.housework.sum())
        assertEquals(2f, result.outing.sum())
    }

    @Test
    fun legacyChart_halfYear_countsLast6CalendarMonths() {
        val result = TrendAggregator.aggregate(TrendPeriod.HALF_YEAR, sessionsLegacy, outingsLegacy, now)
        assertEquals(6, result.xLabels.size)
        assertEquals(3f, result.housework.sum())
        assertEquals(3f, result.outing.sum())
    }

    @Test
    fun legacyChart_emptyInput_producesZeroedSeriesNotCrash() {
        val result = TrendAggregator.aggregate(TrendPeriod.WEEK, emptyList(), emptyList(), now)
        assertEquals(7, result.outing.size)
        assertEquals(0f, result.outing.sum())
        assertEquals(0f, result.housework.sum())
    }
}
