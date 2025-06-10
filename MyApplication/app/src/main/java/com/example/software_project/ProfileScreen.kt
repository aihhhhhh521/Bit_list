package com.example.software_project

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    viewModel: UserViewModel,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    // 首次进入时加载用户资料的逻辑被保留
    LaunchedEffect(Unit) {
        if (viewModel.isLoggedIn) {
            viewModel.loadProfile()
        }
    }

    // 使用 Scaffold 作为屏幕的根布局
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人") },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "打开导航")
                    }
                }
            )
        }
    ) { paddingValues ->
        // 使用 LazyColumn 使屏幕内容可滚动，并方便设置内边距
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(16.dp)
        ) {
            // 头像部分
            item {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { navController.navigate("editProfile") },
                    contentAlignment = Alignment.Center
                ) {
                    if (viewModel.avatarUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(viewModel.avatarUri),
                            contentDescription = "用户头像",
                            modifier = Modifier.fillMaxSize()
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
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 个人信息卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ProfileInfoRow(label = "姓名", value = viewModel.name)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        ProfileInfoRow(label = "邮箱", value = viewModel.email)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        ProfileInfoRow(label = "学校", value = viewModel.school)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        ProfileInfoRow(label = "年级", value = viewModel.grade)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        ProfileInfoRow(label = "学号", value = viewModel.stuId)
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        ProfileInfoRow(label = "生日", value = viewModel.birth)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 操作按钮
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { navController.navigate("editProfile") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("修改个人信息")
                    }
                    Button(
                        onClick = { navController.navigate("changePassword") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("修改密码")
                    }
                    Button(
                        onClick = { navController.navigate("data") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("数据统计")
                    }
                }
            }

            // 加载和错误状态显示
            item {
                if (viewModel.isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
                if (viewModel.errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        viewModel.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * 一个用于在个人资料页中显示一行信息的私有 Composable。
 * 它能确保标签和值左右对齐，使界面更整洁。
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
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.ifEmpty { "未设置" },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.End
        )
    }
}