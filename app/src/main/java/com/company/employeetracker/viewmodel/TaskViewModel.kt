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

    // Track tasks being deleted to prevent re-sync
    private val deletingTaskIds = mutableSetOf<Int>()
    // Track tasks being updated to prevent conflicts
    private val updatingTaskIds = mutableSetOf<Int>()

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
                firebaseRepo.getAllTasksFlow()
                    .distinctUntilChanged()
                    .collect { tasks ->
                        Log.d(tag, "📥 Loaded ${tasks.size} tasks from Firebase")

                        // Remove duplicates and filter out tasks being deleted
                        val uniqueTasks = tasks
                            .distinctBy { it.id }
                            .filter { it.id !in deletingTaskIds }

                        Log.d(tag, "📊 Unique tasks after filtering: ${uniqueTasks.size}")

                        _allTasks.value = uniqueTasks
                        updateTaskCounts(uniqueTasks)

                        // Sync to local DB
                        syncTasksToLocal(uniqueTasks)
                    }
            } catch (e: Exception) {
                Log.e(tag, "⚠️ Error loading from Firebase, using local DB", e)
                taskDao.getAllTasks()
                    .distinctUntilChanged()
                    .collect { tasks ->
                        val uniqueTasks = tasks
                            .distinctBy { it.id }
                            .filter { it.id !in deletingTaskIds }
                        _allTasks.value = uniqueTasks
                        updateTaskCounts(uniqueTasks)
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
                firebaseRepo.getTasksByEmployeeFlow(employeeId)
                    .distinctUntilChanged()
                    .collect { tasks ->
                        Log.d(tag, "📥 Loaded ${tasks.size} tasks for employee $employeeId")

                        // Remove duplicates and filter out tasks being deleted
                        val uniqueTasks = tasks
                            .distinctBy { it.id }
                            .filter { it.id !in deletingTaskIds }

                        Log.d(tag, "📊 Unique tasks after filtering: ${uniqueTasks.size}")

                        _employeeTasks.value = uniqueTasks
                        updateTaskCounts(uniqueTasks)

                        // Sync to local DB
                        syncTasksToLocal(uniqueTasks)
                    }
            } catch (e: Exception) {
                Log.e(tag, "⚠️ Error loading employee tasks, using local DB", e)
                taskDao.getTasksByEmployee(employeeId)
                    .distinctUntilChanged()
                    .collect { tasks ->
                        val uniqueTasks = tasks
                            .distinctBy { it.id }
                            .filter { it.id !in deletingTaskIds }
                        _employeeTasks.value = uniqueTasks
                        updateTaskCounts(uniqueTasks)
                    }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun syncTasksToLocal(tasks: List<Task>) {
        try {
            // Get all existing task IDs in the database for this set
            val taskIds = tasks.map { it.id }.toSet()

            // For each task, update or insert
            tasks.forEach { task ->
                if (task.id !in deletingTaskIds && task.id !in updatingTaskIds) {
                    // Check if task exists
                    val existingTask = taskDao.getTaskById(task.id)

                    if (existingTask != null) {
                        // Only update if the task has actually changed
                        if (existingTask != task) {
                            taskDao.updateTask(task)
                            Log.d(tag, "🔄 Updated task ${task.id} in local DB")
                        }
                    } else {
                        // Insert new task
                        taskDao.insertTask(task)
                        Log.d(tag, "➕ Inserted task ${task.id} in local DB")
                    }
                }
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
                } else {
                    Log.e(tag, "❌ Failed to add task: ${result.exceptionOrNull()?.message}")
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

            // Mark task as being updated
            updatingTaskIds.add(task.id)

            try {
                Log.d(tag, "🔄 Updating task ${task.id}: ${task.title} to status ${task.status}")

                // Update Firebase first
                val result = firebaseRepo.updateTask(task)

                if (result.isSuccess) {
                    Log.d(tag, "✅ Task ${task.id} updated in Firebase")

                    // Update local DB immediately
                    taskDao.updateTask(task)
                    Log.d(tag, "✅ Task ${task.id} updated in local DB")

                    // Update the state immediately to reflect changes
                    if (_allTasks.value.any { it.id == task.id }) {
                        _allTasks.value = _allTasks.value.map {
                            if (it.id == task.id) task else it
                        }
                    }

                    if (_employeeTasks.value.any { it.id == task.id }) {
                        _employeeTasks.value = _employeeTasks.value.map {
                            if (it.id == task.id) task else it
                        }
                    }

                    updateTaskCounts(_employeeTasks.value.ifEmpty { _allTasks.value })

                    // Small delay to let Firebase propagate
                    kotlinx.coroutines.delay(300)
                } else {
                    Log.e(tag, "❌ Firebase update failed: ${result.exceptionOrNull()?.message}")
                    taskDao.updateTask(task)
                }
            } catch (e: Exception) {
                Log.e(tag, "❌ Error updating task", e)
                try {
                    taskDao.updateTask(task)
                } catch (localError: Exception) {
                    Log.e(tag, "❌ Local update failed", localError)
                }
            } finally {
                // Remove from updating set after delay
                viewModelScope.launch {
                    kotlinx.coroutines.delay(500)
                    updatingTaskIds.remove(task.id)
                }
                _isLoading.value = false
            }
        }
    }

    fun updateTaskStatus(taskId: Int, newStatus: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(tag, "=== 🔄 Starting status update for task $taskId to $newStatus ===")

                // Get task from current state (most up-to-date)
                val currentTask = _employeeTasks.value.find { it.id == taskId }
                    ?: _allTasks.value.find { it.id == taskId }

                if (currentTask == null) {
                    Log.e(tag, "❌ Task $taskId not found in state, checking DB...")
                    val dbTask = taskDao.getTaskById(taskId)
                    if (dbTask == null) {
                        Log.e(tag, "❌ Task $taskId not found anywhere!")
                        return@launch
                    }

                    val updatedTask = dbTask.copy(status = newStatus)
                    updateTask(updatedTask)
                } else {
                    Log.d(tag, "📝 Found task: ${currentTask.title}, current status: ${currentTask.status}")

                    // Create updated task with new status
                    val updatedTask = currentTask.copy(status = newStatus)

                    // Update the task
                    updateTask(updatedTask)
                }

                Log.d(tag, "=== ✅ Status update completed for task $taskId ===")
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

            // CRITICAL: Add to deleting set BEFORE deletion
            deletingTaskIds.add(taskId)

            try {
                Log.d(tag, "🗑️ Deleting task $taskId")

                // Delete from Firebase FIRST
                val result = firebaseRepo.deleteTask(taskId)

                if (result.isSuccess) {
                    Log.d(tag, "✅ Task deleted from Firebase")
                } else {
                    Log.e(tag, "❌ Firebase deletion failed: ${result.exceptionOrNull()?.message}")
                }

                // Delete from local DB
                taskDao.deleteTaskById(taskId)
                Log.d(tag, "✅ Task deleted from local DB")

                // Update state immediately by filtering out the deleted task
                _allTasks.value = _allTasks.value.filter { it.id != taskId }
                _employeeTasks.value = _employeeTasks.value.filter { it.id != taskId }

                // Update counts
                updateTaskCounts(_employeeTasks.value.ifEmpty { _allTasks.value })

                Log.d(tag, "✅ Task $taskId removed from state")

                // Give Firebase time to propagate the deletion
                kotlinx.coroutines.delay(1000)

            } catch (e: Exception) {
                Log.e(tag, "❌ Error deleting task", e)
                try {
                    taskDao.deleteTaskById(taskId)
                } catch (localError: Exception) {
                    Log.e(tag, "❌ Local delete failed", localError)
                }
            } finally {
                // Remove from deleting set after a longer delay to ensure Firebase has propagated
                viewModelScope.launch {
                    kotlinx.coroutines.delay(1500)
                    deletingTaskIds.remove(taskId)
                    Log.d(tag, "🔓 Task $taskId removed from deletion tracking")
                }
                _isLoading.value = false
            }
        }
    }

    fun forceRefresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(tag, "🔄 Force refreshing tasks")
                // Clear tracking sets
                updatingTaskIds.clear()
                // Don't clear deletingTaskIds during refresh
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
            taskDao.clearAllTasks()
            deletingTaskIds.clear()
            updatingTaskIds.clear()
            Log.d(tag, "🧹 Cleared all local tasks and tracking sets")
        } catch (e: Exception) {
            Log.e(tag, "❌ Error clearing local tasks", e)
        }
    }

    fun debugPrintAllTasks() {
        Log.d(tag, "========================================")
        Log.d(tag, "📊 ALL TASKS IN STATE")
        Log.d(tag, "========================================")
        _allTasks.value.forEachIndexed { index, task ->
            Log.d(tag, "${index + 1}. ID: ${task.id}, Title: ${task.title}, Status: ${task.status}")
        }
        Log.d(tag, "Total: ${_allTasks.value.size} tasks")
        Log.d(tag, "========================================")
    }

    fun debugPrintEmployeeTasks() {
        Log.d(tag, "========================================")
        Log.d(tag, "📊 EMPLOYEE TASKS IN STATE")
        Log.d(tag, "========================================")
        _employeeTasks.value.forEachIndexed { index, task ->
            Log.d(tag, "${index + 1}. ID: ${task.id}, Title: ${task.title}, Status: ${task.status}")
        }
        Log.d(tag, "Total: ${_employeeTasks.value.size} tasks")
        Log.d(tag, "========================================")
    }

    fun debugPrintDeletingTasks() {
        Log.d(tag, "========================================")
        Log.d(tag, "🗑️ TASKS BEING DELETED")
        Log.d(tag, "========================================")
        if (deletingTaskIds.isEmpty()) {
            Log.d(tag, "No tasks currently being deleted")
        } else {
            deletingTaskIds.forEach { id ->
                Log.d(tag, "Task ID: $id")
            }
        }
        Log.d(tag, "========================================")
    }

    suspend fun debugCheckFirebaseVsLocal(taskId: Int) {
        Log.d(tag, "========================================")
        Log.d(tag, "🔍 CHECKING TASK $taskId")
        Log.d(tag, "========================================")

        // Check local DB
        val localTask = taskDao.getTaskById(taskId)
        Log.d(tag, "Local DB: ${if (localTask != null) "EXISTS - ${localTask.title} (${localTask.status})" else "NOT FOUND"}")

        // Check Firebase
        val firebaseExists = firebaseRepo.taskExists(taskId)
        Log.d(tag, "Firebase: ${if (firebaseExists) "EXISTS" else "NOT FOUND"}")

        // Check state
        val inAllTasks = _allTasks.value.any { it.id == taskId }
        val inEmployeeTasks = _employeeTasks.value.any { it.id == taskId }
        Log.d(tag, "In _allTasks: $inAllTasks")
        Log.d(tag, "In _employeeTasks: $inEmployeeTasks")

        Log.d(tag, "========================================")
    }
}