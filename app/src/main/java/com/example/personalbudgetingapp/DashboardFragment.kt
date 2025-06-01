package com.example.personalbudgetingapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var barChart: BarChart
    private lateinit var db: AppDatabase

    private val minGoal = 200.0
    private val maxGoal = 1000.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        barChart = view.findViewById(R.id.barChart)
        db = AppDatabase.getDatabase(requireContext())

        fetchLocalExpensesAndShowGraph()
        return view
    }

    private fun fetchLocalExpensesAndShowGraph() {
        CoroutineScope(Dispatchers.IO).launch {
            val allExpenses = db.appDao().getAllExpenseEntries()
            val oneMonthAgo = Calendar.getInstance().apply {
                add(Calendar.MONTH, -1)
            }.time

            // Filter only entries from the past month
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val recentExpenses = allExpenses.filter {
                try {
                    val expenseDate = format.parse(it.date)
                    expenseDate != null && expenseDate.after(oneMonthAgo)
                } catch (e: Exception) {
                    false
                }
            }

            // Group and sum by category
            val categoryTotals = mutableMapOf<String, Double>()
            val categories = db.appDao().getAllCategories().associateBy { it.id }

            for (entry in recentExpenses) {
                val categoryName = categories[entry.categoryId]?.name ?: "Unknown"
                categoryTotals[categoryName] =
                    categoryTotals.getOrDefault(categoryName, 0.0) + entry.amount
            }

            requireActivity().runOnUiThread {
                showBarChart(categoryTotals)
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

        val minLine = LimitLine(minGoal.toFloat(), "Min Goal")
        minLine.lineColor = Color.BLUE
        minLine.lineWidth = 2f

        val maxLine = LimitLine(maxGoal.toFloat(), "Max Goal")
        maxLine.lineColor = Color.RED
        maxLine.lineWidth = 2f

        yAxis.removeAllLimitLines()
        yAxis.addLimitLine(minLine)
        yAxis.addLimitLine(maxLine)

        barChart.axisRight.isEnabled = false
        barChart.invalidate()
    }
}
