package com.example.poco.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poco.ui.components.PocoTopBar
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
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            PocoTopBar(title = "", onBack = onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "보호자 앱에서\n이 코드를 스캔해주세요",
                    color = PocoTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
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
}

/** 보호자 기기의 스캔 화면. 카메라 연동 전이라 뷰파인더는 목업이며 버튼으로 다음 단계로 넘어간다. */
@Composable
fun QrScanScreen(
    onScanned: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = Color.Black) {
        Column(modifier = Modifier.fillMaxSize()) {
            PocoTopBar(title = "", onBack = onBack, contentColor = Color.White)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "사용자 기기의 코드를\n화면 안에 맞춰주세요",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
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
}

private val RELATION_OPTIONS = listOf("딸", "아들", "배우자", "요양보호사", "기타")

/** QR 스캔 완료 직후, 보호자가 피보호자와의 관계를 선택하는 화면. */
@Composable
fun RelationSelectScreen(
    onComplete: (relationLabel: String) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf<String?>(null) }

    val isValid = selected != null

    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            PocoTopBar(title = "관계 선택", onBack = onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "연동하는 분과 어떤 관계인가요?",
                    color = PocoTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                RELATION_OPTIONS.chunked(2).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowOptions.forEach { option ->
                            RelationOptionCard(
                                label = option,
                                isSelected = selected == option,
                                onClick = { selected = option },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowOptions.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
                PrimaryButton(
                    text = "완료",
                    onClick = { onComplete(selected.orEmpty()) },
                    enabled = isValid
                )
            }
        }
    }
}

@Composable
private fun RelationOptionCard(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) PocoGreen.copy(alpha = 0.12f) else PocoCardBackground)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) PocoGreen else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) PocoGreen else PocoTextPrimary,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun QrPlaceholder(seed: Int) {
    val random = remember(seed) { Random(seed) }
    val gridSize = 16
    val finderSize = 4
    val isFinderCell = { row: Int, col: Int ->
        (row < finderSize && col < finderSize) ||
            (row < finderSize && col >= gridSize - finderSize) ||
            (row >= gridSize - finderSize && col < finderSize)
    }
    val cells = remember(seed) {
        List(gridSize * gridSize) { index ->
            val row = index / gridSize
            val col = index % gridSize
            if (isFinderCell(row, col)) false else random.nextFloat() > 0.55f
        }
    }
    Box(
        modifier = Modifier
            .size(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PocoCardBackground)
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellSize = size.minDimension / gridSize

            fun drawFinderPattern(originRow: Int, originCol: Int) {
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(originCol * cellSize, originRow * cellSize),
                    size = Size(cellSize * finderSize, cellSize * finderSize)
                )
                drawRect(
                    color = Color.White,
                    topLeft = Offset((originCol + 0.7f) * cellSize, (originRow + 0.7f) * cellSize),
                    size = Size(cellSize * (finderSize - 1.4f), cellSize * (finderSize - 1.4f))
                )
                drawRect(
                    color = Color.Black,
                    topLeft = Offset((originCol + 1.3f) * cellSize, (originRow + 1.3f) * cellSize),
                    size = Size(cellSize * (finderSize - 2.6f), cellSize * (finderSize - 2.6f))
                )
            }

            for (row in 0 until gridSize) {
                for (col in 0 until gridSize) {
                    if (cells[row * gridSize + col]) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(col * cellSize, row * cellSize),
                            size = Size(cellSize * 0.9f, cellSize * 0.9f)
                        )
                    }
                }
            }

            drawFinderPattern(0, 0)
            drawFinderPattern(0, gridSize - finderSize)
            drawFinderPattern(gridSize - finderSize, 0)
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun QrShowScreenPreview() {
    POCOTheme { QrShowScreen(onDone = {}, onBack = {}) }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun QrScanScreenPreview() {
    POCOTheme { QrScanScreen(onScanned = {}, onBack = {}) }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun RelationSelectScreenPreview() {
    POCOTheme { RelationSelectScreen(onComplete = {}, onBack = {}) }
}
