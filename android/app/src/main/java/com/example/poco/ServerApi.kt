package com.example.poco

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class SoundEventRequest(
    val rawFile: String,
    val splitFile: String,
    val predLabel: String,
    val predScore: Double,
    val segIndex: Int,
    val startSec: Int,
    val endSec: Int,
    val smoothedLabel: String
)

/** 환자 기기가 서버에 최신 위치를 저장할 때 보내는 요청 데이터. */
data class LatestLocationRequest(
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val homeState: String,
    val measuredAtEpochMs: Long
)

/** 보호자 화면이 서버에서 최신 환자 위치를 조회했을 때 받는 응답 데이터. */
data class LatestLocationResponse(
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val homeState: String,
    val measuredAtEpochMs: Long
)

/** 소리와 위치 정책이 위험 조건을 만족했을 때 서버로 보내는 알림 데이터. */
data class DangerAlertRequest(
    val deviceId: String,
    val soundLabel: String,
    val level: String,
    val reason: String,
    val homeState: String,
    val latitude: Double?,
    val longitude: Double?,
    val detectedAtEpochMs: Long
)

/** 서버에 저장된 danger-alert 를 조회할 때 받는 응답 데이터. */
data class DangerAlertResponse(
    val id: Long? = null,
    val deviceId: String? = null,
    val soundLabel: String? = null,
    val level: String? = null,
    val reason: String? = null,
    val homeState: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val detectedAtEpochMs: Long? = null
)

/** 서버에 저장된 행동(behavior) 세션을 조회할 때 받는 응답 데이터. */
data class BehaviorSessionResponse(
    val id: Long? = null,
    // 예전 필드 (안 쓰지만 서버 응답에 남아있을 수 있어서 유지)
    val representativeEvent: String? = null,
    val ruleResult: String? = null,
    val startSec: Int? = null,
    val endSec: Int? = null,
    // 세션 상태머신이 실제로 채우는 필드
    val deviceId: String? = null,
    val behavior: String? = null,
    val startTime: Long? = null,
    val confirmedTime: Long? = null,
    val endTime: Long? = null,
    val endReason: String? = null
)

/** 세션 상태머신(세탁/청소/설거지/식사/인지)이 확정 종료됐을 때 서버로 보내는 기록 데이터. */
data class BehaviorSessionRequest(
    val deviceId: String,
    val behavior: String,
    val startTime: Long,
    val confirmedTime: Long,
    val endTime: Long,
    val endReason: String? = null
)

/** 취침/기상 상태머신이 SLEEP 또는 WAKE를 확정했을 때만 서버로 보내는 이벤트. */
data class SleepWakeEventRequest(
    val deviceId: String,
    val eventType: String, // "sleep" | "wake"
    val timestamp: Long
)

/** 서버에 저장된 취침/기상 확정 이벤트를 조회할 때 받는 응답 데이터. */
data class SleepWakeEventResponse(
    val id: Long? = null,
    val deviceId: String? = null,
    val eventType: String? = null, // "sleep" | "wake"
    val timestamp: Long? = null
)

interface SoundEventApi {
    @POST("/api/sound-events")
    fun createSoundEvent(@Body request: SoundEventRequest): Call<Void>

    @GET("/api/sound-events")
    suspend fun getSoundEvents(): List<SoundEventResponse>

    /** 환자별 최신 위치를 저장하거나 갱신한다. */
    @POST("/api/latest-locations")
    fun updateLatestLocation(@Body request: LatestLocationRequest): Call<Void>

    /** deviceId에 해당하는 환자의 가장 최근 위치를 조회한다. */
    @GET("/api/latest-locations")
    suspend fun getLatestLocation(@Query("deviceId") deviceId: String): LatestLocationResponse

    /** 위험 정책이 생성한 위험/위험 후보 알림을 저장한다. */
    @POST("/api/danger-alerts")
    fun createDangerAlert(@Body request: DangerAlertRequest): Call<Void>

    /** deviceId 에 해당하는 위험 알림 목록을 최신순으로 조회한다 (알림 센터 화면용). */
    @GET("/api/danger-alerts")
    suspend fun getDangerAlerts(@Query("deviceId") deviceId: String): List<DangerAlertResponse>

    /** 저장된 행동 세션 전체를 조회한다 (활동 로그 / 타임라인 화면용). */
    @GET("/api/behavior-sessions")
    suspend fun getBehaviorSessions(): List<BehaviorSessionResponse>

    /** 세션 상태머신이 확정 종료한 행동 세션(식사/청소/세탁/설거지/인지)을 저장한다. */
    @POST("/api/behavior-sessions")
    fun createBehaviorSession(@Body request: BehaviorSessionRequest): Call<Void>

    /** 취침/기상 상태머신이 SLEEP 또는 WAKE를 확정했을 때 저장한다. SoundEvent/BehaviorSession과는 별도 엔드포인트. */
    @POST("/api/sleep-wake-events")
    fun createSleepWakeEvent(@Body request: SleepWakeEventRequest): Call<Void>

    /** deviceId 에 해당하는 취침/기상 확정 이벤트 목록을 조회한다 (보호자 화면 수면 시간 표시용). */
    @GET("/api/sleep-wake-events")
    suspend fun getSleepWakeEvents(@Query("deviceId") deviceId: String): List<SleepWakeEventResponse>
}

object ServerApiClient {
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://127.0.0.1:8080/") // USB + adb reverse tcp:8080 tcp:8080 방식으로 씀. Wi-Fi로 바꿀 땐 PC IP로 교체 (ipconfig)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: SoundEventApi = retrofit.create(SoundEventApi::class.java)
}
