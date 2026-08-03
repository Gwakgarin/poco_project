package com.example.poco.session

/** 세션이 확정된 채로 종료됐을 때 서버에 저장할 최종 결과. */
data class BehaviorSessionRecord(
    val behavior: String,
    val startTime: Long,
    val confirmedTime: Long,
    val endTime: Long,
    val endReason: String? = null
)
