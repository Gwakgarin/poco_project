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
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.poco.BehaviorSessionResponse
import com.example.poco.DangerAlertResponse
import com.example.poco.ServerApiClient
import com.example.poco.SoundEventResponse
import com.example.poco.location.LocationStore
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
import com.example.poco.ui.screens.LoginScreen
import com.example.poco.ui.screens.MicSensitivityScreen
import com.example.poco.ui.screens.NotificationCenterScreen
import com.example.poco.ui.screens.NotificationSettingsScreen
import com.example.poco.ui.screens.QrScanScreen
import com.example.poco.ui.screens.QrShowScreen
import com.example.poco.ui.screens.RoleSelectScreen
import com.example.poco.ui.screens.SettingsScreen
import com.example.poco.ui.screens.SignUpScreen
import com.example.poco.ui.screens.SplashScreen
import com.example.poco.ui.screens.TimelineEntry
import com.example.poco.ui.screens.UserHomeScreen
import com.example.poco.ui.screens.UserHomeUiState
import java.text.SimpleDateFormat
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
    val time = startTime?.let { timeFormatter.format(it) } ?: "${startSec ?: 0}s~${endSec ?: 0}s"
    return ActivityLogItem(time = time, label = label, icon = iconForEvent(behavior ?: representativeEvent))
}

private fun SoundEventResponse.toActivityLogItem() = ActivityLogItem(
    time = "${startSec}s~${endSec}s",
    label = smoothedLabel.ifBlank { predLabel },
    icon = iconForEvent(smoothedLabel.ifBlank { predLabel })
)

private fun BehaviorSessionResponse.toTimelineEntry(): TimelineEntry {
    val label = behaviorLabel(behavior).let { if (behavior != null) "$it 감지" else it }
    val time = startTime?.let { timeFormatter.format(it) } ?: "${startSec ?: 0}s~${endSec ?: 0}s"
    return TimelineEntry(time = time, label = label, isRisk = false)
}

private fun DangerAlertResponse.toTimelineEntry() = TimelineEntry(
    time = detectedAtEpochMs?.let { timeFormatter.format(it) } ?: "-",
    label = reason ?: soundLabel ?: "위험 신호 감지",
    isRisk = true
)

private fun DangerAlertResponse.toAnomalyAlert() = AnomalyAlert(
    time = detectedAtEpochMs?.let { timeFormatter.format(it) } ?: "-",
    type = anomalyTypeFor(reason, soundLabel),
    evidence = reason ?: soundLabel ?: "위험 신호가 감지됐어요"
)

object PocoRoutes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val SIGN_UP = "sign_up"
    const val ROLE_SELECT = "role_select"
    const val QR_SHOW = "qr_show"
    const val QR_SCAN = "qr_scan"
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
                onLoginClick = { navController.navigate(PocoRoutes.ROLE_SELECT) },
                onSignUpClick = { navController.navigate(PocoRoutes.SIGN_UP) }
            )
        }
        composable(PocoRoutes.SIGN_UP) {
            SignUpScreen(
                onBack = { navController.popBackStack() },
                onSignUpComplete = { navController.navigate(PocoRoutes.ROLE_SELECT) }
            )
        }
        composable(PocoRoutes.ROLE_SELECT) {
            RoleSelectScreen(
                onSelectUser = { navController.navigate(PocoRoutes.QR_SHOW) },
                onSelectGuardian = { navController.navigate(PocoRoutes.QR_SCAN) }
            )
        }
        composable(PocoRoutes.QR_SHOW) {
            QrShowScreen(
                onDone = { navController.navigateTopLevel(PocoRoutes.USER_HOME, PocoRoutes.LOGIN) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(PocoRoutes.QR_SCAN) {
            QrScanScreen(
                onScanned = { navController.navigateTopLevel(PocoRoutes.GUARDIAN_HOME, PocoRoutes.LOGIN) },
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
            var items by remember { mutableStateOf(emptyList<ActivityLogItem>()) }
            LaunchedEffect(Unit) {
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
                items = runCatching { ServerApiClient.api.getBehaviorSessions() }.getOrDefault(emptyList())
                    .filter { (it.startTime ?: 0L) in startOfToday until endOfToday }
                    .sortedByDescending { it.startTime ?: 0L }
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
                onOpenAccountInfo = { navController.navigate(PocoRoutes.ACCOUNT_INFO) },
                onOpenGuardianDemo = { navController.navigate(PocoRoutes.GUARDIAN_HOME) },
                onOpenEmergencyDemo = { navController.navigate(PocoRoutes.EMERGENCY_USER) }
            )
        }
        composable(PocoRoutes.MIC_SENSITIVITY) {
            MicSensitivityScreen(onBack = { navController.popBackStack() })
        }
        composable(PocoRoutes.NOTIFICATION_SETTINGS) {
            NotificationSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(PocoRoutes.ACCOUNT_INFO) {
            AccountInfoScreen(
                onBack = { navController.popBackStack() },
                onLogout = { navController.navigateTopLevel(PocoRoutes.LOGIN, PocoRoutes.USER_HOME) }
            )
        }
        composable(PocoRoutes.GUARDIAN_LINK_MANAGEMENT) {
            GuardianLinkManagementScreen(
                onBack = { navController.popBackStack() },
                onInviteGuardian = { navController.navigate(PocoRoutes.QR_SHOW) }
            )
        }

        // 긴급 상황
        composable(PocoRoutes.EMERGENCY_USER) {
            EmergencyUserScreen(
                onSafe = { navController.popBackStack() },
                onSos = { navController.navigate(PocoRoutes.EMERGENCY_GUARDIAN) }
            )
        }
        composable(PocoRoutes.EMERGENCY_GUARDIAN) {
            val context = LocalContext.current
            EmergencyGuardianScreen(
                onCheckLocation = { navController.navigate(PocoRoutes.EMERGENCY_LOCATION) },
                onDispatch = { navController.navigate(PocoRoutes.EMERGENCY_DISPATCHED) },
                onCall = {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$LINKED_USER_PHONE_NUMBER")))
                }
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
            GuardianHomeScreen(
                selectedTab = GuardianTab.HOME,
                onTabSelected = { tab -> navController.navigateGuardianTab(tab) },
                onOpenNotifications = { navController.navigate(PocoRoutes.GUARDIAN_ALERTS) }
            )
        }
        composable(PocoRoutes.GUARDIAN_TIMELINE) {
            val context = LocalContext.current
            val locationStore = remember(context) { LocationStore(context) }
            var timeline by remember { mutableStateOf(emptyList<TimelineEntry>()) }
            LaunchedEffect(Unit) {
                val deviceId = locationStore.deviceId()
                val sessions = runCatching { ServerApiClient.api.getBehaviorSessions() }.getOrDefault(emptyList())
                val alerts = runCatching { ServerApiClient.api.getDangerAlerts(deviceId) }.getOrDefault(emptyList())
                // 참고: danger-alerts 는 실제 발생 시각(epoch)이 있지만 behavior-sessions 는 세션 내 상대 초(startSec)만 있어서
                // 두 시간 기준이 달라 정확히 하나의 시간순으로 병합할 수는 없음 -> 위험 알림을 먼저, 그 아래 행동 세션을 보여줌.
                timeline = alerts.map { it.toTimelineEntry() } + sessions.map { it.toTimelineEntry() }
            }
            GuardianDailyMonitoringScreen(
                selectedTab = GuardianTab.TIMELINE,
                onTabSelected = { tab -> navController.navigateGuardianTab(tab) },
                timeline = timeline
            )
        }
        composable(PocoRoutes.GUARDIAN_TREND) {
            GuardianTrendScreen(
                selectedTab = GuardianTab.TREND,
                onTabSelected = { tab -> navController.navigateGuardianTab(tab) }
            )
        }
        composable(PocoRoutes.GUARDIAN_ALERTS) {
            val context = LocalContext.current
            val locationStore = remember(context) { LocationStore(context) }
            var anomalies by remember { mutableStateOf(emptyList<AnomalyAlert>()) }
            LaunchedEffect(Unit) {
                val deviceId = locationStore.deviceId()
                runCatching { ServerApiClient.api.getDangerAlerts(deviceId) }
                    .onSuccess { alerts -> anomalies = alerts.map { it.toAnomalyAlert() } }
            }
            // 참고: "일반 알림"(일일 요약/배터리 부족 등)은 아직 서버에서 만들어주는 로직이 없어서 빈 목록으로 표시됨.
            NotificationCenterScreen(
                selectedTab = GuardianTab.ALERTS,
                onTabSelected = { tab -> navController.navigateGuardianTab(tab) },
                anomalies = anomalies,
                generalNotices = emptyList()
            )
        }
        composable(PocoRoutes.GUARDIAN_SETTINGS) {
            GuardianSettingsScreen(
                selectedTab = GuardianTab.SETTINGS,
                onTabSelected = { tab -> navController.navigateGuardianTab(tab) },
                onOpenUserLinkInfo = { navController.navigate(PocoRoutes.GUARDIAN_USER_LINK_INFO) },
                onOpenNotificationSettings = { navController.navigate(PocoRoutes.GUARDIAN_NOTIFICATION_SETTINGS) },
                onOpenLinkNewUser = { navController.navigate(PocoRoutes.GUARDIAN_LINK_NEW_USER) }
            )
        }
        composable(PocoRoutes.GUARDIAN_USER_LINK_INFO) {
            GuardianUserLinkInfoScreen(
                onBack = { navController.popBackStack() },
                onUnlink = { navController.popBackStack() }
            )
        }
        composable(PocoRoutes.GUARDIAN_NOTIFICATION_SETTINGS) {
            NotificationSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(PocoRoutes.GUARDIAN_LINK_NEW_USER) {
            QrScanScreen(
                onScanned = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

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
