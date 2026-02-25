# SafeByte (Android - Kotlin)

Esta carpeta contiene la app Android de SafeByte/Food DNA.

## Incluido
- Login / Sign up (Firebase + DataStore para sesion local).
- Home.
- Comidas con filtro por alergenos.
- IANutri conectado a `api/IANutri`:
  - `Reformulate`
  - `GenerateSuggestions`
  - `CookingAssistant`
  - `History` (listar y borrar)
- Configuracion de alergenos.
- Scanner de codigo de barras con CameraX + ML Kit + Open Food Facts.

## Abrir en Android Studio
1. Android Studio -> Open -> carpeta `SafeByteAndroidKotlin`.
2. Espera sync de Gradle (JDK 17).
3. Ejecuta en emulador o dispositivo (API 26+).

## URL backend IANutri
La app usa `BuildConfig.SAFEBYTE_API_BASE_URL`.

Valor por defecto:

```text
http://10.0.2.2:5188/
```

Ese valor funciona para emulador Android cuando tu backend ASP.NET corre en `localhost:5188`.

Para cambiarlo, agrega en `gradle.properties`:

```properties
SAFEBYTE_API_BASE_URL=http://TU_HOST_O_IP:PUERTO/
```
