package com.company.employeetracker.ui.screens.admin

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.employeetracker.ui.theme.*
import com.company.employeetracker.viewmodel.EmployeeViewModel
import com.company.employeetracker.viewmodel.ReviewViewModel
import com.company.employeetracker.viewmodel.TaskViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AdminAnalyticsScreen(
    employeeViewModel: EmployeeViewModel = viewModel(),
    taskViewModel: TaskViewModel = viewModel(),
    reviewViewModel: ReviewViewModel = viewModel()
) {
    val context = LocalContext.current
    val employees by employeeViewModel.employees.collectAsState()
    val allTasks by taskViewModel.allTasks.collectAsState()
    val allReviews by reviewViewModel.allReviews.collectAsState()
    val reviewCount by reviewViewModel.reviewCount.collectAsState()

    var showExportDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var showInsights by remember { mutableStateOf(true) }

    // Animated entrance
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    // Calculate analytics
    val topPerformers = allReviews
        .groupBy { it.employeeId }
        .mapValues { entry -> entry.value.map { it.overallRating }.average().toFloat() }
        .toList()
        .sortedByDescending { it.second }
        .take(5)

    val departmentEmployeeCounts = employees.groupBy { it.department }.mapValues { it.value.size }
    val totalEmployees = employees.size

    val completionRate = if (allTasks.isNotEmpty()) {
        (allTasks.count { it.status == "Done" } * 100) / allTasks.size
    } else 0

    val productivityScore = if (allReviews.isNotEmpty()) {
        (allReviews.map { it.overallRating }.average() * 20).toInt()
    } else 0

    val avgTasksPerEmployee = if (totalEmployees > 0) allTasks.size / totalEmployees else 0

    // Trend data
    val performanceTrend = listOf(3.2f, 3.5f, 3.8f, 4.0f, 4.2f, 4.5f)
    val taskCompletionTrend = listOf(45, 52, 58, 65, 72, completionRate)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Animated Header
        item {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it })
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1E293B),
                    shadowElevation = 8.dp
                ) {
                    Box {
                        AnimatedBackgroundParticles()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AnimatedPulsingIcon()
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = "Analytics Hub",
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Real-time insights & performance metrics",
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = { showInsights = !showInsights },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = Color.White.copy(alpha = 0.2f)
                                        )
                                    ) {
                                        Icon(
                                            imageVector = if (showInsights) Icons.Default.Lightbulb else Icons.Default.Lightbulb,
                                            contentDescription = "Toggle Insights",
                                            tint = Color.White
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { showExportDialog = true },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = Color.White.copy(alpha = 0.2f)
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Export",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // AI Insights Section
        if (showInsights) {
            item {
                Spacer(Modifier.height(16.dp))
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn() + slideInHorizontally(initialOffsetX = { -it })
                ) {
                    AIInsightsCard(
                        completionRate = completionRate,
                        productivityScore = productivityScore,
                        topPerformers = topPerformers.size,
                        totalEmployees = totalEmployees
                    )
                }
            }
        }

        // Tab Selector
        item {
            Spacer(Modifier.height(16.dp))
            TabSelector(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }

        // Content based on selected tab
        when (selectedTab) {
            0 -> {
                // Overview Tab
                item {
                    Spacer(Modifier.height(16.dp))
                    AnimatedMetricsGrid(
                        completionRate = completionRate,
                        productivityScore = productivityScore,
                        avgTasksPerEmployee = avgTasksPerEmployee,
                        reviewCount = reviewCount
                    )
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    PerformanceTrendChart(
                        data = performanceTrend,
                        label = "Performance Trend (Last 6 Months)"
                    )
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    TaskCompletionTrendChart(
                        data = taskCompletionTrend,
                        label = "Task Completion Rate (%)"
                    )
                }
            }

            1 -> {
                // Top Performers Tab
                item {
                    Spacer(Modifier.height(16.dp))
                    TopPerformersSection(
                        topPerformers = topPerformers,
                        employees = employees
                    )
                }
            }

            2 -> {
                // Department Tab
                item {
                    Spacer(Modifier.height(16.dp))
                    DepartmentDistributionCard(
                        departmentCounts = departmentEmployeeCounts,
                        totalEmployees = totalEmployees
                    )
                }

                item {
                    Spacer(Modifier.height(16.dp))
                    DepartmentPerformanceComparison(
                        employees = employees,
                        allReviews = allReviews
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(100.dp))
        }
    }

    // Export Dialog
    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExportCSV = {
                exportAsCSV(context, employees, allTasks, allReviews, topPerformers)
                showExportDialog = false
            },
            onExportJSON = {
                exportAsJSON(context, employees, allTasks, allReviews, topPerformers)
                showExportDialog = false
            }
        )
    }
}

// === ANIMATED COMPONENTS ===

@Composable
fun AnimatedBackgroundParticles() {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")

    val particle1X by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "particle1X"
    )

    val particle2Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "particle2Y"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = Color.White.copy(alpha = 0.05f),
            radius = 100f,
            center = Offset(size.width * particle1X, size.height * 0.3f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.08f),
            radius = 80f,
            center = Offset(size.width * 0.7f, size.height * particle2Y)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.06f),
            radius = 60f,
            center = Offset(size.width * 0.2f, size.height * (1 - particle1X) * 0.5f)
        )
    }
}

@Composable
fun AnimatedPulsingIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Icon(
        imageVector = Icons.Default.TrendingUp,
        contentDescription = null,
        tint = AccentYellow,
        modifier = Modifier
            .size(32.dp)
            .scale(scale)
    )
}

@Composable
fun AIInsightsCard(
    completionRate: Int,
    productivityScore: Int,
    topPerformers: Int,
    totalEmployees: Int
) {
    var currentInsight by remember { mutableStateOf(0) }

    val insights = listOf(
        "💡 Task completion is ${if (completionRate > 70) "excellent" else "improving"} at $completionRate%",
        "🚀 Team productivity score: $productivityScore/100",
        "⭐ You have $topPerformers star performers!",
        "👥 Managing $totalEmployees employees effectively"
    )

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(4000)
            currentInsight = (currentInsight + 1) % insights.size
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
                    )
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = currentInsight,
                transitionSpec = {
                    fadeIn() + slideInVertically { it } togetherWith
                            fadeOut() + slideOutVertically { -it }
                },
                label = "insight"
            ) { index ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Insight",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = insights[index],
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TabSelector(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        "Overview" to Icons.Default.Dashboard,
        "Top Performers" to Icons.Default.EmojiEvents,
        "Departments" to Icons.Default.Business
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(tabs) { index, (label, icon) ->
            val isSelected = selectedTab == index
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.9f,
                label = "tabScale"
            )

            Card(
                modifier = Modifier
                    .scale(scale)
                    .clickable { onTabSelected(index) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) GreenPrimary else Color.White
                ),
                elevation = CardDefaults.cardElevation(if (isSelected) 8.dp else 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) Color.White else Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color(0xFF64748B),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedMetricsGrid(
    completionRate: Int,
    productivityScore: Int,
    avgTasksPerEmployee: Int,
    reviewCount: Int
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedMetricCard(
                modifier = Modifier.weight(1f),
                value = "$completionRate%",
                label = "Completion",
                icon = Icons.Default.CheckCircle,
                color = GreenPrimary,
                delay = 0
            )
            AnimatedMetricCard(
                modifier = Modifier.weight(1f),
                value = "$productivityScore",
                label = "Productivity",
                icon = Icons.Default.Speed,
                color = AccentOrange,
                delay = 100
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedMetricCard(
                modifier = Modifier.weight(1f),
                value = "$avgTasksPerEmployee",
                label = "Avg Tasks/Emp",
                icon = Icons.Default.AssignmentInd,
                color = AccentBlue,
                delay = 200
            )
            AnimatedMetricCard(
                modifier = Modifier.weight(1f),
                value = "$reviewCount",
                label = "Reviews",
                icon = Icons.Default.RateReview,
                color = PurplePrimary,
                delay = 300
            )
        }
    }
}

@Composable
fun AnimatedMetricCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    delay: Int = 0
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = value,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Text(
                    text = label,
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun PerformanceTrendChart(
    data: List<Float>,
    label: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = label,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "Trending upward 📈",
                        fontSize = 12.sp,
                        color = GreenPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GreenPrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "+${((data.last() - data.first()) / data.first() * 100).toInt()}%",
                        color = GreenPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            AnimatedLineChart(
                data = data,
                color = GreenPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        }
    }
}

@Composable
fun AnimatedLineChart(
    data: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    var animationProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(1500, easing = FastOutSlowInEasing)
        ) { value, _ ->
            animationProgress = value
        }
    }

    Canvas(modifier = modifier.padding(vertical = 8.dp)) {
        val maxValue = data.maxOrNull() ?: 5f
        val spacing = size.width / (data.size - 1)
        val heightScale = size.height / maxValue

        val path = Path()
        data.forEachIndexed { index, value ->
            val x = index * spacing
            val y = size.height - (value * heightScale * animationProgress)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                val prevX = (index - 1) * spacing
                val prevY = size.height - (data[index - 1] * heightScale * animationProgress)
                val controlX1 = prevX + spacing / 2
                val controlX2 = x - spacing / 2

                path.cubicTo(controlX1, prevY, controlX2, y, x, y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 4.dp.toPx())
        )

        // Draw points
        data.forEachIndexed { index, value ->
            val x = index * spacing
            val y = size.height - (value * heightScale * animationProgress)

            drawCircle(
                color = color,
                radius = 6.dp.toPx(),
                center = Offset(x, y)
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun TaskCompletionTrendChart(
    data: List<Int>,
    label: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            Spacer(Modifier.height(24.dp))

            AnimatedBarChart(
                data = data,
                color = AccentBlue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}

@Composable
fun AnimatedBarChart(
    data: List<Int>,
    color: Color,
    modifier: Modifier = Modifier
) {
    var animationProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(1200, easing = FastOutSlowInEasing)
        ) { value, _ ->
            animationProgress = value
        }
    }

    Canvas(modifier = modifier) {
        val maxValue = data.maxOrNull()?.toFloat() ?: 100f
        val barWidth = (size.width / data.size) * 0.7f
        val spacing = size.width / data.size

        data.forEachIndexed { index, value ->
            val barHeight = (value / maxValue) * size.height * animationProgress
            val x = index * spacing + (spacing - barWidth) / 2

            drawRoundRect(
                color = color.copy(alpha = 0.2f),
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
            )

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(color, color.copy(alpha = 0.6f)),
                    startY = size.height - barHeight,
                    endY = size.height
                ),
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
            )
        }
    }
}

@Composable
fun TopPerformersSection(
    topPerformers: List<Pair<Int, Float>>,
    employees: List<com.company.employeetracker.data.database.entities.User>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = AccentYellow,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Top Performers 🏆",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }

            Spacer(Modifier.height(20.dp))

            topPerformers.forEachIndexed { index, (empId, rating) ->
                val employee = employees.find { it.id == empId }
                employee?.let {
                    AnimatedPerformerCard(
                        rank = index + 1,
                        employee = it,
                        rating = rating,
                        delay = index * 100
                    )
                    if (index < topPerformers.size - 1) {
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedPerformerCard(
    rank: Int,
    employee: com.company.employeetracker.data.database.entities.User,
    rating: Float,
    delay: Int
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it })
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when (rank) {
                    1 -> Color(0xFFFFD700).copy(alpha = 0.1f)
                    2 -> Color(0xFFC0C0C0).copy(alpha = 0.1f)
                    3 -> Color(0xFFCD7F32).copy(alpha = 0.1f)
                    else -> Color(0xFFF8F9FA)
                }
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Medal
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            when (rank) {
                                1 -> Color(0xFFFFD700)
                                2 -> Color(0xFFC0C0C0)
                                3 -> Color(0xFFCD7F32)
                                else -> Color(0xFF94A3B8)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (rank <= 3) {
                            when (rank) {
                                1 -> "🥇"
                                2 -> "🥈"
                                else -> "🥉"
                            }
                        } else "#$rank",
                        color = if (rank > 3) Color.White else Color.Transparent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(16.dp))

                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when (employee.department) {
                                "Design" -> Color(0xFF8B5CF6)
                                "Engineering" -> Color(0xFF3B82F6)
                                "Analytics" -> Color(0xFFF59E0B)
                                else -> Color(0xFF10B981)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = employee.name.split(" ")
                            .mapNotNull { it.firstOrNull() }
                            .take(2)
                            .joinToString(""),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Employee info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = employee.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "${employee.designation} • ${employee.department}",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // Rating
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = AccentYellow,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = String.format("%.1f", rating),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
            }
        }
    }
}

@Composable
fun DepartmentDistributionCard(
    departmentCounts: Map<String, Int>,
    totalEmployees: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Department Distribution",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            Spacer(Modifier.height(24.dp))

            AnimatedPieChart(
                data = departmentCounts,
                total = totalEmployees,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )

            Spacer(Modifier.height(24.dp))

            // Legend
            departmentCounts.entries.forEachIndexed { index, (dept, count) ->
                DepartmentLegendItem(
                    department = dept,
                    count = count,
                    percentage = (count * 100) / totalEmployees,
                    color = getDepartmentColor(index),
                    delay = index * 100
                )
                if (index < departmentCounts.size - 1) {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun AnimatedPieChart(
    data: Map<String, Int>,
    total: Int,
    modifier: Modifier = Modifier
) {
    var animationProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(1500, easing = FastOutSlowInEasing)
        ) { value, _ ->
            animationProgress = value
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2 * 0.7f
            val center = Offset(size.width / 2, size.height / 2)
            var startAngle = -90f

            data.entries.forEachIndexed { index, (_, count) ->
                val sweepAngle = (count.toFloat() / total) * 360f * animationProgress
                val color = getDepartmentColor(index)

                drawArc(
                    color = color.copy(alpha = 0.9f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )

                startAngle += sweepAngle
            }

            // Center hole for donut effect
            drawCircle(
                color = Color.White,
                radius = radius * 0.5f,
                center = center
            )
        }

        // Center text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$total",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = "Employees",
                fontSize = 14.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
fun DepartmentLegendItem(
    department: String,
    count: Int,
    percentage: Int,
    color: Color,
    delay: Int
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { -it })
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = department,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$count",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "($percentage%)",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
fun DepartmentPerformanceComparison(
    employees: List<com.company.employeetracker.data.database.entities.User>,
    allReviews: List<com.company.employeetracker.data.database.entities.Review>
) {
    val deptPerformance = employees.groupBy { it.department }
        .mapValues { (_, emps) ->
            val empIds = emps.map { it.id }
            val reviews = allReviews.filter { it.employeeId in empIds }
            if (reviews.isNotEmpty()) {
                reviews.map { it.overallRating }.average().toFloat()
            } else 0f
        }
        .toList()
        .sortedByDescending { it.second }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Department Performance",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            Spacer(Modifier.height(20.dp))

            deptPerformance.forEachIndexed { index, (dept, rating) ->
                DepartmentPerformanceBar(
                    department = dept,
                    rating = rating,
                    delay = index * 100
                )
                if (index < deptPerformance.size - 1) {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun DepartmentPerformanceBar(
    department: String,
    rating: Float,
    delay: Int
) {
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        animate(
            initialValue = 0f,
            targetValue = rating / 5f,
            animationSpec = tween(1000, easing = FastOutSlowInEasing)
        ) { value, _ ->
            progress = value
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = department,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF475569)
            )
            Text(
                text = String.format("%.1f/5.0", rating),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = getPerformanceColor(rating)
            )
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF1F5F9))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                getPerformanceColor(rating),
                                getPerformanceColor(rating).copy(alpha = 0.7f)
                            )
                        ),
                        shape = RoundedCornerShape(6.dp)
                    )
            )
        }
    }
}

@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onExportCSV: () -> Unit,
    onExportJSON: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = GreenPrimary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Export Analytics",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text("Choose export format:")
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onExportCSV,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.TableChart, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Export as CSV")
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = onExportJSON,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Code, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Export as JSON")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper functions
fun getDepartmentColor(index: Int): Color {
    val colors = listOf(
        Color(0xFF3B82F6),
        Color(0xFF8B5CF6),
        Color(0xFF10B981),
        Color(0xFFF59E0B),
        Color(0xFFEF4444),
        Color(0xFF64748B)
    )
    return colors[index % colors.size]
}

fun getPerformanceColor(rating: Float): Color {
    return when {
        rating >= 4.5f -> Color(0xFF10B981)
        rating >= 3.5f -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
}

// Export functions
private fun exportAsCSV(
    context: Context,
    employees: List<com.company.employeetracker.data.database.entities.User>,
    tasks: List<com.company.employeetracker.data.database.entities.Task>,
    reviews: List<com.company.employeetracker.data.database.entities.Review>,
    topPerformers: List<Pair<Int, Float>>
) {
    try {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "analytics_export_$timestamp.csv"
        val file = File(context.getExternalFilesDir(null), fileName)

        FileWriter(file).use { writer ->
            writer.append("ANALYTICS SUMMARY\n")
            writer.append("Total Employees,${employees.size}\n")
            writer.append("Total Tasks,${tasks.size}\n")
            writer.append("Total Reviews,${reviews.size}\n\n")

            writer.append("TOP PERFORMERS\n")
            writer.append("Rank,Name,Department,Rating\n")
            topPerformers.forEachIndexed { index, (empId, rating) ->
                val emp = employees.find { it.id == empId }
                writer.append("${index + 1},${emp?.name},${emp?.department},${"%.2f".format(rating)}\n")
            }

            writer.append("\nALL EMPLOYEES\n")
            writer.append("Name,Email,Department,Designation,Joining Date\n")
            employees.forEach { emp ->
                writer.append("${emp.name},${emp.email},${emp.department},${emp.designation},${emp.joiningDate}\n")
            }
        }

        shareFile(context, file)
    } catch (e: Exception) {
        android.util.Log.e("Export", "CSV export failed", e)
    }
}

private fun exportAsJSON(
    context: Context,
    employees: List<com.company.employeetracker.data.database.entities.User>,
    tasks: List<com.company.employeetracker.data.database.entities.Task>,
    reviews: List<com.company.employeetracker.data.database.entities.Review>,
    topPerformers: List<Pair<Int, Float>>
) {
    try {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "analytics_export_$timestamp.json"
        val file = File(context.getExternalFilesDir(null), fileName)

        val json = JSONObject().apply {
            put("exportDate", timestamp)
            put("summary", JSONObject().apply {
                put("totalEmployees", employees.size)
                put("totalTasks", tasks.size)
                put("totalReviews", reviews.size)
            })

            put("topPerformers", JSONArray().apply {
                topPerformers.forEach { (empId, rating) ->
                    val emp = employees.find { it.id == empId }
                    put(JSONObject().apply {
                        put("name", emp?.name)
                        put("department", emp?.department)
                        put("rating", rating)
                    })
                }
            })

            put("employees", JSONArray().apply {
                employees.forEach { emp ->
                    put(JSONObject().apply {
                        put("name", emp.name)
                        put("email", emp.email)
                        put("department", emp.department)
                        put("designation", emp.designation)
                    })
                }
            })
        }

        FileWriter(file).use { it.write(json.toString(2)) }
        shareFile(context, file)
    } catch (e: Exception) {
        android.util.Log.e("Export", "JSON export failed", e)
    }
}

private fun shareFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = if (file.name.endsWith(".csv")) "text/csv" else "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(intent, "Share Analytics"))
}