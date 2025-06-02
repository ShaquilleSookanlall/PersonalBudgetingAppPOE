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
import com.example.personalbudgetingapp.databinding.FragmentEditEntryBinding
import com.example.personalbudgetingapp.data.FirebaseService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class EditEntryFragment : Fragment() {

    private var _binding: FragmentEditEntryBinding? = null
    private val binding get() = _binding!!
    private var entry: ExpenseEntry? = null
    private var photoUri: Uri? = null
    private val firebaseService = FirebaseService()
    private val firestore = FirebaseFirestore.getInstance()

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            binding.ivPhoto.visibility = View.VISIBLE
            Glide.with(this).load(photoUri).into(binding.ivPhoto)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        entry = arguments?.getParcelable("entry")

        binding.etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, year, month, day ->
                binding.etDate.setText(String.format("%04d-%02d-%02d", year, month + 1, day))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        firebaseService.getAllCategories { categories ->
            val categoryNames = categories.map { it.name }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCategory.adapter = adapter

            entry?.let { e ->
                binding.etDate.setText(e.date)
                binding.etDescription.setText(e.description)
                binding.etAmount.setText(e.amount.toString())

                val categoryName = categories.find { it.id == e.categoryId.toString() }?.name
                val position = categoryNames.indexOf(categoryName)
                if (position >= 0) binding.spinnerCategory.setSelection(position)

                e.photoUri?.let {
                    photoUri = Uri.parse(it)
                    binding.ivPhoto.visibility = View.VISIBLE
                    Glide.with(this).load(photoUri).into(binding.ivPhoto)
                }
            }
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

        binding.btnUpdate.setOnClickListener {
            val newDate = binding.etDate.text.toString()
            val newDescription = binding.etDescription.text.toString()
            val newAmount = binding.etAmount.text.toString().toDoubleOrNull() ?: 0.0
            val newCategory = binding.spinnerCategory.selectedItem?.toString() ?: return@setOnClickListener

            if (entry == null) return@setOnClickListener
            val userId = entry!!.userId

            CoroutineScope(Dispatchers.IO).launch {
                val parsedOldDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(entry!!.date)
                val parsedNewDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(newDate)

                firestore.collection("expenses")
                    .document(userId)
                    .collection("user_expense_entries")
                    .whereEqualTo("description", entry!!.description)
                    .whereEqualTo("amount", entry!!.amount)
                    .whereEqualTo("date", parsedOldDate)
                    .get()
                    .addOnSuccessListener { documents ->
                        for (doc in documents) {
                            doc.reference.update(
                                mapOf(
                                    "description" to newDescription,
                                    "amount" to newAmount,
                                    "date" to parsedNewDate,
                                    "category" to newCategory,
                                    "photoUri" to photoUri?.toString()
                                )
                            )
                        }

                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(requireContext(), "Entry updated", Toast.LENGTH_SHORT).show()
                            requireActivity().supportFragmentManager.popBackStack()
                        }
                    }
                    .addOnFailureListener {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(requireContext(), "Failed to update entry in Firebase", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(entry: ExpenseEntry): EditEntryFragment {
            val fragment = EditEntryFragment()
            val args = Bundle()
            args.putParcelable("entry", entry)
            fragment.arguments = args
            return fragment
        }
    }
}
