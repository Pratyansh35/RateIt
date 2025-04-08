package com.parawale.rateit.network

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.parawale.rateit.Models.Product
import com.parawale.rateit.Models.Rating
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

            val product = Product(
                id = (0..999999).random(),
                title = title,
                price = price,
                description = description,
                category = category,
                productImages = productImageUrls.ifEmpty { listOf(link) },
                rating = Rating(rate = rating, count = 1),
                invoiceImage = invoiceUrl,
                createdBy = createdBy,
                dateCreated = System.currentTimeMillis()
            )

            val ref = database.push()
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

