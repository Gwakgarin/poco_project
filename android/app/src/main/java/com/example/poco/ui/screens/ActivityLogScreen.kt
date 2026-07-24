package com.example.poco.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.VolumeUp
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
import com.example.poco.ui.theme.PocoGreen
import com.example.poco.ui.theme.PocoTextMuted
import com.example.poco.ui.theme.PocoTextPrimary

data class ActivityLogItem(
    val time: String,
    val label: String,
    val icon: ImageVector
)

private val mockActivityLog = listOf(
    ActivityLogItem("오전 8:45", "식사 소리 감지", Icons.Filled.Restaurant),
    ActivityLogItem("오전 7:30", "TV 소리 감지", Icons.Filled.VolumeUp)
)

@Composable
fun ActivityLogScreen(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    items: List<ActivityLogItem> = mockActivityLog
) {
    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "오늘의 활동 기록",
                    color = PocoTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 24.dp)
                        .padding(top = 16.dp, bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items) { item -> ActivityRow(item) }
                }
            }
            AppBottomNav(selectedTab = selectedTab, onTabSelected = onTabSelected)
        }
    }
}

@Composable
private fun ActivityRow(item: ActivityLogItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PocoCardBackground)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PocoGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = item.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = item.label, color = PocoTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(text = item.time, color = PocoTextMuted, fontSize = 13.sp)
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ActivityLogScreenPreview() {
    POCOTheme {
        ActivityLogScreen(selectedTab = AppTab.ANALYSIS, onTabSelected = {})
    }
}
