// AdminTasksScreen.kt - Enhanced with Delete Functionality

package com.company.employeetracker.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.employeetracker.data.database.entities.Task
import com.company.employeetracker.ui.components.AddTaskDialog
import com.company.employeetracker.ui.components.EmptyStateScreen
import com.company.employeetracker.ui.components.LoadingScreen
import com.company.employeetracker.ui.theme.*
import com.company.employeetracker.viewmodel.EmployeeViewModel
import com.company.employeetracker.viewmodel.TaskViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AdminTasksScreen(
    taskViewModel: TaskViewModel = viewModel(),
    employeeViewModel: EmployeeViewModel = viewModel()
) {
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        delay(800)
        isLoading = false
    }

    if (isLoading) {
        LoadingScreen(message = "Loading tasks...")
        return
    }

    val allTasks by taskViewModel.allTasks.collectAsState()
    val employees by employeeViewModel.employees.collectAsState()
    val pendingCount by taskViewModel.pendingCount.collectAsState()
    val activeCount by taskViewModel.activeCount.collectAsState()
    val completedCount by taskViewModel.completedCount.collectAsState()

    var selectedFilter by remember { mutableStateOf("All") }

    val filteredTasks = when (selectedFilter) {
        "High Priority" -> allTasks.filter { it.priority == "High" || it.priority == "Critical" }
        "Due Today" -> allTasks.filter { it.deadline == java.time.LocalDate.now().toString() }
        "My Tasks" -> allTasks
        else -> allTasks
    }

    val totalTasks = allTasks.size
    val completionRate = if (totalTasks > 0) (completedCount * 100) / totalTasks else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(PurplePrimary, PurpleDark)
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Tasks",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Manage your team's workload",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    IconButton(
                        onClick = { showAddTaskDialog = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.2f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Task",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Stats Grid
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Pending",
                                tint = AccentRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$pendingCount",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pending",
                                fontSize = 12.sp,
                                color = Color(0xFF757575)
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Active",
                                tint = AccentOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$activeCount",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Active",
                                fontSize = 12.sp,
                                color = Color(0xFF757575)
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Done",
                                tint = GreenPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$completedCount",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Done",
                                fontSize = 12.sp,
                                color = Color(0xFF757575)
                            )
                        }
                    }
                }
            }

            // Completion Card
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = PurplePrimary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Assignment,
                                    contentDescription = "Tasks",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Total Tasks",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "$totalTasks",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = "Completion",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Completion",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "$completionRate%",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Filter Chips
            item {
                Spacer(modifier = Modifier.height(16.dp))
                val filters = listOf("All", "High Priority", "Due Today", "My Tasks")

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(filters) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    text = filter,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurplePrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Empty State
            if (filteredTasks.isEmpty()) {
                item {
                    EmptyStateScreen(
                        icon = Icons.Default.AssignmentLate,
                        title = "No Tasks Found",
                        message = "There are no tasks matching your filter criteria.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    )
                }
            } else {
                // Pending Tasks
                if (filteredTasks.any { it.status == "Pending" }) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        TaskSectionHeader(
                            title = "Pending Tasks",
                            count = filteredTasks.count { it.status == "Pending" },
                            color = AccentRed,
                            icon = Icons.Default.Timer
                        )
                    }

                    items(filteredTasks.filter { it.status == "Pending" }) { task ->
                        Spacer(modifier = Modifier.height(12.dp))
                        TaskCardWithDelete(
                            task = task,
                            employeeName = employees.find { it.id == task.employeeId }?.name ?: "Unknown",
                            onDeleteClick = { taskToDelete = task },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // Active Tasks
                if (filteredTasks.any { it.status == "Active" }) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        TaskSectionHeader(
                            title = "In Progress",
                            count = filteredTasks.count { it.status == "Active" },
                            color = AccentBlue,
                            icon = Icons.Default.Refresh
                        )
                    }

                    items(filteredTasks.filter { it.status == "Active" }) { task ->
                        Spacer(modifier = Modifier.height(12.dp))
                        TaskCardWithDelete(
                            task = task,
                            employeeName = employees.find { it.id == task.employeeId }?.name ?: "Unknown",
                            onDeleteClick = { taskToDelete = task },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // Completed Tasks
                if (filteredTasks.any { it.status == "Done" }) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        TaskSectionHeader(
                            title = "Completed",
                            count = filteredTasks.count { it.status == "Done" },
                            color = GreenPrimary,
                            icon = Icons.Default.CheckCircle
                        )
                    }

                    items(filteredTasks.filter { it.status == "Done" }.take(5)) { task ->
                        Spacer(modifier = Modifier.height(12.dp))
                        TaskCardWithDelete(
                            task = task,
                            employeeName = employees.find { it.id == task.employeeId }?.name ?: "Unknown",
                            onDeleteClick = { taskToDelete = task },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onTaskAdded = {}
        )
    }

    // Delete Task Dialog
    if (taskToDelete != null) {
        DeleteTaskDialog(
            task = taskToDelete!!,
            employeeName = employees.find { it.id == taskToDelete!!.employeeId }?.name ?: "Unknown",
            onDismiss = { taskToDelete = null },
            onConfirmDelete = {
                scope.launch {
                    taskViewModel.deleteTask(taskToDelete!!.id)
                    taskToDelete = null
                }
            }
        )
    }
}

@Composable
private fun TaskSectionHeader(
    title: String,
    count: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = color.copy(alpha = 0.1f)
        ) {
            Text(
                text = "$count",
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun TaskCardWithDelete(
    task: Task,
    employeeName: String,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (task.status) {
        "Done" -> GreenPrimary
        "Active" -> AccentOrange
        "Pending" -> AccentRed
        else -> Color.Gray
    }

    val priorityColor = when (task.priority) {
        "High", "Critical" -> AccentRed
        "Medium" -> AccentOrange
        "Low" -> AccentBlue
        else -> Color.Gray
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        fontSize = 13.sp,
                        color = Color(0xFF757575),
                        maxLines = 2
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = AccentRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Assigned To
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Assigned",
                    tint = Color(0xFF757575),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = employeeName,
                    fontSize = 13.sp,
                    color = Color(0xFF757575)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = priorityColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = task.priority,
                            color = priorityColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Deadline",
                        tint = Color(0xFF757575),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = task.deadline,
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = task.status,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteTaskDialog(
    task: Task,
    employeeName: String,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = AccentRed,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Delete Task?",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column {
                Text("You are about to permanently delete this task:")

                Spacer(Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = AccentRed.copy(alpha = 0.05f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = task.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Assigned to: $employeeName",
                            fontSize = 13.sp,
                            color = Color(0xFF757575)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Priority: ${task.priority} • Deadline: ${task.deadline}",
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "This action cannot be undone!",
                    fontSize = 13.sp,
                    color = AccentRed,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Delete", fontWeight = FontWeight.Bold)
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