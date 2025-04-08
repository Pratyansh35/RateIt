package com.parawale.rateit.Models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable  // ✅ Required for serialization
data class Rating(
    val rate: Double = 0.0,
    val count: Int = 0
) : Parcelable