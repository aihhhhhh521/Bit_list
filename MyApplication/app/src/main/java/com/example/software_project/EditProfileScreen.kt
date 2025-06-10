package com.example.software_project

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavHostController, viewModel: UserViewModel) {
    // --- 所有核心逻辑，包括 LaunchedEffect 和 ActivityResultLaunchers 均完整保留 ---
    LaunchedEffect(Unit) {
        if (viewModel.isLoggedIn) {
            viewModel.loadProfile()
        }
    }

    // 用于在UI上预览新选择的头像，在保存时再提交给ViewModel
    var selectedAvatarUri by remember { mutableStateOf<String?>(null) }

    // 当ViewModel加载完数据后，用其初始化本地预览URI
    LaunchedEffect(viewModel.avatarUri) {
        selectedAvatarUri = viewModel.avatarUri
    }

    val context = LocalContext.current
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            viewModel.errorMessage = "需要存储权限以选择头像"
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        // 选择图片后，仅更新本地预览URI状态
        uri?.toString()?.let {
            selectedAvatarUri = it
        }
    }
    // --- 核心逻辑结束 ---


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("修改个人信息") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp)
        ) {
            // --- 1. 头像编辑区 ---
            item {
                Box(contentAlignment = Alignment.Center) {
                    // 圆形头像框
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                // 点击头像区域选择图片
                                val permissionCheck = ContextCompat.checkSelfPermission(context, permissionToRequest)
                                if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    pickImageLauncher.launch("image/*")
                                } else {
                                    permissionLauncher.launch(permissionToRequest)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedAvatarUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(selectedAvatarUri),
                                contentDescription = "用户头像",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop // 保证图片填满圆形
                            )
                        } else {
                            Text(
                                text = "选择头像",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // 编辑小图标覆盖层
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.BottomEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑头像",
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // --- 2. 个人信息表单区 ---
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = viewModel.name,
                        onValueChange = { viewModel.name = it },
                        label = { Text("姓名") },
                        isError = viewModel.errorMessage.contains("姓名"),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = viewModel.email,
                        onValueChange = { viewModel.email = it },
                        label = { Text("邮箱") },
                        isError = viewModel.errorMessage.contains("邮箱"),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = viewModel.stuId,
                        onValueChange = { viewModel.stuId = it },
                        label = { Text("学号") },
                        isError = viewModel.errorMessage.contains("学号"),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = viewModel.school,
                        onValueChange = { viewModel.school = it },
                        label = { Text("学校") },
                        isError = viewModel.errorMessage.contains("学校"),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = viewModel.grade,
                        onValueChange = { viewModel.grade = it },
                        label = { Text("年级") },
                        isError = viewModel.errorMessage.contains("年级"),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = viewModel.birth,
                        onValueChange = { viewModel.birth = it },
                        label = { Text("生日 (YYYY-MM-DD)") },
                        isError = viewModel.errorMessage.contains("生日"),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- 3. 统一的保存按钮和状态显示 ---
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 错误信息显示
                    if (viewModel.errorMessage.isNotEmpty()) {
                        Text(
                            text = viewModel.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    // 加载指示器
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
                    }
                    // **唯一的保存按钮**
                    Button(
                        onClick = {
                            // 在调用ViewModel更新前，将本地预览的URI赋给ViewModel
                            viewModel.avatarUri = selectedAvatarUri
                            viewModel.updateProfile()
                            // 检查操作是否成功，如果成功则返回
                            if (viewModel.errorMessage == "修改成功") {
                                navController.popBackStack()
                            }
                        },
                        enabled = !viewModel.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存更改")
                    }
                }
            }
        }
    }
}