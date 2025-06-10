package com.example.software_project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(navController: NavHostController, viewModel: UserViewModel) {
    var isRegisterMode by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isRegisterMode) "注册" else "登录") }
            )
        }
    ) { paddingValues ->
        // 使用Box将内容居中
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp), // 增加水平边距
            contentAlignment = Alignment.Center // 垂直和水平居中
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isRegisterMode) {
                    RegisterScreenContent(navController, viewModel) {
                        isRegisterMode = false
                    }
                } else {
                    LoginScreenContent(navController, viewModel) {
                        isRegisterMode = true
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreenContent(
    navController: NavHostController,
    viewModel: UserViewModel,
    onToggleMode: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp) // 统一间距
    ) {
        // 1. 添加醒目的标题
        Text(
            text = "欢迎回来",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "登录以继续",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp)) // 标题和表单间的额外间距

        // 2. 使用 OutlinedTextField
        OutlinedTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            label = { Text("邮箱") },
            isError = viewModel.errorMessage.contains("邮箱"),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = viewModel.password,
            onValueChange = { viewModel.password = it },
            label = { Text("密码") },
            visualTransformation = PasswordVisualTransformation(),
            isError = viewModel.errorMessage.contains("密码"),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // 错误信息显示
        if (viewModel.errorMessage.isNotEmpty()) {
            Text(
                text = viewModel.errorMessage,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        // 加载指示器
        if (viewModel.isLoading) {
            CircularProgressIndicator()
        }

        // 主要操作按钮
        Button(
            onClick = { viewModel.login(navController.context, navController) },
            enabled = !viewModel.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("登录")
        }

        // 3. 使用 TextButton 作为次要操作
        TextButton(onClick = onToggleMode) {
            Text("还没有账户？立即注册")
        }
    }
}

@Composable
fun RegisterScreenContent(
    navController: NavHostController,
    viewModel: UserViewModel,
    onToggleMode: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp) // 统一间距
    ) {
        // 1. 添加醒目的标题
        Text(
            text = "创建账户",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "只需几步即可开始",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 2. 使用 OutlinedTextField
        OutlinedTextField(
            value = viewModel.email,
            onValueChange = { viewModel.email = it },
            label = { Text("邮箱") },
            isError = viewModel.errorMessage.contains("邮箱"),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = viewModel.name,
            onValueChange = { viewModel.name = it },
            label = { Text("姓名") },
            isError = viewModel.errorMessage.contains("姓名"),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // 根据是否已发送验证码，显示不同内容
        if (viewModel.isCodeSent) {
            OutlinedTextField(
                value = viewModel.code,
                onValueChange = { viewModel.code = it },
                label = { Text("验证码") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = viewModel.password,
                onValueChange = { viewModel.password = it },
                label = { Text("密码") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = viewModel.confirmPassword,
                onValueChange = { viewModel.confirmPassword = it },
                label = { Text("确认密码") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

        }

        // 错误信息显示
        if (viewModel.errorMessage.isNotEmpty()) {
            Text(
                text = viewModel.errorMessage,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        // 加载指示器
        if (viewModel.isLoading) {
            CircularProgressIndicator()
        }

        // 根据是否已发送验证码，显示不同按钮
        if (viewModel.isCodeSent) {
            Button(
                onClick = { viewModel.verifyAndRegister(navController.context, navController) },
                enabled = !viewModel.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("确认注册")
            }
        } else {
            Button(
                onClick = { viewModel.sendVerificationCode() },
                enabled = !viewModel.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("发送验证码")
            }
        }

        // 3. 使用 TextButton 作为次要操作
        TextButton(onClick = onToggleMode) {
            Text("已经有账户了？返回登录")
        }
    }
}