package com.safebyte.auth

import java.security.MessageDigest
import android.util.Base64

object PasswordHasher {
    fun hashPassword(password: String?): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest((password ?: "").toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}