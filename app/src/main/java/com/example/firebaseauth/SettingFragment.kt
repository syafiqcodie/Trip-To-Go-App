package com.example.firebaseauth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class SettingFragment : Fragment() {
    
    private lateinit var profileImageView: ImageView
    private lateinit var profileNameTextView: TextView
    private lateinit var profileEmailTextView: TextView
    private lateinit var editProfileButton: Button
    private lateinit var changePasswordButton: Button
    private lateinit var signOutButton: Button
    
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var selectedImageUri: Uri? = null
    private lateinit var profilePictureManager: ProfilePictureManager

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                // Save image locally immediately when selected
                profilePictureManager.saveProfilePictureLocally(uri, profileImageView)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_setting, container, false)
        
        // Initialize ProfilePictureManager
        profilePictureManager = ProfilePictureManager(requireContext())
        
        // Initialize views
        profileImageView = view.findViewById(R.id.ivProfilePicture)
        profileNameTextView = view.findViewById(R.id.tvProfileName)
        profileEmailTextView = view.findViewById(R.id.tvProfileEmail)
        editProfileButton = view.findViewById(R.id.btnEditProfile)
        changePasswordButton = view.findViewById(R.id.btnChangePassword)
        signOutButton = view.findViewById(R.id.btnSignOut)
        
        setupProfile()
        setupClickListeners()
        
        return view
    }

    private fun setupProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Set email
            profileEmailTextView.text = currentUser.email
            
            // Set display name
            val displayName = currentUser.displayName ?: "User"
            profileNameTextView.text = displayName
            
            // Load profile picture using ProfilePictureManager
            profilePictureManager.loadProfilePicture(profileImageView)
        }
    }

    private fun setupClickListeners() {
        // Profile picture click to change
        profileImageView.setOnClickListener {
            selectProfilePicture()
        }
        
        // Edit profile button
        editProfileButton.setOnClickListener {
            showEditProfileDialog()
        }
        
        // Change password button
        changePasswordButton.setOnClickListener {
            showChangePasswordDialog()
        }
        
        // Sign out button
        signOutButton.setOnClickListener {
            showSignOutConfirmation()
        }
    }

    private fun selectProfilePicture() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getContent.launch(intent)
    }

    private fun showEditProfileDialog() {
        val currentUser = auth.currentUser ?: return
        
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_profile, null)
        
        val nameEditText = dialogView.findViewById<EditText>(R.id.etProfileName)
        nameEditText.setText(currentUser.displayName ?: "")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newName = nameEditText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    updateProfile(newName)
                } else {
                    Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateProfile(newName: String) {
        val currentUser = auth.currentUser ?: return
        
        // Update display name
        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
            .setDisplayName(newName)
            .build()
        
        currentUser.updateProfile(profileUpdates)
            .addOnSuccessListener {
                profileNameTextView.text = newName
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                
                // Update profile picture if selected
                selectedImageUri?.let { uri ->
                    uploadProfilePicture(uri)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadProfilePicture(imageUri: Uri) {
        val currentUser = auth.currentUser ?: return
        
        val filename = "profile_pictures/${currentUser.uid}_${UUID.randomUUID()}.jpg"
        val imageRef = storage.reference.child(filename)
        
        Toast.makeText(context, "Uploading profile picture...", Toast.LENGTH_SHORT).show()
        
        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    // Update profile picture URL
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setPhotoUri(downloadUri)
                        .build()
                    
                    currentUser.updateProfile(profileUpdates)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to update profile picture: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_change_password, null)
        
        val currentPasswordEditText = dialogView.findViewById<EditText>(R.id.etCurrentPassword)
        val newPasswordEditText = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val confirmPasswordEditText = dialogView.findViewById<EditText>(R.id.etConfirmNewPassword)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Change Password") { _, _ ->
                val currentPassword = currentPasswordEditText.text.toString()
                val newPassword = newPasswordEditText.text.toString()
                val confirmPassword = confirmPasswordEditText.text.toString()
                
                if (currentPassword.isNotEmpty() && newPassword.isNotEmpty() && confirmPassword.isNotEmpty()) {
                    if (newPassword == confirmPassword) {
                        if (newPassword.length >= 6) {
                            changePassword(currentPassword, newPassword)
                        } else {
                            Toast.makeText(context, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "New passwords do not match", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        val currentUser = auth.currentUser ?: return
        
        // First, re-authenticate the user
        val credential = com.google.firebase.auth.EmailAuthProvider
            .getCredential(currentUser.email!!, currentPassword)
        
        currentUser.reauthenticate(credential)
            .addOnSuccessListener {
                // Now change the password
                currentUser.updatePassword(newPassword)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Password changed successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to change password: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Current password is incorrect: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showSignOutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Sign Out") { _, _ ->
                signOut()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun signOut() {
        // Clear local profile picture when signing out
        profilePictureManager.clearLocalProfilePicture()
        
        auth.signOut()
        val intent = Intent(requireContext(), Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}