package com.example.software_project

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeletedTasksScreen(navController: NavHostController, viewModel: TaskViewModel = viewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("回收站") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 将“全部清空”按钮移至顶部操作栏
                    TextButton(
                        onClick = {
                            viewModel.deletedTasks.forEach { task ->
                                viewModel.permanentlyDeleteTask(task)
                            }
                        },
                        enabled = viewModel.deletedTasks.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = "全部清空",
                            tint = if (viewModel.deletedTasks.isNotEmpty()) MaterialTheme.colorScheme.error else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "全部清空",
                            color = if (viewModel.deletedTasks.isNotEmpty()) MaterialTheme.colorScheme.error else Color.Gray
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // 错误信息显示
            if (viewModel.errorMessage.isNotEmpty()) {
                Text(
                    text = viewModel.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // 处理空状态
            if (viewModel.deletedTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "回收站是空的",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                DeletedTaskList(
                    tasks = viewModel.deletedTasks,
                    onDelete = { task -> viewModel.permanentlyDeleteTask(task) },
                    onRestore = { task -> viewModel.restoreTask(task) }
                )
            }
        }
    }
}

@Composable
fun DeletedTaskList(
    tasks: List<Task>,
    onDelete: (Task) -> Unit,
    onRestore: (Task) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tasks, key = { it.id }) { task ->
            DeletedTaskRow(
                task = task,
                onDelete = { onDelete(task) },
                onRestore = { onRestore(task) }
            )
        }
    }
}

@Composable
fun DeletedTaskRow(
    task: Task,
    onDelete: () -> Unit,
    onRestore: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            // 左侧是回收站图标
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.RestoreFromTrash,
                    contentDescription = "已删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            // 标题使用删除线，并用灰色弱化显示
            headlineContent = {
                Text(
                    text = task.title,
                    textDecoration = TextDecoration.LineThrough,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            // 辅助文本显示倒计时
            supportingContent = {
                // 正确处理倒计时文本和颜色的逻辑
                CountdownText(deletedAtString = task.deletedAt)
            },
            // 右侧是“更多”操作菜单
            trailingContent = {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多操作")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("恢复") },
                            onClick = {
                                onRestore()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.RestoreFromTrash, contentDescription = "恢复") }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("永久删除", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                onDelete()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = "永久删除", tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
        )
    }
}

/**
 * 一个专门用于显示删除倒计时的 Composable,
 * 解决了在 try-catch 中调用 Composable 的问题。
 */
@Composable
private fun CountdownText(deletedAtString: String?) {
    val defaultColor = MaterialTheme.colorScheme.onSurfaceVariant
    val warningColor = MaterialTheme.colorScheme.error

    //    这个计算现在是一个纯逻辑操作。
    val (text, color) = remember(deletedAtString, defaultColor, warningColor) {
        if (deletedAtString == null) {
            // 如果传入的字符串为空，直接返回默认值
            return@remember "删除时间未知" to defaultColor
        }

        try {
            val deletionTime = ZonedDateTime.parse(deletedAtString)
            val now = ZonedDateTime.now()
            val daysSinceDeletion = Duration.between(deletionTime, now).toDays()
            val daysLeft = 7 - daysSinceDeletion

            val countdownText = when {
                daysLeft > 1 -> "将在 $daysLeft 天后被永久删除"
                daysLeft == 1L -> "将在 1 天后被永久删除"
                else -> "即将被永久删除"
            }

            val finalColor = if (daysLeft <= 2) warningColor else defaultColor

            // 返回计算出的文本和颜色
            countdownText to finalColor
        } catch (e: DateTimeParseException) {
            Log.e("CountdownText", "无法解析日期: $deletedAtString", e)
            // 解析失败时，返回错误文本和警告色
            "删除日期格式错误" to warningColor
        }
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color
    )
}