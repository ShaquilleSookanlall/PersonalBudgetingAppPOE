package com.example.personalbudgetingapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.personalbudgetingapp.data.FirebaseService
import com.example.personalbudgetingapp.databinding.ActivityDashboardBinding
import com.google.firebase.auth.FirebaseAuth


class DashboardActivity : BaseActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var auth: FirebaseAuth
    private val firebaseService = FirebaseService()

    private val presetCategories = listOf(
        "Food", "Transport", "Utilities", "Health", "Education",
        "Entertainment", "Clothing", "Personal Care", "Miscellaneous"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")

        binding = DataBindingUtil.setContentView(this, R.layout.activity_dashboard)
        auth = FirebaseAuth.getInstance()

        // ✅ Display user email
        val userEmail = auth.currentUser?.email ?: "Guest"
        binding.tvUserEmail.text = "Welcome, $userEmail"

        // ✅ Load budget goal from Firebase
        firebaseService.getBudgetGoal { goal ->
            goal?.let {
                val display = "Min: R%.2f, Max: R%.2f".format(it.minGoal, it.maxGoal)
                binding.tvBudgetAmount.text = display
            } ?: run {
                binding.tvBudgetAmount.text = "⚠️ No budget goal set"
            }
        }

        // ✅ Seed default categories to Firebase
        seedDefaultCategories()

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

        // ✅ Floating action button to create entry
        binding.fabCreateEntry.setOnClickListener {
            loadFragment(CreateEntryFragment())
        }

        // ✅ Bottom navigation handling
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
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }

        // ✅ CSV Export placeholder (you can implement Firebase data download here)
        binding.btnExportCSV.setOnClickListener {
            Toast.makeText(this, "Export not yet implemented with Firebase", Toast.LENGTH_SHORT).show()
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

    private fun seedDefaultCategories() {
        firebaseService.getAllCategories { existing ->
            val existingNames = existing.map { it.name }
            val newCategories = presetCategories.filterNot { existingNames.contains(it) }

            newCategories.forEach { name ->
                firebaseService.uploadCategory(Category(name = name)) { success ->
                    if (success) Log.d(TAG, "Seeded category: $name")
                    else Log.e(TAG, "Failed to seed category: $name")
                }
            }
        }
    }

    companion object {
        private const val TAG = "DashboardActivity"
    }
}
