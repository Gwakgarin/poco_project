package com.example.poco.ui.screens

import com.example.poco.BehaviorSessionResponse
import com.example.poco.DangerAlertResponse
import com.example.poco.OutingEventResponse
import com.example.poco.SleepWakeEventResponse
import com.example.poco.fromServerDateTime
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.sqrt

/**
 * behavior_sessions / outing_events / sleep_wake_events / danger_alerts 실데이터를
 * 장기 추세 화면이 필요로 하는 시계열·통계로 집계한다. 전부 순수 함수라 단위테스트로 검증한다.
 *
 * "가사 활동"은 cleaning · laundry · dishwashing 세 behavior 값만 센다.
 * "대화·미디어 활동"은 cognitive behavior 값을 센다 (PocoNavHost의 "대화·TV 활동" 표시와 동일 기준).
 */
object TrendAggregator {

    private val HOUSEWORK_BEHAVIORS = setOf("cleaning", "laundry", "dishwashing")
    private const val MEDIA_BEHAVIOR = "cognitive"
    private const val MEAL_BEHAVIOR = "meal"
    private val MERGEABLE_BEHAVIORS = setOf("cleaning", "laundry", "dishwashing", "meal")

    private const val MERGE_GAP_MINUTES = 10L
    private const val REPEATED_GAP_MINUTES = 60L
    private const val MIN_VALID_DAYS_FOR_COMPARE = 4
    private const val MIN_VALID_DAYS_FOR_REGULARITY = 4
    private const val CHANGE_PERCENT_THRESHOLD = 20.0
    private const val MIN_SLEEP_MINUTES = 120L
    private const val MAX_SLEEP_MINUTES = 16 * 60L

    // =====================================================================
    // 기존: 1주/1개월/6개월 "장기 활동 추이" 차트용 시계열 (외출·가사 원시 카운트)
    // 외출 "1건" = HOME_TO_OUTSIDE 이벤트 1개 (아래 analyze()의 "왕복 쌍" 정의와는 다름 — 차트는 원시 건수,
    // analyze()는 실제 왕복이 성립한 외출만 센다. 두 정의가 달라 보여도 의도적으로 유지한다.)
    // =====================================================================
    fun aggregate(
        period: TrendPeriod,
        sessions: List<BehaviorSessionResponse>,
        outings: List<OutingEventResponse>,
        nowMillis: Long = System.currentTimeMillis()
    ): TrendSeries {
        val houseworkTimes = sessions
            .filter { it.behavior in HOUSEWORK_BEHAVIORS }
            .mapNotNull { it.startTime.fromServerDateTime() }
        val outingTimes = outings
            .filter { it.transitionType == "HOME_TO_OUTSIDE" }
            .mapNotNull { it.timestamp.fromServerDateTime() }

        val buckets = buildBuckets(period, nowMillis)
        val outing = buckets.map { (start, endExclusive) -> outingTimes.count { it in start until endExclusive }.toFloat() }
        val housework = buckets.map { (start, endExclusive) -> houseworkTimes.count { it in start until endExclusive }.toFloat() }
        val xLabels = labelsFor(period, nowMillis)

        return TrendSeries(xLabels = xLabels, outing = outing, housework = housework)
    }

    // =====================================================================
    // 신규: 생활 규칙성 및 활동 변화 / 주요 변화 / 생활 패턴 요약
    // =====================================================================

    fun analyze(
        period: TrendPeriod,
        sessions: List<BehaviorSessionResponse>,
        outings: List<OutingEventResponse>,
        sleepWakeEvents: List<SleepWakeEventResponse>,
        dangerAlerts: List<DangerAlertResponse>,
        nowMillis: Long = System.currentTimeMillis()
    ): TrendAnalysis {
        val (currentWindow, previousWindow) = currentAndPreviousWindows(period, nowMillis)

        val validDaysCurrent = countValidDays(currentWindow, sessions, outings, sleepWakeEvents, dangerAlerts)
        val validDaysPrevious = countValidDays(previousWindow, sessions, outings, sleepWakeEvents, dangerAlerts)

        val outingCurrent = pairOutings(inWindow(outings) { it.timestamp }.filter { it.timestamp.fromServerDateTime() in currentWindow })
        val outingPrevious = pairOutings(inWindow(outings) { it.timestamp }.filter { it.timestamp.fromServerDateTime() in previousWindow })

        val houseworkCurrent = mergeByBehavior(sessions, HOUSEWORK_BEHAVIORS, currentWindow)
        val houseworkPrevious = mergeByBehavior(sessions, HOUSEWORK_BEHAVIORS, previousWindow)

        val mealCurrent = mergeByBehavior(sessions, setOf(MEAL_BEHAVIOR), currentWindow)[MEAL_BEHAVIOR].orEmpty()
        val mediaCurrentRaw = sessions.filter { it.behavior == MEDIA_BEHAVIOR && startMillis(it) in currentWindow }
        val mediaPreviousRaw = sessions.filter { it.behavior == MEDIA_BEHAVIOR && startMillis(it) in previousWindow }

        val outingStats = buildOutingStats(outingCurrent, outingPrevious, validDaysCurrent, validDaysPrevious)
        val houseworkStats = buildHouseworkStats(houseworkCurrent, houseworkPrevious, validDaysCurrent, validDaysPrevious)
        val mealStats = buildMealStats(mealCurrent, validDaysCurrent)
        val sleepStats = buildSleepStats(sleepWakeEvents, currentWindow, previousWindow, validDaysCurrent, validDaysPrevious)
        val repeatedStats = buildRepeatedActivityStats(sessions, currentWindow)
        val mediaStats = buildMediaStats(mediaCurrentRaw, mediaPreviousRaw, validDaysCurrent, validDaysPrevious)
        val dangerStats = buildDangerStats(dangerAlerts, currentWindow, previousWindow)

        val keyChanges = TrendSummaryGenerator.selectKeyChanges(
            period = period,
            validDaysCurrent = validDaysCurrent,
            validDaysPrevious = validDaysPrevious,
            outing = outingStats,
            housework = houseworkStats,
            meal = mealStats,
            sleep = sleepStats,
            media = mediaStats,
            danger = dangerStats
        )
        val summarySentences = TrendSummaryGenerator.buildSummary(validDaysCurrent, validDaysPrevious, keyChanges)

        return TrendAnalysis(
            period = period,
            validDaysCurrent = validDaysCurrent,
            validDaysPrevious = validDaysPrevious,
            outing = outingStats,
            housework = houseworkStats,
            meal = mealStats,
            sleep = sleepStats,
            repeatedActivity = repeatedStats,
            media = mediaStats,
            danger = dangerStats,
            keyChanges = keyChanges,
            summarySentences = summarySentences
        )
    }

    // ---------- 기간 윈도우 ----------

    /** [현재 기간, 직전 동일 길이 기간] — 둘 다 [start, endExclusive) 형태, Asia/Seoul 자정 기준. */
    internal fun currentAndPreviousWindows(period: TrendPeriod, nowMillis: Long): Pair<LongRange, LongRange> {
        val startOfToday = startOfDay(nowMillis)
        return when (period) {
            TrendPeriod.WEEK -> {
                val curStart = addDays(startOfToday, -6)
                val curEnd = addDays(startOfToday, 1)
                val prevStart = addDays(curStart, -7)
                (curStart until curEnd) to (prevStart until curStart)
            }
            TrendPeriod.MONTH -> {
                val curStart = addDays(startOfToday, -27)
                val curEnd = addDays(startOfToday, 1)
                val prevStart = addDays(curStart, -28)
                (curStart until curEnd) to (prevStart until curStart)
            }
            TrendPeriod.HALF_YEAR -> {
                val cal = Calendar.getInstance()
                cal.timeInMillis = startOfToday
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val curStart = Calendar.getInstance().apply { timeInMillis = cal.timeInMillis; add(Calendar.MONTH, -5) }.timeInMillis
                val curEnd = Calendar.getInstance().apply { timeInMillis = cal.timeInMillis; add(Calendar.MONTH, 1) }.timeInMillis
                val prevStart = Calendar.getInstance().apply { timeInMillis = curStart; add(Calendar.MONTH, -6) }.timeInMillis
                (curStart until curEnd) to (prevStart until curStart)
            }
        }
    }

    private operator fun LongRange.contains(value: Long?): Boolean = value != null && value >= first && value < last + 1

    // ---------- 유효 기록일 ----------

    /**
     * 임시 기준: behavior_sessions·outing_events·sleep_wake_events·danger_alerts 중
     * 하나라도 존재하는 날짜를 "유효 기록일"로 센다. sound_events 전체 재조회는 화면 성능상
     * 과도하다고 판단해 제외했다 (모니터링 heartbeat 데이터가 따로 없어서 생기는 임시 기준).
     */
    internal fun countValidDays(
        window: LongRange,
        sessions: List<BehaviorSessionResponse>,
        outings: List<OutingEventResponse>,
        sleepWakeEvents: List<SleepWakeEventResponse>,
        dangerAlerts: List<DangerAlertResponse>
    ): Int {
        val days = HashSet<Long>()
        sessions.forEach { s -> startMillis(s)?.let { if (window.contains(it)) days.add(startOfDay(it)) } }
        outings.forEach { o -> o.timestamp.fromServerDateTime()?.let { if (window.contains(it)) days.add(startOfDay(it)) } }
        sleepWakeEvents.forEach { e -> e.timestamp.fromServerDateTime()?.let { if (window.contains(it)) days.add(startOfDay(it)) } }
        dangerAlerts.forEach { a -> a.detectedAt.fromServerDateTime()?.let { if (window.contains(it)) days.add(startOfDay(it)) } }
        return days.size
    }

    private fun startMillis(s: BehaviorSessionResponse): Long? = s.startTime.fromServerDateTime()
    private fun endMillis(s: BehaviorSessionResponse): Long? = s.endTime.fromServerDateTime() ?: startMillis(s)

    // ---------- 변화율 ----------

    internal fun changeInfo(current: Double, previous: Double, validDaysCurrent: Int, validDaysPrevious: Int): ChangeInfo {
        if (validDaysCurrent < MIN_VALID_DAYS_FOR_COMPARE || validDaysPrevious < MIN_VALID_DAYS_FOR_COMPARE) {
            return ChangeInfo(ChangeStatus.INSUFFICIENT_RECORD)
        }
        if (previous == 0.0) {
            return ChangeInfo(ChangeStatus.NO_PREVIOUS_DATA)
        }
        val percent = (current - previous) / previous * 100.0
        if (!percent.isFinite()) return ChangeInfo(ChangeStatus.NO_PREVIOUS_DATA)
        return ChangeInfo(ChangeStatus.NORMAL, percent)
    }

    // ---------- 외출 ----------

    private data class OutingPair(val outAt: Long, val homeAt: Long) {
        val durationMinutes: Double get() = (homeAt - outAt) / 60000.0
    }

    private fun <T> inWindow(list: List<T>, ts: (T) -> String?): List<T> = list

    /** OUT 다음에 오는 HOME과 짝지어 "완결된 외출"만 남긴다. 중복 OUT은 먼저 나간 시각을 기준으로 하나만 남긴다. */
    private fun pairOutings(outings: List<OutingEventResponse>): List<OutingPair> {
        val sorted = outings
            .mapNotNull { e -> e.timestamp.fromServerDateTime()?.let { it to e.transitionType } }
            .sortedBy { it.first }
        val pairs = mutableListOf<OutingPair>()
        var pendingOut: Long? = null
        sorted.forEach { (t, type) ->
            when (type) {
                "HOME_TO_OUTSIDE" -> if (pendingOut == null) pendingOut = t // 이미 외출 중이면 중복 OUT은 무시(먼저 나간 시각 유지)
                "OUTSIDE_TO_HOME" -> {
                    val out = pendingOut
                    if (out != null) {
                        pairs.add(OutingPair(out, t))
                        pendingOut = null
                    } // 짝 없는 HOME은 버림
                }
            }
        }
        // 마지막에 짝 없이 남은 OUT은 버림(아직 귀가 기록이 없는 진행중 외출)
        return pairs
    }

    private fun buildOutingStats(current: List<OutingPair>, previous: List<OutingPair>, validDaysCurrent: Int, validDaysPrevious: Int): OutingStats {
        val dailyAverage = if (validDaysCurrent > 0) current.size / validDaysCurrent.toDouble() else null
        val daysWithOuting = current.map { startOfDay(it.outAt) }.distinct().size
        val ratio = if (validDaysCurrent > 0) daysWithOuting / validDaysCurrent.toDouble() * 100.0 else null
        val avgDuration = if (current.isNotEmpty()) current.sumOf { it.durationMinutes } / current.size else null

        val prevDailyAverage = if (validDaysPrevious > 0) previous.size / validDaysPrevious.toDouble() else 0.0
        val change = changeInfo(dailyAverage ?: 0.0, prevDailyAverage, validDaysCurrent, validDaysPrevious)

        return OutingStats(
            pairedTripCount = current.size,
            dailyAverage = dailyAverage,
            daysWithOuting = daysWithOuting,
            outingDayRatioPercent = ratio,
            averageDurationMinutes = avgDuration,
            change = change
        )
    }

    // ---------- 세션 병합(가사활동 · 식사) ----------

    /** 같은 behavior가 이전 세션 종료 후 [MERGE_GAP_MINUTES]분 이내(또는 겹침)에 다시 시작되면 하나로 합친다. */
    internal fun mergeSessions(sessions: List<BehaviorSessionResponse>): List<Pair<Long, Long>> {
        val sorted = sessions
            .mapNotNull { s -> val start = startMillis(s) ?: return@mapNotNull null; val end = endMillis(s) ?: start; Pair(start, end) }
            .sortedBy { it.first }
        val merged = mutableListOf<Pair<Long, Long>>()
        sorted.forEach { (start, end) ->
            val last = merged.lastOrNull()
            if (last != null && start <= last.second + MERGE_GAP_MINUTES * 60_000L) {
                merged[merged.size - 1] = last.first to maxOf(last.second, end)
            } else {
                merged.add(start to end)
            }
        }
        return merged
    }

    private fun mergeByBehavior(sessions: List<BehaviorSessionResponse>, behaviors: Set<String>, window: LongRange): Map<String, List<Pair<Long, Long>>> {
        return behaviors.associateWith { behavior ->
            val filtered = sessions.filter { it.behavior == behavior && (startMillis(it) in window) }
            mergeSessions(filtered)
        }
    }

    private fun buildHouseworkStats(
        current: Map<String, List<Pair<Long, Long>>>,
        previous: Map<String, List<Pair<Long, Long>>>,
        validDaysCurrent: Int,
        validDaysPrevious: Int
    ): HouseworkStats {
        val laundry = current["laundry"].orEmpty()
        val dishwashing = current["dishwashing"].orEmpty()
        val cleaning = current["cleaning"].orEmpty()
        val all = laundry + dishwashing + cleaning
        val totalCount = all.size
        val dailyAverage = if (validDaysCurrent > 0) totalCount / validDaysCurrent.toDouble() else null
        val daysWithActivity = all.map { startOfDay(it.first) }.distinct().size

        val prevTotal = (previous["laundry"].orEmpty() + previous["dishwashing"].orEmpty() + previous["cleaning"].orEmpty()).size
        val prevDailyAverage = if (validDaysPrevious > 0) prevTotal / validDaysPrevious.toDouble() else 0.0
        val change = changeInfo(dailyAverage ?: 0.0, prevDailyAverage, validDaysCurrent, validDaysPrevious)

        return HouseworkStats(
            totalCount = totalCount,
            laundryCount = laundry.size,
            dishwashingCount = dishwashing.size,
            cleaningCount = cleaning.size,
            dailyAverage = dailyAverage,
            daysWithActivity = daysWithActivity,
            change = change
        )
    }

    // ---------- 식사 규칙성 ----------

    /** 00:00~05:59 시각은 24시간(1440분)을 더해 "전날 밤의 연장"처럼 다뤄 자정 경계에서 평균이 깨지지 않게 한다. */
    private fun correctOvernightMinute(minuteOfDay: Int): Int = if (minuteOfDay < 360) minuteOfDay + 1440 else minuteOfDay

    private fun minuteOfDay(epochMillis: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = epochMillis
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    private fun stdDev(values: List<Double>): Double? {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
        return sqrt(variance)
    }

    private fun buildMealStats(mealSessions: List<Pair<Long, Long>>, validDaysCurrent: Int): MealStats {
        val totalCount = mealSessions.size
        val dailyAverage = if (validDaysCurrent > 0) totalCount / validDaysCurrent.toDouble() else null

        val mealDates = mealSessions.map { startOfDay(it.first) }.toSet()
        val daysWithoutMealRecord = (validDaysCurrent - mealDates.size).coerceAtLeast(0)

        val firstMealPerDay = mealSessions
            .groupBy { startOfDay(it.first) }
            .mapValues { (_, list) -> list.minOf { it.first } }
            .values

        val regularity: RegularityLevel
        val stdDevMinutes: Double?
        if (firstMealPerDay.size < MIN_VALID_DAYS_FOR_REGULARITY) {
            regularity = RegularityLevel.INSUFFICIENT
            stdDevMinutes = null
        } else {
            val correctedMinutes = firstMealPerDay.map { correctOvernightMinute(minuteOfDay(it)).toDouble() }
            val sd = stdDev(correctedMinutes) ?: 0.0
            stdDevMinutes = sd
            regularity = regularityFromStdDev(sd)
        }

        return MealStats(
            totalCount = totalCount,
            dailyAverage = dailyAverage,
            daysWithoutMealRecord = daysWithoutMealRecord,
            startTimeStdDevMinutes = stdDevMinutes,
            regularityLevel = regularity
        )
    }

    private fun regularityFromStdDev(sd: Double): RegularityLevel = when {
        sd <= 60.0 -> RegularityLevel.STABLE
        sd <= 120.0 -> RegularityLevel.SOMEWHAT_VARIABLE
        else -> RegularityLevel.HIGHLY_VARIABLE
    }

    // ---------- 수면 규칙성 ----------

    private data class SleepPair(val sleepAt: Long, val wakeAt: Long) {
        val durationMinutes: Double get() = (wakeAt - sleepAt) / 60000.0
    }

    private fun pairSleepWake(events: List<SleepWakeEventResponse>, window: LongRange): List<SleepPair> {
        val sorted = events
            .mapNotNull { e -> e.timestamp.fromServerDateTime()?.let { it to e.eventType } }
            .sortedBy { it.first }
        val pairs = mutableListOf<SleepPair>()
        var pendingSleep: Long? = null
        sorted.forEach { (t, type) ->
            when (type) {
                "SLEEP" -> pendingSleep = t // 연속 SLEEP이면 최신 것으로 갱신(비정상 연속 이벤트는 앞의 것을 버림)
                "WAKE" -> {
                    val sleep = pendingSleep
                    if (sleep != null) {
                        val durationMin = (t - sleep) / 60000.0
                        if (durationMin >= MIN_SLEEP_MINUTES && durationMin <= MAX_SLEEP_MINUTES && sleep in window) {
                            pairs.add(SleepPair(sleep, t))
                        }
                        pendingSleep = null
                    }
                }
            }
        }
        return pairs
    }

    private fun buildSleepStats(
        events: List<SleepWakeEventResponse>,
        currentWindow: LongRange,
        previousWindow: LongRange,
        validDaysCurrent: Int,
        validDaysPrevious: Int
    ): SleepStats {
        // 수면은 취침일 기준 창을 살짝 넉넉히 봐야 자정 넘어 기상하는 경우를 놓치지 않는다.
        val extendedCurrent = currentWindow.first until (currentWindow.last + 1 + 24 * 60 * 60 * 1000L)
        val extendedPrevious = previousWindow.first until (previousWindow.last + 1 + 24 * 60 * 60 * 1000L)
        val current = pairSleepWake(events, currentWindow).filter { it.sleepAt in currentWindow || it.wakeAt in extendedCurrent }
        val previous = pairSleepWake(events, previousWindow).filter { it.sleepAt in previousWindow || it.wakeAt in extendedPrevious }

        val regularity: RegularityLevel
        var startStdDev: Double? = null
        var wakeStdDev: Double? = null
        var avgStart: Double? = null
        var avgWake: Double? = null
        var avgDuration: Double? = null

        // 평균(취침/기상/수면시간)은 데이터가 하나라도 있으면 보여준다. 표준편차·규칙성 판정만
        // "최소 4개 정상 쌍" 기준으로 따로 게이팅한다 — 표본 1~2개로 "변동이 크다/적다"를 판정하는 건 의미가 없어서다.
        if (current.isNotEmpty()) {
            val startMinutes = current.map { correctOvernightMinute(minuteOfDay(it.sleepAt)).toDouble() }
            val wakeMinutes = current.map { minuteOfDay(it.wakeAt).toDouble() }
            avgStart = startMinutes.average() % 1440.0
            avgWake = wakeMinutes.average()
            avgDuration = current.sumOf { it.durationMinutes } / current.size
        }
        if (current.size >= MIN_VALID_DAYS_FOR_REGULARITY) {
            val startMinutes = current.map { correctOvernightMinute(minuteOfDay(it.sleepAt)).toDouble() }
            val wakeMinutes = current.map { minuteOfDay(it.wakeAt).toDouble() }
            startStdDev = stdDev(startMinutes) ?: 0.0
            wakeStdDev = stdDev(wakeMinutes) ?: 0.0
            regularity = regularityFromStdDev(startStdDev)
        } else {
            regularity = RegularityLevel.INSUFFICIENT
        }

        val prevAvgDuration = if (previous.isNotEmpty()) previous.sumOf { it.durationMinutes } / previous.size else 0.0
        val durationChange = changeInfo(avgDuration ?: 0.0, prevAvgDuration, validDaysCurrent, validDaysPrevious)

        return SleepStats(
            averageSleepStartMinutes = avgStart,
            averageWakeMinutes = avgWake,
            averageDurationMinutes = avgDuration,
            sleepStartStdDevMinutes = startStdDev,
            wakeStdDevMinutes = wakeStdDev,
            regularityLevel = regularity,
            durationChange = durationChange,
            validPairCount = current.size
        )
    }

    // ---------- 짧은 간격의 동일 활동 ----------

    /**
     * meal · laundry · dishwashing · cleaning 각각을 10분 병합한 뒤, 병합된 세션 사이 간격이
     * 60분 이내면 "짧은 간격의 동일 활동" 1회로 센다. 인지 저하와 연결하지 않는다 — 센서 감지 기준일 뿐.
     */
    private fun buildRepeatedActivityStats(sessions: List<BehaviorSessionResponse>, window: LongRange): RepeatedActivityStats {
        var count = 0
        MERGEABLE_BEHAVIORS.forEach { behavior ->
            val filtered = sessions.filter { it.behavior == behavior && startMillis(it) in window }
            val merged = mergeSessions(filtered)
            for (i in 1 until merged.size) {
                val gapMinutes = (merged[i].first - merged[i - 1].second) / 60000.0
                if (gapMinutes in 0.0..REPEATED_GAP_MINUTES.toDouble()) count++
            }
        }
        return RepeatedActivityStats(count)
    }

    // ---------- 대화 · 미디어 활동 ----------

    private fun buildMediaStats(
        current: List<BehaviorSessionResponse>,
        previous: List<BehaviorSessionResponse>,
        validDaysCurrent: Int,
        validDaysPrevious: Int
    ): MediaActivityStats {
        fun durationOf(s: BehaviorSessionResponse): Double {
            val start = startMillis(s) ?: return 0.0
            val end = endMillis(s) ?: start
            return (end - start) / 60000.0
        }
        val totalDuration = current.sumOf { durationOf(it) }
        val dailyAverage = if (validDaysCurrent > 0) totalDuration / validDaysCurrent else null
        val daysWithActivity = current.mapNotNull { startMillis(it) }.map { startOfDay(it) }.distinct().size

        val prevTotalDuration = previous.sumOf { durationOf(it) }
        val prevDailyAverage = if (validDaysPrevious > 0) prevTotalDuration / validDaysPrevious else 0.0
        val change = changeInfo(dailyAverage ?: 0.0, prevDailyAverage, validDaysCurrent, validDaysPrevious)

        return MediaActivityStats(
            sessionCount = current.size,
            totalDurationMinutes = totalDuration,
            dailyAverageMinutes = dailyAverage,
            daysWithActivity = daysWithActivity,
            change = change
        )
    }

    // ---------- 위험 알림 ----------

    private fun buildDangerStats(alerts: List<DangerAlertResponse>, currentWindow: LongRange, previousWindow: LongRange): DangerStats {
        val current = alerts.filter { it.detectedAt.fromServerDateTime() in currentWindow }
        val previous = alerts.filter { it.detectedAt.fromServerDateTime() in previousWindow }
        val dangerCount = current.count { it.level == "DANGER" }
        val candidateCount = current.count { it.level == "CANDIDATE" }
        val byLabel = current.filter { it.soundLabel != null }.groupingBy { it.soundLabel!! }.eachCount()
        return DangerStats(
            totalCount = current.size,
            dangerCount = dangerCount,
            candidateCount = candidateCount,
            countByLabel = byLabel,
            absoluteChange = current.size - previous.size
        )
    }

    // ---------- 공용 시간 유틸 ----------

    private fun buildBuckets(period: TrendPeriod, nowMillis: Long): List<Pair<Long, Long>> {
        val startOfToday = startOfDay(nowMillis)
        return when (period) {
            TrendPeriod.WEEK -> (6 downTo 0).map { daysAgo ->
                val start = addDays(startOfToday, -daysAgo)
                start to addDays(start, 1)
            }
            TrendPeriod.MONTH -> {
                val recentWindowStart = addDays(startOfToday, -6)
                (3 downTo 0).map { weeksAgo ->
                    val start = addDays(recentWindowStart, -7 * weeksAgo)
                    start to addDays(start, 7)
                }
            }
            TrendPeriod.HALF_YEAR -> {
                val cal = Calendar.getInstance()
                cal.timeInMillis = startOfToday
                cal.set(Calendar.DAY_OF_MONTH, 1)
                (5 downTo 0).map { monthsAgo ->
                    val start = Calendar.getInstance().apply { timeInMillis = cal.timeInMillis; add(Calendar.MONTH, -monthsAgo) }.timeInMillis
                    val end = Calendar.getInstance().apply { timeInMillis = start; add(Calendar.MONTH, 1) }.timeInMillis
                    start to end
                }
            }
        }
    }

    private fun labelsFor(period: TrendPeriod, nowMillis: Long): List<String> {
        val startOfToday = startOfDay(nowMillis)
        return when (period) {
            TrendPeriod.WEEK -> {
                val fmt = SimpleDateFormat("M/d", Locale.KOREA)
                (6 downTo 0).map { daysAgo -> fmt.format(addDays(startOfToday, -daysAgo)) }
            }
            TrendPeriod.MONTH -> listOf("4주 전", "3주 전", "2주 전", "최근 1주")
            TrendPeriod.HALF_YEAR -> {
                val cal = Calendar.getInstance()
                cal.timeInMillis = startOfToday
                (5 downTo 0).map { monthsAgo ->
                    val c = Calendar.getInstance().apply { timeInMillis = cal.timeInMillis; add(Calendar.MONTH, -monthsAgo) }
                    "${c.get(Calendar.MONTH) + 1}월"
                }
            }
        }
    }

    private fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun addDays(millis: Long, days: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.add(Calendar.DAY_OF_MONTH, days)
        return cal.timeInMillis
    }
}
