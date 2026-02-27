package com.safebyte

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.safebyte.auth.PasswordHasher
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AuthScreen(
    onLogin: (String) -> Unit,
    onSkip: () -> Unit
) {
    var mode by remember { mutableStateOf("login") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFEFF7F0), Color(0xFFF8FBF8))
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(1.6f)
                .fillMaxHeight(0.85f)
                .align(Alignment.TopEnd)
                .background(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF04450B), Color(0xFF40FF53))
                    ),
                    shape = CircleShape
                )
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val isCompact = maxWidth < 900.dp
            if (isCompact) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AuthFormCard(
                        mode = mode,
                        username = username,
                        email = email,
                        password = password,
                        loading = loading,
                        error = error,
                        onModeChange = {
                            mode = it
                            error = ""
                        },
                        onUsernameChange = { username = it },
                        onEmailChange = { email = it.lowercase() },
                        onPasswordChange = { password = it },
                        onSubmit = {
                            if (loading) return@AuthFormCard
                            loading = true
                            error = ""
                            scope.launch {
                                try {
                                    if (mode == "signup") {
                                        if (username.isBlank() || email.isBlank() || password.isBlank()) {
                                            error = "Completa todos los campos."
                                            return@launch
                                        }
                                        val emailLower = email.trim().lowercase()
                                        val existing = db.collection("users").document(emailLower).get().await()
                                        if (existing.exists()) {
                                            error = "El usuario con ese Email ya existe."
                                            return@launch
                                        }

                                        val now = Timestamp.now()
                                        val payload = mapOf(
                                            "username" to username.trim(),
                                            "email" to emailLower,
                                            "passwordHash" to PasswordHasher.hashPassword(password),
                                            "allergens" to emptyList<String>(),
                                            "createdAt" to now,
                                            "allergensUpdatedAt" to now
                                        )
                                        db.collection("users").document(emailLower).set(payload).await()
                                        onLogin(emailLower)
                                    } else {
                                        if (email.isBlank() || password.isBlank()) {
                                            error = "Completa todos los campos."
                                            return@launch
                                        }
                                        val emailLower = email.trim().lowercase()
                                        val doc = db.collection("users").document(emailLower).get().await()
                                        if (!doc.exists()) {
                                            error = "Credenciales invalidas (usuario no encontrado)."
                                            return@launch
                                        }
                                        val storedHash = doc.getString("passwordHash").orEmpty()
                                        val inputHash = PasswordHasher.hashPassword(password)
                                        if (storedHash != inputHash) {
                                            error = "Credenciales invalidas (contrasena incorrecta)."
                                            return@launch
                                        }
                                        onLogin(emailLower)
                                    }
                                } catch (_: Throwable) {
                                    error = "Error de conexion."
                                } finally {
                                    loading = false
                                }
                            }
                        },
                        onSkip = onSkip
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AuthPanel(
                        title = if (mode == "signup") "Ya eres uno de los nuestros?" else "Eres nuevo?",
                        subtitle = if (mode == "signup")
                            "Inicia sesion para inspirarte con multiples recetas"
                        else
                            "Unete a Food DNA y te hara la vida mas facil",
                        buttonText = if (mode == "signup") "Log in" else "Sign up",
                        onClick = {
                            mode = if (mode == "signup") "login" else "signup"
                            error = ""
                        },
                        modifier = Modifier.weight(1f)
                    )

                    AuthFormCard(
                        mode = mode,
                        username = username,
                        email = email,
                        password = password,
                        loading = loading,
                        error = error,
                        onModeChange = {
                            mode = it
                            error = ""
                        },
                        onUsernameChange = { username = it },
                        onEmailChange = { email = it.lowercase() },
                        onPasswordChange = { password = it },
                        onSubmit = {
                            if (loading) return@AuthFormCard
                            loading = true
                            error = ""
                            scope.launch {
                                try {
                                    if (mode == "signup") {
                                        if (username.isBlank() || email.isBlank() || password.isBlank()) {
                                            error = "Completa todos los campos."
                                            return@launch
                                        }
                                        val emailLower = email.trim().lowercase()
                                        val existing = db.collection("users").document(emailLower).get().await()
                                        if (existing.exists()) {
                                            error = "El usuario con ese Email ya existe."
                                            return@launch
                                        }
                                        val now = Timestamp.now()
                                        val payload = mapOf(
                                            "username" to username.trim(),
                                            "email" to emailLower,
                                            "passwordHash" to PasswordHasher.hashPassword(password),
                                            "allergens" to emptyList<String>(),
                                            "createdAt" to now,
                                            "allergensUpdatedAt" to now
                                        )
                                        db.collection("users").document(emailLower).set(payload).await()
                                        onLogin(emailLower)
                                    } else {
                                        if (email.isBlank() || password.isBlank()) {
                                            error = "Completa todos los campos."
                                            return@launch
                                        }
                                        val emailLower = email.trim().lowercase()
                                        val doc = db.collection("users").document(emailLower).get().await()
                                        if (!doc.exists()) {
                                            error = "Credenciales invalidas (usuario no encontrado)."
                                            return@launch
                                        }
                                        val storedHash = doc.getString("passwordHash").orEmpty()
                                        val inputHash = PasswordHasher.hashPassword(password)
                                        if (storedHash != inputHash) {
                                            error = "Credenciales invalidas (contrasena incorrecta)."
                                            return@launch
                                        }
                                        onLogin(emailLower)
                                    }
                                } catch (_: Throwable) {
                                    error = "Error de conexion."
                                } finally {
                                    loading = false
                                }
                            }
                        },
                        onSkip = onSkip,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthPanel(
    title: String,
    subtitle: String,
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.92f))
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
            ) {
                Text(buttonText)
            }
        }
    }
}

@Composable
private fun AuthFormCard(
    mode: String,
    username: String,
    email: String,
    password: String,
    loading: Boolean,
    error: String,
    onModeChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF1F1F1F),
        unfocusedTextColor = Color(0xFF1F1F1F),
        focusedLabelColor = Color(0xFF355242),
        unfocusedLabelColor = Color(0xFF4A4A4A),
        cursorColor = Color(0xFF1E5631),
        focusedContainerColor = Color(0xFFF0F0F0),
        unfocusedContainerColor = Color(0xFFF0F0F0),
        focusedBorderColor = Color(0xFF1E5631),
        unfocusedBorderColor = Color(0xFFD6D6D6)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onModeChange("login") }) {
                    Text("Login", color = if (mode == "login") Color(0xFF1E5631) else Color(0xFF777777))
                }
                TextButton(onClick = { onModeChange("signup") }) {
                    Text("Sign up", color = if (mode == "signup") Color(0xFF1E5631) else Color(0xFF777777))
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                if (mode == "signup") "Sign up" else "Login",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF444444),
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(14.dp))

            if (mode == "signup") {
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text("Username") },
                    singleLine = true,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = authFieldColors
                )
                Spacer(Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = authFieldColors
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = authFieldColors
            )

            if (error.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(error, color = Color(0xFFB00020))
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onSubmit,
                enabled = !loading,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF08A82E)),
                modifier = Modifier.width(170.dp)
            ) {
                Text(if (loading) "Cargando..." else if (mode == "signup") "Sign up" else "Login")
            }

            if (mode == "login") {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onSkip) {
                    Text("Skip Login")
                }
            }
        }
    }
}
