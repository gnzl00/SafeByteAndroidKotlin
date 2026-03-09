import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.gms.google-services")
}

val safeByteApiBaseUrlInput = ((project.findProperty("SAFEBYTE_API_BASE_URL") as? String)
    ?: System.getenv("SAFEBYTE_API_BASE_URL")
    ?: "http://127.0.0.1:5188/")
    .trim()

val safeByteApiBaseUrl = safeByteApiBaseUrlInput
    .let { if (it.endsWith("/")) it else "$it/" }

fun backendHost(baseUrl: String): String =
    runCatching { URI(baseUrl).host?.lowercase().orEmpty() }.getOrDefault("")

fun isLocalBackend(baseUrl: String): Boolean {
    val host = backendHost(baseUrl)
    return host == "localhost" || host == "127.0.0.1" || host == "10.0.2.2"
}

val isLocalBackendUrl = isLocalBackend(safeByteApiBaseUrl)
val requestedTasks = gradle.startParameter.taskNames.joinToString(" ").lowercase()
val isReleaseTaskRequested = requestedTasks.contains("release") || requestedTasks.contains("bundle")
if (isReleaseTaskRequested && isLocalBackendUrl) {
    throw GradleException(
        "SAFEBYTE_API_BASE_URL apunta a $safeByteApiBaseUrl. Para builds release usa una URL publica HTTPS."
    )
}

fun resolveAndroidSdkDir(): String? {
    val fromEnv = System.getenv("ANDROID_SDK_ROOT")
        ?: System.getenv("ANDROID_HOME")
    if (!fromEnv.isNullOrBlank()) return fromEnv

    val localProps = rootProject.file("local.properties")
    if (!localProps.exists()) return null

    val props = Properties()
    localProps.inputStream().use { props.load(it) }
    return props.getProperty("sdk.dir")
}

val setupAdbReverse by tasks.registering {
    group = "safebyte"
    description = "Configura adb reverse tcp:5188 tcp:5188 automaticamente antes de debug."

    doLast {
        val sdkDir = resolveAndroidSdkDir()
        if (sdkDir.isNullOrBlank()) {
            logger.lifecycle("[setupAdbReverse] SDK no encontrado, se omite.")
            return@doLast
        }

        val adbName = if (System.getProperty("os.name").lowercase().contains("win")) "adb.exe" else "adb"
        val adb = file("$sdkDir/platform-tools/$adbName")
        if (!adb.exists()) {
            logger.lifecycle("[setupAdbReverse] adb no encontrado en ${adb.absolutePath}, se omite.")
            return@doLast
        }

        exec {
            commandLine(adb.absolutePath, "start-server")
            isIgnoreExitValue = true
        }

        val devicesOutput = ByteArrayOutputStream()
        exec {
            commandLine(adb.absolutePath, "devices")
            standardOutput = devicesOutput
            isIgnoreExitValue = true
        }
        val hasDevice = devicesOutput
            .toString()
            .lineSequence()
            .map { it.trim() }
            .any { line -> Regex("^[^\\s]+\\s+device$").matches(line) }

        if (!hasDevice) {
            logger.lifecycle("[setupAdbReverse] No hay dispositivo conectado; se omite reverse.")
            return@doLast
        }

        val reverseResult = exec {
            commandLine(adb.absolutePath, "reverse", "tcp:5188", "tcp:5188")
            isIgnoreExitValue = true
        }

        if (reverseResult.exitValue == 0) {
            logger.lifecycle("[setupAdbReverse] OK: tcp:5188 -> tcp:5188")
        } else {
            logger.lifecycle("[setupAdbReverse] Fallo adb reverse, continuando build.")
        }
    }
}

android {
    namespace = "com.safebyte"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.safebyte"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SAFEBYTE_API_BASE_URL", "\"$safeByteApiBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

if (isLocalBackendUrl) {
    tasks.matching { it.name == "preDebugBuild" }.configureEach {
        dependsOn(setupAdbReverse)
    }
}

dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
        implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("com.google.android.material:material:1.12.0")

    // Lifecycle / ViewModel
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Networking (Open Food Facts)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // CameraX + ML Kit barcode scanning
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("com.google.guava:guava:32.1.3-android")

    // Coil (images)
    implementation("io.coil-kt:coil-compose:2.6.0")

    //Firebase y demas
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
