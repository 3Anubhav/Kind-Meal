package com.example.registration_form

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.registration_form.databinding.ActivityMyProfileBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyProfileBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnEditProfile.setOnClickListener { showEditProfileDialog() }

        binding.profileBack.setOnClickListener { finish() }
        loadUserData()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadUserData() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load profile picture if available
        user.photoUrl?.let {
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.ic_profile_placeholder) // Optional: placeholder image
                .error(R.drawable.ic_profile_placeholder) // Optional: error image
                .into(binding.profileImage)
        }

        db.collection("users").document(user.uid)
            .addSnapshotListener { documentSnapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error fetching data: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    Log.d("ProfileData", "Document data: ${documentSnapshot.data}")

                    val name = documentSnapshot.getString("name") ?: "Not specified"
                    val orgType = documentSnapshot.getString("orgType") ?: "Not specified"
                    val address = documentSnapshot.getString("address") ?: "Not specified"
                    val phone = documentSnapshot.getString("phone") ?: "Not specified"
                    val email = documentSnapshot.getString("email") ?: user.email ?: "Not specified"

                    runOnUiThread {
                        binding.organizationName.text = name
                        binding.organizationType.text = orgType
                        binding.address.text = address
                        binding.email.text = email
                        binding.phone.text = phone
                    }
                } else {
                    Toast.makeText(this, "User document doesn't exist", Toast.LENGTH_SHORT).show()
                }
            }
    }

    

    private fun showEditProfileDialog() {
        val userId = auth.currentUser?.uid ?: return

        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)

        // Create dialog with custom rounded style
        val dialog = AlertDialog.Builder(this, R.style.RoundedDialog)
            .setView(dialogView)
            .setTitle("Edit Profile")
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        // Get references to all dialog views
        val etName = dialogView.findViewById<TextInputEditText>(R.id.OrganizationName)
        val etOrgType = dialogView.findViewById<TextInputEditText>(R.id.OrganizationType)
        val etAddress = dialogView.findViewById<TextInputEditText>(R.id.Address)
        val etEmail = dialogView.findViewById<TextInputEditText>(R.id.Email)
        val etPhone = dialogView.findViewById<TextInputEditText>(R.id.Phone)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)

        // Fetch current data from the correct collection
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Set current values using correct field names
                    etName.setText(document.getString("name"))
                    etOrgType.setText(document.getString("orgType"))
                    etAddress.setText(document.getString("address"))
                    etEmail.setText(document.getString("email"))
                    etPhone.setText(document.getString("phone"))

                    // Set save button click listener
                    btnSave.setOnClickListener {
                        val updatedName = etName.text.toString().trim()
                        val updatedOrgType = etOrgType.text.toString().trim()
                        val updatedAddress = etAddress.text.toString().trim()
                        val updatedEmail = etEmail.text.toString().trim()
                        val updatedPhone = etPhone.text.toString().trim()

                        if (validateInputs(
                                etName,
                                etOrgType,
                                etEmail,
                                etPhone,
                                updatedName,
                                updatedOrgType,
                                updatedEmail,
                                updatedPhone
                            )
                        ) {
                            updateProfile(
                                userId,
                                updatedName,
                                updatedOrgType,
                                updatedAddress,
                                updatedEmail,
                                updatedPhone,
                                progressBar,
                                btnSave,
                                dialog
                            )
                        }
                    }

                    dialog.show()
                } else {
                    Toast.makeText(this, "Profile data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateInputs(
        etOrganizationName: TextInputEditText,
        etOrgType: TextInputEditText,
        etEmail: TextInputEditText,
        etPhone: TextInputEditText,
        orgName: String,
        orgType: String,
        email: String,
        phone: String
    ): Boolean {
        var isValid = true

        if (orgName.isEmpty()) {
            etOrganizationName.error = "Organization name is required"
            isValid = false
        } else {
            etOrganizationName.error = null
        }

        if (orgType.isEmpty()) { // Validate orgType
            etOrgType.error = "Organization type is required"
            isValid = false
        } else {
            etOrgType.error = null
        }

        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Enter a valid email"
            isValid = false
        } else {
            etEmail.error = null
        }

        if (phone.isEmpty()) {
            etPhone.error = "Phone is required"
            isValid = false
        } else if (phone.length < 10) {
            etPhone.error = "Enter a valid phone number"
            isValid = false
        } else {
            etPhone.error = null
        }

        return isValid
    }

    private fun updateProfile(
        userId: String,
        name: String,
        orgType: String,
        address: String,
        email: String,
        phone: String,
        progressBar: ProgressBar,
        saveButton: Button,
        dialog: AlertDialog
    ) {
        progressBar.visibility = View.VISIBLE
        saveButton.isEnabled = false

        val updates = hashMapOf<String, Any>(
            "name" to name,
            "orgType" to orgType,
            "address" to address,
            "email" to email,
            "phone" to phone
        )

        db.collection("users").document(userId) // Changed from organizations to users
            .update(updates)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                loadUserData() // Refresh the displayed data
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                saveButton.isEnabled = true
                Toast.makeText(this, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}