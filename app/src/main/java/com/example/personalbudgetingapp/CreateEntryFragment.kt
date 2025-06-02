package com.example.personalbudgetingapp

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.personalbudgetingapp.data.FirebaseService
import com.example.personalbudgetingapp.databinding.FragmentCreateEntryBinding
import com.example.personalbudgetingapp.model.Expense
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CreateEntryFragment : Fragment() {

    private var _binding: FragmentCreateEntryBinding? = null
    private val binding get() = _binding!!
    private var photoUri: Uri? = null
    private val firebaseService = FirebaseService()
    private var categoryNames: List<String> = emptyList()

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            binding.ivPhoto.visibility = View.VISIBLE
            Glide.with(this).load(photoUri).into(binding.ivPhoto)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fetch categories from Firebase
        firebaseService.getAllCategories { categories ->
            categoryNames = categories.map { it.name }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categoryNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCategory.adapter = adapter
        }

        binding.etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    binding.etDate.setText(String.format("%04d-%02d-%02d", year, month + 1, day))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.btnAddPhoto.setOnClickListener {
            val photoFile = createImageFile()
            photoUri = FileProvider.getUriForFile(
                requireContext(),
                "com.example.personalbudgetingapp.fileprovider",
                photoFile
            )
            takePictureLauncher.launch(photoUri)
        }

        binding.btnSaveEntry.setOnClickListener {
            val date = binding.etDate.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val categoryName = binding.spinnerCategory.selectedItem?.toString()
            val amount = binding.etAmount.text.toString().toDoubleOrNull()
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            if (userId == null) {
                Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (date.isEmpty() || description.isEmpty() || categoryName == null || amount == null) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date) ?: Date()

            val expense = Expense(
                amount = amount,
                category = categoryName,
                description = description,
                date = parsedDate
            )

            firebaseService.uploadExpense(expense) { success ->
                if (success) {
                    Toast.makeText(context, "Expense saved", Toast.LENGTH_SHORT).show()
                    clearFields()
                } else {
                    Toast.makeText(context, "Failed to save expense", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun clearFields() {
        binding.etDate.text.clear()
        binding.etDescription.text.clear()
        binding.etAmount.text.clear()
        binding.ivPhoto.visibility = View.GONE
        photoUri = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
