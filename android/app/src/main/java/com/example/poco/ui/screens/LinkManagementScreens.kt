package com.example.poco.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
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

enum class LinkedPersonRole { GUARDIAN, USER }

data class LinkedPerson(
    val linkId: String,
    val name: String,
    val relationLabel: String,
    val linkedAt: String,
    val role: LinkedPersonRole
)

private val mockGuardians = listOf(
    LinkedPerson(linkId = "g1", name = "박서연", relationLabel = "딸", linkedAt = "2026년 3월 2일", role = LinkedPersonRole.GUARDIAN),
    LinkedPerson(linkId = "g2", name = "박준혁", relationLabel = "아들", linkedAt = "2026년 4월 15일", role = LinkedPersonRole.GUARDIAN)
)

private val mockLinkedUsers = listOf(
    LinkedPerson(linkId = "u1", name = "김민수", relationLabel = "아버지", linkedAt = "2026년 3월 2일", role = LinkedPersonRole.USER)
)

/** 사용자 화면 — 나를 지켜보는 보호자 목록 관리 */
@Composable
fun GuardianLinkManagementScreen(
    onBack: () -> Unit,
    onInviteGuardian: () -> Unit,
    modifier: Modifier = Modifier,
    guardians: List<LinkedPerson> = mockGuardians,
    onUnlink: (LinkedPerson) -> Unit = {}
) {
    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            PocoTopBar(title = "보호자 연동 관리", onBack = onBack)

            Column(modifier = Modifier.weight(1f).padding(horizontal = 24.dp, vertical = 12.dp)) {
                Text(text = "연동된 보호자", color = PocoTextMuted, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(guardians, key = { it.linkId }) { guardian ->
                        PersonCard(person = guardian, trailingLabel = "해제", onTrailingClick = { onUnlink(guardian) })
                    }
                }

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
    onUnlink: (LinkedPerson) -> Unit,
    modifier: Modifier = Modifier,
    linkedUsers: List<LinkedPerson> = mockLinkedUsers
) {
    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            PocoTopBar(title = "사용자 연동 정보", onBack = onBack)

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                Text(text = "연동된 사용자", color = PocoTextMuted, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(linkedUsers, key = { it.linkId }) { user ->
                        PersonCard(person = user, trailingLabel = "해제", onTrailingClick = { onUnlink(user) })
                    }
                }
            }
        }
    }
}

private fun roleAccentColor(role: LinkedPersonRole): Color = when (role) {
    LinkedPersonRole.GUARDIAN -> PocoNavy
    LinkedPersonRole.USER -> PocoGreen
}

private fun roleIcon(role: LinkedPersonRole): ImageVector = when (role) {
    LinkedPersonRole.GUARDIAN -> Icons.Filled.SupervisorAccount
    LinkedPersonRole.USER -> Icons.Filled.Person
}

@Composable
private fun PersonCard(
    person: LinkedPerson,
    trailingLabel: String,
    onTrailingClick: () -> Unit = {}
) {
    PocoCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(roleAccentColor(person.role)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = roleIcon(person.role), contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = person.name, color = PocoTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = "${person.relationLabel} · ${person.linkedAt}부터 연동", color = PocoTextMuted, fontSize = 12.sp)
            }
            OutlineChipButton(text = trailingLabel, onClick = onTrailingClick, borderColor = PocoRed)
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
