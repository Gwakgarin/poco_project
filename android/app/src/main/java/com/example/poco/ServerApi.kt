package com.example.poco

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

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

interface SoundEventApi {
    @POST("/api/sound-events")
    fun createSoundEvent(@Body request: SoundEventRequest): Call<SoundEventResponse>

    @GET("/api/sound-events")
    suspend fun getSoundEvents(): List<SoundEventResponse>
}

object ServerApiClient {
    private const val BASE_URL = "http://10.208.2.55:8080/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: SoundEventApi = retrofit.create(SoundEventApi::class.java)
}
