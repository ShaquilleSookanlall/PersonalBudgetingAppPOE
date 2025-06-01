package com.example.personalbudgetingapp.model

import java.util.*

data class Expense(
    val amount: Double,
    val category: String,
    val description: String,
    val date: Date
)
