$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$assetsRoot = Join-Path $root "app/src/main/assets"
$databasePath = Join-Path $assetsRoot "database.json"
$twemojiCache = Join-Path $root "output/twemoji-cache"

New-Item -ItemType Directory -Force $twemojiCache | Out-Null

function New-Dir($path) {
    New-Item -ItemType Directory -Force $path | Out-Null
}

function Get-ItemId([string]$text) {
    $normalized = $text.Normalize([Text.NormalizationForm]::FormD)
    $builder = [Text.StringBuilder]::new()
    foreach ($char in $normalized.ToCharArray()) {
        $category = [Globalization.CharUnicodeInfo]::GetUnicodeCategory($char)
        if ($category -ne [Globalization.UnicodeCategory]::NonSpacingMark) {
            [void]$builder.Append($char)
        }
    }
    $id = $builder.ToString().Normalize([Text.NormalizationForm]::FormC).ToLowerInvariant()
    $id = $id -replace "\s+", "-"
    $id = $id -replace "[^a-z0-9-]", ""
    $id = $id -replace "-+", "-"
    return $id.Trim("-")
}

function New-Stimulus(
    [string]$word,
    [string]$phrase,
    [string[]]$icons,
    [string]$soundPath = $null,
    [string]$imagePrompt = $null
) {
    $id = Get-ItemId $word
    return [ordered]@{
        id = $id
        palavra = $word
        frase = $phrase
        icons = $icons
        soundPath = $soundPath
        promptImagem = if ($imagePrompt) { $imagePrompt } else { "Imagem infantil 2D clara de $word, fundo branco, sem texto" }
    }
}

$curated = @{
    "animais" = @(
        (New-Stimulus "Vaca" "a vaca" @("1f42e") "assets/sons/animais/n1_vaca-mugindo.mp3"),
        (New-Stimulus "Gato" "o gato" @("1f431") "assets/sons/animais/n1_gato-miando.mp3"),
        (New-Stimulus "Cachorro" "o cachorro" @("1f436") "assets/sons/animais/n1_cachorro-latindo.mp3"),
        (New-Stimulus "Cavalo" "o cavalo" @("1f434") "assets/sons/animais/n1_cavalo-relinchando.mp3"),
        (New-Stimulus "Macaco" "o macaco" @("1f435") "assets/sons/animais/n1_macaco-gritando.mp3"),
        (New-Stimulus "Passarinho" "o passarinho" @("1f426") "assets/sons/animais/n1_passarinho-piando.mp3"),
        (New-Stimulus "Porco" "o porco" @("1f437") "assets/sons/animais/n2_porco.mp3"),
        (New-Stimulus "Abelha" "a abelha" @("1f41d") "assets/sons/animais/n2_abelha.mp3"),
        (New-Stimulus "Bode" "o bode" @("1f410") "assets/sons/animais/n2_bode.mp3"),
        (New-Stimulus "Galinha" "a galinha" @("1f414") "assets/sons/animais/n2_galinha.mp3"),
        (New-Stimulus "Leao" "o leao" @("1f981") "assets/sons/animais/n3_leao-rugindo.mp3"),
        (New-Stimulus "Lobo" "o lobo" @("1f43a") "assets/sons/animais/n3_lobo-uivando.mp3"),
        (New-Stimulus "Grilo" "o grilo" @("1f997") "assets/sons/animais/n3_grilo-cantando.mp3"),
        (New-Stimulus "Dinossauro" "o dinossauro" @("1f996") "assets/sons/animais/n2_dinossauro.mp3")
    )
    "casa-objetos" = @(
        (New-Stimulus "Cama" "a cama" @("1f6cf-fe0f")),
        (New-Stimulus "Porta" "a porta" @("1f6aa")),
        (New-Stimulus "Sofa" "o sofa" @("1f6cb-fe0f")),
        (New-Stimulus "Livro" "o livro" @("1f4d6")),
        (New-Stimulus "Lapis" "o lapis" @("270f-fe0f")),
        (New-Stimulus "Chave" "a chave" @("1f511")),
        (New-Stimulus "Copo" "o copo" @("1f95b")),
        (New-Stimulus "Prato" "o prato" @("1f37d-fe0f")),
        (New-Stimulus "Cadeira" "a cadeira" @("1fa91")),
        (New-Stimulus "Relogio" "o relogio" @("1f552")),
        (New-Stimulus "Lampada" "a lampada" @("1f4a1")),
        (New-Stimulus "Telefone" "o telefone" @("260e-fe0f")),
        (New-Stimulus "Tesoura" "a tesoura" @("2702-fe0f")),
        (New-Stimulus "Panela" "a panela" @("1f373")),
        (New-Stimulus "Colher" "a colher" @("1f944")),
        (New-Stimulus "Computador" "o computador" @("1f4bb")),
        (New-Stimulus "Televisao" "a televisao" @("1f4fa"))
    )
    "veiculos" = @(
        (New-Stimulus "Carro" "o carro" @("1f697")),
        (New-Stimulus "Onibus" "o onibus" @("1f68c")),
        (New-Stimulus "Caminhao" "o caminhao" @("1f69a")),
        (New-Stimulus "Bicicleta" "a bicicleta" @("1f6b2")),
        (New-Stimulus "Moto" "a moto" @("1f3cd-fe0f")),
        (New-Stimulus "Barco" "o barco" @("26f5")),
        (New-Stimulus "Aviao" "o aviao" @("2708-fe0f")),
        (New-Stimulus "Trem" "o trem" @("1f682")),
        (New-Stimulus "Trator" "o trator" @("1f69c")),
        (New-Stimulus "Ambulancia" "a ambulancia" @("1f691")),
        (New-Stimulus "Foguete" "o foguete" @("1f680")),
        (New-Stimulus "Helicoptero" "o helicoptero" @("1f681"))
    )
    "corpo-roupas" = @(
        (New-Stimulus "Olho" "o olho" @("1f441-fe0f")),
        (New-Stimulus "Boca" "a boca" @("1f444")),
        (New-Stimulus "Nariz" "o nariz" @("1f443")),
        (New-Stimulus "Mao" "a mao" @("270b")),
        (New-Stimulus "Pe" "o pe" @("1f9b6")),
        (New-Stimulus "Dente" "o dente" @("1f9b7")),
        (New-Stimulus "Calca" "a calca" @("1f456")),
        (New-Stimulus "Meia" "a meia" @("1f9e6")),
        (New-Stimulus "Sapato" "o sapato" @("1f45e")),
        (New-Stimulus "Camisa" "a camisa" @("1f455")),
        (New-Stimulus "Vestido" "o vestido" @("1f457")),
        (New-Stimulus "Chapeu" "o chapeu" @("1f452")),
        (New-Stimulus "Oculos" "o oculos" @("1f453"))
    )
    "natureza" = @(
        (New-Stimulus "Sol" "o sol" @("2600-fe0f")),
        (New-Stimulus "Lua" "a lua" @("1f319")),
        (New-Stimulus "Mar" "o mar" @("1f30a")),
        (New-Stimulus "Flor" "a flor" @("1f33c")),
        (New-Stimulus "Chuva" "a chuva" @("1f327-fe0f")),
        (New-Stimulus "Vento" "o vento" @("1f343")),
        (New-Stimulus "Fogo" "o fogo" @("1f525")),
        (New-Stimulus "Agua" "a agua" @("1f4a7")),
        (New-Stimulus "Ceu" "o ceu" @("2601-fe0f")),
        (New-Stimulus "Arvore" "a arvore" @("1f333")),
        (New-Stimulus "Estrela" "a estrela" @("2b50")),
        (New-Stimulus "Nuvem" "a nuvem" @("2601-fe0f")),
        (New-Stimulus "Jardim" "o jardim" @("1f33c","1f33f","1f33c")),
        (New-Stimulus "Floresta" "a floresta" @("1f332","1f333","1f332") $null "Imagem infantil clara de floresta com varias arvores, fundo branco, sem texto"),
        (New-Stimulus "Vulcao" "o vulcao" @("1f30b")),
        (New-Stimulus "Arco-iris" "o arco-iris" @("1f308")),
        (New-Stimulus "Deserto" "o deserto" @("1f3dc-fe0f"))
    )
    "brinquedos" = @(
        (New-Stimulus "Bola" "a bola" @("26bd")),
        (New-Stimulus "Dado" "o dado" @("1f3b2")),
        (New-Stimulus "Pipa" "a pipa" @("1fa81")),
        (New-Stimulus "Corda" "a corda" @("1faa2")),
        (New-Stimulus "Bloco" "o bloco" @("1f9f1")),
        (New-Stimulus "Balao" "o balao" @("1f388")),
        (New-Stimulus "Ioio" "o ioio" @("1fa80")),
        (New-Stimulus "Ursinho" "o ursinho" @("1f9f8")),
        (New-Stimulus "Carrinho" "o carrinho" @("1f697")),
        (New-Stimulus "Tambor" "o tambor" @("1f941")),
        (New-Stimulus "Patinete" "o patinete" @("1f6f4")),
        (New-Stimulus "Videogame" "o videogame" @("1f3ae"))
    )
    "familia-pessoas" = @(
        (New-Stimulus "Mae" "a mae" @("1f469")),
        (New-Stimulus "Pai" "o pai" @("1f468")),
        (New-Stimulus "Avo" "a avo" @("1f475")),
        (New-Stimulus "Bebe" "o bebe" @("1f476")),
        (New-Stimulus "Menina" "a menina" @("1f467")),
        (New-Stimulus "Menino" "o menino" @("1f466")),
        (New-Stimulus "Professor" "o professor" @("1f9d1-200d-1f3eb")),
        (New-Stimulus "Medico" "o medico" @("1f9d1-200d-2695-fe0f")),
        (New-Stimulus "Bombeiro" "o bombeiro" @("1f9d1-200d-1f692"))
    )
}

function Get-TwemojiImage([string]$code) {
    $candidates = @($code)
    $withoutVariation = ($code -replace "-fe0f", "")
    if ($withoutVariation -ne $code) {
        $candidates += $withoutVariation
    }

    foreach ($candidate in $candidates) {
        $path = Join-Path $twemojiCache "$candidate.png"
        if (-not (Test-Path $path)) {
            $url = "https://raw.githubusercontent.com/twitter/twemoji/master/assets/72x72/$candidate.png"
            try {
                Invoke-WebRequest -Uri $url -UseBasicParsing -OutFile $path
            } catch {
                continue
            }
        }
        return $path
    }

    return $null
}

function Draw-FallbackIcon($graphics, [Drawing.Rectangle]$rect, [Drawing.Color]$color, [string]$label) {
    $pen = [Drawing.Pen]::new($color, 14)
    $brush = [Drawing.SolidBrush]::new($color)
    $font = [Drawing.Font]::new("Arial", 64, [Drawing.FontStyle]::Bold, [Drawing.GraphicsUnit]::Pixel)
    $format = [Drawing.StringFormat]::new()
    $format.Alignment = [Drawing.StringAlignment]::Center
    $format.LineAlignment = [Drawing.StringAlignment]::Center
    try {
        $graphics.DrawEllipse($pen, $rect)
        $graphics.DrawString($label.Substring(0, [Math]::Min(2, $label.Length)).ToUpperInvariant(), $font, $brush, [Drawing.RectangleF]::new($rect.X, $rect.Y, $rect.Width, $rect.Height), $format)
    } finally {
        $pen.Dispose()
        $brush.Dispose()
        $font.Dispose()
        $format.Dispose()
    }
}

function New-StimulusImage([string]$path, [string[]]$icons, [string]$label, [Drawing.Color]$categoryColor) {
    New-Dir (Split-Path -Parent $path)
    $bitmap = [Drawing.Bitmap]::new(512, 512)
    $graphics = [Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.Clear([Drawing.Color]::White)

    $lightBrush = [Drawing.SolidBrush]::new([Drawing.Color]::FromArgb(34, $categoryColor))
    $outlinePen = [Drawing.Pen]::new([Drawing.Color]::FromArgb(120, $categoryColor), 8)
    try {
        $graphics.FillEllipse($lightBrush, 42, 42, 428, 428)
        $graphics.DrawEllipse($outlinePen, 42, 42, 428, 428)

        if ($icons.Count -eq 1) {
            $iconPath = Get-TwemojiImage $icons[0]
            if ($iconPath) {
                $image = [Drawing.Image]::FromFile($iconPath)
                try {
                    $graphics.DrawImage($image, [Drawing.Rectangle]::new(116, 116, 280, 280))
                } finally {
                    $image.Dispose()
                }
            } else {
                Draw-FallbackIcon $graphics ([Drawing.Rectangle]::new(116, 116, 280, 280)) $categoryColor $label
            }
        } else {
            $positions = @(
                [Drawing.Rectangle]::new(70, 150, 180, 180),
                [Drawing.Rectangle]::new(166, 84, 180, 180),
                [Drawing.Rectangle]::new(262, 150, 180, 180)
            )
            for ($i = 0; $i -lt $icons.Count -and $i -lt $positions.Count; $i++) {
                $iconPath = Get-TwemojiImage $icons[$i]
                if ($iconPath) {
                    $image = [Drawing.Image]::FromFile($iconPath)
                    try {
                        $graphics.DrawImage($image, $positions[$i])
                    } finally {
                        $image.Dispose()
                    }
                }
            }
        }
        $bitmap.Save($path, [Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $lightBrush.Dispose()
        $outlinePen.Dispose()
        $graphics.Dispose()
        $bitmap.Dispose()
    }
}

Add-Type -AssemblyName System.Drawing

$db = Get-Content $databasePath -Raw | ConvertFrom-Json

foreach ($category in $db.categorias) {
    if (-not $curated.ContainsKey($category.id)) {
        continue
    }

    $categoryColor = [Drawing.ColorTranslator]::FromHtml($category.cor)
    $items = @()
    foreach ($stimulus in $curated[$category.id]) {
        $imagePath = "assets/imagens/$($category.id)/cur_$($stimulus.id).png"
        $soundPath = if ($stimulus.soundPath) { $stimulus.soundPath } else { "assets/sons/$($category.id)/cur_$($stimulus.id).mp3" }
        $fullImagePath = Join-Path (Join-Path $root "app/src/main") $imagePath
        New-StimulusImage $fullImagePath $stimulus.icons $stimulus.palavra $categoryColor
        $items += [ordered]@{
            id = $stimulus.id
            palavra = $stimulus.palavra
            arquivoImagem = $imagePath
            arquivoSom = $soundPath
            frase = $stimulus.frase
            promptImagem = $stimulus.promptImagem
        }
    }

    $category.niveis = @(
        [ordered]@{
            nivel = 1
            descricao = "Estimulos selecionados"
            itens = $items
        }
    )
}

$json = $db | ConvertTo-Json -Depth 20
[IO.File]::WriteAllText($databasePath, $json, [Text.UTF8Encoding]::new($false))

Write-Host "Curadoria aplicada em database.json."
foreach ($categoryId in $curated.Keys) {
    Write-Host ("{0}: {1} estimulos" -f $categoryId, $curated[$categoryId].Count)
}
