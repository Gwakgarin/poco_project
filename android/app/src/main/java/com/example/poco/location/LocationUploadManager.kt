package com.example.poco.location

import android.util.Log
import com.example.poco.LatestLocationRequest
import com.example.poco.ServerApiClient
import java.io.Closeable
import java.util.concurrent.Executors

/**
 * 최신 위치를 서버에 전송한다.
 * GPS 콜백이나 UI 스레드를 막지 않도록 네트워크 요청을 전용 단일 스레드에서 처리한다.
 */
class LocationUploadManager(
    private val deviceId: String,
    private val onResult: (String) -> Unit = {}
) : Closeable {
    private val executor = Executors.newSingleThreadExecutor()

    /** 위치, 정확도, HOME 상태와 측정 시각을 최신 위치 API로 전송한다. */
    fun upload(sample: LocationSample, state: HomeState) {
        executor.execute {
            val status = try {
                val response = ServerApiClient.api.updateLatestLocation(
                    LatestLocationRequest(
                        deviceId = deviceId,
                        latitude = sample.latitude,
                        longitude = sample.longitude,
                        accuracyMeters = sample.accuracyMeters,
                        homeState = state.name,
                        measuredAtEpochMs = sample.measuredAtEpochMs
                    )
                ).execute()
                if (response.isSuccessful) {
                    "Location saved: HTTP ${response.code()}"
                } else {
                    "Location save failed: HTTP ${response.code()}"
                }
            } catch (error: Throwable) {
                Log.e("POCO", "Latest location upload failed", error)
                "Location save failed: ${error.message ?: error::class.java.simpleName}"
            }
            onResult(status)
        }
    }

    /** 서비스가 종료될 때 남아 있는 업로드 작업과 스레드를 정리한다. */
    override fun close() {
        executor.shutdownNow()
    }
}
