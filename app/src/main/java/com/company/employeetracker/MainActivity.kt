package com.company.employeetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.company.employeetracker.ui.components.AdminBottomNavBar
import com.company.employeetracker.ui.components.EmployeeBottomNavBar
import com.company.employeetracker.ui.screens.admin.*
import com.company.employeetracker.ui.screens.auth.LoginScreen
import com.company.employeetracker.ui.screens.auth.ForgotPasswordScreen
import com.company.employeetracker.ui.screens.admin.AdminProfileScreen
import com.company.employeetracker.ui.screens.employee.EmployeeHomeScreen
import com.company.employeetracker.ui.screens.employee.EmployeeProfileScreen
import com.company.employeetracker.ui.screens.employee.EmployeeReviewsScreen
import com.company.employeetracker.ui.screens.employee.EmployeeTasksScreen
import com.company.employeetracker.ui.screens.employee.NotificationsScreen
import com.company.employeetracker.ui.screens.employee.ChatScreen
import com.company.employeetracker.ui.screens.employee.SelectEmployeeScreen
import com.company.employeetracker.ui.theme.EmployeeTrackerTheme
import com.company.employeetracker.viewmodel.AuthViewModel
import com.google.firebase.database.FirebaseDatabase
import android.util.Log


class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase (if not already initialized)
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception) {
            Log.e("MainActivity", "Firebase already initialized")
        }

        setContent {
            EmployeeTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EmployeeTrackerApp(authViewModel)
                }
            }
        }
    }
}

@Composable
fun EmployeeTrackerApp(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val currentUser by authViewModel.currentUser.collectAsState()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val isAdmin = currentUser?.role == "admin"

    // Show bottom bar only when logged in and not on login or forgot password screens
    val showBottomBar = currentUser != null && currentRoute != "login" && currentRoute != "forgot_password"

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                if (isAdmin) {
                    AdminBottomNavBar(
                        currentRoute = currentRoute ?: "admin_dashboard",
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                } else {
                    EmployeeBottomNavBar(
                        currentRoute = currentRoute ?: "home",
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = if (currentUser != null) {
                if (isAdmin) "admin_dashboard" else "home"
            } else "login",
            modifier = Modifier.padding(paddingValues)
        ) {
            // Login Screen
            composable("login") {
                LoginScreen(
                    onLoginSuccess = { isAdminUser ->
                        if (isAdminUser) {
                            navController.navigate("admin_dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    },
                    onForgotPasswordClick = { navController.navigate("forgot_password") },
                    viewModel = authViewModel
                )
            }

            // Forgot Password Screen
            composable("forgot_password") {
                ForgotPasswordScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // Employee Screens
            composable("home") {
                currentUser?.let { user ->
                    EmployeeHomeScreen(
                        currentUser = user,
                        onNavigateToSelectEmployee = {
                            navController.navigate("select_employee")
                        }
                    )
                }
            }


            composable("tasks") {
                currentUser?.let { user ->
                    EmployeeTasksScreen(
                        currentUser = user,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            composable("reviews") {
                currentUser?.let { user ->
                    EmployeeReviewsScreen(
                        currentUser = user,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            composable("profile") {
                currentUser?.let { user ->
                    EmployeeProfileScreen(
                        currentUser = user,
                        onLogout = {
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        authViewModel = authViewModel
                    )
                }
            }

            // Notifications Screen
            composable("notifications") {
                currentUser?.let { user ->
                    NotificationsScreen(
                        currentUser = user,
                        onBackClick = { navController.popBackStack() },
                        onMessageClick = { userId ->
                            navController.navigate("chat/$userId")
                        }
                    )
                }
            }

            // Chat Screen
            composable("chat/{userId}") { backStackEntry ->
                currentUser?.let { user ->
                    val otherUserId = backStackEntry.arguments?.getString("userId")?.toInt() ?: 0
                    ChatScreen(
                        currentUser = user,
                        otherUserId = otherUserId,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            // Select Employee Screen
            composable("select_employee") {
                currentUser?.let { user ->
                    SelectEmployeeScreen(
                        currentUser = user,
                        onBackClick = { navController.popBackStack() },
                        onEmployeeSelected = { userId ->
                            navController.navigate("chat/$userId") {
                                popUpTo("select_employee") { inclusive = true }
                            }
                        }
                    )
                }
            }

            // Admin Screens
            composable("admin_dashboard") {
                AdminDashboardScreen()
            }

            composable("employees") {
                AdminEmployeesScreen()
            }

            composable("admin_tasks") {
                AdminTasksScreen()
            }

            composable("analytics") {
                AdminAnalyticsScreen()
            }

            composable("admin_profile") {
                currentUser?.let { user ->
                    AdminProfileScreen(
                        currentUser = user,
                        onLogout = {
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }
}
