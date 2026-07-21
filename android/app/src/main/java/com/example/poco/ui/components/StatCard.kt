package com.example.poco.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poco.ui.theme.PocoCardBackground
import com.example.poco.ui.theme.PocoStatusGreen
import com.example.poco.ui.theme.PocoTextMuted

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = PocoStatusGreen,
    backgroundColor: Color = PocoCardBackground
) {
    Column(
        modifier = modifier
            .height(74.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = label, color = PocoTextMuted, fontSize = 16.sp)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(6.dp))
        Text(text = value, color = valueColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}
