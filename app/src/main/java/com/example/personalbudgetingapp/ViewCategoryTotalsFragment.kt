package com.example.personalbudgetingapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.personalbudgetingapp.data.FirebaseService
import com.example.personalbudgetingapp.databinding.FragmentViewCategoryTotalsBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ViewCategoryTotalsFragment : Fragment() {

    private var _binding: FragmentViewCategoryTotalsBinding? = null
    private val binding get() = _binding!!
    private val firebaseService = FirebaseService()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewCategoryTotalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvCategoryTotals.layoutManager = LinearLayoutManager(context)

        // Load all totals on start
        loadCategoryTotals("0000-01-01", "9999-12-31")

        binding.etStartDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                binding.etStartDate.setText(String.format("%04d-%02d-%02d", year, month + 1, day))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.etEndDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                binding.etEndDate.setText(String.format("%04d-%02d-%02d", year, month + 1, day))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnFilterTotals.setOnClickListener {
            val startDate = binding.etStartDate.text.toString().trim()
            val endDate = binding.etEndDate.text.toString().trim()
            val finalStart = if (startDate.isEmpty()) "0000-01-01" else startDate
            val finalEnd = if (endDate.isEmpty()) "9999-12-31" else endDate
            loadCategoryTotals(finalStart, finalEnd)
        }
    }

    private fun loadCategoryTotals(start: String, end: String) {
        CoroutineScope(Dispatchers.IO).launch {
            firebaseService.getAllCategories { categories ->
                firebaseService.getUserExpenses { expenses ->
                    val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(start) ?: Date(0)
                    val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(end) ?: Date()

                    val totals = categories.map { category ->
                        val total = expenses
                            .filter { it.category == category.name }
                            .filter { it.date.after(startDate) && it.date.before(Date(endDate.time + 86400000)) }
                            .sumOf { it.amount }
                        category to total
                    }

                    activity?.runOnUiThread {
                        binding.rvCategoryTotals.adapter = CategoryTotalAdapter(totals)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
