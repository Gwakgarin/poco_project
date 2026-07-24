package com.example.poco.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poco.ui.theme.PocoCardBackground
import com.example.poco.ui.theme.PocoGreen
import com.example.poco.ui.theme.PocoRed
import com.example.poco.ui.theme.PocoTextMuted
import com.example.poco.ui.theme.PocoTextPrimary

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = PocoGreen,
    enabled: Boolean = true
) {
    val resolvedColor = if (enabled) containerColor else PocoTextMuted.copy(alpha = 0.35f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                elevation = if (enabled) 8.dp else 0.dp,
                shape = RoundedCornerShape(26.dp),
                clip = false,
                ambientColor = resolvedColor.copy(alpha = 0.35f),
                spotColor = resolvedColor.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(resolvedColor)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(PocoCardBackground)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) PocoTextMuted else PocoTextMuted.copy(alpha = 0.4f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PrimaryButton(text = text, onClick = onClick, modifier = modifier, containerColor = PocoRed)
}

@Composable
fun OutlineChipButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = PocoTextMuted,
    enabled: Boolean = true
) {
    val resolvedBorderColor = if (enabled) borderColor else borderColor.copy(alpha = 0.35f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, resolvedBorderColor), RoundedCornerShape(20.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(PaddingValues(horizontal = 16.dp, vertical = 8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) PocoTextPrimary else PocoTextMuted.copy(alpha = 0.5f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
