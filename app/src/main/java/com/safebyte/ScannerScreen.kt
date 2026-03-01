package com.safebyte

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
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
import java.util.concurrent.Executors
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun ScannerScreen(
    prefs: UserPrefs,
    userAllergens: Set<String>
) {
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
                                barcode = value
                                manualBarcode = value
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
                            val b = manualBarcode.trim()
                            if (b.isBlank()) {
                                error = "Introduce un codigo de barras."
                            } else {
                                barcode = b
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Buscar producto") }

                    OutlinedButton(
                        onClick = {
                            barcode = null
                            product = null
                            error = null
                            loading = false
                            manualBarcode = ""
                            scanResetToken += 1
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
                            val b = manualBarcode.trim()
                            if (b.isBlank()) {
                                error = "Introduce un codigo de barras."
                            } else {
                                barcode = b
                            }
                        }
                    ) { Text("Buscar producto") }

                    OutlinedButton(
                        onClick = {
                            barcode = null
                            product = null
                            error = null
                            loading = false
                            manualBarcode = ""
                            scanResetToken += 1
                        }
                    ) { Text("Limpiar") }
                }
            }

            LaunchedEffect(barcode) {
                val b = barcode ?: return@LaunchedEffect
                loading = true
                error = null
                product = null

                try {
                    val resp = OpenFoodFacts.api.getProduct(b)
                    if (resp.status == 1 && resp.product != null) {
                        product = resp.product
                    } else {
                        error = "Producto no encontrado."
                    }
                } catch (_: Throwable) {
                    error = "No se pudo consultar el producto en este momento."
                } finally {
                    loading = false
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

                val userOff = mapUserAllergensToOff(userAllergens)
                val productAllergens = extractOffAllergens(null, p.allergens)
                val conflicts = productAllergens.intersect(userOff)

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
    val detectedInSpanish = translateOffAllergensToSpanish(productAllergens)
    val conflictsInSpanish = translateOffAllergensToSpanish(conflicts)

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

private fun mapUserAllergensToOff(user: Set<String>): Set<String> =
    user.flatMap { a ->
        when (a.trim().lowercase()) {
            "lacteos", "leche" -> listOf("milk")
            "gluten" -> listOf("gluten")
            "huevo", "huevos" -> listOf("eggs")
            "soja" -> listOf("soybeans", "soy")
            "cacahuetes", "mani" -> listOf("peanuts")
            "frutos secos" -> listOf("nuts")
            "pescado" -> listOf("fish")
            "marisco", "crustaceos" -> listOf("crustaceans", "shellfish")
            "sesamo" -> listOf("sesame-seeds", "sesame")
            "mostaza" -> listOf("mustard")
            "apio" -> listOf("celery")
            "moluscos" -> listOf("molluscs")
            "sulfitos", "sulfites" -> listOf("sulphur-dioxide-and-sulphites", "sulphites")
            else -> listOf(a.trim().lowercase())
        }
    }.toSet()

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
    return values
        .map { offAllergenToSpanish(it) }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()
        .sorted()
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
    val lifecycleOwner = LocalLifecycleOwner.current
    var lastValue by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(resetToken) {
        lastValue = null
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val previewView = PreviewView(context).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

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
                val executor = Executors.newSingleThreadExecutor()

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
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
                    } else {
                        imageProxy.close()
                    }
                }

                val selector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                } catch (_: Throwable) {
                    // ignore
                }
            }, ContextCompat.getMainExecutor(context))

            previewView
        }
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
