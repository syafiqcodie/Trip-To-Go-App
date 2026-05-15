package com.example.firebaseauth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ReviewsFragment : Fragment() {

    private lateinit var searchEditText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var reviewsAdapter: ReviewsAdapter
    private lateinit var noReviewsText: TextView
    
    private val db = FirebaseFirestore.getInstance()
    private var allReviews = listOf<Review>()
    private var groupedReviews = mapOf<String, List<Review>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_reviews, container, false)

        // Initialize views
        searchEditText = view.findViewById(R.id.etSearchReviews)
        recyclerView = view.findViewById(R.id.rvReviews)
        noReviewsText = view.findViewById(R.id.tvNoReviews)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        reviewsAdapter = ReviewsAdapter()
        recyclerView.adapter = reviewsAdapter

        // Load reviews
        loadReviews()

        return view
    }

    private fun loadReviews() {
        db.collection("reviews")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                val reviews = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val id = doc.id
                    val userId = data["userId"] as? String ?: ""
                    val userName = data["userName"] as? String ?: ""
                    val placeName = data["placeName"] as? String ?: ""
                    val rating = (data["rating"] as? Number)?.toFloat() ?: 0.0f
                    val comment = data["comment"] as? String ?: ""
                    val date = data["date"] as? String ?: ""
                    val timestampString = when (val ts = data["timestamp"]) {
                        is String -> ts
                        is com.google.firebase.Timestamp -> ts.toDate().toString()
                        is Number -> java.util.Date(ts.toLong()).toString()
                        else -> ""
                    }
                    Review(
                        id = id,
                        userId = userId,
                        userName = userName,
                        placeName = placeName,
                        rating = rating,
                        comment = comment,
                        timestamp = timestampString,
                        date = date
                    )
                } ?: emptyList()

                allReviews = reviews
                groupedReviews = reviews.groupBy { it.placeName }
                updateReviewsList(groupedReviews)
            }
    }

    private fun updateReviewsList(grouped: Map<String, List<Review>>) {
        if (grouped.isEmpty()) {
            noReviewsText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            noReviewsText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            reviewsAdapter.setAllGroupedReviews(grouped)
        }
    }
} 