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
}

object ServerApiClient {
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://127.0.0.1:8080/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: SoundEventApi = retrofit.create(SoundEventApi::class.java)
}
