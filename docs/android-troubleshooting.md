# Android Troubleshooting

## 1) Configuracion abre y la app se cierra
- Revisar que la app este en esta version (se removio grid lazy anidado y se protegieron guardados remotos).
- Si falla al guardar alergenos por red/Firestore, la app ya no debe cerrarse.

## 2) No se ve texto en Login/SignUp/Contacto
- Se forzo tema claro y colores explicitos de campos (`text`, `label`, `placeholder`, `cursor`, `border`).
- Si se ven estilos viejos, desinstalar app y reinstalar para limpiar estado local.

## 3) Home no muestra bien "Como usar"
- Los textos de esa seccion ahora usan color explicito de alto contraste.

## 4) IANutri no conecta
La app Android usa `BuildConfig.SAFEBYTE_API_BASE_URL` (sin configuracion manual en pantalla).

Valor por defecto:
- `http://127.0.0.1:5188/`

Notas:
- Si usas movil fisico por Android Studio, habilita tunel ADB:

```powershell
adb reverse tcp:5188 tcp:5188
```

- O ejecuta:

```powershell
.\scripts\setup-adb-reverse.ps1
```

- Para APK real fuera de Android Studio, usa backend publico:

```properties
SAFEBYTE_API_BASE_URL=https://tu-backend-publico/
```

## 5) Historial de IANutri no carga
- Historial remoto solo aplica con cuenta real (no `guest@local`).
- Se soportan respuestas `history` y `History`.
- Validar que backend responda `GET /api/IANutri/History?email=...`.

## 6) APK/proyecto pesa demasiado
- Se eliminaron drawables duplicados con sufijo `_copy`.
- Si el repo sigue pesado, revisar imagenes originales de gran resolucion y considerar compresion.

## 7) Build falla en local
- Usar JDK 17 (Android Studio `jbr`).
- Comando de verificacion usado en este proyecto:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path=\"$env:JAVA_HOME\\bin;$env:Path\"
.\gradlew.bat :app:compileDebugKotlin
```
