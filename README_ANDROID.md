# SafeByte Android (Kotlin + Compose)

Aplicacion movil Android de SafeByte/Food DNA orientada a seguridad alimentaria personalizada.

## Estado del proyecto

Ultima actualizacion de documentacion: 2026-03-14.

Estado funcional actual:
- Login y registro con Firestore.
- Modo invitado (`guest@local`) sin dependencia de backend de usuarios.
- Persistencia local de sesion y alergenos con DataStore.
- Catalogo local de recetas con filtrado por alergenos.
- Escaner de codigo de barras (CameraX + ML Kit) con consulta a Open Food Facts.
- Modulo IANutri (reformulacion, sugerencias, asistente de cocina e historial remoto).
- Pantallas de Home, Configuracion y Contacto (demo local).

## Stack tecnologico

Plataforma y toolchain:
- Android nativo.
- Kotlin 1.9.24.
- Android Gradle Plugin 8.4.2.
- Gradle Wrapper 8.10.
- Java 17.
- `minSdk = 26`, `targetSdk = 34`, `compileSdk = 34`.

UI:
- Jetpack Compose (BOM 2024.06.00).
- Material 3.
- Navigation Compose.

Datos y estado:
- DataStore Preferences.
- Kotlin Coroutines.

Red:
- Retrofit 2.11.0.
- OkHttp 4.12.0.
- Kotlinx Serialization JSON 1.6.3.

Servicios externos:
- Firebase Firestore (BOM 33.1.2).
- Open Food Facts API.

Escaner:
- CameraX 1.3.4.
- ML Kit Barcode Scanning 17.2.0.

Imagenes:
- Coil Compose 2.6.0.

## Decisiones tecnicas clave (resumen)

1. URL de backend IA inyectada en build (`BuildConfig.SAFEBYTE_API_BASE_URL`) y no editable en runtime.
   - Justificacion: evitar errores de configuracion en usuarios finales.
2. Bloqueo de builds `release` cuando la URL apunta a host local.
   - Justificacion: evitar APK/AAB no desplegables fuera de entorno local.
3. Persistencia dual de alergenos (local + Firestore para usuarios reales).
   - Justificacion: mantener UX estable aunque falle la red.
4. Dataset de recetas embebido en app.
   - Justificacion: funcionamiento offline inmediato para el modulo de comidas.
5. Tolerancia a respuestas backend no uniformes en IANutri (`history/History`, `message/Message`).
   - Justificacion: robustez ante variaciones de contrato API.

Para justificaciones completas y tradeoffs, ver:
- `docs/documentacion-completa-safebyte-android.md`

## Requisitos de entorno

- Android Studio con JDK 17 (recomendado: `jbr` incluido en Android Studio).
- SDK Android correctamente configurado (`local.properties` con `sdk.dir`).

## Ejecucion local

1. Abrir esta carpeta en Android Studio.
2. Sincronizar Gradle.
3. Ejecutar en emulador o dispositivo.

Compilacion por terminal (Windows PowerShell):

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:compileDebugKotlin
```

## Configuracion del backend IANutri

La URL efectiva se resuelve en este orden:
1. Propiedad Gradle `SAFEBYTE_API_BASE_URL`.
2. Variable de entorno `SAFEBYTE_API_BASE_URL`.
3. Fallback interno: `http://127.0.0.1:5188/`.

Nota importante:
- En este repositorio, actualmente `gradle.properties` define una URL publica (`https://safebyte-5gxw.onrender.com/`).

Si trabajas con backend local en movil fisico:

```powershell
adb reverse tcp:5188 tcp:5188
```

O usa el script:

```powershell
.\scripts\setup-adb-reverse.ps1
```

Regla de seguridad de build:
- Si intentas compilar `release` con `localhost`, `127.0.0.1` o `10.0.2.2`, Gradle falla intencionalmente.

## Seguridad y limitaciones actuales

- El login actual usa hash SHA-256 en cliente para comparar password guardado en Firestore.
- Esta aproximacion no equivale a un sistema de autenticacion robusto para produccion exigente.
- `android:usesCleartextTraffic="true"` esta activo para compatibilidad con desarrollo HTTP local.
- No hay tests automatizados (`app/src/test` y `app/src/androidTest` no existen).

## Documentacion del proyecto

- Guia tecnica completa: `docs/documentacion-completa-safebyte-android.md`
- Troubleshooting operativo: `docs/android-troubleshooting.md`
