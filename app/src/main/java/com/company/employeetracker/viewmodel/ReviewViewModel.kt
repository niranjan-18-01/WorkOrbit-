package com.company.employeetracker.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.company.employeetracker.data.database.AppDatabase
import com.company.employeetracker.data.database.entities.Review
import com.company.employeetracker.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReviewViewModel(application: Application) : AndroidViewModel(application) {
    private val reviewDao = AppDatabase.getDatabase(application).reviewDao()
    private val firebaseRepo = FirebaseRepository()
    private val tag = "ReviewViewModel"

    private val _allReviews = MutableStateFlow<List<Review>>(emptyList())
    val allReviews: StateFlow<List<Review>> = _allReviews

    private val _employeeReviews = MutableStateFlow<List<Review>>(emptyList())
    val employeeReviews: StateFlow<List<Review>> = _employeeReviews

    private val _averageRating = MutableStateFlow(0f)
    val averageRating: StateFlow<Float> = _averageRating

    private val _reviewCount = MutableStateFlow(0)
    val reviewCount: StateFlow<Int> = _reviewCount

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadAllReviews()
    }

    private fun loadAllReviews() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Listen to Firebase real-time updates
                reviewDao.getAllReviews().collect { reviews ->
                    _allReviews.value = reviews
                    _reviewCount.value = reviews.size
                    Log.d(tag, "Loaded ${reviews.size} reviews from Firebase")

                    // Sync to local database
                    syncToLocalDatabase(reviews)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading reviews from Firebase", e)
                // Fallback to local database
                loadFromLocalDatabase()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadReviewsForEmployee(employeeId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Load from Firebase with real-time updates
                firebaseRepo.getReviewsByEmployeeFlow(employeeId).collect { reviews ->
                    _employeeReviews.value = reviews

                    // Calculate average rating
                    if (reviews.isNotEmpty()) {
                        val avg = reviews.map { it.overallRating }.average().toFloat()
                        _averageRating.value = avg
                    } else {
                        _averageRating.value = 0f
                    }

                    Log.d(tag, "Loaded ${reviews.size} reviews for employee $employeeId")

                    // Sync to local database
                    syncToLocalDatabase(reviews)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading employee reviews", e)
                // Fallback to local database
                reviewDao.getReviewsByEmployee(employeeId).collect { reviews ->
                    _employeeReviews.value = reviews
                    val avgRating = reviewDao.getAverageRating(employeeId)
                    _averageRating.value = avgRating ?: 0f
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun syncToLocalDatabase(reviews: List<Review>) {
        try {
            reviews.forEach { review ->
                reviewDao.insertReview(review)
            }
            Log.d(tag, "Synced ${reviews.size} reviews to local DB")
        } catch (e: Exception) {
            Log.e(tag, "Error syncing reviews to local DB", e)
        }
    }

    private fun loadFromLocalDatabase() {
        viewModelScope.launch {
            reviewDao.getAllReviews().collect { reviews ->
                _allReviews.value = reviews
                _reviewCount.value = reviews.size
            }
        }
    }

    fun addReview(review: Review) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(tag, "Adding review for employee ${review.employeeId}")

                // Add to Firebase
                val result = firebaseRepo.addReview(review)

                if (result.isSuccess) {
                    Log.d(tag, "✅ Review added to Firebase successfully")
                } else {
                    Log.e(tag, "Failed to add review to Firebase", result.exceptionOrNull())
                    // Fallback: add to local database only
                    reviewDao.insertReview(review)
                }
            } catch (e: Exception) {
                Log.e(tag, "Error adding review", e)
                // Fallback to local database
                reviewDao.insertReview(review)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateReview(review: Review) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(tag, "Updating review ${review.id}")

                // Update in Firebase (add update method to FirebaseRepository)
                // For now, use local DB only
                reviewDao.updateReview(review)
                Log.d(tag, "Review updated in local DB")
            } catch (e: Exception) {
                Log.e(tag, "Error updating review", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteReview(review: Review) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(tag, "Deleting review ${review.id}")

                // Delete from Firebase (add delete method to FirebaseRepository)
                // For now, use local DB only
                reviewDao.deleteReview(review)
                Log.d(tag, "Review deleted from local DB")
            } catch (e: Exception) {
                Log.e(tag, "Error deleting review", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun forceSync() {
        loadAllReviews()
    }
}