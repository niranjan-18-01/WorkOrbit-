package com.company.employeetracker.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.employeetracker.ui.components.EmployeeCard
import com.company.employeetracker.ui.components.AddEmployeeDialog
import com.company.employeetracker.ui.theme.*
import com.company.employeetracker.viewmodel.EmployeeViewModel
import com.company.employeetracker.viewmodel.ReviewViewModel
import com.company.employeetracker.viewmodel.TaskViewModel
import com.company.employeetracker.ui.components.EmptyStateScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEmployeesScreen(
    employeeViewModel: EmployeeViewModel = viewModel(),
    taskViewModel: TaskViewModel = viewModel(),
    reviewViewModel: ReviewViewModel = viewModel()
) {
    val employees by employeeViewModel.employees.collectAsState()
    val allTasks by taskViewModel.allTasks.collectAsState()
    val allReviews by reviewViewModel.allReviews.collectAsState()

    // Moved to top-level so both the Add button and EmptyState CTA can toggle it
    var showAddEmployeeDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedDepartment by remember { mutableStateOf("All Departments") }
    var showMenu by remember { mutableStateOf(false) }

    val departments = listOf("All Departments", "Engineering", "Design", "Analytics", "Product", "Management")

    val filteredEmployees = employees.filter { employee ->
        val matchesSearch = employee.name.contains(searchQuery, ignoreCase = true) ||
                employee.designation.contains(searchQuery, ignoreCase = true)
        val matchesDepartment = selectedDepartment == "All Departments" ||
                employee.department == selectedDepartment
        matchesSearch && matchesDepartment
    }

    val activeEmployees = employees.count { emp ->
        allTasks.any { it.employeeId == emp.id && it.status == "Active" }
    }

    val avgRating = if (allReviews.isNotEmpty()) {
        allReviews.map { it.overallRating }.average().toFloat()
    } else 0f

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
                    Column {
                        Text(
                            text = "Team Members",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Employees",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "Employees",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Search Bar
            item {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Search employees...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF757575)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF757575)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }

            // Department Filter
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = Color(0xFF757575),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    var expanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedDepartment,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreenPrimary,
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            departments.forEach { dept ->
                                DropdownMenuItem(
                                    text = { Text(dept) },
                                    onClick = {
                                        selectedDepartment = dept
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Stats Row
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
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${filteredEmployees.size}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentBlue
                            )
                            Text(
                                text = "Total",
                                fontSize = 12.sp,
                                color = Color(0xFF757575)
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$activeEmployees",
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

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = String.format("%.1f", avgRating),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentYellow
                                )
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Rating",
                                    tint = AccentYellow,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Avg Rating",
                                fontSize = 12.sp,
                                color = Color(0xFF757575)
                            )
                        }
                    }
                }
            }

            // Employee List Header
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "All Employees (${filteredEmployees.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Empty state when no employees match filters/search
            if (filteredEmployees.isEmpty()) {
                item {
                    EmptyStateScreen(
                        icon = Icons.Default.People,
                        title = "No Employees Found",
                        message = if (searchQuery.isNotEmpty()) {
                            "No employees match your search criteria."
                        } else {
                            "Start by adding your first employee to the system."
                        },
                        actionText = if (searchQuery.isEmpty()) "Add Employee" else null,
                        onActionClick = if (searchQuery.isEmpty()) {
                            { showAddEmployeeDialog = true }
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    )
                }
            } else {
                // Employee Cards (use stable keys)
                items(items = filteredEmployees, key = { it.id }) { employee ->
                    Spacer(modifier = Modifier.height(12.dp))
                    val employeeTasks = allTasks.filter { it.employeeId == employee.id }
                    val employeeReviews = allReviews.filter { it.employeeId == employee.id }
                    val employeeRating = if (employeeReviews.isNotEmpty()) {
                        employeeReviews.map { it.overallRating }.average().toFloat()
                    } else 0f

                    EmployeeCard(
                        employee = employee,
                        taskCount = employeeTasks.size,
                        rating = employeeRating,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Add Employee Button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showAddEmployeeDialog = true },
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
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add New Employee",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // Add Employee Dialog (outside LazyColumn so it can overlay)
    if (showAddEmployeeDialog) {
        AddEmployeeDialog(
            onDismiss = { showAddEmployeeDialog = false },
            onEmployeeAdded = {
                // Employee added successfully - close dialog and optionally refresh data
                showAddEmployeeDialog = false
            }
        )
    }
}
