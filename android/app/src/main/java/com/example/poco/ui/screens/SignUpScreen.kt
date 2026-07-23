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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.poco.ui.components.PocoTextField
import com.example.poco.ui.components.PocoTopBar
import com.example.poco.ui.components.PrimaryButton
import com.example.poco.ui.theme.POCOTheme
import com.example.poco.ui.theme.PocoTextMuted

@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onSignUpComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }

    val isValid = name.isNotBlank() && email.isNotBlank() &&
        password.isNotBlank() && password == passwordConfirm

    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            PocoTopBar(title = "회원가입", onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "가입 후 역할(사용자/보호자)을 선택할 수 있어요",
                    color = PocoTextMuted,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                PocoTextField(value = name, onValueChange = { name = it }, label = "이름", modifier = Modifier.fillMaxWidth())
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
                PocoTextField(
                    value = passwordConfirm,
                    onValueChange = { passwordConfirm = it },
                    label = "비밀번호 확인",
                    isPassword = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
                PrimaryButton(text = "가입 완료", onClick = onSignUpComplete, enabled = isValid)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun SignUpScreenPreview() {
    POCOTheme {
        SignUpScreen(onBack = {}, onSignUpComplete = {})
    }
}
