package com.ivan.compshop

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.ivan.compshop.data.local.AppDatabase
import com.ivan.compshop.data.repository.AuthRepository
import com.ivan.compshop.data.repository.CartRepository
import com.ivan.compshop.data.repository.ComputerRepository
import androidx.appcompat.app.AppCompatDelegate

class CompShopApplication : Application() {

    lateinit var analytics: FirebaseAnalytics

    val database by lazy { AppDatabase.getDatabase(this) }
    val cartRepository by lazy { CartRepository(database) }
    val computerRepository by lazy { ComputerRepository() }
    val authRepository by lazy { AuthRepository() }

    override fun onCreate() {
        super.onCreate()

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        // Dark Mode
        val isDark = prefs.getBoolean("dark_mode", true)
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // Language
        val lang = prefs.getString("language", "en") ?: "en"
        val locale = java.util.Locale(lang)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        // Notification Channel
        val channel = NotificationChannel(
            "tracking_channel",
            "Order Tracking",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Order tracking status updates"
            enableLights(true)
            enableVibration(true)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        analytics = FirebaseAnalytics.getInstance(this)
    }
}