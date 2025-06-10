package com.example.software_project

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// 主屏幕 Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    navController: NavHostController,
    viewModel: TaskViewModel = viewModel(),
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    // --- 从原始 TaskScreen.kt 中保留的状态和对话框逻辑 ---
    var selectedTaskId by remember { mutableStateOf<Int?>(null) }
    var showSubTaskDialog by remember { mutableStateOf(false) }
    var parentTaskForSubtask by remember { mutableStateOf<Task?>(null) }
    var subTaskTitle by remember { mutableStateOf("") }
    var subTaskDescription by remember { mutableStateOf("") }
    var subTaskWeight by remember { mutableStateOf("1") }
    var subTaskChecklist by remember { mutableStateOf(listOf<ChecklistItem>()) }
    var newChecklistItemText by remember { mutableStateOf("") }

    val attachmentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                selectedTaskId?.let { taskId ->
                    viewModel.uploadAttachmentFromUri(taskId, it)
                }
            }
        }
    )

    if (showSubTaskDialog && parentTaskForSubtask != null) {
        AlertDialog(
            onDismissRequest = { showSubTaskDialog = false },
            title = { Text("为 \"${parentTaskForSubtask?.title}\" 创建子任务") },
            text = {
                // 完整的对话框内容
                LazyColumn {
                    item {
                        Column {
                            OutlinedTextField(value = subTaskTitle, onValueChange = { subTaskTitle = it }, label = { Text("子任务标题") }, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = subTaskDescription, onValueChange = { subTaskDescription = it }, label = { Text("描述") }, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = subTaskWeight, onValueChange = { subTaskWeight = it }, label = { Text("权重 (整数)") }, modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("检查项 (Checklist)", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    items(subTaskChecklist.size) { index ->
                        val item = subTaskChecklist[index]
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
                            Text(item.text, modifier = Modifier.weight(1f))
                            IconButton(onClick = { subTaskChecklist = subTaskChecklist.toMutableList().also { it.removeAt(index) } }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除检查项", tint = Color.Gray)
                            }
                        }
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            OutlinedTextField(value = newChecklistItemText, onValueChange = { newChecklistItemText = it }, label = { Text("新检查项内容") }, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                if (newChecklistItemText.isNotBlank()) {
                                    val newItem = ChecklistItem(id = 0, text = newChecklistItemText, isCompleted = false)
                                    subTaskChecklist = subTaskChecklist + newItem
                                    newChecklistItemText = ""
                                }
                            }) { Icon(Icons.Default.Add, contentDescription = "添加检查项") }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subTaskTitle.isNotBlank()) {
                            viewModel.createSubTask(
                                parentTaskForSubtask!!.id,
                                Task(
                                    id = 0,
                                    title = subTaskTitle,
                                    description = subTaskDescription,
                                    priority = Priority.LOW,
                                    tags = emptyList(),
                                    status = TaskStatus.TODO,
                                    dueDate = parentTaskForSubtask!!.dueDate,
                                    order = 0,
                                    weight = subTaskWeight.toIntOrNull() ?: 1,
                                    checklist = subTaskChecklist
                                )
                            )
                            showSubTaskDialog = false
                            // 重置状态
                            subTaskTitle = ""
                            subTaskDescription = ""
                            subTaskWeight = "1"
                            subTaskChecklist = emptyList()
                            newChecklistItemText = ""
                            parentTaskForSubtask = null
                        } else {
                            viewModel.errorMessage = "子任务标题不能为空"
                        }
                    }
                ) { Text("确认") }
            },
            dismissButton = { Button(onClick = { showSubTaskDialog = false }) { Text("取消") } }
        )
    }
    // --- 状态和对话框逻辑结束 ---

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("任务") },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "打开导航")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.sortByPriority() }) {
                        Icon(
                            if (viewModel.isSortByPriority) Icons.Default.SortByAlpha else Icons.Default.Sort,
                            contentDescription = if (viewModel.isSortByPriority) "按顺序排序" else "按优先级排序"
                        )
                    }
                    IconButton(onClick = { navController.navigate("tomatoClock") }) {
                        Icon(Icons.Default.Timer, contentDescription = "打开番茄钟")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("create") }) {
                Icon(Icons.Default.Add, contentDescription = "新建任务")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (viewModel.errorMessage.isNotEmpty()) {
                    Text(
                        text = viewModel.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                val listState = rememberLazyListState()
                var showPermissionsDialog by remember { mutableStateOf(false) }
                var selectedAttachmentForPermissions by remember { mutableStateOf<Pair<Task, Attachment>?>(null) }

                if (showPermissionsDialog && selectedAttachmentForPermissions != null) {
                    val (task, attachment) = selectedAttachmentForPermissions!!
                    val team = viewModel.teams.find { it.id == task.teamId }
                    if (team != null) {
                        AttachmentPermissionsDialog(
                            team = team,
                            attachment = attachment,
                            onDismiss = { showPermissionsDialog = false },
                            onSave = { newPermissions ->
                                viewModel.updateAttachmentPermissions(task.id, attachment.id, newPermissions)
                                showPermissionsDialog = false
                            }
                        )
                    }
                }

                OptimizedTaskList(
                    tasks = viewModel.tasks.filter { it.status != TaskStatus.DONE && it.parentTaskId == null },
                    listState = listState,
                    viewModel = viewModel,
                    navController = navController,
                    onAddSubTask = { task ->
                        parentTaskForSubtask = task
                        showSubTaskDialog = true
                    },
                    onAddAttachment = { task ->
                        selectedTaskId = task.id
                        attachmentPickerLauncher.launch("*/*")
                    },
                    onManageAttachmentPermissions = { task, attachment ->
                        selectedAttachmentForPermissions = Pair(task, attachment)
                        showPermissionsDialog = true
                    }
                )
            }

            if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun OptimizedTaskList(
    tasks: List<Task>,
    listState: LazyListState,
    viewModel: TaskViewModel,
    navController: NavHostController,
    onAddSubTask: (Task) -> Unit,
    onAddAttachment: (Task) -> Unit,
    onManageAttachmentPermissions: (Task, Attachment) -> Unit,
) {
    var draggedTask by remember { mutableStateOf<Task?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        listState.layoutInfo.visibleItemsInfo
                            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
                            ?.also {
                                draggedTask = tasks.getOrNull(it.index)
                            }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch { listState.scrollBy(dragAmount.y) }
                    },
                    onDragEnd = {
                        draggedTask = null
                        // A complete drag-and-drop reorder implementation is complex
                        // and would require updating the ViewModel state.
                    },
                    onDragCancel = {
                        draggedTask = null
                    }
                )
            },
        contentPadding = PaddingValues(bottom = 80.dp) // For FAB
    ) {
        items(tasks, key = { it.id }) { task ->
            Box(modifier = Modifier.background(if (draggedTask?.id == task.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)) {
                OptimizedTaskItem(
                    task = task,
                    viewModel = viewModel,
                    navController = navController,
                    onAddSubTask = onAddSubTask,
                    onAddAttachment = onAddAttachment,
                    onManageAttachmentPermissions = onManageAttachmentPermissions,
                )
            }
        }
    }
}

@Composable
fun OptimizedTaskItem(
    task: Task,
    viewModel: TaskViewModel,
    navController: NavHostController,
    onAddSubTask: (Task) -> Unit,
    onAddAttachment: (Task) -> Unit,
    onManageAttachmentPermissions: (Task, Attachment) -> Unit,
) {
    val canModify = viewModel.canCurrentUserModifyTask(task)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.status == TaskStatus.DONE) TextDecoration.LineThrough else null
                )
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Chip(label = task.priority.name, color = task.priority.toColor())
                        Chip(label = task.dueDate, icon = Icons.Default.DateRange)
                        if (task.isTeamTask && task.teamId != null) {
                            val teamName = viewModel.teams.find { it.id == task.teamId }?.name ?: "未知团队"
                            Chip(label = teamName, icon = Icons.Default.Group)
                        }
                    }
                }
            },
            trailingContent = {
                if (canModify) {
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多操作")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(text = { Text("编辑") }, onClick = { navController.navigate("editTask/${task.id}"); menuExpanded = false }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                            DropdownMenuItem(text = { Text("标记为进行中") }, onClick = { viewModel.markTaskAsInProgress(task); menuExpanded = false }, leadingIcon = { Icon(Icons.Default.Sync, null) })
                            DropdownMenuItem(text = { Text("标记为已完成") }, onClick = { viewModel.markTaskAsCompleted(task); menuExpanded = false }, leadingIcon = { Icon(Icons.Default.Check, null) })
                            DropdownMenuItem(text = { Text("添加子任务") }, onClick = { onAddSubTask(task); menuExpanded = false }, leadingIcon = { Icon(Icons.Default.PlaylistAdd, null) })
                            DropdownMenuItem(text = { Text("添加附件") }, onClick = { onAddAttachment(task); menuExpanded = false }, leadingIcon = { Icon(Icons.Default.Upload, null) })
                            Divider()
                            DropdownMenuItem(text = { Text("删除", color = MaterialTheme.colorScheme.error) }, onClick = { viewModel.deleteTask(task); menuExpanded = false }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) })
                        }
                    }
                }
            }
        )

        val subTasks = viewModel.tasks.filter { it.parentTaskId == task.id && !it.isDeleted }
        val attachments = task.attachments.filter { !it.isDeleted }
        val isParentTask = viewModel.tasks.any { it.parentTaskId == task.id }

        if (isParentTask || attachments.isNotEmpty()) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isParentTask) {
                    val progress = viewModel.calculateParentTaskProgress(task.id)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(String.format("%.0f%%", progress), style = MaterialTheme.typography.labelSmall)
                    }
                }
                subTasks.forEach { subTask ->
                    SubTaskRow(task = subTask, viewModel = viewModel, canModify = viewModel.canCurrentUserModifyTask(subTask))
                }
                attachments.forEach { attachment ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "${attachment.fileName} (${attachment.sizeInBytes / 1024}KB)", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { viewModel.downloadAttachment(task.id, attachment.id, attachment.fileName) }) {
                            Icon(Icons.Default.Download, contentDescription = "下载", tint = MaterialTheme.colorScheme.primary)
                        }
                        if (canModify) {
                            IconButton(onClick = { viewModel.deleteAttachment(task.id, attachment.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        val team = viewModel.teams.find { it.id == task.teamId }
                        if (task.isTeamTask && team != null && team.members[viewModel.currentUserId] == Role.ADMIN.toString()) {
                            IconButton(onClick = { onManageAttachmentPermissions(task, attachment) }) {
                                Icon(Icons.Default.ManageAccounts, contentDescription = "管理权限", tint = Color.DarkGray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubTaskRow(task: Task, viewModel: TaskViewModel, canModify: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.SubdirectoryArrowRight, contentDescription = "子任务", modifier = Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 14.sp,
                    textDecoration = if (task.status == TaskStatus.DONE) TextDecoration.LineThrough else null
                )
                task.description.takeIf { it.isNotBlank() }?.let {
                    Text(text = it, fontSize = 12.sp, color = Color.Gray)
                }
            }
            if (canModify) {
                Checkbox(
                    checked = task.status == TaskStatus.DONE,
                    onCheckedChange = {
                        if (it) viewModel.markTaskAsCompleted(task)
                        else {
                            val updatedTask = task.copy(status = TaskStatus.TODO)
                            viewModel.updateTask(updatedTask)
                        }
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun Chip(label: String, icon: ImageVector? = null, color: Color = MaterialTheme.colorScheme.surfaceVariant) {
    Surface(
        color = color.copy(alpha = 0.5f),
        shape = CircleShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(16.dp))
            }
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun Priority.toColor(): Color {
    return when (this) {
        Priority.HIGHEST -> Color(0xFFD32F2F)
        Priority.HIGH -> Color(0xFFF57C00)
        Priority.MEDIUM -> Color(0xFF1976D2)
        Priority.LOW -> Color(0xFF388E3C)
        Priority.LOWEST -> Color(0xFF757575)
    }
}