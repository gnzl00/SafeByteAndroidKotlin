package com.safebyte

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class FoodItem(
    val name: String,
    val imageResName: String,
    val ingredients: String,
    val recipe: String,
    val allergens: List<String>
)

@Composable
fun MealsScreen(prefs: UserPrefs) {
    val selectedAllergens by prefs.allergens.collectAsState(initial = emptySet())
    val meals = remember { MealsData.meals }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<FoodItem?>(null) }

    val filteredMeals = remember(query, selectedAllergens) {
        val normalizedUserAllergens = normalizeAllergenSet(selectedAllergens)
        meals.asSequence()
            .filter { it.name.contains(query, ignoreCase = true) }
            .filter { item -> !hasAllergenConflict(item.allergens, normalizedUserAllergens) }
            .toList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFFD0F2D1), Color(0xFFF0F9F4))
                )
            )
            .padding(14.dp)
    ) {
        Text(
            "Comidas Recomendadas",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF1E5631),
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Buscar comidas...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(25.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF1F1F1F),
                unfocusedTextColor = Color(0xFF1F1F1F),
                focusedPlaceholderColor = Color(0xFF60796A),
                unfocusedPlaceholderColor = Color(0xFF60796A),
                cursorColor = Color(0xFF1E5631),
                focusedBorderColor = Color(0xFF1E5631),
                unfocusedBorderColor = Color(0xFFCCCCCC),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        Spacer(Modifier.height(8.dp))
        Text(
            "Resultados: ${filteredMeals.size}",
            color = Color(0xFF4D4D4D)
        )

        Spacer(Modifier.height(8.dp))
        if (filteredMeals.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "No hay comidas visibles con los filtros actuales.",
                    color = Color(0xFF444444)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(220.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredMeals) { item ->
                    MealCard(item = item, onClick = { selected = item })
                }
            }
        }
    }

    selected?.let { item ->
        MealModal(
            item = item,
            selectedAllergens = selectedAllergens,
            onDismiss = { selected = null }
        )
    }
}

@Composable
private fun MealCard(item: FoodItem, onClick: () -> Unit) {
    val resId = drawableId(item.imageResName)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (resId != 0) {
                Image(
                    painter = painterResource(resId),
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Text(
                text = item.name,
                color = Color(0xFF1E5631),
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MealModal(
    item: FoodItem,
    selectedAllergens: Set<String>,
    onDismiss: () -> Unit
) {
    val normalizedUserKeys = normalizeAllergenSet(selectedAllergens).map { allergenKey(it) }.toSet()
    val conflicts = item.allergens.filter { allergenKey(it) in normalizedUserKeys }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        title = { Text(item.name, color = Color(0xFF1E5631), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val resId = drawableId(item.imageResName)
                if (resId != 0) {
                    Image(
                        painter = painterResource(resId),
                        contentDescription = item.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Text("Ingredientes: ${item.ingredients}")
                Text("Receta: ${item.recipe}")
                Text("Alergenos: ${if (item.allergens.isEmpty()) "Ninguno" else item.allergens.joinToString()}")
                if (conflicts.isNotEmpty()) {
                    Text(
                        "Aviso: contiene alergenos seleccionados por ti (${conflicts.joinToString()}).",
                        color = Color(0xFF973333),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    )
}
