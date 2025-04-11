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

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<TextInputEditText>(R.id.login_email_edit_text)
        val passwordEditText = findViewById<TextInputEditText>(R.id.login_password_edit_text)
        val loginButton = findViewById<Button>(R.id.login_login_btn)
        val signUpTextView = findViewById<TextView>(R.id.login_signup_text_view)
        val errorTextView = findViewById<TextView>(R.id.login_error_text_view)
        val rememberSwitch = findViewById<SwitchMaterial>(R.id.login_rem_switch)

        signUpTextView.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                errorTextView.text = "Please enter email and password."
                errorTextView.visibility = TextView.VISIBLE
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password) //Use email for login instead of email.
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Login successful.", Toast.LENGTH_SHORT).show()

                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        errorTextView.text = "Login failed: ${task.exception?.message}"
                        errorTextView.visibility = TextView.VISIBLE
                    }
                }
        }
    }
}