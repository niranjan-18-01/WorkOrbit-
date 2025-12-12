package com.company.employeetracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.company.employeetracker.data.database.AppDatabase
import com.company.employeetracker.data.database.entities.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.company.employeetracker.data.repository.FirebaseRepository


class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val taskDao = AppDatabase.getDatabase(application).taskDao()
    private val firebaseRepo = FirebaseRepository()

    private val _allTasks = MutableStateFlow<List<Task>>(emptyList())
    val allTasks: StateFlow<List<Task>> = _allTasks

    init {
        loadAllTasks()
    }

    private fun loadAllTasks() {
        viewModelScope.launch {
            firebaseRepo.getAllTasksFlow().collect { tasks ->
                _allTasks.value = tasks
                // Sync to local DB
                tasks.forEach { taskDao.insertTask(it) }
            }
        }
    }

    fun addTask(task: Task) {
        viewModelScope.launch {
            firebaseRepo.addTask(task)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            firebaseRepo.updateTask(task)
        }
    }

    fun updateTaskStatus(taskId: Int, newStatus: String) {
        viewModelScope.launch {
            val task = taskDao.getTaskById(taskId)
            task?.let {
                val updatedTask = it.copy(status = newStatus)
                firebaseRepo.updateTask(updatedTask)
            }
        }
    }
}