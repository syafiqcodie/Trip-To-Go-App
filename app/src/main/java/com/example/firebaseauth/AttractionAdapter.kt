package com.example.firebaseauth

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.firebaseauth.R

class AttractionAdapter(
    private val items: List<Attraction>,
    private val onSaveToChecklist: (Attraction) -> Unit
) : RecyclerView.Adapter<AttractionAdapter.ViewHolder>() {

    private val expandedItems = mutableSetOf<Int>()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageView = view.findViewById<ImageView>(R.id.ivAttraction)
        private val nameTxt = view.findViewById<TextView>(R.id.tvName)
        private val descTxt = view.findViewById<TextView>(R.id.tvDescription)
        private val categoryTxt = view.findViewById<TextView>(R.id.tvCategory)
        private val priceTxt = view.findViewById<TextView>(R.id.tvPrice)
        private val distanceTxt = view.findViewById<TextView>(R.id.tvDistance)
        private val dropdownArrow = view.findViewById<ImageView>(R.id.ivDropdownArrow)
        private val dropdownContent = view.findViewById<LinearLayout>(R.id.llDropdownContent)
        private val saveButton = view.findViewById<Button>(R.id.btnSaveToChecklist)
        private val locateButton = view.findViewById<Button>(R.id.btnLocate)

        fun bind(attraction: Attraction, position: Int) {
            imageView.setImageResource(attraction.imageRes)
            nameTxt.text = attraction.name
            descTxt.text = attraction.description
            categoryTxt.text = attraction.category
            
            // Format price with RM currency
            priceTxt.text = if (attraction.price > 0) {
                "Price: RM ${String.format("%.2f", attraction.price)}"
            } else {
                "Price: Free"
            }
            
            // Display distance
            if (attraction.distance > 0) {
                distanceTxt.text = "Distance: ${String.format("%.1f", attraction.distance)} km"
                distanceTxt.visibility = View.VISIBLE
            } else {
                distanceTxt.visibility = View.GONE
            }

            // Set click listener for the entire item to toggle dropdown
            itemView.setOnClickListener {
                toggleDropdown(position)
            }

            // Set click listener for save button
            saveButton.setOnClickListener {
                onSaveToChecklist(attraction)
                Toast.makeText(itemView.context, "${attraction.name} added to checklist!", Toast.LENGTH_SHORT).show()
            }

            // Set click listener for locate button
            locateButton.setOnClickListener {
                openInGoogleMaps(attraction)
            }

            // Update dropdown state
            updateDropdownState(position)
        }

        private fun toggleDropdown(position: Int) {
            if (expandedItems.contains(position)) {
                expandedItems.remove(position)
            } else {
                expandedItems.add(position)
            }
            updateDropdownState(position)
        }

        private fun updateDropdownState(position: Int) {
            val isExpanded = expandedItems.contains(position)
            
            if (isExpanded) {
                dropdownContent.visibility = View.VISIBLE
                dropdownArrow.setImageResource(android.R.drawable.arrow_up_float)
            } else {
                dropdownContent.visibility = View.GONE
                dropdownArrow.setImageResource(android.R.drawable.arrow_down_float)
            }
        }

        private fun openInGoogleMaps(attraction: Attraction) {
            try {
                // Create URI for Google Maps
                val gmmIntentUri = Uri.parse("geo:${attraction.latitude},${attraction.longitude}?q=${attraction.latitude},${attraction.longitude}(${attraction.name})")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                
                // Check if Google Maps is available
                if (mapIntent.resolveActivity(itemView.context.packageManager) != null) {
                    itemView.context.startActivity(mapIntent)
                } else {
                    // Fallback to web browser if Google Maps app is not installed
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${attraction.latitude},${attraction.longitude}"))
                    itemView.context.startActivity(webIntent)
                }
            } catch (e: Exception) {
                Toast.makeText(itemView.context, "Unable to open maps", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attraction, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }
}
