package com.example.poco.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poco.ui.components.PocoCard
import com.example.poco.ui.components.PocoTopBar
import com.example.poco.ui.theme.POCOTheme
import com.example.poco.ui.theme.PocoGreen
import com.example.poco.ui.theme.PocoTextMuted
import com.example.poco.ui.theme.PocoTextPrimary

@Composable
fun MicSensitivityScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sensitivity by remember { mutableFloatStateOf(0.55f) }
    val level = when {
        sensitivity < 0.34f -> "낮음"
        sensitivity < 0.67f -> "보통"
        else -> "높음"
    }

    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            PocoTopBar(title = "마이크 감도", onBack = onBack)

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                Text(
                    text = "감도가 높을수록 작은 소리도 더 잘 감지하지만 배터리 소모가 늘어날 수 있어요",
                    color = PocoTextMuted,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(24.dp))

                PocoCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "현재 감도", color = PocoTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(text = level, color = PocoGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Slider(
                        value = sensitivity,
                        onValueChange = { sensitivity = it },
                        colors = SliderDefaults.colors(
                            thumbColor = PocoGreen,
                            activeTrackColor = PocoGreen
                        )
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "낮음", color = PocoTextMuted, fontSize = 12.sp)
                        Text(text = "높음", color = PocoTextMuted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun MicSensitivityScreenPreview() {
    POCOTheme { MicSensitivityScreen(onBack = {}) }
}
