package com.example.poco.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poco.ui.components.PocoLogoBadge
import com.example.poco.ui.components.PocoTextField
import com.example.poco.ui.components.PocoTopBar
import com.example.poco.ui.components.PrimaryButton
import com.example.poco.ui.components.SecondaryButton
import com.example.poco.ui.theme.POCOTheme
import com.example.poco.ui.theme.PocoGreen
import com.example.poco.ui.theme.PocoTextMuted
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
                PocoLogoBadge(size = 92.dp, badgeColor = Color.White, markColor = PocoGreen)
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "POCO",
                    color = PocoTextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.4).sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "소리로 지키는 가족의 하루",
                    color = PocoTextMuted,
                    fontSize = 14.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryButton(text = "로그인", onClick = onLoginClick)
                SecondaryButton(text = "회원가입", onClick = onSignUpClick)
            }
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

@Composable
fun LoginFormScreen(
    onBack: () -> Unit,
    onLoginComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isValid = email.isNotBlank() && password.isNotBlank()

    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            PocoTopBar(title = "로그인", onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "가입할 때 등록한 이메일과 비밀번호를 입력해주세요",
                    color = PocoTextMuted,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                PocoTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "이메일",
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier.fillMaxWidth()
                )
                PocoTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "비밀번호",
                    isPassword = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
                PrimaryButton(text = "로그인", onClick = onLoginComplete, enabled = isValid)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun LoginFormScreenPreview() {
    POCOTheme {
        LoginFormScreen(onBack = {}, onLoginComplete = {})
    }
}
