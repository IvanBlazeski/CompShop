package com.ivan.compshop.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ivan.compshop.CompShopApplication
import com.ivan.compshop.databinding.ActivityLoginBinding
import com.ivan.compshop.ui.home.HomeActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authRepository: com.ivan.compshop.data.repository.AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = (application as CompShopApplication).authRepository

        // Ако веќе е логиран, оди на Home
        if (authRepository.isLoggedIn) {
            goToHome()
            return
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {

        // Login со Email
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Внесете email и лозинка", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showLoading(true)
            lifecycleScope.launch {
                val result = authRepository.loginWithEmail(email, password)
                showLoading(false)
                result.fold(
                    onSuccess = { goToHome() },
                    onFailure = { Toast.makeText(this@LoginActivity, it.message, Toast.LENGTH_SHORT).show() }
                )
            }
        }

        // Anonymous Login
        binding.btnAnonymous.setOnClickListener {
            showLoading(true)
            lifecycleScope.launch {
                val result = authRepository.loginAnonymously()
                showLoading(false)
                result.fold(
                    onSuccess = { goToHome() },
                    onFailure = { Toast.makeText(this@LoginActivity, it.message, Toast.LENGTH_SHORT).show() }
                )
            }
        }

        // Оди на Register
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun showLoading(show: Boolean) {
        binding.btnLogin.isEnabled = !show
        binding.btnAnonymous.isEnabled = !show
    }

    private fun goToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}