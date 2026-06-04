package com.ivan.compshop.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.ivan.compshop.CompShopApplication
import com.ivan.compshop.R
import com.ivan.compshop.databinding.ActivityLoginBinding
import com.ivan.compshop.ui.home.HomeActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authRepository: com.ivan.compshop.data.repository.AuthRepository

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { token ->
                lifecycleScope.launch {
                    val authResult = authRepository.loginWithGoogle(token)
                    authResult.fold(
                        onSuccess = { goToHome() },
                        onFailure = { Toast.makeText(this@LoginActivity, it.message, Toast.LENGTH_SHORT).show() }
                    )
                }
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = (application as CompShopApplication).authRepository

        if (authRepository.isLoggedIn) {
            goToHome()
            return
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
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

        // Google Sign-In
        binding.btnGoogle.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(this, gso)
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

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