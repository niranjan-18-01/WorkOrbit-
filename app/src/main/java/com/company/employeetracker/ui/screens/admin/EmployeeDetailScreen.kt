package com.company.employeetracker.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.employeetracker.data.database.entities.Attendance
import com.company.employeetracker.data.database.entities.User
import com.company.employeetracker.ui.theme.*
import com.company.employeetracker.viewmodel.AttendanceViewModel
import com.company.employeetracker.viewmodel.ReviewViewModel
import com.company.employeetracker.viewmodel.TaskViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDetailScreen(
    employee: User,
    employees: List<User> = listOf(employee),
    defaultCheckIn: LocalTime = LocalTime.of(9, 0),
    defaultCheckOut: LocalTime = LocalTime.of(18, 0),
    onBackClick: () -> Unit = {},
    onDeleteEmployee: (User) -> Unit = {},
    taskViewModel: TaskViewModel = viewModel(),
    reviewViewModel: ReviewViewModel = viewModel(),
    attendanceViewModel: AttendanceViewModel = viewModel()
) {
    LaunchedEffect(employee.id) {
        taskViewModel.loadTasksForEmployee(employee.id)
        reviewViewModel.loadReviewsForEmployee(employee.id)
        attendanceViewModel.loadAttendanceForEmployee(employee.id)
    }

    val tasks by taskViewModel.employeeTasks.collectAsState()
    val attendance by attendanceViewModel.employeeAttendance.collectAsState()
    val averageRating by reviewViewModel.averageRating.collectAsState()

    var showMarkAttendanceDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val avatarColor = when (employee.department) {
        "Design" -> Color(0xFF9C27B0)
        "Engineering" -> AccentBlue
        "Analytics" -> AccentOrange
        "Product" -> Color(0xFF00BCD4)
        else -> GreenPrimary
    }

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
                            colors = listOf(avatarColor, avatarColor.copy(alpha = 0.8f))
                        )
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color.White.copy(alpha = 0.2f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { showDeleteDialog = true },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = AccentRed.copy(alpha = 0.2f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = employee.name.split(" ").mapNotNull { it.firstOrNull() }
                                .take(2).joinToString(""),
                            color = avatarColor,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Employee Name
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = employee.name,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = employee.designation,
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = employee.department,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Stats Cards
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
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = "Tasks",
                            tint = AccentBlue,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = tasks.size.toString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tasks",
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = AccentYellow,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f", averageRating),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Rating",
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Present",
                            tint = GreenPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = attendance.count { it.status == "Present" }.toString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Present",
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }
            }
        }

        // Employee Info
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Employee Information",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    InfoRow("Email", employee.email)
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow("Department", employee.department)
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow("Designation", employee.designation)
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow("Joining Date", employee.joiningDate)
                    if (employee.contact.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow("Contact", employee.contact)
                    }
                }
            }
        }

        // Mark Attendance Button
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { showMarkAttendanceDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Mark Attendance"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mark Attendance",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Monthly Attendance Calendar
        item {
            Spacer(modifier = Modifier.height(24.dp))
            MonthlyAttendanceCalendar(
                attendance = attendance,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Mark Attendance Dialog
    if (showMarkAttendanceDialog) {
        MarkAttendanceDialog(
            employee = employee,
            onDismiss = { showMarkAttendanceDialog = false },
            onAttendanceMarked = { attendanceItem ->
                attendanceViewModel.markAttendance(attendanceItem)
                showMarkAttendanceDialog = false
            }
        )
    }

    // Delete Confirmation Dialog - FIXED WITH PROPER CALLBACK
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
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
                    text = "Delete Employee?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("Are you sure you want to delete ${employee.name}?")
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "This will also permanently delete:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("• All assigned tasks", fontSize = 13.sp)
                        Text("• Performance reviews", fontSize = 13.sp)
                        Text("• Attendance records", fontSize = 13.sp)
                        Text("• Message history", fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "This action cannot be undone!",
                        color = AccentRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        android.util.Log.d("EmployeeDetail", "Deleting employee: ${employee.name} (ID: ${employee.id})")
                        onDeleteEmployee(employee) // This now properly triggers deletion
                        showDeleteDialog = false
                        onBackClick() // Navigate back after deletion
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MonthlyAttendanceCalendar(
    attendance: List<Attendance>,
    modifier: Modifier = Modifier
) {
    val currentMonth = remember { YearMonth.now() }
    val startDate = currentMonth.atDay(1)
    val endDate = currentMonth.atEndOfMonth()

    // Create attendance map by date
    val attendanceMap = remember(attendance) {
        attendance.associateBy { it.date }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Attendance - ${currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${attendance.count { it.status == "Present" }} days present",
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                }
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = GreenPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Day labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                    Text(
                        text = day,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar grid
            var currentDate = startDate.minusDays(startDate.dayOfWeek.value.toLong() % 7)

            while (currentDate.isBefore(endDate.plusDays(1))) {
                val weekStart = currentDate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in 0..6) {
                        val date = weekStart.plusDays(i.toLong())
                        val dateStr = date.toString()
                        val record = attendanceMap[dateStr]
                        val isCurrentMonth = date.month == currentMonth.month

                        val color = when {
                            !isCurrentMonth -> Color.Transparent
                            record?.status == "Present" -> GreenPrimary
                            record?.status == "Absent" -> AccentRed
                            record?.status == "Half Day" -> AccentOrange
                            record?.status == "Leave" -> AccentBlue
                            date.isAfter(LocalDate.now()) -> Color(0xFFF5F5F5)
                            else -> Color(0xFFEEEEEE)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(color)
                                .then(
                                    if (date == LocalDate.now()) {
                                        Modifier.border(
                                            2.dp,
                                            GreenPrimary,
                                            RoundedCornerShape(6.dp)
                                        )
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCurrentMonth) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    fontSize = 10.sp,
                                    fontWeight = if (record != null) FontWeight.Bold else FontWeight.Normal,
                                    color = if (record != null) Color.White else Color(0xFF424242)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                currentDate = currentDate.plusWeeks(1)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(GreenPrimary, "Present")
                LegendItem(AccentRed, "Absent")
                LegendItem(AccentOrange, "Half Day")
                LegendItem(AccentBlue, "Leave")
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF757575)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF212121)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkAttendanceDialog(
    employee: User,
    onDismiss: () -> Unit,
    onAttendanceMarked: (Attendance) -> Unit
) {
    val today = LocalDate.now()
    var selectedStatus by remember { mutableStateOf("Present") }
    var remarks by remember { mutableStateOf("") }
    var showCheckInPicker by remember { mutableStateOf(false) }
    var showCheckOutPicker by remember { mutableStateOf(false) }
    val isAmPmEnabled = true

    val defaultCheckIn = if (isAmPmEnabled) "09:00 AM" else "09:00"
    val defaultCheckOut = if (isAmPmEnabled) "06:00 PM" else "18:00"

    var checkInTime by remember { mutableStateOf(defaultCheckIn) }
    var checkOutTime by remember { mutableStateOf(defaultCheckOut) }


    AlertDialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Mark Attendance",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${employee.name} • ${today.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
                    fontSize = 14.sp,
                    color = Color(0xFF757575)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Status Selection
                Text("Status", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Present", "Absent").forEach { status ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status },
                            label = { Text(status, fontSize = 12.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Half Day", "Leave").forEach { status ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status },
                            label = { Text(status, fontSize = 12.sp) }
                        )
                    }
                }

                if (selectedStatus == "Present" || selectedStatus == "Half Day") {
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = checkInTime,
                        onValueChange = {},
                        label = { Text("Check In Time") },
                        readOnly = true,
                        leadingIcon = {
                            IconButton(onClick = { showCheckInPicker = true }) {
                                Icon(Icons.Default.AccessTime, contentDescription = "Pick Check In Time")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )


                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = checkOutTime,
                        onValueChange = {},
                        label = { Text("Check Out Time") },
                        readOnly = true,
                        leadingIcon = {
                            IconButton(onClick = { showCheckOutPicker = true }) {
                                Icon(Icons.Default.AccessTime, contentDescription = "Pick Check Out Time")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val attendance = Attendance(
                                employeeId = employee.id,
                                date = today.toString(),
                                checkInTime = if (selectedStatus == "Present" || selectedStatus == "Half Day") checkInTime else "",
                                checkOutTime = if (selectedStatus == "Present" || selectedStatus == "Half Day") checkOutTime else null,
                                status = selectedStatus,
                                remarks = remarks,
                                markedBy = "Admin"
                            )
                            onAttendanceMarked(attendance)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                    ) {
                        Text("Mark")
                    }
                }
            }
        }
    }
    if (showCheckInPicker) {
        TimePickerDialog(
            initialTime = checkInTime,
            isAmPmEnabled = isAmPmEnabled,
            onTimeSelected = {
                checkInTime = it
                showCheckInPicker = false
            },
            onDismiss = { showCheckInPicker = false }
        )
    }

    if (showCheckOutPicker) {
        TimePickerDialog(
            initialTime = checkOutTime,
            isAmPmEnabled = isAmPmEnabled,
            onTimeSelected = {
                checkOutTime = it
                showCheckOutPicker = false
            },
            onDismiss = { showCheckOutPicker = false }
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialTime: String,
    isAmPmEnabled: Boolean,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val formatter = if (isAmPmEnabled) {
        DateTimeFormatter.ofPattern("hh:mm a")
    } else {
        DateTimeFormatter.ofPattern("HH:mm")
    }

    val localTime = try {
        LocalTime.parse(initialTime, formatter)
    } catch (e: Exception) {
        LocalTime.of(9, 0)
    }

    val timePickerState = rememberTimePickerState(
        initialHour = localTime.hour,
        initialMinute = localTime.minute,
        is24Hour = !isAmPmEnabled
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedTime = LocalTime.of(
                        timePickerState.hour,
                        timePickerState.minute
                    )
                    onTimeSelected(selectedTime.format(formatter))
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}
