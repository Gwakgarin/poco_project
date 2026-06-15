package com.example.poco

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

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
    fun createSoundEvent(@Body request: SoundEventRequest): Call<Void>

    @GET("/api/sound-events")
    suspend fun getSoundEvents(): List<SoundEventResponse>
}

object ServerApiClient {
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://127.0.0.1:8080/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: SoundEventApi = retrofit.create(SoundEventApi::class.java)
}
