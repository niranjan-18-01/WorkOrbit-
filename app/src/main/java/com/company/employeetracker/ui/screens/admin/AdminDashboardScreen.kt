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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.employeetracker.ui.components.EmployeeCard
import com.company.employeetracker.ui.components.AddReviewDialog
import com.company.employeetracker.ui.theme.*
import com.company.employeetracker.viewmodel.*
import com.company.employeetracker.data.repository.FirebaseRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminDashboardScreen(
    employeeViewModel: EmployeeViewModel = viewModel(),
    taskViewModel: TaskViewModel = viewModel(),
    reviewViewModel: ReviewViewModel = viewModel(),
    messageViewModel: MessageViewModel = viewModel()
) {
    val employees by employeeViewModel.employees.collectAsState()
    val employeeCount by employeeViewModel.employeeCount.collectAsState()
    val allTasks by taskViewModel.allTasks.collectAsState()
    val allReviews by reviewViewModel.allReviews.collectAsState()
    val unreadCount by messageViewModel.unreadCount.collectAsState()

    val scope = rememberCoroutineScope()
    val firebaseRepo = remember { FirebaseRepository() }

    LaunchedEffect(Unit) {
        messageViewModel.loadUnreadCount(1)
        // Force refresh from Firebase
        scope.launch {
            employeeViewModel.forceSync()
        }
    }

    val completedTasks = allTasks.count { it.status == "Done" }
    val pendingTasks = allTasks.size - completedTasks
    val avgRating =
        if (allReviews.isNotEmpty()) allReviews.map { it.overallRating }.average().toFloat() else 0f

    // Top Performers - synced from Firebase reviews
    val topPerformers = remember(allReviews, employees) {
        allReviews
            .groupBy { it.employeeId }
            .mapValues { it.value.map { r -> r.overallRating }.average().toFloat() }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
    }

    // Recent Activities - real-time from Firebase
    val recentActivities = remember(employees, allTasks, allReviews) {
        buildList {
            // New employees
            employees.sortedByDescending { it.joiningDate }.take(2).forEach {
                add(
                    ActivityItem(
                        Icons.Default.PersonAdd,
                        "New Employee",
                        "${it.name} joined ${it.department}",
                        getRelativeTime(it.joiningDate),
                        GreenPrimary,
                        parseDate(it.joiningDate)
                    )
                )
            }

            // Recently completed tasks
            allTasks.filter { it.status == "Done" }
                .sortedByDescending { it.deadline }
                .take(3)
                .forEach {
                    val emp = employees.find { e -> e.id == it.employeeId }
                    add(
                        ActivityItem(
                            Icons.Default.CheckCircle,
                            "Task Completed",
                            "${it.title} by ${emp?.name ?: "Unknown"}",
                            getRelativeTime(it.deadline),
                            AccentBlue,
                            parseDate(it.deadline)
                        )
                    )
                }

            // Recent reviews
            allReviews.sortedByDescending { it.date }.take(2).forEach {
                val emp = employees.find { e -> e.id == it.employeeId }
                add(
                    ActivityItem(
                        Icons.Default.Star,
                        "Performance Review",
                        "${emp?.name ?: "Unknown"} rated ${String.format("%.1f", it.overallRating)}",
                        getRelativeTime(it.date),
                        AccentYellow,
                        parseDate(it.date)
                    )
                )
            }
        }.sortedByDescending { it.timestamp }.take(10)
    }

    var showAllActivities by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6FA))
    ) {

        /** HEADER **/
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(IndigoPrimary, IndigoDark)
                        )
                    )
                    .padding(24.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Column {
                        Text("Welcome Back", color = Color.White, fontSize = 14.sp)
                        Text(
                            "Admin Dashboard",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Insights at a glance",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }

                    Box {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .offset(x = 18.dp, y = (-4).dp)
                                    .clip(CircleShape)
                                    .background(AccentRed)
                            )
                        }
                    }
                }
            }
        }

        /** STATS **/
        item {
            Spacer(Modifier.height(16.dp))
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminStatCard(Icons.Default.People, employeeCount.toString(), "Employees", AccentBlue, Modifier.weight(1f))
                    AdminStatCard(Icons.Default.CheckCircle, completedTasks.toString(), "Completed", GreenPrimary, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminStatCard(Icons.Default.Schedule, pendingTasks.toString(), "Pending", AccentOrange, Modifier.weight(1f))
                    AdminStatCard(Icons.Default.Star, String.format("%.1f", avgRating), "Avg Rating", AccentYellow, Modifier.weight(1f))
                }
            }
        }

        /** TOP PERFORMERS **/
        if (topPerformers.isNotEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Top Performers",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(topPerformers) { (id, rating) ->
                val emp = employees.find { it.id == id }
                emp?.let {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        EmployeeCard(employee = it, rating = rating)
                    }
                }
            }
        }

        /** RECENT ACTIVITIES **/
        item {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Recent Activities", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                if (recentActivities.size > 5) {
                    TextButton(onClick = { showAllActivities = !showAllActivities }) {
                        Text(if (showAllActivities) "Show Less" else "Show All")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column {
                    val list =
                        if (showAllActivities) recentActivities else recentActivities.take(5)

                    if (list.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No recent activities",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        list.forEachIndexed { index, activity ->
                            ActivityItemView(
                                activity = activity,
                                highlight = index == 0
                            )
                            if (index < list.lastIndex) Divider()
                        }
                    }
                }
            }
        }

        /** ADD REVIEW **/
        item {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { showReviewDialog = true },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentYellow),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Star, null)
                Spacer(Modifier.width(8.dp))
                Text("Add Performance Review", fontWeight = FontWeight.Bold)
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }

    if (showReviewDialog) {
        AddReviewDialog(
            onDismiss = { showReviewDialog = false },
            onReviewAdded = {}
        )
    }
}

/** ---------------- COMPONENTS ---------------- **/

@Composable
fun AdminStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(42.dp)
                        .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color)
                }
                Spacer(Modifier.width(12.dp))
                Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Text(label, fontSize = 13.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ActivityItemView(activity: ActivityItem, highlight: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (highlight) activity.iconColor.copy(alpha = 0.06f) else Color.Transparent)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(if (highlight) 14.dp else 10.dp)
                .background(activity.iconColor, CircleShape)
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(activity.title, fontWeight = FontWeight.SemiBold)
            Text(activity.subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Text(activity.time, fontSize = 11.sp, color = Color.Gray)
    }
}

/** ---------------- MODELS & UTILS ---------------- **/

data class ActivityItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val time: String,
    val iconColor: Color,
    val timestamp: Long
)

private fun getRelativeTime(date: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val d = sdf.parse(date) ?: return "Unknown"
        val days = (Date().time - d.time) / (1000 * 60 * 60 * 24)
        when {
            days < 1 -> "Today"
            days == 1L -> "Yesterday"
            days < 7 -> "${days}d ago"
            days < 30 -> "${days / 7}w ago"
            else -> "${days / 30}mo ago"
        }
    } catch (e: Exception) {
        "Unknown"
    }
}

private fun parseDate(date: String): Long {
    return try {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)?.time ?: 0L
    } catch (e: Exception) {
        0L
    }
}