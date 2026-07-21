package com.example.poco.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poco.R
import com.example.poco.ui.theme.PocoDivider
import com.example.poco.ui.theme.PocoTextMuted

enum class AppTab { HOME, ANALYSIS, SETTINGS }

@Composable
fun AppBottomNav(selectedTab: AppTab, onTabSelected: (AppTab) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = PocoDivider, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavItem(
                label = "홈",
                selected = selectedTab == AppTab.HOME,
                onClick = { onTabSelected(AppTab.HOME) }
            ) { tint ->
                Icon(
                    painter = painterResource(
                        id = if (selectedTab == AppTab.HOME) R.drawable.ic_home_filled else R.drawable.ic_home_outline
                    ),
                    contentDescription = "홈",
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
            NavItem(
                label = "분석",
                selected = selectedTab == AppTab.ANALYSIS,
                onClick = { onTabSelected(AppTab.ANALYSIS) }
            ) { tint ->
                Icon(
                    imageVector = if (selectedTab == AppTab.ANALYSIS) Icons.Filled.BarChart else Icons.Outlined.BarChart,
                    contentDescription = "분석",
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
            NavItem(
                label = "설정",
                selected = selectedTab == AppTab.SETTINGS,
                onClick = { onTabSelected(AppTab.SETTINGS) }
            ) { tint ->
                Icon(
                    painter = painterResource(id = R.drawable.ic_settings_outline),
                    contentDescription = "설정",
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit
) {
    val tint = if (selected) Color.Black else PocoTextMuted
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        icon(tint)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = tint, fontSize = 14.sp)
    }
}
