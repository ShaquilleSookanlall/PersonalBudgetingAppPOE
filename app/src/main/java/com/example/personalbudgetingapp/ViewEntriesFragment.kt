package com.example.personalbudgetingapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.personalbudgetingapp.data.FirebaseService
import com.example.personalbudgetingapp.databinding.FragmentViewEntriesBinding
import com.google.firebase.auth.FirebaseAuth

class ViewEntriesFragment : Fragment() {

    private var _binding: FragmentViewEntriesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: EntryAdapter
    private val firebaseService = FirebaseService()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewEntriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvEntries.layoutManager = LinearLayoutManager(requireContext())
        loadEntries()
    }

    private fun loadEntries() {
        firebaseService.getAllCategories { categories: List<Category> ->
            firebaseService.getUserEntries(userId) { entries: List<ExpenseEntry> ->
                adapter = EntryAdapter(
                    entries,
                    categories,
                    onEditClicked = { entry: ExpenseEntry ->
                        val fragment = EditEntryFragment.newInstance(entry)
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, fragment)
                            .addToBackStack(null)
                            .commit()
                    },
                    onDeleteClicked = { entry: ExpenseEntry ->
                        firebaseService.deleteExpense(entry) { success ->
                            if (success) {
                                firebaseService.getUserEntries(userId) { updatedEntries ->
                                    adapter.updateData(updatedEntries)
                                }
                            } else {
                                Toast.makeText(requireContext(), "Failed to delete entry", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
                binding.rvEntries.adapter = adapter
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
