package com.example.software_project

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun PriorityDropdown(selectedPriority: Priority, onPriorityChange: (Priority) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = selectedPriority.toString(),
            onValueChange = {},
            label = { Text("优先级") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, "选择优先级", Modifier.clickable { expanded = true })
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Priority.values().forEach { priority ->
                DropdownMenuItem(
                    text = { Text(priority.toString()) },
                    onClick = {
                        onPriorityChange(priority)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun StatusDropdown(selectedStatus: TaskStatus, onStatusChange: (TaskStatus) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = selectedStatus.toString(),
            onValueChange = {},
            label = { Text("状态") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, "选择状态", Modifier.clickable { expanded = true })
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TaskStatus.values().forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.toString()) },
                    onClick = {
                        onStatusChange(status)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun RecurringTypeDropdown(selectedType: RecurringType, onTypeChange: (RecurringType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = selectedType.toString(),
            onValueChange = {},
            label = { Text("重复类型") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, "选择重复类型", Modifier.clickable { expanded = true })
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            RecurringType.values().forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.toString()) },
                    onClick = {
                        onTypeChange(type)
                        expanded = false
                    }
                )
            }
        }
    }
}