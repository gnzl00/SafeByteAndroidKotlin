package com.safebyte

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ContactScreen() {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Contacta con nosotros", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("¿Tienes preguntas? ¡Estamos aquí para ayudarte!")

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(name, { name = it; sent = false }, label = { Text("Tu nombre") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(email, { email = it; sent = false }, label = { Text("Tu correo") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            message,
            { message = it; sent = false },
            label = { Text("Tu mensaje") },
            modifier = Modifier.fillMaxWidth().height(140.dp)
        )

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { sent = name.isNotBlank() && email.isNotBlank() && message.isNotBlank() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Enviar") }

        if (sent) {
            Spacer(Modifier.height(8.dp))
            Text("Mensaje enviado (demo local).", color = MaterialTheme.colorScheme.primary)
        } else if (name.isNotBlank() || email.isNotBlank() || message.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text("Nota: en móvil este formulario es solo demostración. Conecta un backend para enviar emails.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
