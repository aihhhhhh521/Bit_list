package com.example.software_project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.xr.compose.testing.toDp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

// Drag-and-drop状态数据类 (保留)
data class DragState(
    val task: Task? = null,
    val pointerOffset: Offset = Offset(0f, 0f)
) {
    val isDragging: Boolean
        get() = task != null
}

enum class ViewMode { DAY, WEEK, MONTH }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavHostController,
    viewModel: TaskViewModel = viewModel(),
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    // --- 所有状态管理逻辑完整保留 ---
    var viewMode by remember { mutableStateOf(ViewMode.MONTH) }
    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val selectedDate = LocalDate.parse(viewModel.selectedDate, dateFormatter)
    val today = LocalDate.now()

    var dragState by remember { mutableStateOf(DragState()) }
    var dateCellBounds by remember { mutableStateOf(emptyMap<LocalDate, Rect>()) }

    val onNavigate: (period: ChronoUnit, amount: Long) -> Unit = { period, amount ->
        val newDate = when (period) {
            ChronoUnit.MONTHS -> selectedDate.plusMonths(amount)
            ChronoUnit.WEEKS -> selectedDate.plusWeeks(amount)
            ChronoUnit.DAYS -> selectedDate.plusDays(amount)
            else -> selectedDate
        }
        viewModel.selectedDate = newDate.format(dateFormatter)
    }
    // --- 状态管理逻辑结束 ---

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日程") },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "打开导航")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                // 1. 使用 SegmentedButton 进行视图切换
                val viewOptions = listOf(ViewMode.DAY, ViewMode.WEEK, ViewMode.MONTH)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    viewOptions.forEachIndexed { index, mode ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = viewOptions.size),
                            onClick = { viewMode = mode },
                            selected = viewMode == mode
                        ) {
                            Text(when(mode) {
                                ViewMode.DAY -> "日"
                                ViewMode.WEEK -> "周"
                                ViewMode.MONTH -> "月"
                            })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // 2. 日历视图
                CalendarView(
                    viewMode = viewMode,
                    selectedDate = selectedDate,
                    tasks = viewModel.tasks,
                    onDateSelected = { date -> viewModel.selectedDate = date.format(dateFormatter) },
                    onDateBoundsChanged = { bounds -> dateCellBounds = bounds },
                    dragTargetDate = findTargetDate(dragState.pointerOffset, dateCellBounds),
                    onNavigate = onNavigate,
                    today = today // 传入当天日期
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 3. 任务列表区域分隔
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("待办任务", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                if (viewModel.errorMessage.isNotEmpty()) {
                    Text(
                        text = viewModel.errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // 4. 任务列表
                LazyColumn {
                    items(viewModel.tasks.filter { it.status != TaskStatus.DONE }, key = { it.id }) { task ->
                        val highlightColor = getHighlightColor(task, viewMode, today)
                        TaskItem(
                            task = task,
                            highlightColor = highlightColor,
                            onDragStart = { dragState = dragState.copy(task = task) },
                            onDrag = { change, dragAmount ->
                                dragState = dragState.copy(pointerOffset = dragState.pointerOffset + dragAmount)
                                change.consume()
                            },
                            onDragEnd = {
                                val targetDate = findTargetDate(dragState.pointerOffset, dateCellBounds)
                                if (targetDate != null) {
                                    viewModel.updateTaskDueDate(task.id, targetDate.format(dateFormatter))
                                }
                                dragState = DragState()
                            },
                            onDragCancel = { dragState = DragState() }
                        )
                    }
                }
            }

            // 拖拽时的“幽灵”视图 (保留)
            if (dragState.isDragging) {
                val task = dragState.task!!
                // 调整偏移量使拖动时指针大致位于卡片中心
                val cardWidth = 200.dp
                val cardHeight = 80.dp
                // 在Composable上下文中进行单位转换
                val density = LocalDensity.current
                val offsetX_dp = with(density) { dragState.pointerOffset.x.toDp() }
                val offsetY_dp = with(density) { dragState.pointerOffset.y.toDp() }

                Card(
                    modifier = Modifier
                        .offset(x = offsetX_dp - (cardWidth / 2), y = offsetY_dp - (cardHeight / 2))
                        .width(cardWidth)
                        .height(cardHeight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = task.title,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(text = "至: ${task.dueDate}", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}


// --- 所有子组件和辅助函数完整保留并优化 ---

@Composable
fun CalendarHeader(
    selectedDate: LocalDate,
    viewMode: ViewMode,
    onNavigate: (period: ChronoUnit, amount: Long) -> Unit
) {
    val headerText = when (viewMode) {
        ViewMode.MONTH -> selectedDate.format(DateTimeFormatter.ofPattern("yyyy 年 MMMM", Locale.CHINA))
        ViewMode.WEEK -> {
            val weekFields = WeekFields.of(Locale.CHINA)
            val weekNumber = selectedDate.get(weekFields.weekOfWeekBasedYear())
            "${selectedDate.year}年, 第 $weekNumber 周"
        }
        ViewMode.DAY -> selectedDate.format(DateTimeFormatter.ofPattern("yyyy 年 MMMM d", Locale.CHINA))
    }

    val navigationPeriod = when(viewMode) {
        ViewMode.MONTH -> ChronoUnit.MONTHS
        ViewMode.WEEK -> ChronoUnit.WEEKS
        ViewMode.DAY -> ChronoUnit.DAYS
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = { onNavigate(navigationPeriod, -1) }) {
            Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "上一个")
        }
        Text(text = headerText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        IconButton(onClick = { onNavigate(navigationPeriod, 1) }) {
            Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "下一个")
        }
    }
}

@Composable
fun CalendarView(
    viewMode: ViewMode,
    selectedDate: LocalDate,
    tasks: List<Task>,
    onDateSelected: (LocalDate) -> Unit,
    onDateBoundsChanged: (Map<LocalDate, Rect>) -> Unit,
    dragTargetDate: LocalDate?,
    onNavigate: (period: ChronoUnit, amount: Long) -> Unit,
    today: LocalDate // 新增参数
) {
    when (viewMode) {
        ViewMode.DAY -> DayView(selectedDate, tasks, viewMode, onNavigate)
        ViewMode.WEEK -> WeekView(selectedDate, tasks, onDateSelected, onDateBoundsChanged, dragTargetDate, viewMode, onNavigate, today)
        ViewMode.MONTH -> MonthView(selectedDate, tasks, onDateSelected, onDateBoundsChanged, dragTargetDate, viewMode, onNavigate, today)
    }
}

@Composable
fun MonthView(
    selectedDate: LocalDate,
    tasks: List<Task>,
    onDateSelected: (LocalDate) -> Unit,
    onDateBoundsChanged: (Map<LocalDate, Rect>) -> Unit,
    dragTargetDate: LocalDate?,
    viewMode: ViewMode,
    onNavigate: (period: ChronoUnit, amount: Long) -> Unit,
    today: LocalDate // 新增参数
) {
    // ... (内部逻辑完整保留)
    val yearMonth = YearMonth.from(selectedDate)
    val firstDayOfMonth = yearMonth.atDay(1)
    val firstDayOfFirstWeek = firstDayOfMonth.with(WeekFields.of(Locale.getDefault()).firstDayOfWeek)
    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val localDateCellBounds = remember { mutableStateOf(mutableMapOf<LocalDate, Rect>()) }

    Column {
        CalendarHeader(selectedDate = selectedDate, viewMode = viewMode, onNavigate = onNavigate)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            val daysOfWeek = arrayOf("日", "一", "二", "三", "四", "五", "六")
            for (day in daysOfWeek) {
                Text(text = day, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        var currentDate = firstDayOfFirstWeek
        repeat(6) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) {
                    val date = currentDate
                    val hasTask = tasks.any { it.dueDate.startsWith(date.format(dateFormatter)) }
                    val isCurrentMonth = YearMonth.from(date) == yearMonth
                    val isBeingDraggedOver = date == dragTargetDate

                    DayCell(
                        date = date,
                        isCurrentMonth = isCurrentMonth,
                        hasTask = hasTask,
                        isDraggedOver = isBeingDraggedOver,
                        isToday = (date == today), // 传入是否是今天
                        modifier = Modifier
                            .weight(1f)
                            .onGloballyPositioned {
                                localDateCellBounds.value[date] = it.getGlobalBounds()
                                onDateBoundsChanged(localDateCellBounds.value)
                            },
                        onClick = { onDateSelected(date) }
                    )
                    currentDate = currentDate.plusDays(1)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun WeekView(
    selectedDate: LocalDate,
    tasks: List<Task>,
    onDateSelected: (LocalDate) -> Unit,
    onDateBoundsChanged: (Map<LocalDate, Rect>) -> Unit,
    dragTargetDate: LocalDate?,
    viewMode: ViewMode,
    onNavigate: (period: ChronoUnit, amount: Long) -> Unit,
    today: LocalDate // 新增参数
) {
    // ... (内部逻辑完整保留)
    val firstDayOfWeek = selectedDate.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val localDateCellBounds = remember { mutableStateOf(mutableMapOf<LocalDate, Rect>()) }

    Column {
        CalendarHeader(selectedDate = selectedDate, viewMode = viewMode, onNavigate = onNavigate)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 0..6) {
                val date = firstDayOfWeek.plusDays(i.toLong())
                val hasTask = tasks.any { it.dueDate.startsWith(date.format(dateFormatter)) }
                val isBeingDraggedOver = date == dragTargetDate

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .onGloballyPositioned {
                            localDateCellBounds.value[date] = it.getGlobalBounds()
                            onDateBoundsChanged(localDateCellBounds.value)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    DayCell(
                        date = date,
                        isCurrentMonth = true,
                        hasTask = hasTask,
                        isDraggedOver = isBeingDraggedOver,
                        isToday = (date == today), // 传入是否是今天
                        modifier = Modifier.aspectRatio(1f),
                        onClick = { onDateSelected(date) }
                    )
                }
            }
        }
    }
}

@Composable
fun DayView(selectedDate: LocalDate, tasks: List<Task>, viewMode: ViewMode, onNavigate: (period: ChronoUnit, amount: Long) -> Unit) {
    // ... (内部逻辑完整保留)
    val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val tasksForDay = tasks.filter { it.dueDate.startsWith(selectedDate.format(isoFormatter)) }

    Column(modifier = Modifier.fillMaxWidth()) {
        CalendarHeader(selectedDate = selectedDate, viewMode = viewMode, onNavigate = onNavigate)
        Text(
            text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy", Locale.CHINA)),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        if (tasksForDay.isEmpty()) {
            Text("当天没有任务。", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        } else {
            tasksForDay.forEach { task ->
                Text("${task.title} - 优先级: ${task.priority}", modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

// 优化的 DayCell
@Composable
fun DayCell(
    date: LocalDate,
    isCurrentMonth: Boolean,
    hasTask: Boolean,
    isDraggedOver: Boolean,
    isToday: Boolean, // 新增参数
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isDraggedOver -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        hasTask -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    val textColor = when {
        isToday -> MaterialTheme.colorScheme.onPrimary
        isCurrentMonth -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.outline
    }
    val todayModifier = if (isToday) {
        modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
    } else {
        modifier.border(1.dp, if(isDraggedOver) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
    }

    Box(
        modifier = todayModifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = textColor,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            fontWeight = if(isToday) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// 优化的 TaskItem
@Composable
fun TaskItem(
    task: Task,
    highlightColor: Color,
    onDragStart: () -> Unit,
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel
                )
            },
        colors = CardDefaults.cardColors(containerColor = highlightColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "截止: ${task.dueDate} - 优先级: ${task.priority}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// 辅助函数 (保留)
@Composable // 1. 标记为 @Composable
fun getHighlightColor(task: Task, viewMode: ViewMode, today: LocalDate): Color {
    // 2. 先从Composable上下文中获取颜色值
    val imminentColor = MaterialTheme.colorScheme.tertiaryContainer
    val defaultColor = MaterialTheme.colorScheme.surfaceVariant
    val overdueColor = Color.Gray.copy(alpha = 0.3f)

    // 3. 将 try-catch 仅用于包裹可能抛出异常的解析代码
    val dueDate = try {
        LocalDate.parse(task.dueDate, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (e: Exception) {
        // 解析失败则直接返回默认颜色
        return defaultColor
    }

    // 4. 使用解析成功后的 dueDate 进行后续逻辑判断
    val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate)
    if (daysUntilDue < 0) return overdueColor

    val isImminent = when (viewMode) {
        ViewMode.DAY -> daysUntilDue in 0..3
        ViewMode.WEEK -> daysUntilDue in 0..(2 * 7)
        ViewMode.MONTH -> daysUntilDue in 0..30
    }

    return if (isImminent) imminentColor else defaultColor
}

private fun findTargetDate(dragPosition: Offset, dateBounds: Map<LocalDate, Rect>): LocalDate? {
    return dateBounds.entries.find { (_, bounds) ->
        dragPosition in bounds
    }?.key
}

fun LayoutCoordinates.getGlobalBounds(): Rect {
    val rootPosition = this.positionInRoot()
    return Rect(rootPosition, this.size.toSize())
}

private fun Task.getDragAnchoredPosition(): Offset {
    return Offset(20f, 20f)
}