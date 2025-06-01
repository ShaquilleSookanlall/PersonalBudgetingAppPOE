package com.example.personalbudgetingapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var barChart: BarChart
    private lateinit var tvBudgetSummary: TextView
    private lateinit var dbLocal: AppDatabase

    private val dbCloud = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        barChart = view.findViewById(R.id.barChart)
        tvBudgetSummary = view.findViewById(R.id.tvBudgetSummary)
        dbLocal = AppDatabase.getDatabase(requireContext())

        fetchExpensesFromFirebase()
        return view
    }

    private fun fetchExpensesFromFirebase() {
        if (userId == null) return

        val oneMonthAgo = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
        }.time

        dbCloud.collection("expenses")
            .document(userId)
            .collection("user_expense_entries")
            .whereGreaterThan("date", oneMonthAgo)
            .get()
            .addOnSuccessListener { result ->
                val categoryTotals = mutableMapOf<String, Double>()
                var totalSpent = 0.0

                for (doc in result) {
                    val category = doc.getString("category") ?: "Unknown"
                    val amount = doc.getDouble("amount") ?: 0.0
                    categoryTotals[category] =
                        categoryTotals.getOrDefault(category, 0.0) + amount
                    totalSpent += amount
                }

                CoroutineScope(Dispatchers.Main).launch {
                    showBarChart(categoryTotals)
                    loadAndShowBudgetSummary(totalSpent)
                }
            }
            .addOnFailureListener {
                tvBudgetSummary.text = "⚠️ Failed to load expense data from Firebase."
            }
    }

    private fun loadAndShowBudgetSummary(totalSpent: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            val budgetGoal = dbLocal.appDao().getBudgetGoal()
            withContext(Dispatchers.Main) {
                if (budgetGoal != null) {
                    val min = budgetGoal.minGoal
                    val max = budgetGoal.maxGoal
                    val status = when {
                        totalSpent < min -> "🟡 Below Minimum Goal"
                        totalSpent > max -> "🔴 Over Budget"
                        else -> "🟢 Within Budget"
                    }

                    val summary = """
                        🔢 Min Goal: R${"%.2f".format(min)}
                        🔢 Max Goal: R${"%.2f".format(max)}
                        💰 Total Spent: R${"%.2f".format(totalSpent)}
                        📊 Status: $status
                    """.trimIndent()

                    tvBudgetSummary.text = summary
                } else {
                    tvBudgetSummary.text = "⚠️ Set your budget goals to enable progress tracking."
                }
            }
        }
    }

    private fun showBarChart(categorySums: Map<String, Double>) {
        val entries = mutableListOf<BarEntry>()
        val labels = categorySums.keys.toList()

        categorySums.entries.forEachIndexed { index, entry ->
            entries.add(BarEntry(index.toFloat(), entry.value.toFloat()))
        }

        val dataSet = BarDataSet(entries, "Spending by Category")
        dataSet.color = Color.parseColor("#4CAF50")
        val data = BarData(dataSet)
        data.barWidth = 0.9f

        barChart.data = data
        barChart.setFitBars(true)
        barChart.description.isEnabled = false

        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.labelRotationAngle = -45f

        val yAxis = barChart.axisLeft
        yAxis.setDrawGridLines(false)
        yAxis.removeAllLimitLines()

        // Add goal lines (we'll load real values via RoomDB summary)
        val defaultMin = 200f
        val defaultMax = 1000f

        val minLine = LimitLine(defaultMin, "Min Goal")
        minLine.lineColor = Color.BLUE
        minLine.lineWidth = 2f

        val maxLine = LimitLine(defaultMax, "Max Goal")
        maxLine.lineColor = Color.RED
        maxLine.lineWidth = 2f

        yAxis.addLimitLine(minLine)
        yAxis.addLimitLine(maxLine)

        barChart.axisRight.isEnabled = false
        barChart.invalidate()
    }
}
