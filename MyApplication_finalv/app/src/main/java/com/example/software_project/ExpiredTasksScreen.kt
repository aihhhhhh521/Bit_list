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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiredTasksScreen(navController: NavHostController, viewModel: TaskViewModel = viewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("已过期任务") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        // 筛选过期任务的逻辑完整保留
        val expiredTasks = remember(viewModel.tasks) {
            val today = LocalDate.now()
            viewModel.tasks.filter { task ->
                try {
                    val dueDate = LocalDate.parse(task.dueDate)
                    // 如果截止日期在今天之前，且任务未完成，则视为过期
                    dueDate.isBefore(today) && task.status != TaskStatus.DONE
                } catch (e: Exception) {
                    false // 日期格式错误则不视为过期
                }
            }
        }

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
            if (expiredTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "没有已过期的任务，太棒了！",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                ExpiredTaskList(
                    tasks = expiredTasks,
                    onDelete = { task -> viewModel.deleteTask(task) },
                    onComplete = { task -> viewModel.markTaskAsCompleted(task) }
                )
            }
        }
    }
}

@Composable
fun ExpiredTaskList(
    tasks: List<Task>,
    onDelete: (Task) -> Unit,
    onComplete: (Task) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tasks, key = { it.id }) { task ->
            ExpiredTaskRow(
                task = task,
                onDelete = { onDelete(task) },
                onComplete = { onComplete(task) }
            )
        }
    }
}

@Composable
fun ExpiredTaskRow(
    task: Task,
    onDelete: () -> Unit,
    onComplete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            // 左侧是醒目的警告图标
            leadingContent = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "已过期",
                    tint = MaterialTheme.colorScheme.error
                )
            },
            // 标题加粗显示
            headlineContent = {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            },
            // 辅助文本显示截止日期
            supportingContent = {
                Text(
                    text = "截止于: ${task.dueDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            },
            // 右侧是“更多”操作菜单
            trailingContent = {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "更多操作",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    // 操作菜单的逻辑完整保留
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("标记为已完成") },
                            onClick = {
                                onComplete()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Check, contentDescription = "标记为已完成", tint = Color.Green.copy(alpha = 0.8f)) }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                onDelete()
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