package com.company.employeetracker.ui.screens.admin

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
import com.company.employeetracker.ui.components.EmployeeCard
import com.company.employeetracker.ui.theme.*
import com.company.employeetracker.viewmodel.EmployeeViewModel
import com.company.employeetracker.viewmodel.ReviewViewModel
import com.company.employeetracker.viewmodel.TaskViewModel
import com.company.employeetracker.ui.components.AddReviewDialog
@Composable
fun AdminDashboardScreen(
    employeeViewModel: EmployeeViewModel = viewModel(),
    taskViewModel: TaskViewModel = viewModel(),
    reviewViewModel: ReviewViewModel = viewModel()
) {
    val employees by employeeViewModel.employees.collectAsState()
    val employeeCount by employeeViewModel.employeeCount.collectAsState()
    val allTasks by taskViewModel.allTasks.collectAsState()
    val allReviews by reviewViewModel.allReviews.collectAsState()
    val reviewCount by reviewViewModel.reviewCount.collectAsState()

    val completedTasks = allTasks.count { it.status == "Done" }
    val pendingTasks = allTasks.size - completedTasks
    val averageRating = if (allReviews.isNotEmpty()) {
        allReviews.map { it.overallRating }.average().toFloat()
    } else 0f

    // Get top performers
    val topPerformers = allReviews
        .groupBy { it.employeeId }
        .mapValues { entry -> entry.value.map { it.overallRating }.average().toFloat() }
        .toList()
        .sortedByDescending { it.second }
        .take(3)

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
                            colors = listOf(IndigoPrimary, IndigoDark)
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
                                text = "Welcome Back! 👋",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "Admin Dashboard",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Empower your team through insights",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        IconButton(onClick = { /* Notifications */ }) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Stats Grid
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminStatCard(
                        icon = Icons.Default.People,
                        value = employeeCount.toString(),
                        label = "Total Employees",
                        color = AccentBlue,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        icon = Icons.Default.CheckCircle,
                        value = completedTasks.toString(),
                        label = "Completed",
                        color = GreenPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdminStatCard(
                        icon = Icons.Default.Schedule,
                        value = pendingTasks.toString(),
                        label = "Pending Tasks",
                        color = AccentOrange,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        icon = Icons.Default.Star,
                        value = String.format("%.1f", averageRating),
                        label = "Avg Rating",
                        color = AccentYellow,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Top Performers Section
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
                    Text(
                        text = "Top Performers",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Trophy",
                        tint = AccentYellow,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GreenLight.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "This Week",
                        color = GreenPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Top Performers List
        items(topPerformers.take(3)) { (employeeId, rating) ->
            val employee = employees.find { it.id == employeeId }
            employee?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    EmployeeCard(
                        employee = it,
                        rating = rating
                    )
                }
            }
        }

        // Recent Activities
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activities",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Activity",
                    tint = AccentOrange,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    ActivityItem(
                        icon = Icons.Default.PersonAdd,
                        title = "New employee added",
                        subtitle = "Sarah Johnson joined Engineering",
                        time = "2h ago",
                        iconColor = GreenPrimary
                    )
                    Divider()
                    ActivityItem(
                        icon = Icons.Default.CheckCircle,
                        title = "Task completed",
                        subtitle = "Code review finished by Sarah",
                        time = "4h ago",
                        iconColor = AccentBlue
                    )
                    Divider()
                    ActivityItem(
                        icon = Icons.Default.Star,
                        title = "Performance review",
                        subtitle = "Emily Davis rated 4.9/5.0",
                        time = "1d ago",
                        iconColor = AccentYellow
                    )
                }
            }
        }

        // Add Review Button
        item {
            var showAddReviewDialog by remember { mutableStateOf(false) }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { showAddReviewDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC107)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Add Review"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add Performance Review",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            if (showAddReviewDialog) {
                AddReviewDialog(
                    onDismiss = { showAddReviewDialog = false },
                    onReviewAdded = {
                        // Review added successfully
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun AdminStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(120.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = value,
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
}

@Composable
fun ActivityItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    time: String,
    iconColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(iconColor, CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF212121)
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF757575)
            )
        }

        Text(
            text = time,
            fontSize = 11.sp,
            color = Color(0xFF9E9E9E)
        )
    }
}