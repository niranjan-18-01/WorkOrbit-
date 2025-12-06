package com.company.employeetracker.ui.screens.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.employeetracker.ui.theme.*
import com.company.employeetracker.viewmodel.EmployeeViewModel
import com.company.employeetracker.viewmodel.ReviewViewModel
import com.company.employeetracker.viewmodel.TaskViewModel

@Composable
fun AdminAnalyticsScreen(
    employeeViewModel: EmployeeViewModel = viewModel(),
    taskViewModel: TaskViewModel = viewModel(),
    reviewViewModel: ReviewViewModel = viewModel()
) {
    val employees by employeeViewModel.employees.collectAsState()
    val allTasks by taskViewModel.allTasks.collectAsState()
    val allReviews by reviewViewModel.allReviews.collectAsState()
    val reviewCount by reviewViewModel.reviewCount.collectAsState()

    // Calculate analytics
    val topPerformers = allReviews
        .groupBy { it.employeeId }
        .mapValues { entry -> entry.value.map { it.overallRating }.average().toFloat() }
        .toList()
        .sortedByDescending { it.second }
        .take(3)

    val departmentEmployeeCounts = employees.groupBy { it.department }.mapValues { it.value.size }
    val totalEmployees = employees.size

    val completionRate = if (allTasks.isNotEmpty()) {
        (allTasks.count { it.status == "Done" } * 100) / allTasks.size
    } else 0

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Analytics & Insights",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Performance metrics and trends",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    IconButton(onClick = { /* Export */ }) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Analytics",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Top Performers Section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Top Performers",
                            tint = AccentYellow,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Top Performers",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    topPerformers.forEachIndexed { index, (employeeId, rating) ->
                        val employee = employees.find { it.id == employeeId }
                        employee?.let {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = when (index) {
                                            0 -> Color(0xFFFFD700).copy(alpha = 0.2f)
                                            1 -> Color(0xFFC0C0C0).copy(alpha = 0.2f)
                                            2 -> Color(0xFFCD7F32).copy(alpha = 0.2f)
                                            else -> Color(0xFF9E9E9E).copy(alpha = 0.2f)
                                        }
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            modifier = Modifier.padding(8.dp),
                                            fontWeight = FontWeight.Bold,
                                            color = when (index) {
                                                0 -> Color(0xFFFFD700)
                                                1 -> Color(0xFF757575)
                                                2 -> Color(0xFFCD7F32)
                                                else -> Color(0xFF9E9E9E)
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (it.department) {
                                                    "Design" -> Color(0xFF9C27B0)
                                                    "Engineering" -> AccentBlue
                                                    "Analytics" -> AccentOrange
                                                    else -> GreenPrimary
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = it.name.split(" ").mapNotNull { word -> word.firstOrNull() }
                                                .take(2).joinToString(""),
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = it.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = it.department,
                                            fontSize = 12.sp,
                                            color = Color(0xFF757575)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Rating",
                                        tint = AccentYellow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format("%.1f", rating),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (index < topPerformers.size - 1) {
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }
        }

        // Employee Ratings Chart
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Employee Ratings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    employees.take(5).forEach { employee ->
                        val employeeReviews = allReviews.filter { it.employeeId == employee.id }
                        val avgRating = if (employeeReviews.isNotEmpty()) {
                            employeeReviews.map { it.overallRating }.average().toFloat()
                        } else 0f

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = employee.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                LinearProgressIndicator(
                                    progress = avgRating / 5f,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .padding(top = 4.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = IndigoPrimary,
                                    trackColor = Color(0xFFE0E0E0)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = String.format("%.1f", avgRating),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = IndigoPrimary
                            )
                        }
                    }
                }
            }
        }

        // Department Distribution
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Department Distribution",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Pie Chart
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DepartmentPieChart(
                            data = departmentEmployeeCounts,
                            total = totalEmployees
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Legend
                    val departmentColors = mapOf(
                        "Engineering" to AccentBlue,
                        "Design" to Color(0xFF9C27B0),
                        "Product" to AccentGreen,
                        "Marketing" to AccentOrange,
                        "Analytics" to AccentOrange,
                        "Others" to AccentRed
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        departmentEmployeeCounts.forEach { (dept, count) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(
                                                departmentColors[dept] ?: Color(0xFF757575)
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = dept,
                                        fontSize = 14.sp,
                                        color = Color(0xFF424242)
                                    )
                                }
                                Text(
                                    text = "${(count * 100 / totalEmployees)}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF212121)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Performance Trend
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Performance Trend",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simple area chart representation
                    PerformanceTrendChart(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Trend",
                            tint = GreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "23% increase from last quarter",
                            fontSize = 14.sp,
                            color = GreenPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Summary Cards
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
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$reviewCount",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary
                        )
                        Text(
                            text = "Total Reviews",
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$completionRate%",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                        Text(
                            text = "Completion Rate",
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }
            }
        }

        // Export Section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Export Reports",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Download comprehensive analytics",
                        fontSize = 14.sp,
                        color = Color(0xFF757575)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { /* Export */ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun DepartmentPieChart(
    data: Map<String, Int>,
    total: Int
) {
    val departmentColors = mapOf(
        "Engineering" to AccentBlue,
        "Design" to Color(0xFF9C27B0),
        "Product" to AccentGreen,
        "Marketing" to AccentOrange,
        "Analytics" to AccentOrange,
        "Others" to AccentRed
    )

    Canvas(modifier = Modifier.size(180.dp)) {
        val radius = size.minDimension / 2
        val center = Offset(size.width / 2, size.height / 2)
        var startAngle = -90f

        data.forEach { (dept, count) ->
            val sweepAngle = (count.toFloat() / total) * 360f
            val color = departmentColors[dept] ?: Color(0xFF757575)

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
            startAngle += sweepAngle
        }

        // Center circle for donut effect
        drawCircle(
            color = Color.White,
            radius = radius * 0.5f,
            center = center
        )

        // Draw border
        drawCircle(
            color = Color(0xFFE0E0E0),
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun PerformanceTrendChart(modifier: Modifier = Modifier) {
    val data = listOf(2.5f, 3.0f, 2.8f, 3.5f, 4.0f, 3.8f, 4.5f)

    Canvas(modifier = modifier.padding(16.dp)) {
        val spacing = size.width / (data.size - 1)
        val maxValue = 5f
        val points = data.mapIndexed { index, value ->
            Offset(
                x = index * spacing,
                y = size.height - (value / maxValue) * size.height
            )
        }

        // Draw area under curve
        val path = androidx.compose.ui.graphics.Path()
        path.moveTo(0f, size.height)
        points.forEach { point ->
            path.lineTo(point.x, point.y)
        }
        path.lineTo(size.width, size.height)
        path.close()

        drawPath(
            path = path,
            color = GreenPrimary.copy(alpha = 0.2f)
        )

        // Draw line
        for (i in 0 until points.size - 1) {
            drawLine(
                color = GreenPrimary,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 3.dp.toPx()
            )
        }

        // Draw points
        points.forEach { point ->
            drawCircle(
                color = GreenPrimary,
                radius = 6.dp.toPx(),
                center = point
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = point
            )
        }
    }
}