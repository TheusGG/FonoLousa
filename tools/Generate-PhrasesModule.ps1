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

function New-Phrase($text, $assets) {
    [ordered]@{
        id = Get-Id $text
        palavra = $text
        assets = $assets
    }
}

$phrases = @(
    (New-Phrase "Menino jogando bola" @("familia-pessoas/cur_menino.png", "brinquedos/cur_bola.png"))
    (New-Phrase "Menina lendo livro" @("familia-pessoas/cur_menina.png", "casa-objetos/cur_livro.png"))
    (New-Phrase "Criança escovando os dentes" @("familia-pessoas/cur_menino.png", "corpo-roupas/cur_dente.png"))
    (New-Phrase "Cachorro correndo no parque" @("animais/cur_cachorro.png", "natureza/cur_arvore.png"))
    (New-Phrase "Gato dormindo no sofá" @("animais/cur_gato.png", "casa-objetos/cur_sofa.png"))
    (New-Phrase "Bebê tomando leite" @("familia-pessoas/cur_bebe.png", "alimentos/cur_leite.png"))
    (New-Phrase "Mãe preparando sopa" @("familia-pessoas/cur_mae.png", "alimentos/cur_sopa.png"))
    (New-Phrase "Pai dirigindo o carro" @("familia-pessoas/cur_pai.png", "veiculos/cur_carro.png"))
    (New-Phrase "Pássaro voando no céu" @("animais/cur_passarinho.png", "natureza/cur_ceu.png"))
    (New-Phrase "Menino lavando as mãos" @("familia-pessoas/cur_menino.png", "corpo-roupas/cur_mao.png", "natureza/cur_agua.png"))
    (New-Phrase "Menina comendo bolo" @("familia-pessoas/cur_menina.png", "alimentos/cur_bolo.png"))
    (New-Phrase "Menino bebendo suco" @("familia-pessoas/cur_menino.png", "alimentos/cur_suco.png"))
    (New-Phrase "Menina chutando bola" @("familia-pessoas/cur_menina.png", "brinquedos/cur_bola.png"))
    (New-Phrase "Menina soltando pipa" @("familia-pessoas/cur_menina.png", "brinquedos/cur_pipa.png"))
    (New-Phrase "Menino tocando tambor" @("familia-pessoas/cur_menino.png", "brinquedos/cur_tambor.png"))
    (New-Phrase "Criança brincando com carrinho" @("familia-pessoas/cur_menino.png", "brinquedos/cur_carrinho.png"))
    (New-Phrase "Bebê dormindo na cama" @("familia-pessoas/cur_bebe.png", "casa-objetos/cur_cama.png"))
    (New-Phrase "Menina segurando balão" @("familia-pessoas/cur_menina.png", "brinquedos/cur_balao.png"))
    (New-Phrase "Menino andando de bicicleta" @("familia-pessoas/cur_menino.png", "veiculos/cur_bicicleta.png"))
    (New-Phrase "Mãe lendo livro" @("familia-pessoas/cur_mae.png", "casa-objetos/cur_livro.png"))
    (New-Phrase "Pai falando no telefone" @("familia-pessoas/cur_pai.png", "casa-objetos/cur_telefone.png"))
    (New-Phrase "Cachorro perto da porta" @("animais/cur_cachorro.png", "casa-objetos/cur_porta.png"))
    (New-Phrase "Gato olhando a lua" @("animais/cur_gato.png", "natureza/cur_lua.png"))
    (New-Phrase "Menina vendo a flor" @("familia-pessoas/cur_menina.png", "natureza/cur_flor.png"))
)

$db = Get-Content -LiteralPath $databasePath -Raw | ConvertFrom-Json
$existingCategories = @($db.categorias | Where-Object { $_.id -ne "frases" })

$phraseItems = @()
foreach ($phrase in $phrases) {
    $phraseItems += [ordered]@{
        id = $phrase.id
        palavra = $phrase.palavra
        arquivoImagem = "assets/imagens/frases/$($phrase.id).png"
        arquivoSom = "assets/sons/frases/$($phrase.id).mp3"
        frase = $phrase.palavra
        promptImagem = "Composição offline usando os mesmos ícones curados das categorias do FonoLousa."
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

function New-RoundRect([int]$x, [int]$y, [int]$width, [int]$height, [int]$radius) {
    $path = [Drawing.Drawing2D.GraphicsPath]::new()
    $diameter = $radius * 2
    $path.AddArc($x, $y, $diameter, $diameter, 180, 90)
    $path.AddArc($x + $width - $diameter, $y, $diameter, $diameter, 270, 90)
    $path.AddArc($x + $width - $diameter, $y + $height - $diameter, $diameter, $diameter, 0, 90)
    $path.AddArc($x, $y + $height - $diameter, $diameter, $diameter, 90, 90)
    $path.CloseFigure()
    return $path
}

function Draw-Asset($graphics, [string]$relativePath, [Drawing.Rectangle]$target) {
    $path = Join-Path (Join-Path $assetsRoot "imagens") $relativePath
    if (-not (Test-Path $path)) {
        throw "Imagem base ausente para frase: $relativePath"
    }
    $image = [Drawing.Image]::FromFile((Resolve-Path $path))
    try {
        $graphics.DrawImage($image, $target)
    } finally {
        $image.Dispose()
    }
}

function Draw-SceneImage($phrase) {
    $imagePath = Join-Path $imageDir "$($phrase.id).png"
    $bitmap = [Drawing.Bitmap]::new(512, 512)
    $graphics = [Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.CompositingQuality = [Drawing.Drawing2D.CompositingQuality]::HighQuality
    $graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::HighQuality

    $accent = [Drawing.ColorTranslator]::FromHtml("#00BCD4")
    $soft = [Drawing.SolidBrush]::new([Drawing.Color]::FromArgb(26, $accent))
    $border = [Drawing.Pen]::new([Drawing.Color]::FromArgb(180, $accent), 6)
    $shadow = [Drawing.SolidBrush]::new([Drawing.Color]::FromArgb(18, 0, 0, 0))
    $white = [Drawing.SolidBrush]::new([Drawing.Color]::White)
    try {
        $graphics.Clear([Drawing.Color]::White)
        $graphics.FillEllipse($soft, 34, 34, 444, 444)
        $graphics.DrawEllipse($border, 34, 34, 444, 444)

        $assetCount = $phrase.assets.Count
        if ($assetCount -eq 2) {
            $rects = @(
                [Drawing.Rectangle]::new(54, 142, 210, 210),
                [Drawing.Rectangle]::new(248, 142, 210, 210)
            )
        } else {
            $rects = @(
                [Drawing.Rectangle]::new(38, 132, 190, 190),
                [Drawing.Rectangle]::new(284, 132, 190, 190),
                [Drawing.Rectangle]::new(161, 268, 190, 190)
            )
        }

        for ($i = 0; $i -lt $assetCount; $i++) {
            $rect = $rects[$i]
            $panel = New-RoundRect ($rect.X + 10) ($rect.Y + 10) ($rect.Width - 20) ($rect.Height - 20) 32
            $graphics.FillPath($shadow, $panel)
            $panel.Dispose()
            Draw-Asset $graphics $phrase.assets[$i] $rect
        }
    } finally {
        $soft.Dispose()
        $border.Dispose()
        $shadow.Dispose()
        $white.Dispose()
        $graphics.Dispose()
        $bitmap.Save($imagePath, [Drawing.Imaging.ImageFormat]::Png)
        $bitmap.Dispose()
    }
}

$activeImages = @($phrases | ForEach-Object { "$($_.id).png" })
$activeAudio = @($phrases | ForEach-Object { "$($_.id).mp3" })
Get-ChildItem -LiteralPath $imageDir -File -Filter "*.png" | Where-Object { $_.Name -notin $activeImages } | Remove-Item -Force
Get-ChildItem -LiteralPath $audioDir -File -Filter "*.mp3" | Where-Object { $_.Name -notin $activeAudio } | Remove-Item -Force

foreach ($phrase in $phrases) {
    Draw-SceneImage $phrase
}

$nodeModule = Join-Path $ttsWorkDir "node_modules/@seepine/edge-tts/dist/index.mjs"
if (-not (Test-Path $nodeModule)) {
    cmd /c npm install @seepine/edge-tts --prefix "$ttsWorkDir"
    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao instalar módulo de TTS neural."
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
    throw "Falha ao gerar áudios neurais das frases."
}

Write-Host "Módulo Frases atualizado: $($phrases.Count) imagens e MP3s em assets."
