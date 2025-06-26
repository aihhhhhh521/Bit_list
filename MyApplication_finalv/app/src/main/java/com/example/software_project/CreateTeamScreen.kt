package com.example.software_project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTeamScreen(navController: NavHostController, viewModel: TaskViewModel = viewModel()) {
    // --- 所有状态管理逻辑完整保留 ---
    var teamName by remember { mutableStateOf("") }
    var teamDescription by remember { mutableStateOf("") }
    // --- 状态管理逻辑结束 ---

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新建团队") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        // 使用Box将表单内容在屏幕上居中
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp), // 增加水平边距
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp) // 统一元素垂直间距
            ) {
                // 添加一个更具描述性的标题
                Text(
                    text = "设置您的团队信息",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                // 使用 OutlinedTextField 统一输入框样式
                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("团队名称*") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = teamDescription,
                    onValueChange = { teamDescription = it },
                    label = { Text("团队描述") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3 // 设置为多行输入框
                )

                // 创建团队按钮，保留所有原有逻辑
                Button(
                    onClick = {
                        if (teamName.isNotEmpty() && teamDescription.isNotEmpty() && viewModel.currentUserId != null) {
                            val newTeam = Team(
                                id = 0,
                                name = teamName,
                                description = teamDescription,
                                members = mapOf(viewModel.currentUserId!! to Role.ADMIN.toString()),
                                tasks = emptyList(),
                                pendingJoinRequests = emptyList()
                            )
                            viewModel.createTeam(newTeam)
                            // 检查 ViewModel 中的错误消息来判断是否成功
                            if (viewModel.errorMessage == "团队创建成功") {
                                navController.popBackStack()
                            }
                        } else {
                            viewModel.errorMessage = "请填写团队名称和描述，或确保已登录"
                        }
                    },
                    enabled = viewModel.currentUserId != null, // 保留按钮可用性逻辑
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("创建团队")
                }

                // 错误信息显示，保留原有逻辑
                if (viewModel.errorMessage.isNotEmpty()) {
                    Text(
                        text = viewModel.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}