# Documentacion Tecnica Completa - SafeByte Android

Fecha de elaboracion: 2026-03-10
Proyecto: SafeByte Android Kotlin (Compose)
Ruta del repo: `SafeByteAndroidKotlin/SafeByteAndroidKotlin`

## 1. Objetivo del proyecto

SafeByte es una app Android enfocada en seguridad alimentaria para personas con alergias. El objetivo funcional es:

1. Permitir registro e inicio de sesion.
2. Guardar y aplicar alergenos del usuario.
3. Filtrar recetas segun restricciones alimentarias.
4. Escanear codigos de barras y verificar alergenos de productos reales con Open Food Facts.
5. Generar sugerencias de comida y asistencia de cocina mediante backend IA (IANutri).
6. Mantener historial IA por usuario autenticado.

## 2. Estado actual medido desde el codigo

Metricas del proyecto:

1. Archivos indexados en repo: 264
2. Archivos Kotlin de app: 17
3. Lineas Kotlin totales: 3587
4. FoodItem en catalogo local: 208
5. Drawables en `res/drawable`: 213
6. Archivos de imagen en `res`: 228
7. Tests unitarios: no hay `app/src/test`
8. Tests instrumentados: no hay `app/src/androidTest`

Distribucion de alergenos declarados en `MealsData.kt`:

1. Gluten: 74
2. Lacteos (en archivo aparece tambien como texto mal codificado): 87
3. Huevo: 36
4. Frutos secos: 18
5. Mariscos: 14
6. Pescado: 13
7. Soja: 22
8. Sesamo (en archivo aparece tambien como texto mal codificado): 3

## 3. Stack tecnologico y versiones

### 3.1 Plataforma y lenguaje

1. Android nativo.
2. Kotlin.
3. Jetpack Compose para UI.
4. Material 3.

### 3.2 Toolchain

1. Android Gradle Plugin: 8.4.2
2. Kotlin plugin: 1.9.24
3. Kotlin serialization plugin: 1.9.24
4. Google services plugin: 4.4.2
5. Gradle wrapper: 8.10
6. Java target: 17
7. minSdk: 26
8. targetSdk: 34
9. compileSdk: 34

### 3.3 Librerias principales

UI y navegacion:

1. `androidx.activity:activity-compose:1.9.0`
2. `androidx.compose` via BOM `2024.06.00`
3. `androidx.compose.material3:material3`
4. `androidx.compose.material:material-icons-extended`
5. `androidx.navigation:navigation-compose:2.7.7`
6. `com.google.android.material:material:1.12.0`

Estado y almacenamiento:

1. `androidx.lifecycle:lifecycle-runtime-ktx:2.7.0`
2. `androidx.datastore:datastore-preferences:1.1.1`

Networking:

1. `retrofit:2.11.0`
2. `retrofit2-kotlinx-serialization-converter:1.0.0`
3. `kotlinx-serialization-json:1.6.3`
4. `okhttp:4.12.0`
5. `logging-interceptor:4.12.0`

Escaner:

1. CameraX (`camera-camera2`, `camera-lifecycle`, `camera-view` 1.3.4)
2. ML Kit Barcode Scanning `17.2.0`
3. Guava Android `32.1.3-android`

Imagenes:

1. Coil Compose `2.6.0`

Backend de usuarios:

1. Firebase BOM `33.1.2`
2. Firestore KTX
3. Coroutines Play Services `1.8.1`

## 4. Estructura del repositorio

Raiz:

1. `build.gradle.kts`: plugins globales.
2. `settings.gradle.kts`: repositorios y modulos.
3. `gradle.properties`: flags Gradle y `SAFEBYTE_API_BASE_URL`.
4. `README_ANDROID.md`: guia funcional resumida.
5. `docs/android-troubleshooting.md`: troubleshooting operativo.
6. `scripts/setup-adb-reverse.ps1`: tunel ADB para backend local.

Modulo app:

1. `app/build.gradle.kts`: configuracion Android, dependencias, guardas de release y tarea adb reverse.
2. `app/src/main/AndroidManifest.xml`: permisos y configuracion de app.
3. `app/src/main/java/com/safebyte/*.kt`: logica completa de UI, dominio, red y almacenamiento.
4. `app/src/main/res/*`: assets visuales, launcher y temas.
5. `app/google-services.json`: vinculacion Firebase del app Android.

## 5. Arquitectura aplicada

No hay arquitectura por capas estricta tipo Clean Architecture formal. La app aplica una arquitectura Compose pragmatica con responsabilidades separadas por archivo:

1. Entrada: `MainActivity`.
2. Shell de app y navegacion: `SafeByteApp`.
3. Pantallas: un archivo por feature.
4. Red: `IANutriApi.kt` y `OpenFoodFacts.kt`.
5. Persistencia local: `UserPrefs.kt` (DataStore).
6. Persistencia remota de perfil: `FirebaseUsersRepo.kt`.
7. Dominio de alergenos: `AllergenUtils.kt`.
8. Dataset local de recetas: `MealsData.kt`.

Decision visible: priorizar velocidad de desarrollo y claridad de lectura por feature sobre separacion extrema en modulos o ViewModel.

Tradeoff:

1. Ventaja: menor sobrecarga arquitectonica.
2. Desventaja: parte de la logica de negocio vive dentro de composables grandes (`IANutriScreen`, `ScannerScreen`), lo que complica testeo unitario.

## 6. Configuracion de build y decisiones de release

### 6.1 Inyeccion de URL backend IA

`BuildConfig.SAFEBYTE_API_BASE_URL` se construye en compile-time desde:

1. propiedad Gradle `SAFEBYTE_API_BASE_URL`,
2. o variable de entorno del mismo nombre,
3. o fallback a `http://127.0.0.1:5188/`.

Decision:

1. El backend base no se cambia en runtime desde la UI.
2. Se evita configuracion manual accidental por usuario final.

### 6.2 Guardas para release seguro

En `app/build.gradle.kts` existe una validacion:

1. Si se intenta tarea release/bundle con host local (`localhost`, `127.0.0.1`, `10.0.2.2`), Gradle falla.

Decision:

1. Evitar publicar APK/AAB dependiente de backend local.

### 6.3 Automatizacion de adb reverse en debug local

Si la URL es local, `preDebugBuild` depende de `setupAdbReverse`.

La tarea:

1. resuelve SDK path,
2. busca adb,
3. levanta server adb,
4. verifica dispositivo conectado,
5. ejecuta `adb reverse tcp:5188 tcp:5188`.

Decision:

1. Reducir friccion en desarrollo con movil fisico.

### 6.4 Tipo release

`release`:

1. `isMinifyEnabled = true`
2. `isShrinkResources = true`
3. usa proguard optimize por defecto.

Observacion:

1. `app/proguard-rules.pro` esta practicamente vacio, por lo que no hay reglas custom definidas.

## 7. AndroidManifest y permisos

Configuracion clave:

1. Permiso INTERNET.
2. Permiso CAMERA.
3. `android:usesCleartextTraffic="true"`.
4. launcher activity exportada (`MainActivity`).
5. tema `Theme.SafeByte`.

Decision `usesCleartextTraffic=true`:

1. Compatibilidad con backend HTTP local en debug.
2. Riesgo: permite trafico no cifrado si se deja en produccion.

## 8. Tema, diseno y UX transversal

`Theme.kt` define:

1. paleta clara basada en verde (enfoque salud/seguridad).
2. tipografia sans-serif.
3. esquinas redondeadas pronunciadas.
4. `WindowCompat.setDecorFitsSystemWindows(window, false)`.

Decision:

1. Se fuerza experiencia visual clara y contraste explicito en componentes para evitar problemas de legibilidad reportados.

## 9. Navegacion global y shell de aplicacion

`SafeByteApp.kt`:

Rutas:

1. Home
2. Comidas
3. IANutri
4. Scanner
5. Config
6. Contacto

Comportamiento:

1. Si no hay sesion: muestra `AuthScreen`.
2. Si hay sesion y alergenos no cargados: spinner.
3. Top bar con titulo dinamico y boton "Cerrar sesion".
4. Bottom navigation persistente.

Decision de persistencia de alergenos:

1. Usuario real: intenta Firestore y cachea en DataStore.
2. Si falla remoto: usa cache local.
3. Invitado (`guest@local`): solo local.

Tradeoff:

1. UX resiliente sin bloqueos por red.
2. Posible divergencia temporal entre local y remoto si hay fallos.

## 10. Dominio de alergenos

`AllergenUtils.kt` concentra normalizacion y matching.

Funciones clave:

1. `canonicalizeAllergen`
2. `allergenKey`
3. `normalizeAllergenSet`
4. `hasAllergenConflict`
5. `hasAllergenConflictWithKeys`

Decision tecnica:

1. Se normaliza por token en minusculas, sin tildes y sin simbolos.
2. Se manejan sinonimos (`soy`/`soja`, `lacte` -> `Lacteos`, etc).
3. Se incluyen correcciones para cadenas con mojibake (`Ã¡`, `ÃƒÂ¡`, etc).

Impacto:

1. Mejora matching entre datos heterogeneos (usuario, Open Food Facts, dataset local).
2. Reduce falsos negativos por diferencias ortograficas/codificacion.

Catalogo de alergenos por defecto (UI settings):

1. Gluten
2. Lacteos
3. Huevo
4. Frutos secos
5. Mariscos
6. Soja

Nota:

1. El motor de normalizacion soporta mas terminos (Pescado, Sesamo) aunque no todos aparecen en selector visual.

## 11. Autenticacion y usuarios

### 11.1 Pantalla de auth

`AuthScreen.kt` implementa login/signup en una sola pantalla con modo dual.

Flujo signup:

1. valida campos.
2. normaliza email a lowercase.
3. consulta si documento Firestore ya existe (`users/{emailLower}`).
4. crea documento con campos:
   - `username`
   - `email`
   - `passwordHash`
   - `allergens` (lista vacia)
   - `createdAt`
   - `allergensUpdatedAt`

Flujo login:

1. valida campos.
2. busca doc por email.
3. compara hash local calculado vs hash guardado.

Modo invitado:

1. boton "Skip Login" en modo login.
2. registra sesion local con `guest@local`.

### 11.2 Hash de password

`PasswordHasher.kt`:

1. SHA-256 + Base64 (sin sal, sin KDF fuerte).

Decision:

1. Implementacion simple y directa.

Riesgo:

1. Seguridad insuficiente para contexto productivo serio.
2. Recomendado migrar a Firebase Auth o backend con Argon2/Bcrypt/PBKDF2 con sal per-user.

### 11.3 Repositorio Firestore de alergenos

`FirebaseUsersRepo.kt`:

1. `getAllergensByEmail`
2. `setAllergensByEmail`

Decision:

1. Encapsular operaciones de Firestore en clase minima reusable.

## 12. Sesion y persistencia local

`UserPrefs.kt` usa DataStore Preferences:

Keys:

1. `logged_in` (Boolean)
2. `logged_email` (String)
3. `allergens` (Set<String>)

Flujos:

1. `isLoggedIn`
2. `loggedEmail`
3. `allergens`

Operaciones:

1. `setLoggedIn(email)`
2. `logout()`
3. `setAllergens(values)`

Decision:

1. Almacenamiento local reactivo con `Flow`.

## 13. Modulo Home

`HomeScreen.kt`:

1. Mensaje de bienvenida.
2. Dos testimonios con imagen.
3. Guia "Como usar" por pasos.
4. Layout responsive (1 o 2 columnas en testimonios).

Decision:

1. Pantalla de onboarding pasivo para orientar uso de features.

## 14. Modulo Comidas

`MealsScreen.kt` + `MealsData.kt`.

### 14.1 Modelo

`FoodItem`:

1. name
2. imageResId
3. ingredients
4. recipe
5. allergens

### 14.2 Dataset

1. 208 recetas embebidas en codigo (objeto `MealsData`).
2. Imagenes locales referenciadas por `R.drawable`.

Decision:

1. Dataset local offline, sin dependencia de backend para catalogo base.

Tradeoff:

1. APK mas pesado por muchas imagenes.
2. Mantenimiento manual del catalogo.

### 14.3 Filtrado y UX

1. Busqueda por texto en nombre.
2. Filtrado por conflicto de alergenos del usuario.
3. Grid adaptativo.
4. Modal con ingredientes, receta y aviso de conflicto.

## 15. Modulo Scanner (Camara + Open Food Facts)

`ScannerScreen.kt` + `OpenFoodFacts.kt`.

### 15.1 Flujo principal

1. Solicita permiso camara.
2. Escanea codigos (EAN13, EAN8, UPC_A, UPC_E, CODE_128) via ML Kit.
3. Alternativa manual: ingreso de codigo.
4. Normaliza codigo a solo digitos (8..14).
5. Consulta Open Food Facts.
6. Muestra producto, ingredientes, alergenos y nutrimentos.
7. Cruza alergenos detectados vs alergenos del usuario.

### 15.2 Cliente Open Food Facts

Decisiones tecnicas destacadas:

1. Multiples hosts fallback:
   - `world.openfoodfacts.org`
   - `es.openfoodfacts.org`
   - `openfoodfacts.org`
2. Reintentos por host: 2.
3. Preferencia DNS IPv4 first.
4. Timeouts:
   - connect 8s
   - read 20s
   - call 25s
5. User-Agent explicito `SafeByteAndroid/1.0`.
6. Logging BASIC solo en debug.
7. Cache de instancia API por baseUrl.

### 15.3 Robustez y concurrencia

En `ScannerScreen`:

1. `activeRequestToken` y `activeRequestJob` evitan race conditions.
2. timeout de consulta: 15s.
3. cancelacion de request anterior al iniciar otra.
4. limpieza de estado y reset de ultimo codigo.
5. manejo detallado de errores de red (DNS, timeout, TLS, conexion).

Decision:

1. Prioridad alta a resiliencia de red en feature de escaneo.

### 15.4 Mapeo de alergenos OFF

Funciones:

1. `mapUserAllergensToOff`
2. `extractOffAllergens`
3. `translateOffAllergensToSpanish`

Decision:

1. Traducir y homologar claves de Open Food Facts a terminos amigables en espanol.

## 16. Modulo IANutri (Backend IA)

`IANutriApi.kt` + `IANutriScreen.kt`.

### 16.1 Endpoints backend usados

1. `POST /api/IANutri/Reformulate`
2. `POST /api/IANutri/GenerateSuggestions`
3. `POST /api/IANutri/CookingAssistant`
4. `GET /api/IANutri/History?email=...`
5. `DELETE /api/IANutri/History?email=...`

### 16.2 Modelado de datos

Requests:

1. `IANutriReformulateRequest`
2. `IANutriGenerateSuggestionsRequest`
3. `IANutriCookingAssistantRequest`

Responses:

1. `IANutriReformulateResponse`
2. `IANutriGenerateSuggestionsResponse`
3. `IANutriCookingAssistantResponse`
4. `IANutriHistoryEnvelope`
5. `IANutriDeleteHistoryResponse`

Decision de compatibilidad:

1. Se soportan variantes de backend con propiedades `history` y `History`.
2. Se soportan `message/Message` y `deleted/Deleted`.

### 16.3 Cliente de red IANutri

`IANutriNetwork`:

1. normaliza base URL (tambien limpia sufijo `/api` si se pasa).
2. cachea APIs por URL.
3. usa JSON con `ignoreUnknownKeys=true`.
4. timeouts:
   - connect 20s
   - read 40s
   - write 40s

### 16.4 Flujo funcional de pantalla IA

Pipeline en `IANutriScreen`:

1. Usuario escribe input libre (max 600 chars).
2. Elige modo (`rapido-basico`, `cena-ligera`, `menu-semanal`).
3. Se reformula peticion.
4. Se generan sugerencias.
5. Usuario abre asistente de cocina para receta seleccionada.
6. Se puede recuperar o borrar historial remoto.

### 16.5 Sanitizacion y normalizacion

Decisiones clave:

1. `sanitizeReformulationSafe` elimina bullets y tokens no deseados (`gpt`, `openai`, etc).
2. `normalizeTextList` deduplica, limpia y limita longitud de listas.
3. `normalizedSuggestion` aplica defaults para campos vacios.

Objetivo:

1. estabilizar UI incluso con respuestas IA incompletas o ruidosas.

### 16.6 Manejo de errores

`Throwable.toApiMessage(apiBaseUrl)`:

1. parsea mensaje de body HTTP si existe.
2. agrega pista contextual de URL backend.
3. sugiere `adb reverse` cuando detecta localhost/127.0.0.1.

Decision:

1. Error messaging orientado a accion operativa real, no mensajes genericos.

## 17. Modulo Configuracion de usuario

`SettingsScreen.kt`:

1. Muestra selector de alergenos con cards toggle.
2. Responsive por columnas segun ancho.
3. Muestra estado de sesion (guest o email real).
4. Boton guardar.
5. Feedback de guardado.

Persistencia:

1. Al guardar llama callback hacia `SafeByteApp`.
2. `SafeByteApp` guarda en local.
3. Si no es guest, intenta sincronizar Firestore.
4. Falla remota no derriba UI.

Decision:

1. priorizar estabilidad sobre consistencia remota inmediata.

## 18. Modulo Contacto

`ContactScreen.kt`:

1. Formulario local: nombre, correo, mensaje.
2. Validacion minima de campos no vacios.
3. No envia nada a backend.
4. Muestra "Mensaje enviado (demo local)".

Decision:

1. feature de contacto implementada como demo visual, no como canal real de mensajeria.

## 19. Integracion Firebase

Elementos:

1. Plugin Google Services activo.
2. `google-services.json` presente.
3. Proyecto Firebase detectado: `fooddna-b91c1`.
4. Package configurado: `com.safebyte`.
5. Firestore utilizado para usuarios.

Schema de documento `users/{emailLower}` inferido:

1. `username: String`
2. `email: String`
3. `passwordHash: String`
4. `allergens: List<String>`
5. `createdAt: Timestamp`
6. `allergensUpdatedAt: Timestamp`

## 20. Recursos visuales y contenido

1. 212 imagenes JPG de recetas/personas.
2. 1 imagen PNG.
3. 15 iconos WEBP en mipmaps.
4. 2 XML adaptive icons.
5. 1 color de fondo launcher.

Decision:

1. usar recursos locales para experiencia rapida sin descarga adicional.

Tradeoff:

1. tamaño de app superior.

## 21. Manejo de estado y concurrencia

Patrones usados:

1. Estado UI con `remember`, `rememberSaveable`, `mutableStateOf`.
2. Reactividad con `collectAsState` de DataStore.
3. Coroutines para IO remoto.
4. Cancelacion explicita de jobs en scanner.
5. `LaunchedEffect` para carga inicial y sincronizaciones.

Decision:

1. Enfoque Compose state-first sin ViewModel en esta iteracion.

## 22. Decisiones tecnicas principales (resumen ejecutivo)

1. **Backend URL en build config, no en UI**
   - motivo: evitar errores de configuracion en usuarios.
   - costo: menor flexibilidad runtime.
2. **Bloqueo release con backend local**
   - motivo: evitar builds no desplegables.
   - costo: obliga a definir backend publico para release.
3. **Soporte guest mode**
   - motivo: reducir friccion de entrada.
   - costo: historial IA y sync remota desactivados en guest.
4. **Persistencia dual alergenos (local + remoto)**
   - motivo: resiliencia offline/intermitencia.
   - costo: posibles desincronizaciones temporales.
5. **Dataset recetas local grande**
   - motivo: funcionalidad offline inmediata.
   - costo: APK pesado y mantenimiento manual.
6. **Sin ViewModel**
   - motivo: simplicidad y velocidad.
   - costo: composables largos, testeo mas dificil.
7. **Hash password simple SHA-256**
   - motivo: implementacion rapida.
   - costo: nivel de seguridad bajo para produccion exigente.
8. **Manejo robusto de Open Food Facts**
   - motivo: fiabilidad en red real.
   - costo: mas complejidad de codigo.
9. **Compatibilidad con variantes de JSON backend IA**
   - motivo: tolerancia a inconsistencias de contrato.
   - costo: logica extra de parsing.
10. **Forzar colores claros explicitos**
   - motivo: solucionar problemas de contraste reportados.
   - costo: menor flexibilidad de tema dinamico.

## 23. Seguridad, privacidad y riesgos

Aspectos positivos:

1. Password no se guarda en texto plano.
2. Historial IA deshabilitado en guest (evita asociacion falsa de usuario).
3. Build release bloquea localhost.

Riesgos detectados:

1. Hash SHA-256 sin sal/KDF robusto.
2. Login propio en Firestore en vez de Firebase Auth (sin controles auth nativos).
3. `usesCleartextTraffic=true` habilita HTTP.
4. No hay validacion fuerte de email/password (solo no vacio).
5. No hay capa de cifrado adicional de preferencias locales.
6. Sin tests automatizados.

## 24. Rendimiento y escalabilidad

Fortalezas:

1. Filtrado de recetas con secuencias y claves normalizadas.
2. Cache de clientes Retrofit por URL.
3. Reuso de API Open Food Facts por host.
4. Timeouts y cancelaciones definidos.

Puntos a vigilar:

1. `MealsData.kt` muy grande embebido en codigo.
2. Composables extensos con logica de negocio incrustada.
3. Muchas imagenes locales impactan tamano de APK y memoria.

## 25. Operacion local y despliegue

### 25.1 Desarrollo local recomendado

1. JDK 17 (Android Studio jbr).
2. Backend IA local o remoto.
3. Si backend local en movil fisico: `adb reverse tcp:5188 tcp:5188`.
4. Script de ayuda: `scripts/setup-adb-reverse.ps1`.

### 25.2 Produccion

1. Definir `SAFEBYTE_API_BASE_URL` publica HTTPS.
2. Evitar hosts locales en release.
3. Revisar politica de cleartext.

## 26. Calidad y testing

Situacion actual:

1. No existen pruebas unitarias ni instrumentadas en el repo.
2. La validacion se basa en ejecucion manual y troubleshooting documentado.

Implicacion:

1. Riesgo de regresiones no detectadas automaticamente.

## 27. Mapa archivo por archivo (codigo Kotlin)

1. `MainActivity.kt`
   - punto de entrada Android y `setContent`.
2. `SafeByteApp.kt`
   - shell principal, navegacion, carga de sesion y sincronizacion de alergenos.
3. `Theme.kt`
   - paleta, tipografia, shapes y ajuste de system windows.
4. `AllergenUtils.kt`
   - normalizacion canonica y comparacion de alergenos.
5. `UserPrefs.kt`
   - DataStore para sesion y alergenos locales.
6. `AuthScreen.kt`
   - login/signup/guest con Firestore.
7. `PasswordHasher.kt`
   - hashing SHA-256 Base64.
8. `FirebaseUsersRepo.kt`
   - lectura/escritura de alergenos en Firestore.
9. `HomeScreen.kt`
   - bienvenida, testimonios y guia de uso.
10. `MealsScreen.kt`
    - busqueda, grid y modal de recetas filtradas.
11. `MealsData.kt`
    - dataset local de 208 recetas.
12. `OpenFoodFacts.kt`
    - cliente Retrofit OFF con retry/fallback.
13. `ScannerScreen.kt`
    - flujo camara+MLKit, consulta OFF, analisis de conflicto alergenos.
14. `IANutriApi.kt`
    - contrato y modelos de backend IA.
15. `IANutriScreen.kt`
    - UI completa IA, pipeline, asistente y historial.
16. `SettingsScreen.kt`
    - seleccion y guardado de alergenos.
17. `ContactScreen.kt`
    - formulario local demo.

## 28. Mapa archivo por archivo (config y soporte)

1. `build.gradle.kts`
   - plugins globales.
2. `settings.gradle.kts`
   - repositorios y modulo `:app`.
3. `gradle.properties`
   - parametros de build y URL backend.
4. `app/build.gradle.kts`
   - android config, dependencias, tareas custom y guardas.
5. `app/src/main/AndroidManifest.xml`
   - permisos, actividad principal y cleartext.
6. `app/proguard-rules.pro`
   - placeholder sin reglas custom.
7. `app/src/main/res/values/themes.xml`
   - tema app base Material3.
8. `README_ANDROID.md`
   - guia rapida de uso y estado de correcciones.
9. `docs/android-troubleshooting.md`
   - FAQ de problemas comunes.
10. `scripts/setup-adb-reverse.ps1`
    - automatizacion de adb reverse.
11. `app/google-services.json`
    - configuracion Firebase para package `com.safebyte`.

## 29. Limitaciones conocidas y deuda tecnica

1. Seguridad de auth basica para entorno serio.
2. Ausencia total de testing automatizado.
3. Dependencia de backend IA externo para funcionalidad premium.
4. Contacto sin backend real.
5. Strings con problemas de codificacion en parte del dataset.
6. Catalogo de alergenos de settings limitado a 6 opciones pese a soporte interno mayor.
7. Falta de capa ViewModel/use-case/repository uniforme.

## 30. Recomendaciones tecnicas priorizadas

Prioridad alta:

1. Migrar autenticacion a Firebase Auth o backend seguro.
2. Sustituir hash SHA-256 simple por KDF fuerte con sal.
3. Desactivar cleartext en release y usar Network Security Config.
4. Agregar tests unitarios para:
   - normalizacion de alergenos,
   - mapping OFF,
   - parseo IANutri.
5. Agregar tests UI/instrumentados para flujos criticos.

Prioridad media:

1. Extraer logica de `IANutriScreen` y `ScannerScreen` a ViewModel.
2. Mover `MealsData` a JSON/DB para mantenimiento mas simple.
3. Corregir codificacion de strings y centralizar textos en recursos.
4. Implementar contacto real contra backend o servicio de soporte.

Prioridad baja:

1. Localizacion completa (es/en).
2. Telemetria de errores no sensibles.
3. Optimizacion de assets (compresion adicional).

## 31. Conclusion

El proyecto esta funcionalmente completo para un MVP robusto en Android con foco claro en seguridad alimentaria personalizada. La aplicacion combina:

1. persistencia local reactiva,
2. sincronizacion remota de perfil,
3. escaneo real de productos con Open Food Facts,
4. generacion y asistencia de menus por backend IA.

Las decisiones tecnicas privilegian experiencia de usuario estable y operatividad de desarrollo. El siguiente salto de madurez recomendado esta en seguridad de autenticacion, testing automatizado y separacion arquitectonica para escalado.

