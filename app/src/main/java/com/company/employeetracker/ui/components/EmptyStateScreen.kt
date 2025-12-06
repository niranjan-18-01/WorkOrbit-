package com.company.employeetracker.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.employeetracker.data.database.entities.User
import com.company.employeetracker.ui.components.TaskCard
import com.company.employeetracker.ui.theme.*
import com.company.employeetracker.viewmodel.ReviewViewModel
import com.company.employeetracker.viewmodel.TaskViewModel
import kotlinx.coroutines.delay
import com.company.employeetracker.ui.components.ErrorStateScreen
import com.company.employeetracker.ui.components.LoadingScreen



@Composable
fun EmployeeHomeScreen(
    currentUser: User,
    onTaskClick: (Int) -> Unit = {},
    taskViewModel: TaskViewModel = viewModel(),
    reviewViewModel: ReviewViewModel = viewModel()
) {
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser.id) {
        try {
            isLoading = true
            hasError = false
            taskViewModel.loadTasksForEmployee(currentUser.id)
            reviewViewModel.loadReviewsForEmployee(currentUser.id)
            // Small delay to allow UI to show loading state briefly; remove/replace in production
            delay(300)
            isLoading = false
        } catch (e: Exception) {
            hasError = true
            isLoading = false
        }
    }

    val tasks by taskViewModel.employeeTasks.collectAsState()
    val reviews by reviewViewModel.employeeReviews.collectAsState()
    val activeCount by taskViewModel.activeCount.collectAsState()
    val pendingCount by taskViewModel.pendingCount.collectAsState()
    val completedCount by taskViewModel.completedCount.collectAsState()

    val totalTasks by remember(activeCount, pendingCount, completedCount) {
        mutableStateOf(activeCount + pendingCount + completedCount)
    }
    val completionPercentage by remember(totalTasks, completedCount) {
        mutableStateOf(if (totalTasks > 0) (completedCount * 100) / totalTasks else 0)
    }
    val latestReview by remember(reviews) { mutableStateOf(reviews.firstOrNull()) }

    // Show Loading
    if (isLoading) {
        LoadingScreen(message = "Loading your dashboard...")
        return
    }

    // Show Error
    if (hasError) {
        ErrorStateScreen(
            title = "Failed to Load Data",
            message = "We couldn't load your dashboard. Please check your connection and try again.",
            onRetry = {
                // Trigger reload by toggling isLoading and re-running LaunchedEffect
                isLoading = true
                hasError = false
            }
        )
        return
    }

    AnimatedVisibility(
        visible = !isLoading && !hasError,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 })
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            // Header
            item {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                // Safe initials
                                val initials = currentUser.name
                                    .trim()
                                    .split(Regex("\\s+"))
                                    .filter { it.isNotEmpty() }
                                    .mapNotNull { it.firstOrNull()?.toString() }
                                    .take(2)
                                    .joinToString("")

                                Text(
                                    text = if (initials.isNotEmpty()) initials else "--",
                                    color = GreenPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "Welcome back! 👋",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = currentUser.name.ifBlank { "Unnamed User" },
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currentUser.designation.ifBlank { "" },
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        IconButton(onClick = { /* TODO: Notifications */ }) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Task Status Cards
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Active Tasks
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "Active",
                                tint = GreenPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = activeCount.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary
                            )
                            Text(
                                text = "Active",
                                fontSize = 12.sp,
                                color = Color(0xFF757575)
                            )
                        }
                    }

                    // On Hold Tasks
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "On Hold",
                                tint = AccentOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = pendingCount.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentOrange
                            )
                            Text(
                                text = "On Hold",
                                fontSize = 12.sp,
                                color = Color(0xFF757575)
                            )
                        }
                    }

                    // Completed Tasks
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = AccentBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = completedCount.toString(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentBlue
                            )
                            Text(
                                text = "Completed",
                                fontSize = 12.sp,
                                color = Color(0xFF757575)
                            )
                        }
                    }
                }
            }

            // Task Completion Card
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Task Completion",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Keep up the great work!",
                                    fontSize = 12.sp,
                                    color = Color(0xFF757575)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GreenLight.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "$completedCount of $totalTasks",
                                    color = GreenPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = if (totalTasks > 0) completedCount.toFloat() / totalTasks else 0f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = GreenPrimary,
                            trackColor = Color(0xFFE0E0E0)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$completionPercentage% Complete",
                                fontSize = 14.sp,
                                color = GreenPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${totalTasks - completedCount} remaining",
                                fontSize = 12.sp,
                                color = Color(0xFF757575)
                            )
                        }
                    }
                }
            }

            // Latest Review using .let to avoid smart-cast error
            latestReview?.let { review ->
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Review",
                                        tint = AccentYellow,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Latest Review",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = review.date ?: "",
                                            fontSize = 12.sp,
                                            color = Color(0xFF757575)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AccentYellow.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = "${review.overallRating}/5",
                                        color = Color(0xFFFF8F00),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = review.remarks ?: "",
                                fontSize = 14.sp,
                                color = Color(0xFF424242),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // Upcoming Tasks
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = "Tasks",
                            tint = Color(0xFF212121),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Upcoming Tasks",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (tasks.isNotEmpty()) {
                        TextButton(onClick = { /* Navigate to all tasks */ }) {
                            Text(
                                text = "View All",
                                color = GreenPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Show Empty State if No Tasks
            if (tasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TaskAlt,
                                    contentDescription = null,
                                    tint = Color(0xFFE0E0E0),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No tasks yet",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF757575)
                                )
                                Text(
                                    text = "New tasks will appear here",
                                    fontSize = 12.sp,
                                    color = Color(0xFF9E9E9E)
                                )
                            }
                        }
                    }
                }
            } else {
                // Task List with Animation
                items(items = tasks.take(3), key = { it.id }) { task ->
                    Spacer(modifier = Modifier.height(12.dp))
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 4 }),
                        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 4 })
                    ) {
                        TaskCard(
                            task = task,
                            onClick = { onTaskClick(task.id) },
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
}
