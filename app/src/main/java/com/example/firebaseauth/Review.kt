package com.example.firebaseauth

import com.google.firebase.Timestamp

data class Review(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val placeName: String = "",
    val rating: Float = 0.0f,
    val comment: String = "",
    val timestamp: String? = null,
    val date: String = "" // Format: "YYYY-MM-DD"
) 