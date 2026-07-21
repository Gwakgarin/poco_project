package com.example.poco.location

/** 환자가 집 안인지 집 밖인지 나타내는 상태. 아직 확실하지 않으면 UNKNOWN을 사용한다. */
enum class HomeState {
    UNKNOWN,
    HOME,
    OUTSIDE
}

/** 위도와 경도로 표현한 지도상의 한 지점. */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

/** Fused Location Provider에서 받은 한 번의 위치 측정값. */
data class LocationSample(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val measuredAtEpochMs: Long
)

/** 집 중심 좌표와 HOME 판정에 사용할 반경을 묶은 설정값. */
data class HomeZone(
    val center: GeoPoint,
    val radiusMeters: Double = DEFAULT_RADIUS_METERS
) {
    init {
        require(radiusMeters > 0.0) { "Home Zone radius must be positive" }
    }

    companion object {
        /** 별도 설정이 없을 때 사용하는 기본 Home Zone 반경. */
        const val DEFAULT_RADIUS_METERS = 100.0
    }
}

/** 상태 머신이 계산한 상태, 집과의 거리, 상태 변경 및 확실성 정보. */
data class HomeStateResult(
    val state: HomeState,
    val distanceMeters: Double,
    val changed: Boolean,
    val isCertain: Boolean
)
