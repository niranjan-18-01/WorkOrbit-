package com.company.employeetracker.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.company.employeetracker.data.database.AppDatabase
import com.company.employeetracker.data.database.entities.Task
import com.company.employeetracker.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val taskDao = AppDatabase.getDatabase(application).taskDao()
    private val firebaseRepo = FirebaseRepository()

    private val tag = "TaskViewModel"

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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadAllTasks()
    }

    private fun loadAllTasks() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                firebaseRepo.getAllTasksFlow().collect { tasks ->
                    Log.d(tag, "Loaded ${tasks.size} tasks from Firebase")
                    _allTasks.value = tasks
                    updateTaskCounts(tasks)

                    // Sync to local DB (replace all to avoid duplicates)
                    syncTasksToLocal(tasks)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading from Firebase, using local DB", e)
                // Fallback to local database
                taskDao.getAllTasks().collect { tasks ->
                    _allTasks.value = tasks
                    updateTaskCounts(tasks)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadTasksForEmployee(employeeId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                firebaseRepo.getTasksByEmployeeFlow(employeeId).collect { tasks ->
                    Log.d(tag, "Loaded ${tasks.size} tasks for employee $employeeId")
                    _employeeTasks.value = tasks
                    updateTaskCounts(tasks)

                    // Sync to local DB
                    syncTasksToLocal(tasks)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading employee tasks, using local DB", e)
                // Fallback to local database
                taskDao.getTasksByEmployee(employeeId).collect { tasks ->
                    _employeeTasks.value = tasks
                    updateTaskCounts(tasks)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun syncTasksToLocal(tasks: List<Task>) {
        try {
            // Use insertTask with REPLACE strategy to avoid duplicates
            tasks.forEach { task ->
                taskDao.insertTask(task)
            }
            Log.d(tag, "Synced ${tasks.size} tasks to local DB")
        } catch (e: Exception) {
            Log.e(tag, "Error syncing to local DB", e)
        }
    }

    private fun updateTaskCounts(tasks: List<Task>) {
        _pendingCount.value = tasks.count { it.status == "Pending" }
        _activeCount.value = tasks.count { it.status == "Active" }
        _completedCount.value = tasks.count { it.status == "Done" }

        Log.d(tag, "Updated counts - Pending: ${_pendingCount.value}, Active: ${_activeCount.value}, Done: ${_completedCount.value}")
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(tag, "Adding new task: ${task.title}")
                val result = firebaseRepo.addTask(task)

                if (result.isSuccess) {
                    Log.d(tag, "Task added successfully to Firebase")
                    // Firebase listener will automatically update the UI
                } else {
                    Log.e(tag, "Failed to add task to Firebase: ${result.exceptionOrNull()?.message}")
                    // Fallback: add to local database only
                    taskDao.insertTask(task)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error adding task", e)
                // Fallback to local database
                taskDao.insertTask(task)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(tag, "Updating task ${task.id}: ${task.title} with status ${task.status}")

                // CRITICAL: Update in Firebase (this will trigger flow and update UI)
                val result = firebaseRepo.updateTask(task)

                if (result.isSuccess) {
                    Log.d(tag, "✅ Task ${task.id} updated successfully in Firebase")

                    // Also update local DB immediately to ensure consistency
                    taskDao.updateTask(task)
                    Log.d(tag, "✅ Task ${task.id} updated in local DB")
                } else {
                    Log.e(tag, "❌ Failed to update task in Firebase: ${result.exceptionOrNull()?.message}")
                    // Fallback: update local database only
                    taskDao.updateTask(task)
                }
            } catch (e: Exception) {
                Log.e(tag, "❌ Error updating task", e)
                // Fallback: update local database only
                try {
                    taskDao.updateTask(task)
                    Log.d(tag, "Updated task in local DB only")
                } catch (localError: Exception) {
                    Log.e(tag, "Local update also failed", localError)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateTaskStatus(taskId: Int, newStatus: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(tag, "=== Starting status update for task $taskId to $newStatus ===")

                // Get the task from current state first (faster)
                val existingTask = _allTasks.value.find { it.id == taskId }
                    ?: _employeeTasks.value.find { it.id == taskId }

                if (existingTask == null) {
                    // Fallback: try to get from local DB
                    val dbTask = taskDao.getTaskById(taskId)
                    if (dbTask == null) {
                        Log.e(tag, "❌ Task $taskId not found in any source!")
                        return@launch
                    }

                    // Update using DB task
                    val updatedTask = dbTask.copy(status = newStatus)
                    Log.d(tag, "Found task in DB: ${dbTask.title}, updating status to $newStatus")
                    updateTask(updatedTask)
                } else {
                    // Update using existing task from state
                    val updatedTask = existingTask.copy(status = newStatus)
                    Log.d(tag, "Found task in state: ${existingTask.title}, updating status to $newStatus")
                    updateTask(updatedTask)
                }

                Log.d(tag, "=== Status update completed for task $taskId ===")
            } catch (e: Exception) {
                Log.e(tag, "❌ Critical error in updateTaskStatus", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(tag, "Deleting task $taskId")

                // Delete from Firebase
                val result = firebaseRepo.deleteTask(taskId)

                if (result.isSuccess) {
                    Log.d(tag, "Task deleted from Firebase successfully")
                } else {
                    Log.e(tag, "Failed to delete from Firebase: ${result.exceptionOrNull()?.message}")
                }

                // Also delete from local DB
                val task = taskDao.getTaskById(taskId)
                task?.let {
                    taskDao.deleteTask(it)
                    Log.d(tag, "Task deleted from local DB")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error deleting task", e)
                // Fallback: delete from local database only
                try {
                    val task = taskDao.getTaskById(taskId)
                    task?.let { taskDao.deleteTask(it) }
                } catch (localError: Exception) {
                    Log.e(tag, "Local delete also failed", localError)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Force refresh from Firebase
     */
    fun forceRefresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(tag, "Force refreshing tasks from Firebase")
                loadAllTasks()
            } catch (e: Exception) {
                Log.e(tag, "Error force refreshing", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear all local tasks (useful for debugging)
     */
    suspend fun clearLocalTasks() {
        try {
            val allLocalTasks = taskDao.getAllTasks()
            allLocalTasks.collect { tasks ->
                tasks.forEach { taskDao.deleteTask(it) }
            }
            Log.d(tag, "Cleared all local tasks")
        } catch (e: Exception) {
            Log.e(tag, "Error clearing local tasks", e)
        }
    }
}