package com.example.poco.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.poco.R

/**
 * POCO 브랜드 마크: 확정된 고정 PNG(res/drawable/poco_mark.png)를 그대로 사용한다.
 * 이미지 자체에 라운드 사각형 모양과 투명 배경이 이미 포함되어 있어 별도 클립이 필요 없다.
 * 로그인·스플래시 등 브랜드 모먼트가 필요한 화면에서 공용으로 재사용한다.
 */
@Composable
fun PocoLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    light: Boolean = false
) {
    Image(
        painter = painterResource(if (light) R.drawable.poco_mark_light else R.drawable.poco_mark),
        contentDescription = "POCO",
        modifier = modifier.size(size)
    )
}
