package com.safebyte

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
fun SettingsScreen(
    emailLower: String,
    currentAllergens: Set<String>,
    onAllergensChanged: (Set<String>) -> Unit
) {
    var working by remember { mutableStateOf(normalizeAllergenSet(currentAllergens)) }
    var statusText by remember { mutableStateOf("") }
    var statusColor by remember { mutableStateOf(Color(0xFF1F6B39)) }

    LaunchedEffect(currentAllergens) {
        working = normalizeAllergenSet(currentAllergens)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF4F7F6), Color(0xFFE9F5E9))))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE9F5E9)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Configuracion de usuario",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF1E5631),
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text("Selecciona los alergenos que deseas evitar.", color = Color(0xFF2E4B3A))
                if (emailLower.isBlank() || emailLower == "guest@local") {
                    Spacer(Modifier.height(4.dp))
                    Text("Modo invitado: se guarda localmente.", color = Color(0xFF8A6D3B))
                } else {
                    Spacer(Modifier.height(4.dp))
                    Text("Sesion: $emailLower", color = Color(0xFF4D4D4D))
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = when {
                maxWidth < 360.dp -> 1
                maxWidth < 600.dp -> 2
                else -> 3
            }
            val rows = remember(columns) { defaultAllergenCatalog.chunked(columns) }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(columns) { col ->
                            val allergen = row.getOrNull(col)
                            if (allergen == null) {
                                Spacer(Modifier.weight(1f))
                            } else {
                                val selected = allergen in working
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .clickable {
                                            working = if (selected) working - allergen else working + allergen
                                            statusText = ""
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) Color(0xFF1E5631) else Color.White
                                    ),
                                    border = BorderStroke(
                                        width = 2.dp,
                                        color = if (selected) Color(0xFF1E5631) else Color(0xFFCCCCCC)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = allergen,
                                            color = if (selected) Color.White else Color(0xFF2E2E2E),
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                onAllergensChanged(working)
                statusText = "Preferencias guardadas con exito."
                statusColor = Color(0xFF1F6B39)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E5631)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Guardar preferencias")
        }

        if (statusText.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(statusText, color = statusColor)
        }
    }
}
