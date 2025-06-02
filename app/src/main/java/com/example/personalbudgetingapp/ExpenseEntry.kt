package com.example.personalbudgetingapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ExpenseEntry(
    val id: Int = 0,                       // Local-only; not needed in Firebase
    var firebaseId: String? = null,        // Optional: Store Firestore doc ID
    val date: String,                      // Format: yyyy-MM-dd
    val description: String,
    val categoryId: Int,                   // Matches your internal Category list
    val amount: Double,
    val photoUri: String? = null,          // Local file URI
    val userId: String                     // For filtering user-specific data
) : Parcelable
