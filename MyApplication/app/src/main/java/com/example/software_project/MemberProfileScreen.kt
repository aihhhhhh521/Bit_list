package com.example.software_project

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberProfileScreen(
    userId: Int,
    navController: NavHostController,
    viewModel: UserViewModel
) {
    // 首次进入时，加载指定ID的用户资料，此核心逻辑完整保留
    LaunchedEffect(key1 = userId) {
        viewModel.loadProfileForViewing(userId)
    }

    // 从ViewModel获取状态
    val userProfile = viewModel.viewedUserProfile
    val isLoading = viewModel.isViewingProfile
    val errorMessage = viewModel.viewProfileError

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("成员资料") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        // 使用Box来居中显示加载和错误状态
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                // 1. 加载中状态
                isLoading -> {
                    CircularProgressIndicator()
                }
                // 2. 错误状态
                errorMessage.isNotEmpty() -> {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                // 3. 成功获取数据状态
                userProfile != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // --- 身份核心区 ---
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (userProfile.avatarUri != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(userProfile.avatarUri),
                                    contentDescription = "成员头像",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "默认头像",
                                    modifier = Modifier.size(72.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = userProfile.name,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = userProfile.email,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(32.dp))

                        // --- 详细信息区 ---
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                ProfileInfoRow("学校:", userProfile.school)
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                ProfileInfoRow("年级:", userProfile.grade)
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                ProfileInfoRow("学号:", userProfile.stuId)
                            }
                        }
                    }
                }
                // 4. 加载完成但数据为空的罕见情况
                else -> {
                    Text(
                        text = "无法找到该用户的信息。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * 辅助组件，用于对齐显示一行信息，完整保留
 */
@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.ifEmpty { "未提供" },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.End
        )
    }
}