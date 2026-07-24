package com.example.poco.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * POCO 브랜드 마크: 소리를 듣는다는 컨셉을 담은 "리스닝 시그널" 아이콘.
 * 로그인·스플래시 등 브랜드 모먼트가 필요한 화면에서 공용으로 재사용한다.
 */
@Composable
fun PocoLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    badgeColor: Color = Color.White,
    markColor: Color = Color(0xFF1FAE73),
    elevated: Boolean = true
) {
    val shadowModifier = if (elevated) {
        Modifier.shadow(
            elevation = 16.dp,
            shape = CircleShape,
            ambientColor = markColor.copy(alpha = 0.35f),
            spotColor = markColor.copy(alpha = 0.35f)
        )
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier
            .size(size)
            .then(shadowModifier)
            .clip(CircleShape)
            .background(badgeColor)
    ) {
        val dotRadius = this.size.minDimension * 0.09f
        val center = Offset(this.size.width * 0.36f, this.size.height * 0.5f)
        drawCircle(color = markColor, radius = dotRadius, center = center)

        listOf(0.62f, 0.85f).forEachIndexed { index, factor ->
            val arcRadius = this.size.minDimension * factor * 0.4f
            val strokeWidth = this.size.minDimension * (0.075f - index * 0.018f)
            drawArc(
                color = markColor.copy(alpha = 1f - index * 0.25f),
                startAngle = -55f,
                sweepAngle = 110f,
                useCenter = false,
                style = Stroke(width = strokeWidth),
                topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                size = Size(arcRadius * 2, arcRadius * 2)
            )
        }
    }
}
