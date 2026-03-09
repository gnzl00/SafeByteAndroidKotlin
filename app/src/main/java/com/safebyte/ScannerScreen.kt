package com.safebyte

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun ScannerScreen(
    userAllergens: Set<String>
) {
    val logTag = "ScannerScreen"
    val ctx = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    var manualBarcode by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var product by remember { mutableStateOf<Product?>(null) }
    var scanResetToken by remember { mutableStateOf(0) }
    var activeRequestToken by remember { mutableStateOf(0) }
    var activeRequestJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val userOff = remember(userAllergens) { mapUserAllergensToOff(userAllergens) }

    fun startLookup(rawValue: String) {
        val normalized = normalizeBarcode(rawValue)
        if (!isLikelyBarcode(normalized)) {
            error = "Codigo de barras invalido. Debe tener entre 8 y 14 digitos."
            return
        }

        activeRequestJob?.cancel()
        activeRequestToken += 1
        val requestToken = activeRequestToken
        loading = true
        error = null
        product = null
        manualBarcode = normalized
        barcode = normalized

        activeRequestJob = scope.launch {
            try {
                val result = withTimeout(SCANNER_LOOKUP_TIMEOUT_MS) {
                    OpenFoodFacts.lookupProduct(normalized)
                }

                if (requestToken != activeRequestToken) return@launch
                when (result) {
                    is OpenFoodFactsLookupResult.Found -> {
                        product = result.product
                    }

                    OpenFoodFactsLookupResult.NotFound -> {
                        error = "Producto no encontrado."
                    }

                    is OpenFoodFactsLookupResult.NetworkError -> {
                        Log.w(
                            logTag,
                            "OpenFoodFacts IO para barcode=$normalized: ${rootCause(result.error).javaClass.simpleName}",
                            result.error
                        )
                        error = networkIssueMessage(result.error)
                    }

                    is OpenFoodFactsLookupResult.HttpError -> {
                        Log.w(logTag, "OpenFoodFacts HTTP ${result.code} para barcode=$normalized")
                        error = when (result.code) {
                            404 -> "Producto no encontrado."
                            429 -> "Demasiadas consultas seguidas. Espera unos segundos."
                            in 500..599 -> "Open Food Facts no responde ahora mismo. Intentalo de nuevo."
                            else -> "No se pudo consultar el producto (HTTP ${result.code})."
                        }
                    }

                    is OpenFoodFactsLookupResult.UnknownError -> {
                        Log.e(
                            logTag,
                            "Fallo inesperado consultando OpenFoodFacts para barcode=$normalized",
                            result.error
                        )
                        error = "No se pudo consultar el producto en este momento."
                    }
                }
            } catch (_: TimeoutCancellationException) {
                if (requestToken == activeRequestToken) {
                    error = "La consulta ha tardado demasiado. Intentalo de nuevo."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (requestToken == activeRequestToken) {
                    Log.e(logTag, "Fallo inesperado consultando OpenFoodFacts para barcode=$normalized", e)
                    error = if (e is IOException) {
                        networkIssueMessage(e)
                    } else {
                        "No se pudo consultar el producto en este momento."
                    }
                }
            } finally {
                if (requestToken == activeRequestToken) {
                    loading = false
                }
            }
        }
    }

    fun clearScannerState() {
        activeRequestJob?.cancel()
        activeRequestToken += 1
        barcode = null
        product = null
        error = null
        loading = false
        manualBarcode = ""
        scanResetToken += 1
    }

    DisposableEffect(Unit) {
        onDispose {
            activeRequestJob?.cancel()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        val previewHeight = when {
            maxHeight < 700.dp -> 220.dp
            maxHeight < 900.dp -> 270.dp
            else -> 320.dp
        }
        val isNarrow = maxWidth < 420.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("Escaneo codigo de barras", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))

            if (!hasCameraPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F7F6)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Para escanear con camara, concede permiso de camara.")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Conceder permiso de camara")
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(previewHeight),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    CameraBarcodePreview(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .clip(RoundedCornerShape(14.dp)),
                        resetToken = scanResetToken,
                        onBarcode = { value ->
                            if (product == null && !loading) {
                                val normalized = normalizeBarcode(value)
                                if (isLikelyBarcode(normalized) && normalized != barcode) {
                                    startLookup(normalized)
                                }
                            }
                        }
                    )
                }
                Spacer(Modifier.height(6.dp))
                if (product == null && !loading) {
                    ScannerLiveIndicator()
                } else {
                    ScannerPausedIndicator()
                }
                Spacer(Modifier.height(4.dp))
                Text("Consejo: si detecta varios codigos, se quedara con el primero.")
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = manualBarcode,
                onValueChange = { manualBarcode = it },
                label = { Text("O introduce el codigo manualmente") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF1F1F1F),
                    unfocusedTextColor = Color(0xFF1F1F1F),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(Modifier.height(8.dp))

            if (isNarrow) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            startLookup(manualBarcode)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Buscar producto") }

                    OutlinedButton(
                        onClick = {
                            clearScannerState()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Limpiar") }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            startLookup(manualBarcode)
                        }
                    ) { Text("Buscar producto") }

                    OutlinedButton(
                        onClick = {
                            clearScannerState()
                        }
                    ) { Text("Limpiar") }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }

            if (barcode != null) {
                Spacer(Modifier.height(8.dp))
                Text("Codigo: $barcode", style = MaterialTheme.typography.bodySmall)
            }

            product?.let { p ->
                Spacer(Modifier.height(8.dp))

                val productAllergens = remember(p.allergensTags, p.allergens) {
                    extractOffAllergens(p.allergensTags, p.allergens)
                }
                val conflicts = remember(productAllergens, userOff) { productAllergens.intersect(userOff) }

                val nutr = p.nutriments
                val kcal100g = nutrToText(nutr?.get("energy-kcal_100g"))
                val fat100g = nutrToText(nutr?.get("fat_100g"))
                val carbs100g = nutrToText(nutr?.get("carbohydrates_100g"))
                val sugars100g = nutrToText(nutr?.get("sugars_100g"))
                val proteins100g = nutrToText(nutr?.get("proteins_100g"))
                val salt100g = nutrToText(nutr?.get("salt_100g"))

                ProductCard(
                    product = p,
                    productAllergens = productAllergens,
                    conflicts = conflicts,
                    hasUserAllergens = userOff.isNotEmpty(),
                    kcal100g = kcal100g,
                    fat100g = fat100g,
                    carbs100g = carbs100g,
                    sugars100g = sugars100g,
                    proteins100g = proteins100g,
                    salt100g = salt100g
                )
            }
        }
    }
}

@Composable
private fun ScannerLiveIndicator() {
    val pulse = rememberInfiniteTransition(label = "scannerPulse")
        .animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scannerPulseAlpha"
        )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFF08A82E).copy(alpha = pulse.value))
        )
        Text("Escaneando en vivo", color = Color(0xFF2D4A39))
    }
}

@Composable
private fun ScannerPausedIndicator() {
    val pulse = rememberInfiniteTransition(label = "scannerPausedPulse")
        .animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scannerPausedAlpha"
        )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF9800).copy(alpha = pulse.value))
        )
        Text("Producto detectado", color = Color(0xFF2D4A39))
    }
}

@Composable
private fun ProductCard(
    product: Product,
    productAllergens: Set<String>,
    conflicts: Set<String>,
    hasUserAllergens: Boolean,
    kcal100g: String?,
    fat100g: String?,
    carbs100g: String?,
    sugars100g: String?,
    proteins100g: String?,
    salt100g: String?
) {
    val allergenText = product.allergens ?: "No especificado"
    val detectedInSpanish = remember(productAllergens) { translateOffAllergensToSpanish(productAllergens) }
    val conflictsInSpanish = remember(conflicts) { translateOffAllergensToSpanish(conflicts) }

    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(product.productName ?: "Nombre desconocido", style = MaterialTheme.typography.titleLarge)

            if (!product.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.productName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }

            if (!hasUserAllergens) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Text(
                        modifier = Modifier.padding(12.dp),
                        text = "No tienes alergenos configurados. Configuralos para recibir alertas personalizadas.",
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            } else if (conflictsInSpanish.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        modifier = Modifier.padding(12.dp),
                        text = "Contiene alergenos que marcaste: ${conflictsInSpanish.joinToString()}",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Text(
                        modifier = Modifier.padding(12.dp),
                        text = "No se detectan alergenos de tu lista en el producto",
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Text("Ingredientes: " + (product.ingredientsText ?: "No disponible"))
            Text("Alergenos (etiqueta): $allergenText")
            Text(
                "Alergenos detectados: ${
                    if (detectedInSpanish.isEmpty()) "No especificados" else detectedInSpanish.joinToString()
                }",
                style = MaterialTheme.typography.bodySmall
            )

            HorizontalDivider()

            Text("Nutricion (por 100g)", style = MaterialTheme.typography.titleMedium)
            Text("Kcal: ${kcal100g ?: "N/D"}")
            Text("Grasas: ${fat100g ?: "N/D"} g")
            Text("Carbohidratos: ${carbs100g ?: "N/D"} g")
            Text("Azucares: ${sugars100g ?: "N/D"} g")
            Text("Proteinas: ${proteins100g ?: "N/D"} g")
            Text("Sal: ${salt100g ?: "N/D"} g")
        }
    }
}

private fun mapUserAllergensToOff(user: Set<String>): Set<String> {
    if (user.isEmpty()) return emptySet()
    val result = linkedSetOf<String>()
    user.forEach { raw ->
        when (raw.trim().lowercase()) {
            "lacteos", "leche" -> result += "milk"
            "gluten" -> result += "gluten"
            "huevo", "huevos" -> result += "eggs"
            "soja" -> {
                result += "soybeans"
                result += "soy"
            }
            "cacahuetes", "mani" -> result += "peanuts"
            "frutos secos" -> result += "nuts"
            "pescado" -> result += "fish"
            "marisco", "crustaceos" -> {
                result += "crustaceans"
                result += "shellfish"
            }
            "sesamo" -> {
                result += "sesame-seeds"
                result += "sesame"
            }
            "mostaza" -> result += "mustard"
            "apio" -> result += "celery"
            "moluscos" -> result += "molluscs"
            "sulfitos", "sulfites" -> {
                result += "sulphur-dioxide-and-sulphites"
                result += "sulphites"
            }
            else -> result += raw.trim().lowercase()
        }
    }
    return result
}

private fun extractOffAllergens(tags: List<String>?, raw: String?): Set<String> {
    val fromTags = tags.orEmpty()
        .map { it.substringAfter(":").lowercase().trim() }
        .filter { it.isNotBlank() }
        .toSet()

    val fromRaw = raw
        ?.split(",", ";")
        ?.map { token ->
            token.trim().lowercase()
                .substringAfter(":")
                .replace("_", "-")
                .trim()
        }
        ?.filter { it.isNotBlank() }
        ?.toSet()
        .orEmpty()

    return fromTags + fromRaw
}

private fun translateOffAllergensToSpanish(values: Set<String>): List<String> {
    if (values.isEmpty()) return emptyList()
    val translated = sortedSetOf<String>()
    values.forEach { value ->
        val clean = offAllergenToSpanish(value).trim()
        if (clean.isNotEmpty()) translated += clean
    }
    return translated.toList()
}

private fun offAllergenToSpanish(value: String): String {
    val key = value
        .trim()
        .lowercase()
        .substringAfter(":")
        .replace("_", "-")
        .trim()

    return when (key) {
        "milk", "dairy", "lactose" -> "Lacteos"
        "gluten" -> "Gluten"
        "egg", "eggs" -> "Huevo"
        "soy", "soybeans" -> "Soja"
        "peanut", "peanuts" -> "Cacahuetes"
        "nut", "nuts", "tree-nuts" -> "Frutos secos"
        "fish" -> "Pescado"
        "crustaceans", "shellfish" -> "Mariscos"
        "sesame", "sesame-seeds" -> "Sesamo"
        "mustard" -> "Mostaza"
        "celery" -> "Apio"
        "molluscs", "mollusks" -> "Moluscos"
        "sulphites", "sulfites", "sulphur-dioxide-and-sulphites" -> "Sulfitos"
        "lupin", "lupine" -> "Altramuz"
        else -> key
            .replace("-", " ")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
private fun CameraBarcodePreview(
    modifier: Modifier,
    resetToken: Int,
    onBarcode: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var lastValue by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(resetToken) {
        lastValue = null
    }

    DisposableEffect(lifecycleOwner, previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128
            )
            .build()
        val scanner = BarcodeScanning.getClient(options)
        val executor: ExecutorService = Executors.newSingleThreadExecutor()

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(executor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage == null) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        val value = barcodes.firstOrNull()?.rawValue
                        if (!value.isNullOrBlank() && value != lastValue) {
                            lastValue = value
                            onBarcode(value)
                        }
                    }
                    .addOnCompleteListener { imageProxy.close() }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (_: Throwable) {
                // ignore
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            try {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            } catch (_: Throwable) {
                // ignore
            }
            try {
                scanner.close()
            } catch (_: Throwable) {
                // ignore
            }
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { previewView }
    )
}

private fun nutrToText(v: JsonElement?): String? {
    if (v == null) return null
    return try {
        v.jsonPrimitive.content
    } catch (_: Throwable) {
        v.toString().trim('"')
    }
}

private fun normalizeBarcode(rawValue: String): String =
    rawValue.trim().filter { it.isDigit() }

private fun isLikelyBarcode(value: String): Boolean =
    value.length in 8..14

private fun networkIssueMessage(error: IOException): String {
    val root = rootCause(error)
    return when (root) {
        is UnknownHostException -> "No se pudo resolver el servidor de Open Food Facts (DNS)."
        is SocketTimeoutException -> "El servidor tarda demasiado en responder. Intentalo de nuevo."
        is SSLHandshakeException, is SSLException -> "Fallo de seguridad TLS al conectar con Open Food Facts."
        is ConnectException -> "No se pudo abrir conexion con Open Food Facts."
        else -> "Fallo de red con Open Food Facts (${root.javaClass.simpleName})."
    }
}

private fun rootCause(t: Throwable): Throwable {
    var current: Throwable = t
    while (current.cause != null && current.cause !== current) {
        current = current.cause!!
    }
    return current
}

private const val SCANNER_LOOKUP_TIMEOUT_MS = 15_000L
