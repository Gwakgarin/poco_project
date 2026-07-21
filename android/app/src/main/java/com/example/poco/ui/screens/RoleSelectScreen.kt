package com.example.poco.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupervisorAccount
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
import com.example.poco.ui.theme.POCOTheme
import com.example.poco.ui.theme.PocoCardBackground
import com.example.poco.ui.theme.PocoGreen
import com.example.poco.ui.theme.PocoNavy
import com.example.poco.ui.theme.PocoTextMuted
import com.example.poco.ui.theme.PocoTextPrimary

@Composable
fun RoleSelectScreen(
    onSelectUser: () -> Unit,
    onSelectGuardian: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "어떤 역할로 시작할까요?",
                color = PocoTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "나중에 설정에서 언제든 바꿀 수 있어요",
                color = PocoTextMuted,
                fontSize = 14.sp
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(40.dp))

            RoleCard(
                title = "사용자",
                description = "일상 소리를 자동으로 감지해요",
                icon = Icons.Filled.Person,
                accentColor = PocoGreen,
                onClick = onSelectUser
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
            RoleCard(
                title = "보호자",
                description = "가족의 생활 패턴을 확인해요",
                icon = Icons.Filled.SupervisorAccount,
                accentColor = PocoNavy,
                onClick = onSelectGuardian
            )
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PocoCardBackground)
            .clickable(onClick = onClick)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(accentColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
        Text(text = title, color = PocoTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
        Text(text = description, color = PocoTextMuted, fontSize = 13.sp)
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun RoleSelectScreenPreview() {
    POCOTheme {
        RoleSelectScreen(onSelectUser = {}, onSelectGuardian = {})
    }
}
