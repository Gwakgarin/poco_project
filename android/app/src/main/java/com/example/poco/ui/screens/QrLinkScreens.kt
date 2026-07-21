package com.example.poco.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poco.ui.components.PrimaryButton
import com.example.poco.ui.theme.POCOTheme
import com.example.poco.ui.theme.PocoCardBackground
import com.example.poco.ui.theme.PocoGreen
import com.example.poco.ui.theme.PocoTextMuted
import com.example.poco.ui.theme.PocoTextPrimary
import kotlin.random.Random

/** 사용자 기기에 표시되는 연동 코드 화면. 보호자가 이 코드를 스캔해 계정을 연결한다. */
@Composable
fun QrShowScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "보호자 앱에서\n이 코드를 스캔해주세요",
                color = PocoTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            QrPlaceholder(seed = 42)
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "연동 코드: 7QK2-90LX", color = PocoTextMuted, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(40.dp))
            PrimaryButton(text = "연동 완료", onClick = onDone)
        }
    }
}

/** 보호자 기기의 스캔 화면. 카메라 연동 전이라 뷰파인더는 목업이며 버튼으로 다음 단계로 넘어간다. */
@Composable
fun QrScanScreen(
    onScanned: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = Color.Black) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "사용자 기기의 코드를\n화면 안에 맞춰주세요",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .border(2.dp, PocoGreen, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = null,
                    tint = PocoGreen,
                    modifier = Modifier.size(64.dp)
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
            PrimaryButton(text = "스캔 완료 (테스트)", onClick = onScanned)
        }
    }
}

@Composable
private fun QrPlaceholder(seed: Int) {
    val random = remember(seed) { Random(seed) }
    val gridSize = 12
    val cells = remember(seed) { List(gridSize * gridSize) { random.nextFloat() > 0.55f } }
    Box(
        modifier = Modifier
            .size(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PocoCardBackground)
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellSize = size.minDimension / gridSize
            for (row in 0 until gridSize) {
                for (col in 0 until gridSize) {
                    if (cells[row * gridSize + col]) {
                        drawRect(
                            color = Color.Black,
                            topLeft = androidx.compose.ui.geometry.Offset(col * cellSize, row * cellSize),
                            size = Size(cellSize, cellSize)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun QrShowScreenPreview() {
    POCOTheme { QrShowScreen(onDone = {}) }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun QrScanScreenPreview() {
    POCOTheme { QrScanScreen(onScanned = {}) }
}
