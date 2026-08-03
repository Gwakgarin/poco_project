package com.example.poco.session

private const val T1_MINUTES = 5L   // 재감지 gap / 종료 timeout
private const val T2_MINUTES = 20L  // 최소 지속시간

enum class LaundryState { IDLE, ACTIVE, CONFIRMED, ENDED }

/**
 * 세탁 세션: washing_machine 소리를 기준으로 IDLE -> ACTIVE -> CONFIRMED -> ENDED 상태를 관리한다.
 * 독립 세션, 다른 세션과 연동 없음.
 */
class LaundrySession(
    private val t1Millis: Long = T1_MINUTES * 60_000L,
    private val t2Millis: Long = T2_MINUTES * 60_000L
) {
    var state: LaundryState = LaundryState.IDLE
        private set
    private var startTime: Long? = null
    private var confirmedTime: Long? = null
    private var lastEventTime: Long? = null

    /** 확정된 세션이 종료됐을 때 호출됨. 여기서 서버 저장을 트리거한다. */
    var onClose: ((BehaviorSessionRecord) -> Unit)? = null

    fun processEvent(label: String, timestamp: Long) {
        if (state == LaundryState.ACTIVE || state == LaundryState.CONFIRMED) {
            checkTimeout(timestamp)
        }
        if (label != "washing_machine") return

        when (state) {
            LaundryState.IDLE, LaundryState.ENDED -> start(timestamp)
            LaundryState.ACTIVE -> {
                lastEventTime = timestamp
                val start = startTime ?: return
                if (timestamp - start >= t2Millis) {
                    state = LaundryState.CONFIRMED
                    confirmedTime = timestamp
                }
            }
            LaundryState.CONFIRMED -> lastEventTime = timestamp
        }
    }

    /** 새 이벤트가 없어도 외부 루프에서 주기적으로 타임아웃 여부를 검사할 수 있게 한다. */
    fun checkTimeout(currentTime: Long) {
        if (state != LaundryState.ACTIVE && state != LaundryState.CONFIRMED) return
        val last = lastEventTime ?: return
        if (currentTime - last <= t1Millis) return

        val confirmed = state == LaundryState.CONFIRMED
        val start = startTime
        val confirmedAt = confirmedTime
        val endTime = last + t1Millis
        state = LaundryState.ENDED

        if (confirmed && start != null && confirmedAt != null) {
            onClose?.invoke(BehaviorSessionRecord("laundry", start, confirmedAt, endTime))
        }
    }

    private fun start(timestamp: Long) {
        state = LaundryState.ACTIVE
        startTime = timestamp
        lastEventTime = timestamp
        confirmedTime = null
    }
}
