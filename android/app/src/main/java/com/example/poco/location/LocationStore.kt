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

    /**
     * 아직 서버에 못 보낸 외출/귀가 이벤트 목록.
     * 외출 이벤트는 전환 순간 딱 한 번만 생기기 때문에, 그 순간 네트워크가 끊겨 있으면 영영 사라진다
     * (최신 위치는 30초마다 다시 보내니까 알아서 복구되지만 이건 그렇지 않다).
     * 그래서 전송 실패분을 여기 적어두고 다음 위치 갱신 때 다시 시도한다.
     * 저장 형식: "HOME_TO_OUTSIDE|1699999999999" 를 ";" 로 이어붙인 문자열.
     */
    fun pendingOutingEvents(): List<Pair<String, Long>> {
        val raw = preferences.getString(KEY_PENDING_OUTING_EVENTS, null) ?: return emptyList()
        return raw.split(ENTRY_SEPARATOR)
            .mapNotNull { entry ->
                val parts = entry.split(FIELD_SEPARATOR)
                val type = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val timestamp = parts.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
                type to timestamp
            }
    }

    /** 재시도 대기열을 통째로 교체한다. 너무 오래된 것까지 무한정 쌓이지 않도록 최근 것 위주로 자른다. */
    fun savePendingOutingEvents(events: List<Pair<String, Long>>) {
        val serialized = events
            .takeLast(MAX_PENDING_OUTING_EVENTS)
            .joinToString(ENTRY_SEPARATOR) { (type, timestamp) -> "$type$FIELD_SEPARATOR$timestamp" }
        preferences.edit().putString(KEY_PENDING_OUTING_EVENTS, serialized).apply()
    }

    /** 전송에 실패한 외출/귀가 이벤트를 재시도 대기열 끝에 추가한다. */
    fun addPendingOutingEvent(transitionType: String, timestampEpochMs: Long) {
        savePendingOutingEvents(pendingOutingEvents() + (transitionType to timestampEpochMs))
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

    /** 로그인한 사용자의 이름. 아직 로그인 전이면 null. */
    fun currentUserName(): String? = preferences.getString(KEY_CURRENT_USER_NAME, null)

    /** 로그인한 사용자의 이메일. 아직 로그인 전이면 null. */
    fun currentUserEmail(): String? = preferences.getString(KEY_CURRENT_USER_EMAIL, null)

    /** 로그인한 사용자의 가입일(서버 datetime 문자열). 아직 로그인 전이거나 서버가 안 내려줬으면 null. */
    fun currentUserJoinedAt(): String? = preferences.getString(KEY_CURRENT_USER_JOINED_AT, null)

    /** 로그인/회원가입 응답을 받은 직후 이 함수로 세션을 저장한다. */
    fun saveSession(userId: Long, role: Int, name: String, email: String, joinedAt: String?) {
        preferences.edit()
            .putLong(KEY_CURRENT_USER_ID, userId)
            .putInt(KEY_CURRENT_USER_ROLE, role)
            .putString(KEY_CURRENT_USER_NAME, name)
            .putString(KEY_CURRENT_USER_EMAIL, email)
            .putString(KEY_CURRENT_USER_JOINED_AT, joinedAt)
            .apply()
    }

    /** 로그아웃 시 세션과 기기 등록 정보를 모두 지운다 (기기 자체 UUID는 유지). */
    fun clearSession() {
        preferences.edit()
            .remove(KEY_CURRENT_USER_ID)
            .remove(KEY_CURRENT_USER_ROLE)
            .remove(KEY_CURRENT_USER_NAME)
            .remove(KEY_CURRENT_USER_EMAIL)
            .remove(KEY_CURRENT_USER_JOINED_AT)
            .remove(KEY_BACKEND_DEVICE_ID)
            // 기기 등록이 풀리므로 재시도 대기열도 비운다 - 안 그러면 다음에 로그인한
            // 다른 사용자의 기기 id로 이전 사용자의 외출 기록이 올라간다.
            .remove(KEY_PENDING_OUTING_EVENTS)
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
        const val KEY_CURRENT_USER_NAME = "current_user_name"
        const val KEY_CURRENT_USER_EMAIL = "current_user_email"
        const val KEY_CURRENT_USER_JOINED_AT = "current_user_joined_at"
        const val KEY_PENDING_OUTING_EVENTS = "pending_outing_events"
        const val ENTRY_SEPARATOR = ";"
        const val FIELD_SEPARATOR = "|"
        const val MAX_PENDING_OUTING_EVENTS = 50
    }
}