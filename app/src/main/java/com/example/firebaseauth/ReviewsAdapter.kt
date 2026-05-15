package com.example.firebaseauth

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.firebaseauth.databinding.ItemReviewBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class ReviewListItem {
    data class PlaceGroup(val placeName: String, var expanded: Boolean = false) : ReviewListItem()
    data class ReviewItem(val review: Review) : ReviewListItem()
}

class ReviewsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val items = mutableListOf<ReviewListItem>()

    fun setGroupedReviews(grouped: Map<String, List<Review>>) {
        items.clear()
        for ((place, reviews) in grouped) {
            items.add(ReviewListItem.PlaceGroup(place, false))
            // Initially collapsed, so don't add reviews yet
        }
        notifyDataSetChanged()
    }

    fun expandGroup(position: Int, reviews: List<Review>) {
        val group = items[position] as? ReviewListItem.PlaceGroup ?: return
        if (!group.expanded) {
            items.addAll(position + 1, reviews.map { ReviewListItem.ReviewItem(it) })
            group.expanded = true
            notifyItemRangeInserted(position + 1, reviews.size)
            notifyItemChanged(position)
        }
    }

    fun collapseGroup(position: Int, reviewsCount: Int) {
        val group = items[position] as? ReviewListItem.PlaceGroup ?: return
        if (group.expanded) {
            repeat(reviewsCount) { items.removeAt(position + 1) }
            group.expanded = false
            notifyItemRangeRemoved(position + 1, reviewsCount)
            notifyItemChanged(position)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ReviewListItem.PlaceGroup -> 0
            is ReviewListItem.ReviewItem -> 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_place_group, parent, false)
            PlaceGroupViewHolder(view)
        } else {
            val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ReviewViewHolder(binding)
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ReviewListItem.PlaceGroup -> (holder as PlaceGroupViewHolder).bind(item, position)
            is ReviewListItem.ReviewItem -> (holder as ReviewViewHolder).bind(item.review)
        }
    }

    inner class PlaceGroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val placeNameText: TextView = view.findViewById(R.id.tvPlaceGroupName)
        private val arrow: ImageView = view.findViewById(R.id.ivDropdownArrow)
        fun bind(group: ReviewListItem.PlaceGroup, position: Int) {
            placeNameText.text = group.placeName
            arrow.rotation = if (group.expanded) 180f else 0f
            itemView.setOnClickListener {
                val reviews = getReviewsForPlace(group.placeName)
                if (group.expanded) {
                    collapseGroup(position, reviews.size)
                } else {
                    expandGroup(position, reviews)
                }
            }
        }
    }

    inner class ReviewViewHolder(private val binding: ItemReviewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(review: Review) {
            binding.apply {
                tvPlaceName.text = review.placeName
                tvUserName.text = review.userName
                tvReviewComment.text = review.comment
                ratingBar.rating = review.rating
                tvReviewDate.text = review.timestamp ?: ""
            }
        }
    }

    // Helper to get reviews for a place (used for expand/collapse)
    private var allGroupedReviews: Map<String, List<Review>> = emptyMap()
    fun setAllGroupedReviews(grouped: Map<String, List<Review>>) {
        allGroupedReviews = grouped
        setGroupedReviews(grouped)
    }
    private fun getReviewsForPlace(placeName: String): List<Review> {
        return allGroupedReviews[placeName] ?: emptyList()
    }
} 