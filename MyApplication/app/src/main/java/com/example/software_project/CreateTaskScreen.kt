package com.example.software_project

import android.app.TimePickerDialog
import android.widget.TimePicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateTaskScreen(
    navController: NavHostController,
    viewModel: TaskViewModel = viewModel()
) {
    // --- 所有状态管理逻辑完整保留 ---
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.LOW) }
    var tags by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(TaskStatus.TODO) }
    var dueDate by remember { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    var isTeamTask by remember { mutableStateOf(false) }
    var selectedTeamId by remember { mutableStateOf<Int?>(null) }
    var checklist by remember { mutableStateOf(listOf<ChecklistItem>()) }
    var newChecklistItemText by remember { mutableStateOf("") }
    var isRecurring by remember { mutableStateOf(false) }
    var recurringType by remember { mutableStateOf(RecurringType.WEEKLY) }
    val daysOfWeek = listOf("一", "二", "三", "四", "五", "六", "日")
    var recurringOnDays by remember { mutableStateOf(setOf<Int>()) }
    var recurringEndDateText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var reminderMethods by remember { mutableStateOf(setOf(ReminderMethod.IN_APP)) }
    var remindAtTimes by remember { mutableStateOf(listOf("1d", "1h")) }
    var newRemindAtTime by remember { mutableStateOf("") }
    var dailyReminderTime by remember { mutableStateOf<LocalTime?>(null) }
    var remindOnAppOpen by remember { mutableStateOf(false) }
    val showTimePicker = remember { mutableStateOf(false) }
    val context = LocalContext.current
    // --- 状态管理逻辑结束 ---

    // 时间选择对话框逻辑完整保留
    if (showTimePicker.value) {
        val onTimeSetListener = { _: TimePicker, hour: Int, minute: Int ->
            dailyReminderTime = LocalTime.of(hour, minute)
            showTimePicker.value = false
        }
        val currentTime = LocalTime.now()
        TimePickerDialog(
            context,
            onTimeSetListener,
            currentTime.hour,
            currentTime.minute,
            true
        ).show()
        showTimePicker.value = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新建任务") },
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
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题*") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("详情") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, label = { Text("截止日期 (YYYY-MM-DD)*") }, modifier = Modifier.fillMaxWidth())
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
                        OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("标签 (逗号分隔)") }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // --- 卡片3: 检查项 (Checklist) ---
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("检查项 (Checklist)", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        // 显示已添加的检查项
                        checklist.forEachIndexed { index, item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
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

            // --- 卡片4: 重复设置 ---
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("重复任务", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
                        }
                        // 条件渲染：只有当重复任务开关打开时才显示
                        if (isRecurring) {
                            Spacer(modifier = Modifier.height(12.dp))
                            RecurringTypeDropdown(selectedType = recurringType, onTypeChange = { recurringType = it })
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("每周重复于:", style = MaterialTheme.typography.bodyMedium)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                daysOfWeek.forEachIndexed { index, day ->
                                    val isSelected = recurringOnDays.contains(index + 1)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            recurringOnDays = if (isSelected) recurringOnDays - (index + 1) else recurringOnDays + (index + 1)
                                        },
                                        label = { Text(day) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = recurringEndDateText,
                                onValueChange = { },
                                label = { Text("重复结束日期 (可选)") },
                                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                                readOnly = true,
                                trailingIcon = { Icon(imageVector = Icons.Default.DateRange, contentDescription = "选择结束日期", modifier = Modifier.clickable { showDatePicker = true }) }
                            )
                            if (showDatePicker) {
                                val datePickerState = rememberDatePickerState()
                                DatePickerDialog(
                                    onDismissRequest = { showDatePicker = false },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                datePickerState.selectedDateMillis?.let { millis ->
                                                    val selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                                                    recurringEndDateText = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                                }
                                                showDatePicker = false
                                            },
                                            enabled = datePickerState.selectedDateMillis != null
                                        ) { Text("确认") }
                                    },
                                    dismissButton = { Button(onClick = { showDatePicker = false }) { Text("取消") } }
                                ) { DatePicker(state = datePickerState) }
                            }
                        }
                    }
                }
            }

            // --- 卡片5: 团队设置 ---
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("团队任务", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Checkbox(checked = isTeamTask, onCheckedChange = { isTeamTask = it })
                        }
                        if (isTeamTask) {
                            Spacer(modifier = Modifier.height(12.dp))
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedTextField(
                                    value = viewModel.teams.find { it.id == selectedTeamId }?.name ?: "选择团队",
                                    onValueChange = {},
                                    label = { Text("团队") },
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true,
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, "选择团队", Modifier.clickable { expanded = true }) }
                                )
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    viewModel.teams.forEach { team ->
                                        DropdownMenuItem(text = { Text(team.name) }, onClick = { selectedTeamId = team.id; expanded = false })
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- 卡片6: 提醒设置 ---
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("提醒设置", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))

                        // 截止前提醒
                        Text("截止前提醒 (例如 '1d', '2h', '30m'):", style = MaterialTheme.typography.bodyMedium)
                        FlowRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            remindAtTimes.forEach { time ->
                                InputChip(
                                    selected = false,
                                    onClick = { /* no action */ },
                                    label = { Text(time) },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "删除提醒",
                                            modifier = Modifier.size(18.dp).clickable {
                                                remindAtTimes = remindAtTimes.toMutableList().also { it.remove(time) }
                                            }
                                        )
                                    }
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = newRemindAtTime, onValueChange = { newRemindAtTime = it }, label = { Text("添加新提醒") }, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                if (newRemindAtTime.isNotBlank() && !remindAtTimes.contains(newRemindAtTime)) {
                                    remindAtTimes = remindAtTimes + newRemindAtTime
                                    newRemindAtTime = ""
                                }
                            }, enabled = newRemindAtTime.isNotBlank()) {
                                Icon(Icons.Default.Add, contentDescription = "添加")
                            }
                        }

                        // 每日固定时间提醒
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("每日固定时间提醒", modifier = Modifier.weight(1f))
                            Switch(checked = dailyReminderTime != null, onCheckedChange = { isChecked -> if (isChecked) { showTimePicker.value = true } else { dailyReminderTime = null } })
                        }
                        if (dailyReminderTime != null) {
                            Text(text = "提醒时间: ${dailyReminderTime?.format(DateTimeFormatter.ofPattern("HH:mm"))}", modifier = Modifier.padding(start = 16.dp))
                        }

                        // 每次打开应用提醒
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("每次打开应用时提醒", modifier = Modifier.weight(1f))
                            Switch(checked = remindOnAppOpen, onCheckedChange = { remindOnAppOpen = it })
                        }
                    }
                }
            }

            // --- 最终操作按钮 ---
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        // 创建任务的onClick逻辑完整保留
                        if (title.isBlank()) {
                            viewModel.errorMessage = "标题不能为空"
                            return@Button
                        }
                        if (isTeamTask && selectedTeamId == null) {
                            viewModel.errorMessage = "请选择一个团队"
                            return@Button
                        }
                        try {
                            LocalDate.parse(dueDate)
                        } catch (e: Exception) {
                            viewModel.errorMessage = "无效的日期格式 (YYYY-MM-DD)"
                            return@Button
                        }
                        val newTask = Task(
                            id = 0,
                            title = title,
                            description = description,
                            priority = priority,
                            tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                            status = status,
                            dueDate = dueDate,
                            order = viewModel.tasks.size,
                            isDeleted = false,
                            isTeamTask = isTeamTask,
                            teamId = if (isTeamTask) selectedTeamId else null,
                            parentTaskId = null,
                            weight = 1,
                            assignedTo = null,
                            checklist = checklist,
                            isRecurring = isRecurring,
                            recurringType = if(isRecurring) recurringType else null,
                            recurringOnDays = if(isRecurring && recurringOnDays.isNotEmpty()) recurringOnDays.toList() else null,
                            recurringEndDate = recurringEndDateText.ifBlank { null },
                            reminderSettings = ReminderSettings(
                                reminderMethods = reminderMethods.toList(),
                                remindAtTimes = remindAtTimes,
                                dailyReminderTime = dailyReminderTime?.format(DateTimeFormatter.ofPattern("HH:mm")),
                                remindOnAppOpen = remindOnAppOpen
                            )
                        )
                        viewModel.createTask(newTask)
                        if (viewModel.errorMessage == "任务创建成功") {
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("创建任务") }

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