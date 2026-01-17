package com.company.employeetracker.ui.components

import android.annotation.SuppressLint
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
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    var deadline by remember {
        mutableStateOf(
            LocalDate.now().plusDays(7).format(formatter)
        )
    }

    var deadlineTimestamp by remember {
        mutableLongStateOf(
            LocalDate.now().plusDays(7)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
    }

    var employeeExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    var titleError by remember { mutableStateOf("") }
    var descriptionError by remember { mutableStateOf("") }
    var employeeError by remember { mutableStateOf("") }

    val priorities = listOf("Low", "Medium", "High", "Critical")

    val todayMillis = java.time.LocalDate.now()
        .atStartOfDay(java.time.ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = deadlineTimestamp,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis >= todayMillis
            }
        }
    )

    val selectedDate = remember(deadlineTimestamp) {
        java.time.Instant.ofEpochMilli(deadlineTimestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
    }

    val isWeekend =
        selectedDate.dayOfWeek == java.time.DayOfWeek.SATURDAY ||
                selectedDate.dayOfWeek == java.time.DayOfWeek.SUNDAY

    if (isWeekend) {
        Text(
            text = "⚠ Deadline falls on a weekend",
            color = MaterialTheme.colorScheme.error,
            fontSize = 12.sp
        )
    }


    fun validateFields(): Boolean {
        var valid = true

        if (title.length < 5) {
            titleError = "Minimum 5 characters"
            valid = false
        }

        if (description.length < 10) {
            descriptionError = "Minimum 10 characters"
            valid = false
        }

        if (selectedEmployeeId == 0) {
            employeeError = "Select an employee"
            valid = false
        }

        return valid
    }

    AlertDialog(
        onDismissRequest = onDismiss
    ) {

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {

            Column(
                modifier = Modifier
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
                        "Create New Task",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = ""
                    },
                    label = { Text("Task Title *") },
                    leadingIcon = { Icon(Icons.Default.Assignment, null) },
                    isError = titleError.isNotEmpty(),
                    supportingText = {
                        if (titleError.isNotEmpty()) {
                            Text(titleError, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        descriptionError = ""
                    },
                    label = { Text("Description *") },
                    leadingIcon = { Icon(Icons.Default.Description, null) },
                    minLines = 3,
                    isError = descriptionError.isNotEmpty(),
                    supportingText = {
                        if (descriptionError.isNotEmpty()) {
                            Text(descriptionError, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                // Employee Dropdown
                ExposedDropdownMenuBox(
                    expanded = employeeExpanded,
                    onExpandedChange = { employeeExpanded = !employeeExpanded }
                ) {

                    OutlinedTextField(
                        value = selectedEmployee,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assign To *") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(employeeExpanded)
                        },
                        isError = employeeError.isNotEmpty(),
                        supportingText = {
                            if (employeeError.isNotEmpty()) {
                                Text(employeeError, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = employeeExpanded,
                        onDismissRequest = { employeeExpanded = false }
                    ) {
                        employees.forEach {
                            DropdownMenuItem(
                                text = { Text(it.name) },
                                onClick = {
                                    selectedEmployee = it.name
                                    selectedEmployeeId = it.id
                                    employeeExpanded = false
                                    employeeError = ""
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Priority
                ExposedDropdownMenuBox(
                    expanded = priorityExpanded,
                    onExpandedChange = { priorityExpanded = !priorityExpanded }
                ) {

                    OutlinedTextField(
                        value = priority,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priority") },
                        leadingIcon = { Icon(Icons.Default.PriorityHigh, null) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(priorityExpanded)
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = priorityExpanded,
                        onDismissRequest = { priorityExpanded = false }
                    ) {
                        priorities.forEach {
                            DropdownMenuItem(
                                text = { Text(it) },
                                onClick = {
                                    priority = it
                                    priorityExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Deadline Field
                OutlinedTextField(
                    value = deadline,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Deadline *") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))

                // Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (validateFields()) {
                                taskViewModel.addTask(
                                    Task(
                                        employeeId = selectedEmployeeId,
                                        title = title,
                                        description = description,
                                        priority = priority,
                                        status = "Pending",
                                        deadline = deadline,
                                        deadlineTimestamp = deadlineTimestamp,
                                        assignedDate = LocalDate.now().toString()
                                    )
                                )
                                onTaskAdded()
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Create Task")
                    }
                }
            }
        }
    }

    // ================= DATE PICKER =================

    if (showDatePicker) {

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },

            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->

                            val selectedDate = java.time.Instant
                                .ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()

                            deadline = selectedDate.format(
                                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                            )

                            deadlineTimestamp = millis

                            // ✅ AUTO PRIORITY
                            val daysLeft = java.time.temporal.ChronoUnit.DAYS.between(
                                java.time.LocalDate.now(),
                                selectedDate
                            )

                            priority = when {
                                daysLeft <= 1 -> "Critical"
                                daysLeft <= 3 -> "High"
                                daysLeft <= 7 -> "Medium"
                                else -> "Low"
                            }
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },

            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }

        ) {
            DatePicker(
                state = datePickerState
            )
        }
    }
}