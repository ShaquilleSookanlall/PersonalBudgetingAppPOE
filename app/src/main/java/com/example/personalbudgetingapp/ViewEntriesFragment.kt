package com.example.personalbudgetingapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.personalbudgetingapp.databinding.FragmentViewEntriesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ViewEntriesFragment : Fragment() {

    private var _binding: FragmentViewEntriesBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var userId: String
    private val db = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var entryList: List<ExpenseEntry> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewEntriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid ?: ""

        if (userId.isEmpty()) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        binding.rvEntries.layoutManager = LinearLayoutManager(context)
        loadEntries("0000-01-01", "9999-12-31")

        binding.etStartDate.setOnClickListener {
            showDatePicker { selected ->
                binding.etStartDate.setText(selected)
            }
        }

        binding.etEndDate.setOnClickListener {
            showDatePicker { selected ->
                binding.etEndDate.setText(selected)
            }
        }

        binding.btnFilterEntries.setOnClickListener {
            val start = binding.etStartDate.text.toString().ifEmpty { "0000-01-01" }
            val end = binding.etEndDate.text.toString().ifEmpty { "9999-12-31" }
            loadEntries(start, end)
        }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            onDateSelected(String.format("%04d-%02d-%02d", year, month + 1, day))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadEntries(startDate: String, endDate: String) {
        val start = dateFormat.parse(startDate)
        val end = dateFormat.parse(endDate)

        db.collection("expenses")
            .document(userId)
            .collection("user_expense_entries")
            .get()
            .addOnSuccessListener { result ->
                entryList = result.mapNotNull { doc ->
                    val category = doc.getString("category") ?: return@mapNotNull null
                    val description = doc.getString("description") ?: ""
                    val amount = doc.getDouble("amount") ?: 0.0
                    val timestamp = doc.getTimestamp("date")?.toDate() ?: return@mapNotNull null
                    val formattedDate = dateFormat.format(timestamp)
                    val photoUri = doc.getString("photoUri")

                    if ((start != null && timestamp.before(start)) || (end != null && timestamp.after(end))) {
                        return@mapNotNull null
                    }

                    ExpenseEntry(
                        id = 0,
                        date = formattedDate,
                        description = description,
                        categoryId = 0,
                        amount = amount,
                        photoUri = photoUri,
                        userId = userId
                    )
                }

                binding.rvEntries.adapter = EntryAdapter(
                    entryList,
                    emptyList(),
                    onEditClicked = { entry -> navigateToEdit(entry) },
                    onDeleteClicked = { entry -> deleteEntry(entry) }
                )

                if (entryList.isEmpty()) {
                    Toast.makeText(requireContext(), "No entries found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load entries from Firebase.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToEdit(entry: ExpenseEntry) {
        val editFragment = EditEntryFragment.newInstance(entry)

        requireActivity().supportFragmentManager.beginTransaction()
            .replace((requireView().parent as ViewGroup).id, editFragment)
            .addToBackStack(null)
            .commit()
    }


    private fun deleteEntry(entry: ExpenseEntry) {
        val docRef = db.collection("expenses")
            .document(userId)
            .collection("user_expense_entries")
            .whereEqualTo("description", entry.description)
            .whereEqualTo("amount", entry.amount)
            .whereEqualTo("date", dateFormat.parse(entry.date))

        docRef.get().addOnSuccessListener { query ->
            if (!query.isEmpty) {
                for (doc in query.documents) {
                    doc.reference.delete()
                }
                loadEntries("0000-01-01", "9999-12-31")
                Toast.makeText(requireContext(), "Entry deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
