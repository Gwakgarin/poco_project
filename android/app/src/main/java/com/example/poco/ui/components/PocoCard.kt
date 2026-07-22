package com.example.poco.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.poco.ui.theme.PocoCardBackground

/** 은은한 그림자가 있는 공용 카드 컨테이너. 화면 전반의 시각적 깊이를 통일한다. */
@Composable
fun PocoCard(
    modifier: Modifier = Modifier,
    containerColor: Color = PocoCardBackground,
    cornerRadius: androidx.compose.ui.unit.Dp = 16.dp,
    contentPadding: androidx.compose.ui.unit.Dp = 16.dp,
    elevation: androidx.compose.ui.unit.Dp = 2.dp,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}
