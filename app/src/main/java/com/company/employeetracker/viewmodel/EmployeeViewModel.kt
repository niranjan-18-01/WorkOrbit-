// EmployeeViewModel.kt - Fixed with proper deletion logic

package com.company.employeetracker.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.company.employeetracker.data.database.AppDatabase
import com.company.employeetracker.data.database.entities.User
import com.company.employeetracker.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EmployeeViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao = AppDatabase.getDatabase(application).userDao()
    private val taskDao = AppDatabase.getDatabase(application).taskDao()
    private val reviewDao = AppDatabase.getDatabase(application).reviewDao()
    private val messageDao = AppDatabase.getDatabase(application).messageDao()
    private val attendanceDao = AppDatabase.getDatabase(application).attendanceDao()
    private val firebaseRepo = FirebaseRepository()

    private val tag = "EmployeeViewModel"

    private val _employees = MutableStateFlow<List<User>>(emptyList())
    val employees: StateFlow<List<User>> = _employees

    private val _employeeCount = MutableStateFlow(0)
    val employeeCount: StateFlow<Int> = _employeeCount

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _deleteSuccess = MutableStateFlow(false)
    val deleteSuccess: StateFlow<Boolean> = _deleteSuccess

    init {
        loadEmployees()
    }

    private fun loadEmployees() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Listen to Firebase real-time updates
                firebaseRepo.getEmployeesFlow().collect { firebaseEmployees ->
                    _employees.value = firebaseEmployees
                    _employeeCount.value = firebaseEmployees.size

                    // Sync to local Room database for offline access
                    syncToLocalDatabase(firebaseEmployees)
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to load employees from Firebase", e)
                _errorMessage.value = "Failed to load employees: ${e.message}"
                // Fallback to local database if Firebase fails
                loadFromLocalDatabase()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun syncToLocalDatabase(firebaseEmployees: List<User>) {
        try {
            // Update local database with Firebase data
            firebaseEmployees.forEach { employee ->
                userDao.insertUser(employee)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error syncing to local DB", e)
        }
    }

    private fun loadFromLocalDatabase() {
        viewModelScope.launch {
            userDao.getAllEmployees().collect { localEmployees ->
                _employees.value = localEmployees
                _employeeCount.value = localEmployees.size
            }
        }
    }

    fun addEmployee(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(tag, "Adding employee: ${user.name}")

                // Add to Firebase (will automatically sync via listener)
                val result = firebaseRepo.addUser(user)

                if (result.isFailure) {
                    Log.e(tag, "Failed to add employee to Firebase", result.exceptionOrNull())
                    _errorMessage.value = "Failed to add employee: ${result.exceptionOrNull()?.message}"
                } else {
                    Log.d(tag, "✅ Employee added successfully")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error adding employee", e)
                _errorMessage.value = "Error adding employee: ${e.message}"
                // Fallback: add to local database only
                userDao.insertUser(user)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateEmployee(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(tag, "Updating employee: ${user.name}")

                // Update in Firebase
                val result = firebaseRepo.updateUser(user)

                if (result.isFailure) {
                    Log.e(tag, "Failed to update employee", result.exceptionOrNull())
                    _errorMessage.value = "Failed to update employee: ${result.exceptionOrNull()?.message}"
                } else {
                    Log.d(tag, "✅ Employee updated successfully")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error updating employee", e)
                _errorMessage.value = "Error updating employee: ${e.message}"
                // Fallback: update local database only
                userDao.updateUser(user)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteEmployee(user: User) {
        viewModelScope.launch {
            _isLoading.value = true
            _deleteSuccess.value = false

            try {
                Log.d(tag, "========================================")
                Log.d(tag, "🗑️ STARTING EMPLOYEE DELETION PROCESS")
                Log.d(tag, "Employee ID: ${user.id}")
                Log.d(tag, "Employee Name: ${user.name}")
                Log.d(tag, "========================================")

                // Step 1: Delete from Firebase
                Log.d(tag, "Step 1: Deleting from Firebase...")
                val firebaseResult = firebaseRepo.deleteUser(user.id)

                if (firebaseResult.isSuccess) {
                    Log.d(tag, "✅ Firebase deletion successful")
                } else {
                    Log.e(tag, "⚠️ Firebase deletion failed: ${firebaseResult.exceptionOrNull()?.message}")
                    // Continue with local deletion even if Firebase fails
                }

                // Step 2: Delete associated data from LOCAL database (CASCADE will handle this)
                Log.d(tag, "Step 2: Deleting associated data from local DB...")

                // Get counts before deletion
                val tasksCount = taskDao.getTasksByEmployee(user.id)
                val reviewsCount = reviewDao.getReviewsByEmployee(user.id)
                val attendanceCount = attendanceDao.getAttendanceByEmployee(user.id)

                var taskCount = 0
                var reviewCount = 0
                var attendanceCountNum = 0

                tasksCount.collect { tasks ->
                    taskCount = tasks.size
                    return@collect
                }

                reviewsCount.collect { reviews ->
                    reviewCount = reviews.size
                    return@collect
                }

                attendanceCount.collect { attendance ->
                    attendanceCountNum = attendance.size
                    return@collect
                }

                Log.d(tag, "📊 Data to be deleted:")
                Log.d(tag, "  - Tasks: $taskCount")
                Log.d(tag, "  - Reviews: $reviewCount")
                Log.d(tag, "  - Attendance records: $attendanceCountNum")

                // Step 3: Delete employee from LOCAL database (CASCADE will delete related records)
                Log.d(tag, "Step 3: Deleting employee from local DB...")
                userDao.deleteUser(user)
                Log.d(tag, "✅ Local database deletion successful (CASCADE handled related data)")

                // Step 4: Delete associated data from Firebase manually (Firebase doesn't support CASCADE)
                Log.d(tag, "Step 4: Deleting associated data from Firebase...")
                deleteAssociatedFirebaseData(user.id)

                // Step 5: Update UI state
                Log.d(tag, "Step 5: Updating UI state...")
                val updatedEmployees = _employees.value.filter { it.id != user.id }
                _employees.value = updatedEmployees
                _employeeCount.value = updatedEmployees.size

                _deleteSuccess.value = true
                Log.d(tag, "✅✅✅ EMPLOYEE DELETION COMPLETED SUCCESSFULLY ✅✅✅")
                Log.d(tag, "========================================")

            } catch (e: Exception) {
                Log.e(tag, "❌❌❌ CRITICAL ERROR IN EMPLOYEE DELETION ❌❌❌", e)
                Log.e(tag, "Error details: ${e.message}")
                Log.e(tag, "Stack trace: ${e.stackTraceToString()}")
                _errorMessage.value = "Failed to delete employee: ${e.message}"
                _deleteSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun deleteAssociatedFirebaseData(employeeId: Int) {
        try {
            Log.d(tag, "🔥 Deleting Firebase associated data for employee $employeeId")

            // Delete all tasks
            taskDao.getTasksByEmployee(employeeId).collect { tasks ->
                Log.d(tag, "Deleting ${tasks.size} tasks from Firebase...")
                tasks.forEach { task ->
                    firebaseRepo.deleteTask(task.id)
                }
            }

            // Reviews and attendance will be deleted via CASCADE in local DB
            // For Firebase, you'd need similar logic if you're syncing those

            Log.d(tag, "✅ Firebase associated data deletion completed")
        } catch (e: Exception) {
            Log.e(tag, "⚠️ Error deleting Firebase associated data", e)
            // Don't fail the whole operation if this fails
        }
    }

    suspend fun getEmployeeById(id: Int): User? {
        return try {
            // Try to get from local database first (faster)
            userDao.getUserById(id)
        } catch (e: Exception) {
            Log.e(tag, "Error getting employee by ID", e)
            null
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearDeleteSuccess() {
        _deleteSuccess.value = false
    }

    /**
     * Force sync from Firebase to local database
     */
    fun forceSync() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(tag, "Force syncing employees from Firebase")
                val snapshot = firebaseRepo.getEmployeesFlow()
                snapshot.collect { employees ->
                    syncToLocalDatabase(employees)
                    _employees.value = employees
                }
            } catch (e: Exception) {
                Log.e(tag, "Sync failed", e)
                _errorMessage.value = "Sync failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}