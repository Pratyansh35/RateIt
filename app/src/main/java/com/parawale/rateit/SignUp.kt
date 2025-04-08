package com.parawale.rateit

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.parawale.rateit.Models.UserDetails

class SignUp : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val nameEditText = findViewById<TextInputEditText>(R.id.signup_name_edit_text)
        val mobileEditText = findViewById<TextInputEditText>(R.id.signup_mobile_edit_text)
        val emailEditText = findViewById<TextInputEditText>(R.id.signup_email_edit_text)
        val passwordEditText = findViewById<TextInputEditText>(R.id.signup_password_edit_text)
        val confirmPasswordEditText = findViewById<TextInputEditText>(R.id.signup_cnf_password_edit_text)
        val signUpButton = findViewById<Button>(R.id.signup_signup_btn)
        val loginTextView = findViewById<TextView>(R.id.signup_login_text_view)
        val errorTextView = findViewById<TextView>(R.id.signup_error_text_view)
        val policySwitch = findViewById<SwitchMaterial>(R.id.signup_policy_switch)


        // Navigate to LoginActivity if user already has account
        loginTextView.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        signUpButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val mobile = mobileEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            val acceptedPolicy = policySwitch.isChecked

            errorTextView.text = ""

            if (name.isEmpty() || mobile.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                errorTextView.text = "Please fill all fields."
                return@setOnClickListener
            }

            if (!acceptedPolicy) {
                errorTextView.text = "Please accept the terms and conditions."
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                errorTextView.text = "Passwords do not match."
                return@setOnClickListener
            }

            // Create Firebase user
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid ?: return@addOnCompleteListener
                        val userMap = hashMapOf(
                            "name" to name,
                            "mobile" to mobile,
                            "email" to email
                        )
                        firestore.collection("users").document(uid)
                            .set(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Sign up successful!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, HomeActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                errorTextView.text = "Error saving user: ${e.localizedMessage}"
                            }
                    } else {
                        errorTextView.text = "Sign up failed: ${task.exception?.localizedMessage}"
                    }
                }
        }
    }
}
