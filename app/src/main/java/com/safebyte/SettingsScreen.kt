package com.safebyte

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val ALLERGENS = listOf("Gluten", "Lácteos", "Huevo", "Frutos secos", "Mariscos", "Soja")

@Composable
fun SettingsScreen(
    prefs: UserPrefs,
    emailLower: String,
    currentAllergens: Set<String>,
    onAllergensChanged: (Set<String>) -> Unit
) {
    var working by remember { mutableStateOf(currentAllergens) }
    var savedMsg by remember { mutableStateOf(false) }

    // Si cambian desde fuera (por ejemplo al cargar Firestore), refrescamos la UI
    LaunchedEffect(currentAllergens) {
        working = currentAllergens
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Configuración de usuario", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))

        if (emailLower.isNotBlank() && emailLower != "guest@local") {
            Text("Sesión: $emailLower", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }

        Text("Selecciona los alérgenos que deseas evitar.")
        Spacer(Modifier.height(12.dp))

        ALLERGENS.forEach { allergen ->
            val checked = allergen in working
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        allergen,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = checked,
                        onCheckedChange = { isChecked ->
                            working = if (isChecked) working + allergen else working - allergen
                            savedMsg = false
                        }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                // Guardado centralizado: SafeByteApp ya lo escribe a DataStore y Firestore
                onAllergensChanged(working)
                savedMsg = true
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Guardar preferencias") }

        if (savedMsg) {
            Spacer(Modifier.height(8.dp))
            Text("Preferencias guardadas con éxito.", color = MaterialTheme.colorScheme.primary)
        }
    }
}