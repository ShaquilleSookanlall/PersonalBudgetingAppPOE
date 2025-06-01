package com.example.personalbudgetingapp.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.personalbudgetingapp.model.Expense
import java.util.*

class FirebaseService {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun uploadExpense(expense: Expense, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Log.e("Firebase", "User not authenticated.")
            onComplete(false)
            return
        }

        val date = Timestamp(expense.date)

        val expenseMap = hashMapOf(
            "amount" to expense.amount,
            "category" to expense.category,
            "description" to expense.description,
            "date" to date
        )

        // ✅ Save to subcollection: expenses/{userId}/user_expense_entries/{auto_id}
        db.collection("expenses")
            .document(userId)
            .collection("user_expense_entries")
            .add(expenseMap)
            .addOnSuccessListener {
                Log.d("Firebase", "Expense uploaded successfully")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Upload failed", e)
                onComplete(false)
            }
    }
}
