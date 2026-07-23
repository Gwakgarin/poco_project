package com.example.poco.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poco.ui.theme.PocoDivider
import com.example.poco.ui.theme.PocoNavy
import com.example.poco.ui.theme.PocoTextMuted

enum class GuardianTab(val label: String, val filled: ImageVector, val outline: ImageVector) {
    HOME("홈", Icons.Filled.Home, Icons.Outlined.Home),
    TIMELINE("타임라인", Icons.Filled.Timeline, Icons.Outlined.Timeline),
    TREND("추이", Icons.Filled.TrendingUp, Icons.Outlined.TrendingUp),
    ALERTS("알림", Icons.Filled.Notifications, Icons.Outlined.Notifications),
    SETTINGS("설정", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun GuardianBottomNav(
    selectedTab: GuardianTab,
    onTabSelected: (GuardianTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)) {
        HorizontalDivider(color = PocoDivider, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            GuardianTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                val tint = if (selected) PocoNavy else PocoTextMuted
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (selected) tab.filled else tab.outline,
                        contentDescription = tab.label,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = tab.label, color = tint, fontSize = 11.sp)
                }
            }
        }
    }
}
