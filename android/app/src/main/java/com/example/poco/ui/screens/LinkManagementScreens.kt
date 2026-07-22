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
import com.example.poco.ui.components.OutlineChipButton
import com.example.poco.ui.components.PocoCard
import com.example.poco.ui.components.PocoTopBar
import com.example.poco.ui.components.PrimaryButton
import com.example.poco.ui.theme.POCOTheme
import com.example.poco.ui.theme.PocoGreen
import com.example.poco.ui.theme.PocoNavy
import com.example.poco.ui.theme.PocoRed
import com.example.poco.ui.theme.PocoTextMuted
import com.example.poco.ui.theme.PocoTextPrimary

/** 사용자 화면 — 나를 지켜보는 보호자 목록 관리 */
@Composable
fun GuardianLinkManagementScreen(
    onBack: () -> Unit,
    onInviteGuardian: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            PocoTopBar(title = "보호자 연동 관리", onBack = onBack)

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                Text(text = "연동된 보호자", color = PocoTextMuted, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                PersonCard(
                    name = "박서연",
                    relation = "딸 · 2026년 3월 2일부터 연동",
                    accentColor = PocoNavy,
                    icon = Icons.Filled.SupervisorAccount,
                    trailingLabel = "해제",
                    trailingColor = PocoRed
                )

                Spacer(modifier = Modifier.height(24.dp))
                PrimaryButton(text = "새 보호자 초대하기", onClick = onInviteGuardian)
            }
        }
    }
}

/** 보호자 화면 — 내가 지켜보고 있는 사용자 연동 정보 */
@Composable
fun GuardianUserLinkInfoScreen(
    onBack: () -> Unit,
    onUnlink: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            PocoTopBar(title = "사용자 연동 정보", onBack = onBack)

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                Text(text = "연동된 사용자", color = PocoTextMuted, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                PersonCard(
                    name = "김민수",
                    relation = "아버지 · 2026년 3월 2일부터 연동",
                    accentColor = PocoGreen,
                    icon = Icons.Filled.Person,
                    trailingLabel = "해제",
                    trailingColor = PocoRed,
                    onTrailingClick = onUnlink
                )
            }
        }
    }
}

@Composable
private fun PersonCard(
    name: String,
    relation: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    trailingLabel: String,
    trailingColor: Color,
    onTrailingClick: () -> Unit = {}
) {
    PocoCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, color = PocoTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = relation, color = PocoTextMuted, fontSize = 12.sp)
            }
            OutlineChipButton(text = trailingLabel, onClick = onTrailingClick, borderColor = trailingColor)
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun GuardianLinkManagementScreenPreview() {
    POCOTheme { GuardianLinkManagementScreen(onBack = {}, onInviteGuardian = {}) }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun GuardianUserLinkInfoScreenPreview() {
    POCOTheme { GuardianUserLinkInfoScreen(onBack = {}, onUnlink = {}) }
}
