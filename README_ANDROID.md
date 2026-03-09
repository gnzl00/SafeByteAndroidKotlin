# SafeByte Android (Kotlin + Compose)

Aplicacion movil de SafeByte/Food DNA.

## Modulos incluidos
- Login y Sign up (Firestore + DataStore para sesion local).
- Home.
- Comidas con filtros por alergenos.
- IANutri:
  - `POST /api/IANutri/Reformulate`
  - `POST /api/IANutri/GenerateSuggestions`
  - `POST /api/IANutri/CookingAssistant`
  - `GET/DELETE /api/IANutri/History`
- Configuracion de usuario:
  - alergenos
- Contacto.
- Scanner de codigo de barras (CameraX + ML Kit + Open Food Facts).

## Estado actual (correcciones aplicadas)
- Contraste corregido en Login, SignUp, Home, IANutri y Contacto.
- Pantalla Config estabilizada (evita cierres por layout/guardado remoto).
- Historial de IANutri robustecido para variantes de respuesta JSON (`history` y `History`).
- Mensajes de error de IANutri mejorados con pista de URL backend.
- Eliminados drawables duplicados `_copy` para bajar peso del proyecto y mejorar fluidez.

## Requisitos
- Android Studio con JDK 17.
- `minSdk 26`, `targetSdk 34`.

## Ejecucion
1. Abrir esta carpeta en Android Studio.
2. Sincronizar Gradle.
3. Ejecutar en emulador o dispositivo.

## URL backend IANutri
La app usa `BuildConfig.SAFEBYTE_API_BASE_URL`.

Si no defines nada, por defecto se usa:

```text
http://127.0.0.1:5188/
```

Notas:
- `http://127.0.0.1:5188/` es solo para desarrollo local.
- La app ya no permite cambiar la URL desde `Config`.
- En movil fisico conectado por Android Studio y backend local, usa `adb reverse`:

```powershell
adb reverse tcp:5188 tcp:5188
```

- O ejecuta el script del proyecto:

```powershell
.\scripts\setup-adb-reverse.ps1
```

- Para despliegue independiente (sin depender del ordenador), define URL publica HTTPS en `gradle.properties`.

Tambien puedes fijar el valor por build en `gradle.properties`:

```properties
SAFEBYTE_API_BASE_URL=https://TU_BACKEND_PUBLICO/
```

Comportamiento de build:
- `debug` + URL local -> intenta `adb reverse` automaticamente.
- `debug` + URL publica -> no usa `adb reverse`.
- `release` + URL local -> el build falla para evitar publicar una APK dependiente de localhost.

## Documentacion extra
- Ver [docs/android-troubleshooting.md](docs/android-troubleshooting.md)
