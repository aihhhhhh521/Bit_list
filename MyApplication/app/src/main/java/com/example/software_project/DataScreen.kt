package com.example.software_project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.axis.formatter.PercentageFormatAxisValueFormatter
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(navController: NavHostController, viewModel: TaskViewModel) {
    LaunchedEffect(Unit) {
        viewModel.loadUserStats()
    }

    val userStats = viewModel.userStats

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据统计") },
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
            // 数据总览卡片
            item {
                val totalFocusHours = (userStats?.totalFocusTime ?: 0L) / 3600.0
                OverviewCard(
                    completedTasks = userStats?.completedTasks ?: 0,
                    inProgressTasks = userStats?.inProgressTasks ?: 0,
                    totalTeams = userStats?.totalTeams ?: 0,
                    totalFocusHours = totalFocusHours
                )
            }

            // 加载指示器
            if (viewModel.isLoading) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            // 任务完成率趋势图卡片
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("任务完成率趋势", style = MaterialTheme.typography.titleLarge)
                        val trendOptions = mapOf("8周" to 8, "12周" to 12, "24周" to 24)
                        Text("显示长度:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            trendOptions.forEach { (label, length) ->
                                FilterChip(
                                    selected = viewModel.trendLength == length,
                                    onClick = { viewModel.updateStatsView(newTrendLength = length) },
                                    label = { Text(label) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        if (userStats?.completionRateTrend?.isNotEmpty() == true) {
                            val lineChartProducer = ChartEntryModelProducer(
                                userStats.completionRateTrend.mapIndexed { index, dataPoint ->
                                    entryOf(index.toFloat(), dataPoint.rate * 100)
                                }
                            )
                            Chart(
                                chart = lineChart(),
                                chartModelProducer = lineChartProducer,
                                startAxis = rememberStartAxis(
                                    title = "完成率 (%)",
                                    valueFormatter = PercentageFormatAxisValueFormatter()
                                ),
                                bottomAxis = rememberBottomAxis(
                                    title = "时间段",
                                    valueFormatter = { value, _ -> userStats.completionRateTrend[value.toInt()].period }
                                ),
                                modifier = Modifier.height(200.dp)
                            )
                        } else if (!viewModel.isLoading) {
                            Text("暂无足够数据生成趋势图。", modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
                        }
                    }
                }
            }

            // 近期任务状态分布图卡片 (包含所有筛选逻辑)
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("近期任务状态分布", style = MaterialTheme.typography.titleLarge)

                        val dateRangeOptions = mapOf("近1天" to 1, "近7天" to 7, "近30天" to 30)
                        Text("时间范围:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            dateRangeOptions.forEach { (label, range) ->
                                FilterChip(
                                    selected = viewModel.taskDateRange == range,
                                    onClick = { viewModel.updateStatsView(newDateRange = range) },
                                    label = { Text(label) }
                                )
                            }
                        }

                        // **被恢复的“状态筛选”逻辑**
                        val statusOptions = mapOf("全部" to "ALL", "待办" to "TODO", "进行中" to "IN_PROGRESS", "已完成" to "COMPLETED")
                        Text("状态筛选:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            statusOptions.forEach { (label, value) ->
                                FilterChip(
                                    selected = viewModel.statusFilter == value,
                                    onClick = {
                                        viewModel.updateStatsView(newStatusFilter = value)
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // **被恢复的图表动态更新逻辑**
                        // --- 修改后的正确逻辑 ---
                        val summary = userStats?.recentTaskSummary
                        // 使用标准的 if-else if 结构
                        if (summary != null) {
                            val chartEntries = when (viewModel.statusFilter) {
                                "TODO" -> listOf(entryOf(0, summary.todo))
                                "IN_PROGRESS" -> listOf(entryOf(0, summary.inProgress))
                                "COMPLETED" -> listOf(entryOf(0, summary.completed))
                                else -> listOf(
                                    entryOf(0, summary.todo),
                                    entryOf(1, summary.inProgress),
                                    entryOf(2, summary.completed)
                                )
                            }

                            val bottomAxisFormatter: AxisValueFormatter<AxisPosition.Horizontal.Bottom> =
                                when (viewModel.statusFilter) {
                                    "TODO" -> AxisValueFormatter { _, _ -> "待办" }
                                    "IN_PROGRESS" -> AxisValueFormatter { _, _ -> "进行中" }
                                    "COMPLETED" -> AxisValueFormatter { _, _ -> "已完成" }
                                    else -> AxisValueFormatter { value, _ ->
                                        when (value.toInt()) {
                                            0 -> "待办"
                                            1 -> "进行中"
                                            2 -> "已完成"
                                            else -> ""
                                        }
                                    }
                                }

                            if (chartEntries.any { it.y > 0 }) {
                                val columnChartProducer = ChartEntryModelProducer(chartEntries)
                                Chart(
                                    chart = columnChart(),
                                    chartModelProducer = columnChartProducer,
                                    startAxis = rememberStartAxis(title = "任务数"),
                                    bottomAxis = rememberBottomAxis(
                                        title = "状态",
                                        valueFormatter = bottomAxisFormatter
                                    ),
                                    modifier = Modifier.height(200.dp)
                                )
                            } else if (!viewModel.isLoading){
                                Text("此筛选条件下无数据。", modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
                            }
                        } else if (!viewModel.isLoading) {
                            // 当 summary 为 null 且不在加载中时，显示此文本
                            Text("暂无数据。", modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
                        }
                        // --- 逻辑修改结束 ---
                    }
                }
            }

            // 时间分配报告卡片
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        val reportTitle = when(viewModel.timeAllocationPeriod) {
                            "daily" -> "时间分配报告 (按日)"
                            "weekly" -> "时间分配报告 (按周)"
                            else -> "时间分配报告 (按标签)"
                        }
                        Text(reportTitle, style = MaterialTheme.typography.titleLarge)

                        val allocationPeriodOptions = mapOf("按标签" to "tags", "按日" to "daily", "按周" to "weekly")
                        Text("报告维度:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            allocationPeriodOptions.forEach { (label, period) ->
                                FilterChip(
                                    selected = viewModel.timeAllocationPeriod == period,
                                    onClick = { viewModel.updateStatsView(newTimeAllocationPeriod = period) },
                                    label = { Text(label) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        if (viewModel.timeAllocationReport.isNotEmpty()) {
                            viewModel.timeAllocationReport.forEach { dataPoint ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(dataPoint.category, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            text = "${String.format("%.1f", dataPoint.percentage * 100)}%",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { dataPoint.percentage },
                                        modifier = Modifier.fillMaxWidth().height(8.dp)
                                    )
                                }
                            }
                        } else if (!viewModel.isLoading) {
                            Text("暂无专注数据生成报告。", modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
                        }
                    }
                }
            }

            // 建议栏卡片
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, contentDescription = "建议", tint = Color(0xFFFBC02D))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("建议栏", style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        if (viewModel.suggestions.isNotEmpty()) {
                            viewModel.suggestions.forEach { suggestion ->
                                Text(
                                    text = "• $suggestion",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        } else if (!viewModel.isLoading) {
                            Text("暂无优化建议。", modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
                        }
                    }
                }
            }

            // 错误信息显示
            item {
                if (viewModel.errorMessage.isNotEmpty()) {
                    Text(
                        viewModel.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OverviewCard(
    completedTasks: Int,
    inProgressTasks: Int,
    totalTeams: Int,
    totalFocusHours: Double
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("数据总览", style = MaterialTheme.typography.titleLarge)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("已完成任务:", fontWeight = FontWeight.Bold)
                Text("$completedTasks")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("进行中任务:", fontWeight = FontWeight.Bold)
                Text("$inProgressTasks")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("参与团队数:", fontWeight = FontWeight.Bold)
                Text("$totalTeams")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("总专注时长:", fontWeight = FontWeight.Bold)
                Text("${String.format("%.2f", totalFocusHours)} 小时")
            }
        }
    }
}