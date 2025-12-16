package com.company.employeetracker.ui.screens.admin

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.*
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Header
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1E293B),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
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
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

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

        // Top Performers Section
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
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Top Performers",
                            tint = Color(0xFFFFB020),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Top Performers",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
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
                                            else -> Color(0xFF64748B).copy(alpha = 0.2f)
                                        }
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            modifier = Modifier.padding(8.dp),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = when (index) {
                                                0 -> Color(0xFFFFD700)
                                                1 -> Color(0xFF94A3B8)
                                                2 -> Color(0xFFCD7F32)
                                                else -> Color(0xFF64748B)
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
                                                    "Design" -> Color(0xFF8B5CF6)
                                                    "Engineering" -> Color(0xFF3B82F6)
                                                    "Analytics" -> Color(0xFFF59E0B)
                                                    else -> Color(0xFF10B981)
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
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = it.department,
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Rating",
                                        tint = Color(0xFFFFB020),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format("%.1f", rating),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                            }

                            if (index < topPerformers.size - 1) {
                                Divider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = Color(0xFFE2E8F0)
                                )
                            }
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
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Department Distribution",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Pie Chart
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
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
                        "Engineering" to Color(0xFF3B82F6),
                        "Design" to Color(0xFF8B5CF6),
                        "Product" to Color(0xFF10B981),
                        "Marketing" to Color(0xFFF59E0B),
                        "Analytics" to Color(0xFFEF4444),
                        "Others" to Color(0xFF64748B)
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
                                                departmentColors[dept] ?: Color(0xFF64748B)
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = dept,
                                        fontSize = 14.sp,
                                        color = Color(0xFF475569)
                                    )
                                }
                                Text(
                                    text = "${(count * 100 / totalEmployees)}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B)
                                )
                            }
                        }
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
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
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
                            color = Color(0xFF16A34A)
                        )
                        Text(
                            text = "Total Reviews",
                            fontSize = 12.sp,
                            color = Color(0xFF15803D)
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDEF7EC)),
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
                            color = Color(0xFF047857)
                        )
                        Text(
                            text = "Completion Rate",
                            fontSize = 12.sp,
                            color = Color(0xFF065F46)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
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
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            exportAsCSV(context, employees, allTasks, allReviews, topPerformers)
                            showExportDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                    ) {
                        Icon(Icons.Default.TableChart, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export as CSV")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            exportAsJSON(context, employees, allTasks, allReviews, topPerformers)
                            showExportDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Icon(Icons.Default.Code, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export as JSON")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DepartmentPieChart(
    data: Map<String, Int>,
    total: Int
) {
    val departmentColors = mapOf(
        "Engineering" to Color(0xFF3B82F6),
        "Design" to Color(0xFF8B5CF6),
        "Product" to Color(0xFF10B981),
        "Marketing" to Color(0xFFF59E0B),
        "Analytics" to Color(0xFFEF4444),
        "Others" to Color(0xFF64748B)
    )

    Canvas(modifier = Modifier.size(180.dp)) {
        val radius = size.minDimension / 2
        val center = Offset(size.width / 2, size.height / 2)
        var startAngle = -90f

        data.forEach { (dept, count) ->
            val sweepAngle = (count.toFloat() / total) * 360f
            val color = departmentColors[dept] ?: Color(0xFF64748B)

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
    }
}

// Export Functions
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
            // Summary
            writer.append("ANALYTICS SUMMARY\n")
            writer.append("Total Employees,${employees.size}\n")
            writer.append("Total Tasks,${tasks.size}\n")
            writer.append("Total Reviews,${reviews.size}\n\n")

            // Top Performers
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