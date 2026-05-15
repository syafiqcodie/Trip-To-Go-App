package com.example.firebaseauth
data class Attraction(
    val name: String,
    val description: String,
    val category: String,
    val price: Double,
    val imageRes: Int,
    val latitude: Double,
    val longitude: Double,
    val distance: Double = 0.0 // Distance in KM from user's location
)
