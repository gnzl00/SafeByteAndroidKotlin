package com.safebyte.data

import com.google.firebase.firestore.FirebaseFirestore
import com.safebyte.normalizeAllergenSet
import kotlinx.coroutines.tasks.await

class FirebaseUsersRepo(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getAllergensByEmail(emailLower: String): Set<String> {
        val doc = db.collection("users").document(emailLower).get().await()
        val list = doc.get("allergens") as? List<*> ?: emptyList<Any>()
        return normalizeAllergenSet(list.mapNotNull { it as? String })
    }

    suspend fun setAllergensByEmail(emailLower: String, allergens: Set<String>) {
        val normalized = normalizeAllergenSet(allergens)
        db.collection("users").document(emailLower)
            .update("allergens", normalized.toList())
            .await()
    }
}
