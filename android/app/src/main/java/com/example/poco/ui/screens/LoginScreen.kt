package com.example.poco.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poco.ui.components.PrimaryButton
import com.example.poco.ui.components.SecondaryButton
import com.example.poco.ui.theme.POCOTheme
import com.example.poco.ui.theme.PocoGreen
import com.example.poco.ui.theme.PocoTextPrimary

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PocoLogoMark()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "POCO",
                    color = PocoTextPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 35.dp)
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryButton(text = "로그인", onClick = onLoginClick)
                SecondaryButton(text = "회원가입", onClick = onSignUpClick)
            }
        }
    }
}

@Composable
private fun PocoLogoMark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = PocoGreen)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Canvas(modifier = Modifier.size(20.dp)) {
            val radius = size.minDimension / 2f
            drawArc(
                color = PocoGreen,
                startAngle = 110f,
                sweepAngle = 140f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.7f),
                topLeft = Offset(size.width * 0.1f, size.height * 0.1f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.8f, size.height * 0.8f)
            )
        }
        Spacer(modifier = Modifier.width(2.dp))
        Canvas(modifier = Modifier.size(41.dp)) {
            drawCircle(color = PocoGreen)
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun LoginScreenPreview() {
    POCOTheme {
        LoginScreen(onLoginClick = {}, onSignUpClick = {})
    }
}
