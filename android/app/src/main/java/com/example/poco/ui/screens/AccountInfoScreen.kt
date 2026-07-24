package com.example.poco.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poco.ui.components.PocoCard
import com.example.poco.ui.components.PocoTopBar
import com.example.poco.ui.components.SecondaryButton
import com.example.poco.ui.theme.POCOTheme
import com.example.poco.ui.theme.PocoDivider
import com.example.poco.ui.theme.PocoGreen
import com.example.poco.ui.theme.PocoTextMuted
import com.example.poco.ui.theme.PocoTextPrimary

@Composable
fun AccountInfoScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            PocoTopBar(title = "계정 정보", onBack = onBack)

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(PocoGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = "김민수", color = PocoTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(text = "minsu.kim@example.com", color = PocoTextMuted, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                PocoCard {
                    InfoRow(label = "가입일", value = "2026년 3월 2일")
                    HorizontalDivider(color = PocoDivider, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))
                    InfoRow(label = "역할", value = "사용자")
                    HorizontalDivider(color = PocoDivider, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))
                    InfoRow(label = "연동된 보호자", value = "1명")
                }

                Spacer(modifier = Modifier.height(32.dp))
                SecondaryButton(text = "로그아웃", onClick = onLogout)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = PocoTextMuted, fontSize = 14.sp)
        Text(text = value, color = PocoTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun AccountInfoScreenPreview() {
    POCOTheme { AccountInfoScreen(onBack = {}, onLogout = {}) }
}
