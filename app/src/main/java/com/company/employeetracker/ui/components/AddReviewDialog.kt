package com.company.employeetracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.company.employeetracker.data.database.entities.Review
import com.company.employeetracker.ui.theme.GreenPrimary
import com.company.employeetracker.viewmodel.EmployeeViewModel
import com.company.employeetracker.viewmodel.ReviewViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReviewDialog(
    onDismiss: () -> Unit,
    onReviewAdded: () -> Unit,
    employeeViewModel: EmployeeViewModel = viewModel(),
    reviewViewModel: ReviewViewModel = viewModel()
) {
    val employees by employeeViewModel.employees.collectAsState()

    var selectedEmployee by remember { mutableStateOf("Select Employee") }
    var selectedEmployeeId by remember { mutableStateOf(0) }
    var quality by remember { mutableStateOf(3f) }
    var communication by remember { mutableStateOf(3f) }
    var innovation by remember { mutableStateOf(3f) }
    var timeliness by remember { mutableStateOf(3f) }
    var attendance by remember { mutableStateOf(3f) }
    var remarks by remember { mutableStateOf("") }
    var employeeExpanded by remember { mutableStateOf(false) }

    // Validation states
    var employeeError by remember { mutableStateOf("") }
    var remarksError by remember { mutableStateOf("") }

    // Calculate overall rating
    val overallRating = (quality + communication + innovation + timeliness + attendance) / 5f

    // Validate fields
    fun validateFields(): Boolean {
        var isValid = true

        if (selectedEmployeeId == 0) {
            employeeError = "Please select an employee"
            isValid = false
        } else {
            employeeError = ""
        }

        if (remarks.isBlank()) {
            remarksError = "Review remarks are required"
            isValid = false
        } else if (remarks.length < 20) {
            remarksError = "Remarks must be at least 20 characters"
            isValid = false
        } else {
            remarksError = ""
        }

        return isValid
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Add Performance Review",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Rate employee on 5 metrics",
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Employee Selection
                ExposedDropdownMenuBox(
                    expanded = employeeExpanded,
                    onExpandedChange = { employeeExpanded = !employeeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedEmployee,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Employee *") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = employeeExpanded)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        isError = employeeError.isNotEmpty(),
                        supportingText = {
                            if (employeeError.isNotEmpty()) {
                                Text(
                                    text = employeeError,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = employeeExpanded,
                        onDismissRequest = { employeeExpanded = false }
                    ) {
                        employees.forEach { employee ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(employee.name, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${employee.designation} • ${employee.department}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF757575)
                                        )
                                    }
                                },
                                onClick = {
                                    selectedEmployee = employee.name
                                    selectedEmployeeId = employee.id
                                    employeeExpanded = false
                                    employeeError = ""
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Rating Sliders
                Text(
                    text = "Performance Metrics",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quality of Work
                RatingSlider(
                    label = "Quality of Work",
                    value = quality,
                    onValueChange = { quality = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Communication
                RatingSlider(
                    label = "Communication",
                    value = communication,
                    onValueChange = { communication = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Innovation
                RatingSlider(
                    label = "Innovation",
                    value = innovation,
                    onValueChange = { innovation = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Timeliness
                RatingSlider(
                    label = "Timeliness",
                    value = timeliness,
                    onValueChange = { timeliness = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Attendance
                RatingSlider(
                    label = "Attendance",
                    value = attendance,
                    onValueChange = { attendance = it }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Overall Rating Display
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = GreenPrimary.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Overall Rating",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%.1f", overallRating),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary
                            )
                            Text(
                                text = " / 5.0",
                                fontSize = 14.sp,
                                color = Color(0xFF757575)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Remarks Field
                OutlinedTextField(
                    value = remarks,
                    onValueChange = {
                        remarks = it
                        remarksError = ""
                    },
                    label = { Text("Review Remarks *") },
                    placeholder = { Text("Provide detailed feedback...") },
                    leadingIcon = {
                        Icon(Icons.Default.Comment, contentDescription = null)
                    },
                    isError = remarksError.isNotEmpty(),
                    supportingText = {
                        if (remarksError.isNotEmpty()) {
                            Text(
                                text = remarksError,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    minLines = 4,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "* Required fields",
                    fontSize = 12.sp,
                    color = Color(0xFF757575)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (validateFields()) {
                                val newReview = Review(
                                    employeeId = selectedEmployeeId,
                                    date = LocalDate.now().toString(),
                                    quality = quality,
                                    communication = communication,
                                    innovation = innovation,
                                    timeliness = timeliness,
                                    attendance = attendance,
                                    overallRating = overallRating,
                                    remarks = remarks,
                                    reviewedBy = "Admin User"
                                )
                                reviewViewModel.addReview(newReview)
                                onReviewAdded()
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Submit Review")
                    }
                }
            }
        }
    }
}

@Composable
fun RatingSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = GreenPrimary.copy(alpha = 0.1f)
            ) {
                Text(
                    text = String.format("%.1f", value),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = GreenPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..5f,
            steps = 9,
            colors = SliderDefaults.colors(
                thumbColor = GreenPrimary,
                activeTrackColor = GreenPrimary,
                inactiveTrackColor = Color(0xFFE0E0E0)
            )
        )
    }
}