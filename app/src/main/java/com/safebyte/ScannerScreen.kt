package com.safebyte

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun ScannerScreen(
    prefs: UserPrefs,
    userAllergens: Set<String>
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

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

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Escaneo código de barras", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))

        if (!hasCameraPermission) {
            Text("Para escanear con cámara, concede permiso de cámara.")
            Spacer(Modifier.height(8.dp))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text("Conceder permiso de cámara")
            }
        } else {
            CameraBarcodePreview(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                onBarcode = { value ->
                    barcode = value
                    manualBarcode = value
                }
            )
            Spacer(Modifier.height(8.dp))
            Text("Consejo: si detecta varios códigos, se quedará con el primero.")
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = manualBarcode,
            onValueChange = { manualBarcode = it },
            label = { Text("O introduce el código manualmente") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val b = manualBarcode.trim()
                    if (b.isBlank()) {
                        error = "Introduce un código de barras."
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
                }
            ) { Text("Limpiar") }
        }

        // Buscar producto cuando cambia barcode
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
                error = "Error al consultar Open Food Facts."
            } finally {
                loading = false
            }
        }

        Spacer(Modifier.height(16.dp))

        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }

        if (barcode != null) {
            Spacer(Modifier.height(8.dp))
            Text("Código: $barcode", style = MaterialTheme.typography.bodySmall)
        }

        product?.let { p ->
            Spacer(Modifier.height(12.dp))

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

@Composable
private fun ProductCard(
    product: Product,
    productAllergens: Set<String>,
    conflicts: Set<String>,
    kcal100g: String?,
    fat100g: String?,
    carbs100g: String?,
    sugars100g: String?,
    proteins100g: String?,
    salt100g: String?
) {
    val allergenText = product.allergens ?: "No especificado"

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

            if (conflicts.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        modifier = Modifier.padding(12.dp),
                        text = "⚠️ Contiene alérgenos que marcaste: ${conflicts.joinToString()}",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Text(
                        modifier = Modifier.padding(12.dp),
                        text = "✅ No se detectan alérgenos de tu lista en el producto",
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Text("Ingredientes: " + (product.ingredientsText ?: "No disponible"))

            Text("Alérgenos (etiqueta): $allergenText")
            Text(
                "Alérgenos detectados (API): ${
                    if (productAllergens.isEmpty()) "No especificados" else productAllergens.joinToString()
                }",
                style = MaterialTheme.typography.bodySmall
            )

            Divider()

            Text("Nutrición (por 100g)", style = MaterialTheme.typography.titleMedium)
            Text("Kcal: ${kcal100g ?: "N/D"}")
            Text("Grasas: ${fat100g ?: "N/D"} g")
            Text("Carbohidratos: ${carbs100g ?: "N/D"} g")
            Text("Azúcares: ${sugars100g ?: "N/D"} g")
            Text("Proteínas: ${proteins100g ?: "N/D"} g")
            Text("Sal: ${salt100g ?: "N/D"} g")
        }
    }
}

private fun mapUserAllergensToOff(user: Set<String>): Set<String> =
    user.flatMap { a ->
        when (a.trim().lowercase()) {
            "lácteos", "lacteos", "leche" -> listOf("milk")
            "gluten" -> listOf("gluten")
            "huevo", "huevos" -> listOf("eggs")
            "soja" -> listOf("soybeans", "soy")
            "cacahuetes", "mani", "maní" -> listOf("peanuts")
            "frutos secos" -> listOf("nuts")
            "pescado" -> listOf("fish")
            "marisco", "crustaceos", "crustáceos" -> listOf("crustaceans", "shellfish")
            "sesamo", "sésamo" -> listOf("sesame-seeds", "sesame")
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
                .substringAfter(":")          // <- quita en:gluten / es:gluten
                .replace("_", "-")
                .trim()
        }
        ?.filter { it.isNotBlank() }
        ?.toSet()
        .orEmpty()

    return fromTags + fromRaw
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
private fun CameraBarcodePreview(
    modifier: Modifier,
    onBarcode: (String) -> Unit
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lastValue by remember { mutableStateOf<String?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val previewView = PreviewView(context)

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