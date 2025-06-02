package com.example.personalbudgetingapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.personalbudgetingapp.data.FirebaseService
import com.example.personalbudgetingapp.databinding.FragmentCreateCategoryBinding

class CreateCategoryFragment : Fragment() {

    private var _binding: FragmentCreateCategoryBinding? = null
    private val binding get() = _binding!!
    private val firebaseService = FirebaseService()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSaveCategory.setOnClickListener {
            val categoryName = binding.etCategoryName.text.toString().trim()

            if (categoryName.isEmpty()) {
                Toast.makeText(context, "Please enter a category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val category = Category(name = categoryName)

            firebaseService.uploadCategory(category) { success ->
                if (success) {
                    Toast.makeText(context, "Category saved", Toast.LENGTH_SHORT).show()
                    binding.etCategoryName.text.clear()
                } else {
                    Toast.makeText(context, "Failed to save category", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
