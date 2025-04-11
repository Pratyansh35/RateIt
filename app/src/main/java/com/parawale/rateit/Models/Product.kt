package com.parawale.rateit.Models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Product(
    val id: Int = 0,
    val title: String = "",
    val price: Double = 0.0,
    val description: String = "",
    val category: String = "",
    val genuine: Boolean = false,
    val productImages: List<String> = emptyList(),
    val rating: Rating = Rating( 0.0, 0),
    var invoiceImage: String? = null,
    var createdBy: String? = null,
    val dateCreated: Long = System.currentTimeMillis(),
    val reviews: Map<String, Review> = emptyMap()
) : Parcelable



