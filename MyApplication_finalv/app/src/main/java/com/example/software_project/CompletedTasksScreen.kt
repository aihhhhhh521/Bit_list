package com.example.software_project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedTasksScreen(navController: NavHostController, viewModel: TaskViewModel = viewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("已完成任务") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        // 从ViewModel中筛选出已完成的任务
        val completedTasks = viewModel.tasks.filter { it.status == TaskStatus.DONE }

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
            if (completedTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "还没有已完成的任务",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                CompletedTaskList(
                    tasks = completedTasks,
                    onDelete = { task -> viewModel.deleteTask(task) },
                    onRestore = { task -> viewModel.restoreTask(task) }
                )
            }
        }
    }
}

@Composable
fun CompletedTaskList(
    tasks: List<Task>,
    onDelete: (Task) -> Unit,
    onRestore: (Task) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tasks, key = { it.id }) { task ->
            CompletedTaskRow(
                task = task,
                onDelete = { onDelete(task) },
                onRestore = { onRestore(task) }
            )
        }
    }
}

@Composable
fun CompletedTaskRow(
    task: Task,
    onDelete: (Task) -> Unit,
    onRestore: (Task) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        // 使用 ListItem 来构建清晰的条目
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            // 左侧是对勾图标，明确表示已完成
            leadingContent = {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "已完成",
                    tint = Color.Green.copy(alpha = 0.8f)
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
            // 辅助文本显示截止日期
            supportingContent = {
                Text(
                    text = "截止于: ${task.dueDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            // 右侧是“更多”操作菜单
            trailingContent = {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多操作")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("恢复任务") },
                            onClick = {
                                onRestore(task)
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Restore, contentDescription = "恢复") }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("彻底删除", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                onDelete(task)
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
        )
    }
}