package com.example.poco.session

private const val T1_MINUTES = 5L  // 재감지 gap / 종료 timeout
private const val M_MINUTES = 5L   // 최소 지속시간(확정 기준)

enum class CognitiveState { IDLE, ACTIVE, CONFIRMED, ENDED }

/**
 * 인지 세션: YAMNet 네이티브 출력(Speech/Conversation/Television 중 max, THRESHOLD=0.5)이
 * 기준치 이상이면 "인지 활동 감지"로 보고 ACTIVE -> CONFIRMED(M분 이상 지속) -> ENDED(T1분 무감지) 로 관리한다.
 * 커스텀 재학습 없이 기존 YAMNet 호출 결과를 그대로 쓰는 방식(스펙 6번). 독립 세션, 연동 없음.
 */
class CognitiveSession(
    private val t1Millis: Long = T1_MINUTES * 60_000L,
    private val mMillis: Long = M_MINUTES * 60_000L
) {
    var state: CognitiveState = CognitiveState.IDLE
        private set
    private var startTime: Long? = null
    private var confirmedTime: Long? = null
    private var lastEventTime: Long? = null

    var onClose: ((BehaviorSessionRecord) -> Unit)? = null

    /** isCognitive = cognitiveScore >= THRESHOLD 여부는 호출부(AudioMonitorService)에서 판단해서 넘겨준다. */
    fun processEvent(isCognitive: Boolean, timestamp: Long) {
        if (state == CognitiveState.ACTIVE || state == CognitiveState.CONFIRMED) {
            checkTimeout(timestamp)
        }
        if (!isCognitive) return

        when (state) {
            CognitiveState.IDLE, CognitiveState.ENDED -> start(timestamp)
            CognitiveState.ACTIVE -> {
                lastEventTime = timestamp
                val start = startTime ?: return
                if (timestamp - start >= mMillis) {
                    state = CognitiveState.CONFIRMED
                    confirmedTime = timestamp
                }
            }
            CognitiveState.CONFIRMED -> lastEventTime = timestamp
        }
    }

    fun checkTimeout(currentTime: Long) {
        if (state != CognitiveState.ACTIVE && state != CognitiveState.CONFIRMED) return
        val last = lastEventTime ?: return
        if (currentTime - last <= t1Millis) return

        val confirmed = state == CognitiveState.CONFIRMED
        val start = startTime
        val confirmedAt = confirmedTime
        val endTime = last + t1Millis
        state = CognitiveState.ENDED

        if (confirmed && start != null && confirmedAt != null) {
            onClose?.invoke(BehaviorSessionRecord("cognitive", start, confirmedAt, endTime))
        }
    }

    private fun start(timestamp: Long) {
        state = CognitiveState.ACTIVE
        startTime = timestamp
        lastEventTime = timestamp
        confirmedTime = null
    }
}
