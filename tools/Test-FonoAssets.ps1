$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$assetsRoot = Join-Path $root "app/src/main/assets"
$databasePath = Join-Path $assetsRoot "database.json"
$outputDir = Join-Path $root "output"
$reportPath = Join-Path $outputDir "asset-audit.csv"

New-Item -ItemType Directory -Force $outputDir | Out-Null

$animalSounds = [ordered]@{
    "vaca|boi" = "muuu"
    "gato" = "miau"
    "pato" = "quack quack"
    "sapo" = "coax coax"
    "rato" = "iii iii"
    "cobra" = "ssss"
    "pinto|pintinho|passarinho" = "piu piu"
    "foca" = "arf arf"
    "urso|dinossauro" = "grrr"
    "bode" = "beee"
    "macaco" = "u u a a"
    "cachorro" = "au au"
    "porco" = "oinc oinc"
    "ovelha" = "meee"
    "cavalo" = "iirrr"
    "galinha" = "cocorico"
    "abelha" = "zzzz"
    "elefante" = "pruuu"
}

function Get-AudioFallback($item, $level) {
    $source = " $($item.id -split "-" | Select-Object -First 1) $($item.palavra.ToLowerInvariant()) $($item.frase.ToLowerInvariant()) "
    foreach ($entry in $animalSounds.GetEnumerator()) {
        foreach ($token in $entry.Key.Split("|")) {
            if ($source.Contains(" $token ")) {
                return "animal:$($entry.Value)"
            }
        }
    }
    if ($level.nivel -eq 4) {
        return "tts:$($item.frase)"
    }
    return "tts:$($item.palavra)"
}

$db = Get-Content $databasePath -Raw | ConvertFrom-Json
$rows = @()

foreach ($category in $db.categorias) {
    foreach ($level in $category.niveis) {
        foreach ($item in $level.itens) {
            $imagePath = Join-Path (Join-Path $root "app/src/main") $item.arquivoImagem
            $soundPath = Join-Path (Join-Path $root "app/src/main") $item.arquivoSom
            $image = Get-Item $imagePath -ErrorAction SilentlyContinue
            $sound = Get-Item $soundPath -ErrorAction SilentlyContinue
            $audioFallback = Get-AudioFallback $item $level

            $rows += [pscustomobject]@{
                categoria = $category.id
                nivel = $level.nivel
                id = $item.id
                palavra = $item.palavra
                imagem = $item.arquivoImagem
                imagemExiste = [bool]$image
                imagemTipo = if ($image -and $image.Length -gt 120000) { "real/custom" } elseif ($image) { "placeholder" } else { "missing" }
                som = $item.arquivoSom
                somExiste = [bool]$sound
                audioFallback = $audioFallback
            }
        }
    }
}

$rows | Export-Csv -NoTypeInformation -Encoding UTF8 $reportPath

$total = $rows.Count
$realImages = ($rows | Where-Object { $_.imagemTipo -eq "real/custom" }).Count
$placeholders = ($rows | Where-Object { $_.imagemTipo -eq "placeholder" }).Count
$missingImages = ($rows | Where-Object { $_.imagemTipo -eq "missing" }).Count
$realSounds = ($rows | Where-Object { $_.somExiste }).Count
$animalFallbacks = ($rows | Where-Object { $_.audioFallback -like "animal:*" }).Count

Write-Host "FonoLousa asset audit"
Write-Host "Total itens:          $total"
Write-Host "Imagens reais/custom: $realImages"
Write-Host "Imagens placeholder:  $placeholders"
Write-Host "Imagens ausentes:     $missingImages"
Write-Host "Sons MP3 reais:       $realSounds"
Write-Host "Fallback animal/TTS:  $animalFallbacks animal, $($total - $animalFallbacks) TTS"
Write-Host "Relatorio:            $reportPath"
