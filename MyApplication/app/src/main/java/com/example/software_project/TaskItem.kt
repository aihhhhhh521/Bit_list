package com.example.software_project

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TaskItem(
    content: @Composable () -> Unit,
    menuActions: @Composable () -> Unit,
    showMenuButton: Boolean = true
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
            // 如果允许显示菜单按钮，则渲染它
            if (showMenuButton) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多", tint = Color.Gray)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        menuActions()
                    }
                }
            }
        }
    }
}

@Composable
fun TaskActionButtons(
    onDelete: () -> Unit,
    onComplete: () -> Unit,
    onAddAttachment: () -> Unit,
    onAddSubTask: () -> Unit, // Add new parameter for the action
    onEdit: () -> Unit, // 新增编辑回调
    onMarkInProgress: () -> Unit // 新增“进行中”回调
) {
    DropdownMenuItem(
        text = { Text("编辑") },
        onClick = { onEdit() },
        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Gray) }
    )
    DropdownMenuItem(
        text = { Text("标记为进行中") },
        onClick = { onMarkInProgress() },
        leadingIcon = { Icon(Icons.Default.Sync, contentDescription = null, tint = Color.Blue) }
    )
    DropdownMenuItem(
        text = { Text("标记为已完成") },
        onClick = { onComplete() },
        leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, tint = Color.Green) }
    )
    // Add the new menu item for creating a sub-task
    DropdownMenuItem(
        text = { Text("添加子任务") },
        onClick = { onAddSubTask() },
        leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = Color.Blue) }
    )
    DropdownMenuItem(
        text = { Text("添加附件") },
        onClick = { onAddAttachment() },
        leadingIcon = { Icon(Icons.Default.Upload, contentDescription = null, tint = Color.Blue) }
    )
    DropdownMenuItem(
        text = { Text("删除") },
        onClick = { onDelete() },
        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
    )
}

@Composable
fun CompletedTaskActionButtons(
    onDelete: () -> Unit,
    onRestore: () -> Unit
) {
    DropdownMenuItem(
        text = { Text("删除") },
        onClick = {
            onDelete()
        },
        leadingIcon = {
            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
        }
    )
    DropdownMenuItem(
        text = { Text("恢复") },
        onClick = {
            onRestore()
        },
        leadingIcon = {
            Icon(Icons.Default.Restore, contentDescription = null, tint = Color.Green)
        }
    )
}

@Composable
fun DeletedTaskActionButtons(
    onDelete: () -> Unit,
    onRestore: () -> Unit
) {
    DropdownMenuItem(
        text = { Text("永久删除") },
        onClick = {
            onDelete()
        },
        leadingIcon = {
            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
        }
    )
    DropdownMenuItem(
        text = { Text("恢复") },
        onClick = {
            onRestore()
        },
        leadingIcon = {
            Icon(Icons.Default.Restore, contentDescription = null, tint = Color.Green)
        }
    )
}

@Composable
fun ExpiredTaskActionButtons(
    onDelete: () -> Unit,
    onComplete: () -> Unit
) {
    DropdownMenuItem(
        text = { Text("删除") },
        onClick = {
            onDelete()
        },
        leadingIcon = {
            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
        }
    )
    DropdownMenuItem(
        text = { Text("标记为已完成") },
        onClick = {
            onComplete()
        },
        leadingIcon = {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Green)
        }
    )
}