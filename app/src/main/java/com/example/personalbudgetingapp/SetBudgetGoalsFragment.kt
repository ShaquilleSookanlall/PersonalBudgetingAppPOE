package com.example.personalbudgetingapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.personalbudgetingapp.data.FirebaseService
import com.example.personalbudgetingapp.databinding.FragmentSetBudgetGoalsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SetBudgetGoalsFragment : Fragment() {

    private var _binding: FragmentSetBudgetGoalsBinding? = null
    private val binding get() = _binding!!
    private val firebaseService = FirebaseService()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView called")
        _binding = FragmentSetBudgetGoalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called")

        // Load existing budget goals from Firebase
        firebaseService.getBudgetGoal { budgetGoal ->
            if (budgetGoal != null) {
                Log.d(TAG, "Loaded budget goals: min=${budgetGoal.minGoal}, max=${budgetGoal.maxGoal}")
                binding.etMinGoal.setText(budgetGoal.minGoal.toString())
                binding.etMaxGoal.setText(budgetGoal.maxGoal.toString())
            } else {
                Log.d(TAG, "No budget goals found for user")
            }
        }

        binding.btnSaveGoals.setOnClickListener {
            Log.d(TAG, "Save Goals button clicked")
            val minGoal = binding.etMinGoal.text.toString().toDoubleOrNull()
            val maxGoal = binding.etMaxGoal.text.toString().toDoubleOrNull()

            if (minGoal == null || maxGoal == null) {
                Toast.makeText(context, "Please enter valid numbers for goals", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (minGoal <= 0 || maxGoal <= 0) {
                Toast.makeText(context, "Goals must be positive numbers", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (minGoal >= maxGoal) {
                Toast.makeText(context, "Maximum goal must be greater than minimum goal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                Log.d(TAG, "Saving goals to Firebase: min=$minGoal, max=$maxGoal")
                firebaseService.uploadBudgetGoal(minGoal, maxGoal) { success ->
                    CoroutineScope(Dispatchers.Main).launch {
                        if (success) {
                            Toast.makeText(context, "Budget goals saved!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to save budget goals", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called")
        _binding = null
    }

    companion object {
        private const val TAG = "SetBudgetGoalsFragment"
    }
}
