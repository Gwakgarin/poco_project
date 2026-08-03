package com.example.poco.session

private const val T1_MINUTES = 2L  // 재감지 gap / 종료 timeout
private const val T2_MINUTES = 2L  // 최소 지속시간

enum class CleaningState { IDLE, ACTIVE, CONFIRMED, ENDED }

/**
 * 청소 세션: vacuum 소리를 기준으로 IDLE -> ACTIVE -> CONFIRMED -> ENDED 상태를 관리한다.
 * 독립 세션, 다른 세션과 연동 없음.
 */
class CleaningSession(
    private val t1Millis: Long = T1_MINUTES * 60_000L,
    private val t2Millis: Long = T2_MINUTES * 60_000L
) {
    var state: CleaningState = CleaningState.IDLE
        private set
    private var startTime: Long? = null
    private var confirmedTime: Long? = null
    private var lastEventTime: Long? = null

    var onClose: ((BehaviorSessionRecord) -> Unit)? = null

    fun processEvent(label: String, timestamp: Long) {
        if (state == CleaningState.ACTIVE || state == CleaningState.CONFIRMED) {
            checkTimeout(timestamp)
        }
        if (label != "vacuum") return

        when (state) {
            CleaningState.IDLE, CleaningState.ENDED -> start(timestamp)
            CleaningState.ACTIVE -> {
                lastEventTime = timestamp
                val start = startTime ?: return
                if (timestamp - start >= t2Millis) {
                    state = CleaningState.CONFIRMED
                    confirmedTime = timestamp
                }
            }
            CleaningState.CONFIRMED -> lastEventTime = timestamp
        }
    }

    fun checkTimeout(currentTime: Long) {
        if (state != CleaningState.ACTIVE && state != CleaningState.CONFIRMED) return
        val last = lastEventTime ?: return
        if (currentTime - last <= t1Millis) return

        val confirmed = state == CleaningState.CONFIRMED
        val start = startTime
        val confirmedAt = confirmedTime
        val endTime = last + t1Millis
        state = CleaningState.ENDED

        if (confirmed && start != null && confirmedAt != null) {
            onClose?.invoke(BehaviorSessionRecord("cleaning", start, confirmedAt, endTime))
        }
    }

    private fun start(timestamp: Long) {
        state = CleaningState.ACTIVE
        startTime = timestamp
        lastEventTime = timestamp
        confirmedTime = null
    }
}
