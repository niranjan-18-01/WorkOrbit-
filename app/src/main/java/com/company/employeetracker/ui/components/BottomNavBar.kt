package com.company.employeetracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.company.employeetracker.ui.theme.GreenPrimary

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Home")
    object Tasks : BottomNavItem("tasks", Icons.AutoMirrored.Filled.Assignment, "Tasks")
    object Reviews : BottomNavItem("reviews", Icons.Default.Star, "Reviews")
    object Profile : BottomNavItem("profile", Icons.Default.Person, "Profile")
}

sealed class AdminBottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Dashboard : AdminBottomNavItem("admin_dashboard", Icons.Default.Dashboard, "Dashboard")
    object Employees : AdminBottomNavItem("employees", Icons.Default.People, "Employees")
    object Tasks : AdminBottomNavItem("admin_tasks", Icons.AutoMirrored.Filled.Assignment, "Tasks")
    object Analytics : AdminBottomNavItem("analytics", Icons.AutoMirrored.Filled.TrendingUp, "Analytics")
    object Profile : AdminBottomNavItem("admin_profile", Icons.Default.Settings, "Profile")
}

@Composable
fun EmployeeBottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Tasks,
        BottomNavItem.Reviews,
        BottomNavItem.Profile
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GreenPrimary,
                    selectedTextColor = GreenPrimary,
                    indicatorColor = GreenPrimary.copy(alpha = 0.1f)
                )
            )
        }
    }
}

@Composable
fun AdminBottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        AdminBottomNavItem.Dashboard,
        AdminBottomNavItem.Employees,
        AdminBottomNavItem.Tasks,
        AdminBottomNavItem.Analytics,
        AdminBottomNavItem.Profile
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            )
        }
    }
}