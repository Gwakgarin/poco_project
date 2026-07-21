package com.example.poco.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.poco.ui.components.AppTab
import com.example.poco.ui.components.GuardianTab
import com.example.poco.ui.screens.ActivityLogScreen
import com.example.poco.ui.screens.EmergencyGuardianScreen
import com.example.poco.ui.screens.EmergencyUserScreen
import com.example.poco.ui.screens.GuardianDailyMonitoringScreen
import com.example.poco.ui.screens.GuardianHomeScreen
import com.example.poco.ui.screens.GuardianSettingsScreen
import com.example.poco.ui.screens.GuardianTrendScreen
import com.example.poco.ui.screens.LoginScreen
import com.example.poco.ui.screens.NotificationCenterScreen
import com.example.poco.ui.screens.QrScanScreen
import com.example.poco.ui.screens.QrShowScreen
import com.example.poco.ui.screens.RoleSelectScreen
import com.example.poco.ui.screens.SettingsScreen
import com.example.poco.ui.screens.UserHomeScreen
import com.example.poco.ui.screens.UserHomeUiState

object PocoRoutes {
    const val LOGIN = "login"
    const val ROLE_SELECT = "role_select"
    const val QR_SHOW = "qr_show"
    const val QR_SCAN = "qr_scan"
    const val USER_HOME = "user_home"
    const val USER_ACTIVITY = "user_activity"
    const val USER_SETTINGS = "user_settings"
    const val EMERGENCY_USER = "emergency_user"
    const val EMERGENCY_GUARDIAN = "emergency_guardian"
    const val GUARDIAN_HOME = "guardian_home"
    const val GUARDIAN_TIMELINE = "guardian_timeline"
    const val GUARDIAN_TREND = "guardian_trend"
    const val GUARDIAN_ALERTS = "guardian_alerts"
    const val GUARDIAN_SETTINGS = "guardian_settings"
}

private fun NavHostController.navigateTopLevel(route: String, popUpToRoute: String) {
    navigate(route) {
        popUpTo(popUpToRoute) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun PocoNavHost(
    homeUiState: UserHomeUiState,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = PocoRoutes.LOGIN, modifier = modifier) {
        composable(PocoRoutes.LOGIN) {
            LoginScreen(
                onLoginClick = { navController.navigate(PocoRoutes.ROLE_SELECT) },
                onSignUpClick = { navController.navigate(PocoRoutes.ROLE_SELECT) }
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
                onDone = { navController.navigateTopLevel(PocoRoutes.USER_HOME, PocoRoutes.LOGIN) }
            )
        }
        composable(PocoRoutes.QR_SCAN) {
            QrScanScreen(
                onScanned = { navController.navigateTopLevel(PocoRoutes.GUARDIAN_HOME, PocoRoutes.LOGIN) }
            )
        }

        // 사용자 모드
        composable(PocoRoutes.USER_HOME) {
            UserHomeScreen(
                uiState = homeUiState,
                selectedTab = AppTab.HOME,
                onTabSelected = { tab -> navController.navigateUserTab(tab) }
            )
        }
        composable(PocoRoutes.USER_ACTIVITY) {
            ActivityLogScreen(
                selectedTab = AppTab.ANALYSIS,
                onTabSelected = { tab -> navController.navigateUserTab(tab) }
            )
        }
        composable(PocoRoutes.USER_SETTINGS) {
            SettingsScreen(
                selectedTab = AppTab.SETTINGS,
                onTabSelected = { tab -> navController.navigateUserTab(tab) },
                onOpenGuardianDemo = { navController.navigate(PocoRoutes.GUARDIAN_HOME) },
                onOpenEmergencyDemo = { navController.navigate(PocoRoutes.EMERGENCY_USER) }
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
            EmergencyGuardianScreen(
                onCheckLocation = {},
                onDispatch = {},
                onCall = {}
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
            GuardianDailyMonitoringScreen(
                selectedTab = GuardianTab.TIMELINE,
                onTabSelected = { tab -> navController.navigateGuardianTab(tab) }
            )
        }
        composable(PocoRoutes.GUARDIAN_TREND) {
            GuardianTrendScreen(
                selectedTab = GuardianTab.TREND,
                onTabSelected = { tab -> navController.navigateGuardianTab(tab) }
            )
        }
        composable(PocoRoutes.GUARDIAN_ALERTS) {
            NotificationCenterScreen(
                selectedTab = GuardianTab.ALERTS,
                onTabSelected = { tab -> navController.navigateGuardianTab(tab) }
            )
        }
        composable(PocoRoutes.GUARDIAN_SETTINGS) {
            GuardianSettingsScreen(
                selectedTab = GuardianTab.SETTINGS,
                onTabSelected = { tab -> navController.navigateGuardianTab(tab) }
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
