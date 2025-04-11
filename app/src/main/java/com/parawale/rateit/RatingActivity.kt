package com.parawale.rateit

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.parawale.rateit.Models.Review
import kotlinx.coroutines.launch

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
    private lateinit var reviewsContainer: LinearLayout
    private lateinit var reviewFormContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rating)

        // Initialize Views
        tvProductName = findViewById(R.id.tvProductName)
        tvProductPrice = findViewById(R.id.tvProductPrice)
        tvProductDescription = findViewById(R.id.tvProductDescription)
        tvUserPhone = findViewById(R.id.tvUserPhone)
        ratingBar = findViewById(R.id.ratingBar)
        etReview = findViewById(R.id.etReview)
        btnSubmitRating = findViewById(R.id.btnSubmitRating)
        imageContainer = findViewById(R.id.productImageContainer)
        tvReviewLabel = findViewById(R.id.tvReviewLabel)
        reviewsContainer = findViewById(R.id.reviewsContainer)
        reviewFormContainer = findViewById(R.id.reviewFormContainer)

        // Get product data from Intent
        val productName = intent.getStringExtra("productName") ?: "N/A"
        val productPrice = intent.getDoubleExtra("productPrice", 0.0)
        val productDescription = intent.getStringExtra("productDescription") ?: ""
        val userEmailFromIntent = intent.getStringExtra("userEmail") ?: "Unknown"
        val imageUrls = intent.getStringArrayListExtra("imageUrls") ?: arrayListOf()
        val productKey = intent.getIntExtra("productId", 0).toString()

        // Set product info
        tvProductName.text = productName
        tvProductPrice.text = "₹%.2f".format(productPrice)
        tvProductDescription.text = productDescription
        tvUserPhone.text = "Posted by: $userEmailFromIntent"

        // Display images
        imageUrls.forEach { url ->
            val imageView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(300, 300).apply {
                    setMargins(12, 0, 12, 0)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            Glide.with(this)
                .load(url)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(imageView)

            imageContainer.addView(imageView)
        }

        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: "Unknown"

        if (userEmailFromIntent == currentUserEmail) {
            disableRatingForOwner()
        } else {
            enableRatingSubmission(productName, productKey)
        }

        // Load all previous reviews
        loadAllReviews(productKey)
    }

    private fun disableRatingForOwner() {
        ratingBar.isEnabled = false
        etReview.isEnabled = false
        etReview.setText("This is your product listing.")
        etReview.hint = "You can't review your own product"
        tvReviewLabel.text = "Reviewed By You"
        btnSubmitRating.text = "Your Review"
        btnSubmitRating.isEnabled = false
    }

    private fun enableRatingSubmission(productName: String, productId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserEmail = currentUser?.email ?: return

        val reviewRef = FirebaseDatabase.getInstance().getReference("rateit/$productId/reviews")

        reviewRef.orderByChild("reviewBy").equalTo(currentUserEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // User already reviewed
                        for (reviewSnap in snapshot.children) {
                            val review = reviewSnap.getValue(Review::class.java)
                            showAlreadySubmittedReview(review)
                            break
                        }
                    } else {
                        // User can submit review
                        reviewFormContainer.visibility = View.VISIBLE
                        btnSubmitRating.setOnClickListener {
                            val rating = ratingBar.rating
                            val reviewText = etReview.text.toString().trim()

                            if (reviewText.isEmpty()) {
                                Toast.makeText(this@RatingActivity, "Please write a review", Toast.LENGTH_SHORT).show()
                                return@setOnClickListener
                            }

                            val reviewObj = Review(
                                reviewBy = currentUserEmail,
                                comment = reviewText,
                                rating = rating,
                                date = System.currentTimeMillis()
                            )

                            lifecycleScope.launch {
                                submitReview(productId, reviewObj)
                                Toast.makeText(
                                    this@RatingActivity,
                                    "Thanks for rating ${productName.take(10)}!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@RatingActivity, "Failed to check existing review", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showAlreadySubmittedReview(review: Review?) {
        reviewFormContainer.visibility = View.GONE
        val reviewTextView = TextView(this).apply {
            text = "\"${review?.comment}\" - ${review?.reviewBy}"
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(0, 24, 0, 8)
        }

        val ratingBar = RatingBar(this).apply {
            numStars = 5
            stepSize = 0.5f
            rating = review?.rating ?: 0f
        }

        reviewsContainer.addView(TextView(this).apply {
            text = "Your Review"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            setPadding(0, 24, 0, 0)
        }, 0)

        reviewsContainer.addView(reviewTextView, 1)
        reviewsContainer.addView(ratingBar, 2)
    }


    private fun submitReview(productId: String, review: Review) {
        val ref = FirebaseDatabase.getInstance().getReference("rateit")
        val reviewId = ref.child(productId).child("reviews").push().key

        reviewId?.let {
            val reviewWithId = review.copy(id = it)
            ref.child(productId).child("reviews").child(it).setValue(reviewWithId)
        }
    }


    private fun loadAllReviews(productId: String) {
        val ref = FirebaseDatabase.getInstance().getReference("rateit")

        ref.child(productId).child("reviews")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    reviewsContainer.removeAllViews()

                    for (reviewSnap in snapshot.children) {
                        val review = reviewSnap.getValue(Review::class.java)
                        review?.let {
                            val reviewView = layoutInflater.inflate(
                                R.layout.item_review,
                                reviewsContainer,
                                false
                            )

                            reviewView.findViewById<TextView>(R.id.tvReviewerEmail).text = it.reviewBy
                            reviewView.findViewById<RatingBar>(R.id.ratingBarSmall).rating =
                                it.rating
                            reviewView.findViewById<TextView>(R.id.tvReviewText).text = it.comment

                            reviewsContainer.addView(reviewView)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("RatingActivity", "Failed to load reviews: ${error.message}")
                }
            }
            )
    }
}


