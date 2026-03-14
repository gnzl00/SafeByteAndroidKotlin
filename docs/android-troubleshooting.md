# Android Troubleshooting - SafeByte

Documento actualizado: 2026-03-14

## 1) Configuracion se abre y la app se cierra

Verificaciones:
- Confirmar que estas usando la version actual del proyecto.
- Revisar Logcat para excepciones en `SettingsScreen` o sincronizacion Firestore.

Comportamiento esperado actual:
- Si falla guardado remoto de alergenos, la app no debe cerrarse.
- Debe mantenerse persistencia local y mostrar feedback de guardado.

## 2) Texto con bajo contraste en Login/Home/Contacto

Comportamiento esperado:
- Los campos y textos usan colores explicitos de alto contraste.

Si se ve incorrecto:
1. Limpia la app instalada (desinstalar/reinstalar).
2. Recompila con `clean` + `assembleDebug`.
3. Verifica que no haya overlays de accesibilidad forzando colores.

## 3) IANutri no conecta

La app usa `BuildConfig.SAFEBYTE_API_BASE_URL`.

Orden de resolucion:
1. `SAFEBYTE_API_BASE_URL` en Gradle.
2. Variable de entorno del mismo nombre.
3. Fallback interno: `http://127.0.0.1:5188/`.

Nota del repo actual:
- `gradle.properties` ya define URL publica: `https://safebyte-5gxw.onrender.com/`.

Si pruebas backend local en movil fisico:

```powershell
adb reverse tcp:5188 tcp:5188
```

Alternativa:

```powershell
.\scripts\setup-adb-reverse.ps1
```

## 4) Build release falla por URL backend

Sintoma:
- Gradle falla al pedir `release` o `bundle` con URL local.

Causa esperada:
- Existe una guarda en `app/build.gradle.kts` que bloquea release con:
  - `localhost`
  - `127.0.0.1`
  - `10.0.2.2`

Solucion:
- Definir una URL publica HTTPS en `SAFEBYTE_API_BASE_URL`.

## 5) Historial IANutri no carga o no borra

Checklist:
1. Confirmar que la sesion no sea `guest@local`.
2. Verificar respuesta de backend para:
- `GET /api/IANutri/History?email=...`
- `DELETE /api/IANutri/History?email=...`
3. Revisar conectividad y logs de backend.

Nota:
- El cliente soporta variantes `history/History` y `message/Message`.

## 6) Scanner no detecta o falla consulta de producto

Checklist rapido:
1. Permiso de camara concedido.
2. Codigo valido (8 a 14 digitos).
3. Conexion a internet disponible.

Comportamiento esperado:
- La app reintenta consulta OFF por multiples hosts.
- Mensajes distinguen fallos DNS, timeout, TLS o conexion.

Si persiste:
- Probar ingreso manual del codigo en la caja de texto.
- Validar el mismo codigo en Open Food Facts web.

## 7) Firestore devuelve `PERMISSION_DENIED` o `UNAUTHENTICATED`

Causa habitual:
- Reglas de Firestore no permiten la operacion actual.

Acciones:
1. Revisar reglas del proyecto Firebase asociado.
2. Verificar que `google-services.json` corresponde al paquete `com.safebyte`.
3. Confirmar conectividad de red y proyecto Firebase correcto.

## 8) Build local falla con JDK

Requisito:
- JDK 17.

Comando recomendado en PowerShell:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:compileDebugKotlin
```

## 9) APK/proyecto demasiado pesado

Causas principales:
- Catalogo de recetas e imagenes locales grande.

Mitigaciones:
1. Optimizar/comprimir drawables fuente.
2. Evaluar mover dataset a formato externo (JSON/DB).
3. Revisar shrink/minify de release.

## 10) Datos de recetas con caracteres raros

Sintoma:
- Textos con codificacion incorrecta (mojibake) en algunas recetas.

Estado:
- El dominio de alergenos tiene correcciones de normalizacion para matching, pero el contenido visual puede seguir mostrando strings degradados.

Solucion recomendada:
- Normalizar codificacion UTF-8 del dataset en `MealsData.kt` o migrarlo a fuente de datos controlada.
