package com.example.personalbudgetingapp.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class FirebaseService {

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    fun uploadExpense(
        amount: Double,
        category: String,
        description: String,
        dateString: String,
        onComplete: (Boolean) -> Unit
    ) {
        if (userId == null) {
            onComplete(false)
            return
        }

        val date: Date? = try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
        } catch (e: Exception) {
            null
        }

        if (date == null) {
            onComplete(false)
            return
        }

        val expenseMap = hashMapOf(
            "amount" to amount,
            "category" to category,
            "description" to description,
            "date" to Timestamp(date),
            "userId" to userId
        )

        db.collection("expenses")
            .add(expenseMap)
            .addOnSuccessListener {
                Log.d("Firebase", "Expense synced successfully")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Sync failed", e)
                onComplete(false)
            }
    }
}
