package com.example.poco.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.poco.BehaviorSessionResponse
import com.example.poco.AlertRequest
import com.example.poco.AlertResponse
import com.example.poco.DangerAlertResponse
import com.example.poco.NoticeResponse
import com.example.poco.DeviceRequest
import com.example.poco.DeviceResponse
import com.example.poco.EmergencyDispatchRequest
import com.example.poco.LoginRequest
import com.example.poco.NotificationSettingsRequest
import com.example.poco.RedeemLinkRequest
import com.example.poco.ServerApiClient
import com.example.poco.SignUpRequest
import com.example.poco.SleepWakeEventResponse
import com.example.poco.SoundEventResponse
import com.example.poco.UserLinkResponse
import com.example.poco.fromServerDateTime
import com.example.poco.toServerDateTime
import com.example.poco.location.LocationStore
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import com.example.poco.ui.components.AppTab
import com.example.poco.ui.components.GuardianTab
import com.example.poco.ui.screens.AccountInfoScreen
import com.example.poco.ui.screens.ActivityLogItem
import com.example.poco.ui.screens.ActivityLogScreen
import com.example.poco.ui.screens.AnomalyAlert
import com.example.poco.ui.screens.AnomalyType
import com.example.poco.ui.screens.EmergencyDispatchedScreen
import com.example.poco.ui.screens.EmergencyGuardianScreen
import com.example.poco.ui.screens.EmergencyLocationScreen
import com.example.poco.ui.screens.EmergencyUserScreen
import com.example.poco.ui.screens.GuardianDailyMonitoringScreen
import com.example.poco.ui.screens.GuardianHomeScreen
import com.example.poco.ui.screens.GuardianLinkManagementScreen
import com.example.poco.ui.screens.GuardianSettingsScreen
import com.example.poco.ui.screens.GuardianTrendScreen
import com.example.poco.ui.screens.GuardianUserLinkInfoScreen
import com.example.poco.ui.screens.LinkedPerson
import com.example.poco.ui.screens.LinkedPersonRole
import com.example.poco.ui.screens.LoginFormScreen
import com.example.poco.ui.screens.LoginScreen
import com.example.poco.ui.screens.MicSensitivityScreen
import com.example.poco.ui.screens.ChangeStatus
import com.example.poco.ui.screens.GeneralNotice
import com.example.poco.ui.screens.NotificationCenterScreen
import com.example.poco.ui.screens.RegularityLevel
import com.example.poco.ui.screens.TrendAggregator
import com.example.poco.ui.screens.TrendPeriod
import com.example.poco.ui.theme.PocoGreen
import com.example.poco.ui.theme.PocoTextMuted
import com.example.poco.ui.screens.NotificationSettingsScreen
import com.example.poco.ui.screens.QrScanScreen
import com.example.poco.ui.screens.QrShowScreen
import com.example.poco.ui.screens.RelationSelectScreen
import com.example.poco.ui.screens.RoleSelectScreen
import com.example.poco.ui.screens.SettingsScreen
import com.example.poco.ui.screens.SignUpScreen
import com.example.poco.ui.screens.SplashScreen
import com.example.poco.ui.screens.TimelineEntry
import com.example.poco.ui.screens.TrendUiState
import com.example.poco.ui.screens.TrendUiStateMapper
import com.example.poco.ui.screens.UserHomeScreen
import com.example.poco.ui.screens.UserHomeUiState
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** behavior-session 의 대표 이벤트 문구를 보고 적당한 아이콘을 골라준다 (없으면 기본 아이콘). */
private fun iconForEvent(label: String?): androidx.compose.ui.graphics.vector.ImageVector {
    val text = label.orEmpty()
    return if (text.contains("식사") || text.contains("meal", ignoreCase = true)) {
        Icons.Filled.Restaurant
    } else {
        Icons.Filled.VolumeUp
    }
}

/** danger-alert 의 reason/soundLabel 문구를 보고 이상탐지 카테고리를 추정한다 (정식 이상탐지 로직이 붙기 전까지의 임시 매핑). */
private fun anomalyTypeFor(reason: String?, soundLabel: String?): AnomalyType {
    val text = "${reason.orEmpty()} ${soundLabel.orEmpty()}"
    return when {
        text.contains("식사") || text.contains("meal", ignoreCase = true) -> AnomalyType.MEAL_IRREGULAR
        text.contains("외출") || text.contains("outing", ignoreCase = true) -> AnomalyType.OUTING_DECREASE
        else -> AnomalyType.COGNITIVE_DECREASE
    }
}

private val timeFormatter = SimpleDateFormat("a h:mm", Locale.KOREAN)
private val joinedDateFormatter = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREAN)

/** "meal"/"cleaning" 같은 영문 behavior 코드를 화면에 보여줄 한글 라벨로 바꿔준다. */
private fun behaviorLabel(behavior: String?): String = when (behavior) {
    "meal" -> "식사"
    "cleaning" -> "청소"
    "laundry" -> "세탁"
    "dishwashing" -> "설거지"
    "cognitive" -> "대화·TV 활동"
    else -> behavior ?: "행동 감지"
}

private fun BehaviorSessionResponse.toActivityLogItem(): ActivityLogItem {
    val label = behaviorLabel(behavior).let { if (behavior != null) "$it 감지" else it }
    val startTimeMs = startTime.fromServerDateTime()
    val time = startTimeMs?.let { timeFormatter.format(it) } ?: "${startSec ?: 0}s~${endSec ?: 0}s"
    return ActivityLogItem(time = time, label = label, icon = iconForEvent(behavior ?: representativeEvent))
}

private fun SoundEventResponse.toActivityLogItem() = ActivityLogItem(
    time = "${startSec}s~${endSec}s",
    label = smoothedLabel.ifBlank { predLabel },
    icon = iconForEvent(smoothedLabel.ifBlank { predLabel })
)

private fun BehaviorSessionResponse.toTimelineEntry(): TimelineEntry {
    val label = behaviorLabel(behavior).let { if (behavior != null) "$it 감지" else it }
    val startTimeMs = startTime.fromServerDateTime()
    val time = startTimeMs?.let { timeFormatter.format(it) } ?: "${startSec ?: 0}s~${endSec ?: 0}s"
    return TimelineEntry(time = time, label = label, isRisk = false)
}

/** label_map.json의 8개 소리 클래스 코드를 한글로 바꿔준다 (행동 세션용 behaviorLabel과는 별개). */
private fun soundClassLabel(soundLabel: String?): String = when (soundLabel) {
    "car_horn" -> "경적"
    "dishes" -> "그릇 소리"
    "microwave" -> "전자레인지"
    "scream" -> "비명"
    "tv" -> "TV"
    "vacuum" -> "청소기"
    "washing_machine" -> "세탁기"
    "water" -> "물소리"
    else -> soundLabel ?: "알 수 없는 소리"
}

/** DangerPolicy.kt가 만드는 "scream_detected"/"car_horn_repeated" 같은 영문 reason 코드를 한글로 바꿔준다. */
private fun dangerReasonLabel(reason: String?, soundLabel: String?): String = when (reason) {
    "scream_detected" -> "비명 감지"
    "car_horn_repeated" -> "반복 경적 감지"
    else -> "${soundClassLabel(soundLabel)} 감지"
}

private fun DangerAlertResponse.toTimelineEntry() = TimelineEntry(
    time = detectedAt.fromServerDateTime()?.let { timeFormatter.format(it) } ?: "-",
    label = dangerReasonLabel(reason, soundLabel),
    isRisk = true
)

private fun DangerAlertResponse.toAnomalyAlert() = AnomalyAlert(
    time = detectedAt.fromServerDateTime()?.let { timeFormatter.format(it) } ?: "-",
    type = anomalyTypeFor(reason, soundLabel),
    evidence = dangerReasonLabel(reason, soundLabel)
)

/** /api/alerts에 실제로 생성/조회된 이상탐지 알림을 화면 모델로 바꾼다. */
private fun AlertResponse.toAnomalyAlert() = AnomalyAlert(
    time = time.fromServerDateTime()?.let { timeFormatter.format(it) } ?: "-",
    type = runCatching { AnomalyType.valueOf(type ?: "") }.getOrDefault(AnomalyType.COGNITIVE_DECREASE),
    evidence = evidence ?: "이상 활동이 감지됐어요"
)

/** /api/notices 조회 응답(NoticeService가 규칙으로 채워준 배터리부족·일일요약)을 화면 모델로 바꾼다. */
private fun NoticeResponse.toGeneralNotice(): GeneralNotice {
    val isBattery = title?.contains("배터리") == true
    return GeneralNotice(
        time = time.fromServerDateTime()?.let { timeFormatter.format(it) } ?: "-",
        title = title ?: "알림",
        description = description ?: "",
        icon = if (isBattery) Icons.Filled.BatteryAlert else Icons.Filled.CheckCircle,
        tint = if (isBattery) PocoTextMuted else PocoGreen
    )
}

private fun SleepWakeEventResponse.toTimelineEntry() = TimelineEntry(
    time = timestamp.fromServerDateTime()?.let { timeFormatter.format(it) } ?: "-",
    label = if (eventType.equals("wake", ignoreCase = true)) "기상" else "취침",
    isRisk = false
)

/**
 * sleep 확정 이벤트 -> 바로 다음 wake 확정 이벤트로 이어지는 구간을 짝지은 뒤,
 * "기상 시각이 오늘인" 페어(=어젯밤부터 오늘 아침까지 잔 수면)만 총 수면 시간에 합산한다.
 *
 * 취침은 보통 전날 밤, 기상은 당일 아침이라 두 이벤트의 날짜가 다르다. 그래서 이 함수는
 * (today-필터링된 리스트가 아니라) 필터링 전 전체 sleepWakeEvents를 받아야 한다 — 미리
 * 오늘 것만 걸러서 넘기면 취침 이벤트가 통째로 빠져서 페어가 깨지고 "-"만 나오게 된다.
 */
private fun List<SleepWakeEventResponse>.totalSleepDurationLabel(): String {
    val sorted = sortedBy { it.timestamp.fromServerDateTime() ?: 0L }
    var totalMs = 0L
    var pendingSleepAt: Long? = null
    for (event in sorted) {
        val eventTimeMs = event.timestamp.fromServerDateTime()
        when {
            event.eventType.equals("sleep", ignoreCase = true) -> pendingSleepAt = eventTimeMs
            event.eventType.equals("wake", ignoreCase = true) -> {
                val sleptAt = pendingSleepAt
                val wokeAt = eventTimeMs
                if (sleptAt != null && wokeAt != null && wokeAt > sleptAt && wokeAt.isToday()) {
                    totalMs += wokeAt - sleptAt
                }
                pendingSleepAt = null
            }
        }
    }
    if (totalMs <= 0L) return "-"
    val totalMinutes = totalMs / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}시간 ${minutes}분" else "${minutes}분"
}

/** 오늘 확정된 behavior_sessions를 시작 시각(startTime) 기준 0~23시 버킷으로 묶어 "24시간 생활 리듬" 막대그래프 값을 만든다. */
private fun List<BehaviorSessionResponse>.hourlyRhythmToday(): List<Int> {
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    val startOfToday = cal.timeInMillis
    val endOfToday = startOfToday + 24 * 60 * 60 * 1000L

    val buckets = IntArray(24)
    for (session in this) {
        val startTime = session.startTime.fromServerDateTime() ?: continue
        if (startTime !in startOfToday until endOfToday) continue
        cal.timeInMillis = startTime
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        buckets[hour] += 1
    }
    return buckets.toList()
}

object PocoRoutes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val LOGIN_FORM = "login_form"
    const val SIGN_UP = "sign_up"
    const val ROLE_SELECT = "role_select"
    const val QR_SHOW = "qr_show"
    const val QR_SCAN = "qr_scan"
    const val RELATION_SELECT = "relation_select"
    const val USER_HOME = "user_home"
    const val USER_ACTIVITY = "user_activity"
    const val USER_SETTINGS = "user_settings"
    const val MIC_SENSITIVITY = "mic_sensitivity"
    const val NOTIFICATION_SETTINGS = "notification_settings"
    const val ACCOUNT_INFO = "account_info"
    const val GUARDIAN_LINK_MANAGEMENT = "guardian_link_management"
    const val EMERGENCY_USER = "emergency_user"
    const val EMERGENCY_GUARDIAN = "emergency_guardian"
    const val EMERGENCY_LOCATION = "emergency_location"
    const val EMERGENCY_DISPATCHED = "emergency_dispatched"
    const val GUARDIAN_HOME = "guardian_home"
    const val GUARDIAN_TIMELINE = "guardian_timeline"
    const val GUARDIAN_TREND = "guardian_trend"
    const val GUARDIAN_ALERTS = "guardian_alerts"
    const val GUARDIAN_SETTINGS = "guardian_settings"
    const val GUARDIAN_USER_LINK_INFO = "guardian_user_link_info"
    const val GUARDIAN_NOTIFICATION_SETTINGS = "guardian_notification_settings"
    const val GUARDIAN_LINK_NEW_USER = "guardian_link_new_user"
}

// TODO: replace with the linked user's real phone number and last-known location once the backend exposes them.
private const val LINKED_USER_PHONE_NUMBER = "01000000000"
private const val LINKED_USER_LAST_LOCATION_QUERY = "37.5665,126.9780"

/** SIGN_UP 화면에서 입력받은 값을 ROLE_SELECT에서 실제 signup() 호출 때까지 들고 있기 위한 홀더. */
private data class PendingSignUp(
    val name: String,
    val email: String,
    val phoneNumber: String,
    val password: String
)

/** role=0(사용자)이면 기기를 등록하고 backendDeviceId를 저장한다.
 *  네트워크 재시도 등으로 registerDevice가 중복 호출되면 devices.user_id UNIQUE 제약에 걸려 실패하는데,
 *  그 경우엔 이미 등록된 기기를 다시 조회해서 정상 처리한다. */
private suspend fun registerDeviceIfUser(locationStore: LocationStore, userId: Long, role: Int) {
    if (role != 0) return
    val device = runCatching {
        ServerApiClient.api.registerDevice(
            DeviceRequest(
                userId = userId,
                deviceUuid = locationStore.deviceId(),
                micSensitivity = 0.5f,
                micOn = true
            )
        )
    }.getOrElse { ServerApiClient.api.getDeviceByUserId(userId) }
    locationStore.saveBackendDeviceId(device.id)
}

/** 서버가 실패 이유를 구체적으로 안 알려줘서(상태 코드만 다름) 완벽하진 않지만,
 *  500(주로 이메일 unique 제약 위반)이면 이메일 중복 가능성을 안내하고, 그 외엔 일반 실패 메시지를 보여준다. */
private fun signUpErrorMessage(throwable: Throwable): String {
    val code = (throwable as? retrofit2.HttpException)?.code()
    return when (code) {
        500 -> "이미 가입된 이메일이거나 서버에 문제가 있어요. 다른 이메일로 다시 시도해보세요"
        else -> "가입에 실패했어요. 잠시 후 다시 시도해주세요"
    }
}

private data class MonitoredUser(val device: DeviceResponse, val relationLabel: String, val name: String?)

/** 보호자 화면들은 자기 자신의 backendDeviceId가 아니라, 연동된(GUARDIAN으로 링크된) 사용자의 기기를 봐야 한다.
 *  링크 목록에서 첫 번째 연동 사용자를 찾아 그 사용자의 기기 정보를 조회한다. */
private suspend fun resolveMonitoredUser(locationStore: LocationStore): MonitoredUser? {
    val guardianId = locationStore.currentUserId() ?: return null
    val link = ServerApiClient.api.getMyLinks(guardianId, "GUARDIAN").firstOrNull() ?: return null
    val device = ServerApiClient.api.getDeviceByUserId(link.userId)
    val name = runCatching { ServerApiClient.api.getUser(link.userId).name }.getOrNull()
    return MonitoredUser(device = device, relationLabel = link.relationLabel ?: "연동된 사용자", name = name)
}

/** epoch ms가 "오늘"(기기 로컬 자정~자정) 범위인지 확인한다. */
private fun Long?.isToday(): Boolean {
    if (this == null) return false
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    val startOfToday = cal.timeInMillis
    val endOfToday = startOfToday + 24 * 60 * 60 * 1000L
    return this in startOfToday until endOfToday
}

private fun NavHostController.navigateTopLevel(route: String, popUpToRoute: String) {
    navigate(route) {
        popUpTo(popUpToRoute) { inclusive = true }
        launchSingleTop = true
    }
}

private val slideInFromRight = slideInHorizontally(animationSpec = tween(280)) { it / 3 } + fadeIn(tween(280))
private val slideOutToLeft = slideOutHorizontally(animationSpec = tween(280)) { -it / 4 } + fadeOut(tween(200))
private val slideInFromLeft = slideInHorizontally(animationSpec = tween(280)) { -it / 4 } + fadeIn(tween(280))
private val slideOutToRight = slideOutHorizontally(animationSpec = tween(280)) { it / 3 } + fadeOut(tween(200))

@Composable
fun PocoNavHost(
    homeUiState: UserHomeUiState,
    onSaveCurrentLocationAsHome: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    var pendingSignUp by remember { mutableStateOf<PendingSignUp?>(null) }
    var pendingLinkCode by remember { mutableStateOf<String?>(null) }

    NavHost(
        navController = navController,
        startDestination = PocoRoutes.SPLASH,
        modifier = modifier,
        enterTransition = { slideInFromRight },
        exitTransition = { slideOutToLeft },
        popEnterTransition = { slideInFromLeft },
        popExitTransition = { slideOutToRight }
    ) {
        composable(PocoRoutes.SPLASH) {
            SplashScreen(
                onTimeout = { navController.navigateTopLevel(PocoRoutes.LOGIN, PocoRoutes.SPLASH) }
            )
        }
        composable(PocoRoutes.LOGIN) {
            LoginScreen(
                onLoginClick = { navController.navigate(PocoRoutes.LOGIN_FORM) },
                onSignUpClick = { navController.navigate(PocoRoutes.SIGN_UP) }
            )
        }
        composable(PocoRoutes.LOGIN_FORM) {
            val context = LocalContext.current
            val locationStore = remember(context) { LocationStore(context) }
            val scope = rememberCoroutineScope()
            var errorMessage by remember { mutableStateOf<String?>(null) }
            LoginFormScreen(
                onBack = { navController.popBackStack() },
                errorMessage = errorMessage,
                onLoginComplete = { email, password ->
                    scope.launch {
                        runCatching { ServerApiClient.api.login(LoginRequest(email, password)) }
                            .onSuccess { response ->
                                val user = response.user
                                if (!response.success || user == null) {
                                    errorMessage = response.message ?: "이메일 또는 비밀번호를 확인해주세요"
                                    return@onSuccess
                                }
                                errorMessage = null
                                locationStore.saveSession(user.id, user.role, user.name, user.email, user.joinedAt)
                                runCatching { registerDeviceIfUser(locationStore, user.id, user.role) }
                                val homeRoute = if (user.role == 0) PocoRoutes.USER_HOME else PocoRoutes.GUARDIAN_HOME
                                navController.navigateTopLevel(homeRoute, PocoRoutes.LOGIN)
                            }
                            .onFailure { errorMessage = "로그인에 실패했어요. 잠시 후 다시 시도해주세요" }
                    }
                }
            )
        }
        composable(PocoRoutes.SIGN_UP) {
            SignUpScreen(
                onBack = { navController.popBackStack() },
                onSignUpComplete = { name, email, phoneNumber, password ->
                    pendingSignUp = PendingSignUp(name, email, phoneNumber, password)
                    navController.navigate(PocoRoutes.ROLE_SELECT)
                }
            )
        }
        composable(PocoRoutes.ROLE_SELECT) {
            val context = LocalContext.current
            val locationStore = remember(context) { LocationStore(context) }
            val scope = rememberCoroutineScope()

            var isSubmitting by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            fun completeSignUp(role: Int, onDone: () -> Unit) {
                val pending = pendingSignUp ?: return
                if (isSubmitting) return
                isSubmitting = true
                errorMessage = null
                scope.launch {
                    runCatching {
                        val user = ServerApiClient.api.signup(
                            SignUpRequest(
                                name = pending.name,
                                email = pending.email,
                                password = pending.password,
                                phoneNumber = pending.phoneNumber,
                                role = role
                            )
                        )
                        locationStore.saveSession(user.id, user.role, user.name, user.email, user.joinedAt)
                        runCatching { registerDeviceIfUser(locationStore, user.id, user.role) }
                    }.onSuccess {
                        pendingSignUp = null
                        onDone()
                    }.onFailure { throwable ->
                        isSubmitting = false
                        errorMessage = signUpErrorMessage(throwable)
                    }
                }
            }

            RoleSelectScreen(
                onSelectUser = { completeSignUp(role = 0) { navController.navigate(PocoRoutes.QR_SHOW) } },
                onSelectGuardian = { completeSignUp(role = 1) { navController.navigate(PocoRoutes.QR_SCAN) } },
                enabled = !isSubmitting,
                errorMessage = errorMessage
            )
        }
        composable(PocoRoutes.QR_SHOW) {
            val context = LocalContext.current
            val locationStore = remember(context) { LocationStore(context) }
            var code by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(Unit) {
                val userId = locationStore.currentUserId() ?: return@LaunchedEffect
                runCatching { ServerApiClient.api.issueLinkCode(userId) }
                    .onSuccess { code = it.code }
            }
            QrShowScreen(
                code = code,
                onDone = { navController.navigateTopLevel(PocoRoutes.USER_HOME, PocoRoutes.LOGIN) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(PocoRoutes.QR_SCAN) {
            QrScanScreen(
                onScanned = { code ->
                    pendingLinkCode = code
                    navController.navigate(PocoRoutes.RELATION_SELECT)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(PocoRoutes.RELATION_SELECT) {
            val context = LocalContext.current
            val locationStore = remember(context) { LocationStore(context) }
            val scope = rememberCoroutineScope()
            RelationSelectScreen(
                onComplete = { relationLabel ->
                    val code = pendingLinkCode
                    val guardianId = locationStore.currentUserId()
                    if (code == null || guardianId == null) {
                        navController.navigateTopLevel(PocoRoutes.GUARDIAN_HOME, PocoRoutes.LOGIN)
                        return@RelationSelectScreen
                    }
                    scope.launch {
                        runCatching {
                            ServerApiClient.api.redeemLinkCode(
                                RedeemLinkRequest(code = code, guardianId = guardianId, relationLabel = relationLabel)
                            )
                        }
                        pendingLinkCode = null
                        navController.navigateTopLevel(PocoRoutes.GUARDIAN_HOME, PocoRoutes.LOGIN)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // 사용자 모드
        composable(PocoRoutes.USER_HOME) {
            UserHomeScreen(
                uiState = homeUiState,
                selectedTab = AppTab.HOME,
                onSaveCurrentLocationAsHome = onSaveCurrentLocationAsHome,
                onTabSelected = { tab -> navController.navigateUserTab(tab) },
                onSosClick = { navController.navigate(PocoRoutes.EMERGENCY_USER) }
            )
        }
        composable(PocoRoutes.USER_ACTIVITY) {
            val context = LocalContext.current
            val locationStore = remember(context) { LocationStore(context) }
            var items by remember { mutableStateOf(emptyList<ActivityLogItem>()) }
            LaunchedEffect(Unit) {
                val deviceId = locationStore.backendDeviceId()
                if (deviceId == null) return@LaunchedEffect
                val startOfToday = run {
                    val cal = java.util.Calendar.getInstance()
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    cal.set(java.util.Calendar.SECOND, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }
                val endOfToday = startOfToday + 24 * 60 * 60 * 1000L

                // sound_events는 아무 로직 없이 찍히는 원본 로그라 "활동 기록"에는 안 맞아서 뺌.
                // behavior_sessions만 사용 (세션 로직을 거쳐 확정된 것만 여기 들어옴), 그중 오늘 것만 필터링.
                items = runCatching { ServerApiClient.api.getBehaviorSessions(deviceId) }.getOrDefault(emptyList())
                    .filter { (it.startTime.fromServerDateTime() ?: 0L) in startOfToday until endOfToday }
                    .sortedByDescending { it.startTime.fromServerDateTime() ?: 0L }
                    .map { it.toActivityLogItem() }
            }
            ActivityLogScreen(
                selectedTab = AppTab.ANALYSIS,
                onTabSelected = { tab -> navController.navigateUserTab(tab) },
                items = items
            )
        }
        composable(PocoRoutes.USER_SETTINGS) {
            SettingsScreen(
                selectedTab = AppTab.SETTINGS,
                onTabSelected = { tab -> navController.navigateUserTab(tab) },
                onOpenMicSensitivity = { navController.navigate(PocoRoutes.MIC_SENSITIVITY) },
                onOpenNotificationSettings = { navController.navigate(PocoRoutes.NOTIFICATION_SETTINGS) },
                onOpenGuardianLinkManagement = { navController.navigate(PocoRoutes.GUARDIAN_LINK_MANAGEMENT) },
                onOpenAccountInfo = { navController.navigate(PocoRoutes.ACCOUNT_INFO) }
            )
        }
        composable(PocoRoutes.MIC_SENSITIVITY) {
            MicSensitivityScreen(onBack = { navController.popBackStack() })
        }
        composable(PocoRoutes.NOTIFICATION_SETTINGS) {
            NotificationSettingsRoute(onBack = { navController.popBackStack() })
        }
        composable(PocoRoutes.ACCOUNT_INFO) {
            val context = LocalContext.current
            val locationStore = remember(context) { LocationStore(context) }
            var linkedGuardianCountLabel by remember { mutableStateOf("-") }
            LaunchedEffect(Unit) {
                val userId = locationStore.currentUserId() ?: return@LaunchedEffect
                linkedGuardianCountLabel = runCatching { ServerApiClient.api.getMyLinks(userId, "USER") }
                    .getOrNull()
                    ?.let { "${it.size}명" } ?: "-"
            }
            AccountInfoScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    locationStore.clearSession()
                    navController.navigateTopLevel(PocoRoutes.LOGIN, PocoRoutes.USER_HOME)
                },
                name = locationStore.currentUserName() ?: "회원",
                email = locationStore.currentUserEmail() ?: "-",
                joinedAtLabel = locationStore.currentUserJoinedAt().fromServerDateTime()?.let { joinedDateFormatter.format(it) } ?: "-",
                roleLabel = "사용자",
                linkedGuardianCountLabel = linkedGuardianCountLabel
            )
        }
        composable(PocoRoutes.GUARDIAN_LINK_MANAGEMENT) {
            val context = LocalContext.current
            val locationStore = remember(context) { LocationStore(context) }
            val scope = rememberCoroutineScope()
            var guardians by remember { mutableStateOf(emptyList<LinkedPerson>()) }
            LaunchedEffect(Unit) {
                val userId = locationStore.currentUserId() ?: return@LaunchedEffect
                guardians = runCatching { ServerApiClient.api.getMyLinks(userId, "USER") }
                    .getOrDefault(emptyList())
                    .map { it.toLinkedPerson(role = LinkedPersonRole.GUARDIAN) }
            }
            GuardianLinkManagementScreen(
                onBack = { navController.popBackStack() },
                onInviteGuardian = { navController.navigate(PocoRoutes.QR_SHOW) },
                guardians = guardians,
                onUnlink = { person ->
                    guardians = guardians.filterNot { it.linkId == person.linkId }
                    scope.launch {
                        person.linkId.toLongOrNull()?.let { linkId ->
                            runCatching { ServerApiClient.api.deleteLink(linkId) }
                        }
                    }
                }
            )
        }

        // 긴급 상황
        composable(PocoRoutes.EMERGENCY_USER) {
            EmergencyUserScreen(
                onSafe = { navController.popBackStack() },
                onSos = { navController.navigate(PocoRoutes.EMERGENCY_GUARDIAN) }
            )
        }
        composable(PocoRoutes.EMERGENCY_LOCATION) {
            val context = LocalContext.current
            EmergencyLocationScreen(
                onBack = { navController.popBackStack() },
                onOpenInMaps = {
                    val geoUri = Uri.parse("geo:0,0?q=${Uri.encode(LINKED_USER_LAST_LOCATION_QUERY)}")
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
                    } catch (e: ActivityNotFoundException) {
                        val webUri = Uri.parse("https://maps.google.com/?q=${Uri.encode(LINKED_USER_LAST_LOCATION_QUERY)}")
                        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                    }
                }
            )
        }
        composable(PocoRoutes.EMERGENCY_DISPATCHED) {
            EmergencyDispatchedScreen(
                onDone = { navController.navigateTopLevel(PocoRoutes.GUARDIAN_HOME, PocoRoutes.EMERGENCY_GUARDIAN) }
            )
        }

        // 보호자 모드
        composable(PocoRoutes.GUARDIAN_HOME) {
            val context = LocalContext.current
            val locationStore = remember(context) { LocationStore(context) }
            var monitoredUserLabel by remember { mutableStateOf("연동된 사용자") }
            var micLabel by remember { mutableStateOf("-") }
            var gpsLabel by remember { mutableStateOf("-") }
            var batteryLabel by remember { mutableStateOf("-") }
            var mealCountLabel by remember { mutableStateOf("-") }
            var outingLabel by remember { mutableStateOf("-") }
            var cognitiveDurationLabel by remember { mutableStateOf("-") }
            var sleepDurationLabel by remember { mutableStateOf("-") }
            var wakeTimelineEntry by remember { mutableStateOf(TimelineEntry("-", "기상 정보 없음")) }
            var latestAlert by remember { mutableStateOf<TimelineEntry?>(null) }
            var recentTimeline by remember { mutableStateOf(emptyList<TimelineEntry>()) }
            LaunchedEffect(Unit) {
                val monitored = runCatching { resolveMonitoredUser(locationStore) }.getOrNull() ?: return@LaunchedEffect
                monitoredUserLabel = monitored.name?.let { "${it}님" } ?: monitored.relationLabel
                micLabel = if (monitored.device.micOn) "ON" else "OFF"
                gpsLabel = if (monitored.device.gpsOn == true) "ON" else "OFF"
                batteryLabel = monitored.device.batteryPercent?.let { "$it%" } ?: "-"

                val deviceId = monitored.device.id
                val sessions = runCatching { ServerApiClient.api.getBehaviorSessions(deviceId) }.getOrDefault(emptyList())
                val alerts = runCatching { ServerApiClient.api.getDangerAlerts(deviceId) }.getOrDefault(emptyList())
                val sleepWakeEvents = runCatching { ServerApiClient.api.getSleepWakeEvents(deviceId) }.getOrDefault(emptyList())
                val outingEvents = runCatching { ServerApiClient.api.getOutingEvents(deviceId) }.getOrDefault(emptyList())

                val todaySessions = sessions.filter { it.startTime.fromServerDateTime().isToday() }
                mealCountLabel = "${todaySessions.count { it.behavior == "meal" }}회"
                cognitiveDurationLabel = todaySessions
                    .filter { it.behavior == "cognitive" }
                    .sumOf { session ->
                        val start = session.startTime.fromServerDateTime()
                        val end = (session.endTime ?: session.confirmedTime).fromServerDateTime()
                        if (start != null && end != null && end > start) end - start else 0L
                    }
                    .let { totalMs -> "${totalMs / 60_000L}분" } // 활동이 없으면 "-"가 아니라 "0분"으로 (식사 횟수 "0회"와 같은 표현 방식)
                outingLabel = if (outingEvents.any {
                        it.transitionType.equals("HOME_TO_OUTSIDE", ignoreCase = true) && it.timestamp.fromServerDateTime().isToday()
                    }) "다녀옴" else "외출 없음"

                // "오늘의 활동 요약"·"오늘의 일간 타임라인"이라 이름 붙은 만큼, 다른 지표들처럼 오늘 것만 써야 한다.
                // 단, 수면은 취침(전날 밤)·기상(당일 아침)의 날짜가 다르므로 sleepWakeEvents는 여기서
                // 미리 today로 거르지 않고 원본 그대로 totalSleepDurationLabel()에 넘긴다(그 안에서 기상일 기준으로 처리).
                val todayAlerts = alerts.filter { it.detectedAt.fromServerDateTime().isToday() }
                val todayWakeEvents = sleepWakeEvents.filter {
                    it.eventType.equals("wake", ignoreCase = true) && it.timestamp.fromServerDateTime().isToday()
                }

                sleepDurationLabel = sleepWakeEvents.totalSleepDurationLabel()
                todayWakeEvents
                    .maxByOrNull { it.timestamp.fromServerDateTime() ?: 0L }
                    ?.let { wakeTimelineEntry = it.toTimelineEntry() }

                latestAlert = alerts.maxByOrNull { it.detectedAt.fromServerDateTime() ?: 0L }?.toTimelineEntry()
                // 기상은 위에서 이미 wakeTimelineEntry로 별도 표시하므로, 여기 목록에 또 넣으면 중복 표시된다.
                recentTimeline = (todayAlerts.map { it.toTimelineEntry() } +
                    todaySessions.map { it.toTimelineEntry() }).take(5)
            }
            GuardianHomeScreen(
                selectedTab = GuardianTab.HOME,
                onTabSelected = { tab -> navController.navigateGuardianTab(tab) },
                onOpenNotifications = { navController.navigate(PocoRoutes.GUARDIAN_ALERTS) },
                monitoredUserLabel = monitoredUserLabel,
                mealCountLabel = mealCountLabel,
                outingLabel = outingLabel,
                cognitiveDurationLabel = cognitiveDurationLabel,
                micLabel = micLabel,
                gpsLabel = gpsLabel,
                batteryLabel = batteryLabel,
                latestAlert = latestAlert,
                sleepDurationLabel = sleepDurationLabel,
                wakeTimelineEntry = wakeTimelineEntry,
                recentTimeline = recentTimeline
            )
        }
        composable(PocoRoutes.GUARDIAN_TIMELINE) {
            val context = LocalContext.current
            val locationStore = remember(context) { LocationStore(context) }
            var timeline by remember { mutableStateOf(emptyList<TimelineEntry>()) }
            var sleepDurationLabel by remember { mutableStateOf("-") }
            LaunchedEffect(Unit) {
                val deviceId = runCatching { resolveMonitoredUser(locationStore) }.getOrNull()?.device?.id ?: return@LaunchedEffect
                val sessions = runCatching { ServerApiClient.api.getBehaviorSessions(deviceId) }.getOrDefault(emptyList())
                val alerts = runCatching { ServerApiClient.api.getDangerAlerts(deviceId) }.getOrDefault(emptyList())
                val sleepWakeEvents = runCatching { ServerApiClient.api.getSleepWakeEvents(deviceId) }.getOrDefault(emptyList())
                // 화면 제목이 "오늘의 모니터링"이라 전부 오늘 것만 걸러서 써야 한다 — 안 그러면(예: 수면 시간)
                // 여러 날짜의 기록이 합쳐져서 "112시간" 같은 값이 나온다.
                val todaySessions = sessions.filter { it.startTime.fromServerDateTime().isToday() }
                val todayAlerts = alerts.filter { it.detectedAt.fromServerDateTime().isToday() }
                val todaySleepWakeEvents = sleepWakeEvents.filter { it.timestamp.fromServerDateTime().isToday() }
                // 참고: danger-alerts 는 실제 발생 시각(epoch)이 있지만 behavior-sessions 는 세션 내 상대 초(startSec)만 있어서
                // 두 시간 기준이 달라 정확히 하나의 시간순으로 병합할 수는 없음 -> 위험 알림을 먼저, 그 아래 행동 세션을 보여줌.
                timeline = todayAlerts.map { it.toTimelineEntry() } +
                    todaySessions.map { it.toTimelineEntry() } +
                    todaySleepWakeEvents.map { it.toTimelineEntry() }
                // 수면 시간은 취침(전날 밤)·기상(당일 아침) 날짜가 달라서 today로 미리 거른 리스트가 아니라
                // 원본 sleepWakeEvents를 넘겨야 한다(그 안에서 기상일 기준으로 페어를 골라낸다).
                sleepDurationLabel = sleepWakeEvents.totalSleepDurationLabel()
            }
            GuardianDailyMonitoringScreen(
                selectedTab = GuardianTab.TIMELINE,
                onTabSelected = { tab -> navController.navigateGuardianTab(tab) },
                timeline = timeline,
                sleepDurationLabel = sleepDurationLabel
            )
        }
        composable(PocoRoutes.GUARDIAN_TREND) {
            val context = LocalContext.current
            val locationStore = remember(context) { LocationStore(context) }
            var hourlyRhythm by remember { mutableStateOf(List(24) { 0 }) }
            var trendState by remember { mutableStateOf<TrendUiState>(TrendUiState.Loading) }
            LaunchedEffect(Unit) {
                val monitoredResult = runCatching { resolveMonitoredUser(locationStore) }
                val deviceId = monitoredResult.getOrNull()?.device?.id
                if (deviceId == null) {
                    if (monitoredResult.isFailure) {
                        trendState = TrendUiState.Error("서버에 연결할 수 없어요. 잠시 후 다시 시도해주세요.")
                    }
                    return@LaunchedEffect
                }

                // behavior_sessions·outing_events·sleep_wake_events·danger_alerts를 병렬로 조회.
                // 전부 성공해야만 화면을 보여주는 구조는 피한다 — 각 API의 성공/빈 데이터/실패를 그대로
                // TrendUiStateMapper(순수 함수)에 넘겨서 emptyList로 뭉개지 않고 구분되게 한다.
                val sessionsDeferred = async { runCatching { ServerApiClient.api.getBehaviorSessions(deviceId) } }
                val outingsDeferred = async { runCatching { ServerApiClient.api.getOutingEvents(deviceId) } }
                val sleepDeferred = async { runCatching { ServerApiClient.api.getSleepWakeEvents(deviceId) } }
                val dangerDeferred = async { runCatching { ServerApiClient.api.getDangerAlerts(deviceId) } }

                val sessionsResult = sessionsDeferred.await()
                val outingsResult = outingsDeferred.await()
                val sleepResult = sleepDeferred.await()
                val dangerResult = dangerDeferred.await()

                hourlyRhythm = sessionsResult.getOrDefault(emptyList()).hourlyRhythmToday()
                trendState = TrendUiStateMapper.resolve(sessionsResult, outingsResult, sleepResult, dangerResult, System.currentTimeMillis())
            }
            GuardianTrendScreen(
                selectedTab = GuardianTab.TREND,
                onTabSelected = { tab -> navController.navigateGuardianTab(tab) },
                hourlyRhythm = hourlyRhythm,
                trendState = trendState
            )
        }
        composable(PocoRoutes.GUARDIAN_ALERTS) {
            val context = LocalContext.current
            val locationStore = remember(context) { LocationStore(context) }
            var anomalies by remember { mutableStateOf(emptyList<AnomalyAlert>()) }
            var generalNotices by remember { mutableStateOf(emptyList<GeneralNotice>()) }
            LaunchedEffect(Unit) {
                val deviceId = runCatching { resolveMonitoredUser(locationStore) }.getOrNull()?.device?.id ?: return@LaunchedEffect

                // 일반 알림: 조회만 하면 서버가 오늘자 배터리부족/일일요약을 규칙으로 채워서 돌려준다(NoticeService 참고).
                runCatching { ServerApiClient.api.getNotices(deviceId) }
                    .onSuccess { notices -> generalNotices = notices.map { it.toGeneralNotice() } }

                val dangerAlertsList = runCatching { ServerApiClient.api.getDangerAlerts(deviceId) }.getOrDefault(emptyList())
                val existingAnomalyAlerts = runCatching { ServerApiClient.api.getAlerts(deviceId) }.getOrDefault(emptyList())

                // 이상탐지: 이미 만들어둔 장기추세 분석 엔진(TrendAggregator)의 판정을 그대로 재사용해서
                // 기준(20% 이상 변화 + 최소 유효기록일)을 통과하면 이상탐지 알림을 생성한다.
                // 같은 날 같은 유형을 중복 생성하지 않도록 오늘자 기존 알림 유형을 먼저 확인한다.
                val today = LocalDate.now()
                val existingTypesToday = existingAnomalyAlerts
                    .filter { it.time.fromServerDateTime()?.let { ms -> Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate() } == today }
                    .mapNotNull { it.type }
                    .toSet()

                val sessions = runCatching { ServerApiClient.api.getBehaviorSessions(deviceId) }.getOrDefault(emptyList())
                val outings = runCatching { ServerApiClient.api.getOutingEvents(deviceId) }.getOrDefault(emptyList())
                val sleepEvents = runCatching { ServerApiClient.api.getSleepWakeEvents(deviceId) }.getOrDefault(emptyList())
                val analysis = TrendAggregator.analyze(TrendPeriod.WEEK, sessions, outings, sleepEvents, dangerAlertsList)

                val newAlerts = mutableListOf<Pair<String, String>>() // type to evidence
                if ("OUTING_DECREASE" !in existingTypesToday &&
                    analysis.outing.change.status == ChangeStatus.NORMAL &&
                    (analysis.outing.change.changePercent ?: 0.0) <= -20.0
                ) {
                    newAlerts += "OUTING_DECREASE" to "이전 기간 대비 외출 빈도 %.0f%% 감소".format(-(analysis.outing.change.changePercent ?: 0.0))
                }
                if ("MEAL_IRREGULAR" !in existingTypesToday && analysis.meal.regularityLevel == RegularityLevel.HIGHLY_VARIABLE) {
                    newAlerts += "MEAL_IRREGULAR" to "최근 식사 시작 시각 표준편차 %.0f분으로 변동이 커요".format(analysis.meal.startTimeStdDevMinutes ?: 0.0)
                }
                if ("COGNITIVE_DECREASE" !in existingTypesToday &&
                    analysis.media.change.status == ChangeStatus.NORMAL &&
                    (analysis.media.change.changePercent ?: 0.0) <= -20.0
                ) {
                    newAlerts += "COGNITIVE_DECREASE" to "이전 기간 대비 대화·미디어 활동 %.0f%% 감소".format(-(analysis.media.change.changePercent ?: 0.0))
                }

                val createdAlerts = newAlerts.mapNotNull { (type, evidence) ->
                    runCatching {
                        ServerApiClient.api.createAlert(
                            AlertRequest(deviceId = deviceId, type = type, time = System.currentTimeMillis().toServerDateTime(), evidence = evidence)
                        )
                    }.getOrNull()
                }

                // danger_alerts(비명·경적)는 "위험 알림"이라 홈 화면 주의 배너·타임라인에서 이미 보여준다.
                // 여기(이상탐지)엔 진짜 이상탐지 alerts 테이블 것만 넣는다 — 안 그러면 anomalyTypeFor()의
                // fallback 때문에 "비명 감지"까지 전부 "대화·인지 자극 감소"로 오분류돼 보인다.
                anomalies = (existingAnomalyAlerts.map { it.toAnomalyAlert() } +
                    createdAlerts.map { it.toAnomalyAlert() })
            }
            NotificationCenterScreen(
                selectedTab = GuardianTab.ALERTS,
                onTabSelected = { tab -> navController.navigateGuardianTab(tab) },
                anomalies = anomalies,
                generalNotices = generalNotices
            )
        }
        composable(PocoRoutes.GUARDIAN_SETTINGS) {
            val context = LocalContext.current
            val locationStore = remember(context) { LocationStore(context) }
            GuardianSettingsScreen(
                selectedTab = GuardianTab.SETTINGS,
                onTabSelected = { tab -> navController.navigateGuardianTab(tab) },
                onOpenUserLinkInfo = { navController.navigate(PocoRoutes.GUARDIAN_USER_LINK_INFO) },
                onOpenNotificationSettings = { navController.navigate(PocoRoutes.GUARDIAN_NOTIFICATION_SETTINGS) },
                onOpenLinkNewUser = { navController.navigate(PocoRoutes.GUARDIAN_LINK_NEW_USER) },
                onLogout = {
                    locationStore.clearSession()
                    navController.navigateTopLevel(PocoRoutes.LOGIN, PocoRoutes.GUARDIAN_HOME)
                }
            )
        }
        composable(PocoRoutes.GUARDIAN_USER_LINK_INFO) {
            val context = LocalContext.current
            val locationStore = remember(context) { LocationStore(context) }
            val scope = rememberCoroutineScope()
            var linkedUsers by remember { mutableStateOf(emptyList<LinkedPerson>()) }
            LaunchedEffect(Unit) {
                val userId = locationStore.currentUserId() ?: return@LaunchedEffect
                linkedUsers = runCatching { ServerApiClient.api.getMyLinks(userId, "GUARDIAN") }
                    .getOrDefault(emptyList())
                    .map { it.toLinkedPerson(role = LinkedPersonRole.USER) }
            }
            GuardianUserLinkInfoScreen(
                onBack = { navController.popBackStack() },
                linkedUsers = linkedUsers,
                onUnlink = { person ->
                    linkedUsers = linkedUsers.filterNot { it.linkId == person.linkId }
                    scope.launch {
                        person.linkId.toLongOrNull()?.let { linkId ->
                            runCatching { ServerApiClient.api.deleteLink(linkId) }
                        }
                    }
                    navController.popBackStack()
                }
            )
        }
        composable(PocoRoutes.GUARDIAN_NOTIFICATION_SETTINGS) {
            NotificationSettingsRoute(onBack = { navController.popBackStack() })
        }
        composable(PocoRoutes.GUARDIAN_LINK_NEW_USER) {
            val context = LocalContext.current
            val locationStore = remember(context) { LocationStore(context) }
            val scope = rememberCoroutineScope()
            var scannedCode by remember { mutableStateOf<String?>(null) }
            val code = scannedCode
            if (code != null) {
                RelationSelectScreen(
                    onComplete = { relationLabel ->
                        val guardianId = locationStore.currentUserId()
                        if (guardianId != null) {
                            scope.launch {
                                runCatching {
                                    ServerApiClient.api.redeemLinkCode(
                                        RedeemLinkRequest(code = code, guardianId = guardianId, relationLabel = relationLabel)
                                    )
                                }
                                navController.popBackStack()
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                    onBack = { scannedCode = null }
                )
            } else {
                QrScanScreen(
                    onScanned = { scannedCode = it },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // 응급 상황
        composable(PocoRoutes.EMERGENCY_GUARDIAN) {
            val context = LocalContext.current
            val locationStore = remember(context) { LocationStore(context) }
            val scope = rememberCoroutineScope()
            var targetDeviceId by remember { mutableStateOf<Long?>(null) }
            LaunchedEffect(Unit) {
                val userId = locationStore.currentUserId() ?: return@LaunchedEffect
                val role = locationStore.currentUserRole()
                targetDeviceId = if (role == 0) {
                    locationStore.backendDeviceId()
                } else {
                    runCatching { ServerApiClient.api.getMyLinks(userId, "GUARDIAN") }
                        .getOrDefault(emptyList())
                        .firstOrNull()
                        ?.userId
                        ?.let { patientUserId ->
                            runCatching { ServerApiClient.api.getDeviceByUserId(patientUserId) }.getOrNull()?.id
                        }
                }
            }
            EmergencyGuardianScreen(
                onCheckLocation = { navController.navigate(PocoRoutes.EMERGENCY_LOCATION) },
                onDispatch = {
                    val deviceId = targetDeviceId
                    val dispatchedBy = locationStore.currentUserId()
                    if (deviceId != null && dispatchedBy != null) {
                        scope.launch {
                            runCatching {
                                ServerApiClient.api.dispatchEmergency(EmergencyDispatchRequest(deviceId, dispatchedBy))
                            }
                            navController.navigate(PocoRoutes.EMERGENCY_DISPATCHED)
                        }
                    } else {
                        navController.navigate(PocoRoutes.EMERGENCY_DISPATCHED)
                    }
                },
                onCall = {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$LINKED_USER_PHONE_NUMBER")))
                }
            )
        }
    }
}

/** 알림 설정 화면 공통 래퍼 — 화면 자체는 dumb component라 여기서 실제 조회/저장 API를 붙인다.
 *  서버 필드 순서(emergencyAlert, activityAnomalyAlert, lowBatteryAlert, dailySummaryAlert)와
 *  화면의 toggleItems 순서가 반드시 같아야 한다. */
@Composable
private fun NotificationSettingsRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val locationStore = remember(context) { LocationStore(context) }
    val scope = rememberCoroutineScope()
    var userId by remember { mutableStateOf<Long?>(null) }
    var values by remember { mutableStateOf(listOf(true, true, true, false)) }
    LaunchedEffect(Unit) {
        val id = locationStore.currentUserId() ?: return@LaunchedEffect
        userId = id
        runCatching { ServerApiClient.api.getNotificationSettings(id) }
            .onSuccess { settings ->
                values = listOf(
                    settings.emergencyAlert ?: true,
                    settings.activityAnomalyAlert ?: true,
                    settings.lowBatteryAlert ?: true,
                    settings.dailySummaryAlert ?: false
                )
            }
    }
    NotificationSettingsScreen(
        onBack = onBack,
        values = values,
        onToggle = { index, value ->
            val updated = values.toMutableList().also { it[index] = value }
            values = updated
            val id = userId ?: return@NotificationSettingsScreen
            scope.launch {
                runCatching {
                    ServerApiClient.api.updateNotificationSettings(
                        NotificationSettingsRequest(
                            userId = id,
                            emergencyAlert = updated[0],
                            activityAnomalyAlert = updated[1],
                            lowBatteryAlert = updated[2],
                            dailySummaryAlert = updated[3]
                        )
                    )
                }
            }
        }
    )
}

/** UserLinkResponse에는 상대방 이름이 없어서(백엔드에 유저 조회 API가 아직 없음) 임시로 관계 라벨만 보여준다. */
private fun UserLinkResponse.toLinkedPerson(role: LinkedPersonRole): LinkedPerson = LinkedPerson(
    linkId = id.toString(),
    name = relationLabel ?: "연동된 사용자",
    relationLabel = relationLabel ?: "-",
    linkedAt = linkedAt?.fromServerDateTime()?.let { timeFormatter.format(it) } ?: "-",
    role = role
)

private fun NavHostController.navigateUserTab(tab: AppTab) {
    val route = when (tab) {
        AppTab.HOME -> PocoRoutes.USER_HOME
        AppTab.ANALYSIS -> PocoRoutes.USER_ACTIVITY
        AppTab.SETTINGS -> PocoRoutes.USER_SETTINGS
    }
    navigate(route) {
        popUpTo(PocoRoutes.USER_HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.navigateGuardianTab(tab: GuardianTab) {
    val route = when (tab) {
        GuardianTab.HOME -> PocoRoutes.GUARDIAN_HOME
        GuardianTab.TIMELINE -> PocoRoutes.GUARDIAN_TIMELINE
        GuardianTab.TREND -> PocoRoutes.GUARDIAN_TREND
        GuardianTab.ALERTS -> PocoRoutes.GUARDIAN_ALERTS
        GuardianTab.SETTINGS -> PocoRoutes.GUARDIAN_SETTINGS
    }
    navigate(route) {
        popUpTo(PocoRoutes.GUARDIAN_HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}