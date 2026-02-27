param(
    [int]$Port = 5188
)

$root = Split-Path -Parent $PSScriptRoot
$localProps = Join-Path $root "local.properties"

if (-not (Test-Path $localProps)) {
    Write-Error "No se encontro local.properties en $root"
    exit 1
}

$sdkLine = Get-Content $localProps | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
if (-not $sdkLine) {
    Write-Error "No se encontro sdk.dir en local.properties"
    exit 1
}

$rawSdk = $sdkLine.Substring("sdk.dir=".Length)
$sdkPath = $rawSdk.Replace("\\:", ":").Replace("\\", "\")
$adb = Join-Path $sdkPath "platform-tools\adb.exe"

if (-not (Test-Path $adb)) {
    Write-Error "No se encontro adb en: $adb"
    exit 1
}

& $adb start-server | Out-Null
$devices = & $adb devices
if (-not ($devices -match "device$")) {
    Write-Error "No hay dispositivos/emuladores conectados."
    exit 1
}

& $adb reverse "tcp:$Port" "tcp:$Port"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Fallo adb reverse para el puerto $Port."
    exit 1
}

Write-Output "adb reverse configurado: tcp:$Port -> tcp:$Port"
