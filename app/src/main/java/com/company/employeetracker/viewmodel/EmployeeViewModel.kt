package com.company.employeetracker.viewmodel

import android.app.Application
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

    private val _employees = MutableStateFlow<List<User>>(emptyList())
    val employees: StateFlow<List<User>> = _employees

    private val _employeeCount = MutableStateFlow(0)
    val employeeCount: StateFlow<Int> = _employeeCount

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

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
            android.util.Log.e("EmployeeViewModel", "Error syncing to local DB", e)
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
                // Add to Firebase (will automatically sync via listener)
                val result = firebaseRepo.addUser(user)
                if (result.isFailure) {
                    _errorMessage.value = "Failed to add employee: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
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
                // Update in Firebase
                val result = firebaseRepo.updateUser(user)
                if (result.isFailure) {
                    _errorMessage.value = "Failed to update employee: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
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
            try {
                // Delete from Firebase
                val result = firebaseRepo.deleteUser(user.id)
                if (result.isFailure) {
                    _errorMessage.value = "Failed to delete employee: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting employee: ${e.message}"
                // Fallback: delete from local database only
                userDao.deleteUser(user)
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun getEmployeeById(id: Int): User? {
        return try {
            // Try to get from local database first (faster)
            userDao.getUserById(id)
        } catch (e: Exception) {
            null
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Force sync from Firebase to local database
     */
    fun forceSync() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = firebaseRepo.getEmployeesFlow()
                snapshot.collect { employees ->
                    syncToLocalDatabase(employees)
                    _employees.value = employees
                }
            } catch (e: Exception) {
                _errorMessage.value = "Sync failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}