package com.safebyte

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class FoodItem(
    val name: String,
    val imageResName: String,
    val ingredients: String,
    val recipe: String,
    val allergens: List<String>,
)

@Composable
fun MealsScreen(prefs: UserPrefs) {
    val selectedAllergens by prefs.allergens.collectAsState(initial = emptySet())
    var query by remember { mutableStateOf("") }
    var hideUnsafe by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<FoodItem?>(null) }

    val meals = remember { MealsData.meals }

    val filtered = remember(query, hideUnsafe, selectedAllergens) {
        meals.asSequence()
            .filter { it.name.contains(query, ignoreCase = true) }
            .filter { item ->
                if (!hideUnsafe) true
                else item.allergens.none { it in selectedAllergens }
            }
            .toList()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Comidas recomendadas",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Buscar comidas...") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = hideUnsafe,
                onClick = { hideUnsafe = !hideUnsafe },
                label = { Text("Ocultar con mis alergenos") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
            if (selectedAllergens.isEmpty()) {
                AssistChip(
                    onClick = { /* no-op */ },
                    label = { Text("Configura alergenos para filtrar") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Resultados: ${filtered.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered) { item ->
                MealRow(item = item, onClick = { selected = item })
            }
        }
    }

    if (selected != null) {
        MealDetailDialog(
            item = selected!!,
            selectedAllergens = selectedAllergens,
            onDismiss = { selected = null }
        )
    }
}

@Composable
private fun MealRow(item: FoodItem, onClick: () -> Unit) {
    val resId = drawableId(item.imageResName)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(Modifier.padding(12.dp)) {
            if (resId != 0) {
                Image(
                    painter = painterResource(resId),
                    contentDescription = item.name,
                    modifier = Modifier.size(72.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Alergenos: " + (if (item.allergens.isEmpty()) "Ninguno" else item.allergens.joinToString()),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun MealDetailDialog(item: FoodItem, selectedAllergens: Set<String>, onDismiss: () -> Unit) {
    val unsafe = item.allergens.any { it in selectedAllergens } && selectedAllergens.isNotEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
        title = { Text(item.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val resId = drawableId(item.imageResName)
                if (resId != 0) {
                    Image(
                        painter = painterResource(resId),
                        contentDescription = item.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
                if (unsafe) {
                    Text(
                        "Contiene alergenos marcados por ti: " +
                            item.allergens.filter { it in selectedAllergens }.joinToString(),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text("Ingredientes: ${item.ingredients}")
                Text("Receta:")
                Text("Alergenos: " + (if (item.allergens.isEmpty()) "Ninguno" else item.allergens.joinToString()))
            }
        }
    )
}
