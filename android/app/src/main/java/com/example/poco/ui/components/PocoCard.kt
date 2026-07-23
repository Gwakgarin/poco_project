package com.example.poco.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.poco.ui.theme.PocoCardBackground

/** 은은하고 넓게 퍼지는 그림자를 쓰는 공용 카드 컨테이너. 화면 전반의 시각적 깊이를 통일한다. */
@Composable
fun PocoCard(
    modifier: Modifier = Modifier,
    containerColor: Color = PocoCardBackground,
    cornerRadius: Dp = 20.dp,
    contentPadding: Dp = 18.dp,
    elevation: Dp = 10.dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(shape)
            .background(containerColor)
            .padding(contentPadding)
    ) {
        content()
    }
}
