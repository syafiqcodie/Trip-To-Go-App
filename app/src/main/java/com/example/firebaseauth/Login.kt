package com.example.firebaseauth

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.firebaseauth.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class Login : AppCompatActivity() {

    private lateinit var emailInput:    EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginBtn: Button
    private lateinit var registerText: TextView
    private lateinit var forgotPasswordText: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login) // or your layout filename

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Find views
        emailInput    = findViewById(R.id.email)
        passwordInput = findViewById(R.id.password)
        loginBtn      = findViewById(R.id.login_btn)
        registerText  = findViewById(R.id.register_btn)
        forgotPasswordText = findViewById(R.id.forgot_password_btn)
        progressBar   = findViewById(R.id.progressBar)

        // If user taps "Register", go to Register screen
        registerText.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
            finish()
        }

        // If user taps "Forgot Password", show forgot password dialog
        forgotPasswordText.setOnClickListener {
            showForgotPasswordDialog()
        }

        loginBtn.setOnClickListener {
            val email    = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            // Input validation
            if (email.isEmpty()) {
                emailInput.error = "Enter email"
                emailInput.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                passwordInput.error = "Enter password"
                passwordInput.requestFocus()
                return@setOnClickListener
            }
            if (password.length < 6) {
                passwordInput.error = "Password must be at least 6 characters"
                passwordInput.requestFocus()
                return@setOnClickListener
            }

            // Show progress
            progressBar.visibility = View.VISIBLE

            // Sign in with Firebase
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    progressBar.visibility = View.GONE
                    if (task.isSuccessful) {
                        // Login success, navigate to Home/Main screen
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        // Login failed
                        Toast.makeText(
                            this,
                            "Authentication failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    private fun showForgotPasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password, null)
        val emailEditText = dialogView.findViewById<EditText>(R.id.etForgotPasswordEmail)

        // Pre-fill with email if user has already entered it
        emailEditText.setText(emailInput.text.toString())

        MaterialAlertDialogBuilder(this)
            .setTitle("Reset Password")
            .setView(dialogView)
            .setPositiveButton("Send Reset Link") { _, _ ->
                val email = emailEditText.text.toString().trim()
                if (email.isNotEmpty()) {
                    sendPasswordResetEmail(email)
                } else {
                    Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendPasswordResetEmail(email: String) {
        progressBar.visibility = View.VISIBLE
        
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Password reset email sent to $email",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Failed to send reset email: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}
