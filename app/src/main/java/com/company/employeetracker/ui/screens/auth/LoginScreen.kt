package com.company.employeetracker.ui.screens.auth

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.employeetracker.ui.theme.GreenPrimary
import com.company.employeetracker.ui.theme.GreenDark
import com.company.employeetracker.viewmodel.AuthViewModel
import androidx.compose.material.icons.automirrored.filled.ArrowForward

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (isAdmin: Boolean) -> Unit,
    viewModel: AuthViewModel = viewModel(),
    onForgotPasswordClick: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)

    var email by remember { mutableStateOf(sharedPreferences.getString("saved_email", "") ?: "") }
    var password by remember { mutableStateOf(sharedPreferences.getString("saved_password", "") ?: "") }
    var rememberMe by remember { mutableStateOf(sharedPreferences.getBoolean("remember_me", false)) }
    var showPassword by remember { mutableStateOf(false) }

    val currentUser by viewModel.currentUser.collectAsState()
    val loginError by viewModel.loginError.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            // Save credentials if Remember Me is checked
            if (rememberMe) {
                sharedPreferences.edit().apply {
                    putString("saved_email", email)
                    putString("saved_password", password)
                    putBoolean("remember_me", true)
                    apply()
                }
            } else {
                // Clear saved credentials if Remember Me is unchecked
                sharedPreferences.edit().clear().apply()
            }

            onLoginSuccess(user.role == "admin")
        }
    }

    fun performLogin() {
        viewModel.login(email, password) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(GreenPrimary, GreenDark)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Logo with animation effect
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Employee Tracker",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Sign in to manage your team",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Login Card - Made more compact
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Compact Title
                    Text(
                        text = "Welcome Back! 👋",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                    Text(
                        text = "Sign in to continue",
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Email Field - Compact
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter your email", fontSize = 14.sp) },
                        label = { Text("Email Address", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                tint = GreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedLabelColor = GreenPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Field - Compact
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter your password", fontSize = 14.sp) },
                        label = { Text("Password", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Password",
                                tint = GreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.Visibility
                                    else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password",
                                    tint = Color(0xFF757575),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedLabelColor = GreenPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Remember Me & Forgot Password - Compact
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = {
                                    rememberMe = it
                                    // Clear saved credentials if unchecked
                                    if (!it) {
                                        sharedPreferences.edit().clear().apply()
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = GreenPrimary
                                ),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Remember me",
                                fontSize = 12.sp,
                                color = Color(0xFF757575)
                            )
                        }

                        TextButton(
                            onClick = { onForgotPasswordClick() },
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text(
                                text = "Forgot password?",
                                color = GreenPrimary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (loginError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = loginError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Login Button - Compact
                    Button(
                        onClick = { performLogin() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(4.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Sign In",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Sign In",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Divider - Compact
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = "OR",
                            modifier = Modifier.padding(horizontal = 12.dp),
                            fontSize = 11.sp,
                            color = Color(0xFF9E9E9E)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Login Buttons - Modified to auto-fill and login
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Admin Quick Login
                        OutlinedButton(
                            onClick = {
                                email = "admin@company.com"
                                password = "admin123"
                                // Auto-login after filling
                                performLogin()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = GreenPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                GreenPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Admin",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Employee Quick Login
                        OutlinedButton(
                            onClick = {
                                email = "manoj@company.com"
                                password = "pass123"
                                // Auto-login after filling
                                performLogin()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = GreenPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                GreenPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Employee",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Employee",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer
            Text(
                text = "Powered by MindMatrix",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}