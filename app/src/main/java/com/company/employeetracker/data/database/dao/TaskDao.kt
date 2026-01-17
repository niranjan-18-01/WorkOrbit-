package com.company.employeetracker.data.database.dao

import androidx.room.*
import com.company.employeetracker.data.database.entities.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // ---------------- BASIC CRUD ----------------

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

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Int)

    @Query("DELETE FROM tasks WHERE employeeId = :employeeId")
    suspend fun deleteTasksForEmployee(employeeId: Int)

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()

    // ---------------- STATUS COUNTS ----------------

    @Query("SELECT COUNT(*) FROM tasks WHERE status = :status")
    suspend fun getTaskCountByStatus(status: String): Int

    @Query("""
        SELECT COUNT(*) FROM tasks 
        WHERE employeeId = :employeeId AND status = :status
    """)
    suspend fun getEmployeeTaskCountByStatus(
        employeeId: Int,
        status: String
    ): Int

    // ---------------- DEADLINE BASED (IMPORTANT) ----------------

    // 🔥 Tasks sorted by nearest deadline (Admin dashboard)
    @Query("""
        SELECT * FROM tasks 
        ORDER BY deadlineTimestamp ASC
    """)
    fun getTasksSortedByDeadline(): Flow<List<Task>>

    // 🔥 Upcoming tasks (next X days)
    @Query("""
        SELECT * FROM tasks 
        WHERE deadlineTimestamp BETWEEN :now AND :future
        ORDER BY deadlineTimestamp ASC
    """)
    fun getUpcomingTasks(
        now: Long,
        future: Long
    ): Flow<List<Task>>

    // 🔥 Overdue tasks (missed deadline)
    @Query("""
        SELECT * FROM tasks 
        WHERE deadlineTimestamp < :now 
        AND status != 'Done'
        ORDER BY deadlineTimestamp ASC
    """)
    fun getOverdueTasks(now: Long): Flow<List<Task>>

    // 🔥 Employee-wise upcoming tasks
    @Query("""
        SELECT * FROM tasks 
        WHERE employeeId = :employeeId
        AND deadlineTimestamp >= :now
        ORDER BY deadlineTimestamp ASC
    """)
    fun getEmployeeUpcomingTasks(
        employeeId: Int,
        now: Long
    ): Flow<List<Task>>

    // 🔥 Auto-clean completed old tasks (optional)
    @Query("""
        DELETE FROM tasks 
        WHERE status = 'Done'
        AND deadlineTimestamp < :cutoff
    """)
    suspend fun deleteOldCompletedTasks(cutoff: Long)
}
