package com.example.firebaseauth

data class ChecklistItem(
    val id: String = "",
    val place: String = "",
    val photoUrl: String? = null,
    val isChecked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val date: String = "",  // Format: "YYYY-MM-DD"
    val budget: Double = 0.0,
    val hasReview: Boolean = false
) 