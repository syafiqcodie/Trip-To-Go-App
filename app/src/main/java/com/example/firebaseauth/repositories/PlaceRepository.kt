package com.example.firebaseauth.repositories

import com.example.firebaseauth.models.Place
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class PlaceRepository {
    private val db = FirebaseFirestore.getInstance()
    private val placesCollection = db.collection("places")

    suspend fun getPlaces(): List<Place> {
        return try {
            val snapshot = placesCollection.get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Place::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun addPlace(place: Place): String {
        return try {
            val docRef = placesCollection.add(place).await()
            docRef.id
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    suspend fun updatePlace(place: Place) {
        try {
            placesCollection.document(place.id).set(place).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deletePlace(placeId: String) {
        try {
            placesCollection.document(placeId).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addReview(placeId: String, rating: Double, review: String) {
        try {
            val placeRef = placesCollection.document(placeId)
            val place = placeRef.get().await().toObject(Place::class.java)
            
            place?.let {
                val newRating = ((it.rating * it.reviewCount) + rating) / (it.reviewCount + 1)
                val newReviewCount = it.reviewCount + 1
                
                placeRef.update(
                    mapOf(
                        "rating" to newRating,
                        "reviewCount" to newReviewCount
                    )
                ).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 