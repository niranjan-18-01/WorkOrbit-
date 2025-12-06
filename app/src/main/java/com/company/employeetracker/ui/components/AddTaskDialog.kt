package com.company.employeetracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.employeetracker.data.database.entities.Task
import com.company.employeetracker.ui.theme.GreenPrimary
import com.company.employeetracker.viewmodel.EmployeeViewModel
import com.company.employeetracker.viewmodel.TaskViewModel
import java.time.LocalDate
import android.annotation.SuppressLint

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("NewApi")
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onTaskAdded: () -> Unit,
    employeeViewModel: EmployeeViewModel = viewModel(),
    taskViewModel: TaskViewModel = viewModel()
) {
    val employees by employeeViewModel.employees.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedEmployee by remember { mutableStateOf("Select Employee") }
    var selectedEmployeeId by remember { mutableIntStateOf(0) }
    var priority by remember { mutableStateOf("Medium") }
    var deadline by remember { mutableStateOf(LocalDate.now().plusDays(7).toString()) }
    var employeeExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }

    // Validation states
    var titleError by remember { mutableStateOf("") }
    var descriptionError by remember { mutableStateOf("") }
    var employeeError by remember { mutableStateOf("") }

    val priorities = listOf("Low", "Medium", "High", "Critical")

    // Validate fields
    fun validateFields(): Boolean {
        var isValid = true

        if (title.isBlank()) {
            titleError = "Task title is required"
            isValid = false
        } else if (title.length < 5) {
            titleError = "Title must be at least 5 characters"
            isValid = false
        } else {
            titleError = ""
        }

        if (description.isBlank()) {
            descriptionError = "Task description is required"
            isValid = false
        } else if (description.length < 10) {
            descriptionError = "Description must be at least 10 characters"
            isValid = false
        } else {
            descriptionError = ""
        }

        if (selectedEmployeeId == 0) {
            employeeError = "Please select an employee"
            isValid = false
        } else {
            employeeError = ""
        }

        return isValid
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create New Task",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = ""
                    },
                    label = { Text("Task Title *") },
                    placeholder = { Text("Enter task title") },
                    leadingIcon = {
                        Icon(Icons.Default.Assignment, contentDescription = null)
                    },
                    isError = titleError.isNotEmpty(),
                    supportingText = {
                        if (titleError.isNotEmpty()) {
                            Text(
                                text = titleError,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description Field
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        descriptionError = ""
                    },
                    label = { Text("Description *") },
                    placeholder = { Text("Enter detailed task description") },
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null)
                    },
                    isError = descriptionError.isNotEmpty(),
                    supportingText = {
                        if (descriptionError.isNotEmpty()) {
                            Text(
                                text = descriptionError,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Employee Selection Dropdown
                ExposedDropdownMenuBox(
                    expanded = employeeExpanded,
                    onExpandedChange = { employeeExpanded = !employeeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedEmployee,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assign To *") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = employeeExpanded)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        isError = employeeError.isNotEmpty(),
                        supportingText = {
                            if (employeeError.isNotEmpty()) {
                                Text(
                                    text = employeeError,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = employeeExpanded,
                        onDismissRequest = { employeeExpanded = false }
                    ) {
                        employees.forEach { employee ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(employee.name, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${employee.designation} • ${employee.department}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF757575)
                                        )
                                    }
                                },
                                onClick = {
                                    selectedEmployee = employee.name
                                    selectedEmployeeId = employee.id
                                    employeeExpanded = false
                                    employeeError = ""
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Priority Dropdown
                ExposedDropdownMenuBox(
                    expanded = priorityExpanded,
                    onExpandedChange = { priorityExpanded = !priorityExpanded }
                ) {
                    OutlinedTextField(
                        value = priority,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priority *") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.PriorityHigh, contentDescription = null)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = priorityExpanded,
                        onDismissRequest = { priorityExpanded = false }
                    ) {
                        priorities.forEach { pri ->
                            DropdownMenuItem(
                                text = { Text(pri) },
                                onClick = {
                                    priority = pri
                                    priorityExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Deadline Info
                OutlinedTextField(
                    value = "Due: ${deadline}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Deadline") },
                    leadingIcon = {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "* Required fields",
                    fontSize = 12.sp,
                    color = Color(0xFF757575)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (validateFields()) {
                                val newTask = Task(
                                    employeeId = selectedEmployeeId,
                                    title = title,
                                    description = description,
                                    priority = priority,
                                    status = "Pending",
                                    deadline = deadline,
                                    assignedDate = LocalDate.now().toString()
                                )
                                taskViewModel.addTask(newTask)
                                onTaskAdded()
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create Task")
                    }
                }
            }
        }
    }
}