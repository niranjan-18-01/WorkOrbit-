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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.employeetracker.data.database.entities.User
import com.company.employeetracker.ui.components.*
import com.company.employeetracker.ui.theme.*
import com.company.employeetracker.viewmodel.*
import androidx.compose.ui.graphics.vector.ImageVector

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

    var selectedEmployee by remember { mutableStateOf<User?>(null) }
    var showAddEmployeeDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedDepartment by remember { mutableStateOf("All Departments") }

    val departments = listOf(
        "All Departments", "Engineering", "Design", "Analytics", "Product", "Management"
    )

    val filteredEmployees = employees.filter {
        (it.name.contains(searchQuery, true) ||
                it.designation.contains(searchQuery, true)) &&
                (selectedDepartment == "All Departments" || it.department == selectedDepartment)
    }

    val activeEmployees = employees.count { emp ->
        allTasks.any { it.employeeId == emp.id && it.status == "Active" }
    }

    val avgRating =
        if (allReviews.isNotEmpty()) allReviews.map { it.overallRating }.average().toFloat() else 0f

    if (selectedEmployee != null) {
        EmployeeDetailScreen(
            employee = selectedEmployee!!,
            employees = employees,
            onBackClick = { selectedEmployee = null },
            onDeleteEmployee = { employee ->
                employeeViewModel.deleteEmployee(employee)
                selectedEmployee = null
            }
        )
        return
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddEmployeeDialog = true },
                containerColor = GreenPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Employee")
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {

            /** HEADER **/
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(GreenPrimary, GreenDark)
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
                        Text("Team Overview", color = Color.White, fontSize = 14.sp)
                        Text(
                            "Employees",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            LazyColumn {

                /** SEARCH **/
                item {
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search employees…") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                /** FILTER **/
                item {
                    Spacer(Modifier.height(12.dp))
                    var expanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedDepartment,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            departments.forEach {
                                DropdownMenuItem(
                                    text = { Text(it) },
                                    onClick = {
                                        selectedDepartment = it
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                /** STATS **/
                item {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatMiniCard(
                            value = filteredEmployees.size.toString(),
                            label = "Total",
                            color = AccentBlue
                        )
                        StatMiniCard(
                            value = activeEmployees.toString(),
                            label = "Active",
                            color = GreenPrimary
                        )
                        StatMiniCard(
                            value = String.format("%.1f", avgRating),
                            label = "Rating",
                            color = AccentYellow,
                            icon = Icons.Default.Star
                        )
                    }
                }

                /** LIST HEADER **/
                item {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "All Employees (${filteredEmployees.size})",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                /** EMPTY STATE **/
                if (filteredEmployees.isEmpty()) {
                    item {
                        EmptyStateScreen(
                            icon = Icons.Default.People,
                            title = "No Employees Found",
                            message = "Try adjusting your search or filters."
                        )
                    }
                } else {
                    items(filteredEmployees, key = { it.id }) { employee ->
                        val empTasks = allTasks.filter { it.employeeId == employee.id }
                        val empReviews = allReviews.filter { it.employeeId == employee.id }
                        val rating =
                            if (empReviews.isNotEmpty())
                                empReviews.map { it.overallRating }.average().toFloat()
                            else 0f

                        Spacer(Modifier.height(12.dp))
                        EmployeeCard(
                            employee = employee,
                            taskCount = empTasks.size,
                            rating = rating,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            onClick = { selectedEmployee = employee }
                        )
                    }
                }

                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }

    if (showAddEmployeeDialog) {
        AddEmployeeDialog(
            onDismiss = { showAddEmployeeDialog = false },
            onEmployeeAdded = { showAddEmployeeDialog = false }
        )
    }
}

/** SMALL STAT CARD **/
@Composable
private fun StatMiniCard(
    value: String,
    label: String,
    color: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )

                icon?.let {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}