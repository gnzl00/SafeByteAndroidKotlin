package com.safebyte

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ContactScreen() {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    val contactFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF1F1F1F),
        unfocusedTextColor = Color(0xFF1F1F1F),
        focusedPlaceholderColor = Color(0xFF5D6A62),
        unfocusedPlaceholderColor = Color(0xFF5D6A62),
        cursorColor = Color(0xFF1E5631),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = Color(0xFF1E5631),
        unfocusedBorderColor = Color(0xFFC2CEC4)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFFF4F7F6), Color(0xFFE9F5E9)))
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE9F5E9)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Contacta con Nosotros",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF1E5631),
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text("Tienes preguntas? Estamos aqui para ayudarte.")
            }
        }

        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; sent = false },
                    placeholder = { Text("Tu nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = contactFieldColors
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; sent = false },
                    placeholder = { Text("Tu correo") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = contactFieldColors
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it; sent = false },
                    placeholder = { Text("Tu mensaje") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    colors = contactFieldColors
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { sent = name.isNotBlank() && email.isNotBlank() && message.isNotBlank() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E5631)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Enviar")
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        when {
            sent -> Text("Mensaje enviado (demo local).", color = Color(0xFF1F6B39))
            name.isNotBlank() || email.isNotBlank() || message.isNotBlank() ->
                Text("Nota: este formulario en Android es demostracion local.", color = Color(0xFF4D4D4D))
        }
    }
}
