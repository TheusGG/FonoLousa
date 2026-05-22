param(
    [string]$Owner = "TheusGG",
    [string]$Repo = "FonoLousa",
    [string]$VersionName = "1.0.5",
    [switch]$UsePagesApk
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$tag = "v$VersionName"
$pagesBaseUrl = "https://$($Owner.ToLowerInvariant()).github.io/$Repo"
$manifestUrl = "$pagesBaseUrl/fonolousa-update.json"
$apkUrl = if ($UsePagesApk) {
    "$pagesBaseUrl/FonoLousa-debug.apk"
} else {
    "https://github.com/$Owner/$Repo/releases/download/$tag/FonoLousa-debug.apk"
}

Push-Location $root
try {
    powershell -ExecutionPolicy Bypass -File .\build-apk.ps1 -ManifestUrl $manifestUrl -ApkUrl $apkUrl

    New-Item -ItemType Directory -Force docs | Out-Null
    Copy-Item -LiteralPath output\index.html -Destination docs\index.html -Force
    Copy-Item -LiteralPath output\fonolousa-update.json -Destination docs\fonolousa-update.json -Force
    if ($UsePagesApk) {
        Copy-Item -LiteralPath output\FonoLousa-debug.apk -Destination docs\FonoLousa-debug.apk -Force
    }

    Write-Host ""
    Write-Host "GitHub Pages URL: $pagesBaseUrl/"
    Write-Host "Manifest URL:      $manifestUrl"
    Write-Host "Release tag:       $tag"
    Write-Host "APK URL:           $apkUrl"
    Write-Host ""
    Write-Host "Arquivos prontos:"
    Write-Host "  output\FonoLousa-debug.apk"
    Write-Host "  docs\index.html"
    Write-Host "  docs\fonolousa-update.json"
} finally {
    Pop-Location
}
