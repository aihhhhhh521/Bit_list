package com.example.software_project

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(
    navController: NavHostController,
    viewModel: TaskViewModel = viewModel<TaskViewModel>(),
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    // --- 所有状态管理逻辑完整保留 ---
    var joinTeamId by remember { mutableStateOf("") }
    // --- 状态管理逻辑结束 ---

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("团队协作") },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "打开导航")
                    }
                }
            )
        },
        floatingActionButton = {
            // “新建团队”作为主要操作，使用FAB
            FloatingActionButton(onClick = { navController.navigate("createTeam") }) {
                Icon(Icons.Default.Add, contentDescription = "新建团队")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 卡片1: 加入团队功能区 ---
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("通过ID加入团队", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = joinTeamId,
                                onValueChange = { joinTeamId = it },
                                label = { Text("输入团队ID") },
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = {
                                val idToJoin = joinTeamId.toIntOrNull()
                                if (idToJoin != null) {
                                    viewModel.requestJoinTeam(idToJoin)
                                    joinTeamId = ""
                                } else {
                                    viewModel.errorMessage = "请输入有效的团队ID"
                                }
                            }) {
                                Text("申请")
                            }
                        }
                    }
                }
            }

            // --- 团队列表区 ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("我的团队", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    Text("${viewModel.teams.size}个团队", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // 处理空状态
            if (viewModel.teams.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "您还没有加入任何团队。\n点击右下角“+”创建一个吧！",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // 团队列表
                items(viewModel.teams, key = { it.id }) { team ->
                    TeamRow(
                        team = team,
                        progress = viewModel.calculateTeamProgress(team.id),
                        onClick = { navController.navigate("team/${team.id}") }
                    )
                }
            }

            // 错误信息显示
            if (viewModel.errorMessage.isNotEmpty()) {
                item {
                    Text(
                        text = viewModel.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * 优化的团队列表项
 * @param team 团队数据
 * @param progress 团队任务进度
 * @param onClick 点击事件回调
 */
@Composable
private fun TeamRow(
    team: Team,
    progress: Float,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // 使整个卡片可点击
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            // 左侧团队图标
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = "团队",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            // 中间显示团队名称和描述
            headlineContent = {
                Text(team.name, fontWeight = FontWeight.Bold)
            },
            supportingContent = {
                Column {
                    Text(
                        text = team.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${team.members.size} 位成员",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

            },
            // 右侧显示进度
            trailingContent = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "${progress.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        )
    }
}