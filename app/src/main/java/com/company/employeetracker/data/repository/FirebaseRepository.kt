package com.company.employeetracker.data.repository

import android.util.Log
import com.company.employeetracker.data.database.entities.*
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val tag = "FirebaseRepository"

    // Reference paths
    private val usersRef = database.child("users")
    private val tasksRef = database.child("tasks")
    private val reviewsRef = database.child("reviews")
    private val messagesRef = database.child("messages")
    private val attendanceRef = database.child("attendance")

    // ==================== USER OPERATIONS ====================

    suspend fun addUser(user: User): Result<String> {
        return try {
            val userId = if (user.id == 0) {
                usersRef.push().key ?: System.currentTimeMillis().toString()
            } else {
                user.id.toString()
            }

            val userMap = hashMapOf<String, Any>(
                "id" to (userId.toIntOrNull() ?: System.currentTimeMillis().toInt()),
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
            Log.d(tag, "User added successfully: ${user.name}")
            Result.success(userId)
        } catch (e: Exception) {
            Log.e(tag, "Error adding user: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            val userMap = hashMapOf<String, Any>(
                "id" to user.id,
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
            usersRef.child(user.id.toString()).setValue(userMap).await()
            Log.d(tag, "User updated successfully: ${user.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error updating user: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteUser(userId: Int): Result<Unit> {
        return try {
            usersRef.child(userId.toString()).removeValue().await()
            Log.d(tag, "User deleted successfully: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error deleting user: ${e.message}", e)
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
                            id = (child.child("id").value as? Long)?.toInt()
                                ?: child.child("id").value.toString().toIntOrNull() ?: 0,
                            email = child.child("email").value?.toString() ?: "",
                            password = child.child("password").value?.toString() ?: "",
                            name = child.child("name").value?.toString() ?: "",
                            role = child.child("role").value?.toString() ?: "",
                            designation = child.child("designation").value?.toString() ?: "",
                            department = child.child("department").value?.toString() ?: "",
                            joiningDate = child.child("joiningDate").value?.toString() ?: "",
                            contact = child.child("contact").value?.toString() ?: "",
                            profileImage = child.child("profileImage").value?.toString() ?: ""
                        )
                        users.add(user)
                    } catch (e: Exception) {
                        Log.e(tag, "Error parsing user: ${e.message}", e)
                    }
                }
                trySend(users)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(tag, "Users listener cancelled: ${error.message}")
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
                        val role = child.child("role").value?.toString()
                        if (role == "employee") {
                            val user = User(
                                id = (child.child("id").value as? Long)?.toInt()
                                    ?: child.child("id").value.toString().toIntOrNull() ?: 0,
                                email = child.child("email").value?.toString() ?: "",
                                password = child.child("password").value?.toString() ?: "",
                                name = child.child("name").value?.toString() ?: "",
                                role = role,
                                designation = child.child("designation").value?.toString() ?: "",
                                department = child.child("department").value?.toString() ?: "",
                                joiningDate = child.child("joiningDate").value?.toString() ?: "",
                                contact = child.child("contact").value?.toString() ?: "",
                                profileImage = child.child("profileImage").value?.toString() ?: ""
                            )
                            employees.add(user)
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error parsing employee: ${e.message}", e)
                    }
                }
                trySend(employees)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(tag, "Employees listener cancelled: ${error.message}")
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
                val storedPassword = child.child("password").value?.toString()
                if (storedPassword == password) {
                    user = User(
                        id = (child.child("id").value as? Long)?.toInt()
                            ?: child.child("id").value.toString().toIntOrNull() ?: 0,
                        email = child.child("email").value?.toString() ?: "",
                        password = storedPassword,
                        name = child.child("name").value?.toString() ?: "",
                        role = child.child("role").value?.toString() ?: "",
                        designation = child.child("designation").value?.toString() ?: "",
                        department = child.child("department").value?.toString() ?: "",
                        joiningDate = child.child("joiningDate").value?.toString() ?: "",
                        contact = child.child("contact").value?.toString() ?: "",
                        profileImage = child.child("profileImage").value?.toString() ?: ""
                    )
                }
            }
            Result.success(user)
        } catch (e: Exception) {
            Log.e(tag, "Error logging in: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ==================== TASK OPERATIONS ====================

    suspend fun addTask(task: Task): Result<String> {
        return try {
            val taskId = if (task.id == 0) {
                tasksRef.push().key ?: System.currentTimeMillis().toString()
            } else {
                task.id.toString()
            }

            val taskMap = hashMapOf<String, Any>(
                "id" to (taskId.toIntOrNull() ?: System.currentTimeMillis().toInt()),
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
            Log.d(tag, "Task added successfully: ${task.title}")
            Result.success(taskId)
        } catch (e: Exception) {
            Log.e(tag, "Error adding task: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateTask(task: Task): Result<Unit> {
        return try {
            val taskMap = hashMapOf<String, Any>(
                "id" to task.id,
                "employeeId" to task.employeeId,
                "title" to task.title,
                "description" to task.description,
                "priority" to task.priority,
                "status" to task.status,
                "deadline" to task.deadline,
                "assignedDate" to task.assignedDate,
                "assignedBy" to task.assignedBy
            )
            tasksRef.child(task.id.toString()).setValue(taskMap).await()
            Log.d(tag, "Task updated successfully: ${task.title}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error updating task: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: Int): Result<Unit> {
        return try {
            Log.d(tag, "🗑️ Deleting task $taskId from Firebase")

            // Delete the task from Firebase
            tasksRef.child(taskId.toString()).removeValue().await()

            Log.d(tag, "✅ Task $taskId deleted successfully from Firebase")

            // Wait a moment to ensure Firebase propagates the deletion
            kotlinx.coroutines.delay(100)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "❌ Error deleting task $taskId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun taskExists(taskId: Int): Boolean {
        return try {
            val snapshot = tasksRef.child(taskId.toString()).get().await()
            snapshot.exists()
        } catch (e: Exception) {
            Log.e(tag, "Error checking task existence: ${e.message}", e)
            false
        }
    }
    fun getAllTasksFlow(): Flow<List<Task>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tasks = mutableListOf<Task>()
                snapshot.children.forEach { child ->
                    try {
                        val task = Task(
                            id = (child.child("id").value as? Long)?.toInt()
                                ?: child.child("id").value.toString().toIntOrNull() ?: 0,
                            employeeId = (child.child("employeeId").value as? Long)?.toInt()
                                ?: child.child("employeeId").value.toString().toIntOrNull() ?: 0,
                            title = child.child("title").value?.toString() ?: "",
                            description = child.child("description").value?.toString() ?: "",
                            priority = child.child("priority").value?.toString() ?: "",
                            status = child.child("status").value?.toString() ?: "",
                            deadline = child.child("deadline").value?.toString() ?: "",
                            assignedDate = child.child("assignedDate").value?.toString() ?: "",
                            assignedBy = child.child("assignedBy").value?.toString() ?: ""
                        )
                        tasks.add(task)
                    } catch (e: Exception) {
                        Log.e(tag, "Error parsing task: ${e.message}", e)
                    }
                }
                trySend(tasks)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(tag, "Tasks listener cancelled: ${error.message}")
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
                        val empId = (child.child("employeeId").value as? Long)?.toInt()
                            ?: child.child("employeeId").value.toString().toIntOrNull() ?: 0
                        if (empId == employeeId) {
                            val task = Task(
                                id = (child.child("id").value as? Long)?.toInt()
                                    ?: child.child("id").value.toString().toIntOrNull() ?: 0,
                                employeeId = empId,
                                title = child.child("title").value?.toString() ?: "",
                                description = child.child("description").value?.toString() ?: "",
                                priority = child.child("priority").value?.toString() ?: "",
                                status = child.child("status").value?.toString() ?: "",
                                deadline = child.child("deadline").value?.toString() ?: "",
                                assignedDate = child.child("assignedDate").value?.toString() ?: "",
                                assignedBy = child.child("assignedBy").value?.toString() ?: ""
                            )
                            tasks.add(task)
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error parsing employee task: ${e.message}", e)
                    }
                }
                trySend(tasks)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(tag, "Employee tasks listener cancelled: ${error.message}")
                close(error.toException())
            }
        }
        tasksRef.addValueEventListener(listener)
        awaitClose { tasksRef.removeEventListener(listener) }
    }

    // ==================== REVIEW OPERATIONS ====================

    suspend fun addReview(review: Review): Result<String> {
        return try {
            val reviewId = if (review.id == 0) {
                reviewsRef.push().key ?: System.currentTimeMillis().toString()
            } else {
                review.id.toString()
            }

            val reviewMap = hashMapOf<String, Any>(
                "id" to (reviewId.toIntOrNull() ?: System.currentTimeMillis().toInt()),
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
            Log.d(tag, "Review added successfully for employee: ${review.employeeId}")
            Result.success(reviewId)
        } catch (e: Exception) {
            Log.e(tag, "Error adding review: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateReview(review: Review): Result<Unit> {
        return try {
            val reviewMap = hashMapOf<String, Any>(
                "id" to review.id,
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
            reviewsRef.child(review.id.toString()).setValue(reviewMap).await()
            Log.d(tag, "Review updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error updating review: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteReview(reviewId: Int): Result<Unit> {
        return try {
            reviewsRef.child(reviewId.toString()).removeValue().await()
            Log.d(tag, "Review deleted successfully: $reviewId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error deleting review: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getAllReviewsFlow(): Flow<List<Review>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val reviews = mutableListOf<Review>()
                snapshot.children.forEach { child ->
                    try {
                        val review = Review(
                            id = (child.child("id").value as? Long)?.toInt()
                                ?: child.child("id").value.toString().toIntOrNull() ?: 0,
                            employeeId = (child.child("employeeId").value as? Long)?.toInt()
                                ?: child.child("employeeId").value.toString().toIntOrNull() ?: 0,
                            date = child.child("date").value?.toString() ?: "",
                            quality = (child.child("quality").value as? Number)?.toFloat() ?: 0f,
                            communication = (child.child("communication").value as? Number)?.toFloat() ?: 0f,
                            innovation = (child.child("innovation").value as? Number)?.toFloat() ?: 0f,
                            timeliness = (child.child("timeliness").value as? Number)?.toFloat() ?: 0f,
                            attendance = (child.child("attendance").value as? Number)?.toFloat() ?: 0f,
                            overallRating = (child.child("overallRating").value as? Number)?.toFloat() ?: 0f,
                            remarks = child.child("remarks").value?.toString() ?: "",
                            reviewedBy = child.child("reviewedBy").value?.toString() ?: ""
                        )
                        reviews.add(review)
                    } catch (e: Exception) {
                        Log.e(tag, "Error parsing review: ${e.message}", e)
                    }
                }
                trySend(reviews)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(tag, "Reviews listener cancelled: ${error.message}")
                close(error.toException())
            }
        }
        reviewsRef.addValueEventListener(listener)
        awaitClose { reviewsRef.removeEventListener(listener) }
    }

    fun getReviewsByEmployeeFlow(employeeId: Int): Flow<List<Review>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val reviews = mutableListOf<Review>()
                snapshot.children.forEach { child ->
                    try {
                        val empId = (child.child("employeeId").value as? Long)?.toInt()
                            ?: child.child("employeeId").value.toString().toIntOrNull() ?: 0
                        if (empId == employeeId) {
                            val review = Review(
                                id = (child.child("id").value as? Long)?.toInt()
                                    ?: child.child("id").value.toString().toIntOrNull() ?: 0,
                                employeeId = empId,
                                date = child.child("date").value?.toString() ?: "",
                                quality = (child.child("quality").value as? Number)?.toFloat() ?: 0f,
                                communication = (child.child("communication").value as? Number)?.toFloat() ?: 0f,
                                innovation = (child.child("innovation").value as? Number)?.toFloat() ?: 0f,
                                timeliness = (child.child("timeliness").value as? Number)?.toFloat() ?: 0f,
                                attendance = (child.child("attendance").value as? Number)?.toFloat() ?: 0f,
                                overallRating = (child.child("overallRating").value as? Number)?.toFloat() ?: 0f,
                                remarks = child.child("remarks").value?.toString() ?: "",
                                reviewedBy = child.child("reviewedBy").value?.toString() ?: ""
                            )
                            reviews.add(review)
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error parsing review: ${e.message}", e)
                    }
                }
                trySend(reviews)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(tag, "Reviews listener cancelled: ${error.message}")
                close(error.toException())
            }
        }
        reviewsRef.addValueEventListener(listener)
        awaitClose { reviewsRef.removeEventListener(listener) }
    }

    // ==================== MESSAGE OPERATIONS ====================

    suspend fun sendMessage(message: Message): Result<String> {
        return try {
            val messageId = if (message.id == 0) {
                messagesRef.push().key ?: System.currentTimeMillis().toString()
            } else {
                message.id.toString()
            }

            val messageMap = hashMapOf<String, Any>(
                "id" to (messageId.toIntOrNull() ?: System.currentTimeMillis().toInt()),
                "senderId" to message.senderId,
                "receiverId" to message.receiverId,
                "message" to message.message,
                "timestamp" to message.timestamp,
                "isRead" to message.isRead,
                "messageType" to message.messageType,
                "relatedReviewId" to (message.relatedReviewId ?: 0)
            )

            messagesRef.child(messageId).setValue(messageMap).await()
            Log.d(tag, "Message sent successfully")
            Result.success(messageId)
        } catch (e: Exception) {
            Log.e(tag, "Error sending message: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun markMessageAsRead(messageId: Int): Result<Unit> {
        return try {
            messagesRef.child(messageId.toString()).child("isRead").setValue(true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error marking message as read: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getConversationFlow(userId1: Int, userId2: Int): Flow<List<Message>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<Message>()
                snapshot.children.forEach { child ->
                    try {
                        val senderId = (child.child("senderId").value as? Long)?.toInt()
                            ?: child.child("senderId").value.toString().toIntOrNull() ?: 0
                        val receiverId = (child.child("receiverId").value as? Long)?.toInt()
                            ?: child.child("receiverId").value.toString().toIntOrNull() ?: 0

                        if ((senderId == userId1 && receiverId == userId2) ||
                            (senderId == userId2 && receiverId == userId1)) {
                            val message = Message(
                                id = (child.child("id").value as? Long)?.toInt()
                                    ?: child.child("id").value.toString().toIntOrNull() ?: 0,
                                senderId = senderId,
                                receiverId = receiverId,
                                message = child.child("message").value?.toString() ?: "",
                                timestamp = (child.child("timestamp").value as? Long) ?: 0L,
                                isRead = child.child("isRead").value as? Boolean ?: false,
                                messageType = child.child("messageType").value?.toString() ?: "DIRECT",
                                relatedReviewId = (child.child("relatedReviewId").value as? Long)?.toInt()
                            )
                            messages.add(message)
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error parsing message: ${e.message}", e)
                    }
                }
                trySend(messages.sortedBy { it.timestamp })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(tag, "Conversation listener cancelled: ${error.message}")
                close(error.toException())
            }
        }
        messagesRef.addValueEventListener(listener)
        awaitClose { messagesRef.removeEventListener(listener) }
    }

    // ==================== ATTENDANCE OPERATIONS ====================

    suspend fun markAttendance(attendance: Attendance): Result<String> {
        return try {
            val attendanceId = if (attendance.id == 0) {
                attendanceRef.push().key ?: System.currentTimeMillis().toString()
            } else {
                attendance.id.toString()
            }

            val attendanceMap = hashMapOf<String, Any>(
                "id" to (attendanceId.toIntOrNull() ?: System.currentTimeMillis().toInt()),
                "employeeId" to attendance.employeeId,
                "date" to attendance.date,
                "checkInTime" to attendance.checkInTime,
                "checkOutTime" to (attendance.checkOutTime ?: ""),
                "status" to attendance.status,
                "remarks" to attendance.remarks,
                "markedBy" to attendance.markedBy
            )

            attendanceRef.child(attendanceId).setValue(attendanceMap).await()
            Log.d(tag, "Attendance marked successfully")
            Result.success(attendanceId)
        } catch (e: Exception) {
            Log.e(tag, "Error marking attendance: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getAttendanceByEmployeeFlow(employeeId: Int): Flow<List<Attendance>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val attendanceList = mutableListOf<Attendance>()
                snapshot.children.forEach { child ->
                    try {
                        val empId = (child.child("employeeId").value as? Long)?.toInt()
                            ?: child.child("employeeId").value.toString().toIntOrNull() ?: 0
                        if (empId == employeeId) {
                            val attendance = Attendance(
                                id = (child.child("id").value as? Long)?.toInt()
                                    ?: child.child("id").value.toString().toIntOrNull() ?: 0,
                                employeeId = empId,
                                date = child.child("date").value?.toString() ?: "",
                                checkInTime = child.child("checkInTime").value?.toString() ?: "",
                                checkOutTime = child.child("checkOutTime").value?.toString(),
                                status = child.child("status").value?.toString() ?: "",
                                remarks = child.child("remarks").value?.toString() ?: "",
                                markedBy = child.child("markedBy").value?.toString() ?: ""
                            )
                            attendanceList.add(attendance)
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error parsing attendance: ${e.message}", e)
                    }
                }
                trySend(attendanceList.sortedByDescending { it.date })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(tag, "Attendance listener cancelled: ${error.message}")
                close(error.toException())
            }
        }
        attendanceRef.addValueEventListener(listener)
        awaitClose { attendanceRef.removeEventListener(listener) }
    }

    /**
     * Cascade delete: Deletes employee and ALL related data from Firebase
     */
    suspend fun deleteEmployeeCascade(employeeId: Int): Result<Unit> {
        return try {
            Log.d(tag, "Starting cascade delete for employee: $employeeId")

            // 1. Delete all tasks for this employee
            val tasksSnapshot = tasksRef.orderByChild("employeeId").equalTo(employeeId.toDouble()).get().await()
            tasksSnapshot.children.forEach { taskSnapshot ->
                taskSnapshot.ref.removeValue().await()
                Log.d(tag, "Deleted task: ${taskSnapshot.key}")
            }

            // 2. Delete all reviews for this employee
            val reviewsSnapshot = reviewsRef.orderByChild("employeeId").equalTo(employeeId.toDouble()).get().await()
            reviewsSnapshot.children.forEach { reviewSnapshot ->
                reviewSnapshot.ref.removeValue().await()
                Log.d(tag, "Deleted review: ${reviewSnapshot.key}")
            }

            // 3. Delete all messages sent by or to this employee
            val messagesSentSnapshot = messagesRef.orderByChild("senderId").equalTo(employeeId.toDouble()).get().await()
            messagesSentSnapshot.children.forEach { messageSnapshot ->
                messageSnapshot.ref.removeValue().await()
                Log.d(tag, "Deleted sent message: ${messageSnapshot.key}")
            }

            val messagesReceivedSnapshot = messagesRef.orderByChild("receiverId").equalTo(employeeId.toDouble()).get().await()
            messagesReceivedSnapshot.children.forEach { messageSnapshot ->
                messageSnapshot.ref.removeValue().await()
                Log.d(tag, "Deleted received message: ${messageSnapshot.key}")
            }

            // 4. Delete all attendance records for this employee
            val attendanceSnapshot = attendanceRef.orderByChild("employeeId").equalTo(employeeId.toDouble()).get().await()
            attendanceSnapshot.children.forEach { attSnapshot ->
                attSnapshot.ref.removeValue().await()
                Log.d(tag, "Deleted attendance: ${attSnapshot.key}")
            }

            // 5. Finally, delete the employee user record
            usersRef.child(employeeId.toString()).removeValue().await()
            Log.d(tag, "Deleted employee user record: $employeeId")

            Log.d(tag, "✅ Successfully completed cascade delete for employee: $employeeId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "❌ Error in cascade delete for employee $employeeId: ${e.message}", e)
            Result.failure(e)
        }
    }
}