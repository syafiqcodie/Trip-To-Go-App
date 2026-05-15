package com.example.firebaseauth

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class Register : AppCompatActivity() {


    private lateinit var emailInput:    EditText
    private lateinit var passwordInput: EditText
    private lateinit var registerBtn:   Button
    private lateinit var loginText:     TextView
    private lateinit var progressBar:   ProgressBar

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()



        emailInput    = findViewById(R.id.email)
        passwordInput = findViewById(R.id.password)
        registerBtn   = findViewById(R.id.reg_btn)
        loginText     = findViewById(R.id.login_btn)
        progressBar   = findViewById(R.id.progressBar)


        loginText.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            finish()
        }

        registerBtn.setOnClickListener {

            val email    = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            // validation...
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

            progressBar.visibility = View.VISIBLE

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    progressBar.visibility = View.GONE
                    if (task.isSuccessful) {
                        // update display name...
                        val user = auth.currentUser
                        val profileUpdates = UserProfileChangeRequest.Builder()

                            .build()
                        user?.updateProfile(profileUpdates)
                            ?.addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    Toast.makeText(
                                        this,
                                        "Account created successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Profile update failed.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    } else {
                        Toast.makeText(
                            this,
                            "Authentication failed: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }
}
