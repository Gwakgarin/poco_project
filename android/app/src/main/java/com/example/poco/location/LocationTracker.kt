package com.example.poco.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * Fused Location Provider에 주기적인 위치 업데이트를 요청한다.
 * 수신한 위치는 로컬에 저장하고 Home Zone이 설정되어 있으면 상태 머신으로 판정한다.
 */
class LocationTracker(
    context: Context,
    private val store: LocationStore,
    private val onLocation: (LocationSample, HomeStateResult?) -> Unit,
    private val onError: (Throwable) -> Unit
) {
    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)
    private val stateMachine = HomeStateMachine(store.getLatest()?.second ?: HomeState.UNKNOWN)
    private var started = false

    /** Fused Location Provider가 전달한 위치들을 앱 모델로 변환하는 콜백. */
    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { location ->
                // 정확도 정보가 없는 좌표는 HOME/OUTSIDE 판정에 사용하지 않는다.
                if (!location.hasAccuracy() || location.accuracy < 0f) return@forEach
                val sample = LocationSample(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyMeters = location.accuracy,
                    measuredAtEpochMs = location.time
                )
                val homeZone = store.getHomeZone()
                // 집을 아직 설정하지 않았다면 상태 머신을 실행하지 않고 UNKNOWN으로 저장한다.
                val stateResult = homeZone?.let { stateMachine.update(sample, it) }
                val state = stateResult?.state ?: HomeState.UNKNOWN
                store.saveLatest(sample, state)
                onLocation(sample, stateResult)
            }
        }
    }

    /** 위치 권한을 확인한 뒤 주기적인 고정밀 위치 수집을 시작한다. */
    fun start() {
        if (started) return
        if (!hasLocationPermission()) {
            onError(SecurityException("Location permission is not granted"))
            return
        }

        // 기본 30초 주기이며, 이동량과 시스템 상황에 따라 실제 전달 간격은 달라질 수 있다.
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            UPDATE_INTERVAL_MS
        )
            .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
            .setMaxUpdateDelayMillis(MAX_UPDATE_DELAY_MS)
            .setMinUpdateDistanceMeters(MIN_UPDATE_DISTANCE_METERS)
            .build()

        try {
            started = true
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                .addOnFailureListener { error ->
                    started = false
                    onError(error)
                }
        } catch (error: SecurityException) {
            started = false
            onError(error)
        }
    }

    /** 등록했던 위치 콜백을 해제해 GPS 수집을 중단한다. */
    fun stop() {
        if (!started) return
        client.removeLocationUpdates(callback)
        started = false
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val UPDATE_INTERVAL_MS = 30_000L
        const val MIN_UPDATE_INTERVAL_MS = 15_000L
        const val MAX_UPDATE_DELAY_MS = 60_000L
        const val MIN_UPDATE_DISTANCE_METERS = 10f
    }
}
