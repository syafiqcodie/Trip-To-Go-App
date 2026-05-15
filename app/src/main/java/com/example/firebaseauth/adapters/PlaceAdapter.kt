package com.example.firebaseauth.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.firebaseauth.databinding.ItemPlaceBinding
import com.example.firebaseauth.models.Place
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.*

class PlaceAdapter(
    private val onNavigateClick: (Place) -> Unit,
    private val onAddToChecklist: (Place) -> Unit,
    private val onReviewClick: (Place) -> Unit,
    private var userLatitude: Double = 0.0,
    private var userLongitude: Double = 0.0
) : ListAdapter<Place, PlaceAdapter.PlaceViewHolder>(PlaceDiffCallback()) {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("ms", "MY"))
    private var expandedPosition = RecyclerView.NO_POSITION

    fun updateUserLocation(lat: Double, lon: Double) {
        userLatitude = lat
        userLongitude = lon
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val binding = ItemPlaceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        holder.bind(getItem(position), position == expandedPosition)
        holder.itemView.setOnClickListener {
            toggleDropdown(position)
        }
        holder.binding.buttonExpand.setOnClickListener {
            toggleDropdown(position)
        }
    }

    private fun toggleDropdown(position: Int) {
        val previousExpanded = expandedPosition
        expandedPosition = if (expandedPosition == position) RecyclerView.NO_POSITION else position
        notifyItemChanged(previousExpanded)
        notifyItemChanged(position)
    }

    inner class PlaceViewHolder(
        val binding: ItemPlaceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(place: Place, expanded: Boolean) {
            binding.textViewPlaceName.text = place.name
            // Dropdown content
            binding.dropdownContent.visibility = if (expanded) View.VISIBLE else View.GONE
            binding.buttonExpand.rotation = if (expanded) 180f else 0f

            binding.textViewDescription.text = place.description
            binding.textViewCategory.text = place.category
            binding.textViewPrice.text = if (place.price > 0) {
                "RM ${String.format("%.2f", place.price)}"
            } else {
                "Free"
            }
            if (userLatitude != 0.0 && userLongitude != 0.0) {
                val distance = calculateDistance(userLatitude, userLongitude, place.latitude, place.longitude)
                binding.textViewRating.text = "Rating: ${place.rating} (${place.reviewCount} reviews) • ${String.format("%.1f", distance)} km away"
            } else {
                binding.textViewRating.text = "Rating: ${place.rating} (${place.reviewCount} reviews)"
            }
            Glide.with(binding.imageViewPlace)
                .load(place.imageUrl)
                .centerCrop()
                .into(binding.imageViewPlace)
            binding.buttonNavigate.setOnClickListener {
                onNavigateClick(place)
            }
            binding.buttonAddToChecklist.setOnClickListener {
                onAddToChecklist(place)
            }
            binding.buttonReview.setOnClickListener {
                onReviewClick(place)
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
    }

    private class PlaceDiffCallback : DiffUtil.ItemCallback<Place>() {
        override fun areItemsTheSame(oldItem: Place, newItem: Place): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Place, newItem: Place): Boolean {
            return oldItem == newItem
        }
    }
} 