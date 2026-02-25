package com.safebyte

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private data class IANutriMode(val key: String, val label: String)
private enum class StatusKind { INFO, SUCCESS, WARNING, ERROR }
private data class UiStatus(val text: String = "", val kind: StatusKind = StatusKind.INFO)

private val modes = listOf(
    IANutriMode("rapido-basico", "Rapido y basico"),
    IANutriMode("cena-ligera", "Cena ligera"),
    IANutriMode("menu-semanal", "Menu semanal")
)

private val IaBgTop = Color(0xFFEAF3EC)
private val IaBgBottom = Color(0xFFF4F7F6)
private val IaSurface = Color(0xFFF7FBF8)
private val IaBorder = Color(0xFFD8E2DC)
private val IaPrimary = Color(0xFF1E5631)
private val IaSubtle = Color(0xFF4F5F55)
private val IaSuccess = Color(0xFF1F6B39)
private val IaWarning = Color(0xFF9A5A00)
private val IaError = Color(0xFF973333)

private fun modeLabel(mode: String): String = when (mode) {
    "rapido-basico" -> "Rapido y basico"
    "cena-ligera" -> "Cena ligera"
    "menu-semanal" -> "Menu semanal"
    else -> "Personalizado"
}

private fun modeFromLabel(label: String): String {
    val l = label.lowercase()
    return when {
        l.contains("rapido") -> "rapido-basico"
        l.contains("cena") -> "cena-ligera"
        l.contains("semanal") -> "menu-semanal"
        else -> ""
    }
}

private fun statusColor(kind: StatusKind): Color = when (kind) {
    StatusKind.INFO -> Color(0xFF4A6352)
    StatusKind.SUCCESS -> IaSuccess
    StatusKind.WARNING -> IaWarning
    StatusKind.ERROR -> IaError
}

private fun sanitizeReformulation(text: String): String {
    val blocked = listOf("modelo", "gpt", "openai", "enfoque:", "notas:")
    return text.replace("\r", "\n")
        .split('\n')
        .map { it.trim().trimStart('-', '*', '•').trim() }
        .filter { it.isNotBlank() }
        .filter { line -> blocked.none { line.lowercase().contains(it) } }
        .joinToString(" ")
        .trim()
}

private fun List<String>.normalizeTextList(limit: Int, fallback: List<String> = emptyList()): List<String> {
    val values = linkedSetOf<String>()
    for (value in this) {
        val clean = value.trim()
        if (clean.isNotEmpty()) values.add(clean)
        if (values.size >= limit) break
    }
    if (values.isNotEmpty()) return values.toList()
    return fallback.map { it.trim() }.filter { it.isNotEmpty() }.take(limit)
}

private fun IANutriRecipeSuggestion.normalizedSuggestion(): IANutriRecipeSuggestion {
    return IANutriRecipeSuggestion(
        title = title.trim().ifBlank { "Plato sugerido" },
        description = description.trim().ifBlank { "Sin descripcion adicional." },
        estimatedTime = estimatedTime.trim().ifBlank { "20-30 min" },
        difficulty = difficulty.trim().ifBlank { "Media" },
        ingredients = ingredients.normalizeTextList(20, listOf("Ajusta ingredientes a tu despensa.")),
        steps = steps.normalizeTextList(20, listOf("Cocina con fuego medio y ajusta condimentos.")),
        allergensDetected = allergensDetected.normalizeTextList(8),
        safeSubstitutions = safeSubstitutions.normalizeTextList(8),
        allergyWarning = allergyWarning.trim()
    )
}

private fun formatHistoryDate(value: String?): String {
    if (value.isNullOrBlank()) return "Sin fecha"
    return try {
        OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    } catch (_: Throwable) {
        value.take(16)
    }
}

private fun Throwable.toApiMessage(): String {
    if (this is HttpException) {
        val body = try { response()?.errorBody()?.string().orEmpty() } catch (_: Throwable) { "" }
        val parsed = extractApiErrorMessage(body)
        if (parsed.isNotBlank()) return parsed
        return "Error ${code()} en la API IANutri."
    }
    if (this is IOException) return "No se pudo conectar al backend IANutri."
    return message?.trim().takeUnless { it.isNullOrBlank() } ?: "Error inesperado."
}

private fun extractApiErrorMessage(body: String): String {
    if (body.isBlank()) return ""
    val regex = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
    val match = regex.find(body)?.groupValues?.getOrNull(1)?.trim()
    if (!match.isNullOrBlank()) return match
    return body.trim().replace("\n", " ").take(220)
}

@Composable
fun IANutriScreen(
    email: String,
    allergens: Set<String>
) {
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val normalizedAllergens = remember(allergens) { allergens.map { it.trim() }.filter { it.isNotEmpty() }.sorted() }
    val apiEmail = remember(email) {
        val e = email.trim().lowercase()
        if (e.isBlank() || e == "guest@local") "" else e
    }
    val canUseHistory = apiEmail.isNotBlank()

    var input by rememberSaveable { mutableStateOf("") }
    var selectedMode by rememberSaveable { mutableStateOf("") }
    var reformulatedPrompt by rememberSaveable { mutableStateOf("") }
    var notes by remember { mutableStateOf<List<String>>(emptyList()) }

    var summary by remember { mutableStateOf("") }
    var warnings by remember { mutableStateOf<List<String>>(emptyList()) }
    var substitutions by remember { mutableStateOf<List<String>>(emptyList()) }
    var suggestions by remember { mutableStateOf<List<IANutriRecipeSuggestion>>(emptyList()) }
    var activeHistoryId by remember { mutableStateOf("") }

    var assistantIntro by remember { mutableStateOf("") }
    var assistantItems by remember { mutableStateOf<List<String>>(emptyList()) }
    var assistantSteps by remember { mutableStateOf<List<String>>(emptyList()) }
    var assistantSafety by remember { mutableStateOf<List<String>>(emptyList()) }
    var showAssistant by remember { mutableStateOf(false) }

    var history by remember { mutableStateOf<List<IANutriHistoryItem>>(emptyList()) }
    var historyExpanded by rememberSaveable { mutableStateOf(false) }

    var plannerStatus by remember { mutableStateOf(UiStatus()) }
    var resultStatus by remember { mutableStateOf(UiStatus()) }
    var assistantStatus by remember { mutableStateOf(UiStatus()) }
    var historyStatus by remember { mutableStateOf(UiStatus()) }

    var reformulating by remember { mutableStateOf(false) }
    var generating by remember { mutableStateOf(false) }
    var loadingAssistant by remember { mutableStateOf(false) }
    var loadingHistory by remember { mutableStateOf(false) }
    var clearingHistory by remember { mutableStateOf(false) }

    suspend fun refreshHistory(showError: Boolean) {
        if (!canUseHistory) {
            history = emptyList()
            historyStatus = UiStatus("Inicia sesion para guardar historial IA.", StatusKind.INFO)
            return
        }
        loadingHistory = true
        try {
            history = withContext(Dispatchers.IO) { IANutriNetwork.api.getHistory(apiEmail).history }
            historyStatus = UiStatus()
        } catch (t: Throwable) {
            history = emptyList()
            if (showError) {
                historyStatus = UiStatus("No se pudo cargar historial: ${t.toApiMessage()}", StatusKind.ERROR)
            }
        } finally {
            loadingHistory = false
        }
    }

    suspend fun reformulateIfPossible(showLoadingText: Boolean = true): Boolean {
        val trimmedInput = input.trim()
        if (trimmedInput.isEmpty()) {
            plannerStatus = UiStatus("Escribe ingredientes o una idea antes de reformular.", StatusKind.ERROR)
            return false
        }
        if (selectedMode.isBlank()) {
            plannerStatus = UiStatus("Selecciona un modo para reformular.", StatusKind.ERROR)
            return false
        }

        reformulating = true
        if (showLoadingText) plannerStatus = UiStatus("Reformulando peticion...", StatusKind.INFO)
        return try {
            val response = withContext(Dispatchers.IO) {
                IANutriNetwork.api.reformulate(
                    IANutriReformulateRequest(
                        email = apiEmail,
                        userInput = trimmedInput,
                        option = selectedMode,
                        allergens = normalizedAllergens
                    )
                )
            }
            reformulatedPrompt = sanitizeReformulation(response.reformulatedPrompt)
            notes = response.notes.normalizeTextList(5)
            if (reformulatedPrompt.isBlank()) {
                plannerStatus = UiStatus("No se pudo reformular la peticion.", StatusKind.ERROR)
                false
            } else {
                plannerStatus = UiStatus("Peticion reformulada. Ya puedes generar sugerencias.", StatusKind.SUCCESS)
                true
            }
        } catch (t: Throwable) {
            reformulatedPrompt = ""
            notes = emptyList()
            plannerStatus = UiStatus("Error al reformular: ${t.toApiMessage()}", StatusKind.ERROR)
            false
        } finally {
            reformulating = false
        }
    }

    suspend fun generateAll() {
        if (input.trim().isBlank()) {
            plannerStatus = UiStatus("Escribe ingredientes o una idea para generar opciones.", StatusKind.ERROR)
            return
        }
        if (selectedMode.isBlank()) {
            plannerStatus = UiStatus("Selecciona primero una orientacion.", StatusKind.ERROR)
            return
        }
        if (reformulatedPrompt.isBlank()) {
            val ok = reformulateIfPossible(false)
            if (!ok) return
        }

        generating = true
        resultStatus = UiStatus("Generando sugerencias...", StatusKind.INFO)
        try {
            val response = withContext(Dispatchers.IO) {
                IANutriNetwork.api.generateSuggestions(
                    IANutriGenerateSuggestionsRequest(
                        email = apiEmail,
                        userInput = input.trim(),
                        option = modeLabel(selectedMode),
                        reformulatedPrompt = reformulatedPrompt,
                        allergens = normalizedAllergens
                    )
                )
            }
            summary = response.summary.trim()
            warnings = response.globalWarnings.normalizeTextList(8)
            substitutions = response.generalSubstitutions.normalizeTextList(10)
            suggestions = response.suggestions.map { it.normalizedSuggestion() }
            activeHistoryId = response.historyId.trim()
            showAssistant = false
            if (canUseHistory) refreshHistory(false)
            resultStatus = if (suggestions.isEmpty()) {
                UiStatus("No hubo sugerencias para esta peticion.", StatusKind.WARNING)
            } else {
                UiStatus("Pulsa una sugerencia para abrir el asistente de cocina.", StatusKind.SUCCESS)
            }
        } catch (t: Throwable) {
            resultStatus = UiStatus("No se pudieron generar sugerencias: ${t.toApiMessage()}", StatusKind.ERROR)
        } finally {
            generating = false
        }
    }

    suspend fun openAssistant(recipe: IANutriRecipeSuggestion) {
        showAssistant = true
        assistantIntro = "Preparando guia para ${recipe.title}..."
        assistantItems = emptyList()
        assistantSteps = emptyList()
        assistantSafety = emptyList()
        loadingAssistant = true
        assistantStatus = UiStatus("Generando asistente de cocina...", StatusKind.INFO)
        try {
            val response = withContext(Dispatchers.IO) {
                IANutriNetwork.api.cookingAssistant(
                    IANutriCookingAssistantRequest(
                        email = apiEmail,
                        allergens = normalizedAllergens,
                        historyId = activeHistoryId,
                        recipe = recipe
                    )
                )
            }
            assistantIntro = response.intro.ifBlank { "Vamos a cocinar ${recipe.title} paso a paso." }
            assistantItems = response.requiredItems.normalizeTextList(14, recipe.ingredients)
            assistantSteps = response.stepByStep.normalizeTextList(16, recipe.steps)
            assistantSafety = response.safetyNotes.normalizeTextList(
                10,
                listOf("Evita contaminacion cruzada con: ${normalizedAllergens.joinToString().ifBlank { "sin alergenos configurados" }}.")
            )
            assistantStatus = UiStatus("Guia lista. Puedes cocinar paso a paso.", StatusKind.SUCCESS)
        } catch (t: Throwable) {
            assistantIntro = "Sigue estos pasos base para preparar ${recipe.title}."
            assistantItems = recipe.ingredients.normalizeTextList(14)
            assistantSteps = recipe.steps.normalizeTextList(16)
            assistantSafety = listOf(
                "Confirma etiquetas y evita trazas de: ${normalizedAllergens.joinToString().ifBlank { "sin alergenos configurados" }}."
            )
            assistantStatus = UiStatus("No se pudo generar guia IA: ${t.toApiMessage()}", StatusKind.ERROR)
        } finally {
            loadingAssistant = false
        }
    }

    fun restoreHistoryItem(item: IANutriHistoryItem) {
        activeHistoryId = item.id
        selectedMode = modeFromLabel(item.option)
        input = item.userInput
        reformulatedPrompt = sanitizeReformulation(item.reformulatedPrompt)
        notes = emptyList()
        summary = item.summary
        warnings = item.globalWarnings.normalizeTextList(8)
        substitutions = item.generalSubstitutions.normalizeTextList(10)
        suggestions = item.suggestions.map { it.normalizedSuggestion() }
        showAssistant = false
        resultStatus = UiStatus("Conversacion recuperada del historial.", StatusKind.SUCCESS)
    }

    LaunchedEffect(apiEmail) { refreshHistory(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(IaBgTop, IaBgBottom)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = IaSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, IaBorder)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SAFEBYTE AI - MENUS SEGUROS", color = IaPrimary, fontWeight = FontWeight.Bold)
                    Text("Asistente de Menus Seguros", style = MaterialTheme.typography.headlineSmall, color = IaPrimary, fontWeight = FontWeight.Bold)
                    Text("Describe lo que quieres comer o los ingredientes que tienes. El asistente usa tus alergenos guardados.", color = IaSubtle)
                    Text("Tus alergenos", color = IaPrimary, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (normalizedAllergens.isEmpty()) {
                            Text("Sin alergenos guardados", color = IaSubtle)
                        } else {
                            normalizedAllergens.forEach { value ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE7ECE8)),
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Text(value, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                }
                            }
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = IaSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, IaBorder)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cuentanos tu plan", style = MaterialTheme.typography.headlineSmall, color = IaPrimary, fontWeight = FontWeight.Bold)
                    Text("Mas detalle = mejores sugerencias", color = IaSubtle)
                    Text("Que tienes en la nevera o que te apetece?", color = IaPrimary, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = input,
                        onValueChange = {
                            input = it.take(600)
                            reformulatedPrompt = ""
                            notes = emptyList()
                            plannerStatus = UiStatus()
                        },
                        modifier = Modifier.fillMaxWidth().height(148.dp),
                        placeholder = { Text("Ej: Tengo pollo, arroz y verduras. Quiero algo rapido.") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFFDFEFE),
                            unfocusedContainerColor = Color(0xFFFDFEFE),
                            focusedBorderColor = IaPrimary,
                            unfocusedBorderColor = Color(0xFFC6D2C8)
                        )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        modes.forEach { mode ->
                            FilterChip(
                                selected = selectedMode == mode.key,
                                enabled = !reformulating && !generating,
                                onClick = {
                                    selectedMode = mode.key
                                    scope.launch { reformulateIfPossible() }
                                },
                                label = { Text(mode.label) },
                                border = BorderStroke(1.dp, if (selectedMode == mode.key) IaPrimary else Color(0xFFAFC2B2)),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IaPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFFF1F7F3),
                                    labelColor = IaPrimary
                                )
                            )
                        }
                    }
                    if (reformulatedPrompt.isNotBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FBF8)),
                            border = BorderStroke(1.dp, Color(0xFFD7E1D8)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Asi he entendido tu peticion", color = IaPrimary, fontWeight = FontWeight.SemiBold)
                                Text(reformulatedPrompt, color = Color(0xFF2E4B3A))
                                notes.forEach { Text("* $it", color = Color(0xFF3F5A49)) }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${input.length}/600", color = Color(0xFF5D6A62))
                        Button(
                            onClick = { scope.launch { generateAll() } },
                            enabled = !generating && !reformulating,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IaPrimary,
                                disabledContainerColor = Color(0xFF8AA18F)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (generating) {
                                CircularProgressIndicator(modifier = Modifier.height(16.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(Modifier.padding(4.dp))
                                Text("Generando...")
                            } else {
                                Text("Generar sugerencias")
                            }
                        }
                    }
                    if (plannerStatus.text.isNotBlank()) Text(plannerStatus.text, color = statusColor(plannerStatus.kind))
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = IaSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, IaBorder)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Resultado", style = MaterialTheme.typography.headlineSmall, color = IaPrimary, fontWeight = FontWeight.Bold)
                    if (summary.isBlank() && suggestions.isEmpty() && warnings.isEmpty() && substitutions.isEmpty()) {
                        Text("Aqui apareceran las sugerencias personalizadas.", color = IaSubtle)
                    }
                    if (summary.isNotBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF5F0)),
                            border = BorderStroke(1.dp, Color(0xFFD7E1D8)),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text(summary, color = Color(0xFF355140), modifier = Modifier.padding(10.dp)) }
                    }
                    warnings.forEach { Text("Aviso: $it", color = IaWarning) }
                    substitutions.forEach { Text("Sustitucion sugerida: $it", color = IaSuccess) }
                    suggestions.forEachIndexed { idx, recipe ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { scope.launch { openAssistant(recipe) } },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFEFD)),
                            border = BorderStroke(1.dp, Color(0xFFD4DFD5)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${idx + 1}. ${recipe.title}", color = IaPrimary, fontWeight = FontWeight.SemiBold)
                                Text("Tiempo: ${recipe.estimatedTime} | Dificultad: ${recipe.difficulty}", color = Color(0xFF45624F))
                                Text(recipe.description, color = IaSubtle)
                                Text("Ingredientes: ${recipe.ingredients.take(8).joinToString()}", color = IaSubtle)
                                Text("Pasos clave: ${recipe.steps.take(3).joinToString(" | ")}", color = IaSubtle)
                                if (recipe.allergyWarning.isNotBlank()) Text(recipe.allergyWarning, color = IaWarning)
                            }
                        }
                    }
                    if (resultStatus.text.isNotBlank()) Text(resultStatus.text, color = statusColor(resultStatus.kind))
                }
            }

            if (showAssistant) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = IaSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFFC7D8CB))
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Asistente de cocina", style = MaterialTheme.typography.headlineSmall, color = IaPrimary, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { showAssistant = false }) { Text("Cerrar", color = IaPrimary) }
                        }
                        Text(assistantIntro.ifBlank { "Preparando guia..." }, color = IaSubtle)
                        Text("Necesitaras", color = IaPrimary, fontWeight = FontWeight.SemiBold)
                        assistantItems.forEach { Text("* $it", color = IaSubtle) }
                        Text("Paso a paso", color = IaPrimary, fontWeight = FontWeight.SemiBold)
                        assistantSteps.forEachIndexed { idx, step -> Text("${idx + 1}. $step", color = IaSubtle) }
                        Text("Notas de seguridad", color = IaPrimary, fontWeight = FontWeight.SemiBold)
                        assistantSafety.forEach { Text("* $it", color = IaSubtle) }
                        if (loadingAssistant) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.height(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.padding(4.dp))
                                Text("Generando guia...", color = IaSubtle)
                            }
                        }
                        if (assistantStatus.text.isNotBlank()) Text(assistantStatus.text, color = statusColor(assistantStatus.kind))
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = IaSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, IaBorder)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Historial de conversaciones", style = MaterialTheme.typography.titleLarge, color = IaPrimary, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { historyExpanded = !historyExpanded }) {
                            Text(if (historyExpanded) "Ocultar" else "Mostrar", color = IaPrimary)
                        }
                    }
                    if (historyExpanded) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { scope.launch { refreshHistory(true) } },
                                enabled = canUseHistory && !loadingHistory
                            ) { Text(if (loadingHistory) "Cargando..." else "Recargar") }

                            Button(
                                onClick = {
                                    scope.launch {
                                        if (!canUseHistory) {
                                            historyStatus = UiStatus("Debes iniciar sesion para borrar historial.", StatusKind.ERROR)
                                            return@launch
                                        }
                                        clearingHistory = true
                                        historyStatus = UiStatus("Borrando historial...", StatusKind.INFO)
                                        try {
                                            withContext(Dispatchers.IO) { IANutriNetwork.api.deleteHistory(apiEmail) }
                                            history = emptyList()
                                            activeHistoryId = ""
                                            historyStatus = UiStatus("Historial eliminado correctamente.", StatusKind.SUCCESS)
                                        } catch (t: Throwable) {
                                            historyStatus = UiStatus("No se pudo borrar historial: ${t.toApiMessage()}", StatusKind.ERROR)
                                        } finally {
                                            clearingHistory = false
                                        }
                                    }
                                },
                                enabled = canUseHistory && history.isNotEmpty() && !clearingHistory
                            ) {
                                Text(if (clearingHistory) "Borrando..." else "Borrar historial")
                            }
                        }

                        if (!canUseHistory) {
                            Text("Inicia sesion con cuenta real para guardar historial.", color = IaSubtle)
                        } else if (loadingHistory) {
                            Text("Cargando historial...", color = IaSubtle)
                        } else if (history.isEmpty()) {
                            Text("Aun no hay conversaciones guardadas.", color = IaSubtle)
                        } else {
                            history.forEach { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { restoreHistoryItem(item) },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFEFE)),
                                    border = BorderStroke(1.dp, Color(0xFFD5DFD7)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(if (item.option.isNotBlank()) "Modo: ${item.option}" else "Conversacion", color = IaPrimary, fontWeight = FontWeight.SemiBold)
                                        Text("Pedido: ${item.userInput.ifBlank { "Sin texto" }}", color = IaSubtle)
                                        Text("Resumen: ${item.summary.ifBlank { "Sin resumen" }}", color = IaSubtle)
                                        Text("${formatHistoryDate(item.createdAtUtc)} - ${item.suggestions.size} sugerencia(s)", color = Color(0xFF45624F))
                                    }
                                }
                            }
                        }
                        if (historyStatus.text.isNotBlank()) Text(historyStatus.text, color = statusColor(historyStatus.kind))
                    }
                }
            }
        }
    }
}
