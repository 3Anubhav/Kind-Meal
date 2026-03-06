package com.example.registration_form

import android.os.Build
import android.os.Bundle
import android.transition.Slide
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class Settings : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setDarkStatusBarIcons(window)

        // Set up toolbar with back button
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Window enter/exit animations
        window.enterTransition = Slide(Gravity.END)
        window.exitTransition = Slide(Gravity.START)

        // Initialize settings
        setupSettings()
    }


    fun setDarkStatusBarIcons(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        }
    }

    private fun setupSettings() {
        // Notification switch setup
        val notificationSwitch = findViewById<SwitchCompat>(R.id.notification_switch)
        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Save preference
            // Example: SharedPreferences.Editor.putBoolean("notifications_enabled", isChecked).apply()
        }
    }

    // Handle change password click from XML onClick attribute
    fun onChangePasswordClick(view: View) {
        showChangePasswordDialog()
    }

    private fun showChangePasswordDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Change Password")
            .setView(R.layout.dialog_change_password) // Create this layout file
            .setPositiveButton("Change") { dialog, _ ->
                // Handle password change logic here
                // Example: validate inputs and update password
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}