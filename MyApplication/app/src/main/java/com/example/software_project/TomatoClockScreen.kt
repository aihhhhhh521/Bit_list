package com.example.software_project

import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import java.time.ZonedDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TomatoClockScreen(navController: NavHostController, taskViewModel: TaskViewModel) {
    // --- 所有状态管理逻辑完整保留 ---
    var tomatoClock by remember { mutableStateOf<TomatoClockView?>(null) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var taskMenuExpanded by remember { mutableStateOf(false) }
    var timerState by remember { mutableStateOf(TomatoClockView.TimerState.IDLE) }

    val lifecycleOwner = LocalLifecycleOwner.current
    // --- 所有 DisposableEffect 核心逻辑完整保留 ---
    DisposableEffect(lifecycleOwner, tomatoClock) {
        val observer = LifecycleEventObserver { _, event ->
            tomatoClock?.let { clock -> if (event == Lifecycle.Event.ON_PAUSE && clock.isStarted && !clock.isPaused) clock.pause() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(navController, tomatoClock) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            tomatoClock?.let { clock ->
                if (clock.isStarted && !clock.isPaused && destination.route != "tomatoClock") {
                    clock.stop()
                }
            }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }
    // --- 核心逻辑结束 ---

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("番茄钟") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        // 使用一个垂直居中的Column来重新组织布局
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround // 使用SpaceAround来分布元素
        ) {
            // --- 1. 任务关联区 ---
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("关联任务", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = selectedTask?.title ?: "无",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    // 使用Box来包裹按钮和下拉菜单
                    Box {
                        OutlinedButton(onClick = { taskMenuExpanded = true }) {
                            Text("更换")
                        }
                        DropdownMenu(
                            expanded = taskMenuExpanded,
                            onDismissRequest = { taskMenuExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.75f)
                        ) {
                            DropdownMenuItem(text = { Text("不关联任务") }, onClick = { selectedTask = null; taskMenuExpanded = false })
                            taskViewModel.tasks.filter { it.status != TaskStatus.DONE }.forEach { task ->
                                DropdownMenuItem(text = { Text(task.title) }, onClick = { selectedTask = task; taskMenuExpanded = false })
                            }
                        }
                    }
                }
            }

            // --- 2. 计时器核心显示区 ---
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 显示当前状态
                val currentStateText = when (timerState) {
                    TomatoClockView.TimerState.IDLE -> "准备开始"
                    TomatoClockView.TimerState.WORKING -> "专注中..."
                    TomatoClockView.TimerState.BREAK -> "休息一下..."
                }
                Text(
                    text = currentStateText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                // 番茄钟视图
                Box(
                    modifier = Modifier.size(300.dp), // 给予一个固定的、较大的尺寸
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            TomatoClockView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                // 所有监听器逻辑完整保留
                                setFocusDataListener(object : TomatoClockView.FocusDataListener {
                                    override fun onFocusCompleted(durationInSeconds: Long) {
                                        val userId = taskViewModel.currentUserId
                                        if (userId != null) {
                                            taskViewModel.submitFocusData(
                                                TomatoFocusData(
                                                    userId = userId,
                                                    durationInSeconds = durationInSeconds,
                                                    timestamp = ZonedDateTime.now().toString(),
                                                    taskId = selectedTask?.id ?: 0
                                                )
                                            )
                                        }
                                    }
                                })
                                setStateListener(object : TomatoClockView.TimerStateListener {
                                    override fun onStateChanged(newState: TomatoClockView.TimerState) {
                                        timerState = newState
                                    }
                                })
                                tomatoClock = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }


            // --- 3. 控制按钮区 ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally)
            ) {
                // 开始/暂停/继续 按钮 (主要操作)
                Button(
                    onClick = {
                        tomatoClock?.let { clock ->
                            if (!clock.isStarted) clock.start() else {
                                if (clock.isPaused) clock.resume() else clock.pause()
                            }
                        }
                    },
                    enabled = timerState != TomatoClockView.TimerState.BREAK,
                    modifier = Modifier.height(56.dp).widthIn(min = 120.dp)
                ) {
                    val isWorking = timerState == TomatoClockView.TimerState.WORKING
                    val icon = if (isWorking && tomatoClock?.isPaused == false) Icons.Default.Pause else Icons.Default.PlayArrow
                    val text = when {
                        timerState == TomatoClockView.TimerState.IDLE -> "开始"
                        timerState == TomatoClockView.TimerState.BREAK -> "休息中"
                        tomatoClock?.isPaused == true -> "继续"
                        else -> "暂停"
                    }
                    Icon(imageVector = icon, contentDescription = text, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing));
                    Text(text, style = MaterialTheme.typography.titleMedium)
                }

                // 停止按钮 (次要操作)
                OutlinedButton(
                    onClick = { tomatoClock?.stop() },
                    enabled = timerState != TomatoClockView.TimerState.IDLE,
                    modifier = Modifier.height(56.dp).widthIn(min = 120.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "停止", modifier = Modifier.size(ButtonDefaults.IconSize));
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing));
                    Text("停止", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}