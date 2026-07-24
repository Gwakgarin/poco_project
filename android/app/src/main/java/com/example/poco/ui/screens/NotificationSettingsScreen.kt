package com.example.poco.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poco.ui.components.PocoTopBar
import com.example.poco.ui.theme.POCOTheme
import com.example.poco.ui.theme.PocoDivider
import com.example.poco.ui.theme.PocoGreen
import com.example.poco.ui.theme.PocoTextMuted
import com.example.poco.ui.theme.PocoTextPrimary

private data class ToggleItem(val title: String, val description: String, val default: Boolean)

private val defaultToggles = listOf(
    ToggleItem("긴급 알림", "SOS·큰 소리 감지 시 즉시 알림", true),
    ToggleItem("활동 이상 알림", "평소와 다른 생활 패턴 감지 시 알림", true),
    ToggleItem("배터리 부족 알림", "기기 배터리가 15% 이하일 때 알림", true),
    ToggleItem("일일 요약 알림", "매일 저녁 하루 활동 요약 전송", false)
)

@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            PocoTopBar(title = "알림 설정", onBack = onBack)
            Column(modifier = Modifier.padding(top = 8.dp)) {
                defaultToggles.forEach { item ->
                    var checked by remember { mutableStateOf(item.default) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.title, color = PocoTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = item.description, color = PocoTextMuted, fontSize = 12.sp)
                        }
                        Switch(
                            checked = checked,
                            onCheckedChange = { checked = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = PocoGreen)
                        )
                    }
                    HorizontalDivider(color = PocoDivider, thickness = 1.dp, modifier = Modifier.padding(horizontal = 24.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun NotificationSettingsScreenPreview() {
    POCOTheme { NotificationSettingsScreen(onBack = {}) }
}
