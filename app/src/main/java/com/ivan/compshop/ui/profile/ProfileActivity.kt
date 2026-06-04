package com.ivan.compshop.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ivan.compshop.databinding.ActivityProfileBinding
import com.ivan.compshop.model.User
import com.ivan.compshop.ui.auth.LoginActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        loadProfile()
        setupClickListeners()
    }

    private fun loadProfile() {
        val user = auth.currentUser
        binding.tvEmail.text = user?.email ?: "Guest"

        user?.uid?.let { uid ->
            lifecycleScope.launch {
                try {
                    val doc = firestore.collection("users")
                        .document(uid)
                        .get()
                        .await()

                    val profile = doc.toObject(User::class.java)
                    profile?.let {
                        binding.etFullName.setText(it.fullName)
                        binding.etPhone.setText(it.phone)
                        binding.etAddress.setText(it.address)
                    }
                } catch (e: Exception) {
                    // Нема профил уште
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            saveProfile()
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }

    private fun saveProfile() {
        val uid = auth.currentUser?.uid ?: return

        val user = User(
            uid = uid,
            fullName = binding.etFullName.text.toString().trim(),
            email = auth.currentUser?.email ?: "",
            phone = binding.etPhone.text.toString().trim(),
            address = binding.etAddress.text.toString().trim()
        )

        lifecycleScope.launch {
            try {
                firestore.collection("users")
                    .document(uid)
                    .set(user)
                    .await()
                Toast.makeText(this@ProfileActivity,
                    getString(com.ivan.compshop.R.string.save),
                    Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity,
                    getString(com.ivan.compshop.R.string.error_occurred),
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
}