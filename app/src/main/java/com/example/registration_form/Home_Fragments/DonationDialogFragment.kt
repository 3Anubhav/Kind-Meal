package com.example.registration_form.Home_Fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.registration_form.Donation
import com.example.registration_form.R
import com.example.registration_form.SharedViewModel
import com.example.registration_form.databinding.DialogDonationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DonationDialogFragment : DialogFragment() {

    private var _binding: DialogDonationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SharedViewModel by activityViewModels()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDonationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val foodTypes = listOf("Raw", "Cooked")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, foodTypes)
        binding.foodTypeEditText.setAdapter(adapter)

        setupTextWatchers()
        setupSubmitButton()
    }

    private fun setupTextWatchers() {
        binding.foodTypeEditText.doOnTextChanged { _, _, _, _ ->
            binding.foodTypeInputLayout.error = null
        }
        binding.quantityEditText.doOnTextChanged { _, _, _, _ ->
            binding.quantityInputLayout.error = null
        }
        // Changed ?.error to .error because descriptionInputLayout should be non-null in your binding
        binding.descriptionEditText.doOnTextChanged { _, _, _, _ ->
            binding.descriptionInputLayout.error = null
        }
    }

    private fun setupSubmitButton() {
        binding.submitButton.setOnClickListener {
            hideKeyboard()

            val foodType = binding.foodTypeEditText.text?.toString()?.trim() ?: ""
            val quantityStr = binding.quantityEditText.text?.toString()?.trim() ?: ""
            val description = binding.descriptionEditText.text?.toString()?.trim() ?: ""

            if (!validateInput(foodType, quantityStr, description)) return@setOnClickListener

            val quantity = quantityStr.toInt()
            val userId = auth.currentUser?.uid

            if (userId.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val restaurantName = document.getString("name") ?: "Unknown Restaurant"

                    val donation = Donation(
                        foodType = foodType,
                        quantity = quantity,
                        description = description,
                        status = "PENDING",
                        date = System.currentTimeMillis(),
                        restaurantName = restaurantName,
                        createdBy = userId
                    )

                    viewModel.addDonation(donation)
                    Toast.makeText(requireContext(), "Donation added!", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to fetch restaurant name", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun validateInput(foodType: String, quantityStr: String, description: String): Boolean {
        var isValid = true

        if (foodType.isEmpty()) {
            binding.foodTypeInputLayout.error = "Please enter food type"
            isValid = false
        }

        if (quantityStr.isEmpty()) {
            binding.quantityInputLayout.error = "Please enter quantity"
            isValid = false
        } else {
            val quantity = quantityStr.toIntOrNull()
            if (quantity == null || quantity <= 0) {
                binding.quantityInputLayout.error = "Quantity must be a positive number"
                isValid = false
            }
        }

        if (description.isEmpty()) {
            binding.descriptionInputLayout.error = "Please enter description"
            isValid = false
        }

        return isValid
    }

    @SuppressLint("ServiceCast")
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_rounded)
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
