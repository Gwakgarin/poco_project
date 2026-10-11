package com.example.poco.location

import android.util.Log
import com.example.poco.LatestLocationRequest
import com.example.poco.OutingEventRequest
import com.example.poco.ServerApiClient
import com.example.poco.toServerDateTime
import java.io.Closeable
import java.util.concurrent.Executors

/**
 * 최신 위치를 서버에 전송한다.
 * GPS 콜백이나 UI 스레드를 막지 않도록 네트워크 요청을 전용 단일 스레드에서 처리한다.
 *
 * deviceId는 생성 시점의 고정값이 아니라 LocationStore에서 매번 다시 읽어온다 -
 * 서비스가 시작될 때는 아직 기기 등록(devices.id 발급)이 안 끝났을 수도 있어서,
 * 실제로 업로드하는 시점에 등록이 끝나 있는지 다시 확인하는 게 안전하다.
 */
class LocationUploadManager(
    private val locationStore: LocationStore,
    private val onResult: (String) -> Unit = {}
) : Closeable {
    private val executor = Executors.newSingleThreadExecutor()

    /** 위치, 정확도, HOME 상태와 측정 시각을 최신 위치 API로 전송한다. */
    fun upload(sample: LocationSample, state: HomeState) {
        executor.execute {
            val deviceId = locationStore.backendDeviceId()
            if (deviceId == null) {
                Log.w("POCO", "Latest location upload skipped: device not registered yet")
                onResult("Location save skipped: device not registered yet")
                return@execute
            }
            val status = try {
                val response = ServerApiClient.api.updateLatestLocation(
                    LatestLocationRequest(
                        deviceId = deviceId,
                        latitude = sample.latitude,
                        longitude = sample.longitude,
                        accuracyMeters = sample.accuracyMeters,
                        homeState = state.name,
                        measuredAt = sample.measuredAtEpochMs.toServerDateTime()
                    )
                ).execute()
                if (response.isSuccessful) {
                    Log.d("POCO", "Latest location saved: HTTP ${response.code()} (${state.name})")
                    "Location saved: HTTP ${response.code()}"
                } else {
                    Log.w("POCO", "Latest location save failed: HTTP ${response.code()}")
                    "Location save failed: HTTP ${response.code()}"
                }
            } catch (error: Throwable) {
                Log.e("POCO", "Latest location upload failed", error)
                "Location save failed: ${error.message ?: error::class.java.simpleName}"
            }
            onResult(status)
        }
    }

    /**
     * HOME<->OUTSIDE 상태가 실제로 바뀐 순간(= 외출 또는 귀가) 하나의 이벤트로 저장한다.
     * 매 위치 갱신마다 부르는 게 아니라, HomeStateMachine이 changed=true를 돌려줄 때만 호출해야 한다.
     *
     * 최신 위치와 달리 이 이벤트는 전환 순간 한 번만 생기므로, 그때 네트워크가 끊겨 있으면 그냥 사라진다.
     * 그래서 실패하면 LocationStore의 재시도 대기열에 넣어두고 flushPendingOutingEvents()가 다시 보낸다.
     */
    fun uploadOutingEvent(transitionType: String, measuredAtEpochMs: Long) {
        executor.execute {
            val deviceId = locationStore.backendDeviceId()
            if (deviceId == null) {
                // 기기 등록 전이면 지금은 못 보내지만 전환 자체는 실제로 일어났으므로 대기열에 남긴다.
                Log.w("POCO", "Outing event queued: device not registered yet ($transitionType)")
                locationStore.addPendingOutingEvent(transitionType, measuredAtEpochMs)
                onResult("Outing event queued: device not registered yet")
                return@execute
            }
            val sent = sendOutingEvent(deviceId, transitionType, measuredAtEpochMs)
            if (!sent) {
                locationStore.addPendingOutingEvent(transitionType, measuredAtEpochMs)
            }
            onResult(
                if (sent) "Outing event saved ($transitionType)"
                else "Outing event queued for retry ($transitionType)"
            )
        }
    }

    /**
     * 전송 못 한 외출/귀가 이벤트를 다시 보낸다. 위치 갱신마다(=30초 주기) 호출하면
     * 네트워크가 돌아왔을 때 자동으로 밀린 기록이 올라간다. 성공한 것만 대기열에서 지운다.
     */
    fun flushPendingOutingEvents() {
        executor.execute {
            val pending = locationStore.pendingOutingEvents()
            if (pending.isEmpty()) return@execute
            val deviceId = locationStore.backendDeviceId() ?: return@execute

            val stillPending = pending.filterNot { (transitionType, timestamp) ->
                sendOutingEvent(deviceId, transitionType, timestamp)
            }
            if (stillPending.size != pending.size) {
                Log.d("POCO", "Outing event retry: ${pending.size - stillPending.size} sent, ${stillPending.size} left")
                locationStore.savePendingOutingEvents(stillPending)
                onResult("Outing event retry: ${pending.size - stillPending.size} sent")
            }
        }
    }

    /** 실제 전송 1건. 성공하면 true. 반드시 executor 스레드에서만 호출한다. */
    private fun sendOutingEvent(deviceId: Long, transitionType: String, measuredAtEpochMs: Long): Boolean =
        try {
            val response = ServerApiClient.api.createOutingEvent(
                OutingEventRequest(
                    deviceId = deviceId,
                    transitionType = transitionType,
                    timestamp = measuredAtEpochMs.toServerDateTime()
                )
            ).execute()
            if (response.isSuccessful) {
                Log.d("POCO", "Outing event saved ($transitionType): HTTP ${response.code()}")
            } else {
                Log.w("POCO", "Outing event save failed ($transitionType): HTTP ${response.code()}")
            }
            response.isSuccessful
        } catch (error: Throwable) {
            Log.e("POCO", "Outing event upload failed ($transitionType)", error)
            false
        }

    /** 서비스가 종료될 때 남아 있는 업로드 작업과 스레드를 정리한다. */
    override fun close() {
        executor.shutdownNow()
    }
}