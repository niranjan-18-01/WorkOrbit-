package com.company.employeetracker.data.repository

import android.util.Log
import com.company.employeetracker.data.database.entities.*
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Repository for Real-time Data Synchronization
 * Handles CRUD operations with Firebase Realtime Database
 */
class FirebaseRepository {
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val TAG = "FirebaseRepository"

    // Reference paths
    private val usersRef = database.child("users")
    private val tasksRef = database.child("tasks")
    private val reviewsRef = database.child("reviews")
    private val messagesRef = database.child("messages")
    private val attendanceRef = database.child("attendance")

    // ==================== USER OPERATIONS ====================

    suspend fun addUser(user: User): Result<String> {
        return try {
            val userId = if (user.id == 0) usersRef.push().key!! else user.id.toString()
            val userMap = mapOf(
                "id" to userId,
                "email" to user.email,
                "password" to user.password,
                "name" to user.name,
                "role" to user.role,
                "designation" to user.designation,
                "department" to user.department,
                "joiningDate" to user.joiningDate,
                "contact" to user.contact,
                "profileImage" to user.profileImage
            )
            usersRef.child(userId).setValue(userMap).await()
            Result.success(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding user", e)
            Result.failure(e)
        }
    }

    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            val userMap = mapOf(
                "email" to user.email,
                "password" to user.password,
                "name" to user.name,
                "role" to user.role,
                "designation" to user.designation,
                "department" to user.department,
                "joiningDate" to user.joiningDate,
                "contact" to user.contact,
                "profileImage" to user.profileImage
            )
            usersRef.child(user.id.toString()).updateChildren(userMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user", e)
            Result.failure(e)
        }
    }

    suspend fun deleteUser(userId: Int): Result<Unit> {
        return try {
            usersRef.child(userId.toString()).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user", e)
            Result.failure(e)
        }
    }

    fun getAllUsersFlow(): Flow<List<User>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = mutableListOf<User>()
                snapshot.children.forEach { child ->
                    try {
                        val user = User(
                            id = child.child("id").value.toString().toIntOrNull() ?: 0,
                            email = child.child("email").value.toString(),
                            password = child.child("password").value.toString(),
                            name = child.child("name").value.toString(),
                            role = child.child("role").value.toString(),
                            designation = child.child("designation").value.toString(),
                            department = child.child("department").value.toString(),
                            joiningDate = child.child("joiningDate").value.toString(),
                            contact = child.child("contact").value.toString(),
                            profileImage = child.child("profileImage").value.toString()
                        )
                        users.add(user)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing user", e)
                    }
                }
                trySend(users)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Users listener cancelled", error.toException())
                close(error.toException())
            }
        }
        usersRef.addValueEventListener(listener)
        awaitClose { usersRef.removeEventListener(listener) }
    }

    fun getEmployeesFlow(): Flow<List<User>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val employees = mutableListOf<User>()
                snapshot.children.forEach { child ->
                    try {
                        val role = child.child("role").value.toString()
                        if (role == "employee") {
                            val user = User(
                                id = child.child("id").value.toString().toIntOrNull() ?: 0,
                                email = child.child("email").value.toString(),
                                password = child.child("password").value.toString(),
                                name = child.child("name").value.toString(),
                                role = role,
                                designation = child.child("designation").value.toString(),
                                department = child.child("department").value.toString(),
                                joiningDate = child.child("joiningDate").value.toString(),
                                contact = child.child("contact").value.toString(),
                                profileImage = child.child("profileImage").value.toString()
                            )
                            employees.add(user)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing employee", e)
                    }
                }
                trySend(employees)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Employees listener cancelled", error.toException())
                close(error.toException())
            }
        }
        usersRef.addValueEventListener(listener)
        awaitClose { usersRef.removeEventListener(listener) }
    }

    suspend fun loginUser(email: String, password: String): Result<User?> {
        return try {
            val snapshot = usersRef.orderByChild("email").equalTo(email).get().await()
            var user: User? = null
            snapshot.children.forEach { child ->
                val storedPassword = child.child("password").value.toString()
                if (storedPassword == password) {
                    user = User(
                        id = child.child("id").value.toString().toIntOrNull() ?: 0,
                        email = child.child("email").value.toString(),
                        password = storedPassword,
                        name = child.child("name").value.toString(),
                        role = child.child("role").value.toString(),
                        designation = child.child("designation").value.toString(),
                        department = child.child("department").value.toString(),
                        joiningDate = child.child("joiningDate").value.toString(),
                        contact = child.child("contact").value.toString(),
                        profileImage = child.child("profileImage").value.toString()
                    )
                }
            }
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging in", e)
            Result.failure(e)
        }
    }

    // ==================== TASK OPERATIONS ====================

    suspend fun addTask(task: Task): Result<String> {
        return try {
            val taskId = if (task.id == 0) tasksRef.push().key!! else task.id.toString()
            val taskMap = mapOf(
                "id" to taskId,
                "employeeId" to task.employeeId,
                "title" to task.title,
                "description" to task.description,
                "priority" to task.priority,
                "status" to task.status,
                "deadline" to task.deadline,
                "assignedDate" to task.assignedDate,
                "assignedBy" to task.assignedBy
            )
            tasksRef.child(taskId).setValue(taskMap).await()
            Result.success(taskId)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding task", e)
            Result.failure(e)
        }
    }

    suspend fun updateTask(task: Task): Result<Unit> {
        return try {
            val taskMap = mapOf(
                "employeeId" to task.employeeId,
                "title" to task.title,
                "description" to task.description,
                "priority" to task.priority,
                "status" to task.status,
                "deadline" to task.deadline,
                "assignedDate" to task.assignedDate,
                "assignedBy" to task.assignedBy
            )
            tasksRef.child(task.id.toString()).updateChildren(taskMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating task", e)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: Int): Result<Unit> {
        return try {
            tasksRef.child(taskId.toString()).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting task", e)
            Result.failure(e)
        }
    }

    fun getAllTasksFlow(): Flow<List<Task>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tasks = mutableListOf<Task>()
                snapshot.children.forEach { child ->
                    try {
                        val task = Task(
                            id = child.child("id").value.toString().toIntOrNull() ?: 0,
                            employeeId = child.child("employeeId").value.toString().toIntOrNull() ?: 0,
                            title = child.child("title").value.toString(),
                            description = child.child("description").value.toString(),
                            priority = child.child("priority").value.toString(),
                            status = child.child("status").value.toString(),
                            deadline = child.child("deadline").value.toString(),
                            assignedDate = child.child("assignedDate").value.toString(),
                            assignedBy = child.child("assignedBy").value.toString()
                        )
                        tasks.add(task)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing task", e)
                    }
                }
                trySend(tasks)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Tasks listener cancelled", error.toException())
                close(error.toException())
            }
        }
        tasksRef.addValueEventListener(listener)
        awaitClose { tasksRef.removeEventListener(listener) }
    }

    fun getTasksByEmployeeFlow(employeeId: Int): Flow<List<Task>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tasks = mutableListOf<Task>()
                snapshot.children.forEach { child ->
                    try {
                        val empId = child.child("employeeId").value.toString().toIntOrNull() ?: 0
                        if (empId == employeeId) {
                            val task = Task(
                                id = child.child("id").value.toString().toIntOrNull() ?: 0,
                                employeeId = empId,
                                title = child.child("title").value.toString(),
                                description = child.child("description").value.toString(),
                                priority = child.child("priority").value.toString(),
                                status = child.child("status").value.toString(),
                                deadline = child.child("deadline").value.toString(),
                                assignedDate = child.child("assignedDate").value.toString(),
                                assignedBy = child.child("assignedBy").value.toString()
                            )
                            tasks.add(task)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing employee task", e)
                    }
                }
                trySend(tasks)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Employee tasks listener cancelled", error.toException())
                close(error.toException())
            }
        }
        tasksRef.addValueEventListener(listener)
        awaitClose { tasksRef.removeEventListener(listener) }
    }

    // ==================== REVIEW OPERATIONS ====================

    suspend fun addReview(review: Review): Result<String> {
        return try {
            val reviewId = if (review.id == 0) reviewsRef.push().key!! else review.id.toString()
            val reviewMap = mapOf(
                "id" to reviewId,
                "employeeId" to review.employeeId,
                "date" to review.date,
                "quality" to review.quality,
                "communication" to review.communication,
                "innovation" to review.innovation,
                "timeliness" to review.timeliness,
                "attendance" to review.attendance,
                "overallRating" to review.overallRating,
                "remarks" to review.remarks,
                "reviewedBy" to review.reviewedBy
            )
            reviewsRef.child(reviewId).setValue(reviewMap).await()
            Result.success(reviewId)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding review", e)
            Result.failure(e)
        }
    }

    fun getReviewsByEmployeeFlow(employeeId: Int): Flow<List<Review>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val reviews = mutableListOf<Review>()
                snapshot.children.forEach { child ->
                    try {
                        val empId = child.child("employeeId").value.toString().toIntOrNull() ?: 0
                        if (empId == employeeId) {
                            val review = Review(
                                id = child.child("id").value.toString().toIntOrNull() ?: 0,
                                employeeId = empId,
                                date = child.child("date").value.toString(),
                                quality = child.child("quality").value.toString().toFloatOrNull() ?: 0f,
                                communication = child.child("communication").value.toString().toFloatOrNull() ?: 0f,
                                innovation = child.child("innovation").value.toString().toFloatOrNull() ?: 0f,
                                timeliness = child.child("timeliness").value.toString().toFloatOrNull() ?: 0f,
                                attendance = child.child("attendance").value.toString().toFloatOrNull() ?: 0f,
                                overallRating = child.child("overallRating").value.toString().toFloatOrNull() ?: 0f,
                                remarks = child.child("remarks").value.toString(),
                                reviewedBy = child.child("reviewedBy").value.toString()
                            )
                            reviews.add(review)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing review", e)
                    }
                }
                trySend(reviews)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Reviews listener cancelled", error.toException())
                close(error.toException())
            }
        }
        reviewsRef.addValueEventListener(listener)
        awaitClose { reviewsRef.removeEventListener(listener) }
    }

    // ==================== MESSAGE OPERATIONS ====================

    suspend fun sendMessage(message: Message): Result<String> {
        return try {
            val messageId = if (message.id == 0) messagesRef.push().key!! else message.id.toString()
            val messageMap = mapOf(
                "id" to messageId,
                "senderId" to message.senderId,
                "receiverId" to message.receiverId,
                "message" to message.message,
                "timestamp" to message.timestamp,
                "isRead" to message.isRead,
                "messageType" to message.messageType,
                "relatedReviewId" to message.relatedReviewId
            )
            messagesRef.child(messageId).setValue(messageMap).await()
            Result.success(messageId)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            Result.failure(e)
        }
    }

    suspend fun markMessageAsRead(messageId: Int): Result<Unit> {
        return try {
            messagesRef.child(messageId.toString()).child("isRead").setValue(true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking message as read", e)
            Result.failure(e)
        }
    }

    fun getConversationFlow(userId1: Int, userId2: Int): Flow<List<Message>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<Message>()
                snapshot.children.forEach { child ->
                    try {
                        val senderId = child.child("senderId").value.toString().toIntOrNull() ?: 0
                        val receiverId = child.child("receiverId").value.toString().toIntOrNull() ?: 0

                        if ((senderId == userId1 && receiverId == userId2) ||
                            (senderId == userId2 && receiverId == userId1)) {
                            val message = Message(
                                id = child.child("id").value.toString().toIntOrNull() ?: 0,
                                senderId = senderId,
                                receiverId = receiverId,
                                message = child.child("message").value.toString(),
                                timestamp = child.child("timestamp").value.toString().toLongOrNull() ?: 0L,
                                isRead = child.child("isRead").value as? Boolean ?: false,
                                messageType = child.child("messageType").value.toString(),
                                relatedReviewId = child.child("relatedReviewId").value.toString().toIntOrNull()
                            )
                            messages.add(message)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message", e)
                    }
                }
                trySend(messages.sortedBy { it.timestamp })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Conversation listener cancelled", error.toException())
                close(error.toException())
            }
        }
        messagesRef.addValueEventListener(listener)
        awaitClose { messagesRef.removeEventListener(listener) }
    }

    // ==================== ATTENDANCE OPERATIONS ====================

    suspend fun markAttendance(attendance: Attendance): Result<String> {
        return try {
            val attendanceId = if (attendance.id == 0) attendanceRef.push().key!! else attendance.id.toString()
            val attendanceMap = mapOf(
                "id" to attendanceId,
                "employeeId" to attendance.employeeId,
                "date" to attendance.date,
                "checkInTime" to attendance.checkInTime,
                "checkOutTime" to attendance.checkOutTime,
                "status" to attendance.status,
                "remarks" to attendance.remarks,
                "markedBy" to attendance.markedBy
            )
            attendanceRef.child(attendanceId).setValue(attendanceMap).await()
            Result.success(attendanceId)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking attendance", e)
            Result.failure(e)
        }
    }

    fun getAttendanceByEmployeeFlow(employeeId: Int): Flow<List<Attendance>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val attendanceList = mutableListOf<Attendance>()
                snapshot.children.forEach { child ->
                    try {
                        val empId = child.child("employeeId").value.toString().toIntOrNull() ?: 0
                        if (empId == employeeId) {
                            val attendance = Attendance(
                                id = child.child("id").value.toString().toIntOrNull() ?: 0,
                                employeeId = empId,
                                date = child.child("date").value.toString(),
                                checkInTime = child.child("checkInTime").value.toString(),
                                checkOutTime = child.child("checkOutTime").value.toString(),
                                status = child.child("status").value.toString(),
                                remarks = child.child("remarks").value.toString(),
                                markedBy = child.child("markedBy").value.toString()
                            )
                            attendanceList.add(attendance)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing attendance", e)
                    }
                }
                trySend(attendanceList.sortedByDescending { it.date })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Attendance listener cancelled", error.toException())
                close(error.toException())
            }
        }
        attendanceRef.addValueEventListener(listener)
        awaitClose { attendanceRef.removeEventListener(listener) }
    }
}