package com.example.poco.session

private const val T2_MINUTES = 2L   // water -> dishes 최대 허용 gap
private const val T3_MINUTES = 5L   // 재감지 허용 gap / 세션 종료 timeout
private const val L_MINUTES = 60L   // microwave 선행 확인 lookback

enum class DishwashingState { IDLE, PENDING, CONFIRMED, ENDED }

/**
 * 설거지 세션: water 감지로 시작(PENDING) -> T2분 이내 dishes 감지로 확정(CONFIRMED).
 * 확정 종료 시, 시작 시각 기준 최근 L분 이내 microwave 감지가 있었으면 "식사"로 흡수되어
 * (mealSession.triggerEndByDishwashing 호출) 독립적으로 기록하지 않고, 없으면 독립 가사노동(설거지)으로 기록한다.
 */
class DishwashingSession(
    private val mealSession: MealSession? = null,
    private val t2Millis: Long = T2_MINUTES * 60_000L,
    private val t3Millis: Long = T3_MINUTES * 60_000L,
    private val lMillis: Long = L_MINUTES * 60_000L
) {
    var state: DishwashingState = DishwashingState.IDLE
        private set
    private var startTime: Long? = null
    private var confirmedTime: Long? = null
    private var lastEventTime: Long? = null
    private var branch: String? = null // "meal_trigger" | "independent"
    private var lastMicrowaveTime: Long? = null

    var onClose: ((BehaviorSessionRecord) -> Unit)? = null

    fun processEvent(label: String, timestamp: Long) {
        // microwave는 washing 상태와 무관하게 lookback 판단용으로만 기록
        if (label == "microwave") {
            lastMicrowaveTime = timestamp
        }

        if (state == DishwashingState.PENDING || state == DishwashingState.CONFIRMED) {
            checkTimeout(timestamp)
        }

        when (state) {
            DishwashingState.IDLE -> if (label == "water") start(timestamp)
            DishwashingState.PENDING -> {
                if (label == "water") lastEventTime = timestamp
                else if (label == "dishes") confirm(timestamp)
            }
            DishwashingState.CONFIRMED -> if (label == "water" || label == "dishes") lastEventTime = timestamp
            DishwashingState.ENDED -> if (label == "water") start(timestamp)
        }
    }

    private fun start(timestamp: Long) {
        state = DishwashingState.PENDING
        startTime = timestamp
        lastEventTime = timestamp
        confirmedTime = null
        branch = null
    }

    private fun confirm(timestamp: Long) {
        state = DishwashingState.CONFIRMED
        confirmedTime = timestamp
        lastEventTime = timestamp
    }

    private fun checkTimeout(currentTime: Long) {
        val last = lastEventTime ?: return
        when (state) {
            DishwashingState.PENDING -> if (currentTime - last > t2Millis) {
                // dishes 없이 폐기 (단순 손 씻기 등) -> 기록 없이 IDLE로
                state = DishwashingState.IDLE
                startTime = null
            }
            DishwashingState.CONFIRMED -> if (currentTime - last > t3Millis) close(currentTime)
            else -> {}
        }
    }

    // Step 4: 후처리 분기
    private fun close(timestamp: Long) {
        state = DishwashingState.ENDED

        val start = startTime
        val microwaveTime = lastMicrowaveTime
        val hasRecentMicrowave = microwaveTime != null && start != null &&
            (start - microwaveTime) <= lMillis && microwaveTime <= start

        if (hasRecentMicrowave) {
            branch = "meal_trigger"
            mealSession?.triggerEndByDishwashing(timestamp)
        } else {
            branch = "independent"
            val confirmedAt = confirmedTime
            if (start != null && confirmedAt != null) {
                onClose?.invoke(BehaviorSessionRecord("dishwashing", start, confirmedAt, timestamp))
            }
        }
    }
}
