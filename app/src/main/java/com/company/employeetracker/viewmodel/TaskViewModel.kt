package com.company.employeetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.company.employeetracker.data.database.AppDatabase
import com.company.employeetracker.data.database.entities.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val taskDao = AppDatabase.getDatabase(application).taskDao()

    private val _allTasks = MutableStateFlow<List<Task>>(emptyList())
    val allTasks: StateFlow<List<Task>> = _allTasks

    private val _employeeTasks = MutableStateFlow<List<Task>>(emptyList())
    val employeeTasks: StateFlow<List<Task>> = _employeeTasks

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount

    private val _activeCount = MutableStateFlow(0)
    val activeCount: StateFlow<Int> = _activeCount

    private val _completedCount = MutableStateFlow(0)
    val completedCount: StateFlow<Int> = _completedCount

    // Store current employee ID for reload operations
    private var currentEmployeeId: Int? = null

    init {
        loadAllTasks()
    }

    private fun loadAllTasks() {
        viewModelScope.launch {
            taskDao.getAllTasks().collect { tasks ->
                _allTasks.value = tasks
                updateTaskCounts()
            }
        }
    }

    fun loadTasksForEmployee(employeeId: Int) {
        currentEmployeeId = employeeId
        viewModelScope.launch {
            taskDao.getTasksByEmployee(employeeId).collect { tasks ->
                _employeeTasks.value = tasks
                updateEmployeeTaskCounts(employeeId)
            }
        }
    }

    private fun updateTaskCounts() {
        viewModelScope.launch {
            _pendingCount.value = taskDao.getTaskCountByStatus("Pending")
            _activeCount.value = taskDao.getTaskCountByStatus("Active")
            _completedCount.value = taskDao.getTaskCountByStatus("Done")
        }
    }

    private fun updateEmployeeTaskCounts(employeeId: Int) {
        viewModelScope.launch {
            _pendingCount.value = taskDao.getEmployeeTaskCountByStatus(employeeId, "Pending")
            _activeCount.value = taskDao.getEmployeeTaskCountByStatus(employeeId, "Active")
            _completedCount.value = taskDao.getEmployeeTaskCountByStatus(employeeId, "Done")
        }
    }

    fun updateTaskStatus(taskId: Int, newStatus: String) {
        viewModelScope.launch {
            // Get the task first
            val task = taskDao.getTaskById(taskId)
            task?.let {
                // Update the task with new status
                val updatedTask = it.copy(status = newStatus)
                taskDao.updateTask(updatedTask)

                // Reload tasks if we have a current employee
                currentEmployeeId?.let { empId ->
                    loadTasksForEmployee(empId)
                }
            }
        }
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            taskDao.insertTask(task)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskDao.updateTask(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
        }
    }
}