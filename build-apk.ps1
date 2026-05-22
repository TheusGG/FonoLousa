param(
    [string]$BaseUrl = "",
    [string]$ApkUrl = "",
    [string]$ManifestUrl = ""
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$androidStudioJdk = "C:\Program Files\Android\Android Studio\jbr"
$androidSdk = "C:\Users\TheusGG\AppData\Local\Android\Sdk"
$gradleBin = Join-Path $root "tools\gradle-9.2.1\bin"

if (-not (Test-Path (Join-Path $androidStudioJdk "bin\java.exe"))) {
    throw "Java do Android Studio nao encontrado em: $androidStudioJdk"
}

if (-not (Test-Path $androidSdk)) {
    throw "Android SDK nao encontrado em: $androidSdk"
}

if (-not (Test-Path (Join-Path $gradleBin "gradle.bat"))) {
    throw "Gradle local nao encontrado em: $gradleBin"
}

$env:JAVA_HOME = $androidStudioJdk
$env:ANDROID_HOME = $androidSdk
$env:ANDROID_SDK_ROOT = $androidSdk
$env:PATH = "$androidStudioJdk\bin;$gradleBin;$androidSdk\platform-tools;$env:PATH"

Push-Location $root
try {
    $gradleArgs = @("assembleDebug")
    if (-not [string]::IsNullOrWhiteSpace($ManifestUrl)) {
        $gradleArgs += "-Pfonolousa.updateManifestUrl=$ManifestUrl"
    }

    gradle @gradleArgs
    New-Item -ItemType Directory -Force output | Out-Null
    Copy-Item -LiteralPath app\build\outputs\apk\debug\app-debug.apk -Destination output\FonoLousa-debug.apk -Force
    $resolvedApkUrl = if (-not [string]::IsNullOrWhiteSpace($ApkUrl)) {
        $ApkUrl
    } elseif ([string]::IsNullOrWhiteSpace($BaseUrl)) {
        "https://SEU-DOMINIO-OU-GITHUB/FonoLousa-debug.apk"
    } else {
        $BaseUrl.TrimEnd("/") + "/FonoLousa-debug.apk"
    }
    $manifest = [ordered]@{
        app = "FonoLousa"
        versionCode = 6
        versionName = "1.0.5"
        apkUrl = $resolvedApkUrl
        notes = "Correcao dos sons: removidos audios duvidosos e mantidos apenas MP3s reais/confiaveis; demais itens usam TTS."
    } | ConvertTo-Json -Depth 4
    [IO.File]::WriteAllText((Join-Path $root "output\fonolousa-update.json"), $manifest, [Text.UTF8Encoding]::new($false))

    $html = @"
<!doctype html>
<html lang="pt-BR">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>FonoLousa - Download</title>
  <style>
    body { font-family: Arial, sans-serif; margin: 32px; background: #1B5E20; color: #fff; }
    main { max-width: 720px; margin: auto; }
    a { display: inline-block; margin-top: 20px; padding: 16px 22px; background: #FFC107; color: #1a1a1a; border-radius: 8px; font-weight: 700; text-decoration: none; }
    code { background: rgba(255,255,255,.14); padding: 3px 6px; border-radius: 4px; }
  </style>
</head>
<body>
  <main>
    <h1>FonoLousa</h1>
    <p>Versao 1.0.5 de teste para instalacao em tablet Android.</p>
    <a href="FonoLousa-debug.apk">Baixar APK</a>
    <p>Manifesto de atualizacao: <code>fonolousa-update.json</code></p>
  </main>
</body>
</html>
"@
    [IO.File]::WriteAllText((Join-Path $root "output\index.html"), $html, [Text.UTF8Encoding]::new($false))
    Write-Host "APK gerado em: $root\output\FonoLousa-debug.apk"
    Write-Host "Manifesto gerado em: $root\output\fonolousa-update.json"
    Write-Host "Pagina gerada em: $root\output\index.html"
    if (-not [string]::IsNullOrWhiteSpace($ManifestUrl)) {
        Write-Host "APK configurado para consultar: $ManifestUrl"
    }
} finally {
    Pop-Location
}
