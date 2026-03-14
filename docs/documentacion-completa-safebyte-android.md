# Documentacion Tecnica Completa - SafeByte Android

Fecha de actualizacion: 2026-03-14  
Proyecto: SafeByte Android Kotlin (Compose)  
Modulo principal: `:app`

## 1. Objetivo del proyecto

SafeByte es una app Android para reducir riesgo alimentario en personas con alergias. El producto combina:
- Gestion de sesion y perfil de alergenos.
- Catalogo local de comidas filtrado por riesgo.
- Escaneo real de productos de supermercado.
- Asistencia IA para sugerencias y cocina segura.

## 2. Alcance funcional actual

1. Autenticacion:
- Login y signup contra Firestore.
- Modo invitado (`guest@local`).

2. Perfil de alergenos:
- Seleccion y guardado local.
- Sincronizacion remota para cuenta real.

3. Comidas:
- Dataset local de recetas e imagenes.
- Busqueda por texto.
- Filtrado por conflictos de alergenos.

4. Escaner:
- Camara con CameraX + ML Kit.
- Entrada manual de codigo de barras.
- Consulta de producto en Open Food Facts.
- Comparacion de alergenos del producto vs alergenos del usuario.

5. IANutri:
- Reformulacion de pedido.
- Generacion de sugerencias.
- Asistente de cocina por receta.
- Historial remoto por email.

6. Contacto:
- Formulario local de demostracion (sin envio backend).

## 3. Inventario tecnico verificable

Metricas tomadas del codigo actual:
- Archivos versionados (`git ls-files`): 266.
- Archivos Kotlin en `app/src/main/java/com/safebyte`: 17.
- Lineas Kotlin (17 archivos): 3687.
- Entradas `FoodItem` en `MealsData.kt`: 208.
- Drawables en `app/src/main/res/drawable`: 213.
- Archivos totales en `app/src/main/res`: 232.
- Tests unitarios: no existe `app/src/test`.
- Tests instrumentados: no existe `app/src/androidTest`.

## 4. Tecnologias usadas y versiones

### 4.1 Plataforma y build
- Android nativo.
- Kotlin 1.9.24.
- Android Gradle Plugin 8.4.2.
- Gradle Wrapper 8.10.
- Java target 17.
- `compileSdk 34`, `targetSdk 34`, `minSdk 26`.

### 4.2 UI y navegacion
- Jetpack Compose BOM `2024.06.00`.
- `androidx.activity:activity-compose:1.9.0`.
- `androidx.compose.material3:material3`.
- `androidx.compose.material:material-icons-extended`.
- `androidx.navigation:navigation-compose:2.7.7`.
- `com.google.android.material:material:1.12.0`.

### 4.3 Estado y persistencia local
- `androidx.lifecycle:lifecycle-runtime-ktx:2.7.0`.
- `androidx.datastore:datastore-preferences:1.1.1`.
- Kotlin Flow + Coroutines.

### 4.4 Red y serializacion
- `com.squareup.retrofit2:retrofit:2.11.0`.
- `com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0`.
- `org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3`.
- `com.squareup.okhttp3:okhttp:4.12.0`.
- `com.squareup.okhttp3:logging-interceptor:4.12.0`.

### 4.5 Escaner y vision
- CameraX:
  - `camera-camera2:1.3.4`
  - `camera-lifecycle:1.3.4`
  - `camera-view:1.3.4`
- ML Kit Barcode:
  - `com.google.mlkit:barcode-scanning:17.2.0`
- `com.google.guava:guava:32.1.3-android`.

### 4.6 Backend de usuarios y servicios
- Firebase BOM `33.1.2`.
- `firebase-firestore-ktx`.
- `kotlinx-coroutines-play-services:1.8.1`.

### 4.7 Imagenes
- `io.coil-kt:coil-compose:2.6.0`.

## 5. Estructura del repositorio

Raiz:
- `build.gradle.kts`: plugins globales.
- `settings.gradle.kts`: repositorios y modulo `:app`.
- `gradle.properties`: parametros de build y URL de backend IA.
- `README_ANDROID.md`: guia rapida.
- `docs/android-troubleshooting.md`: incidentes comunes.
- `docs/documentacion-completa-safebyte-android.md`: este documento.
- `scripts/setup-adb-reverse.ps1`: utilitario ADB.

Modulo `app`:
- `app/build.gradle.kts`: configuracion Android, dependencias, tareas custom y guardas release.
- `app/src/main/AndroidManifest.xml`: permisos y flags de red.
- `app/src/main/java/com/safebyte/*.kt`: UI, dominio, red y persistencia.
- `app/src/main/res/*`: temas y assets.
- `app/google-services.json`: vinculacion Firebase.

## 6. Arquitectura aplicada

No se aplica Clean Architecture formal por capas. Se usa una arquitectura Compose pragmatica por feature:
- Entrada: `MainActivity`.
- Orquestacion y navegacion: `SafeByteApp`.
- Pantallas: `*Screen.kt`.
- Dominio alergenos: `AllergenUtils.kt`.
- Persistencia local: `UserPrefs.kt`.
- Persistencia remota simple: `FirebaseUsersRepo.kt`.
- Clientes de red: `IANutriApi.kt`, `OpenFoodFacts.kt`.
- Dataset local: `MealsData.kt`.

Justificacion tecnica:
- Menor coste de desarrollo para MVP.
- Curva de mantenimiento baja para equipo pequeno.

Tradeoff:
- Parte de logica de negocio en composables grandes.
- Menor testabilidad unitaria sin capa ViewModel/use-case extendida.

## 7. Flujo funcional principal

1. Arranque:
- `MainActivity` monta `SafeByteTheme` y `SafeByteApp`.

2. Sesion:
- Si no hay login, muestra `AuthScreen`.
- Si hay login, carga alergenos desde remoto/local y luego muestra shell de navegacion.

3. Navegacion:
- Rutas primarias: Home, Scanner, Comidas, IANutri.
- Rutas secundarias: Configuracion, Soporte.

4. Persistencia de alergenos:
- Siempre cache local.
- Sync Firestore solo para usuario real.
- En fallo remoto, no se bloquea la UI.

## 8. Configuracion de backend y build

### 8.1 Resolucion de `SAFEBYTE_API_BASE_URL`
Orden de prioridad:
1. Propiedad Gradle `SAFEBYTE_API_BASE_URL`.
2. Variable de entorno `SAFEBYTE_API_BASE_URL`.
3. Fallback: `http://127.0.0.1:5188/`.

Estado actual del repo:
- `gradle.properties` define `https://safebyte-5gxw.onrender.com/`.

### 8.2 Justificacion de diseno
Decision:
- URL de backend en BuildConfig y no editable en Settings.

Justificacion:
- Evita errores de operacion y soporte por configuraciones runtime inconsistentes.
- Permite control por entorno de build.

Coste:
- Menor flexibilidad para cambiar backend desde UI.

### 8.3 Guarda de release
Decision:
- Fallar build release cuando host es local (`localhost`, `127.0.0.1`, `10.0.2.2`).

Justificacion:
- Previene publicar binarios dependientes de entorno de desarrollo.

### 8.4 Automatizacion ADB reverse
Decision:
- Registrar tarea `setupAdbReverse` y engancharla a `preDebugBuild` cuando host es local.

Justificacion:
- Reduce friccion en pruebas con movil fisico.

## 9. AndroidManifest y permisos

Configuracion observada:
- `INTERNET`.
- `CAMERA`.
- `android:usesCleartextTraffic="true"`.
- `MainActivity` exportada para launcher.

Justificacion de `usesCleartextTraffic=true`:
- Permitir desarrollo con backend HTTP local.

Riesgo:
- Debe revisarse para release productivo con politica de red estricta.

## 10. Modulo de autenticacion

Archivo: `AuthScreen.kt`.

Comportamiento:
- Pantalla dual login/signup.
- Signup:
  - valida campos no vacios,
  - normaliza email,
  - verifica existencia,
  - crea documento `users/{emailLower}`.
- Login:
  - busca documento,
  - compara hash almacenado con hash calculado.
- Modo invitado:
  - `Skip Login` guarda sesion local `guest@local`.

Justificacion tecnica:
- Permitir acceso inmediato (modo invitado) sin bloquear adopcion.

Tradeoff:
- Funcionalidades remotas como historial se limitan en invitado.

## 11. Gestion de passwords

Archivo: `PasswordHasher.kt`.

Implementacion actual:
- SHA-256 + Base64 en cliente.

Justificacion historica:
- Implementacion simple para MVP academico.

Riesgo:
- No es un esquema robusto para un entorno productivo exigente.

Recomendacion:
- Migrar autenticacion a Firebase Auth o backend dedicado con KDF fuerte y salt por usuario.

## 12. Persistencia local y remota de perfil

Archivos: `UserPrefs.kt`, `FirebaseUsersRepo.kt`, `SafeByteApp.kt`.

Decision:
- Persistencia dual:
  - local (DataStore) para continuidad de uso,
  - remota (Firestore) para cuenta real.

Justificacion:
- App usable aun con conectividad inestable.

Tradeoff:
- Posibles desincronizaciones temporales entre cache local y remoto.

## 13. Dominio de alergenos

Archivo: `AllergenUtils.kt`.

Capacidades:
- Canonicalizacion de nombres.
- Normalizacion por token sin acentos/simbolos.
- Mapeo de sinonimos (ej. `soy` -> `Soja`).
- Correcciones para texto con mojibake.

Decision:
- Unificar matching de fuentes heterogeneas (dataset local, OFF, inputs usuario).

Impacto:
- Reduce falsos negativos por ortografia/codificacion.

## 14. Modulo Comidas

Archivos: `MealsScreen.kt`, `MealsData.kt`.

Comportamiento:
- Dataset embebido de 208 recetas.
- Filtro por texto y conflicto de alergenos.
- Vista tipo grid + modal detalle.

Decision:
- Datos locales en codigo.

Justificacion:
- Disponibilidad offline y respuesta inmediata.

Tradeoff:
- Mayor peso de APK.
- Mantenimiento manual de contenido.
- Parte de strings del dataset presenta problemas de codificacion.

## 15. Modulo Scanner

Archivos: `ScannerScreen.kt`, `OpenFoodFacts.kt`.

Flujo:
- Permiso de camara.
- Escaneo formatos EAN/UPC/CODE_128.
- Alternativa manual.
- Consulta Open Food Facts.
- Presentacion de conflictos con alergenos del usuario.

Decisiones tecnicas relevantes:
1. Fallback de hosts OFF (`world`, `es`, `openfoodfacts`).
2. Reintentos por host.
3. DNS preferencia IPv4.
4. Timeouts definidos (8s connect, 20s read, 25s call).
5. Control de concurrencia con `activeRequestToken` + cancelacion de job.
6. Timeout total de lookup de 15s.
7. Mensajeria especifica por tipo de error de red.

Justificacion:
- Robustez operativa en condiciones de red reales.

Tradeoff:
- Mayor complejidad de codigo respecto a un cliente HTTP basico.

## 16. Modulo IANutri

Archivos: `IANutriApi.kt`, `IANutriScreen.kt`.

Endpoints usados:
- `POST /api/IANutri/Reformulate`
- `POST /api/IANutri/GenerateSuggestions`
- `POST /api/IANutri/CookingAssistant`
- `GET /api/IANutri/History`
- `DELETE /api/IANutri/History`

Decision de compatibilidad:
- Soporte de variantes de contrato (`history/History`, `message/Message`, `deleted/Deleted`).

Justificacion:
- Tolerancia a backends con casing no uniforme.

Otras decisiones:
- Sanitizacion de reformulacion (`sanitizeReformulationSafe`).
- Normalizacion y limites de listas (`normalizeTextList`).
- Fallback local de guia de cocina si falla backend.
- Mensajes de error con pista de backend (`toApiMessage` + hint URL).

Justificacion:
- UX estable aun con respuestas incompletas o fallos de red.

## 17. Tema y experiencia visual

Archivo: `Theme.kt`.

Decision:
- Tema claro propio con paleta verde y tipografia consistente.
- `WindowCompat.setDecorFitsSystemWindows(window, false)` para control de insets.

Justificacion:
- Resolver incidencias de contraste y legibilidad.
- Mantener una identidad visual estable entre pantallas.

## 18. Resumen de decisiones tecnicas y justificaciones

1. URL backend en build, no en UI.
- Justificacion: control operacional y menos errores de usuario.
- Coste: menor flexibilidad runtime.

2. Bloqueo release con backend local.
- Justificacion: evitar despliegues rotos.
- Coste: exige configurar URL publica para release.

3. Persistencia dual local+remota de alergenos.
- Justificacion: resiliencia offline/intermitencia.
- Coste: posible divergencia temporal.

4. Modo invitado.
- Justificacion: reducir friccion de entrada.
- Coste: funciones remotas limitadas.

5. Dataset local de recetas.
- Justificacion: experiencia offline inmediata.
- Coste: app mas pesada y mantenimiento manual.

6. Cliente OFF con fallback/retry/timeout.
- Justificacion: robustez ante inestabilidad de red.
- Coste: complejidad extra.

7. Compatibilidad amplia de JSON IANutri.
- Justificacion: tolerar inconsistencias del backend.
- Coste: logica de parseo mas extensa.

8. Auth propia basada en Firestore + hash simple.
- Justificacion: rapidez de implementacion.
- Coste: deuda de seguridad para produccion.

## 19. Riesgos y deuda tecnica

Riesgos principales:
1. Esquema de autenticacion no endurecido para produccion.
2. `usesCleartextTraffic=true` activo globalmente.
3. Sin suite de tests automatizados.
4. Strings con mojibake en dataset de recetas.
5. Composables grandes con logica mezclada UI + negocio.

## 20. Operacion local y despliegue

Desarrollo local:
1. Usar JDK 17.
2. Compilar `:app:compileDebugKotlin`.
3. Si backend local en movil fisico, aplicar `adb reverse tcp:5188 tcp:5188`.

Release:
1. Definir `SAFEBYTE_API_BASE_URL` publica HTTPS.
2. Evitar hosts locales (bloqueado por Gradle).
3. Revisar politica de cleartext y endurecer red.

## 21. Recomendaciones tecnicas priorizadas

Prioridad alta:
1. Migrar autenticacion a Firebase Auth o backend auth dedicado.
2. Implementar KDF robusto para passwords (si aplica credencial propia).
3. Endurecer politica de red en release (HTTPS only + network security config).
4. Crear tests unitarios para:
- normalizacion de alergenos,
- parseo IANutri,
- mapeo OFF.

Prioridad media:
1. Extraer logica de `IANutriScreen` y `ScannerScreen` a ViewModel/capas de caso de uso.
2. Mover `MealsData` a JSON/BD para mantenimiento y versionado mas limpio.
3. Corregir codificacion de strings del dataset.

Prioridad baja:
1. Internacionalizacion completa.
2. Telemetria de errores no sensibles.
3. Optimizar assets para reducir tamano de APK.

## 22. Conclusion

SafeByte Android esta en un estado funcional de MVP solido: cubre autenticacion, perfil de riesgo alimentario, catalogo local, escaneo real y asistencia IA. Las decisiones actuales priorizan entrega rapida, experiencia estable y resiliencia de red. El siguiente salto de madurez debe centrarse en seguridad de autenticacion, automatizacion de pruebas y modularizacion de la logica de negocio.
