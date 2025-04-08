package com.parawale.rateit

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.parawale.rateit.Models.Product

class RatingActivity : AppCompatActivity() {

    private lateinit var tvProductName: TextView
    private lateinit var tvProductPrice: TextView
    private lateinit var tvProductDescription: TextView
    private lateinit var tvUserPhone: TextView
    private lateinit var ratingBar: RatingBar
    private lateinit var etReview: EditText
    private lateinit var btnSubmitRating: Button
    private lateinit var imageContainer: LinearLayout
    private lateinit var tvReviewLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rating)

        // View bindings
        tvProductName = findViewById(R.id.tvProductName)
        tvProductPrice = findViewById(R.id.tvProductPrice)
        tvProductDescription = findViewById(R.id.tvProductDescription)
        tvUserPhone = findViewById(R.id.tvUserPhone)
        ratingBar = findViewById(R.id.ratingBar)
        etReview = findViewById(R.id.etReview)
        btnSubmitRating = findViewById(R.id.btnSubmitRating)
        imageContainer = findViewById(R.id.productImageContainer)
        tvReviewLabel = findViewById(R.id.tvReviewLabel)

        // Get data from intent
        val productName = intent.getStringExtra("productName") ?: "N/A"
        val productPrice = intent.getStringExtra("productPrice") ?: "₹0.00"
        val productDescription = intent.getStringExtra("productDescription") ?: ""
        val userEmailFromIntent = intent.getStringExtra("userEmail") ?: "Unknown"
        val imageUrls = intent.getStringArrayListExtra("imageUrls") ?: arrayListOf()

        // Set product details
        tvProductName.text = productName
        tvProductPrice.text = productPrice
        tvProductDescription.text = productDescription
        tvUserPhone.text = "Posted by: $userEmailFromIntent"

        // Load product images horizontally
        for (url in imageUrls) {
            val imageView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(300, 300).apply {
                    setMargins(12, 0, 12, 0)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                contentDescription = "Product image"
            }
            Glide.with(this)
                .load(url)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(imageView)
            imageContainer.addView(imageView)
        }

        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: "Unknown"

        if (userEmailFromIntent == currentUserEmail) {
            // User is the owner
            ratingBar.isEnabled = false
            etReview.isEnabled = false
            etReview.hint = "You can't review your own product"
            etReview.setText("This is your product listing.")
            tvReviewLabel.text = "Reviewed By You"
            btnSubmitRating.text = "Your Review"
            btnSubmitRating.isEnabled = false
        } else {
            // Submit rating
            tvReviewLabel.text = "Submit your review"
            btnSubmitRating.setOnClickListener {
                val rating = ratingBar.rating
                val review = etReview.text.toString().trim()

                if (review.isEmpty()) {
                    Toast.makeText(this, "Please write a review", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // TODO: Save rating and review to Firebase/Backend
                Toast.makeText(this, "Thanks for rating $productName!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}

