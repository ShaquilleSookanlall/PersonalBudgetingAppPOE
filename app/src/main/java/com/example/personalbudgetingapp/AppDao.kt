package com.example.personalbudgetingapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert

@Dao
interface AppDao {

    // Expense Entries
    @Insert
    suspend fun insertExpenseEntry(entry: ExpenseEntry)

    @Delete
    suspend fun deleteExpenseEntry(entry: ExpenseEntry)

    @Update
    suspend fun updateExpenseEntry(entry: ExpenseEntry)

    @Query("SELECT * FROM expense_entries")
    suspend fun getAllExpenseEntries(): List<ExpenseEntry>

    @Query("SELECT * FROM expense_entries WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getEntriesInPeriod(startDate: String, endDate: String): List<ExpenseEntry>

    // Categories
    @Insert
    suspend fun insertCategory(category: Category)

    @Query("SELECT * FROM categories WHERE deleted = 0")
    suspend fun getAllCategories(): List<Category>

    @Query("UPDATE categories SET deleted = 1 WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM categories WHERE name = :name AND deleted = 1)")
    suspend fun isCategoryDeletedByName(name: String): Boolean

    @Query("SELECT SUM(amount) FROM expense_entries WHERE categoryId = :categoryId AND date BETWEEN :startDate AND :endDate AND userId = :userId")
    suspend fun getTotalForCategory(categoryId: Int, startDate: String, endDate: String, userId: String): Double?

    // Budget Goals
    @Upsert
    suspend fun setBudgetGoal(budgetGoal: BudgetGoal)

    suspend fun setBudgetGoal(minGoal: Double, maxGoal: Double) {
        val budgetGoal = BudgetGoal(id = 1, minGoal = minGoal, maxGoal = maxGoal)
        setBudgetGoal(budgetGoal)
    }

    @Query("SELECT * FROM budget_goals WHERE id = 1")
    suspend fun getBudgetGoal(): BudgetGoal?

    @Query("SELECT * FROM expense_entries WHERE userId = :userId")
    suspend fun getAllEntriesForUser(userId: String): List<ExpenseEntry>

}
