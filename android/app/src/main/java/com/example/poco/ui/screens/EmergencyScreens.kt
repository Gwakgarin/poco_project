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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poco.ui.components.DangerButton
import com.example.poco.ui.components.PrimaryButton
import com.example.poco.ui.components.SecondaryButton
import com.example.poco.ui.theme.POCOTheme
import com.example.poco.ui.theme.PocoNavy
import com.example.poco.ui.theme.PocoRed
import com.example.poco.ui.theme.PocoRedGlowEnd
import com.example.poco.ui.theme.PocoRedGlowStart
import com.example.poco.ui.theme.PocoTextMuted
import com.example.poco.ui.theme.PocoTextPrimary

/** 사용자 모드 — 평소보다 큰 소리가 감지됐을 때 뜨는 확인 화면 (빨간 테마). */
@Composable
fun EmergencyUserScreen(
    onSafe: () -> Unit,
    onSos: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            0f to PocoRedGlowStart,
                            0.85f to Color.White,
                            1f to PocoRedGlowEnd
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = PocoRed,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "평소보다 큰 소리가\n감지되었어요",
                color = PocoTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "괜찮으신가요? 응답이 없으면\n보호자에게 자동으로 알림이 전송돼요",
                color = PocoTextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryButton(text = "괜찮아요", onClick = onSafe)
                DangerButton(text = "SOS 요청", onClick = onSos)
            }
        }
    }
}

/** 보호자 모드 — 사용자의 긴급 상황이 감지됐을 때 뜨는 대응 화면. */
@Composable
fun EmergencyGuardianScreen(
    onCheckLocation: () -> Unit,
    onDispatch: () -> Unit,
    onCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = PocoNavy) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(PocoRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Filled.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "긴급 상황이 감지되었습니다",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "어머니 · 5분 전 · 응답 없음",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color.White)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Filled.LocationOn, contentDescription = null, tint = PocoRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "마지막 위치: 자택 인근 250m", color = PocoTextPrimary, fontSize = 14.sp)
                }
                SecondaryButton(text = "위치 확인하기", onClick = onCheckLocation)
                DangerButton(text = "출동하기", onClick = onDispatch)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Call, contentDescription = null, tint = PocoTextMuted, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "전화 걸기",
                        color = PocoTextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun EmergencyUserScreenPreview() {
    POCOTheme { EmergencyUserScreen(onSafe = {}, onSos = {}) }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun EmergencyGuardianScreenPreview() {
    POCOTheme { EmergencyGuardianScreen(onCheckLocation = {}, onDispatch = {}, onCall = {}) }
}
