package com.example.personalbudgetingapp.data

import android.net.Uri
import android.util.Log
import com.example.personalbudgetingapp.Category
import com.example.personalbudgetingapp.ExpenseEntry
import com.example.personalbudgetingapp.model.BudgetGoal
import com.example.personalbudgetingapp.model.Expense
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class FirebaseService {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUserId(): String? = auth.currentUser?.uid

    // -------------------- EXPENSE FUNCTIONALITY --------------------

    fun uploadExpense(expense: Expense, onComplete: (Boolean) -> Unit) {
        val userId = getUserId()
        if (userId == null) {
            onComplete(false)
            return
        }

        val expenseMap = hashMapOf(
            "amount" to expense.amount,
            "category" to expense.category,
            "description" to expense.description,
            "date" to Timestamp(expense.date),
            "photoUrl" to expense.photoUrl
        )

        db.collection("expenses")
            .document(userId)
            .collection("user_expense_entries")
            .add(expenseMap)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun uploadExpenseWithImage(expense: Expense, imageUri: Uri?, onComplete: (Boolean) -> Unit) {
        val userId = getUserId()
        if (userId == null) {
            onComplete(false)
            return
        }

        if (imageUri != null) {
            val fileName = "expense_photos/${userId}_${System.currentTimeMillis()}.jpg"
            val storageRef = FirebaseStorage.getInstance().reference.child(fileName)

            storageRef.putFile(imageUri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) task.exception?.let { throw it }
                    storageRef.downloadUrl
                }
                .addOnSuccessListener { uri ->
                    val updated = expense.copy(photoUrl = uri.toString())
                    uploadExpense(updated, onComplete)
                }
                .addOnFailureListener { onComplete(false) }
        } else {
            uploadExpense(expense, onComplete)
        }
    }

    fun getUserExpenses(onResult: (List<Expense>) -> Unit) {
        val userId = getUserId() ?: return onResult(emptyList())

        db.collection("expenses")
            .document(userId)
            .collection("user_expense_entries")
            .get()
            .addOnSuccessListener { result ->
                val expenses = result.documents.mapNotNull { doc ->
                    try {
                        Expense(
                            amount = doc.getDouble("amount") ?: 0.0,
                            category = doc.getString("category") ?: "Unknown",
                            description = doc.getString("description") ?: "",
                            date = doc.getTimestamp("date")?.toDate() ?: Date(),
                            photoUrl = doc.getString("photoUrl")
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                onResult(expenses)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun getUserEntries(userId: String, onResult: (List<ExpenseEntry>) -> Unit) {
        db.collection("expenses")
            .document(userId)
            .collection("user_expense_entries")
            .get()
            .addOnSuccessListener { result ->
                val entries = result.documents.mapNotNull { doc ->
                    try {
                        ExpenseEntry(
                            description = doc.getString("description") ?: "",
                            amount = doc.getDouble("amount") ?: 0.0,
                            date = doc.getTimestamp("date")?.toDate()?.let {
                                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
                            } ?: "",
                            categoryId = doc.getString("category")?.toIntOrNull() ?: -1,  // ✅ FIXED
                            photoUri = doc.getString("photoUrl"),
                            userId = userId
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                onResult(entries)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    fun deleteExpense(entry: ExpenseEntry, onComplete: (Boolean) -> Unit) {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(entry.date)
        val timestamp = date?.let { Timestamp(it) } ?: return onComplete(false)

        db.collection("expenses")
            .document(entry.userId)
            .collection("user_expense_entries")
            .whereEqualTo("description", entry.description)
            .whereEqualTo("amount", entry.amount)
            .whereEqualTo("date", timestamp)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) doc.reference.delete()
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    // -------------------- BUDGET GOAL FUNCTIONALITY --------------------

    fun uploadBudgetGoal(minGoal: Double, maxGoal: Double, onComplete: (Boolean) -> Unit) {
        val userId = getUserId() ?: return onComplete(false)
        val goal = BudgetGoal(minGoal, maxGoal, userId)

        db.collection("budget_goals")
            .document(userId)
            .set(goal)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun getBudgetGoal(onResult: (BudgetGoal?) -> Unit) {
        val userId = getUserId() ?: return onResult(null)

        db.collection("budget_goals")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                onResult(doc.toObject(BudgetGoal::class.java))
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    // -------------------- CATEGORY FUNCTIONALITY --------------------

    fun uploadCategory(category: Category, onComplete: (Boolean) -> Unit) {
        val userId = getUserId() ?: return onComplete(false)
        val docId = category.id.ifEmpty { category.name }

        db.collection("categories")
            .document(userId)
            .collection("user_categories")
            .document(docId)
            .set(category, SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun getAllCategories(onResult: (List<Category>) -> Unit) {
        val userId = getUserId() ?: return onResult(emptyList())

        db.collection("categories")
            .document(userId)
            .collection("user_categories")
            .whereEqualTo("deleted", false)
            .get()
            .addOnSuccessListener { result ->
                val categories = result.documents.mapNotNull {
                    it.toObject(Category::class.java)?.copy(id = it.id)
                }
                onResult(categories)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun deleteCategoryById(categoryId: String, onComplete: (Boolean) -> Unit) {
        val userId = getUserId() ?: return onComplete(false)

        db.collection("categories")
            .document(userId)
            .collection("user_categories")
            .document(categoryId)
            .update("deleted", true)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}
