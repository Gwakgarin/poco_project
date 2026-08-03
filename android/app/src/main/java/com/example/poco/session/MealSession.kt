package com.example.poco.session

private const val T1_MINUTES = 10L  // 조리(microwave) 재감지 허용 gap
private const val T2_MINUTES = 20L  // CONFIRMED 이후 무감지 timeout (설거지 트리거보다 먼저 도달하면 이걸로 종료)

private val COOKING_LABELS = setOf("cooking", "microwave")

enum class MealState { IDLE, PREPARING, CONFIRMED, ENDED }

/**
 * 식사 세션: cooking/microwave 감지로 시작 -> dishes 감지로 확정 -> 아래 둘 중 먼저 도달하는 조건으로 종료.
 *   (a) 자체 T2분 무감지 timeout
 *   (b) 설거지 세션이 "microwave 선행 확인됨"으로 판단해서 걸어주는 트리거 ([triggerEndByDishwashing])
 */
class MealSession(
    private val t1Millis: Long = T1_MINUTES * 60_000L,
    private val t2Millis: Long = T2_MINUTES * 60_000L
) {
    var state: MealState = MealState.IDLE
        private set
    private var startTime: Long? = null
    private var confirmedTime: Long? = null
    private var lastEventTime: Long? = null

    var onClose: ((BehaviorSessionRecord) -> Unit)? = null

    fun processEvent(label: String, timestamp: Long) {
        if (state == MealState.PREPARING || state == MealState.CONFIRMED) {
            checkTimeout(timestamp)
        }

        when (state) {
            MealState.IDLE -> if (label in COOKING_LABELS) start(timestamp)
            MealState.PREPARING -> {
                if (label in COOKING_LABELS) {
                    lastEventTime = timestamp
                } else if (label == "dishes") {
                    confirm(timestamp)
                }
            }
            MealState.CONFIRMED -> {
                if (label in COOKING_LABELS || label == "dishes") {
                    lastEventTime = timestamp
                }
            }
            MealState.ENDED -> if (label in COOKING_LABELS) start(timestamp)
        }
    }

    /** 설거지 세션(Step 4)에서 microwave 선행이 확인되면 식사 세션을 여기서 바로 종료시킨다. */
    fun triggerEndByDishwashing(timestamp: Long) {
        if (state == MealState.CONFIRMED) {
            close(timestamp, "dishwashing_trigger")
        }
    }

    private fun checkTimeout(currentTime: Long) {
        val last = lastEventTime ?: return
        when (state) {
            MealState.PREPARING -> if (currentTime - last > t1Millis) close(currentTime, "no_activity")
            MealState.CONFIRMED -> if (currentTime - last > t2Millis) close(currentTime, "timeout")
            else -> {}
        }
    }

    private fun start(timestamp: Long) {
        state = MealState.PREPARING
        startTime = timestamp
        lastEventTime = timestamp
        confirmedTime = null
    }

    private fun confirm(timestamp: Long) {
        state = MealState.CONFIRMED
        confirmedTime = timestamp
        lastEventTime = timestamp
    }

    private fun close(timestamp: Long, reason: String) {
        val wasConfirmed = state == MealState.CONFIRMED
        state = MealState.ENDED

        val start = startTime
        val confirmedAt = confirmedTime
        if (wasConfirmed && start != null && confirmedAt != null) {
            onClose?.invoke(BehaviorSessionRecord("meal", start, confirmedAt, timestamp, reason))
        }
    }
}
