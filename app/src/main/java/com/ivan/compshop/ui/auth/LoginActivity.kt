package com.ivan.compshop.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.ivan.compshop.CompShopApplication
import com.ivan.compshop.R
import com.ivan.compshop.databinding.ActivityLoginBinding
import com.ivan.compshop.ui.home.HomeActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authRepository: com.ivan.compshop.data.repository.AuthRepository
    private lateinit var callbackManager: CallbackManager
    private val auth = FirebaseAuth.getInstance()

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
        callbackManager = CallbackManager.Factory.create()

        if (authRepository.isLoggedIn) {
            goToHome()
            return
        }

        setupClickListeners()
        setupFacebookLogin()
    }

    private fun setupFacebookLogin() {
        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
                    lifecycleScope.launch {
                        try {
                            auth.signOut()
                            auth.signInWithCredential(credential).await()
                            goToHome()
                        } catch (e: Exception) {
                            Toast.makeText(this@LoginActivity, e.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onCancel() {}
                override fun onError(error: FacebookException) {
                    Toast.makeText(this@LoginActivity, error.message, Toast.LENGTH_SHORT).show()
                }
            })
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

        binding.btnGoogle.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(this, gso)
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        binding.btnFacebook.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(
                this,
                callbackManager,
                listOf("public_profile")
            )
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