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
                    Log.d(tag, "📥 Loaded ${tasks.size} tasks from Firebase")
                    _allTasks.value = tasks
                    updateTaskCounts(tasks)

                    // Sync to local DB
                    syncTasksToLocal(tasks)
                }
            } catch (e: Exception) {
                Log.e(tag, "⚠️ Error loading from Firebase, using local DB", e)
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
                    Log.d(tag, "📥 Loaded ${tasks.size} tasks for employee $employeeId")
                    _employeeTasks.value = tasks
                    updateTaskCounts(tasks)

                    // Sync to local DB
                    syncTasksToLocal(tasks)
                }
            } catch (e: Exception) {
                Log.e(tag, "⚠️ Error loading employee tasks, using local DB", e)
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
            tasks.forEach { task ->
                taskDao.insertTask(task)
            }
            Log.d(tag, "💾 Synced ${tasks.size} tasks to local DB")
        } catch (e: Exception) {
            Log.e(tag, "❌ Error syncing to local DB", e)
        }
    }

    private fun updateTaskCounts(tasks: List<Task>) {
        _pendingCount.value = tasks.count { it.status == "Pending" }
        _activeCount.value = tasks.count { it.status == "Active" }
        _completedCount.value = tasks.count { it.status == "Done" }

        Log.d(tag, "📊 Updated counts - Pending: ${_pendingCount.value}, Active: ${_activeCount.value}, Done: ${_completedCount.value}")
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(tag, "➕ Adding new task: ${task.title}")
                val result = firebaseRepo.addTask(task)

                if (result.isSuccess) {
                    Log.d(tag, "✅ Task added successfully to Firebase")
                    // Also add to local DB immediately
                    taskDao.insertTask(task)
                } else {
                    Log.e(tag, "❌ Failed to add task to Firebase: ${result.exceptionOrNull()?.message}")
                    taskDao.insertTask(task)
                }
            } catch (e: Exception) {
                Log.e(tag, "❌ Error adding task", e)
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
                Log.d(tag, "🔄 Updating task ${task.id}: ${task.title} to status ${task.status}")

                // CRITICAL FIX: Update both Firebase AND local DB with the SAME task object
                val result = firebaseRepo.updateTask(task)

                if (result.isSuccess) {
                    Log.d(tag, "✅ Task ${task.id} updated in Firebase")

                    // Update local DB with exact same task
                    taskDao.updateTask(task)
                    Log.d(tag, "✅ Task ${task.id} updated in local DB")

                    // Force UI refresh to prevent duplication
                    refreshTaskList(task.employeeId)
                } else {
                    Log.e(tag, "❌ Failed to update task in Firebase: ${result.exceptionOrNull()?.message}")
                    taskDao.updateTask(task)
                }
            } catch (e: Exception) {
                Log.e(tag, "❌ Error updating task", e)
                try {
                    taskDao.updateTask(task)
                    Log.d(tag, "✅ Task updated in local DB only")
                } catch (localError: Exception) {
                    Log.e(tag, "❌ Local update also failed", localError)
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
                Log.d(tag, "=== 🔄 Starting status update for task $taskId to $newStatus ===")

                // CRITICAL FIX: Get task from Firebase first to ensure we have the latest version
                val currentTask = _employeeTasks.value.find { it.id == taskId }
                    ?: _allTasks.value.find { it.id == taskId }
                    ?: taskDao.getTaskById(taskId)

                if (currentTask == null) {
                    Log.e(tag, "❌ Task $taskId not found!")
                    return@launch
                }

                Log.d(tag, "📝 Found task: ${currentTask.title}, current status: ${currentTask.status}")

                // Create updated task with new status
                val updatedTask = currentTask.copy(status = newStatus)

                Log.d(tag, "🔄 Updating task $taskId from '${currentTask.status}' to '$newStatus'")

                // Update using the main update function
                updateTask(updatedTask)

                Log.d(tag, "=== ✅ Status update completed for task $taskId ===")
            } catch (e: Exception) {
                Log.e(tag, "❌ Critical error in updateTaskStatus", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun refreshTaskList(employeeId: Int) {
        try {
            // Force refresh from Firebase to get the latest state
            Log.d(tag, "🔄 Force refreshing task list for employee $employeeId")

            // Small delay to ensure Firebase has propagated the update
            kotlinx.coroutines.delay(500)

            // The flow will automatically update the UI
            Log.d(tag, "✅ Task list refreshed")
        } catch (e: Exception) {
            Log.e(tag, "❌ Error refreshing task list", e)
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(tag, "🗑️ Deleting task $taskId")

                // Delete from Firebase
                val result = firebaseRepo.deleteTask(taskId)

                if (result.isSuccess) {
                    Log.d(tag, "✅ Task deleted from Firebase")
                } else {
                    Log.e(tag, "❌ Failed to delete from Firebase: ${result.exceptionOrNull()?.message}")
                }

                // Also delete from local DB
                val task = taskDao.getTaskById(taskId)
                task?.let {
                    taskDao.deleteTask(it)
                    Log.d(tag, "✅ Task deleted from local DB")
                }
            } catch (e: Exception) {
                Log.e(tag, "❌ Error deleting task", e)
                try {
                    val task = taskDao.getTaskById(taskId)
                    task?.let { taskDao.deleteTask(it) }
                } catch (localError: Exception) {
                    Log.e(tag, "❌ Local delete also failed", localError)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun forceRefresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(tag, "🔄 Force refreshing all tasks from Firebase")
                loadAllTasks()
            } catch (e: Exception) {
                Log.e(tag, "❌ Error force refreshing", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun clearLocalTasks() {
        try {
            val allLocalTasks = taskDao.getAllTasks()
            allLocalTasks.collect { tasks ->
                tasks.forEach { taskDao.deleteTask(it) }
            }
            Log.d(tag, "🧹 Cleared all local tasks")
        } catch (e: Exception) {
            Log.e(tag, "❌ Error clearing local tasks", e)
        }
    }
}