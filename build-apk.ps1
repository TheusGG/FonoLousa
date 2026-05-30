param(
    [string]$BaseUrl = "",
    [string]$ApkUrl = "",
    [string]$ManifestUrl = "",
    [ValidateSet("Debug", "Release")]
    [string]$BuildType = "Debug",
    [string]$ApkFileName = "",
    [string]$ReleaseNotes = "Auditoria tecnica, melhorias no relatorio clinico, banco local e canal de atualizacao."
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
    $buildFile = Join-Path $root "app\build.gradle.kts"
    $buildText = Get-Content -LiteralPath $buildFile -Raw
    $versionCodeMatch = [regex]::Match($buildText, "versionCode\s*=\s*(\d+)")
    $versionNameMatch = [regex]::Match($buildText, "versionName\s*=\s*`"([^`"]+)`"")

    if (-not $versionCodeMatch.Success -or -not $versionNameMatch.Success) {
        throw "Nao foi possivel ler versionCode/versionName em app\build.gradle.kts."
    }

    $versionCode = [int]$versionCodeMatch.Groups[1].Value
    $versionName = $versionNameMatch.Groups[1].Value

    $gradleArgs = @("assemble$BuildType")
    if (-not [string]::IsNullOrWhiteSpace($ManifestUrl)) {
        $gradleArgs += "-Pfonolousa.updateManifestUrl=$ManifestUrl"
    }

    gradle @gradleArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle falhou com codigo $LASTEXITCODE. APK nao foi atualizado."
    }
    New-Item -ItemType Directory -Force output | Out-Null
    $buildTypeLower = $BuildType.ToLowerInvariant()
    $outputApkName = if (-not [string]::IsNullOrWhiteSpace($ApkFileName)) {
        $ApkFileName
    } else {
        "FonoLousa-$buildTypeLower.apk"
    }
    Copy-Item -LiteralPath "app\build\outputs\apk\$buildTypeLower\app-$buildTypeLower.apk" -Destination "output\$outputApkName" -Force
    $resolvedApkUrl = if (-not [string]::IsNullOrWhiteSpace($ApkUrl)) {
        $ApkUrl
    } elseif ([string]::IsNullOrWhiteSpace($BaseUrl)) {
        "https://SEU-DOMINIO-OU-GITHUB/$outputApkName"
    } else {
        $BaseUrl.TrimEnd("/") + "/$outputApkName"
    }
    $manifest = [ordered]@{
        app = "FonoLousa"
        versionCode = $versionCode
        versionName = $versionName
        apkUrl = $resolvedApkUrl
        notes = $ReleaseNotes
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
    <p>Vers&atilde;o $versionName ($versionCode) para instala&ccedil;&atilde;o em celular ou tablet Android.</p>
    <a href="$outputApkName">Baixar APK</a>
    <p>Manifesto de atualiza&ccedil;&atilde;o: <code>fonolousa-update.json</code></p>
  </main>
</body>
</html>
"@
    [IO.File]::WriteAllText((Join-Path $root "output\index.html"), $html, [Text.UTF8Encoding]::new($false))
    Write-Host "APK gerado em: $root\output\$outputApkName"
    Write-Host "Manifesto gerado em: $root\output\fonolousa-update.json"
    Write-Host "Pagina gerada em: $root\output\index.html"
    if (-not [string]::IsNullOrWhiteSpace($ManifestUrl)) {
        Write-Host "APK configurado para consultar: $ManifestUrl"
    }
} finally {
    Pop-Location
}
