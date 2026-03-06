package com.example.registration_form

import android.animation.Animator
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.DatePicker
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.example.registration_form.databinding.ItemDonationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NgoDonationAdapter(
    private val isNgo: Boolean,
    private val isAcceptedTab: Boolean = false,
    private val onAccept: (Donation) -> Unit,
    private val onDeny: (Donation) -> Unit,
    private val onStatusChanged: (() -> Unit)? = null
) : ListAdapter<Donation, NgoDonationAdapter.DonationViewHolder>(DiffCallback()) {

    inner class DonationViewHolder(val binding: ItemDonationBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DonationViewHolder {
        val binding = ItemDonationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DonationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DonationViewHolder, position: Int) {
        val donation = getItem(position)

        with(holder.binding) {

            when (donation.status) {
                "ACCEPTED" -> root.setCardBackgroundColor(Color.parseColor("#FBFFFA"))
                "PENDING" -> root.setCardBackgroundColor(Color.parseColor("#FFFDF5"))
                else -> root.setCardBackgroundColor(Color.WHITE) // Default white
            }

            foodType.text = donation.foodType ?: "Food"
            tvDescription.text = donation.description
            tvQuantity.text = "Servings: ${donation.quantity}"
            date.text = formatRelativeDate(donation.date)

            if (isNgo) {
                tvStatus.visibility = View.GONE
            } else {
                tvStatus.visibility = View.VISIBLE
                tvStatus.text = if (donation.status == "ACCEPTED" && !donation.acceptedBy.isNullOrEmpty()) {
                    "Status: ACCEPTED by ${donation.acceptedBy}"
                } else {
                    "Status: ${donation.status}"
                }
            }

            if (isNgo) {
                if (!donation.restaurantName.isNullOrEmpty()) {
                    tvAddedBy.text = "${donation.restaurantName}"
                    tvAddedBy.visibility = View.VISIBLE
                } else if (!donation.createdBy.isNullOrEmpty()) {
                    // fallback if name missing
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(donation.createdBy!!)
                        .get()
                        .addOnSuccessListener { doc ->
                            val name = doc.getString("name") ?: "Unknown"
                            tvAddedBy.text = "Added by: $name"
                            tvAddedBy.visibility = View.VISIBLE
                        }
                        .addOnFailureListener {
                            tvAddedBy.text = "Added by: Unknown"
                            tvAddedBy.visibility = View.VISIBLE
                        }
                } else {
                    tvAddedBy.visibility = View.GONE
                }
            } else {
                tvAddedBy.visibility = View.GONE
            }

            // Accept/Deny buttons
            ngoActions.visibility = if (isNgo && donation.status == "PENDING") View.VISIBLE else View.GONE

            btnAccept.setOnClickListener {
                val ngoUid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(ngoUid)
                    .get()
                    .addOnSuccessListener { doc ->
                        val ngoName = doc.getString("name") ?: "NGO"
                        val donationRef = FirebaseDatabase.getInstance()
                            .getReference("donations")
                            .child(donation.id)

                        donationRef.child("status").setValue("ACCEPTED")
                        donationRef.child("acceptedBy").setValue(ngoName)

                        onAccept(donation.copy(status = "ACCEPTED", acceptedBy = ngoName))
                        showSuccessAnimation(holder.itemView.context) {
                            onStatusChanged?.invoke()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(holder.itemView.context, "Failed to fetch NGO name", Toast.LENGTH_SHORT).show()
                    }
            }

            btnDeny.setOnClickListener {
                onDeny(donation)
            }

            // Delivery scheduling section - only show for accepted donations in NGO view
            if (isNgo && isAcceptedTab && donation.status == "ACCEPTED") {
                deliveryInfo.visibility = View.VISIBLE

                // Update delivery status text
                if (donation.deliveryScheduled == true && donation.deliveryDate != null) {
                    val deliveryDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(Date(donation.deliveryDate))
                    tvDeliveryStatus.text = "Delivery : $deliveryDate"
                    btnSchedule.text = "Reschedule Delivery"
                } else {
                    tvDeliveryStatus.text = "Delivery: NOT SCHEDULED"
                    btnSchedule.text = "Schedule Delivery"
                }

                btnSchedule.setOnClickListener {
                    showDatePickerDialog(holder.itemView.context, donation)
                }
            } else {
                deliveryInfo.visibility = View.GONE
            }
        }
    }

    private fun showDatePickerDialog(context: Context, donation: Donation) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay)
                }

                // Ensure selected date is not in the past
                if (selectedCalendar.timeInMillis < System.currentTimeMillis()) {
                    Toast.makeText(context, "Please select a future date", Toast.LENGTH_SHORT).show()
                    return@DatePickerDialog
                }

                // Update donation in Firebase
                val donationRef = FirebaseDatabase.getInstance()
                    .getReference("donations")
                    .child(donation.id)

                donationRef.child("deliveryDate").setValue(selectedCalendar.timeInMillis)
                donationRef.child("deliveryScheduled").setValue(true)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Delivery scheduled successfully!", Toast.LENGTH_SHORT).show()
                        onStatusChanged?.invoke() // Refresh the list
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to schedule delivery", Toast.LENGTH_SHORT).show()
                    }
            },
            year, month, day
        )

        // Set minimum date to today
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()

        // Apply dark green theme programmatically
        applyDarkGreenThemeProgrammatically(context, datePickerDialog)

        // Apply rounded corners to the dialog window
        applyRoundedCorners(context, datePickerDialog)

        datePickerDialog.show()
    }

    private fun applyDarkGreenThemeProgrammatically(context: Context, datePickerDialog: DatePickerDialog) {
        // Set window background to white
        datePickerDialog.window?.setBackgroundDrawableResource(android.R.color.white)

        // Style buttons with dark green text and light gray background
        val positiveButton = datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE)
        val negativeButton = datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)

        val darkGreen = ContextCompat.getColor(context, R.color.dark_green_primary)
        val lightGray = ContextCompat.getColor(context, R.color.light_gray)

        positiveButton?.setTextColor(darkGreen)
        negativeButton?.setTextColor(darkGreen)

        // Add background with rounded corners to buttons
        positiveButton?.background = createRoundedButtonBackground(context, lightGray, darkGreen)
        negativeButton?.background = createRoundedButtonBackground(context, lightGray, darkGreen)

        // Additional dark green styling for Lollipop and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                // Set the header background color
                val headerId = context.resources.getIdentifier("date_picker_header", "id", "android")
                val headerView = datePickerDialog.datePicker.findViewById<View>(headerId)
                headerView?.setBackgroundColor(darkGreen)

                // Set text color in header for better contrast
                val headerTextId = context.resources.getIdentifier("date_picker_header_date", "id", "android")
                val headerTextView = datePickerDialog.datePicker.findViewById<TextView>(headerTextId)
                headerTextView?.setTextColor(ContextCompat.getColor(context, R.color.white))

                // Set the selected day circle color - CORRECTED APPROACH
                setDatePickerSelectionColor(context, datePickerDialog.datePicker, darkGreen)

            } catch (e: Exception) {
                Log.e("DatePicker", "Error applying green theme: ${e.message}")
            }
        }
    }

    private fun setDatePickerSelectionColor(context: Context, datePicker: DatePicker, color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+ - use the proper color accent methods
            try {
                datePicker.setBackgroundColor(Color.TRANSPARENT)
            } catch (e: Exception) {
                Log.e("DatePicker", "Error setting background: ${e.message}")
            }
        }

        // Try to find calendar view and modify it
        try {
            val calendarViewId = context.resources.getIdentifier("calendar_view", "id", "android")
            if (calendarViewId != 0) {
                val calendarView = datePicker.findViewById<View>(calendarViewId)
                calendarView?.setBackgroundColor(color)
            }
        } catch (e: Exception) {
            Log.e("DatePicker", "Error finding calendar view: ${e.message}")
        }
    }


    private fun applyRoundedCorners(context: Context, datePickerDialog: DatePickerDialog) {
        datePickerDialog.window?.let { window ->
            window.setBackgroundDrawable(createRoundedCornerDrawable(context))

            // Set window attributes for rounded corners
            val layoutParams = WindowManager.LayoutParams().apply {
                copyFrom(window.attributes)
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                gravity = Gravity.CENTER
            }
            window.attributes = layoutParams
        }
    }

    private fun createRoundedCornerDrawable(context: Context): Drawable {
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        shape.cornerRadius = 16f.dpToPx(context) // Convert dp to pixels
        shape.setColor(Color.WHITE)
        shape.setStroke(2, ContextCompat.getColor(context, R.color.dark_green_primary))
        return shape
    }

    private fun createRoundedButtonBackground(context: Context, backgroundColor: Int, strokeColor: Int): Drawable {
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        shape.cornerRadius = 8f.dpToPx(context) // Smaller radius for buttons
        shape.setColor(backgroundColor)
        shape.setStroke(1, strokeColor)
        return shape
    }

    // Extension function to convert dp to pixels with context parameter
    private fun Float.dpToPx(context: Context): Float {
        return this * context.resources.displayMetrics.density
    }

    private fun formatRelativeDate(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val oneDay = 24 * 60 * 60 * 1000
        return when {
            diff < oneDay -> "Today"
            diff < 2 * oneDay -> "Yesterday"
            diff < 7 * oneDay -> "${diff / oneDay} days ago"
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    private fun showSuccessAnimation(context: Context, onComplete: () -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.success_animation_dialog, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        val lottie = dialogView.findViewById<LottieAnimationView>(R.id.lottieSuccess)
        lottie.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                dialog.dismiss()
                Toast.makeText(context, "Donation Accepted!", Toast.LENGTH_SHORT).show()
                onComplete()
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
        lottie.playAnimation()
    }


    class DiffCallback : DiffUtil.ItemCallback<Donation>() {
        override fun areItemsTheSame(oldItem: Donation, newItem: Donation) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Donation, newItem: Donation) = oldItem == newItem
    }
}