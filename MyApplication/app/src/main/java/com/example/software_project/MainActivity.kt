package com.example.software_project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.rememberAsyncImagePainter
import com.example.software_project.ui.theme.Software_projectTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var userViewModel: UserViewModel
    private lateinit var taskViewModel: TaskViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        userViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return UserViewModel(application) as T
            }
        })[UserViewModel::class.java]

        taskViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TaskViewModel(application) as T
            }
        })[TaskViewModel::class.java]

        setContent {
            Software_projectTheme {
                MainScreen(
                    navController = rememberNavController(),
                    startDestination = if (userViewModel.isLoggedIn) "tasks" else "auth",
                    userViewModel = userViewModel,
                    taskViewModel = taskViewModel
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    navController: NavHostController,
    startDestination: String,
    userViewModel: UserViewModel,
    taskViewModel: TaskViewModel
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(taskViewModel.tasks) {
        val tasksToRemind = taskViewModel.tasks.filter {
            it.status != TaskStatus.DONE && it.reminderSettings?.remindOnAppOpen == true
        }
        tasksToRemind.forEach { task ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "提醒: ${task.title}",
                    actionLabel = "查看",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            OptimizedDrawerContent(navController, userViewModel, scope, drawerState)
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {
            composable("auth") { AuthScreen(navController, userViewModel) }
            composable("tasks") { TaskScreen(navController, taskViewModel, drawerState, scope) }
            composable("create") { CreateTaskScreen(navController, taskViewModel) }
            composable("createTeam") { CreateTeamScreen(navController, taskViewModel) }
            composable("deleted") { DeletedTasksScreen(navController, taskViewModel) }
            composable("completed") { CompletedTasksScreen(navController, taskViewModel) }
            composable("expired") { ExpiredTasksScreen(navController, taskViewModel) }
            composable("calendar") { CalendarScreen(navController, taskViewModel, drawerState, scope) }
            composable("team") { TeamScreen(navController, taskViewModel, drawerState, scope) }
            composable(
                route = "team/{teamId}",
                arguments = listOf(navArgument("teamId") { type = NavType.IntType })
            ) { backStackEntry ->
                val teamId = backStackEntry.arguments?.getInt("teamId") ?: 0
                TeamDetailScreen(
                    navController = navController,
                    teamId = teamId,
                    viewModel = taskViewModel
                )
            }
            composable("profile") { ProfileScreen(navController, userViewModel, drawerState, scope) }
            composable("editProfile") { EditProfileScreen(navController, userViewModel) }
            composable("changePassword") { ChangePasswordScreen(navController, userViewModel) }
            composable("data") { DataScreen(navController, taskViewModel) }
            composable("tomatoClock") { TomatoClockScreen(navController, taskViewModel) }
            composable(
                "editTask/{taskId}",
                arguments = listOf(navArgument("taskId") { type = NavType.IntType })
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getInt("taskId")
                if (taskId != null) {
                    EditTaskScreen(navController = navController, taskId = taskId, viewModel = taskViewModel)
                }
            }
            composable(
                "memberProfile/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.IntType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getInt("userId") ?: 0
                if (userId != 0) {
                    MemberProfileScreen(
                        userId = userId,
                        navController = navController,
                        viewModel = userViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun OptimizedDrawerContent(
    navController: NavHostController,
    userViewModel: UserViewModel,
    scope: CoroutineScope,
    drawerState: DrawerState
) {
    // 1. 新增一个状态来控制退出登录确认对话框的显示
    var showLogoutDialog by remember { mutableStateOf(false) }

    // 3. 当 showLogoutDialog 为 true 时，显示 AlertDialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("确认退出登录") },
            text = { Text("您确定要退出当前账户吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        userViewModel.logout(navController)
                        scope.launch { drawerState.close() }
                    }
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("取消")
                }
            }
        )
    }

    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .padding(top = 48.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        navController.navigate("profile")
                        scope.launch { drawerState.close() }
                    }
            ) {
                if (userViewModel.avatarUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(userViewModel.avatarUri),
                        contentDescription = "用户头像",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "默认头像",
                        modifier = Modifier.size(48.dp).align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = userViewModel.name.takeIf { it.isNotBlank() } ?: "未设置姓名",
                style = MaterialTheme.typography.titleMedium
            )
        }

        val navItems = listOf(
            NavItem("任务", "tasks", Icons.Default.Task),
            NavItem("日程", "calendar", Icons.Default.CalendarToday),
            NavItem("团队", "team", Icons.Default.Group),
            NavItem("个人", "profile", Icons.Default.Person)
        )
        navItems.forEach { item ->
            NavigationDrawerItem(
                icon = { Icon(item.icon!!, null) },
                label = { Text(item.label) },
                selected = false,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                    scope.launch { drawerState.close() }
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

        Divider(modifier = Modifier.padding(vertical = 12.dp))

        Text(
            "任务分类",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.CheckCircle, null) },
            label = { Text("已完成") },
            selected = false,
            onClick = { navController.navigate("completed"); scope.launch { drawerState.close() } },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Warning, null) },
            label = { Text("已过期") },
            selected = false,
            onClick = { navController.navigate("expired"); scope.launch { drawerState.close() } },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Delete, null) },
            label = { Text("回收站") },
            selected = false,
            onClick = { navController.navigate("deleted"); scope.launch { drawerState.close() } },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )

        Spacer(modifier = Modifier.weight(1f))

        // 2. 修改“退出登录”项的 onClick 事件
        NavigationDrawerItem(
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
            label = { Text("退出登录") },
            selected = false,
            onClick = {
                // 不再直接退出，而是显示确认对话框
                showLogoutDialog = true
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}

data class NavItem(val label: String, val route: String, val icon: ImageVector? = null)