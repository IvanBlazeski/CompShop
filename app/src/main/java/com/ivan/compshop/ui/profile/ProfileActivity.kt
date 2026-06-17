package com.ivan.compshop.ui.profile

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.bumptech.glide.Glide
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ivan.compshop.databinding.ActivityProfileBinding
import com.ivan.compshop.ui.auth.LoginActivity
import android.content.Intent
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var cameraImageUri: Uri? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCamera()
        else Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { uri ->
                val path = saveImageToInternalStorage(uri)
                if (path != null) {
                    Glide.with(this).load(java.io.File(path)).circleCrop().into(binding.ivAvatar)
                    binding.ivAvatar.clearColorFilter()
                    getSharedPreferences("settings", MODE_PRIVATE)
                        .edit().putString("profile_image_path", path).apply()
                    Toast.makeText(this, "Profile photo updated! 📷", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val path = saveImageToInternalStorage(it)
            if (path != null) {
                Glide.with(this).load(java.io.File(path)).circleCrop().into(binding.ivAvatar)
                binding.ivAvatar.clearColorFilter()
                getSharedPreferences("settings", MODE_PRIVATE)
                    .edit().putString("profile_image_path", path).apply()
                Toast.makeText(this, "Profile photo updated! 📷", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        loadProfile()
        loadStats()
        setupEdit()
        setupSave()
        setupAvatar()
        setupChangePassword()
        setupLanguage()
        setupDarkMode()
        setupLogout()
    }

    private fun loadProfile() {
        val user = auth.currentUser ?: return
        val userId = getUserId()

        binding.tvEmail.text = user.email ?: user.displayName ?: "User"
        binding.tvDisplayName.text = user.displayName ?: user.email?.substringBefore("@") ?: "User"

        // Вчитај зачувана слика веднаш
        val savedPath = getSharedPreferences("settings", MODE_PRIVATE)
            .getString("profile_image_path", null)
        if (savedPath != null) {
            Glide.with(this)
                .load(java.io.File(savedPath))
                .circleCrop()
                .into(binding.ivAvatar)
            binding.ivAvatar.clearColorFilter()
        }

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                binding.tvDisplayName.text = doc.getString("displayName")
                    ?: user.displayName
                            ?: user.email?.substringBefore("@") ?: "User"
                binding.etFullName.setText(doc.getString("displayName") ?: "")
                binding.etPhone.setText(doc.getString("phone") ?: "")
                binding.etAddress.setText(doc.getString("address") ?: "")

                val createdAt = doc.getTimestamp("createdAt")
                if (createdAt != null) {
                    val sdf = java.text.SimpleDateFormat("MMM yyyy", Locale.getDefault())
                    binding.tvMemberSince.text = "Member since ${sdf.format(createdAt.toDate())}"
                }
            }
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = java.io.File(filesDir, "profile_photo.jpg")
            val outputStream = java.io.FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun loadStats() {
        val userId = getUserId()
        var totalOrderCount = 0
        var totalSpentAmount = 0.0
        var completed = 0

        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = java.util.Calendar.getInstance()
        val dates = (0..30).map {
            val date = sdf.format(calendar.time)
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
            date
        }

        dates.forEach { date ->
            firestore.collection("orders").document(date).collection(userId)
                .get()
                .addOnSuccessListener { snapshot ->
                    totalOrderCount += snapshot.size()
                    totalSpentAmount += snapshot.documents.sumOf { it.getDouble("totalPrice") ?: 0.0 }
                    completed++
                    if (completed == dates.size) {
                        binding.tvOrderCount.text = totalOrderCount.toString()
                        binding.tvTotalSpent.text = "$${"%.0f".format(totalSpentAmount)}"
                    }
                }
                .addOnFailureListener {
                    completed++
                    if (completed == dates.size) {
                        binding.tvOrderCount.text = totalOrderCount.toString()
                        binding.tvTotalSpent.text = "$${"%.0f".format(totalSpentAmount)}"
                    }
                }
        }
    }

    private fun setupEdit() {
        val fields = listOf(binding.etFullName, binding.etPhone, binding.etAddress)
        fields.forEach { it.isEnabled = false; it.alpha = 0.7f }

        binding.btnEdit.setOnClickListener {
            fields.forEach { it.isEnabled = true; it.alpha = 1.0f }
            binding.btnSave.visibility = android.view.View.VISIBLE
            binding.btnEdit.text = "Cancel"
            binding.btnEdit.setOnClickListener {
                fields.forEach { it.isEnabled = false; it.alpha = 0.7f }
                binding.btnSave.visibility = android.view.View.GONE
                binding.btnEdit.text = "Edit Profile"
                setupEdit()
                loadProfile()
            }
        }
    }

    private fun setupSave() {
        binding.btnSave.setOnClickListener {
            val userId = getUserId()
            val updates = hashMapOf(
                "uid" to (auth.currentUser?.uid ?: ""),
                "email" to (auth.currentUser?.email ?: ""),
                "displayName" to binding.etFullName.text.toString().trim(),
                "phone" to binding.etPhone.text.toString().trim(),
                "address" to binding.etAddress.text.toString().trim()
            )
            firestore.collection("users").document(userId)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    binding.tvDisplayName.text = binding.etFullName.text.toString().trim()
                    binding.btnSave.visibility = android.view.View.GONE
                    binding.btnEdit.text = "Edit Profile"
                    val fields = listOf(binding.etFullName, binding.etPhone, binding.etAddress)
                    fields.forEach { it.isEnabled = false; it.alpha = 0.7f }
                    Toast.makeText(this, "Profile saved! ✅", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupAvatar() {
        binding.layoutAvatar.setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("Choose photo")
                .setItems(arrayOf("📷 Camera", "🖼️ Gallery")) { _, which ->
                    when (which) {
                        0 -> {
                            if (checkSelfPermission(android.Manifest.permission.CAMERA)
                                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                openCamera()
                            } else {
                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                        }
                        1 -> imagePickerLauncher.launch("image/*")
                    }
                }
                .show()
        }
    }

    private fun openCamera() {
        try {
            val photoFile = java.io.File.createTempFile("profile_", ".jpg", cacheDir)
            cameraImageUri = androidx.core.content.FileProvider.getUriForFile(
                this, "${packageName}.provider", photoFile
            )
            cameraLauncher.launch(cameraImageUri)
        } catch (e: Exception) {
            Toast.makeText(this, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupChangePassword() {
        binding.layoutChangePassword.setOnClickListener {
            val user = auth.currentUser ?: return@setOnClickListener
            val isEmailProvider = user.providerData.any { it.providerId == "password" }
            if (!isEmailProvider) {
                Toast.makeText(this, "Only available for email/password accounts", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dialogView = layoutInflater.inflate(com.ivan.compshop.R.layout.dialog_change_password, null)
            val dialog = android.app.AlertDialog.Builder(this).setView(dialogView).create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val etCurrent = dialogView.findViewById<android.widget.EditText>(com.ivan.compshop.R.id.etCurrentPassword)
            val etNew = dialogView.findViewById<android.widget.EditText>(com.ivan.compshop.R.id.etNewPassword)
            val btnChange = dialogView.findViewById<android.widget.Button>(com.ivan.compshop.R.id.btnChangePassword)

            btnChange?.setOnClickListener {
                val currentPass = etCurrent?.text.toString().trim()
                val newPass = etNew?.text.toString().trim()
                if (currentPass.isEmpty() || newPass.length < 6) {
                    Toast.makeText(this, "Fill all fields (min 6 chars)", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val credential = EmailAuthProvider.getCredential(user.email!!, currentPass)
                user.reauthenticate(credential)
                    .addOnSuccessListener {
                        user.updatePassword(newPass)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Password changed! ✅", Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Wrong current password ❌", Toast.LENGTH_SHORT).show()
                    }
            }
            dialog.show()
        }
    }

    private fun setupLanguage() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val currentLang = prefs.getString("language", "en")

        if (currentLang == "mk") {
            binding.btnLanguageMK.setBackgroundResource(com.ivan.compshop.R.drawable.btn_neon_gradient)
            binding.btnLanguageMK.setTextColor(android.graphics.Color.WHITE)
            binding.btnLanguageEN.setBackgroundResource(com.ivan.compshop.R.drawable.btn_social_neon)
            binding.btnLanguageEN.setTextColor(android.graphics.Color.parseColor("#00D4FF"))
        }

        binding.btnLanguageEN.setOnClickListener {
            prefs.edit().putString("language", "en").apply()
            binding.btnLanguageEN.setBackgroundResource(com.ivan.compshop.R.drawable.btn_neon_gradient)
            binding.btnLanguageEN.setTextColor(android.graphics.Color.WHITE)
            binding.btnLanguageMK.setBackgroundResource(com.ivan.compshop.R.drawable.btn_social_neon)
            binding.btnLanguageMK.setTextColor(android.graphics.Color.parseColor("#00D4FF"))
            setLocale("en")
        }

        binding.btnLanguageMK.setOnClickListener {
            prefs.edit().putString("language", "mk").apply()
            binding.btnLanguageMK.setBackgroundResource(com.ivan.compshop.R.drawable.btn_neon_gradient)
            binding.btnLanguageMK.setTextColor(android.graphics.Color.WHITE)
            binding.btnLanguageEN.setBackgroundResource(com.ivan.compshop.R.drawable.btn_social_neon)
            binding.btnLanguageEN.setTextColor(android.graphics.Color.parseColor("#00D4FF"))
            setLocale("mk")
        }
    }

    private fun setLocale(lang: String) {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        prefs.edit().putString("language", lang).apply()
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = android.content.res.Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun setupDarkMode() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_mode", true)
        binding.switchDarkMode.isChecked = isDark

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            if (isChecked) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }

    private fun setupLogout() {
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }

    private fun getUserId(): String {
        val user = auth.currentUser ?: return ""
        return when {
            !user.email.isNullOrEmpty() -> user.email!!
            !user.displayName.isNullOrEmpty() -> user.displayName!!.replace(" ", "_") + "_" + user.uid.take(6)
            else -> user.uid
        }
    }
}