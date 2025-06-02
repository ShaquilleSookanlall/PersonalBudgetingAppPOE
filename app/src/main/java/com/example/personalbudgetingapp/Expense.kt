package com.example.personalbudgetingapp.model

import java.util.Date

data class Expense(
    val amount: Double = 0.0,
    val category: String = "Unknown", // Already a string
    val description: String = "",
    val date: Date = Date(),
    val photoUrl: String? = null
)

