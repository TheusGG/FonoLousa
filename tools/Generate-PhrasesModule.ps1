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

function New-Phrase([string]$text, [string]$scene) {
    [ordered]@{
        id = Get-Id $text
        palavra = $text
        scene = $scene
    }
}

function Join-Text($parts) {
    return -join $parts
}

$phrases = @(
    (New-Phrase "Menino jogando bola." "soccer")
    (New-Phrase (Join-Text @("Menina comendo ma", [char]0x00E7, [char]0x00E3, ".")) "apple")
    (New-Phrase "Cachorro correndo no parque." "dog-run")
    (New-Phrase (Join-Text @("Gato dormindo no sof", [char]0x00E1, ".")) "cat-sofa")
    (New-Phrase (Join-Text @("Crian", [char]0x00E7, "a lendo um livro.")) "reading")
    (New-Phrase "Papai lavando o carro." "washing-car")
    (New-Phrase (Join-Text @("Mam", [char]0x00E3, "e fazendo um bolo.")) "cake")
    (New-Phrase (Join-Text @("Vov", [char]0x00F4, " andando de bicicleta.")) "grandpa-bike")
    (New-Phrase (Join-Text @("Beb", [char]0x00EA, " brincando com blocos.")) "baby-blocks")
    (New-Phrase "Menino tomando banho." "bath")
    (New-Phrase "Menina escovando os dentes." "brush-teeth")
    (New-Phrase (Join-Text @("Fam", [char]0x00ED, "lia jantando na mesa.")) "family-dinner")
    (New-Phrase (Join-Text @("P", [char]0x00E1, "ssaro voando no c", [char]0x00E9, "u.")) "bird-sky")
    (New-Phrase (Join-Text @("Peixe nadando no aqu", [char]0x00E1, "rio.")) "fish-tank")
    (New-Phrase "Sol brilhando muito forte." "sun")
    (New-Phrase (Join-Text @("Crian", [char]0x00E7, "a pulando corda.")) "jump-rope")
    (New-Phrase "Menino vestindo a camiseta." "shirt")
    (New-Phrase (Join-Text @("Menina bebendo ", [char]0x00E1, "gua gelada.")) "water")
    (New-Phrase "Cachorro latindo para o gato." "dog-cat")
    (New-Phrase "Papai lendo o jornal." "newspaper")
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
        promptImagem = "High-quality 2D flat vector sticker, white background, thick outlines, vibrant colors, child-friendly, representing: $($phrase.palavra)"
    }
}

$phraseCategory = [ordered]@{
    id = "frases"
    nome = "Frases"
    cor = "#00BCD4"
    niveis = @(
        [ordered]@{
            nivel = 4
            descricao = "Ações"
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

function New-Brush([string]$color, [int]$alpha = 255) {
    $c = [Drawing.ColorTranslator]::FromHtml($color)
    return [Drawing.SolidBrush]::new([Drawing.Color]::FromArgb($alpha, $c.R, $c.G, $c.B))
}

function New-Pen([string]$color, [int]$width, [int]$alpha = 255) {
    $c = [Drawing.ColorTranslator]::FromHtml($color)
    $pen = [Drawing.Pen]::new([Drawing.Color]::FromArgb($alpha, $c.R, $c.G, $c.B), $width)
    $pen.StartCap = [Drawing.Drawing2D.LineCap]::Round
    $pen.EndCap = [Drawing.Drawing2D.LineCap]::Round
    $pen.LineJoin = [Drawing.Drawing2D.LineJoin]::Round
    return $pen
}

function Fill-Round($g, [int]$x, [int]$y, [int]$w, [int]$h, [int]$r, [string]$color, [int]$alpha = 255) {
    $path = New-RoundRect $x $y $w $h $r
    $brush = New-Brush $color $alpha
    try { $g.FillPath($brush, $path) } finally { $brush.Dispose(); $path.Dispose() }
}

function Draw-Round($g, [int]$x, [int]$y, [int]$w, [int]$h, [int]$r, [string]$color, [int]$width, [int]$alpha = 255) {
    $path = New-RoundRect $x $y $w $h $r
    $pen = New-Pen $color $width $alpha
    try { $g.DrawPath($pen, $path) } finally { $pen.Dispose(); $path.Dispose() }
}

function Draw-Ellipse($g, [int]$x, [int]$y, [int]$w, [int]$h, [string]$fill, [string]$stroke = "#2F3437", [int]$strokeWidth = 6) {
    $brush = New-Brush $fill
    $pen = New-Pen $stroke $strokeWidth
    try {
        $g.FillEllipse($brush, $x, $y, $w, $h)
        $g.DrawEllipse($pen, $x, $y, $w, $h)
    } finally {
        $brush.Dispose()
        $pen.Dispose()
    }
}

function Draw-Line($g, [int]$x1, [int]$y1, [int]$x2, [int]$y2, [string]$color = "#2F3437", [int]$width = 10) {
    $pen = New-Pen $color $width
    try { $g.DrawLine($pen, $x1, $y1, $x2, $y2) } finally { $pen.Dispose() }
}

function Draw-SceneBackground($g, [string]$mode = "outdoor") {
    $shadowPath = New-RoundRect 70 72 628 628 86
    $shadow = New-Brush "#000000" 22
    $scenePath = New-RoundRect 62 56 628 628 86
    $borderWhite = New-Pen "#FFFFFF" 28
    $borderGray = New-Pen "#C9CED1" 4
    try {
        $g.FillPath($shadow, $shadowPath)
        $g.SetClip($scenePath)
        if ($mode -eq "indoor") {
            $g.Clear([Drawing.Color]::White)
            $wall = New-Brush "#FFF3E0"
            $floor = New-Brush "#E9CFA9"
            $g.FillRectangle($wall, 62, 56, 628, 390)
            $g.FillRectangle($floor, 62, 446, 628, 238)
            $wall.Dispose()
            $floor.Dispose()
        } elseif ($mode -eq "water") {
            $sky = New-Brush "#CDEFFF"
            $water = New-Brush "#66D6F2"
            $g.FillRectangle($sky, 62, 56, 628, 260)
            $g.FillRectangle($water, 62, 316, 628, 368)
            $sky.Dispose()
            $water.Dispose()
        } else {
            $sky = New-Brush "#BCEBFF"
            $grass = New-Brush "#8BC34A"
            $g.FillRectangle($sky, 62, 56, 628, 395)
            $g.FillRectangle($grass, 62, 451, 628, 233)
            $sky.Dispose()
            $grass.Dispose()
        }
        $g.ResetClip()
        $g.DrawPath($borderWhite, $scenePath)
        $g.DrawPath($borderGray, $scenePath)
    } finally {
        $shadow.Dispose()
        $shadowPath.Dispose()
        $scenePath.Dispose()
        $borderWhite.Dispose()
        $borderGray.Dispose()
    }
}

function Draw-Cloud($g, [int]$x, [int]$y, [double]$s = 1.0) {
    $brush = New-Brush "#FFFFFF" 235
    try {
        $g.FillEllipse($brush, $x, $y + [int](20 * $s), [int](70 * $s), [int](42 * $s))
        $g.FillEllipse($brush, $x + [int](36 * $s), $y, [int](72 * $s), [int](62 * $s))
        $g.FillEllipse($brush, $x + [int](86 * $s), $y + [int](18 * $s), [int](78 * $s), [int](46 * $s))
        $g.FillRectangle($brush, $x + [int](30 * $s), $y + [int](36 * $s), [int](110 * $s), [int](30 * $s))
    } finally {
        $brush.Dispose()
    }
}

function Draw-Sun($g, [int]$x, [int]$y, [int]$size = 86) {
    $ray = New-Pen "#FFD54F" 9
    try {
        for ($i = 0; $i -lt 12; $i++) {
            $a = $i * [Math]::PI / 6
            $cx = $x + ($size / 2)
            $cy = $y + ($size / 2)
            $x1 = [int]($cx + [Math]::Cos($a) * ($size * 0.68))
            $y1 = [int]($cy + [Math]::Sin($a) * ($size * 0.68))
            $x2 = [int]($cx + [Math]::Cos($a) * ($size * 0.98))
            $y2 = [int]($cy + [Math]::Sin($a) * ($size * 0.98))
            $g.DrawLine($ray, $x1, $y1, $x2, $y2)
        }
    } finally {
        $ray.Dispose()
    }
    Draw-Ellipse $g $x $y $size $size "#FFE66D" "#FBC02D" 5
    Draw-Ellipse $g ($x + [int]($size * 0.28)) ($y + [int]($size * 0.34)) 9 13 "#5D4037" "#5D4037" 1
    Draw-Ellipse $g ($x + [int]($size * 0.60)) ($y + [int]($size * 0.34)) 9 13 "#5D4037" "#5D4037" 1
    $smile = New-Pen "#5D4037" 4
    try { $g.DrawArc($smile, $x + [int]($size * 0.33), $y + [int]($size * 0.43), [int]($size * 0.34), [int]($size * 0.28), 20, 140) } finally { $smile.Dispose() }
}

function Draw-Tree($g, [int]$x, [int]$y, [double]$s = 1.0) {
    Fill-Round $g ($x + [int](22 * $s)) ($y + [int](70 * $s)) ([int](26 * $s)) ([int](82 * $s)) 8 "#8D5A2B"
    Draw-Ellipse $g $x $y ([int](76 * $s)) ([int](88 * $s)) "#5DBB4B" "#347C36" 4
    Draw-Ellipse $g ($x - [int](26 * $s)) ($y + [int](34 * $s)) ([int](68 * $s)) ([int](74 * $s)) "#6FD05E" "#347C36" 4
    Draw-Ellipse $g ($x + [int](42 * $s)) ($y + [int](40 * $s)) ([int](68 * $s)) ([int](74 * $s)) "#6FD05E" "#347C36" 4
}

function Draw-Person($g, [int]$x, [int]$baseY, [double]$s, [string]$kind, [string]$pose, [string]$shirt = "#2196F3") {
    $sc = { param($n) [int][Math]::Round($n * $s) }
    $skin = "#FFD56B"
    $hair = if ($kind -in @("menina", "mae")) { "#5D4037" } elseif ($kind -eq "avo") { "#DADADA" } else { "#8D4B24" }
    $bottom = if ($kind -eq "bebe") { "#FFCA28" } else { "#FFFFFF" }
    $outline = "#2F3437"

    $bodyX = $x - (& $sc 34)
    $bodyY = $baseY - (& $sc 158)
    $bodyW = & $sc 68
    $bodyH = if ($kind -eq "bebe") { & $sc 78 } else { & $sc 108 }

    $armPen = New-Pen $skin (& $sc 16)
    $legPen = New-Pen $skin (& $sc 17)
    $outlinePen = New-Pen $outline (& $sc 5)
    try {
        Fill-Round $g $bodyX $bodyY $bodyW $bodyH (& $sc 22) $shirt
        Draw-Round $g $bodyX $bodyY $bodyW $bodyH (& $sc 22) $outline (& $sc 5)

        $shoulderY = $bodyY + (& $sc 22)
        if ($pose -eq "eating" -or $pose -eq "drinking" -or $pose -eq "brush") {
            $g.DrawLine($armPen, $bodyX + (& $sc 8), $shoulderY, $x - (& $sc 32), $bodyY + (& $sc 54))
            $g.DrawLine($armPen, $bodyX + $bodyW - (& $sc 8), $shoulderY, $x + (& $sc 38), $bodyY - (& $sc 22))
        } elseif ($pose -eq "jump" -or $pose -eq "dressing") {
            $g.DrawLine($armPen, $bodyX + (& $sc 8), $shoulderY, $x - (& $sc 70), $bodyY - (& $sc 48))
            $g.DrawLine($armPen, $bodyX + $bodyW - (& $sc 8), $shoulderY, $x + (& $sc 70), $bodyY - (& $sc 48))
        } elseif ($pose -eq "running") {
            $g.DrawLine($armPen, $bodyX + (& $sc 8), $shoulderY, $x - (& $sc 70), $bodyY + (& $sc 70))
            $g.DrawLine($armPen, $bodyX + $bodyW - (& $sc 8), $shoulderY, $x + (& $sc 70), $bodyY + (& $sc 6))
        } else {
            $g.DrawLine($armPen, $bodyX + (& $sc 8), $shoulderY, $x - (& $sc 62), $bodyY + (& $sc 60))
            $g.DrawLine($armPen, $bodyX + $bodyW - (& $sc 8), $shoulderY, $x + (& $sc 62), $bodyY + (& $sc 60))
        }

        if ($kind -ne "bebe") {
            if ($pose -eq "running" -or $pose -eq "jump") {
                $g.DrawLine($legPen, $x - (& $sc 18), $bodyY + $bodyH - (& $sc 4), $x - (& $sc 70), $baseY - (& $sc 8))
                $g.DrawLine($legPen, $x + (& $sc 18), $bodyY + $bodyH - (& $sc 4), $x + (& $sc 76), $baseY - (& $sc 2))
            } else {
                $g.DrawLine($legPen, $x - (& $sc 18), $bodyY + $bodyH - (& $sc 4), $x - (& $sc 36), $baseY)
                $g.DrawLine($legPen, $x + (& $sc 18), $bodyY + $bodyH - (& $sc 4), $x + (& $sc 36), $baseY)
            }
            Draw-Line $g ($x - (& $sc 55)) ($baseY + (& $sc 2)) ($x - (& $sc 22)) ($baseY + (& $sc 2)) $outline (& $sc 8)
            Draw-Line $g ($x + (& $sc 22)) ($baseY + (& $sc 2)) ($x + (& $sc 58)) ($baseY + (& $sc 2)) $outline (& $sc 8)
        } else {
            Draw-Ellipse $g ($x - (& $sc 68)) ($baseY - (& $sc 62)) (& $sc 136) (& $sc 72) $bottom $outline (& $sc 5)
        }
    } finally {
        $armPen.Dispose()
        $legPen.Dispose()
        $outlinePen.Dispose()
    }

    $headSize = & $sc 82
    $headX = $x - [int]($headSize / 2)
    $headY = $bodyY - (& $sc 86)
    Draw-Ellipse $g $headX $headY $headSize $headSize $skin $outline (& $sc 5)

    if ($kind -in @("menina", "mae")) {
        Draw-Ellipse $g ($headX - (& $sc 10)) ($headY + (& $sc 8)) (& $sc 100) (& $sc 88) $hair $outline (& $sc 4)
        Draw-Ellipse $g ($headX + (& $sc 8)) ($headY + (& $sc 8)) (& $sc 66) (& $sc 64) $skin $skin 1
    } elseif ($kind -eq "avo") {
        Draw-Ellipse $g ($headX + (& $sc 5)) ($headY - (& $sc 8)) (& $sc 72) (& $sc 36) $hair $outline (& $sc 4)
        Draw-Ellipse $g ($headX + (& $sc 9)) ($headY + (& $sc 32)) (& $sc 24) (& $sc 16) "#FFFFFF" $outline (& $sc 3)
        Draw-Ellipse $g ($headX + (& $sc 49)) ($headY + (& $sc 32)) (& $sc 24) (& $sc 16) "#FFFFFF" $outline (& $sc 3)
        Draw-Line $g ($headX + (& $sc 33)) ($headY + (& $sc 40)) ($headX + (& $sc 49)) ($headY + (& $sc 40)) $outline (& $sc 3)
    } else {
        $hairBrush = New-Brush $hair
        try { $g.FillPie($hairBrush, $headX, $headY - (& $sc 12), $headSize, (& $sc 56), 180, 180) } finally { $hairBrush.Dispose() }
    }

    Draw-Ellipse $g ($headX + (& $sc 22)) ($headY + (& $sc 35)) (& $sc 7) (& $sc 11) "#2F3437" "#2F3437" 1
    Draw-Ellipse $g ($headX + (& $sc 53)) ($headY + (& $sc 35)) (& $sc 7) (& $sc 11) "#2F3437" "#2F3437" 1
    $mouth = New-Pen "#8D4B24" (& $sc 4)
    try { $g.DrawArc($mouth, $headX + (& $sc 27), $headY + (& $sc 45), (& $sc 30), (& $sc 18), 10, 160) } finally { $mouth.Dispose() }
}

function Draw-SoccerBall($g, [int]$x, [int]$y, [int]$size) {
    Draw-Ellipse $g $x $y $size $size "#FFFFFF" "#2F3437" 5
    $black = New-Brush "#2F3437"
    try {
        $points = @(
            [Drawing.Point]::new($x + [int]($size * 0.50), $y + [int]($size * 0.22)),
            [Drawing.Point]::new($x + [int]($size * 0.66), $y + [int]($size * 0.38)),
            [Drawing.Point]::new($x + [int]($size * 0.60), $y + [int]($size * 0.60)),
            [Drawing.Point]::new($x + [int]($size * 0.40), $y + [int]($size * 0.60)),
            [Drawing.Point]::new($x + [int]($size * 0.34), $y + [int]($size * 0.38))
        )
        $g.FillPolygon($black, $points)
        $g.FillEllipse($black, $x + [int]($size * 0.12), $y + [int]($size * 0.34), [int]($size * 0.16), [int]($size * 0.22))
        $g.FillEllipse($black, $x + [int]($size * 0.72), $y + [int]($size * 0.34), [int]($size * 0.16), [int]($size * 0.22))
        $g.FillEllipse($black, $x + [int]($size * 0.42), $y + [int]($size * 0.76), [int]($size * 0.18), [int]($size * 0.14))
    } finally {
        $black.Dispose()
    }
}

function Draw-Apple($g, [int]$x, [int]$y, [int]$s) {
    Draw-Ellipse $g $x $y $s $s "#E53935" "#2F3437" 5
    Draw-Line $g ($x + [int]($s * 0.52)) ($y + [int]($s * 0.10)) ($x + [int]($s * 0.58)) ($y - [int]($s * 0.20)) "#6D4C41" 8
    Draw-Ellipse $g ($x + [int]($s * 0.58)) ($y - [int]($s * 0.25)) ([int]($s * 0.34)) ([int]($s * 0.20)) "#5DBB4B" "#347C36" 4
}

function Draw-Book($g, [int]$x, [int]$y, [int]$w, [int]$h) {
    Fill-Round $g $x $y $w $h 20 "#42A5F5"
    Draw-Round $g $x $y $w $h 20 "#2F3437" 5
    Draw-Line $g ($x + [int]($w / 2)) ($y + 10) ($x + [int]($w / 2)) ($y + $h - 10) "#FFFFFF" 4
    Draw-Line $g ($x + 30) ($y + 42) ($x + [int]($w / 2) - 18) ($y + 42) "#FFFFFF" 4
    Draw-Line $g ($x + [int]($w / 2) + 18) ($y + 42) ($x + $w - 30) ($y + 42) "#FFFFFF" 4
}

function Draw-Dog($g, [int]$x, [int]$y, [double]$s = 1.0, [bool]$barking = $false) {
    $sc = { param($n) [int][Math]::Round($n * $s) }
    Draw-Ellipse $g $x $y (& $sc 150) (& $sc 90) "#A76A43" "#2F3437" (& $sc 5)
    Draw-Ellipse $g ($x + (& $sc 112)) ($y - (& $sc 58)) (& $sc 88) (& $sc 82) "#B8784D" "#2F3437" (& $sc 5)
    Draw-Ellipse $g ($x + (& $sc 130)) ($y - (& $sc 34)) (& $sc 18) (& $sc 20) "#2F3437" "#2F3437" 1
    Draw-Ellipse $g ($x + (& $sc 166)) ($y - (& $sc 34)) (& $sc 18) (& $sc 20) "#2F3437" "#2F3437" 1
    Draw-Ellipse $g ($x + (& $sc 150)) ($y - (& $sc 10)) (& $sc 22) (& $sc 16) "#2F3437" "#2F3437" 1
    Draw-Ellipse $g ($x + (& $sc 102)) ($y - (& $sc 52)) (& $sc 26) (& $sc 58) "#6D3D25" "#2F3437" (& $sc 4)
    Draw-Ellipse $g ($x + (& $sc 176)) ($y - (& $sc 52)) (& $sc 26) (& $sc 58) "#6D3D25" "#2F3437" (& $sc 4)
    Draw-Line $g ($x + (& $sc 16)) ($y + (& $sc 76)) ($x + (& $sc 2)) ($y + (& $sc 116)) "#2F3437" (& $sc 9)
    Draw-Line $g ($x + (& $sc 72)) ($y + (& $sc 80)) ($x + (& $sc 58)) ($y + (& $sc 122)) "#2F3437" (& $sc 9)
    Draw-Line $g ($x + (& $sc 128)) ($y + (& $sc 78)) ($x + (& $sc 118)) ($y + (& $sc 118)) "#2F3437" (& $sc 9)
    Draw-Line $g ($x + (& $sc 6)) ($y + (& $sc 18)) ($x - (& $sc 46)) ($y - (& $sc 18)) "#A76A43" (& $sc 12)
    if ($barking) {
        Draw-Line $g ($x + (& $sc 220)) ($y - (& $sc 18)) ($x + (& $sc 252)) ($y - (& $sc 40)) "#2F3437" (& $sc 5)
        Draw-Line $g ($x + (& $sc 222)) ($y + (& $sc 6)) ($x + (& $sc 262)) ($y + (& $sc 6)) "#2F3437" (& $sc 5)
    }
}

function Draw-Cat($g, [int]$x, [int]$y, [double]$s = 1.0) {
    $sc = { param($n) [int][Math]::Round($n * $s) }
    Draw-Ellipse $g $x $y (& $sc 140) (& $sc 76) "#FF9800" "#2F3437" (& $sc 5)
    Draw-Ellipse $g ($x + (& $sc 100)) ($y - (& $sc 48)) (& $sc 78) (& $sc 74) "#FFA726" "#2F3437" (& $sc 5)
    $ear = New-Brush "#FFA726"
    $outline = New-Pen "#2F3437" (& $sc 5)
    try {
        $g.FillPolygon($ear, @([Drawing.Point]::new($x + (& $sc 110), $y - (& $sc 38)), [Drawing.Point]::new($x + (& $sc 122), $y - (& $sc 78)), [Drawing.Point]::new($x + (& $sc 140), $y - (& $sc 40))))
        $g.DrawPolygon($outline, @([Drawing.Point]::new($x + (& $sc 110), $y - (& $sc 38)), [Drawing.Point]::new($x + (& $sc 122), $y - (& $sc 78)), [Drawing.Point]::new($x + (& $sc 140), $y - (& $sc 40))))
        $g.FillPolygon($ear, @([Drawing.Point]::new($x + (& $sc 146), $y - (& $sc 40)), [Drawing.Point]::new($x + (& $sc 166), $y - (& $sc 78)), [Drawing.Point]::new($x + (& $sc 174), $y - (& $sc 36))))
        $g.DrawPolygon($outline, @([Drawing.Point]::new($x + (& $sc 146), $y - (& $sc 40)), [Drawing.Point]::new($x + (& $sc 166), $y - (& $sc 78)), [Drawing.Point]::new($x + (& $sc 174), $y - (& $sc 36))))
    } finally {
        $ear.Dispose()
        $outline.Dispose()
    }
    Draw-Line $g ($x + (& $sc 20)) ($y + (& $sc 20)) ($x - (& $sc 28)) ($y - (& $sc 22)) "#FFA726" (& $sc 11)
    Draw-Line $g ($x + (& $sc 128)) ($y - (& $sc 10)) ($x + (& $sc 148)) ($y - (& $sc 2)) "#2F3437" (& $sc 4)
    Draw-Line $g ($x + (& $sc 166)) ($y - (& $sc 10)) ($x + (& $sc 146)) ($y - (& $sc 2)) "#2F3437" (& $sc 4)
}

function Draw-Sofa($g, [int]$x, [int]$y, [int]$w, [int]$h) {
    Fill-Round $g $x ($y + [int]($h * 0.25)) $w ([int]($h * 0.62)) 34 "#7CB342"
    Draw-Round $g $x ($y + [int]($h * 0.25)) $w ([int]($h * 0.62)) 34 "#2F3437" 5
    Fill-Round $g ($x + 22) $y ($w - 44) ([int]($h * 0.45)) 34 "#8BC34A"
    Draw-Round $g ($x + 22) $y ($w - 44) ([int]($h * 0.45)) 34 "#2F3437" 5
}

function Draw-Car($g, [int]$x, [int]$y, [int]$w, [int]$h) {
    Fill-Round $g $x ($y + [int]($h * 0.28)) $w ([int]($h * 0.46)) 24 "#EF5350"
    Draw-Round $g $x ($y + [int]($h * 0.28)) $w ([int]($h * 0.46)) 24 "#2F3437" 5
    Fill-Round $g ($x + [int]($w * 0.18)) $y ([int]($w * 0.48)) ([int]($h * 0.40)) 24 "#EF5350"
    Draw-Round $g ($x + [int]($w * 0.18)) $y ([int]($w * 0.48)) ([int]($h * 0.40)) 24 "#2F3437" 5
    Fill-Round $g ($x + [int]($w * 0.25)) ($y + [int]($h * 0.08)) ([int]($w * 0.16)) ([int]($h * 0.22)) 10 "#BBDEFB"
    Fill-Round $g ($x + [int]($w * 0.45)) ($y + [int]($h * 0.08)) ([int]($w * 0.16)) ([int]($h * 0.22)) 10 "#BBDEFB"
    Draw-Ellipse $g ($x + [int]($w * 0.14)) ($y + [int]($h * 0.62)) ([int]($h * 0.30)) ([int]($h * 0.30)) "#2F3437" "#2F3437" 1
    Draw-Ellipse $g ($x + [int]($w * 0.68)) ($y + [int]($h * 0.62)) ([int]($h * 0.30)) ([int]($h * 0.30)) "#2F3437" "#2F3437" 1
}

function Draw-Bike($g, [int]$x, [int]$y, [int]$s) {
    Draw-Ellipse $g $x $y $s $s "#FFFFFF" "#2F3437" 7
    Draw-Ellipse $g ($x + [int]($s * 1.85)) $y $s $s "#FFFFFF" "#2F3437" 7
    Draw-Line $g ($x + [int]($s * 0.50)) ($y + [int]($s * 0.50)) ($x + [int]($s * 1.15)) ($y - [int]($s * 0.10)) "#2F3437" 8
    Draw-Line $g ($x + [int]($s * 1.15)) ($y - [int]($s * 0.10)) ($x + [int]($s * 2.35)) ($y + [int]($s * 0.50)) "#2F3437" 8
    Draw-Line $g ($x + [int]($s * 0.50)) ($y + [int]($s * 0.50)) ($x + [int]($s * 2.35)) ($y + [int]($s * 0.50)) "#2F3437" 8
    Draw-Line $g ($x + [int]($s * 1.15)) ($y - [int]($s * 0.10)) ($x + [int]($s * 1.38)) ($y + [int]($s * 0.50)) "#2F3437" 8
    Draw-Line $g ($x + [int]($s * 2.35)) ($y + [int]($s * 0.50)) ($x + [int]($s * 2.62)) ($y - [int]($s * 0.18)) "#2F3437" 8
}

function Draw-Blocks($g, [int]$x, [int]$y) {
    $colors = @("#EF5350", "#42A5F5", "#FFCA28", "#66BB6A")
    for ($i = 0; $i -lt 4; $i++) {
        $bx = $x + (($i % 2) * 86)
        $by = $y + ([Math]::Floor($i / 2) * 76)
        Fill-Round $g $bx $by 72 64 12 $colors[$i]
        Draw-Round $g $bx $by 72 64 12 "#2F3437" 4
    }
}

function Draw-Cake($g, [int]$x, [int]$y) {
    Fill-Round $g $x ($y + 80) 230 90 18 "#F8BBD0"
    Draw-Round $g $x ($y + 80) 230 90 18 "#2F3437" 5
    Fill-Round $g ($x + 20) ($y + 35) 190 64 18 "#FFF3E0"
    Draw-Round $g ($x + 20) ($y + 35) 190 64 18 "#2F3437" 5
    Draw-Line $g ($x + 40) ($y + 20) ($x + 40) ($y + 50) "#FF7043" 8
    Draw-Line $g ($x + 112) ($y + 20) ($x + 112) ($y + 50) "#42A5F5" 8
    Draw-Line $g ($x + 184) ($y + 20) ($x + 184) ($y + 50) "#66BB6A" 8
}

function Draw-Bath($g, [int]$x, [int]$y) {
    Fill-Round $g $x ($y + 85) 300 120 34 "#FFFFFF"
    Draw-Round $g $x ($y + 85) 300 120 34 "#2F3437" 6
    Draw-Line $g ($x + 42) ($y + 84) ($x + 42) ($y + 40) "#78909C" 8
    Draw-Line $g ($x + 42) ($y + 40) ($x + 106) ($y + 40) "#78909C" 8
    Draw-Ellipse $g ($x + 94) ($y + 28) 34 34 "#90CAF9" "#2F3437" 4
    for ($i = 0; $i -lt 8; $i++) {
        Draw-Ellipse $g ($x + 70 + $i * 25) ($y + 62 + (($i % 2) * 12)) 26 26 "#BBDEFB" "#64B5F6" 3
    }
}

function Draw-Toothbrush($g, [int]$x, [int]$y) {
    Draw-Line $g $x $y ($x + 160) ($y - 46) "#29B6F6" 18
    Fill-Round $g ($x + 150) ($y - 72) 72 48 18 "#FFFFFF"
    Draw-Round $g ($x + 150) ($y - 72) 72 48 18 "#2F3437" 5
    Draw-Line $g ($x + 180) ($y - 68) ($x + 180) ($y - 110) "#FFFFFF" 6
    Draw-Line $g ($x + 198) ($y - 68) ($x + 198) ($y - 110) "#FFFFFF" 6
}

function Draw-Table($g, [int]$x, [int]$y, [int]$w) {
    Fill-Round $g $x $y $w 76 26 "#8D6E63"
    Draw-Round $g $x $y $w 76 26 "#2F3437" 5
    Draw-Line $g ($x + 60) ($y + 70) ($x + 40) ($y + 158) "#6D4C41" 12
    Draw-Line $g ($x + $w - 60) ($y + 70) ($x + $w - 40) ($y + 158) "#6D4C41" 12
    Draw-Ellipse $g ($x + [int]($w / 2) - 44) ($y + 8) 88 42 "#FFFFFF" "#2F3437" 4
}

function Draw-Bird($g, [int]$x, [int]$y, [double]$s = 1.0) {
    $sc = { param($n) [int][Math]::Round($n * $s) }
    Draw-Ellipse $g $x $y (& $sc 112) (& $sc 78) "#42A5F5" "#2F3437" (& $sc 5)
    Draw-Ellipse $g ($x + (& $sc 78)) ($y - (& $sc 42)) (& $sc 70) (& $sc 70) "#64B5F6" "#2F3437" (& $sc 5)
    $wing = New-Brush "#1E88E5"
    $outline = New-Pen "#2F3437" (& $sc 5)
    try {
        $g.FillPie($wing, $x + (& $sc 12), $y - (& $sc 42), (& $sc 112), (& $sc 96), 20, 140)
        $g.DrawArc($outline, $x + (& $sc 12), $y - (& $sc 42), (& $sc 112), (& $sc 96), 20, 140)
        $beak = New-Brush "#FFCA28"
        $g.FillPolygon($beak, @([Drawing.Point]::new($x + (& $sc 142), $y - (& $sc 8)), [Drawing.Point]::new($x + (& $sc 184), $y + (& $sc 8)), [Drawing.Point]::new($x + (& $sc 142), $y + (& $sc 24))))
        $beak.Dispose()
    } finally {
        $wing.Dispose()
        $outline.Dispose()
    }
    Draw-Ellipse $g ($x + (& $sc 110)) ($y - (& $sc 16)) (& $sc 11) (& $sc 15) "#2F3437" "#2F3437" 1
}

function Draw-Fish($g, [int]$x, [int]$y, [double]$s = 1.0) {
    $sc = { param($n) [int][Math]::Round($n * $s) }
    Draw-Ellipse $g $x $y (& $sc 154) (& $sc 90) "#FF7043" "#2F3437" (& $sc 5)
    $fin = New-Brush "#FFB74D"
    $outline = New-Pen "#2F3437" (& $sc 5)
    try {
        $g.FillPolygon($fin, @([Drawing.Point]::new($x - (& $sc 4), $y + (& $sc 44)), [Drawing.Point]::new($x - (& $sc 58), $y + (& $sc 6)), [Drawing.Point]::new($x - (& $sc 58), $y + (& $sc 82))))
        $g.DrawPolygon($outline, @([Drawing.Point]::new($x - (& $sc 4), $y + (& $sc 44)), [Drawing.Point]::new($x - (& $sc 58), $y + (& $sc 6)), [Drawing.Point]::new($x - (& $sc 58), $y + (& $sc 82))))
        $g.FillPolygon($fin, @([Drawing.Point]::new($x + (& $sc 62), $y + (& $sc 26)), [Drawing.Point]::new($x + (& $sc 92), $y - (& $sc 30)), [Drawing.Point]::new($x + (& $sc 102), $y + (& $sc 28))))
        $g.DrawPolygon($outline, @([Drawing.Point]::new($x + (& $sc 62), $y + (& $sc 26)), [Drawing.Point]::new($x + (& $sc 92), $y - (& $sc 30)), [Drawing.Point]::new($x + (& $sc 102), $y + (& $sc 28))))
    } finally {
        $fin.Dispose()
        $outline.Dispose()
    }
    Draw-Ellipse $g ($x + (& $sc 116)) ($y + (& $sc 30)) (& $sc 14) (& $sc 14) "#2F3437" "#2F3437" 1
}

function Draw-Shirt($g, [int]$x, [int]$y, [int]$s) {
    $brush = New-Brush "#42A5F5"
    $outline = New-Pen "#2F3437" 6
    try {
        $points = @(
            [Drawing.Point]::new($x + [int]($s * 0.25), $y),
            [Drawing.Point]::new($x + [int]($s * 0.75), $y),
            [Drawing.Point]::new($x + $s, $y + [int]($s * 0.20)),
            [Drawing.Point]::new($x + [int]($s * 0.82), $y + [int]($s * 0.45)),
            [Drawing.Point]::new($x + [int]($s * 0.72), $y + [int]($s * 0.36)),
            [Drawing.Point]::new($x + [int]($s * 0.72), $y + $s),
            [Drawing.Point]::new($x + [int]($s * 0.28), $y + $s),
            [Drawing.Point]::new($x + [int]($s * 0.28), $y + [int]($s * 0.36)),
            [Drawing.Point]::new($x + [int]($s * 0.18), $y + [int]($s * 0.45)),
            [Drawing.Point]::new($x, $y + [int]($s * 0.20))
        )
        $g.FillPolygon($brush, $points)
        $g.DrawPolygon($outline, $points)
    } finally {
        $brush.Dispose()
        $outline.Dispose()
    }
}

function Draw-WaterGlass($g, [int]$x, [int]$y) {
    Fill-Round $g $x $y 82 132 18 "#E3F2FD"
    Draw-Round $g $x $y 82 132 18 "#2F3437" 5
    Fill-Round $g ($x + 8) ($y + 50) 66 66 14 "#64B5F6" 180
    Draw-Line $g ($x + 14) ($y + 34) ($x + 68) ($y + 34) "#90CAF9" 5
}

function Draw-Newspaper($g, [int]$x, [int]$y) {
    Fill-Round $g $x $y 180 130 8 "#FFFFFF"
    Draw-Round $g $x $y 180 130 8 "#2F3437" 5
    Draw-Line $g ($x + 20) ($y + 30) ($x + 156) ($y + 30) "#2F3437" 5
    Draw-Line $g ($x + 20) ($y + 58) ($x + 156) ($y + 58) "#90A4AE" 4
    Draw-Line $g ($x + 20) ($y + 84) ($x + 156) ($y + 84) "#90A4AE" 4
    Draw-Line $g ($x + 90) ($y + 12) ($x + 90) ($y + 118) "#90A4AE" 4
}

function Draw-SceneImage($phrase) {
    $imagePath = Join-Path $imageDir "$($phrase.id).png"
    $bitmap = [Drawing.Bitmap]::new(768, 768)
    $graphics = [Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.CompositingQuality = [Drawing.Drawing2D.CompositingQuality]::HighQuality
    $graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::HighQuality

    try {
        $graphics.Clear([Drawing.Color]::White)

        switch ($phrase.scene) {
            "soccer" {
                Draw-SceneBackground $graphics "outdoor"; Draw-Sun $graphics 568 96 82; Draw-Cloud $graphics 120 122 0.8; Draw-Tree $graphics 108 386 0.8
                Draw-Person $graphics 332 608 1.42 "menino" "running" "#1E88E5"; Draw-SoccerBall $graphics 456 560 94
                Draw-Line $graphics 566 404 660 404 "#FFFFFF" 8; Draw-Line $graphics 566 404 566 494 "#FFFFFF" 8; Draw-Line $graphics 660 404 660 494 "#FFFFFF" 8
            }
            "apple" {
                Draw-SceneBackground $graphics "outdoor"; Draw-Tree $graphics 506 350 1.0; Draw-Person $graphics 344 606 1.36 "menina" "eating" "#EC407A"; Draw-Apple $graphics 475 284 72
            }
            "dog-run" {
                Draw-SceneBackground $graphics "outdoor"; Draw-Cloud $graphics 136 120 0.75; Draw-Tree $graphics 526 356 0.9; Draw-Dog $graphics 260 454 1.25 $false
                Draw-Line $graphics 180 554 112 530 "#2F3437" 5; Draw-Line $graphics 200 486 132 458 "#2F3437" 5
            }
            "cat-sofa" {
                Draw-SceneBackground $graphics "indoor"; Draw-Sofa $graphics 176 430 414 178; Draw-Cat $graphics 316 420 1.10
                Draw-Cloud $graphics 474 214 0.42
            }
            "reading" {
                Draw-SceneBackground $graphics "indoor"; Draw-Person $graphics 360 620 1.35 "menina" "reading" "#7E57C2"; Draw-Book $graphics 270 378 190 120
            }
            "washing-car" {
                Draw-SceneBackground $graphics "outdoor"; Draw-Car $graphics 330 448 260 154; Draw-Person $graphics 238 614 1.20 "pai" "standing" "#26A69A"
                for ($i = 0; $i -lt 7; $i++) { Draw-Ellipse $graphics (420 + $i * 24) (374 + (($i % 2) * 18)) 24 24 "#BBDEFB" "#64B5F6" 3 }
            }
            "cake" {
                Draw-SceneBackground $graphics "indoor"; Draw-Person $graphics 260 626 1.25 "mae" "standing" "#EF5350"; Draw-Table $graphics 332 520 260; Draw-Cake $graphics 348 334
            }
            "grandpa-bike" {
                Draw-SceneBackground $graphics "outdoor"; Draw-Sun $graphics 570 104 76; Draw-Bike $graphics 244 500 88; Draw-Person $graphics 382 514 1.05 "avo" "standing" "#8EACBB"
            }
            "baby-blocks" {
                Draw-SceneBackground $graphics "indoor"; Draw-Person $graphics 286 580 1.36 "bebe" "standing" "#FFCA28"; Draw-Blocks $graphics 438 456
            }
            "bath" {
                Draw-SceneBackground $graphics "indoor"; Draw-Bath $graphics 242 396; Draw-Person $graphics 398 514 0.92 "menino" "standing" "#4FC3F7"
            }
            "brush-teeth" {
                Draw-SceneBackground $graphics "indoor"; Draw-Person $graphics 340 616 1.38 "menina" "brush" "#EC407A"; Draw-Toothbrush $graphics 426 332
            }
            "family-dinner" {
                Draw-SceneBackground $graphics "indoor"; Draw-Person $graphics 218 498 0.84 "mae" "standing" "#EF5350"; Draw-Person $graphics 360 488 0.90 "pai" "standing" "#42A5F5"; Draw-Person $graphics 508 510 0.72 "menino" "standing" "#66BB6A"; Draw-Table $graphics 188 530 392
            }
            "bird-sky" {
                Draw-SceneBackground $graphics "outdoor"; Draw-Sun $graphics 574 102 78; Draw-Cloud $graphics 128 146 0.9; Draw-Cloud $graphics 432 226 0.65; Draw-Bird $graphics 286 304 1.32
            }
            "fish-tank" {
                Draw-SceneBackground $graphics "water"; Fill-Round $graphics 170 242 430 290 34 "#B3E5FC" 175; Draw-Round $graphics 170 242 430 290 34 "#2F3437" 6
                Draw-Fish $graphics 322 344 1.25
                for ($i = 0; $i -lt 7; $i++) { Draw-Ellipse $graphics (244 + $i * 44) (278 + (($i % 3) * 26)) 18 18 "#FFFFFF" "#81D4FA" 3 }
            }
            "sun" {
                Draw-SceneBackground $graphics "outdoor"; Draw-Sun $graphics 256 168 246; Draw-Cloud $graphics 126 460 0.72; Draw-Cloud $graphics 500 454 0.62
            }
            "jump-rope" {
                Draw-SceneBackground $graphics "outdoor"; Draw-Person $graphics 372 602 1.42 "menino" "jump" "#43A047"
                $rope = New-Pen "#8D6E63" 8
                try { $graphics.DrawArc($rope, 164, 232, 416, 420, 208, 124) } finally { $rope.Dispose() }
            }
            "shirt" {
                Draw-SceneBackground $graphics "indoor"; Draw-Person $graphics 300 624 1.28 "menino" "dressing" "#FFA726"; Draw-Shirt $graphics 430 330 156
            }
            "water" {
                Draw-SceneBackground $graphics "indoor"; Draw-Person $graphics 334 618 1.34 "menina" "drinking" "#AB47BC"; Draw-WaterGlass $graphics 464 316
                Draw-Ellipse $graphics 554 338 44 44 "#E1F5FE" "#81D4FA" 4
            }
            "dog-cat" {
                Draw-SceneBackground $graphics "outdoor"; Draw-Dog $graphics 154 466 1.04 $true; Draw-Cat $graphics 466 482 0.88
            }
            "newspaper" {
                Draw-SceneBackground $graphics "indoor"; Draw-Person $graphics 360 626 1.30 "pai" "reading" "#42A5F5"; Draw-Newspaper $graphics 268 374
            }
            default {
                Draw-SceneBackground $graphics "outdoor"; Draw-Person $graphics 360 620 1.30 "menino" "standing" "#42A5F5"
            }
        }
    } finally {
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

function Invoke-ElevenLabsTts($phrasesToGenerate) {
    if ([string]::IsNullOrWhiteSpace($env:ELEVENLABS_API_KEY) -or [string]::IsNullOrWhiteSpace($env:FONOLOUSA_ELEVENLABS_VOICE_ID)) {
        return $false
    }

    $headers = @{
        "xi-api-key" = $env:ELEVENLABS_API_KEY
        "Accept" = "audio/mpeg"
        "Content-Type" = "application/json"
    }
    foreach ($phrase in $phrasesToGenerate) {
        $out = Join-Path $audioDir "$($phrase.id).mp3"
        if ((Test-Path $out) -and ((Get-Item $out).Length -gt 8000)) { continue }
        $body = @{
            text = $phrase.palavra
            model_id = "eleven_multilingual_v2"
            voice_settings = @{
                stability = 0.50
                similarity_boost = 0.75
                style = 0.0
                use_speaker_boost = $true
            }
        } | ConvertTo-Json -Depth 4
        $url = "https://api.elevenlabs.io/v1/text-to-speech/$($env:FONOLOUSA_ELEVENLABS_VOICE_ID)"
        Invoke-WebRequest -Uri $url -Method Post -Headers $headers -Body $body -OutFile $out | Out-Null
    }
    return $true
}

if (-not (Invoke-ElevenLabsTts $phrases)) {
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
  const response = await tts.call(phrase.text);
  fs.writeFileSync(phrase.output, response.data);
}
'@

    $env:FONO_PHRASE_TTS_PAYLOAD = $ttsPayload
    node --input-type=module -e $nodeCode
    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao gerar audios neurais das frases."
    }
}

Write-Host "Modulo Frases atualizado: $($phrases.Count) imagens e MP3s em assets."
