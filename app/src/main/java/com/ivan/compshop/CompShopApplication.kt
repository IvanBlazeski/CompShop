package com.ivan.compshop

import android.app.Application
import com.google.firebase.analytics.FirebaseAnalytics
import com.ivan.compshop.data.local.AppDatabase
import com.ivan.compshop.data.repository.AuthRepository
import com.ivan.compshop.data.repository.CartRepository
import com.ivan.compshop.data.repository.ComputerRepository

class CompShopApplication : Application() {

    lateinit var analytics: FirebaseAnalytics

    val database by lazy { AppDatabase.getDatabase(this) }
    val cartRepository by lazy { CartRepository(database) }
    val computerRepository by lazy { ComputerRepository() }
    val authRepository by lazy { AuthRepository() }

    override fun onCreate() {
        super.onCreate()
        analytics = FirebaseAnalytics.getInstance(this)
    }
}