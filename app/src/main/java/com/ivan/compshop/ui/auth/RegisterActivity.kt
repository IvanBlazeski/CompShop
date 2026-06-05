package com.ivan.compshop.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ivan.compshop.CompShopApplication
import com.ivan.compshop.databinding.ActivityRegisterBinding
import com.ivan.compshop.ui.home.HomeActivity
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authRepository: com.ivan.compshop.data.repository.AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = (application as CompShopApplication).authRepository
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Пополнете ги сите полиња", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Лозинката мора да има минимум 6 карактери", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val result = authRepository.registerWithEmail(email, password)
                result.fold(
                    onSuccess = { user ->
                        authRepository.saveUserToFirestore(user)
                        Toast.makeText(
                            this@RegisterActivity,
                            "Регистрацијата е успешна!",
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(Intent(this@RegisterActivity, HomeActivity::class.java))
                        finish()
                    },
                    onFailure = {
                        Toast.makeText(this@RegisterActivity, it.message, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }
}