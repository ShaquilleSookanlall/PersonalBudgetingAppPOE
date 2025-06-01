package com.example.personalbudgetingapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.personalbudgetingapp.databinding.ActivityDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class DashboardActivity : BaseActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: AppDatabase

    private val presetCategories = listOf(
        "Food", "Transport", "Utilities", "Health", "Education",
        "Entertainment", "Clothing", "Personal Care", "Miscellaneous"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")

        binding = DataBindingUtil.setContentView(this, R.layout.activity_dashboard)
        db = AppDatabase.getDatabase(this)
        ensureDefaultCategories()

        auth = FirebaseAuth.getInstance()

        // ✅ Show user email dynamically
        val userEmail = auth.currentUser?.email ?: "Guest"
        binding.tvUserEmail.text = "Welcome, $userEmail"

        // ✅ Load and show user's budget goal
        CoroutineScope(Dispatchers.IO).launch {
            val goal = db.appDao().getBudgetGoal()
            goal?.let {
                val display = "Min: R%.2f, Max: R%.2f".format(it.minGoal, it.maxGoal)
                runOnUiThread {
                    binding.tvBudgetAmount.text = display
                }
            }
        }

        // ✅ Card navigation
        binding.cardSetBudgetGoals.setOnClickListener {
            loadFragment(SetBudgetGoalsFragment())
        }

        binding.cardViewEntries.setOnClickListener {
            loadFragment(ViewEntriesFragment())
        }

        binding.cardViewCategoryTotals.setOnClickListener {
            loadFragment(ViewCategoryTotalsFragment())
        }

        // ✅ FAB for creating new entries
        binding.fabCreateEntry.setOnClickListener {
            loadFragment(CreateEntryFragment())
        }

        // ✅ Bottom navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_month -> {
                    binding.fragmentContainer.visibility = View.GONE
                    binding.scrollView.visibility = View.VISIBLE
                    binding.tvUserEmail.visibility = View.VISIBLE
                    binding.fabCreateEntry.visibility = View.VISIBLE
                    true
                }
                R.id.nav_categories -> {
                    loadFragment(ViewCategoriesFragment())
                    true
                }
                R.id.nav_dashboard -> {
                    loadFragment(DashboardFragment()) // ✅ Load graph-based dashboard
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }

        // ✅ Export to CSV
        binding.btnExportCSV.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val userId = auth.currentUser?.uid ?: return@launch
                val entries = db.appDao().getAllEntriesForUser(userId)

                if (entries.isEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@DashboardActivity, "No entries to export", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val csvBuilder = StringBuilder()
                csvBuilder.append("Date,Description,Amount,Category,PhotoUri\n")

                val categories = db.appDao().getAllCategories().associateBy { it.id }

                for (entry in entries) {
                    val categoryName = categories[entry.categoryId]?.name ?: "Unknown"
                    csvBuilder.append("${entry.date},\"${entry.description}\",${entry.amount},$categoryName,${entry.photoUri ?: ""}\n")
                }

                val fileName = "expense_export_${System.currentTimeMillis()}.csv"
                val file = File(getExternalFilesDir(null), fileName)
                file.writeText(csvBuilder.toString())

                runOnUiThread {
                    Toast.makeText(this@DashboardActivity, "Exported to ${file.absolutePath}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        binding.scrollView.visibility = View.GONE
        binding.tvUserEmail.visibility = View.GONE
        binding.fabCreateEntry.visibility = View.GONE

        binding.fragmentContainer.visibility = View.VISIBLE

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun ensureDefaultCategories() {
        CoroutineScope(Dispatchers.IO).launch {
            val existingCategories = db.appDao().getAllCategories()
            val existingNames = existingCategories.map { it.name }

            for (categoryName in presetCategories) {
                if (existingNames.contains(categoryName)) continue

                val isDeleted = db.appDao().isCategoryDeletedByName(categoryName)
                if (!isDeleted) {
                    db.appDao().insertCategory(Category(name = categoryName))
                }
            }
        }
    }

    companion object {
        private const val TAG = "DashboardActivity"
    }
}
