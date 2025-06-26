package com.example.software_project

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    navController: NavHostController,
    taskId: Int,
    viewModel: TaskViewModel = viewModel()
) {
    val taskToEdit = viewModel.tasks.find { it.id == taskId }

    // 如果任务未找到，则显示错误信息并返回
    if (taskToEdit == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("错误") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("未找到要编辑的任务。", style = MaterialTheme.typography.bodyLarge)
            }
        }
        return
    }

    // --- 状态初始化：使用已有任务数据 ---
    // 使用 taskToEdit 作为 remember 的 key，确保在 taskToEdit 变化时状态能被重置
    var title by remember(taskToEdit) { mutableStateOf(taskToEdit.title) }
    var description by remember(taskToEdit) { mutableStateOf(taskToEdit.description) }
    var priority by remember(taskToEdit) { mutableStateOf(taskToEdit.priority) }
    var tags by remember(taskToEdit) { mutableStateOf(taskToEdit.tags.joinToString(",")) }
    var dueDate by remember(taskToEdit) { mutableStateOf(taskToEdit.dueDate) }
    var status by remember(taskToEdit) { mutableStateOf(taskToEdit.status) }
    var checklist by remember(taskToEdit) { mutableStateOf(taskToEdit.checklist) }
    var newChecklistItemText by remember { mutableStateOf("") }
    // --- 状态初始化结束 ---

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑任务") },
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 卡片1: 基本信息 ---
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("基本信息", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("标题*") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("详情") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = dueDate,
                            onValueChange = { dueDate = it },
                            label = { Text("截止日期 (YYYY-MM-DD)*") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // --- 卡片2: 任务属性 ---
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("任务属性", style = MaterialTheme.typography.titleMedium)
                        PriorityDropdown(selectedPriority = priority, onPriorityChange = { priority = it })
                        StatusDropdown(selectedStatus = status, onStatusChange = { status = it })
                        OutlinedTextField(
                            value = tags,
                            onValueChange = { tags = it },
                            label = { Text("标签 (逗号分隔)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // --- 卡片3: 检查项 (Checklist) ---
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("检查项 (Checklist)", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        // 显示已有的检查项
                        checklist.forEachIndexed { index, item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = item.isCompleted,
                                    onCheckedChange = { isChecked ->
                                        val newList = checklist.toMutableList()
                                        newList[index] = item.copy(isCompleted = isChecked)
                                        checklist = newList
                                    }
                                )
                                Text(item.text, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    checklist = checklist.toMutableList().also { it.removeAt(index) }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除检查项", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        // 添加新检查项的输入框
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            OutlinedTextField(value = newChecklistItemText, onValueChange = { newChecklistItemText = it }, label = { Text("新检查项内容") }, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                if (newChecklistItemText.isNotBlank()) {
                                    val newItem = ChecklistItem(id = 0, text = newChecklistItemText, isCompleted = false)
                                    checklist = checklist + newItem
                                    newChecklistItemText = ""
                                }
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "添加检查项")
                            }
                        }
                    }
                }
            }

            // --- 最终操作按钮 ---
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        try {
                            LocalDate.parse(dueDate) // 验证日期格式
                            val updatedTask = taskToEdit.copy(
                                title = title,
                                description = description,
                                priority = priority,
                                tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                dueDate = dueDate,
                                status = status,
                                checklist = checklist
                            )
                            viewModel.updateTask(updatedTask)
                            navController.popBackStack()
                        } catch (e: Exception) {
                            viewModel.errorMessage = "无效的日期格式 (YYYY-MM-DD)"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("保存更改")
                }

                if (viewModel.errorMessage.isNotEmpty()) {
                    Text(
                        text = viewModel.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}