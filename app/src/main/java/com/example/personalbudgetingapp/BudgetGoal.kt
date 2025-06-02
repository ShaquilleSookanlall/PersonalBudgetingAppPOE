package com.example.personalbudgetingapp.model

data class BudgetGoal(
    val minGoal: Double = 0.0,
    val maxGoal: Double = 0.0,
    val userId: String = ""
)
