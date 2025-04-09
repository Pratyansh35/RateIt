package com.parawale.rateit.Models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Review(
    val id: String = "",
    val userId: String = "",
    val comment: String = "",
    val rating: Float = 0f,
    val date: Long = System.currentTimeMillis()
) : Parcelable
