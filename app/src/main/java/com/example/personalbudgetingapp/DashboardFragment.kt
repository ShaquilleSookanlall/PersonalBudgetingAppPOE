package com.example.personalbudgetingapp

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.personalbudgetingapp.data.FirebaseService
import com.github.mikephil.charting.charts.*
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var barChart: BarChart
    private lateinit var pieChart: PieChart
    private lateinit var lineChart: LineChart
    private lateinit var tvBudgetSummary: TextView
    private lateinit var btnSelectDateRange: Button

    private val dbCloud = FirebaseFirestore.getInstance()
    private val firebaseService = FirebaseService()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private val chartColors = listOf(
        Color.parseColor("#4CAF50"),
        Color.parseColor("#2196F3"),
        Color.parseColor("#FFC107"),
        Color.parseColor("#E91E63"),
        Color.parseColor("#9C27B0"),
        Color.parseColor("#FF5722")
    )

    private var startDate: Date? = null
    private var endDate: Date? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        barChart = view.findViewById(R.id.barChart)
        pieChart = view.findViewById(R.id.pieChart)
        lineChart = view.findViewById(R.id.lineChart)
        tvBudgetSummary = view.findViewById(R.id.tvBudgetSummary)
        btnSelectDateRange = view.findViewById(R.id.btnSelectDateRange)

        btnSelectDateRange.setOnClickListener {
            openDateRangePicker()
        }

        fetchExpensesFromFirebase()
        return view
    }

    private fun openDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Date Range")
            .setTheme(R.style.CustomDatePickerTheme)
            .build()

        picker.show(parentFragmentManager, picker.toString())

        picker.addOnPositiveButtonClickListener { selection ->
            startDate = Date(selection.first ?: 0)
            endDate = Date(selection.second ?: 0)
            fetchExpensesFromFirebase()
        }
    }

    private fun fetchExpensesFromFirebase() {
        if (userId == null) return

        val baseQuery = dbCloud.collection("expenses").document(userId).collection("user_expense_entries")
        var query: com.google.firebase.firestore.Query = baseQuery

        startDate?.let {
            query = query.whereGreaterThanOrEqualTo("date", it)
        }

        endDate?.let {
            query = query.whereLessThanOrEqualTo("date", it)
        }

        query.get().addOnSuccessListener { result ->
            if (result.isEmpty) {
                tvBudgetSummary.text = "📭 No entries in this date range."
                barChart.clear()
                pieChart.clear()
                lineChart.clear()
                return@addOnSuccessListener
            }

            val categoryTotals = mutableMapOf<String, Double>()
            val spendingOverTime = mutableListOf<Pair<Date, Double>>()
            var totalSpent = 0.0

            for (doc in result) {
                val category = doc.getString("category") ?: "Unknown"
                val amount = doc.getDouble("amount") ?: 0.0
                val date = doc.getDate("date") ?: Date()

                categoryTotals[category] = categoryTotals.getOrDefault(category, 0.0) + amount
                spendingOverTime.add(date to amount)
                totalSpent += amount
            }

            showBarChart(categoryTotals)
            showPieChart(categoryTotals, totalSpent)
            showLineChart(spendingOverTime)
            loadAndShowBudgetSummary(totalSpent)
        }.addOnFailureListener {
            tvBudgetSummary.text = "⚠️ Failed to load expense data from Firebase."
        }
    }

    private fun loadAndShowBudgetSummary(totalSpent: Double) {
        firebaseService.getBudgetGoal { budgetGoal ->
            if (budgetGoal != null) {
                val min = budgetGoal.minGoal
                val max = budgetGoal.maxGoal
                val status = when {
                    totalSpent < min -> "Below Minimum Goal"
                    totalSpent > max -> "Over Budget"
                    else -> "Within Budget"
                }

                val summary = """
Minimum Goal:         R${"%.2f".format(min)}
Maximum Limit:        R${"%.2f".format(max)}
Total Expenditure:    R${"%.2f".format(totalSpent)}
Budget Status:        $status
""".trimIndent()

                tvBudgetSummary.text = summary
                updateLimitLines(min.toFloat(), max.toFloat())
            } else {
                tvBudgetSummary.text = "⚠️ Set your budget goals to enable progress tracking."
                updateLimitLines(200f, 1000f)
            }
        }
    }

    private fun showBarChart(categorySums: Map<String, Double>) {
        val entries = categorySums.entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value.toFloat())
        }

        val labels = categorySums.keys.toList()
        val dataSet = BarDataSet(entries, "Spending by Category")
        dataSet.colors = chartColors.shuffled()
        val data = BarData(dataSet)
        data.barWidth = 0.9f

        barChart.data = data
        barChart.setFitBars(true)
        barChart.description.isEnabled = false

        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.labelRotationAngle = -45f
        xAxis.granularity = 1f
        xAxis.labelCount = labels.size
        xAxis.setDrawGridLines(false)

        barChart.axisLeft.setDrawGridLines(false)
        barChart.axisRight.isEnabled = false
        barChart.legend.isEnabled = true
        barChart.invalidate()
    }

    private fun showPieChart(categorySums: Map<String, Double>, total: Double) {
        val pieEntries = categorySums.map { (category, amount) ->
            PieEntry((amount / total * 100).toFloat(), category)
        }

        val dataSet = PieDataSet(pieEntries, "")
        dataSet.colors = chartColors.shuffled()
        dataSet.valueTextSize = 12f
        dataSet.sliceSpace = 2f

        val pieData = PieData(dataSet)
        pieData.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format("%.1f%%", value)
            }
        })

        pieChart.data = pieData
        pieChart.setUsePercentValues(true)
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.setTransparentCircleAlpha(0)
        pieChart.description.isEnabled = false
        pieChart.animateY(1000)
        pieChart.legend.isEnabled = true
        pieChart.invalidate()
    }

    private fun showLineChart(spendingData: List<Pair<Date, Double>>) {
        val sortedData = spendingData.sortedBy { it.first }
        val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

        val entries = sortedData.mapIndexed { index, (date, amount) ->
            Entry(index.toFloat(), amount.toFloat())
        }

        val labels = sortedData.map { dateFormat.format(it.first) }

        val dataSet = LineDataSet(entries, "Spending Over Time")
        dataSet.color = Color.BLUE
        dataSet.valueTextColor = Color.BLACK
        dataSet.setDrawFilled(true)
        dataSet.circleRadius = 4f

        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.description.isEnabled = false
        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        lineChart.xAxis.labelRotationAngle = -45f
        lineChart.axisRight.isEnabled = false
        lineChart.invalidate()
    }

    private fun updateLimitLines(minGoal: Float, maxGoal: Float) {
        val yAxis = barChart.axisLeft
        yAxis.removeAllLimitLines()

        val minLine = LimitLine(minGoal, "Min Goal")
        minLine.lineColor = Color.BLUE
        minLine.lineWidth = 2f

        val maxLine = LimitLine(maxGoal, "Max Limit")
        maxLine.lineColor = Color.RED
        maxLine.lineWidth = 2f

        yAxis.addLimitLine(minLine)
        yAxis.addLimitLine(maxLine)
        barChart.invalidate()
    }
}
