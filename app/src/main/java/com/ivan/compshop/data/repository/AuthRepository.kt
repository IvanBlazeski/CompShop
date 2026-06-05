package com.ivan.compshop.data.repository

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth: FirebaseAuth = Firebase.auth

    val currentUser: FirebaseUser? get() = auth.currentUser

    val isLoggedIn: Boolean get() = auth.currentUser != null

    suspend fun loginWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginAnonymously(): Result<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveUserToFirestore(user: FirebaseUser) {
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val email = user.email
            val displayName = user.displayName ?: "User"

            val userData = hashMapOf(
                "uid" to user.uid,
                "email" to (email ?: ""),
                "displayName" to displayName,
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            // Приоритет: email → displayName → uid
            val documentId = when {
                !email.isNullOrEmpty() -> email
                displayName != "User" -> "${displayName.replace(" ", "_")}_${user.uid.take(6)}"
                else -> user.uid
            }

            firestore.collection("users")
                .document(documentId)
                .set(userData, com.google.firebase.firestore.SetOptions.merge())
                .await()

        } catch (e: Exception) {
            // Ignore
        }
    }

    fun logout() {
        auth.signOut()
    }
}