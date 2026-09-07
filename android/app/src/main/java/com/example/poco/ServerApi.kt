package com.example.poco

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// =========================================================
// 시간 값 관련 참고
// 서버(ERD 반영 후)는 시간을 더 이상 epoch ms(Long) 로 주고받지 않고,
// "2026-08-31T14:23:01" 같은 ISO-8601 문자열(datetime)로 주고받습니다.
// 그래서 아래 모든 시간 필드는 Long -> String 으로 바뀌었습니다.
// 화면에서 Long(밀리초)이 필요하면 이 문자열을 파싱해서 쓰면 됩니다.
// =========================================================

// ---------------------------------------------------------
// 1. 회원가입 / 로그인  (신규 - 기존엔 서버 연결 자체가 없었음)
// ---------------------------------------------------------

/** 회원가입 요청. role: 0=일반 사용자(피보호자), 1=보호자 */
data class SignUpRequest(
    val name: String,
    val email: String,
    val password: String,
    val phoneNumber: String,
    val role: Int
)

data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
    val phoneNumber: String,
    val role: Int,
    val joinedAt: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val success: Boolean,
    val user: UserResponse? = null,
    val message: String? = null
)

// ---------------------------------------------------------
// 2. 기기 등록/조회  (신규)
// ---------------------------------------------------------

data class DeviceRequest(
    val userId: Long,
    val deviceUuid: String,
    val micSensitivity: Float,
    val micOn: Boolean,
    val gpsOn: Boolean? = null
)

data class DeviceResponse(
    val id: Long,
    val deviceUuid: String,
    val userId: Long,
    val micSensitivity: Float,
    val micOn: Boolean,
    val gpsOn: Boolean? = null,
    val batteryPercent: Int? = null,
    val lastSeenAt: String? = null
)

// ---------------------------------------------------------
// 3. 보호자 연동  (신규)
// ---------------------------------------------------------

data class LinkCodeResponse(
    val code: String,
    val userId: Long,
    val expiresAt: String,
    val usedAt: String? = null,
    val createdAt: String? = null
)

/** relationLabel 값은 반드시 FAMILY | CAREGIVER | FRIEND | OTHER 중 하나 (대소문자 무관) */
data class RedeemLinkRequest(
    val code: String,
    val guardianId: Long,
    val relationLabel: String
)

data class RedeemLinkResult(
    val success: Boolean,
    val link: UserLinkResponse? = null,
    val message: String? = null
)

data class UserLinkResponse(
    val id: Long,
    val userId: Long,
    val guardianId: Long,
    val relationLabel: String? = null,
    val linkedAt: String? = null
)

// ---------------------------------------------------------
// 4. 알림 설정  (신규)
// ---------------------------------------------------------

data class NotificationSettingsResponse(
    val userId: Long,
    val emergencyAlert: Boolean? = null,
    val activityAnomalyAlert: Boolean? = null,
    val lowBatteryAlert: Boolean? = null,
    val dailySummaryAlert: Boolean? = null
)

data class NotificationSettingsRequest(
    val userId: Long,
    val emergencyAlert: Boolean? = null,
    val activityAnomalyAlert: Boolean? = null,
    val lowBatteryAlert: Boolean? = null,
    val dailySummaryAlert: Boolean? = null
)

// ---------------------------------------------------------
// 5. 응급 요청  (신규)
// ---------------------------------------------------------

/**
 * dispatchedBy는 이제 항상 필수입니다 (ERD: not null).
 * 본인이 SOS를 직접 눌러도, 로그인된 사용자 본인의 id를 dispatchedBy에 넣어서 보내야 해요.
 * 안 보내면 서버가 요청을 거부합니다.
 */
data class EmergencyDispatchRequest(
    val deviceId: Long,
    val dispatchedBy: Long
)

data class EmergencyDispatchResult(
    val success: Boolean,
    val dispatch: EmergencyDispatchResponse? = null,
    val message: String? = null
)

data class EmergencyDispatchResponse(
    val id: Long,
    val deviceId: Long,
    val dispatchedBy: Long,
    val requestedAt: String? = null,
    val responseStatus: String? = null // REQUESTED | DISPATCHED | ARRIVED | CANCELLED
)

// ---------------------------------------------------------
// 6. 소리 이벤트  (기존 - deviceId 타입만 수정 + deviceId 필드 추가)
// ---------------------------------------------------------

data class SoundEventRequest(
    val deviceId: Long, // 기존엔 빠져있던 필드. 서버가 이제 필수로 요구함
    val rawFile: String,
    val splitFile: String,
    val predLabel: String,
    val predScore: Double,
    val segIndex: Int,
    val startSec: Int,
    val endSec: Int,
    val smoothedLabel: String
)

data class SoundEventResponse(
    val id: Long,
    val deviceId: Long? = null,   // 기존엔 빠져있던 필드
    val createdAt: String? = null, // 기존엔 빠져있던 필드
    val rawFile: String,
    val splitFile: String,
    val predLabel: String,
    val predScore: Double,
    val segIndex: Int,
    val startSec: Int,
    val endSec: Int,
    val smoothedLabel: String
)

// ---------------------------------------------------------
// 7. 최신 위치  (기존 - deviceId: String -> Long, 시간: Long -> String)
// ---------------------------------------------------------

/** 환자 기기가 서버에 최신 위치를 저장할 때 보내는 요청 데이터. */
data class LatestLocationRequest(
    val deviceId: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val homeState: String,
    val measuredAt: String
)

/** 보호자 화면이 서버에서 최신 환자 위치를 조회했을 때 받는 응답 데이터. */
data class LatestLocationResponse(
    val id: Long? = null,
    val deviceId: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val homeState: String,
    val measuredAt: String
)

// ---------------------------------------------------------
// 8. 위험 알림  (기존 - deviceId: String -> Long, 시간: Long -> String,
//    homeState/latitude/longitude 는 서버에서 제거됨 - 더 이상 안 보내도 됨)
// ---------------------------------------------------------

/** 소리와 위치 정책이 위험 조건을 만족했을 때 서버로 보내는 알림 데이터. */
data class DangerAlertRequest(
    val deviceId: Long,
    val soundLabel: String,
    val level: String,
    val reason: String,
    val detectedAt: String
    // homeState / latitude / longitude 는 danger_alerts 테이블에서 제거되어 더 이상 안 보냄.
    // 위치 정보가 필요하면 LatestLocationRequest로 따로 보내주세요.
)

/** 서버에 저장된 danger-alert 를 조회할 때 받는 응답 데이터. */
data class DangerAlertResponse(
    val id: Long? = null,
    val deviceId: Long? = null,
    val soundLabel: String? = null,
    val level: String? = null,
    val reason: String? = null,
    val detectedAt: String? = null
)

// ---------------------------------------------------------
// 9. 행동 세션  (기존 - deviceId: String -> Long, 시간: Long -> String)
// ---------------------------------------------------------

/** 서버에 저장된 행동(behavior) 세션을 조회할 때 받는 응답 데이터. */
data class BehaviorSessionResponse(
    val id: Long? = null,
    // 예전 필드 (안 쓰지만 서버 응답에 남아있을 수 있어서 유지)
    val representativeEvent: String? = null,
    val ruleResult: String? = null,
    val startSec: Int? = null,
    val endSec: Int? = null,
    // 세션 상태머신이 실제로 채우는 필드
    val deviceId: Long? = null,
    val behavior: String? = null,
    val startTime: String? = null,
    val confirmedTime: String? = null,
    val endTime: String? = null,
    val endReason: String? = null
)

/** 세션 상태머신(세탁/청소/설거지/식사/인지)이 확정 종료됐을 때 서버로 보내는 기록 데이터. */
data class BehaviorSessionRequest(
    val deviceId: Long,
    val behavior: String,
    val startTime: String,
    val confirmedTime: String,
    val endTime: String,
    val endReason: String? = null
)

// ---------------------------------------------------------
// 10. 취침/기상 이벤트  (기존 - deviceId: String -> Long, 시간: Long -> String)
// ---------------------------------------------------------

/** 취침/기상 상태머신이 SLEEP 또는 WAKE를 확정했을 때만 서버로 보내는 이벤트. */
data class SleepWakeEventRequest(
    val deviceId: Long,
    val eventType: String, // "SLEEP" | "WAKE"
    val timestamp: String
)

/** 서버에 저장된 취침/기상 확정 이벤트를 조회할 때 받는 응답 데이터. */
data class SleepWakeEventResponse(
    val id: Long? = null,
    val deviceId: Long? = null,
    val eventType: String? = null, // "SLEEP" | "WAKE"
    val timestamp: String? = null
)

// ---------------------------------------------------------
// 11. 외출/귀가 이벤트 (기존에 서버엔 있었는데 앱에서 호출을 안 하고 있던 것 - 참고용으로 추가)
// ---------------------------------------------------------

data class OutingEventRequest(
    val deviceId: Long,
    val transitionType: String,
    val timestamp: String
)

data class OutingEventResponse(
    val id: Long? = null,
    val deviceId: Long? = null,
    val transitionType: String? = null,
    val timestamp: String? = null
)

// =========================================================
// API 인터페이스
// =========================================================

interface PocoApi {

    // ---- 회원가입 / 로그인 ----
    @POST("/api/users/signup")
    suspend fun signup(@Body request: SignUpRequest): UserResponse

    @POST("/api/users/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    // ---- 기기 ----
    @POST("/api/devices")
    suspend fun registerDevice(@Body request: DeviceRequest): DeviceResponse

    @GET("/api/devices")
    suspend fun getDeviceByUserId(@Query("userId") userId: Long): DeviceResponse

    // ---- 보호자 연동 ----
    @POST("/api/link/code")
    suspend fun issueLinkCode(@Query("userId") userId: Long): LinkCodeResponse

    @POST("/api/link/redeem")
    suspend fun redeemLinkCode(@Body request: RedeemLinkRequest): RedeemLinkResult

    @GET("/api/link/list")
    suspend fun getMyLinks(@Query("userId") userId: Long, @Query("as") asRole: String): List<UserLinkResponse>

    @DELETE("/api/link/{linkId}")
    suspend fun deleteLink(@Path("linkId") linkId: Long)

    // ---- 알림 설정 ----
    @GET("/api/notification-settings")
    suspend fun getNotificationSettings(@Query("userId") userId: Long): NotificationSettingsResponse

    @PUT("/api/notification-settings")
    suspend fun updateNotificationSettings(@Body request: NotificationSettingsRequest): NotificationSettingsResponse

    // ---- 응급 요청 ----
    @POST("/api/emergency/dispatch")
    suspend fun dispatchEmergency(@Body request: EmergencyDispatchRequest): EmergencyDispatchResult

    @GET("/api/emergency/history")
    suspend fun getEmergencyHistory(@Query("deviceId") deviceId: Long): List<EmergencyDispatchResponse>

    // ---- 소리 이벤트 ----
    @POST("/api/sound-events")
    fun createSoundEvent(@Body request: SoundEventRequest): Call<Void>

    @GET("/api/sound-events")
    suspend fun getSoundEvents(@Query("deviceId") deviceId: Long? = null): List<SoundEventResponse>

    // ---- 최신 위치 ----
    @POST("/api/latest-locations")
    fun updateLatestLocation(@Body request: LatestLocationRequest): Call<Void>

    @GET("/api/latest-locations")
    suspend fun getLatestLocation(@Query("deviceId") deviceId: Long): LatestLocationResponse

    // ---- 위험 알림 ----
    @POST("/api/danger-alerts")
    fun createDangerAlert(@Body request: DangerAlertRequest): Call<Void>

    @GET("/api/danger-alerts")
    suspend fun getDangerAlerts(@Query("deviceId") deviceId: Long): List<DangerAlertResponse>

    // ---- 행동 세션 ----
    @GET("/api/behavior-sessions")
    suspend fun getBehaviorSessions(@Query("deviceId") deviceId: Long? = null): List<BehaviorSessionResponse>

    @POST("/api/behavior-sessions")
    fun createBehaviorSession(@Body request: BehaviorSessionRequest): Call<Void>

    // ---- 취침/기상 이벤트 ----
    @POST("/api/sleep-wake-events")
    fun createSleepWakeEvent(@Body request: SleepWakeEventRequest): Call<Void>

    @GET("/api/sleep-wake-events")
    suspend fun getSleepWakeEvents(@Query("deviceId") deviceId: Long): List<SleepWakeEventResponse>

    // ---- 외출/귀가 이벤트 ----
    @POST("/api/outing-events")
    fun createOutingEvent(@Body request: OutingEventRequest): Call<Void>

    @GET("/api/outing-events")
    suspend fun getOutingEvents(@Query("deviceId") deviceId: Long): List<OutingEventResponse>
}

object ServerApiClient {
    // TODO: AWS 배포되면 여기를 실제 서버 주소로 바꿔주세요. 예: "http://<AWS 공인IP>:8080/"
    // - USB + adb reverse tcp:8080 tcp:8080 방식으로 테스트할 땐: "http://127.0.0.1:8080/"
    // - 에뮬레이터에서 로컬 서버 테스트할 땐: "http://10.0.2.2:8080/"
    // - 같은 Wi-Fi에서 실기기 테스트할 땐: PC의 로컬 IP (ipconfig로 확인)
    private const val BASE_URL = "http://127.0.0.1:8080/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: PocoApi = retrofit.create(PocoApi::class.java)
}

// =========================================================
// epoch ms(Long, 앱 내부에서 쓰던 방식) <-> 서버 datetime 문자열 변환 유틸
// 서버(application.properties: serverTimezone=Asia/Seoul)와 시간대를 맞추기 위해
// Asia/Seoul 기준으로 변환한다. 앱 내부 로직(LocationSample 등)은 그대로 epoch ms를
// 쓰고, 서버로 보내거나 받을 때 이 경계에서만 변환하면 된다.
// =========================================================

private val SERVER_ZONE: java.time.ZoneId = java.time.ZoneId.of("Asia/Seoul")

/** epoch ms -> 서버가 이해하는 "2026-08-31T14:23:01.123" 형태의 문자열로 변환 */
fun Long.toServerDateTime(): String =
    java.time.Instant.ofEpochMilli(this).atZone(SERVER_ZONE).toLocalDateTime().toString()

/** 서버가 내려준 datetime 문자열 -> epoch ms 로 변환 (null/파싱 실패 시 null) */
fun String?.fromServerDateTime(): Long? {
    if (this.isNullOrBlank()) return null
    return runCatching {
        java.time.LocalDateTime.parse(this).atZone(SERVER_ZONE).toInstant().toEpochMilli()
    }.getOrNull()
}
