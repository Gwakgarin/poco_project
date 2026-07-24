package com.example.poco.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poco.ui.components.AppBottomNav
import com.example.poco.ui.components.AppTab
import com.example.poco.ui.theme.POCOTheme
import com.example.poco.ui.theme.PocoCardBackground
import com.example.poco.ui.theme.PocoDivider
import com.example.poco.ui.theme.PocoRed
import com.example.poco.ui.theme.PocoTextMuted
import com.example.poco.ui.theme.PocoTextPrimary

@Composable
fun SettingsScreen(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onOpenMicSensitivity: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenGuardianLinkManagement: () -> Unit,
    onOpenAccountInfo: () -> Unit,
    onOpenGuardianDemo: () -> Unit,
    onOpenEmergencyDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "설정",
                    color = PocoTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 24.dp)
                        .padding(top = 16.dp, bottom = 16.dp)
                )

                SettingsRow(icon = Icons.Filled.Mic, label = "마이크 감도", onClick = onOpenMicSensitivity)
                SettingsRow(icon = Icons.Filled.NotificationsActive, label = "알림 설정", onClick = onOpenNotificationSettings)
                SettingsRow(icon = Icons.Filled.Group, label = "보호자 연동 관리", onClick = onOpenGuardianLinkManagement)
                SettingsRow(icon = Icons.Filled.Person, label = "계정 정보", onClick = onOpenAccountInfo)

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "데모 미리보기",
                    color = PocoTextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow(icon = Icons.Filled.Group, label = "보호자 모드 미리보기", onClick = onOpenGuardianDemo)
                SettingsRow(
                    icon = Icons.Filled.ReportProblem,
                    label = "긴급 상황 화면 미리보기",
                    labelColor = PocoRed,
                    onClick = onOpenEmergencyDemo
                )
            }
            AppBottomNav(selectedTab = selectedTab, onTabSelected = onTabSelected)
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    labelColor: Color = PocoTextPrimary,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = PocoTextMuted, modifier = Modifier.height(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, color = labelColor, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = PocoTextMuted
        )
    }
    HorizontalDivider(color = PocoDivider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 24.dp))
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun SettingsScreenPreview() {
    POCOTheme {
        SettingsScreen(
            selectedTab = AppTab.SETTINGS,
            onTabSelected = {},
            onOpenMicSensitivity = {},
            onOpenNotificationSettings = {},
            onOpenGuardianLinkManagement = {},
            onOpenAccountInfo = {},
            onOpenGuardianDemo = {},
            onOpenEmergencyDemo = {}
        )
    }
}
