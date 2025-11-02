package com.health.virtualdoctor.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.health.virtualdoctor.R

class LoginActivity : AppCompatActivity() {

    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvForgotPassword: View
    private lateinit var tvRegisterLink: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Rendre la barre de statut transparente
        window.statusBarColor = resources.getColor(R.color.transparent, theme)

        initViews()
        setupListeners()
        setupValidation()
    }

    private fun initViews() {
        tilEmail = findViewById(R.id.tilEmail)
        etEmail = findViewById(R.id.etEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvRegisterLink = findViewById(R.id.tvRegisterLink)
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener {
            if (validateInputs()) {
                performLogin()
            }
        }

        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Fonctionnalité à venir", Toast.LENGTH_SHORT).show()
        }

        tvRegisterLink.setOnClickListener {
            navigateToRegister()
        }
    }

    private fun setupValidation() {
        // Validation en temps réel pour l'email
        etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                tilEmail.error = null
            }
        })

        // Validation en temps réel pour le mot de passe
        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                tilPassword.error = null
            }
        })
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Validation email
        val email = etEmail.text.toString().trim()
        if (email.isEmpty()) {
            tilEmail.error = "L'email est requis"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Email invalide"
            isValid = false
        } else {
            tilEmail.error = null
        }

        // Validation mot de passe
        val password = etPassword.text.toString()
        if (password.isEmpty()) {
            tilPassword.error = "Le mot de passe est requis"
            isValid = false
        } else if (password.length < 6) {
            tilPassword.error = "Au moins 6 caractères"
            isValid = false
        } else {
            tilPassword.error = null
        }

        return isValid
    }

    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        // Désactiver le bouton pendant le traitement
        btnLogin.isEnabled = false
        btnLogin.text = "Connexion..."

        // Simuler une connexion (à remplacer par l'appel API réel)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            btnLogin.isEnabled = true
            btnLogin.text = getString(R.string.login_button)

            // Pour le moment, afficher un message de succès
            Toast.makeText(this, "Connexion réussie!", Toast.LENGTH_SHORT).show()

            // TODO: Navigation vers HomeActivity ou DoctorDashboardActivity selon le rôle
            // navigateToHome()
        }, 2000)
    }

    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    // À implémenter plus tard
    // private fun navigateToHome() {
    //     val intent = Intent(this, HomeActivity::class.java)
    //     startActivity(intent)
    //     finish()
    // }
}