package com.example.poco.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.poco.ui.theme.KohiBaeum
import com.example.poco.ui.theme.POCOTheme
import com.example.poco.ui.theme.PocoNavy
import kotlinx.coroutines.delay

private const val SPLASH_DURATION_MS = 1400L

@Composable
fun SplashScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MS)
        onTimeout()
    }

    Surface(modifier = modifier.fillMaxSize(), color = PocoNavy) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "POCO",
                color = Color.White,
                fontFamily = KohiBaeum,
                fontSize = 34.sp,
                letterSpacing = (-1.4).sp
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun SplashScreenPreview() {
    POCOTheme {
        SplashScreen(onTimeout = {})
    }
}
