$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$assetsRoot = Join-Path $root "app/src/main/assets"
$databasePath = Join-Path $assetsRoot "database.json"
$imageDir = Join-Path $assetsRoot "imagens/frases"
$audioDir = Join-Path $assetsRoot "sons/frases"
$ttsWorkDir = Join-Path $env:TEMP "fonolousa-edge-tts"

New-Item -ItemType Directory -Force $imageDir | Out-Null
New-Item -ItemType Directory -Force $audioDir | Out-Null
New-Item -ItemType Directory -Force $ttsWorkDir | Out-Null

function Remove-Accent([string]$text) {
    $normalized = $text.Normalize([Text.NormalizationForm]::FormD)
    $builder = [Text.StringBuilder]::new()
    foreach ($char in $normalized.ToCharArray()) {
        $category = [Globalization.CharUnicodeInfo]::GetUnicodeCategory($char)
        if ($category -ne [Globalization.UnicodeCategory]::NonSpacingMark) {
            [void]$builder.Append($char)
        }
    }
    return $builder.ToString().Normalize([Text.NormalizationForm]::FormC)
}

function Get-Id([string]$text) {
    $id = (Remove-Accent $text).ToLowerInvariant().Trim()
    $id = $id -replace "\s+", "-"
    $id = $id -replace "[^a-z0-9-]", ""
    $id = $id -replace "-+", "-"
    return $id.Trim("-")
}

$phrases = @(
    [ordered]@{ id = "menino-jogando-bola"; palavra = "Menino jogando bola"; glyphs = @("boy", "ball") },
    [ordered]@{ id = "menina-lendo-livro"; palavra = "Menina lendo livro"; glyphs = @("girl", "book") },
    [ordered]@{ id = "crianca-escovando-os-dentes"; palavra = "Criança escovando os dentes"; glyphs = @("child", "toothbrush", "tooth") },
    [ordered]@{ id = "cachorro-correndo-no-parque"; palavra = "Cachorro correndo no parque"; glyphs = @("dog", "tree") },
    [ordered]@{ id = "gato-dormindo-no-sofa"; palavra = "Gato dormindo no sofá"; glyphs = @("cat", "sofa") },
    [ordered]@{ id = "bebe-tomando-leite"; palavra = "Bebê tomando leite"; glyphs = @("baby", "milk") },
    [ordered]@{ id = "mae-cozinhando-sopa"; palavra = "Mãe cozinhando sopa"; glyphs = @("mother", "soup") },
    [ordered]@{ id = "pai-dirigindo-o-carro"; palavra = "Pai dirigindo o carro"; glyphs = @("father", "car") },
    [ordered]@{ id = "passaro-voando-no-ceu"; palavra = "Pássaro voando no céu"; glyphs = @("bird", "cloud") },
    [ordered]@{ id = "menino-lavando-as-maos"; palavra = "Menino lavando as mãos"; glyphs = @("boy", "water", "hands") }
)

$db = Get-Content -LiteralPath $databasePath -Raw | ConvertFrom-Json
$existingCategories = @($db.categorias | Where-Object { $_.id -ne "frases" })

$phraseItems = @()
foreach ($phrase in $phrases) {
    $id = if ($phrase.id) { $phrase.id } else { Get-Id $phrase.palavra }
    $phraseItems += [ordered]@{
        id = $id
        palavra = $phrase.palavra
        arquivoImagem = "assets/imagens/frases/$id.png"
        arquivoSom = "assets/sons/frases/$id.mp3"
        frase = $phrase.palavra
        promptImagem = "High-quality 2D flat vector sticker, white background, thick outlines, vibrant colors, child-friendly, representing $($phrase.palavra)"
    }
}

$phraseCategory = [ordered]@{
    id = "frases"
    nome = "Frases"
    cor = "#00BCD4"
    niveis = @(
        [ordered]@{
            nivel = 1
            descricao = "Frases simples"
            itens = $phraseItems
        }
    )
}

$db.categorias = @($existingCategories) + @($phraseCategory)
$json = $db | ConvertTo-Json -Depth 12
[IO.File]::WriteAllText($databasePath, $json, [Text.UTF8Encoding]::new($false))

Add-Type -AssemblyName System.Drawing

$sceneColors = @{
    "boy" = [Drawing.Color]::FromArgb(255, 105, 180)
    "girl" = [Drawing.Color]::FromArgb(255, 151, 177)
    "child" = [Drawing.Color]::FromArgb(255, 193, 7)
    "dog" = [Drawing.Color]::FromArgb(141, 85, 36)
    "cat" = [Drawing.Color]::FromArgb(255, 152, 0)
    "baby" = [Drawing.Color]::FromArgb(129, 212, 250)
    "mother" = [Drawing.Color]::FromArgb(233, 30, 99)
    "father" = [Drawing.Color]::FromArgb(33, 150, 243)
    "bird" = [Drawing.Color]::FromArgb(3, 169, 244)
    "ball" = [Drawing.Color]::FromArgb(76, 175, 80)
    "book" = [Drawing.Color]::FromArgb(156, 39, 176)
    "toothbrush" = [Drawing.Color]::FromArgb(0, 188, 212)
    "tooth" = [Drawing.Color]::White
    "tree" = [Drawing.Color]::FromArgb(76, 175, 80)
    "sofa" = [Drawing.Color]::FromArgb(102, 187, 106)
    "milk" = [Drawing.Color]::White
    "soup" = [Drawing.Color]::FromArgb(255, 112, 67)
    "car" = [Drawing.Color]::FromArgb(239, 83, 80)
    "cloud" = [Drawing.Color]::White
    "water" = [Drawing.Color]::FromArgb(79, 195, 247)
    "hands" = [Drawing.Color]::FromArgb(255, 204, 188)
}

function New-Pen([Drawing.Color]$color, [int]$width = 8) {
    return [Drawing.Pen]::new([Drawing.Color]::FromArgb(255, 45, 45, 45), $width)
}

function New-Brush([Drawing.Color]$color) {
    return [Drawing.SolidBrush]::new($color)
}

function Draw-Person($graphics, [string]$kind, [int]$x, [int]$y, [int]$scale) {
    $skin = [Drawing.Color]::FromArgb(255, 215, 170, 125)
    $hair = [Drawing.Color]::FromArgb(80, 45, 30)
    $shirt = $sceneColors[$kind]
    $outline = New-Pen $shirt 8
    $skinBrush = New-Brush $skin
    $hairBrush = New-Brush $hair
    $shirtBrush = New-Brush $shirt
    try {
        $graphics.FillEllipse($skinBrush, $x + 28, $y, 82, 82)
        $graphics.DrawEllipse($outline, $x + 28, $y, 82, 82)
        if ($kind -in @("girl", "mother")) {
            $graphics.FillPie($hairBrush, $x + 18, $y - 12, 104, 92, 180, 180)
        } else {
            $graphics.FillPie($hairBrush, $x + 28, $y - 10, 82, 42, 180, 180)
        }
        $graphics.FillEllipse([Drawing.Brushes]::Black, $x + 52, $y + 34, 8, 12)
        $graphics.FillEllipse([Drawing.Brushes]::Black, $x + 78, $y + 34, 8, 12)
        $graphics.DrawArc($outline, $x + 55, $y + 48, 34, 18, 20, 140)
        $graphics.FillPie($shirtBrush, $x + 12, $y + 88, 114, 142, 180, 180)
        $graphics.DrawArc($outline, $x + 12, $y + 88, 114, 142, 180, 180)
        $graphics.DrawLine($outline, $x + 34, $y + 118, $x - 2, $y + 172)
        $graphics.DrawLine($outline, $x + 104, $y + 118, $x + 142, $y + 172)
        $graphics.DrawLine($outline, $x + 48, $y + 220, $x + 30, $y + 292)
        $graphics.DrawLine($outline, $x + 90, $y + 220, $x + 112, $y + 292)
    } finally {
        $outline.Dispose()
        $skinBrush.Dispose()
        $hairBrush.Dispose()
        $shirtBrush.Dispose()
    }
}

function Draw-Object($graphics, [string]$kind, [int]$x, [int]$y) {
    $color = $sceneColors[$kind]
    $outline = New-Pen $color 9
    $brush = New-Brush $color
    $light = New-Brush ([Drawing.Color]::FromArgb(64, $color))
    try {
        switch ($kind) {
            "ball" {
                $graphics.FillEllipse($brush, $x, $y, 116, 116)
                $graphics.DrawEllipse($outline, $x, $y, 116, 116)
                $graphics.DrawArc($outline, $x + 12, $y + 20, 92, 76, 20, 140)
                $graphics.DrawLine($outline, $x + 58, $y, $x + 58, $y + 116)
            }
            "book" {
                $graphics.FillRectangle($brush, $x, $y, 144, 96)
                $graphics.DrawRectangle($outline, $x, $y, 144, 96)
                $graphics.DrawLine($outline, $x + 72, $y, $x + 72, $y + 96)
                $graphics.DrawLine($outline, $x + 22, $y + 28, $x + 56, $y + 28)
                $graphics.DrawLine($outline, $x + 88, $y + 28, $x + 122, $y + 28)
            }
            "toothbrush" {
                $graphics.FillRectangle($brush, $x, $y + 58, 164, 24)
                $graphics.DrawRectangle($outline, $x, $y + 58, 164, 24)
                $graphics.DrawRectangle($outline, $x + 118, $y + 20, 46, 52)
                $graphics.DrawLine($outline, $x + 126, $y + 20, $x + 126, $y - 6)
                $graphics.DrawLine($outline, $x + 142, $y + 20, $x + 142, $y - 6)
                $graphics.DrawLine($outline, $x + 158, $y + 20, $x + 158, $y - 6)
            }
            "tooth" {
                $graphics.FillPie([Drawing.Brushes]::White, $x, $y, 106, 126, 180, 180)
                $graphics.DrawArc($outline, $x, $y, 106, 126, 180, 180)
                $graphics.DrawLine($outline, $x + 14, $y + 62, $x + 32, $y + 128)
                $graphics.DrawLine($outline, $x + 72, $y + 128, $x + 92, $y + 62)
            }
            "tree" {
                $trunk = New-Brush ([Drawing.Color]::FromArgb(121, 85, 72))
                $graphics.FillRectangle($trunk, $x + 48, $y + 92, 30, 86)
                $graphics.FillEllipse($brush, $x, $y, 126, 120)
                $graphics.DrawEllipse($outline, $x, $y, 126, 120)
                $trunk.Dispose()
            }
            "sofa" {
                $graphics.FillRectangle($brush, $x + 10, $y + 64, 170, 86)
                $graphics.DrawRectangle($outline, $x + 10, $y + 64, 170, 86)
                $graphics.FillRectangle($light, $x + 28, $y + 20, 132, 70)
                $graphics.DrawRectangle($outline, $x + 28, $y + 20, 132, 70)
                $graphics.DrawLine($outline, $x + 42, $y + 150, $x + 22, $y + 184)
                $graphics.DrawLine($outline, $x + 150, $y + 150, $x + 172, $y + 184)
            }
            "milk" {
                $graphics.FillRectangle([Drawing.Brushes]::White, $x + 28, $y, 82, 146)
                $graphics.DrawRectangle($outline, $x + 28, $y, 82, 146)
                $graphics.DrawLine($outline, $x + 28, $y + 38, $x + 110, $y + 38)
                $graphics.DrawString("LEITE", [Drawing.Font]::new("Arial", 18, [Drawing.FontStyle]::Bold), [Drawing.Brushes]::SteelBlue, $x + 32, $y + 70)
            }
            "soup" {
                $graphics.FillEllipse($brush, $x, $y + 44, 166, 76)
                $graphics.DrawEllipse($outline, $x, $y + 44, 166, 76)
                $graphics.DrawArc($outline, $x + 18, $y, 44, 74, 200, 120)
                $graphics.DrawArc($outline, $x + 76, $y, 44, 74, 200, 120)
            }
            "car" {
                $graphics.FillRectangle($brush, $x, $y + 56, 180, 78)
                $graphics.DrawRectangle($outline, $x, $y + 56, 180, 78)
                $graphics.FillRectangle($light, $x + 48, $y + 10, 86, 52)
                $graphics.DrawRectangle($outline, $x + 48, $y + 10, 86, 52)
                $graphics.FillEllipse([Drawing.Brushes]::White, $x + 24, $y + 116, 42, 42)
                $graphics.FillEllipse([Drawing.Brushes]::White, $x + 122, $y + 116, 42, 42)
                $graphics.DrawEllipse($outline, $x + 24, $y + 116, 42, 42)
                $graphics.DrawEllipse($outline, $x + 122, $y + 116, 42, 42)
            }
            "cloud" {
                $graphics.FillEllipse([Drawing.Brushes]::White, $x, $y + 28, 90, 72)
                $graphics.FillEllipse([Drawing.Brushes]::White, $x + 54, $y, 102, 96)
                $graphics.FillEllipse([Drawing.Brushes]::White, $x + 128, $y + 38, 74, 62)
                $graphics.DrawArc($outline, $x, $y + 28, 90, 72, 180, 180)
                $graphics.DrawArc($outline, $x + 54, $y, 102, 96, 180, 180)
                $graphics.DrawArc($outline, $x + 128, $y + 38, 74, 62, 180, 180)
                $graphics.DrawLine($outline, $x + 18, $y + 100, $x + 188, $y + 100)
            }
            "water" {
                $graphics.FillEllipse($brush, $x, $y, 62, 92)
                $graphics.DrawEllipse($outline, $x, $y, 62, 92)
                $graphics.FillEllipse($brush, $x + 54, $y + 40, 54, 72)
                $graphics.DrawEllipse($outline, $x + 54, $y + 40, 54, 72)
            }
            "hands" {
                $graphics.FillEllipse($brush, $x, $y, 76, 56)
                $graphics.FillEllipse($brush, $x + 72, $y, 76, 56)
                $graphics.DrawEllipse($outline, $x, $y, 76, 56)
                $graphics.DrawEllipse($outline, $x + 72, $y, 76, 56)
            }
        }
    } finally {
        $outline.Dispose()
        $brush.Dispose()
        $light.Dispose()
    }
}

function Draw-Animal($graphics, [string]$kind, [int]$x, [int]$y) {
    $color = $sceneColors[$kind]
    $outline = New-Pen $color 9
    $brush = New-Brush $color
    try {
        if ($kind -eq "bird") {
            $graphics.FillEllipse($brush, $x + 30, $y + 30, 120, 86)
            $graphics.DrawEllipse($outline, $x + 30, $y + 30, 120, 86)
            $graphics.FillPie($brush, $x, $y + 14, 96, 112, 320, 140)
            $graphics.DrawArc($outline, $x, $y + 14, 96, 112, 320, 140)
            $graphics.DrawLine($outline, $x + 146, $y + 68, $x + 188, $y + 52)
            $graphics.DrawLine($outline, $x + 146, $y + 78, $x + 188, $y + 94)
            $graphics.FillEllipse([Drawing.Brushes]::Black, $x + 124, $y + 56, 10, 10)
            $graphics.DrawLine($outline, $x + 88, $y + 108, $x + 74, $y + 154)
            $graphics.DrawLine($outline, $x + 116, $y + 108, $x + 130, $y + 154)
            return
        }
        $graphics.FillEllipse($brush, $x + 40, $y + 66, 164, 102)
        $graphics.DrawEllipse($outline, $x + 40, $y + 66, 164, 102)
        $graphics.FillEllipse($brush, $x, $y + 24, 104, 100)
        $graphics.DrawEllipse($outline, $x, $y + 24, 104, 100)
        $graphics.FillEllipse([Drawing.Brushes]::Black, $x + 32, $y + 62, 10, 12)
        $graphics.FillEllipse([Drawing.Brushes]::Black, $x + 66, $y + 62, 10, 12)
        if ($kind -eq "cat") {
            $graphics.DrawPolygon($outline, [Drawing.Point[]]@([Drawing.Point]::new($x + 18, $y + 34), [Drawing.Point]::new($x + 36, $y - 8), [Drawing.Point]::new($x + 56, $y + 36)))
            $graphics.DrawPolygon($outline, [Drawing.Point[]]@([Drawing.Point]::new($x + 76, $y + 36), [Drawing.Point]::new($x + 94, $y - 8), [Drawing.Point]::new($x + 104, $y + 40)))
        } else {
            $graphics.FillEllipse($brush, $x - 12, $y + 48, 28, 58)
            $graphics.FillEllipse($brush, $x + 88, $y + 48, 28, 58)
        }
        $graphics.DrawArc($outline, $x + 184, $y + 34, 70, 90, 110, 140)
        $graphics.DrawLine($outline, $x + 82, $y + 156, $x + 66, $y + 216)
        $graphics.DrawLine($outline, $x + 166, $y + 156, $x + 184, $y + 216)
    } finally {
        $outline.Dispose()
        $brush.Dispose()
    }
}

foreach ($phrase in $phrases) {
    $imagePath = Join-Path $imageDir "$($phrase.id).png"
    $bitmap = [Drawing.Bitmap]::new(900, 620)
    $graphics = [Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.Clear([Drawing.Color]::White)
    $accent = [Drawing.ColorTranslator]::FromHtml("#00BCD4")
    $outline = New-Pen $accent 10
    $soft = New-Brush ([Drawing.Color]::FromArgb(35, $accent))
    $graphics.FillEllipse($soft, 88, 34, 724, 524)
    $graphics.DrawEllipse($outline, 88, 34, 724, 524)

    switch ($phrase.id) {
        "menino-jogando-bola" {
            Draw-Person $graphics "boy" 220 118 1
            Draw-Object $graphics "ball" 560 330
        }
        "menina-lendo-livro" {
            Draw-Person $graphics "girl" 214 120 1
            Draw-Object $graphics "book" 520 282
        }
        "crianca-escovando-os-dentes" {
            Draw-Person $graphics "child" 176 126 1
            Draw-Object $graphics "toothbrush" 460 224
            Draw-Object $graphics "tooth" 620 190
        }
        "cachorro-correndo-no-parque" {
            Draw-Animal $graphics "dog" 214 240
            Draw-Object $graphics "tree" 594 170
        }
        "gato-dormindo-no-sofa" {
            Draw-Object $graphics "sofa" 432 276
            Draw-Animal $graphics "cat" 198 262
        }
        "bebe-tomando-leite" {
            Draw-Person $graphics "baby" 250 122 1
            Draw-Object $graphics "milk" 542 224
        }
        "mae-cozinhando-sopa" {
            Draw-Person $graphics "mother" 208 120 1
            Draw-Object $graphics "soup" 532 260
        }
        "pai-dirigindo-o-carro" {
            Draw-Person $graphics "father" 160 100 1
            Draw-Object $graphics "car" 488 292
        }
        "passaro-voando-no-ceu" {
            Draw-Animal $graphics "bird" 256 206
            Draw-Object $graphics "cloud" 512 162
        }
        "menino-lavando-as-maos" {
            Draw-Person $graphics "boy" 178 124 1
            Draw-Object $graphics "water" 526 226
            Draw-Object $graphics "hands" 586 338
        }
    }

    $bitmap.Save($imagePath, [Drawing.Imaging.ImageFormat]::Png)
    $outline.Dispose()
    $soft.Dispose()
    $graphics.Dispose()
    $bitmap.Dispose()
}

$nodeModule = Join-Path $ttsWorkDir "node_modules/@seepine/edge-tts/dist/index.mjs"
if (-not (Test-Path $nodeModule)) {
    cmd /c npm install @seepine/edge-tts --prefix "$ttsWorkDir"
    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao instalar modulo de TTS neural."
    }
}

$ttsPayload = [ordered]@{
    moduleUrl = ([System.Uri]$nodeModule).AbsoluteUri
    phrases = @($phrases | ForEach-Object {
        [ordered]@{
            text = $_.palavra
            output = (Join-Path $audioDir "$($_.id).mp3")
        }
    })
} | ConvertTo-Json -Depth 5 -Compress

$nodeCode = @'
import fs from 'node:fs';
const payload = JSON.parse(process.env.FONO_PHRASE_TTS_PAYLOAD);
const { EdgeTTS } = await import(payload.moduleUrl);
const tts = new EdgeTTS({
  voice: 'pt-BR-FranciscaNeural',
  lang: 'pt-BR',
  outputFormat: 'audio-24khz-96kbitrate-mono-mp3',
  rate: '-6%',
  pitch: '+0%'
});
for (const phrase of payload.phrases) {
  if (fs.existsSync(phrase.output) && fs.statSync(phrase.output).size > 8000) continue;
  const response = await tts.call(`${phrase.text}.`);
  fs.writeFileSync(phrase.output, response.data);
}
'@

$env:FONO_PHRASE_TTS_PAYLOAD = $ttsPayload
node --input-type=module -e $nodeCode
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao gerar audios neurais das frases."
}

Write-Host "Modulo Frases atualizado: $($phrases.Count) imagens e MP3s em assets."
