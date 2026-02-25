package com.safebyte.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseUsersRepo(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getAllergensByEmail(emailLower: String): Set<String> {
        val doc = db.collection("users").document(emailLower).get().await()
        val list = doc.get("allergens") as? List<*> ?: emptyList<Any>()
        return list.mapNotNull { it as? String }.map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    suspend fun setAllergensByEmail(emailLower: String, allergens: Set<String>) {
        db.collection("users").document(emailLower)
            .update("allergens", allergens.toList())
            .await()
    }
}