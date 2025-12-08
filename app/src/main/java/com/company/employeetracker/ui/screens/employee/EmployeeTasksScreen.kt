package com.company.employeetracker.ui.screens.employee

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.employeetracker.data.database.entities.Task
import com.company.employeetracker.data.database.entities.User
import com.company.employeetracker.ui.theme.*
import com.company.employeetracker.viewmodel.TaskViewModel

@Composable
fun EmployeeTasksScreen(
    currentUser: User,
    onBackClick: () -> Unit = {},
    taskViewModel: TaskViewModel = viewModel()
) {
    LaunchedEffect(currentUser.id) {
        taskViewModel.loadTasksForEmployee(currentUser.id)
    }

    val tasks by taskViewModel.employeeTasks.collectAsState()
    val pendingCount by taskViewModel.pendingCount.collectAsState()
    val activeCount by taskViewModel.activeCount.collectAsState()
    val completedCount by taskViewModel.completedCount.collectAsState()

    var selectedFilter by remember { mutableStateOf("All") }
    var selectedTask by remember { mutableStateOf<Task?>(null) }

    val filteredTasks = when (selectedFilter) {
        "Pending" -> tasks.filter { it.status == "Pending" }
        "Active" -> tasks.filter { it.status == "Active" }
        "Done" -> tasks.filter { it.status == "Done" }
        "Priority" -> tasks.sortedByDescending {
            when (it.priority) {
                "High" -> 3
                "Medium" -> 2
                "Low" -> 1
                else -> 0
            }
        }
        else -> tasks
    }

    val totalTasks = tasks.size
    val completionPercentage = if (totalTasks > 0) (completedCount * 100) / totalTasks else 0

    // Show task detail if a task is selected
    if (selectedTask != null) {
        TaskDetailScreen(
            task = selectedTask!!,
            onBackClick = { selectedTask = null },
            onStatusUpdate = { task, newStatus ->
                taskViewModel.updateTaskStatus(task.id, newStatus)
                selectedTask = null
            }
        )
        return
    }

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
                        colors = listOf(GreenPrimary, GreenDark)
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
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = "Tasks",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$totalTasks Tasks",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TaskAlt,
                        contentDescription = "My Tasks",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "My Tasks",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Track your progress and deadlines",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Overall Progress Card
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = "Progress",
                                        tint = GreenPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Overall Progress",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$completedCount of $totalTasks completed",
                                    fontSize = 13.sp,
                                    color = Color(0xFF757575)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    completionPercentage >= 75 -> GreenLight.copy(alpha = 0.15f)
                                    completionPercentage >= 50 -> AccentOrange.copy(alpha = 0.15f)
                                    else -> AccentRed.copy(alpha = 0.15f)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$completionPercentage",
                                        color = when {
                                            completionPercentage >= 75 -> GreenPrimary
                                            completionPercentage >= 50 -> AccentOrange
                                            else -> AccentRed
                                        },
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "%",
                                        color = when {
                                            completionPercentage >= 75 -> GreenPrimary
                                            completionPercentage >= 50 -> AccentOrange
                                            else -> AccentRed
                                        },
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LinearProgressIndicator(
                            progress = { if (totalTasks > 0) completedCount.toFloat() / totalTasks else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = when {
                                completionPercentage >= 75 -> GreenPrimary
                                completionPercentage >= 50 -> AccentOrange
                                else -> AccentRed
                            },
                            trackColor = Color(0xFFE0E0E0)
                        )
                    }
                }
            }

            // Status Cards
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Schedule,
                        count = pendingCount,
                        label = "Pending",
                        color = AccentRed,
                        backgroundColor = AccentRed.copy(alpha = 0.1f)
                    )

                    StatusCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.PlayArrow,
                        count = activeCount,
                        label = "Active",
                        color = AccentOrange,
                        backgroundColor = AccentOrange.copy(alpha = 0.1f)
                    )

                    StatusCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CheckCircle,
                        count = completedCount,
                        label = "Done",
                        color = GreenPrimary,
                        backgroundColor = GreenLight.copy(alpha = 0.1f)
                    )
                }
            }

            // Filter Section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = Color(0xFF424242),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Filter Tasks",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listOf("All", "Pending", "Active", "Done", "Priority")) { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = {
                                    Text(
                                        text = filter,
                                        fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (selectedFilter == filter) {
                                    {
                                        Icon(
                                            imageVector = when (filter) {
                                                "Priority" -> Icons.Default.PriorityHigh
                                                "Pending" -> Icons.Default.Schedule
                                                "Active" -> Icons.Default.PlayArrow
                                                "Done" -> Icons.Default.CheckCircle
                                                else -> Icons.Default.ViewList
                                            },
                                            contentDescription = filter,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }

            // Task Count Header
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedFilter == "All") "All Tasks" else "$selectedFilter Tasks",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = GreenPrimary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${filteredTasks.size}",
                            color = GreenPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Task List
            items(filteredTasks) { task ->
                Spacer(modifier = Modifier.height(12.dp))
                EnhancedTaskCard(
                    task = task,
                    onClick = { selectedTask = task },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun StatusCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    label: String,
    color: Color,
    backgroundColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF757575)
            )
        }
    }
}

@Composable
fun EnhancedTaskCard(
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (task.status) {
        "Done" -> GreenPrimary
        "Active" -> AccentOrange
        "Pending" -> AccentRed
        else -> Color.Gray
    }

    val priorityColor = when (task.priority) {
        "High" -> AccentRed
        "Medium" -> AccentOrange
        "Low" -> AccentBlue
        else -> Color.Gray
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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

                    Spacer(modifier = Modifier.height(8.dp))

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

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "Priority",
                        tint = priorityColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = task.priority,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = priorityColor
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Deadline",
                        tint = Color(0xFF757575),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = task.deadline,
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "View Details",
                    tint = GreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}