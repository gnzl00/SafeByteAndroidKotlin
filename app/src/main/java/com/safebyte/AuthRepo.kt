package com.safebyte.auth

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class LoginResult(val ok: Boolean, val error: String? = null, val username: String? = null)

class AuthRepo(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun login(email: String, password: String): LoginResult {
        val doc = db.collection("users").document(email.trim().lowercase()).get().await()
        if (!doc.exists()) return LoginResult(false, "Usuario no encontrado")

        val storedHash = doc.getString("passwordHash") ?: return LoginResult(false, "Usuario inválido")
        val inputHash = PasswordHasher.hashPassword(password)

        if (storedHash != inputHash) return LoginResult(false, "Contraseña incorrecta")

        val username = doc.getString("username")
        return LoginResult(true, username = username)
    }
}