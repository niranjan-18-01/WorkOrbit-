package com.company.employeetracker.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["employeeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Task(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val employeeId: Int,

    val title: String,

    val description: String,

    val priority: String, // Low | Medium | High | Critical

    val status: String, // Pending | Active | Done

    val deadline: String, // yyyy-MM-dd (for UI)

    val deadlineTimestamp: Long, // ✅ epoch millis (for sorting & logic)

    val assignedDate: String,

    val assignedBy: String = ""
)
