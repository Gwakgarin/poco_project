package com.example.poco.location

import com.example.poco.ServerApiClient

/** 보호자 화면에서 환자의 최신 위치를 조회하기 위한 읽기 전용 저장소. */
class PatientLocationRepository {
    /** 서버 응답을 앱에서 사용하는 위치 모델과 HOME 상태로 변환해 반환한다. */
    suspend fun getLatest(deviceId: String): Pair<LocationSample, HomeState> {
        val response = ServerApiClient.api.getLatestLocation(deviceId)
        val state = runCatching { HomeState.valueOf(response.homeState) }
            .getOrDefault(HomeState.UNKNOWN)
        return LocationSample(
            latitude = response.latitude,
            longitude = response.longitude,
            accuracyMeters = response.accuracyMeters,
            measuredAtEpochMs = response.measuredAtEpochMs
        ) to state
    }
}
