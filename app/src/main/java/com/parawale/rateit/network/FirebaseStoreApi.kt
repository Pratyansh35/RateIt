package com.parawale.rateit.network

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.parawale.rateit.Models.Product
import com.parawale.rateit.Models.Rating
import com.parawale.rateit.Models.Review
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resumeWithException

object FirebaseStoreApi {

    private val database = FirebaseDatabase.getInstance().getReference("rateit")
    private val storage = FirebaseStorage.getInstance().reference

    suspend fun fetchProducts(): List<Product> = suspendCancellableCoroutine { continuation ->
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val productList = mutableListOf<Product>()
                for (productSnapshot in snapshot.children) {
                    val product = productSnapshot.getValue(Product::class.java)
                    if (product != null) {
                        productList.add(product)
                    }
                }
                continuation.resume(productList, null)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseStoreApi", "Database Error: ${error.message}")
                continuation.resume(emptyList(), null)
            }
        })
    }

    suspend fun submitReview(productKey: String, newReview: Review) {
        try {
            val reviewRef = database.child(productKey).child("reviews").push()
            reviewRef.setValue(newReview).await()

            // Also update the product's rating (average and count)
            val ratingRef = database.child(productKey).child("rating")
            ratingRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val rating = currentData.getValue(Rating::class.java) ?: Rating(0.0, 0)
                    val newCount = rating.count + 1
                    val newAvg = ((rating.rate * rating.count) + newReview.rating) / newCount
                    currentData.value = Rating(newAvg, newCount)
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?
                ) {
                    if (error != null) {
                        Log.e("Firebase", "Rating update failed: ${error.message}")
                    } else {
                        Log.d("Firebase", "Rating updated successfully")
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("Firebase", "submitReview failed: ${e.localizedMessage}")
        }
    }


    suspend fun uploadProductData(
        title: String,
        price: Double,
        description: String,
        category: String,
        link: String,
        rating: Double,
        productImageUris: List<Uri>,
        invoiceUri: Uri?,
        context: Context
    ): Product? = withContext(Dispatchers.IO) {
        try {
            val productImageUrls = productImageUris.map { uploadImage(it, "product_images", context) }
            val invoiceUrl = invoiceUri?.let { uploadImage(it, "invoices", context) }

            val currentUser = FirebaseAuth.getInstance().currentUser
            val createdBy = currentUser?.email ?: currentUser?.phoneNumber ?: currentUser?.uid ?: "Unknown"
            val userId = currentUser?.uid ?: "unknown_user"
            val reviewId = database.push().key ?: UUID.randomUUID().toString()

            val firstReview = Review(
                id = reviewId,
                userId = userId,
                comment =
                ,
                rating = rating.toFloat()
            )

            val productId = (0..999999).random()

            val product = Product(
                id = productId,
                title = title,
                price = price,
                description = description,
                category = category,
                productImages = productImageUrls.ifEmpty { listOf(link) },
                rating = Rating(rate = rating, count = 1),
                invoiceImage = invoiceUrl,
                createdBy = createdBy,
                dateCreated = System.currentTimeMillis(),
                reviews = mapOf(firstReview.id to firstReview)

            )

            val ref = database.child(productId.toString())
            ref.setValue(product).await()
            product
        } catch (e: Exception) {
            Log.e("FirebaseStoreApi", "Upload failed: ${e.localizedMessage}")
            null
        }
    }




    private suspend fun uploadImage(uri: Uri, folder: String, context: Context): String =
        suspendCancellableCoroutine { continuation ->
            val imageRef = storage.child("$folder/${UUID.randomUUID()}")
            imageRef.putFile(uri)
                .continueWithTask { imageRef.downloadUrl }
                .addOnSuccessListener { uri ->
                    continuation.resume(uri.toString(), null)
                }
                .addOnFailureListener {
                    continuation.resumeWithException(it)
                }
        }
}

