package com.example.personalbudgetingapp

data class Category(
    val id: String = "",              // Firebase document ID
    val name: String = "",
    val deleted: Boolean = false
)
