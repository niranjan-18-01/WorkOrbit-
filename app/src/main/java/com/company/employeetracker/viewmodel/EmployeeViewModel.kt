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
                firebaseRepo.getEmployeesFlow().collect { firebaseEmployees ->
                    _employees.value = firebaseEmployees
                    _employeeCount.value = firebaseEmployees.size
                    syncToLocalDatabase(firebaseEmployees)
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to load employees from Firebase", e)
                _errorMessage.value = "Failed to load employees: ${e.message}"
                loadFromLocalDatabase()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun syncToLocalDatabase(firebaseEmployees: List<User>) {
        try {
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
                Log.d(tag, "🗑️ STARTING CASCADE DELETE")
                Log.d(tag, "Employee ID: ${user.id}")
                Log.d(tag, "Employee Name: ${user.name}")
                Log.d(tag, "========================================")

                // ✅ FIX: Use CASCADE DELETE from Firebase
                Log.d(tag, "Step 1: Deleting from Firebase (CASCADE)...")
                val firebaseResult = firebaseRepo.deleteEmployeeCascade(user.id)

                if (firebaseResult.isSuccess) {
                    Log.d(tag, "✅ Firebase CASCADE delete successful")
                } else {
                    Log.e(tag, "⚠️ Firebase deletion failed: ${firebaseResult.exceptionOrNull()?.message}")
                    throw Exception("Firebase deletion failed: ${firebaseResult.exceptionOrNull()?.message}")
                }

                // Step 2: Delete from LOCAL database (CASCADE will handle related records)
                Log.d(tag, "Step 2: Deleting from local DB (CASCADE)...")
                userDao.deleteUser(user)
                Log.d(tag, "✅ Local database CASCADE delete successful")

                // Step 3: Update UI state immediately
                Log.d(tag, "Step 3: Updating UI state...")
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

    suspend fun getEmployeeById(id: Int): User? {
        return try {
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