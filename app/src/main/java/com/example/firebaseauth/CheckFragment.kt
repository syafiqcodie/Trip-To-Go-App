package com.example.firebaseauth

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.firebaseauth.databinding.DialogAddChecklistItemBinding
import com.example.firebaseauth.databinding.FragmentCheckBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import com.google.firebase.Timestamp

class CheckFragment : Fragment() {
    private var _binding: FragmentCheckBinding? = null
    private val binding get() = _binding!!
    private lateinit var checklistAdapter: ChecklistAdapter
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var selectedImageUri: Uri? = null
    private var editingItem: ChecklistItem? = null
    private val calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                // Update the image preview in the dialog
                dialogBinding?.ivPhotoPreview?.apply {
                    setImageURI(uri)
                    visibility = View.VISIBLE
                }
            }
        }
    }

    private var dialogBinding: DialogAddChecklistItemBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupAddButton()
        loadChecklistItems()
    }

    private fun setupRecyclerView() {
        checklistAdapter = ChecklistAdapter(
            onItemChecked = { item, isChecked ->
                updateItemStatus(item.id, isChecked)
            },
            onItemDeleted = { item ->
                deleteItem(item.id)
            },
            onItemEdit = { item ->
                showEditItemDialog(item)
            },
            onItemReview = { item ->
                showReviewDialog(item)
            }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = checklistAdapter
        }
    }

    private fun setupAddButton() {
        binding.fabAddItem.setOnClickListener {
            showAddItemDialog()
        }
    }

    private fun loadChecklistItems() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Please sign in to view your checklist", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users")
            .document(currentUser.uid)
            .collection("checklist")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(context, "Error loading items: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChecklistItem::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                checklistAdapter.submitList(items)
            }
    }

    private fun updateItemStatus(itemId: String, isChecked: Boolean) {
        val currentUser = auth.currentUser ?: return
        db.collection("users")
            .document(currentUser.uid)
            .collection("checklist")
            .document(itemId)
            .update("isChecked", isChecked)
            .addOnFailureListener {
                Toast.makeText(context, "Error updating item", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteItem(itemId: String) {
        val currentUser = auth.currentUser ?: return
        db.collection("users")
            .document(currentUser.uid)
            .collection("checklist")
            .document(itemId)
            .delete()
            .addOnFailureListener {
                Toast.makeText(context, "Error deleting item", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddItemDialog() {
        editingItem = null
        showItemDialog(null)
    }

    private fun showEditItemDialog(item: ChecklistItem) {
        editingItem = item
        showItemDialog(item)
    }

    private fun showItemDialog(item: ChecklistItem?) {
        dialogBinding = DialogAddChecklistItemBinding.inflate(layoutInflater)
        selectedImageUri = null

        // Set up date picker
        dialogBinding?.etDate?.setOnClickListener {
            showDatePicker()
        }

        // Pre-fill data if editing
        item?.let {
            dialogBinding?.apply {
                etPlace.setText(it.place)
                etDate.setText(it.date)
                etBudget.setText(it.budget.toString())
                it.photoUrl?.let { url ->
                    ivPhotoPreview.apply {
                        Glide.with(this)
                            .load(url)
                            .centerCrop()
                            .into(this)
                        visibility = View.VISIBLE
                    }
                }
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (item == null) "Add New Item" else "Edit Item")
            .setView(dialogBinding?.root)
            .setPositiveButton(if (item == null) "Add" else "Save") { _, _ ->
                val place = dialogBinding?.etPlace?.text?.toString()
                val date = dialogBinding?.etDate?.text?.toString()
                val budget = dialogBinding?.etBudget?.text?.toString()?.toDoubleOrNull() ?: 0.0

                if (!place.isNullOrBlank() && !date.isNullOrBlank()) {
                    if (item == null) {
                        addNewItem(place, date, budget)
                    } else {
                        updateItem(item.id, place, date, budget)
                    }
                } else {
                    Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()



        dialog.show()
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                dialogBinding?.etDate?.setText(dateFormatter.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun addNewItem(place: String, date: String, budget: Double) {
        val currentUser = auth.currentUser ?: return
        selectedImageUri?.let { uri ->
            // Show loading state
            Toast.makeText(context, "Uploading image...", Toast.LENGTH_SHORT).show()
            
            // Create a unique filename
            val filename = "checklist_images/${UUID.randomUUID()}.jpg"
            val imageRef = storage.reference.child("users/${currentUser.uid}/$filename")
            
            // Upload the image
            imageRef.putFile(uri)
                .addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    Toast.makeText(context, "Uploading: $progress%", Toast.LENGTH_SHORT).show()
                }
                .addOnSuccessListener {
                    // Get the download URL
                    imageRef.downloadUrl
                        .addOnSuccessListener { downloadUri ->
                            saveItemToFirestore(place, date, budget, downloadUri.toString())
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error getting download URL: ${e.message}", Toast.LENGTH_SHORT).show()
                            // Save item without photo
                            saveItemToFirestore(place, date, budget, null)
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error uploading image: ${e.message}", Toast.LENGTH_SHORT).show()
                    // Save item without photo
                    saveItemToFirestore(place, date, budget, null)
                }
        } ?: run {
            // Save item without photo
            saveItemToFirestore(place, date, budget, null)
        }
    }

    private fun updateItem(itemId: String, place: String, date: String, budget: Double) {
        val currentUser = auth.currentUser ?: return
        selectedImageUri?.let { uri ->
            // Show loading state
            Toast.makeText(context, "Uploading image...", Toast.LENGTH_SHORT).show()
            
            // Create a unique filename
            val filename = "checklist_images/${UUID.randomUUID()}.jpg"
            val imageRef = storage.reference.child("users/${currentUser.uid}/$filename")
            
            // Upload the image
            imageRef.putFile(uri)
                .addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    Toast.makeText(context, "Uploading: $progress%", Toast.LENGTH_SHORT).show()
                }
                .addOnSuccessListener {
                    // Get the download URL
                    imageRef.downloadUrl
                        .addOnSuccessListener { downloadUri ->
                            updateItemInFirestore(itemId, place, date, budget, downloadUri.toString())
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error getting download URL: ${e.message}", Toast.LENGTH_SHORT).show()
                            // Update without changing photo
                            updateItemInFirestore(itemId, place, date, budget, editingItem?.photoUrl)
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error uploading image: ${e.message}", Toast.LENGTH_SHORT).show()
                    // Update without changing photo
                    updateItemInFirestore(itemId, place, date, budget, editingItem?.photoUrl)
                }
        } ?: run {
            // Update without changing photo
            updateItemInFirestore(itemId, place, date, budget, editingItem?.photoUrl)
        }
    }

    private fun saveItemToFirestore(place: String, date: String, budget: Double, photoUrl: String?) {
        val currentUser = auth.currentUser ?: return
        val item = ChecklistItem(
            place = place,
            photoUrl = photoUrl,
            date = date,
            budget = budget
        )

        db.collection("users")
            .document(currentUser.uid)
            .collection("checklist")
            .add(item)
            .addOnSuccessListener {
                Toast.makeText(context, "Item added successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error adding item", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateItemInFirestore(itemId: String, place: String, date: String, budget: Double, photoUrl: String?) {
        val currentUser = auth.currentUser ?: return
        val updates = hashMapOf<String, Any>(
            "place" to place,
            "date" to date,
            "budget" to budget
        )
        photoUrl?.let { updates["photoUrl"] = it }

        db.collection("users")
            .document(currentUser.uid)
            .collection("checklist")
            .document(itemId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(context, "Item updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error updating item", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showReviewDialog(item: ChecklistItem) {
        val currentUser = auth.currentUser ?: return
        
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_review, null)
        
        val placeNameText = dialogView.findViewById<TextView>(R.id.tvPlaceName)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val commentEditText = dialogView.findViewById<EditText>(R.id.etReviewComment)
        
        placeNameText.text = item.place
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Review")
            .setView(dialogView)
            .setPositiveButton("Submit Review") { _, _ ->
                val rating = ratingBar.rating
                val comment = commentEditText.text.toString().trim()
                
                if (comment.isNotEmpty()) {
                    submitReview(item, rating, comment, currentUser)
                } else {
                    Toast.makeText(context, "Please write a review comment", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitReview(item: ChecklistItem, rating: Float, comment: String, currentUser: com.google.firebase.auth.FirebaseUser) {
        val review = Review(
            userId = currentUser.uid,
            userName = currentUser.displayName ?: "Anonymous",
            placeName = item.place,
            rating = rating,
            comment = comment,
            timestamp = dateFormatter.format(calendar.time),
            date = dateFormatter.format(calendar.time)
        )
        
        // Save review to Firestore
        db.collection("reviews")
            .add(review)
            .addOnSuccessListener { documentReference ->
                // Update checklist item to mark it as reviewed
                db.collection("users")
                    .document(currentUser.uid)
                    .collection("checklist")
                    .document(item.id)
                    .update("hasReview", true)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Review submitted successfully!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Error updating checklist item", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error submitting review: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        dialogBinding = null
    }
} 