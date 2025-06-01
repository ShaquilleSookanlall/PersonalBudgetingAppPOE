package com.example.personalbudgetingapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

        binding.btnFilterEntries.setOnClickListener {
            val start = binding.etStartDate.text.toString().ifEmpty { "0000-01-01" }
            val end = binding.etEndDate.text.toString().ifEmpty { "9999-12-31" }
            loadEntries(start, end)
        }
    }

    private fun loadEntries(startDate: String, endDate: String) {
        val start = runCatching { dateFormat.parse(startDate) }.getOrNull()
        val end = runCatching { dateFormat.parse(endDate) }.getOrNull()

        db.collection("expenses")
            .document(userId)
            .collection("user_expense_entries")
            .get()
            .addOnSuccessListener { result ->
                val entries = result.mapNotNull { doc ->
                    try {
                        val category = doc.getString("category") ?: return@mapNotNull null
                        val description = doc.getString("description") ?: ""
                        val amount = doc.getDouble("amount") ?: 0.0
                        val timestamp = doc.getTimestamp("date")?.toDate() ?: return@mapNotNull null
                        val formattedDate = dateFormat.format(timestamp)

                        if ((start != null && timestamp.before(start)) || (end != null && timestamp.after(end))) {
                            return@mapNotNull null
                        }

                        ExpenseEntry(
                            id = 0,
                            date = formattedDate,
                            description = description,
                            categoryId = 0,
                            amount = amount,
                            photoUri = null,
                            userId = userId,
                            firebaseId = doc.id
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                binding.rvEntries.adapter = EntryAdapter(entries, emptyList(),
                    onEditClicked = { showEditDialog(it) },
                    onDeleteClicked = { confirmDelete(it) }
                )

                if (entries.isEmpty()) {
                    Toast.makeText(requireContext(), "No entries found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load entries from Firebase.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmDelete(entry: ExpenseEntry) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Entry")
            .setMessage("Are you sure you want to delete this entry?")
            .setPositiveButton("Delete") { _, _ -> deleteEntryFromFirebase(entry) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteEntryFromFirebase(entry: ExpenseEntry) {
        val id = entry.firebaseId ?: return
        db.collection("expenses")
            .document(userId)
            .collection("user_expense_entries")
            .document(id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Entry deleted", Toast.LENGTH_SHORT).show()
                reloadEntries()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to delete entry", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEditDialog(entry: ExpenseEntry) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_entry, null)
        val etDescription = dialogView.findViewById<EditText>(R.id.etEditDescription)
        val etAmount = dialogView.findViewById<EditText>(R.id.etEditAmount)

        etDescription.setText(entry.description)
        etAmount.setText(entry.amount.toString())

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Entry")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val updatedDesc = etDescription.text.toString()
                val updatedAmount = etAmount.text.toString().toDoubleOrNull() ?: entry.amount
                updateEntryInFirebase(entry, updatedDesc, updatedAmount)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateEntryInFirebase(entry: ExpenseEntry, description: String, amount: Double) {
        val id = entry.firebaseId ?: return
        db.collection("expenses")
            .document(userId)
            .collection("user_expense_entries")
            .document(id)
            .update(
                mapOf(
                    "description" to description,
                    "amount" to amount
                )
            )
            .addOnSuccessListener {
                Toast.makeText(context, "Entry updated", Toast.LENGTH_SHORT).show()
                reloadEntries()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun reloadEntries() {
        val start = binding.etStartDate.text.toString().ifEmpty { "0000-01-01" }
        val end = binding.etEndDate.text.toString().ifEmpty { "9999-12-31" }
        loadEntries(start, end)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
