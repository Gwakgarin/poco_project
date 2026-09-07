package com.example.poco.location

import android.content.Context
import java.util.UUID

/**
 * Home Zone, 최신 위치, 현재 상태와 기기 식별자를 SharedPreferences에 보관한다.
 * 앱을 종료했다가 다시 실행해도 마지막 설정과 위치 상태를 복원하기 위한 로컬 저장소다.
 */
class LocationStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /** 사용자가 지정한 집 좌표와 반경을 저장한다. */
    fun saveHomeZone(homeZone: HomeZone) {
        preferences.edit()
            .putString(KEY_HOME_LATITUDE, homeZone.center.latitude.toString())
            .putString(KEY_HOME_LONGITUDE, homeZone.center.longitude.toString())
            .putString(KEY_HOME_RADIUS, homeZone.radiusMeters.toString())
            .apply()
    }

    /** 저장된 Home Zone을 반환하며 아직 설정하지 않았다면 null을 반환한다. */
    fun getHomeZone(): HomeZone? {
        val latitude = preferences.getString(KEY_HOME_LATITUDE, null)?.toDoubleOrNull() ?: return null
        val longitude = preferences.getString(KEY_HOME_LONGITUDE, null)?.toDoubleOrNull() ?: return null
        val radius = preferences.getString(KEY_HOME_RADIUS, null)?.toDoubleOrNull()
            ?: HomeZone.DEFAULT_RADIUS_METERS
        return HomeZone(GeoPoint(latitude, longitude), radius)
    }

    /** 가장 최근 위치와 그 위치를 기준으로 판정한 상태를 함께 저장한다. */
    fun saveLatest(sample: LocationSample, state: HomeState) {
        preferences.edit()
            .putString(KEY_LATEST_LATITUDE, sample.latitude.toString())
            .putString(KEY_LATEST_LONGITUDE, sample.longitude.toString())
            .putFloat(KEY_LATEST_ACCURACY, sample.accuracyMeters)
            .putLong(KEY_LATEST_TIME, sample.measuredAtEpochMs)
            .putString(KEY_HOME_STATE, state.name)
            .apply()
    }

    /** 저장된 최신 위치와 상태를 반환하며 수집 이력이 없다면 null을 반환한다. */
    fun getLatest(): Pair<LocationSample, HomeState>? {
        val latitude = preferences.getString(KEY_LATEST_LATITUDE, null)?.toDoubleOrNull() ?: return null
        val longitude = preferences.getString(KEY_LATEST_LONGITUDE, null)?.toDoubleOrNull() ?: return null
        val sample = LocationSample(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = preferences.getFloat(KEY_LATEST_ACCURACY, 0f),
            measuredAtEpochMs = preferences.getLong(KEY_LATEST_TIME, 0L)
        )
        val state = runCatching {
            HomeState.valueOf(preferences.getString(KEY_HOME_STATE, HomeState.UNKNOWN.name)!!)
        }.getOrDefault(HomeState.UNKNOWN)
        return sample to state
    }

    /** 서버가 환자 기기를 구분할 수 있도록 앱 설치별 UUID를 생성하고 재사용한다.
     *  이 값은 devices.device_uuid 로 서버에 등록하는 용도로만 쓰고,
     *  실제 이벤트 API에는 backendDeviceId()가 반환하는 숫자 id를 써야 한다. */
    fun deviceId(): String {
        preferences.getString(KEY_DEVICE_ID, null)?.let { return it }
        val newId = UUID.randomUUID().toString()
        preferences.edit().putString(KEY_DEVICE_ID, newId).apply()
        return newId
    }

    /** /api/devices 에 등록한 뒤 서버가 내려준 진짜 기기 id(Long). 아직 등록 전이면 null. */
    fun backendDeviceId(): Long? {
        val id = preferences.getLong(KEY_BACKEND_DEVICE_ID, -1L)
        return if (id == -1L) null else id
    }

    /** 기기 등록(POST /api/devices) 응답을 받은 후 이 함수로 저장해두면 다음부터 재사용된다. */
    fun saveBackendDeviceId(id: Long) {
        preferences.edit().putLong(KEY_BACKEND_DEVICE_ID, id).apply()
    }

    /** 로그인/회원가입에 성공한 사용자의 id. 아직 로그인 전이면 null. */
    fun currentUserId(): Long? {
        val id = preferences.getLong(KEY_CURRENT_USER_ID, -1L)
        return if (id == -1L) null else id
    }

    /** 로그인한 사용자의 역할. 0 = USER(사용자), 1 = GUARDIAN(보호자). */
    fun currentUserRole(): Int? {
        val role = preferences.getInt(KEY_CURRENT_USER_ROLE, -1)
        return if (role == -1) null else role
    }

    /** 로그인/회원가입 응답을 받은 직후 이 함수로 세션을 저장한다. */
    fun saveSession(userId: Long, role: Int) {
        preferences.edit()
            .putLong(KEY_CURRENT_USER_ID, userId)
            .putInt(KEY_CURRENT_USER_ROLE, role)
            .apply()
    }

    /** 로그아웃 시 세션과 기기 등록 정보를 모두 지운다 (기기 자체 UUID는 유지). */
    fun clearSession() {
        preferences.edit()
            .remove(KEY_CURRENT_USER_ID)
            .remove(KEY_CURRENT_USER_ROLE)
            .remove(KEY_BACKEND_DEVICE_ID)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "poco_location"
        const val KEY_HOME_LATITUDE = "home_latitude"
        const val KEY_HOME_LONGITUDE = "home_longitude"
        const val KEY_HOME_RADIUS = "home_radius"
        const val KEY_LATEST_LATITUDE = "latest_latitude"
        const val KEY_LATEST_LONGITUDE = "latest_longitude"
        const val KEY_LATEST_ACCURACY = "latest_accuracy"
        const val KEY_LATEST_TIME = "latest_time"
        const val KEY_HOME_STATE = "home_state"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_BACKEND_DEVICE_ID = "backend_device_id"
        const val KEY_CURRENT_USER_ID = "current_user_id"
        const val KEY_CURRENT_USER_ROLE = "current_user_role"
    }
}
