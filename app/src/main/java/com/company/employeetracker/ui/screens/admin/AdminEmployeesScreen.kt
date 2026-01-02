// AdminEmployeesScreen.kt - Enhanced with Delete Options

package com.company.employeetracker.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.employeetracker.data.database.entities.User
import com.company.employeetracker.ui.components.*
import com.company.employeetracker.ui.theme.*
import com.company.employeetracker.viewmodel.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEmployeesScreen(
    employeeViewModel: EmployeeViewModel = viewModel(),
    taskViewModel: TaskViewModel = viewModel(),
    reviewViewModel: ReviewViewModel = viewModel(),
    attendanceViewModel: AttendanceViewModel = viewModel()
) {
    val employees by employeeViewModel.employees.collectAsState()
    val allTasks by taskViewModel.allTasks.collectAsState()
    val allReviews by reviewViewModel.allReviews.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedEmployee by remember { mutableStateOf<User?>(null) }
    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var employeeToDelete by remember { mutableStateOf<User?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedDepartment by remember { mutableStateOf("All Departments") }

    val departments = listOf(
        "All Departments", "Engineering", "Design", "Analytics", "Product", "Management"
    )

    val filteredEmployees = employees.filter {
        (it.name.contains(searchQuery, ignoreCase = true) ||
                it.designation.contains(searchQuery, ignoreCase = true) ||
                it.department.contains(searchQuery, ignoreCase = true)) &&
                (selectedDepartment == "All Departments" || it.department == selectedDepartment)
    }

    val activeEmployees = employees.count { emp ->
        allTasks.any { it.employeeId == emp.id && it.status == "Active" }
    }

    val avgRating =
        if (allReviews.isNotEmpty()) allReviews.map { it.overallRating }.average().toFloat() else 0f

    if (selectedEmployee != null) {
        EmployeeDetailScreen(
            employee = selectedEmployee!!,
            employees = employees,
            onBackClick = { selectedEmployee = null },
            onDeleteEmployee = { employee ->
                employeeToDelete = employee
                selectedEmployee = null
            }
        )
        return
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddEmployeeDialog = true },
                containerColor = GreenPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Employee")
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {

            /** HEADER **/
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(GreenPrimary, GreenDark)
                        )
                    )
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Team Overview", color = Color.White, fontSize = 14.sp)
                        Text(
                            "Employees",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            LazyColumn {

                /** SEARCH **/
                item {
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search employees…") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                /** FILTER **/
                item {
                    Spacer(Modifier.height(12.dp))
                    var expanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedDepartment,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            departments.forEach {
                                DropdownMenuItem(
                                    text = { Text(it) },
                                    onClick = {
                                        selectedDepartment = it
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                /** STATS **/
                item {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.People,
                            value = "(${filteredEmployees.size})",
                            label = "Total",
                            color = AccentBlue
                        )

                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.CheckCircle,
                            value = "5",
                            label = "Active",
                            color = GreenPrimary
                        )

                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Star,
                            value = "4.3",
                            label = "Rating",
                            color = AccentYellow
                        )
                    }
                }

                /** LIST HEADER **/
                item {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "All Employees (${filteredEmployees.size})",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                /** EMPTY STATE **/
                if (filteredEmployees.isEmpty()) {
                    item {
                        EmptyStateScreen(
                            icon = Icons.Default.People,
                            title = "No Employees Found",
                            message = "Try adjusting your search or filters."
                        )
                    }
                } else {
                    items(filteredEmployees, key = { it.id }) { employee ->
                        val empTasks = allTasks.filter { it.employeeId == employee.id }
                        val empReviews = allReviews.filter { it.employeeId == employee.id }
                        val rating =
                            if (empReviews.isNotEmpty())
                                empReviews.map { it.overallRating }.average().toFloat()
                            else 0f

                        Spacer(Modifier.height(12.dp))

                        // Enhanced Employee Card with Delete Option
                        EmployeeCardWithActions(
                            employee = employee,
                            taskCount = empTasks.size,
                            rating = rating,
                            onViewClick = { selectedEmployee = employee },
                            onDeleteClick = { employeeToDelete = employee },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }

    if (showAddEmployeeDialog) {
        AddEmployeeDialog(
            onDismiss = { showAddEmployeeDialog = false },
            onEmployeeAdded = { showAddEmployeeDialog = false }
        )
    }

    // Delete success/error handling
    val deleteSuccess by employeeViewModel.deleteSuccess.collectAsState()
    val errorMessage by employeeViewModel.errorMessage.collectAsState()

    LaunchedEffect(deleteSuccess) {
        if (deleteSuccess) {
            // Show success message (you can add a Snackbar here)
            android.util.Log.d("AdminEmployees", "Employee deleted successfully!")
            employeeViewModel.clearDeleteSuccess()
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            // Show error message (you can add a Snackbar here)
            android.util.Log.e("AdminEmployees", "Delete error: $errorMessage")
            kotlinx.coroutines.delay(3000)
            employeeViewModel.clearError()
        }
    }

    // Delete Confirmation Dialog
    if (employeeToDelete != null) {
        DeleteEmployeeDialog(
            employee = employeeToDelete!!,
            onDismiss = { employeeToDelete = null },
            onConfirmDelete = {
                scope.launch {
                    android.util.Log.d("AdminEmployees", "Delete button clicked for: ${employeeToDelete!!.name}")

                    // Delete employee (this handles both Firebase and Local DB)
                    employeeViewModel.deleteEmployee(employeeToDelete!!)

                    // Delete all tasks from Firebase
                    val employeeId = employeeToDelete!!.id
                    allTasks.filter { it.employeeId == employeeId }
                        .forEach { task ->
                            android.util.Log.d("AdminEmployees", "Deleting task: ${task.title}")
                            taskViewModel.deleteTask(task.id)
                        }

                    employeeToDelete = null
                }
            }
        )
    }
}

@Composable
private fun EmployeeCardWithActions(
    employee: User,
    taskCount: Int,
    rating: Float,
    onViewClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val avatarColor = when (employee.department) {
        "Design" -> Color(0xFF9C27B0)
        "Engineering" -> Color(0xFF2196F3)
        "Analytics" -> Color(0xFFFF5722)
        "Product" -> Color(0xFF00BCD4)
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            // Main content (clickable to view details)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onViewClick)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = employee.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }
                            .take(2).joinToString(""),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Employee Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = employee.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                    Text(
                        text = employee.designation,
                        fontSize = 14.sp,
                        color = Color(0xFF757575)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Department Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = avatarColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = employee.department,
                            color = avatarColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (taskCount > 0 || rating > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (taskCount > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Assignment,
                                        contentDescription = "Tasks",
                                        tint = Color(0xFF757575),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "$taskCount tasks",
                                        fontSize = 12.sp,
                                        color = Color(0xFF757575)
                                    )
                                }
                            }
                        }
                    }
                }

                // Rating
                if (rating > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%.1f", rating),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF212121)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFE0E0E0))

            // Action Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = onViewClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = GreenPrimary
                    )
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("View Details", fontSize = 13.sp)
                }

                TextButton(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = AccentRed
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Delete", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun DeleteEmployeeDialog(
    employee: User,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = null,
                tint = AccentRed,
                modifier = Modifier.size(56.dp)
            )
        },
        title = {
            Text(
                text = "Delete Employee?",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "You are about to permanently delete:",
                    fontSize = 14.sp,
                    color = Color(0xFF757575)
                )
                Spacer(Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = AccentRed.copy(alpha = 0.05f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = AccentRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = employee.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = employee.designation,
                                    fontSize = 13.sp,
                                    color = Color(0xFF757575)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "⚠️ This will also delete:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = AccentRed
                )
                Spacer(Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DeleteWarningItem("All assigned tasks")
                    DeleteWarningItem("Performance reviews")
                    DeleteWarningItem("Attendance records")
                    DeleteWarningItem("Message history")
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "This action cannot be undone!",
                    fontSize = 13.sp,
                    color = AccentRed,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentRed
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Delete Permanently", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeleteWarningItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(AccentRed)
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color(0xFF424242)
        )
    }
}

@Composable
private fun StatMiniCard(
    value: String,
    label: String,
    color: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )

                icon?.let {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}