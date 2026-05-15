package com.example.firebaseauth.models

data class Place(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val googleMapsUrl: String = "",
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) 