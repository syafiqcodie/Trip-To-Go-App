package com.example.firebaseauth

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firebaseauth.adapters.PlaceAdapter
import com.example.firebaseauth.databinding.FragmentHomeBinding
import com.example.firebaseauth.models.Place
import com.example.firebaseauth.repositories.PlaceRepository
import com.example.firebaseauth.viewmodels.HomeViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: PlaceAdapter
    private val repository = PlaceRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    private lateinit var searchEditText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var attractionAdapter: AttractionAdapter
    
    // Category filter buttons
    private lateinit var btnAllCategories: Button
    private lateinit var btnNature: Button
    private lateinit var btnHistoricalSite: Button
    private lateinit var btnModernArchitecture: Button
    private lateinit var btnCulturalSite: Button
    private lateinit var btnNearMe: Button
    
    private lateinit var btnNearMeMain: com.google.android.material.button.MaterialButton
    private var isMainNearMeActive = false
    private val NEAR_ME_RADIUS_KM = 15.0

    private var currentSearchQuery = ""
    private var currentCategoryFilter = "All"
    private var isNearMeFilterActive = false
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0

    // Reference point (Kuala Lumpur city center)
    private val referenceLatitude = 3.1390
    private val referenceLongitude = 101.6869

    private val allAttractions = listOf(
        Attraction("Petronas Twin Towers", "The iconic twin skyscrapers in Kuala Lumpur, standing at 452 meters.", "Modern Architecture", 150.0, R.drawable.petronas, 3.1579, 101.7116, 0.0),
        Attraction("Batu Caves", "A limestone hill with caves and Hindu temples located near Kuala Lumpur.", "Cultural Site", 0.0, R.drawable.batucaves, 3.2373, 101.6835, 0.0),
        Attraction("Langkawi Island", "An archipelago of 99 islands known for its stunning beaches and natural beauty.", "Nature", 200.0, R.drawable.langkawi, 6.3500, 99.8000, 0.0),
        Attraction("Mount Kinabalu", "The highest peak in Malaysia, located in the state of Sabah.", "Nature", 300.0, R.drawable.mountkinabalu, 6.0750, 116.5583, 0.0),
        Attraction("George Town, Penang", "A UNESCO World Heritage site known for its colonial architecture and vibrant street art.", "Historical Site", 50.0, R.drawable.georgetown, 5.4164, 100.3327, 0.0),
        Attraction("Taman Negara National Park", "A vast rainforest with diverse flora and fauna, ideal for eco-tourism.", "Nature", 100.0, R.drawable.tamannegara, 4.5500, 102.4500, 0.0),
        Attraction("Sultan Abdul Samad Building", "An iconic historical building in Kuala Lumpur with Moorish architectural style.", "Historical Site", 0.0, R.drawable.sasbuilding, 3.1486, 101.6944, 0.0),
        Attraction("Melaka (Malacca)", "A city rich in history with colonial buildings, forts, and museums.", "Historical Site", 75.0, R.drawable.melaka, 2.1896, 102.2501, 0.0),
        Attraction("Perhentian Islands", "Beautiful tropical islands perfect for snorkeling and diving, located off the coast of Terengganu.", "Nature", 250.0, R.drawable.stopisland, 5.9167, 102.7333, 0.0),
        Attraction("Putrajaya", "A planned city known for its modern architecture, including the Putra Mosque and Putrajaya Lake.", "Modern Architecture", 25.0, R.drawable.putrajaya, 2.9431, 101.6994, 0.0)
    )

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                viewModel.getCurrentLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                viewModel.getCurrentLocation()
            }
            else -> {
                Toast.makeText(context, "Location permission is required for distance calculation", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewModel()
        setupRecyclerView()
        setupSearchView()
        setupCategoryButtons()
        setupNearMeMainButton()
        setupLocationPermission()
        setupAddPlaceButton()
        loadPlaces()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        viewModel.places.observe(viewLifecycleOwner) { places ->
            adapter.submitList(places)
        }
    }

    private fun setupRecyclerView() {
        adapter = PlaceAdapter(
            onNavigateClick = { place ->
                val url = viewModel.getGoogleMapsUrl(place)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            },
            onAddToChecklist = { place ->
                saveToChecklist(place)
            },
            onReviewClick = { place ->
                showReviewDialog(place)
            },
            userLatitude = userLatitude,
            userLongitude = userLongitude
        )
        binding.recyclerViewPlaces.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@HomeFragment.adapter
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentSearchQuery = query ?: ""
                applyFilters()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText ?: ""
                applyFilters()
                return true
            }
        })
    }

    private fun setupLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.getCurrentLocation()
            }
            else -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
        
        // Observe location changes
        viewModel.currentLocation.observe(viewLifecycleOwner) { location ->
            location?.let {
                userLatitude = it.latitude
                userLongitude = it.longitude
                adapter.updateUserLocation(userLatitude, userLongitude)
            }
        }
    }

    private fun setupAddPlaceButton() {
        binding.fabAddPlace.setOnClickListener {
            showAddPlaceDialog()
        }
    }

    private fun loadPlaces() {
        CoroutineScope(Dispatchers.Main).launch {
            val places = repository.getPlaces()
            if (places.isEmpty()) {
                // If no places exist, show option to load sample data
                showLoadSampleDataDialog()
            } else {
                viewModel.updatePlaces(places)
            }
        }
    }

    private fun showLoadSampleDataDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("No Places Found")
            .setMessage("Would you like to load sample Malaysian tourism places to get started?")
            .setPositiveButton("Load Sample Data") { _, _ ->
                loadSamplePlacesToFirestore()
            }
            .setNegativeButton("Skip") { _, _ ->
                viewModel.updatePlaces(emptyList())
            }
            .setCancelable(false)
            .show()
    }

    private fun loadSamplePlacesToFirestore() {
        val places = listOf(
            Place(
                name = "Petronas Twin Towers",
                description = "Iconic twin skyscrapers in Kuala Lumpur, offering a skybridge and observation deck. The world's tallest twin towers and a symbol of Malaysia's modernization.",
                category = "Modern Architecture",
                price = 80.0,
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/9/9f/Petronas_Twin_Towers_2014.jpg",
                latitude = 3.1579,
                longitude = 101.7114,
                googleMapsUrl = "https://goo.gl/maps/6Q1Q1Q1Q1Q12"
            ),
            Place(
                name = "Batu Caves",
                description = "Famous limestone caves and Hindu temple with a giant golden statue. Features 272 colorful steps leading to the main cave temple.",
                category = "Nature",
                price = 0.0,
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/6/6e/Batu_Caves_Statue.jpg",
                latitude = 3.2379,
                longitude = 101.6831,
                googleMapsUrl = "https://goo.gl/maps/2Q2Q2Q2Q2Q22"
            ),
            Place(
                name = "Langkawi Sky Bridge",
                description = "Curved pedestrian bridge with stunning views of Langkawi. Suspended 700 meters above sea level, offering panoramic views of the Andaman Sea.",
                category = "Nature",
                price = 35.0,
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/2/2d/Langkawi_Sky_Bridge.jpg",
                latitude = 6.3818,
                longitude = 99.6701,
                googleMapsUrl = "https://goo.gl/maps/3Q3Q3Q3Q3Q32"
            ),
            Place(
                name = "George Town, Penang",
                description = "UNESCO World Heritage Site known for its street art and colonial architecture. Famous for its food culture and vibrant street life.",
                category = "Cultural Site",
                price = 0.0,
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/4/4e/George_Town_Penang.jpg",
                latitude = 5.4141,
                longitude = 100.3288,
                googleMapsUrl = "https://goo.gl/maps/4Q4Q4Q4Q4Q42"
            ),
            Place(
                name = "Mount Kinabalu",
                description = "Malaysia's highest peak, popular for hiking and biodiversity. Home to over 5,000 plant species and 326 bird species.",
                category = "Nature",
                price = 200.0,
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/5/5e/Mount_Kinabalu.jpg",
                latitude = 6.0754,
                longitude = 116.5584,
                googleMapsUrl = "https://goo.gl/maps/5Q5Q5Q5Q5Q52"
            ),
            Place(
                name = "Melaka Historic City",
                description = "Historic city with Dutch, Portuguese, and British colonial buildings. Known for its rich cultural heritage and delicious local cuisine.",
                category = "Historical Site",
                price = 0.0,
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/7/7e/Melaka_Stadthuys.jpg",
                latitude = 2.1896,
                longitude = 102.2501,
                googleMapsUrl = "https://goo.gl/maps/6Q6Q6Q6Q6Q62"
            ),
            Place(
                name = "Putrajaya",
                description = "Malaysia's administrative capital, known for its modern architecture and parks. Features the iconic Putra Mosque and beautiful gardens.",
                category = "Modern Architecture",
                price = 0.0,
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/8/8e/Putrajaya_Mosque.jpg",
                latitude = 2.9264,
                longitude = 101.6964,
                googleMapsUrl = "https://goo.gl/maps/7Q7Q7Q7Q7Q72"
            ),
            Place(
                name = "Taman Negara",
                description = "One of the world's oldest rainforests, great for jungle trekking and wildlife. Home to rare species like the Malayan tiger and Asian elephant.",
                category = "Nature",
                price = 10.0,
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/3/3e/Taman_Negara.jpg",
                latitude = 4.7103,
                longitude = 102.4018,
                googleMapsUrl = "https://goo.gl/maps/8Q8Q8Q8Q8Q82"
            ),
            Place(
                name = "Sasaran Sky Mirror",
                description = "Natural mirror effect on the beach during low tide, perfect for photography. Creates stunning reflections of the sky and clouds.",
                category = "Nature",
                price = 30.0,
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/1/1e/Sasaran_Sky_Mirror.jpg",
                latitude = 3.3422,
                longitude = 101.2552,
                googleMapsUrl = "https://goo.gl/maps/9Q9Q9Q9Q9Q92"
            ),
            Place(
                name = "Redang Island",
                description = "Pristine island with white sandy beaches and crystal-clear waters. Perfect for snorkeling, diving, and beach activities.",
                category = "Nature",
                price = 50.0,
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/2/2e/Redang_Island.jpg",
                latitude = 5.7799,
                longitude = 103.0060,
                googleMapsUrl = "https://goo.gl/maps/0Q0Q0Q0Q0Q02"
            )
        )

        CoroutineScope(Dispatchers.Main).launch {
            // Add the sample places without deleting existing ones
            places.forEach { place ->
                repository.addPlace(place)
            }
            // Reload places after adding sample data
            val updatedPlaces = repository.getPlaces()
            viewModel.updatePlaces(updatedPlaces)
            Toast.makeText(context, "Sample places loaded successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddPlaceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_place, null)
        
        // Setup category dropdown
        val categoryDropdown = dialogView.findViewById<AutoCompleteTextView>(R.id.editTextCategory)
        val categories = arrayOf("Nature", "Historical Site", "Modern Architecture", "Cultural Site")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        categoryDropdown.setAdapter(adapter)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add New Place")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = dialogView.findViewById<TextInputEditText>(R.id.editTextPlaceName).text.toString()
                val description = dialogView.findViewById<TextInputEditText>(R.id.editTextDescription).text.toString()
                val category = dialogView.findViewById<AutoCompleteTextView>(R.id.editTextCategory).text.toString()
                val price = dialogView.findViewById<TextInputEditText>(R.id.editTextPrice).text.toString().toDoubleOrNull() ?: 0.0
                val imageUrl = dialogView.findViewById<TextInputEditText>(R.id.editTextImageUrl).text.toString()
                val latitude = dialogView.findViewById<TextInputEditText>(R.id.editTextLatitude).text.toString().toDoubleOrNull() ?: 0.0
                val longitude = dialogView.findViewById<TextInputEditText>(R.id.editTextLongitude).text.toString().toDoubleOrNull() ?: 0.0
                val googleMapsUrl = dialogView.findViewById<TextInputEditText>(R.id.editTextGoogleMapsUrl).text.toString()

                if (name.isBlank() || description.isBlank() || category.isBlank() || imageUrl.isBlank()) {
                    Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val place = Place(
                    name = name,
                    description = description,
                    category = category,
                    price = price,
                    imageUrl = imageUrl,
                    latitude = latitude,
                    longitude = longitude,
                    googleMapsUrl = googleMapsUrl
                )

                CoroutineScope(Dispatchers.Main).launch {
                    repository.addPlace(place)
                    loadPlaces()
                    Toast.makeText(context, "Place added successfully!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showReviewDialog(place: Place) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Please sign in to review places", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_review, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingbar)
        val reviewEditText = dialogView.findViewById<TextInputEditText>(R.id.editTextReview)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Review ${place.name}")
            .setView(dialogView)
            .setPositiveButton("Submit") { _, _ ->
                val rating = ratingBar.rating.toDouble()
                val reviewText = reviewEditText.text.toString()
                val userId = currentUser.uid
                val userName = currentUser.displayName ?: "Anonymous"
                val placeName = place.name
                val timestamp = dateFormatter.format(calendar.time)
                val date = dateFormatter.format(calendar.time)

                CoroutineScope(Dispatchers.Main).launch {
                    // Update place rating and review count
                    repository.addReview(place.id, rating, reviewText)

                    // Save review to Firestore for review page
                    val review = Review(
                        userId = userId,
                        userName = userName,
                        placeName = placeName,
                        rating = rating.toFloat(),
                        comment = reviewText,
                        timestamp = timestamp,
                        date = date
                    )
                    db.collection("reviews")
                        .add(review)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Review submitted successfully!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error saving review: ${e.message}", Toast.LENGTH_SHORT).show()
                        }

                    loadPlaces()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveToChecklist(place: Place) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Please sign in to save items to your checklist", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a ChecklistItem from the Place data
        val checklistItem = ChecklistItem(
            place = place.name,
            photoUrl = place.imageUrl,
            isChecked = false,
            timestamp = System.currentTimeMillis(),
            date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(java.util.Calendar.getInstance().time),
            budget = place.price,
            hasReview = false
        )

        // Save to Firestore
        db.collection("users")
            .document(currentUser.uid)
            .collection("checklist")
            .add(checklistItem)
            .addOnSuccessListener {
                Toast.makeText(context, "${place.name} added to your checklist!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error saving to checklist: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupCategoryButtons() {
        // Initialize category buttons
        btnAllCategories = binding.btnAllCategories
        btnNature = binding.btnNature
        btnHistoricalSite = binding.btnHistoricalSite
        btnModernArchitecture = binding.btnModernArchitecture
        btnCulturalSite = binding.btnCulturalSite
        btnNearMe = binding.btnNearMe

        // Set click listeners
        btnAllCategories.setOnClickListener { filterByCategory("All") }
        btnNature.setOnClickListener { filterByCategory("Nature") }
        btnHistoricalSite.setOnClickListener { filterByCategory("Historical Site") }
        btnModernArchitecture.setOnClickListener { filterByCategory("Modern Architecture") }
        btnCulturalSite.setOnClickListener { filterByCategory("Cultural Site") }
        btnNearMe.setOnClickListener { filterByNearMe() }
    }

    private fun filterByCategory(category: String) {
        currentCategoryFilter = category
        isNearMeFilterActive = false
        updateCategoryButtonStates()
        applyFilters()
    }

    private fun updateCategoryButtonStates() {
        // Reset all buttons to unselected state
        btnAllCategories.background = ContextCompat.getDrawable(requireContext(), R.drawable.category_button_unselected)
        btnAllCategories.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
        
        btnNature.background = ContextCompat.getDrawable(requireContext(), R.drawable.category_button_unselected)
        btnNature.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
        
        btnHistoricalSite.background = ContextCompat.getDrawable(requireContext(), R.drawable.category_button_unselected)
        btnHistoricalSite.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
        
        btnModernArchitecture.background = ContextCompat.getDrawable(requireContext(), R.drawable.category_button_unselected)
        btnModernArchitecture.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
        
        btnCulturalSite.background = ContextCompat.getDrawable(requireContext(), R.drawable.category_button_unselected)
        btnCulturalSite.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
        
        btnNearMe.background = ContextCompat.getDrawable(requireContext(), R.drawable.category_button_unselected)
        btnNearMe.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))

        // Set the selected button
        if (isNearMeFilterActive) {
            btnNearMe.background = ContextCompat.getDrawable(requireContext(), R.drawable.category_button_selected)
            btnNearMe.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        } else {
            when (currentCategoryFilter) {
                "All" -> {
                    btnAllCategories.background = ContextCompat.getDrawable(requireContext(), R.drawable.category_button_selected)
                    btnAllCategories.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                }
                "Nature" -> {
                    btnNature.background = ContextCompat.getDrawable(requireContext(), R.drawable.category_button_selected)
                    btnNature.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                }
                "Historical Site" -> {
                    btnHistoricalSite.background = ContextCompat.getDrawable(requireContext(), R.drawable.category_button_selected)
                    btnHistoricalSite.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                }
                "Modern Architecture" -> {
                    btnModernArchitecture.background = ContextCompat.getDrawable(requireContext(), R.drawable.category_button_selected)
                    btnModernArchitecture.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                }
                "Cultural Site" -> {
                    btnCulturalSite.background = ContextCompat.getDrawable(requireContext(), R.drawable.category_button_selected)
                    btnCulturalSite.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                }
            }
        }
    }

    private fun filterByNearMe() {
        if (userLatitude == 0.0 && userLongitude == 0.0) {
            Toast.makeText(context, "Location not available. Please enable location services.", Toast.LENGTH_LONG).show()
            return
        }
        isNearMeFilterActive = true
        currentCategoryFilter = "All"
        updateCategoryButtonStates()
        applyFilters()
    }

    private fun setupNearMeMainButton() {
        btnNearMeMain = binding.btnNearMeMain
        btnNearMeMain.setOnClickListener {
            isMainNearMeActive = !isMainNearMeActive
            updateNearMeMainButtonState()
            applyFilters()
        }
        updateNearMeMainButtonState()
    }

    private fun updateNearMeMainButtonState() {
        if (isMainNearMeActive) {
            btnNearMeMain.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
            btnNearMeMain.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        } else {
            btnNearMeMain.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
            btnNearMeMain.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        }
    }

    private fun applyFilters() {
        CoroutineScope(Dispatchers.Main).launch {
            val allPlaces = repository.getPlaces()
            val filteredPlaces = allPlaces.filter { place ->
                val matchesSearch = currentSearchQuery.isEmpty() || 
                    place.name.contains(currentSearchQuery, ignoreCase = true) ||
                    place.description.contains(currentSearchQuery, ignoreCase = true)
                val matchesCategory = currentCategoryFilter == "All" || 
                    place.category == currentCategoryFilter
                val matchesNearMe = !isNearMeFilterActive || 
                    (userLatitude != 0.0 && userLongitude != 0.0 && 
                     calculateDistance(userLatitude, userLongitude, place.latitude, place.longitude) <= 50.0)
                val matchesMainNearMe = !isMainNearMeActive || 
                    (userLatitude != 0.0 && userLongitude != 0.0 && 
                     calculateDistance(userLatitude, userLongitude, place.latitude, place.longitude) <= NEAR_ME_RADIUS_KM)
                matchesSearch && matchesCategory && matchesNearMe && matchesMainNearMe
            }
            viewModel.updatePlaces(filteredPlaces)
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Earth's radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}