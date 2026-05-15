package com.example.firebaseauth

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.firebaseauth.databinding.ItemChecklistBinding
import java.text.NumberFormat
import java.util.Locale

class ChecklistAdapter(
    private val onItemChecked: (ChecklistItem, Boolean) -> Unit,
    private val onItemDeleted: (ChecklistItem) -> Unit,
    private val onItemEdit: (ChecklistItem) -> Unit,
    private val onItemReview: (ChecklistItem) -> Unit
) : ListAdapter<ChecklistItem, ChecklistAdapter.ViewHolder>(ChecklistDiffCallback()) {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("ms", "MY"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChecklistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemChecklistBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChecklistItem) {
            binding.apply {
                tvPlace.text = item.place
                tvDate.text = item.date
                
                // Format budget with RM currency
                tvBudget.text = if (item.budget > 0) {
                    "Budget: RM ${String.format("%.2f", item.budget)}"
                } else {
                    "Budget: Free"
                }
                
                checkbox.isChecked = item.isChecked

                // Update review button appearance based on whether review exists
                btnReview.setImageResource(
                    if (item.hasReview) android.R.drawable.btn_star_big_on
                    else android.R.drawable.btn_star_big_off
                )

                // Load image if available
                item.photoUrl?.let { url ->
                    Glide.with(ivPhoto)
                        .load(url)
                        .centerCrop()
                        .into(ivPhoto)
                    ivPhoto.visibility = android.view.View.VISIBLE
                } ?: run {
                    ivPhoto.visibility = android.view.View.GONE
                }

                // Set up click listeners
                checkbox.setOnCheckedChangeListener { _, isChecked ->
                    onItemChecked(item, isChecked)
                }

                btnDelete.setOnClickListener {
                    onItemDeleted(item)
                }

                btnEdit.setOnClickListener {
                    onItemEdit(item)
                }

                btnReview.setOnClickListener {
                    onItemReview(item)
                }
            }
        }
    }

    private class ChecklistDiffCallback : DiffUtil.ItemCallback<ChecklistItem>() {
        override fun areItemsTheSame(oldItem: ChecklistItem, newItem: ChecklistItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChecklistItem, newItem: ChecklistItem): Boolean {
            return oldItem == newItem
        }
    }
} 