$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$assetsRoot = Join-Path $root "app/src/main/assets"
$databasePath = Join-Path $assetsRoot "database.json"
$soundRoot = Join-Path $assetsRoot "sons/animais"
$imageRoot = Join-Path $assetsRoot "imagens/animais"
$outputDir = Join-Path $root "output"
$sourceReportPath = Join-Path $outputDir "animal-audio-sources.csv"

New-Item -ItemType Directory -Force $soundRoot, $imageRoot, $outputDir | Out-Null

$levels = @(
    @{
        nivel = 1
        descricao = "Sons mais usados"
        itens = @(
            @{ id = "vaca-mugindo"; palavra = "Vaca"; frase = "a vaca"; glyph = "🐮"; file = "n1_vaca-mugindo"; title = "Cow moo"; url = "https://assets.mixkit.co/active_storage/sfx/1744/1744-preview.mp3" },
            @{ id = "gato-miando"; palavra = "Gato"; frase = "o gato"; glyph = "🐱"; file = "n1_gato-miando"; title = "Sweet kitty meow"; url = "https://assets.mixkit.co/active_storage/sfx/93/93-preview.mp3" },
            @{ id = "cachorro-latindo"; palavra = "Cachorro"; frase = "o cachorro"; glyph = "🐶"; file = "n1_cachorro-latindo"; title = "Dog barking twice"; url = "https://assets.mixkit.co/active_storage/sfx/1/1-preview.mp3" },
            @{ id = "cavalo-relinchando"; palavra = "Cavalo"; frase = "o cavalo"; glyph = "🐴"; file = "n1_cavalo-relinchando"; title = "Scared horse neighing"; url = "https://assets.mixkit.co/active_storage/sfx/85/85-preview.mp3" },
            @{ id = "macaco-gritando"; palavra = "Macaco"; frase = "o macaco"; glyph = "🐵"; file = "n1_macaco-gritando"; title = "Cartoon monkey mocking and giggling"; url = "https://assets.mixkit.co/active_storage/sfx/108/108-preview.mp3" },
            @{ id = "passarinho-piando"; palavra = "Passarinho"; frase = "o passarinho"; glyph = "🐦"; file = "n1_passarinho-piando"; title = "Little bird calling chirp"; url = "https://assets.mixkit.co/active_storage/sfx/23/23-preview.mp3" }
        )
    },
    @{
        nivel = 2
        descricao = "Animais da fazenda"
        itens = @(
            @{ id = "vaca-no-estabulo"; palavra = "Vaca"; frase = "a vaca"; glyph = "🐮"; file = "n2_vaca-no-estabulo"; title = "Cow moo in the barn"; url = "https://assets.mixkit.co/active_storage/sfx/1751/1751-preview.mp3" },
            @{ id = "galo-cantando"; palavra = "Galo"; frase = "o galo"; glyph = "🐓"; file = "n2_galo-cantando"; title = "Rooster crowing in the morning"; url = "https://assets.mixkit.co/active_storage/sfx/2462/2462-preview.mp3" },
            @{ id = "burro-zurrando"; palavra = "Burro"; frase = "o burro"; glyph = "🐴"; file = "n2_burro-zurrando"; title = "Donkey scream"; url = "https://assets.mixkit.co/active_storage/sfx/1770/1770-preview.mp3" },
            @{ id = "gansos-gritando"; palavra = "Gansos"; frase = "os gansos"; glyph = "🪿"; file = "n2_gansos-gritando"; title = "Flock of wild geese"; url = "https://assets.mixkit.co/active_storage/sfx/20/20-preview.mp3" },
            @{ id = "gato-pequeno"; palavra = "Gato pequeno"; frase = "o gato pequeno"; glyph = "🐱"; file = "n2_gato-pequeno"; title = "Cartoon little cat meow"; url = "https://assets.mixkit.co/active_storage/sfx/91/91-preview.mp3" },
            @{ id = "cachorro-bravo"; palavra = "Cachorro bravo"; frase = "o cachorro bravo"; glyph = "🐶"; file = "n2_cachorro-bravo"; title = "Medium size angry dog bark"; url = "https://assets.mixkit.co/active_storage/sfx/54/54-preview.mp3" }
        )
    },
    @{
        nivel = 3
        descricao = "Animais selvagens"
        itens = @(
            @{ id = "leao-rugindo"; palavra = "Leão"; frase = "o leão"; glyph = "🦁"; file = "n3_leao-rugindo"; title = "Wild lion animal roar"; url = "https://assets.mixkit.co/active_storage/sfx/6/6-preview.mp3" },
            @{ id = "lobo-uivando"; palavra = "Lobo"; frase = "o lobo"; glyph = "🐺"; file = "n3_lobo-uivando"; title = "Wolf howling"; url = "https://assets.mixkit.co/active_storage/sfx/1775/1775-preview.mp3" },
            @{ id = "lobos-uivando"; palavra = "Lobos"; frase = "os lobos"; glyph = "🐺"; file = "n3_lobos-uivando"; title = "Wolves pack howling"; url = "https://assets.mixkit.co/active_storage/sfx/1776/1776-preview.mp3" },
            @{ id = "cachorro-rosnando"; palavra = "Cachorro rosnando"; frase = "o cachorro rosnando"; glyph = "🐶"; file = "n3_cachorro-rosnando"; title = "Giant dog aggressive growl"; url = "https://assets.mixkit.co/active_storage/sfx/59/59-preview.mp3" },
            @{ id = "grilo-cantando"; palavra = "Grilo"; frase = "o grilo"; glyph = "🦗"; file = "n3_grilo-cantando"; title = "Single cricket screech"; url = "https://assets.mixkit.co/active_storage/sfx/1780/1780-preview.mp3" }
        )
    },
    @{
        nivel = 4
        descricao = "Sons extras"
        itens = @(
            @{ id = "gato-pedindo"; palavra = "Gato pedindo"; frase = "o gato pedindo"; glyph = "🐱"; file = "n4_gato-pedindo"; title = "Cartoon kitty begging meow"; url = "https://assets.mixkit.co/active_storage/sfx/92/92-preview.mp3" },
            @{ id = "gato-com-dor"; palavra = "Gato miando"; frase = "o gato miando"; glyph = "🐱"; file = "n4_gato-com-dor"; title = "Little cat pain meow"; url = "https://assets.mixkit.co/active_storage/sfx/87/87-preview.mp3" },
            @{ id = "cavalo-forte"; palavra = "Cavalo forte"; frase = "o cavalo forte"; glyph = "🐴"; file = "n4_cavalo-forte"; title = "Intense horse stallion neigh"; url = "https://assets.mixkit.co/active_storage/sfx/76/76-preview.mp3" },
            @{ id = "cachorros-latindo"; palavra = "Cachorros"; frase = "os cachorros"; glyph = "🐶"; file = "n4_cachorros-latindo"; title = "Horde of barking dogs"; url = "https://assets.mixkit.co/active_storage/sfx/60/60-preview.mp3" }
        )
    }
)

function Save-Mp3($url, $path) {
    Invoke-WebRequest -Uri $url -UseBasicParsing -OutFile $path
    $file = Get-Item $path
    if ($file.Length -lt 1000) {
        throw "Downloaded audio is too small: $path"
    }
}

Add-Type -AssemblyName System.Drawing

function Draw-AnimalImage($path, $glyph, $colorHex) {
    $color = [Drawing.ColorTranslator]::FromHtml($colorHex)
    $bitmap = [Drawing.Bitmap]::new(512, 512)
    $graphics = [Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.Clear([Drawing.Color]::White)

    $circleBrush = [Drawing.SolidBrush]::new([Drawing.Color]::FromArgb(34, $color))
    $graphics.FillEllipse($circleBrush, 38, 38, 436, 436)

    $font = [Drawing.Font]::new("Segoe UI Emoji", 216, [Drawing.FontStyle]::Regular, [Drawing.GraphicsUnit]::Pixel)
    $fallbackFont = [Drawing.Font]::new("Segoe UI Symbol", 216, [Drawing.FontStyle]::Regular, [Drawing.GraphicsUnit]::Pixel)
    $brush = [Drawing.SolidBrush]::new([Drawing.Color]::FromArgb(245, 40, 40, 40))
    $format = [Drawing.StringFormat]::new()
    $format.Alignment = [Drawing.StringAlignment]::Center
    $format.LineAlignment = [Drawing.StringAlignment]::Center

    try {
        $graphics.DrawString($glyph, $font, $brush, [Drawing.RectangleF]::new(32, 24, 448, 448), $format)
    } catch {
        $graphics.DrawString($glyph, $fallbackFont, $brush, [Drawing.RectangleF]::new(32, 24, 448, 448), $format)
    }

    $bitmap.Save($path, [Drawing.Imaging.ImageFormat]::Png)
    $format.Dispose()
    $brush.Dispose()
    $font.Dispose()
    $fallbackFont.Dispose()
    $circleBrush.Dispose()
    $graphics.Dispose()
    $bitmap.Dispose()
}

$db = Get-Content $databasePath -Raw | ConvertFrom-Json
$animalCategory = $db.categorias | Where-Object { $_.id -eq "animais" }
$animalCategory.nome = "Animais"
$animalCategory.cor = "#4CAF50"
$animalCategory.niveis = @()

$soundPath = Resolve-Path $soundRoot
if ($soundPath.Path -like (Resolve-Path $root).Path + "*") {
    Get-ChildItem -LiteralPath $soundPath.Path -Filter "*.mp3" -File | Remove-Item -Force
}

$rows = @()
foreach ($level in $levels) {
    $levelJson = [ordered]@{
        nivel = $level.nivel
        descricao = $level.descricao
        itens = @()
    }

    foreach ($item in $level.itens) {
        $imageRelative = "assets/imagens/animais/$($item.file).png"
        $soundRelative = "assets/sons/animais/$($item.file).mp3"
        $imageFull = Join-Path $assetsRoot ($imageRelative.Remove(0, "assets/".Length))
        $soundFull = Join-Path $assetsRoot ($soundRelative.Remove(0, "assets/".Length))

        Draw-AnimalImage $imageFull $item.glyph "#4CAF50"
        Save-Mp3 $item.url $soundFull

        $levelJson.itens += [ordered]@{
            id = $item.id
            palavra = $item.palavra
            arquivoImagem = $imageRelative
            arquivoSom = $soundRelative
            frase = $item.frase
            promptImagem = "Imagem infantil simples de $($item.palavra), sem texto, fundo branco"
        }

        $rows += [pscustomobject]@{
            itemId = $item.id
            palavra = $item.palavra
            frase = $item.frase
            arquivoSom = $soundRelative
            provider = "Mixkit"
            title = $item.title
            url = $item.url
            license = "Free Mixkit sound effect for commercial and non-commercial projects under the Mixkit License."
        }
    }

    $animalCategory.niveis += $levelJson
}

$json = $db | ConvertTo-Json -Depth 12
[IO.File]::WriteAllText($databasePath, $json, [Text.UTF8Encoding]::new($false))
$rows | Export-Csv -NoTypeInformation -Encoding UTF8 $sourceReportPath

Write-Host "Audio-backed animal items: $($rows.Count)"
Write-Host "Database updated:          $databasePath"
Write-Host "Source report:             $sourceReportPath"


