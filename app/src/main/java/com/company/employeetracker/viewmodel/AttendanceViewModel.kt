package com.company.employeetracker.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.company.employeetracker.data.database.AppDatabase
import com.company.employeetracker.data.database.entities.Attendance
import com.company.employeetracker.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {
    private val attendanceDao = AppDatabase.getDatabase(application).attendanceDao()
    private val firebaseRepo = FirebaseRepository()
    private val tag = "AttendanceViewModel"

    private val _allAttendance = MutableStateFlow<List<Attendance>>(emptyList())
    val allAttendance: StateFlow<List<Attendance>> = _allAttendance

    private val _employeeAttendance = MutableStateFlow<List<Attendance>>(emptyList())
    val employeeAttendance: StateFlow<List<Attendance>> = _employeeAttendance

    private val _todayAttendance = MutableStateFlow<List<Attendance>>(emptyList())
    val todayAttendance: StateFlow<List<Attendance>> = _todayAttendance

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadAllAttendance()
    }

    private fun loadAllAttendance() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Load from local database for now
                // TODO: Implement Firebase attendance flow if needed
                attendanceDao.getAllAttendance().collect { attendance ->
                    _allAttendance.value = attendance
                    Log.d(tag, "Loaded ${attendance.size} attendance records")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading all attendance", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadAttendanceForEmployee(employeeId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Load from Firebase with real-time updates
                firebaseRepo.getAttendanceByEmployeeFlow(employeeId).collect { attendance ->
                    _employeeAttendance.value = attendance
                    Log.d(tag, "Loaded ${attendance.size} attendance records for employee $employeeId")

                    // Sync to local database
                    syncAttendanceToLocal(attendance)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading employee attendance from Firebase", e)
                // Fallback to local database
                attendanceDao.getAttendanceByEmployee(employeeId).collect { attendance ->
                    _employeeAttendance.value = attendance
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadTodayAttendance(date: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                attendanceDao.getAttendanceByDate(date).collect { attendance ->
                    _todayAttendance.value = attendance
                    Log.d(tag, "Loaded ${attendance.size} attendance records for date $date")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading today's attendance", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun syncAttendanceToLocal(attendanceList: List<Attendance>) {
        try {
            attendanceList.forEach { attendance ->
                attendanceDao.insertAttendance(attendance)
            }
            Log.d(tag, "Synced ${attendanceList.size} attendance records to local DB")
        } catch (e: Exception) {
            Log.e(tag, "Error syncing attendance to local DB", e)
        }
    }

    fun markAttendance(attendance: Attendance) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(tag, "Marking attendance for employee ${attendance.employeeId}")

                // Save to Firebase
                val result = firebaseRepo.markAttendance(attendance)

                if (result.isSuccess) {
                    Log.d(tag, "✅ Attendance marked in Firebase successfully")
                } else {
                    Log.e(tag, "Failed to mark attendance in Firebase", result.exceptionOrNull())
                    // Fallback: save to local database only
                    attendanceDao.insertAttendance(attendance)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error marking attendance", e)
                // Fallback to local database
                attendanceDao.insertAttendance(attendance)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateAttendance(attendance: Attendance) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(tag, "Updating attendance ${attendance.id}")

                // Update in Firebase (reuse markAttendance as it uses REPLACE strategy)
                val result = firebaseRepo.markAttendance(attendance)

                if (result.isSuccess) {
                    Log.d(tag, "Attendance updated in Firebase")
                } else {
                    Log.e(tag, "Failed to update attendance in Firebase")
                }

                // Update in local database
                attendanceDao.updateAttendance(attendance)
            } catch (e: Exception) {
                Log.e(tag, "Error updating attendance", e)
                attendanceDao.updateAttendance(attendance)
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun getAttendanceForDate(employeeId: Int, date: String): Attendance? {
        return try {
            attendanceDao.getAttendanceByEmployeeAndDate(employeeId, date)
        } catch (e: Exception) {
            Log.e(tag, "Error getting attendance for date", e)
            null
        }
    }

    suspend fun getPresentDays(employeeId: Int): Int {
        return try {
            attendanceDao.getPresentDays(employeeId)
        } catch (e: Exception) {
            Log.e(tag, "Error getting present days", e)
            0
        }
    }

    suspend fun getAbsentDays(employeeId: Int): Int {
        return try {
            attendanceDao.getAbsentDays(employeeId)
        } catch (e: Exception) {
            Log.e(tag, "Error getting absent days", e)
            0
        }
    }

    fun forceSync(employeeId: Int) {
        loadAttendanceForEmployee(employeeId)
    }
}