package com.example.poco.location

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 집과 현재 위치 사이의 거리 및 GPS 정확도를 이용해 HOME/OUTSIDE 상태를 관리한다.
 *
 * - distance + accuracy <= radius: 확실한 HOME
 * - distance - accuracy > radius: 확실한 OUTSIDE
 * - 그 외: GPS 오차 범위가 경계와 겹치므로 기존 상태 유지
 */
class HomeStateMachine(initialState: HomeState = HomeState.UNKNOWN) {
    var state: HomeState = initialState
        private set

    /** 새 위치를 반영하고 판정 결과와 집까지의 거리를 반환한다. */
    fun update(sample: LocationSample, homeZone: HomeZone): HomeStateResult {
        val distance = distanceMeters(
            GeoPoint(sample.latitude, sample.longitude),
            homeZone.center
        )
        val accuracy = sample.accuracyMeters.coerceAtLeast(0f).toDouble()
        // 정확도를 현재 위치 주변의 오차 반경으로 보고 확실한 경우에만 상태를 변경한다.
        val nextState = when {
            distance + accuracy <= homeZone.radiusMeters -> HomeState.HOME
            distance - accuracy > homeZone.radiusMeters -> HomeState.OUTSIDE
            else -> state
        }
        val certain = nextState != state ||
            distance + accuracy <= homeZone.radiusMeters ||
            distance - accuracy > homeZone.radiusMeters
        val changed = nextState != state
        state = nextState

        return HomeStateResult(
            state = state,
            distanceMeters = distance,
            changed = changed,
            isCertain = certain
        )
    }

    /** Home Zone 변경 등으로 기존 판단을 폐기해야 할 때 상태를 초기화한다. */
    fun reset(newState: HomeState = HomeState.UNKNOWN) {
        state = newState
    }

    companion object {
        private const val EARTH_RADIUS_METERS = 6_371_000.0

        /** Haversine 공식을 사용해 두 위경도 좌표 사이의 직선거리를 미터로 계산한다. */
        fun distanceMeters(from: GeoPoint, to: GeoPoint): Double {
            val lat1 = Math.toRadians(from.latitude)
            val lat2 = Math.toRadians(to.latitude)
            val deltaLat = Math.toRadians(to.latitude - from.latitude)
            val deltaLon = Math.toRadians(to.longitude - from.longitude)
            val a = (sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2)
                ).coerceIn(0.0, 1.0)
            return EARTH_RADIUS_METERS * 2 * atan2(sqrt(a), sqrt(1 - a))
        }
    }
}
