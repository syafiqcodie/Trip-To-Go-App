package com.example.firebaseauth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ProfilePictureManager(private val context: Context) {
    
    private val auth = FirebaseAuth.getInstance()
    
    fun saveProfilePictureLocally(imageUri: Uri, imageView: ImageView) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            
            // Create a circular bitmap
            val circularBitmap = createCircularBitmap(bitmap)
            
            // Save to local file
            val file = getLocalProfilePictureFile()
            val outputStream = FileOutputStream(file)
            circularBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.close()
            
            // Update the ImageView
            imageView.setImageBitmap(circularBitmap)
            
        } catch (e: IOException) {
            Toast.makeText(context, "Failed to save image locally: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    fun loadProfilePicture(imageView: ImageView) {
        val currentUser = auth.currentUser ?: return
        
        // Try to load local profile picture first
        val localProfileFile = getLocalProfilePictureFile()
        if (localProfileFile.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(localProfileFile.absolutePath)
                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                loadProfilePictureFromFirebase(currentUser, imageView)
            }
        } else {
            // Load from Firebase if no local image
            loadProfilePictureFromFirebase(currentUser, imageView)
        }
    }
    
    private fun loadProfilePictureFromFirebase(currentUser: com.google.firebase.auth.FirebaseUser, imageView: ImageView) {
        currentUser.photoUrl?.let { photoUrl ->
            Glide.with(context)
                .load(photoUrl)
                .circleCrop()
                .placeholder(R.drawable.default_profile)
                .into(imageView)
        } ?: run {
            // Set default profile picture
            imageView.setImageResource(R.drawable.default_profile)
        }
    }
    
    private fun createCircularBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val size = minOf(width, height)
        
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        val paint = android.graphics.Paint()
        val rect = android.graphics.Rect(0, 0, size, size)
        
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = android.graphics.Color.parseColor("#BAB399")
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, null, rect, paint)
        
        return output
    }
    
    private fun getLocalProfilePictureFile(): File {
        val currentUser = auth.currentUser
        val fileName = "profile_picture_${currentUser?.uid ?: "default"}.jpg"
        return File(context.filesDir, fileName)
    }
    
    fun clearLocalProfilePicture() {
        val file = getLocalProfilePictureFile()
        if (file.exists()) {
            file.delete()
        }
    }
    
    fun hasLocalProfilePicture(): Boolean {
        return getLocalProfilePictureFile().exists()
    }
} 