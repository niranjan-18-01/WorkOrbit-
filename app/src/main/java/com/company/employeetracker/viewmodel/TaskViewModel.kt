package com.company.employeetracker.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.company.employeetracker.data.database.AppDatabase
import com.company.employeetracker.data.database.entities.Task
import com.company.employeetracker.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val taskDao = AppDatabase.getDatabase(application).taskDao()
    private val firebaseRepo = FirebaseRepository()
    private val tag = "TaskViewModel"

    private val deletingTaskIds = mutableSetOf<Int>()
    private val updatingTaskIds = mutableSetOf<Int>()

    private val _allTasks = MutableStateFlow<List<Task>>(emptyList())
    val allTasks: StateFlow<List<Task>> = _allTasks

    // 🔥 SORTED BY DEADLINE (URGENT FIRST)
    val tasksSortedByDeadline: StateFlow<List<Task>> =
        _allTasks
            .map { tasks -> tasks.sortedBy { it.deadlineTimestamp } }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

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
            try {
                firebaseRepo.getAllTasksFlow()
                    .distinctUntilChanged()
                    .collect { tasks ->
                        val unique = tasks
                            .distinctBy { it.id }
                            .filter { it.id !in deletingTaskIds }

                        _allTasks.value = unique
                        updateCounts(unique)
                    }
            } catch (e: Exception) {
                Log.e(tag, "Firebase failed, using local DB", e)
                taskDao.getAllTasks().collect {
                    _allTasks.value = it
                    updateCounts(it)
                }
            }
        }
    }

    private fun updateCounts(tasks: List<Task>) {
        _pendingCount.value = tasks.count { it.status == "Pending" }
        _activeCount.value = tasks.count { it.status == "Active" }
        _completedCount.value = tasks.count { it.status == "Done" }
    }

    fun addTask(task: Task) = viewModelScope.launch {
        firebaseRepo.addTask(task)
        taskDao.insertTask(task)
    }

    fun updateTask(task: Task) = viewModelScope.launch {
        updatingTaskIds.add(task.id)
        firebaseRepo.updateTask(task)
        taskDao.updateTask(task)
        updatingTaskIds.remove(task.id)
    }

    fun deleteTask(taskId: Int) = viewModelScope.launch {
        deletingTaskIds.add(taskId)
        firebaseRepo.deleteTask(taskId)
        taskDao.deleteTaskById(taskId)
        _allTasks.value = _allTasks.value.filterNot { it.id == taskId }
        deletingTaskIds.remove(taskId)
    }

    // 🔥 HELPER: Overdue check
    fun isTaskOverdue(task: Task): Boolean =
        task.deadlineTimestamp < System.currentTimeMillis() &&
                task.status != "Done"
}
