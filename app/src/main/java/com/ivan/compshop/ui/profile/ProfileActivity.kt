package com.ivan.compshop.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ivan.compshop.databinding.ActivityProfileBinding
import com.ivan.compshop.ui.auth.LoginActivity
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        loadProfile()
        loadStats()
        setupSave()
        setupLogout()
    }

    private fun loadProfile() {
        val user = auth.currentUser ?: return

        binding.tvEmail.text = user.email ?: user.displayName ?: "User"
        binding.tvDisplayName.text = user.displayName ?: user.email?.substringBefore("@") ?: "User"

        val userId = when {
            !user.email.isNullOrEmpty() -> user.email!!
            !user.displayName.isNullOrEmpty() -> user.displayName!!.replace(" ", "_") + "_" + user.uid.take(6)
            else -> user.uid
        }

        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                binding.etFullName.setText(doc.getString("displayName") ?: "")
                binding.etPhone.setText(doc.getString("phone") ?: "")
                binding.etAddress.setText(doc.getString("address") ?: "")
            }
    }

    private fun loadStats() {
        val user = auth.currentUser ?: return

        val userId = when {
            !user.email.isNullOrEmpty() -> user.email!!
            !user.displayName.isNullOrEmpty() -> user.displayName!!.replace(" ", "_") + "_" + user.uid.take(6)
            else -> user.uid
        }

        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())

        firestore.collection("orders")
            .document(today)
            .collection(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val orderCount = snapshot.size()
                val totalSpent = snapshot.documents.sumOf { it.getDouble("totalPrice") ?: 0.0 }

                binding.tvOrderCount.text = orderCount.toString()
                binding.tvTotalSpent.text = "$${"%.0f".format(totalSpent)}"
            }
    }

    private fun setupSave() {
        binding.btnSave.setOnClickListener {
            val user = auth.currentUser ?: return@setOnClickListener

            val userId = when {
                !user.email.isNullOrEmpty() -> user.email!!
                !user.displayName.isNullOrEmpty() -> user.displayName!!.replace(" ", "_") + "_" + user.uid.take(6)
                else -> user.uid
            }

            val updates = hashMapOf(
                "displayName" to binding.etFullName.text.toString().trim(),
                "phone" to binding.etPhone.text.toString().trim(),
                "address" to binding.etAddress.text.toString().trim()
            )

            firestore.collection("users")
                .document(userId)
                .update(updates as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile saved! ✅", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error saving profile", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupLogout() {
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }
}