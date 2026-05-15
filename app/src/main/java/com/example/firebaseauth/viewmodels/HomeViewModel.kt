package com.example.firebaseauth.viewmodels

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.firebaseauth.models.Place
import com.example.firebaseauth.repositories.PlaceRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PlaceRepository()
    private val _places = MutableLiveData<List<Place>>()
    val places: LiveData<List<Place>> = _places

    private val _currentLocation = MutableLiveData<Location>()
    val currentLocation: LiveData<Location> = _currentLocation

    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(application)

    fun getCurrentLocation() {
        viewModelScope.launch {
            try {
                val location = fusedLocationClient.lastLocation.await()
                location?.let {
                    _currentLocation.postValue(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updatePlaces(placesList: List<Place>) {
        _places.value = placesList
    }

    fun filterPlaces(query: String) {
        viewModelScope.launch {
            try {
                val filteredPlaces = repository.getPlaces().filter { place ->
                    place.name.contains(query, ignoreCase = true) ||
                    place.description.contains(query, ignoreCase = true) ||
                    place.category.contains(query, ignoreCase = true)
                }
                _places.postValue(filteredPlaces)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getGoogleMapsUrl(place: Place): String {
        return "https://www.google.com/maps/dir/?api=1&destination=${place.latitude},${place.longitude}"
    }
} 