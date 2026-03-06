package com.example.registration_form

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Registration : AppCompatActivity() {
    @SuppressLint("SetTextI18n", "CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        val auth = FirebaseAuth.getInstance()

        val dropdown =
            findViewById<androidx.appcompat.widget.AppCompatAutoCompleteTextView>(R.id.orgTypeDropdown)
        val types = arrayOf("Restaurant", "NGO")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, types)
        dropdown.setAdapter(adapter)

        val number = findViewById<TextInputEditText>(R.id.number_input)
        number.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val currentText = number.text.toString()
                if (!currentText.startsWith("+91 ")) {
                    number.setText("+91 ")
                    number.setSelection(number.text?.length ?: 0)
                }
            }
        }


        val composeView = findViewById<ComposeView>(R.id.compose_button)
        composeView.setContent {
            MaterialTheme {
                AnimatedGradientButton(
                    text = "Confirm",
                    onClick = {
                        val loadingDialog = showLoadingDialog()

                        val fields = listOf<TextInputLayout>(
                            findViewById(R.id.name),
                            findViewById(R.id.password),
                            findViewById(R.id.orgTypeDropdownLayout),
                            findViewById(R.id.address),
                            findViewById(R.id.email),
                            findViewById(R.id.number)
                        )

                        fields.forEach { field -> field.error = null }
                        val allFilled = fields.all { field -> field.editText?.text?.isNotEmpty() == true }

                        if (allFilled) {
                            val name = findViewById<TextInputLayout>(R.id.name).editText?.text.toString()
                            val email = findViewById<TextInputLayout>(R.id.email).editText?.text.toString()
                            val password = findViewById<TextInputLayout>(R.id.password).editText?.text.toString()
                            val number = findViewById<TextInputLayout>(R.id.number).editText?.text.toString()
                            val orgtype = findViewById<TextInputLayout>(R.id.orgTypeDropdownLayout).editText?.text.toString()
                            val address = findViewById<TextInputLayout>(R.id.address).editText?.text.toString()

                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val userId = auth.currentUser?.uid ?: return@addOnCompleteListener

                                        val user = hashMapOf(
                                            "name" to name,
                                            "email" to email,
                                            "pass" to password,
                                            "phone" to number,
                                            "orgType" to orgtype,
                                            "address" to address
                                        )

                                        FirebaseFirestore.getInstance()
                                            .collection("users")
                                            .document(userId)
                                            .set(user)
                                            .addOnSuccessListener {
                                                loadingDialog.dismiss() // ⬅️ Dismiss here
                                                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                                                fields.forEach { it.editText?.text?.clear() }
                                                startActivity(Intent(this, Launch::class.java))
                                                finish()
                                            }
                                            .addOnFailureListener {
                                                loadingDialog.dismiss()
                                                Toast.makeText(this, "Firestore save failed: ${it.message}", Toast.LENGTH_LONG).show()
                                            }
                                    } else {
                                        loadingDialog.dismiss()
                                        Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                        } else {
                            loadingDialog.dismiss()
                            fields.forEach { field ->
                                if (field.editText?.text?.isEmpty() == true) {
                                    field.error = "Required"
                                }
                            }
                        }

                        fields.forEach { field ->
                            field.editText?.addTextChangedListener(object : TextWatcher {
                                override fun afterTextChanged(s: Editable?) {
                                    if (!s.isNullOrEmpty()) field.error = null
                                }

                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            })
                        }
                    }
                )
            }
        }
    }
}

fun AppCompatActivity.showLoadingDialog(): AlertDialog {
    val dialogView: View = LayoutInflater.from(this).inflate(R.layout.dialog_loading_register, null)
    val dialog = AlertDialog.Builder(this)
        .setView(dialogView)
        .setCancelable(false)
        .create()
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    dialog.show()
    return dialog
}

@Composable
fun AnimatedGradientButton(
    text: String = "Smooth Button",
    onClick: () -> Unit = {}
) {
    val color1 = Color(0xFFCE9A00) // Gold
    val color2 = Color(0xFF349F52) // Green

    val transition = rememberInfiniteTransition()
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val startColor = lerp(color1, color2, progress)
    val endColor = lerp(color2, color1, progress)

    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                brush = Brush.linearGradient(listOf(startColor, endColor)),
                shape = RoundedCornerShape(13.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 32.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 18.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
