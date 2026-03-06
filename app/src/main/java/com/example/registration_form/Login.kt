package com.example.registration_form

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Window
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

@Suppress("DEPRECATION")
class Login : AppCompatActivity() {

    private lateinit var loadingDialog: android.app.AlertDialog
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private val RC_SIGN_IN = 1001

    @SuppressLint("CutPasteId", "UseKtx")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        fun setDarkStatusBarIcons(window: Window) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            }
        }
        setDarkStatusBarIcons(window)

        val emailField = findViewById<TextInputLayout>(R.id.login_email)
        val passwordField = findViewById<TextInputLayout>(R.id.login_pass)
        val fields = listOf(emailField, passwordField)

        findViewById<Button>(R.id.confirm_button).setOnClickListener {
            val email = emailField.editText?.text.toString().trim()
            val password = passwordField.editText?.text.toString().trim()

            fields.forEach { it.error = null }

            if (email.isEmpty() || password.isEmpty()) {
                fields.forEach {
                    if (it.editText?.text.isNullOrEmpty()) {
                        it.error = "Required"
                    }
                }
            } else {
                showLoadingDialog()
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                FirebaseFirestore.getInstance().collection("users").document(userId).get()
                                    .addOnSuccessListener { document ->
                                        hideLoadingDialog()
                                        if (document != null && document.exists()) {
                                            val orgType = document.getString("orgType")
                                            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                                            getSharedPreferences("MyPrefs", MODE_PRIVATE).edit()
                                                .putBoolean("isLoggedIn", true).apply()
                                            if (orgType == "NGO") {
                                                startActivity(Intent(this, Ngo_home::class.java).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                })
                                            } else {
                                                startActivity(Intent(this, Home::class.java).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                })
                                            }
                                            finish()
                                        } else {
                                            Toast.makeText(this, "User data not found.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                    .addOnFailureListener {
                                        hideLoadingDialog()
                                        Toast.makeText(this, "Failed to fetch user data.", Toast.LENGTH_LONG).show()
                                    }
                            }
                        } else {
                            hideLoadingDialog()
                            Toast.makeText(this, "Login failed: Please login with Google", Toast.LENGTH_LONG).show()
                        }
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

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        findViewById<Button>(R.id.btn_register).setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    signInWithGoogle(idToken)
                } else {
                    Toast.makeText(this, "Google account error", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Signin failed !", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signInWithGoogle(idToken: String) {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        val userEmail = account?.email?.lowercase()

        if (userEmail == null) {
            Toast.makeText(this, "Failed to retrieve email.", Toast.LENGTH_SHORT).show()
            return
        }

        showLoadingDialog()
        FirebaseFirestore.getInstance().collection("users")
            .whereEqualTo("email", userEmail)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(credential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val userId = auth.currentUser?.uid
                                if (userId != null) {
                                    FirebaseFirestore.getInstance().collection("users").document(userId).get()
                                        .addOnSuccessListener { document ->
                                            hideLoadingDialog()
                                            if (document != null && document.exists()) {
                                                val orgType = document.getString("orgType")
                                                Toast.makeText(this, "Google login successful!", Toast.LENGTH_SHORT).show()
                                                if (orgType == "NGO") {
                                                    startActivity(Intent(this, Ngo_home::class.java).apply {
                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                    })
                                                } else {
                                                    startActivity(Intent(this, Home::class.java).apply {
                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                    })
                                                }
                                                finish()
                                            } else {
                                                Toast.makeText(this, "User data not found.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                        .addOnFailureListener {
                                            hideLoadingDialog()
                                            Toast.makeText(this, "Failed to fetch user data.", Toast.LENGTH_LONG).show()
                                        }
                                }
                            } else {
                                hideLoadingDialog()
                                Toast.makeText(this, "Auth failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    hideLoadingDialog()
                    Toast.makeText(this, "Access denied: Email not registered", Toast.LENGTH_LONG).show()
                    googleSignInClient.signOut()
                }
            }
            .addOnFailureListener {
                hideLoadingDialog()
                Toast.makeText(this, "Firestore error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    @SuppressLint("InflateParams")
    private fun showLoadingDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setView(layoutInflater.inflate(R.layout.dialog_loading, null))
        builder.setCancelable(false)
        loadingDialog = builder.create()
        loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loadingDialog.show()
    }

    private fun hideLoadingDialog() {
        if (::loadingDialog.isInitialized && loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }
}
