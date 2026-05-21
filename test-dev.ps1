param(
    [string]$Device = "",
    [string]$Connect = "",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$adb = "C:\Users\TheusGG\AppData\Local\Android\Sdk\platform-tools\adb.exe"

if (-not (Test-Path $adb)) {
    throw "ADB nao encontrado em: $adb"
}

Push-Location $root
try {
    if (-not [string]::IsNullOrWhiteSpace($Connect)) {
        & $adb connect $Connect
    }

    if (-not $SkipBuild) {
        powershell -ExecutionPolicy Bypass -File .\build-apk.ps1 `
            -ManifestUrl "https://raw.githubusercontent.com/TheusGG/FonoLousa/main/docs/fonolousa-update.json" `
            -ApkUrl "https://github.com/TheusGG/FonoLousa/raw/main/docs/FonoLousa-debug.apk"
    }

    $deviceArgs = @()
    if (-not [string]::IsNullOrWhiteSpace($Device)) {
        $deviceArgs = @("-s", $Device)
    }

    $devices = (& $adb devices) | Select-Object -Skip 1 | Where-Object { $_ -match "\tdevice$" }
    if ($devices.Count -eq 0) {
        throw "Nenhum dispositivo ADB conectado. Abra o LDPlayer ou conecte com: powershell -ExecutionPolicy Bypass -File test-dev.ps1 -Connect 127.0.0.1:5555"
    }

    & $adb @deviceArgs install -r .\output\FonoLousa-debug.apk
    & $adb @deviceArgs shell am start -n com.fonolousa.app/.MainActivity

    Write-Host "App instalado e aberto no dispositivo/emulador."
} finally {
    Pop-Location
}
