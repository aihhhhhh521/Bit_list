package com.example.software_project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AttachmentPermissionsDialog(
    team: Team,
    attachment: Attachment,
    onDismiss: () -> Unit,
    onSave: (newPermissions: Map<Int, Role>) -> Unit
) {
    // 使用 mutableStateOf 创建一个在组合期间可以观察和修改的权限状态
    var tempPermissions by remember { mutableStateOf(attachment.permissions ?: emptyMap()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理附件权限") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(team.members.keys.toList()) { memberId ->
                    var expanded by remember { mutableStateOf(false) }
                    // 获取成员当前的角色，如果不存在则默认为 MEMBER
                    val currentRole = tempPermissions[memberId] ?: Role.MEMBER

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("成员ID: $memberId")
                        Box {
                            Button(onClick = { expanded = true }) {
                                Text(currentRole.name)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                Role.values().forEach { role ->
                                    DropdownMenuItem(
                                        text = { Text(role.name) },
                                        onClick = {
                                            // 更新临时权限状态
                                            tempPermissions = tempPermissions.toMutableMap().apply {
                                                this[memberId] = role
                                            }
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(tempPermissions) }) {
                Text("保存")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}