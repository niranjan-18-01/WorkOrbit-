package com.company.employeetracker.data.database.dao

import androidx.room.*
import com.company.employeetracker.data.database.entities.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE employeeId = :employeeId")
    fun getTasksByEmployee(employeeId: Int): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    // NEW: Delete task by ID
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Int)

    // NEW: Delete all tasks for an employee
    @Query("DELETE FROM tasks WHERE employeeId = :employeeId")
    suspend fun deleteTasksForEmployee(employeeId: Int)

    @Query("SELECT COUNT(*) FROM tasks WHERE status = :status")
    suspend fun getTaskCountByStatus(status: String): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE employeeId = :employeeId AND status = :status")
    suspend fun getEmployeeTaskCountByStatus(employeeId: Int, status: String): Int

    // NEW: Clear all tasks (for debugging)
    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()
}