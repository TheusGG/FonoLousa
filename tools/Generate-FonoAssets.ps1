$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$assetsRoot = Join-Path $root "app/src/main/assets"

function New-Dir($path) {
    New-Item -ItemType Directory -Force $path | Out-Null
}

function Get-Article([string]$word) {
    $lower = $word.ToLowerInvariant()
    $exceptions = @{
        "mao" = "a"
        "mae" = "a"
        "agua" = "a"
        "unha" = "a"
    }
    if ($exceptions.ContainsKey($lower)) {
        return $exceptions[$lower]
    }
    if ($lower.EndsWith("a") -or $lower.EndsWith("ã")) {
        return "a"
    }
    return "o"
}

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

function Get-Id([string]$name) {
    $id = (Remove-Accent $name).ToLowerInvariant().Trim()
    $id = $id -replace "\s+", "-"
    $id = $id -replace "[^a-z0-9-]", ""
    $id = $id -replace "-+", "-"
    return $id.Trim("-")
}

function To-Title([string]$text) {
    if ([string]::IsNullOrWhiteSpace($text)) {
        return $text
    }
    return $text.Substring(0, 1).ToUpperInvariant() + $text.Substring(1)
}

$categories = @(
    @{
        id = "animais"; nome = "Animais"; cor = "#4CAF50"; niveis = @(
            @{ nivel = 1; descricao = "Palavras curtas"; itens = @("vaca","gato","pato","sapo","boi","rato","peixe","cobra","pinto","foca","urso","bode") },
            @{ nivel = 2; descricao = "Palavras medias"; itens = @("cavalo","galinha","macaco","coelho","ovelha","tartaruga","cachorro","porco","pintinho","abelha","girafa","camelo") },
            @{ nivel = 3; descricao = "Palavras longas"; itens = @("elefante","borboleta","crocodilo","passarinho","tucano","joaninha","caranguejo","hipopotamo","lagartixa","dinossauro") },
            @{ nivel = 4; descricao = "Frases"; itens = @("o gato","a vaca","o cavalo","a galinha","o elefante","o pato amarelo","o sapo verde","a borboleta azul","o cachorro grande","o passarinho pequeno") }
        )
    },
    @{
        id = "alimentos"; nome = "Alimentos"; cor = "#F44336"; niveis = @(
            @{ nivel = 1; descricao = "Palavras curtas"; itens = @("leite","arroz","ovo","bolo","sopa","suco","cafe","uva","bife","queijo","pera","doce") },
            @{ nivel = 2; descricao = "Palavras medias"; itens = @("banana","frango","sorvete","cenoura","pipoca","biscoito","iogurte","presunto","lasanha","panqueca","farofa","sanduiche") },
            @{ nivel = 3; descricao = "Palavras longas"; itens = @("chocolate","macarrao","gelatina","torresmo","brigadeiro","cocada","canjica","pamonha","pudim","goiabada") },
            @{ nivel = 4; descricao = "Frases"; itens = @("o leite","a banana","o bolo","a sopa","o sorvete","a fruta","o suco gelado","a pipoca","o sanduiche","a cenoura") }
        )
    },
    @{
        id = "casa-objetos"; nome = "Casa e Objetos"; cor = "#2196F3"; niveis = @(
            @{ nivel = 1; descricao = "Palavras curtas"; itens = @("cama","mesa","porta","sofa","livro","lapis","chave","copo","prato","sino","jarro","vela") },
            @{ nivel = 2; descricao = "Palavras medias"; itens = @("cadeira","janela","fogao","relogio","lampada","telefone","tesoura","cobertor","panela","colher","lencol","travesseiro") },
            @{ nivel = 3; descricao = "Palavras longas"; itens = @("geladeira","televisao","chuveiro","computador","ventilador","aspirador","liquidificador","cafeteira","espremedor","micro-ondas") },
            @{ nivel = 4; descricao = "Frases"; itens = @("a mesa","a cama","a porta","a cadeira","o sofa","a janela","o livro","o telefone","a lampada","a televisao") }
        )
    },
    @{
        id = "veiculos"; nome = "Veiculos"; cor = "#FF9800"; niveis = @(
            @{ nivel = 1; descricao = "Palavras curtas"; itens = @("carro","moto","trem","barco","van","taxi","jato","nave","metro","trator","bonde","fusca") },
            @{ nivel = 2; descricao = "Palavras medias"; itens = @("bicicleta","patinete","caminhao","furgao","skate","velotrol","charrete","jangada","canoa","teleferico","jetski","kart") },
            @{ nivel = 3; descricao = "Palavras longas"; itens = @("helicoptero","ambulancia","escavadeira","guindaste","carreta","caminhonete","motocicleta","submarino","aeronave","foguete") },
            @{ nivel = 4; descricao = "Frases"; itens = @("o carro","a moto","o trem","o barco","a bicicleta","o caminhao","a ambulancia","o helicoptero","o trator","o foguete") }
        )
    },
    @{
        id = "corpo-roupas"; nome = "Corpo e Roupas"; cor = "#E91E63"; niveis = @(
            @{ nivel = 1; descricao = "Palavras curtas"; itens = @("olho","boca","nariz","mao","pe","dedo","braco","unha","calca","saia","meia","dente") },
            @{ nivel = 2; descricao = "Palavras medias"; itens = @("cabelo","cabeca","sapato","camisa","vestido","chapeu","ombro","cotovelo","joelho","gravata","blusa","bermuda") },
            @{ nivel = 3; descricao = "Palavras longas"; itens = @("oculos","pijama","macacao","jardineira","sobretudo","moletom","agasalho","uniforme","sandalias","chinelo") },
            @{ nivel = 4; descricao = "Frases"; itens = @("o olho","a boca","a mao","o pe","o sapato","a camisa","o chapeu","o vestido","a calca","a saia") }
        )
    },
    @{
        id = "natureza"; nome = "Natureza"; cor = "#FFEB3B"; niveis = @(
            @{ nivel = 1; descricao = "Palavras curtas"; itens = @("sol","lua","mar","rio","flor","chuva","vento","fogo","agua","pedra","ceu","monte") },
            @{ nivel = 2; descricao = "Palavras medias"; itens = @("arvore","estrela","nuvem","montanha","jardim","floresta","vulcao","semente","galho","espinho","cascata","nascente") },
            @{ nivel = 3; descricao = "Palavras longas"; itens = @("arco-iris","cachoeira","tempestade","deserto","por-do-sol","vegetacao","planicie","penhasco","oceano","lagoa") },
            @{ nivel = 4; descricao = "Frases"; itens = @("o sol","a lua","a flor","a chuva","a arvore","a estrela","a montanha","a cachoeira","a floresta","o arco-iris") }
        )
    },
    @{
        id = "brinquedos"; nome = "Brinquedos"; cor = "#9C27B0"; niveis = @(
            @{ nivel = 1; descricao = "Palavras curtas"; itens = @("bola","piao","dado","peteca","pipa","corda","bloco","patins","balao","ioio","sino","apito") },
            @{ nivel = 2; descricao = "Palavras medias"; itens = @("boneca","carrinho","fantoche","ursinho","domino","tambor","balanco","gangorra","cavalinho","escorrega","gira-gira","quebra-cabeca") },
            @{ nivel = 3; descricao = "Palavras longas"; itens = @("escorregador","velocipede","videogame","patinete","aeromodelo","trenzinho","bonecao","pula-pula","bolinha-de-gude","barco-controle") },
            @{ nivel = 4; descricao = "Frases"; itens = @("a bola","a boneca","o carrinho","o ursinho","a pipa","a peteca","o tambor","o domino","o balao","o fantoche") }
        )
    },
    @{
        id = "familia-pessoas"; nome = "Familia e Pessoas"; cor = "#795548"; niveis = @(
            @{ nivel = 1; descricao = "Palavras curtas"; itens = @("mae","pai","vo","vo","tia","tio","bebe","avo","avo","primo","filho","neto") },
            @{ nivel = 2; descricao = "Palavras medias"; itens = @("irmao","irma","amigo","vizinho","medico","bombeiro","pintor","carteiro","dentista","pedreiro","motorista","professor") },
            @{ nivel = 3; descricao = "Palavras longas"; itens = @("veterinario","cabeleireiro","advogado","engenheiro","arquiteto","fisioterapeuta","nutricionista","farmaceutico","jornalista","astronauta") },
            @{ nivel = 4; descricao = "Frases"; itens = @("a mae","o pai","o bebe","o irmao","o amigo","o medico","o bombeiro","o professor","a professora","o veterinario") }
        )
    }
)

$database = [ordered]@{
    app = "FonoLousa"
    versao = "1.0.0"
    categorias = @()
}

foreach ($category in $categories) {
    $categoryJson = [ordered]@{
        id = $category.id
        nome = $category.nome
        cor = $category.cor
        niveis = @()
    }

    New-Dir (Join-Path $assetsRoot "imagens/$($category.id)")
    New-Dir (Join-Path $assetsRoot "sons/$($category.id)")

    foreach ($level in $category.niveis) {
        $levelJson = [ordered]@{
            nivel = $level.nivel
            descricao = $level.descricao
            itens = @()
        }
        $seenIds = @{}

        foreach ($rawItem in $level.itens) {
            $words = $rawItem.Split(" ", [StringSplitOptions]::RemoveEmptyEntries)
            $baseWord = $words[$words.Length - 1]
            $displayWord = To-Title $baseWord
            $phrase = $rawItem
            if ($level.nivel -ne 4) {
                $phrase = "$(Get-Article $rawItem) $rawItem"
                $displayWord = To-Title $rawItem
            }

            $baseId = Get-Id $baseWord
            $itemId = $baseId
            if ($seenIds.ContainsKey($baseId)) {
                $seenIds[$baseId] += 1
                $itemId = "$baseId-$($seenIds[$baseId])"
            } else {
                $seenIds[$baseId] = 1
            }

            $prefix = "n$($level.nivel)"
            $imagePath = "assets/imagens/$($category.id)/${prefix}_${itemId}.png"
            $soundPath = "assets/sons/$($category.id)/${prefix}_${itemId}.mp3"

            $levelJson.itens += [ordered]@{
                id = $itemId
                palavra = $displayWord
                arquivoImagem = $imagePath
                arquivoSom = $soundPath
                frase = $phrase
                promptImagem = "Ilustracao infantil colorida em estilo cartoon, tracos simples e amigaveis, de $displayWord, fundo branco, sem texto, estilo livro infantil"
            }
        }
        $categoryJson.niveis += $levelJson
    }
    $database.categorias += $categoryJson
}

New-Dir $assetsRoot
$json = $database | ConvertTo-Json -Depth 10
[IO.File]::WriteAllText((Join-Path $assetsRoot "database.json"), $json, [Text.UTF8Encoding]::new($false))

Add-Type -AssemblyName System.Drawing

foreach ($category in $database.categorias) {
    $categoryColor = [Drawing.ColorTranslator]::FromHtml($category.cor)
    foreach ($level in $category.niveis) {
        foreach ($item in $level.itens) {
            $relativeImagePath = $item.arquivoImagem.Remove(0, "assets/".Length)
            $fullImagePath = Join-Path $assetsRoot $relativeImagePath
            New-Dir (Split-Path -Parent $fullImagePath)

            $bitmap = [Drawing.Bitmap]::new(512, 512)
            $graphics = [Drawing.Graphics]::FromImage($bitmap)
            $graphics.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
            $graphics.Clear([Drawing.Color]::White)

            $brush = [Drawing.SolidBrush]::new($categoryColor)
            $lightBrush = [Drawing.SolidBrush]::new([Drawing.Color]::FromArgb(28, $categoryColor))
            $pen = [Drawing.Pen]::new($categoryColor, 12)
            $graphics.FillEllipse($lightBrush, 36, 36, 440, 440)
            $graphics.DrawEllipse($pen, 42, 42, 428, 428)

            $fontSize = if ($item.palavra.Length -gt 12) { 42 } elseif ($item.palavra.Length -gt 8) { 50 } else { 62 }
            $font = [Drawing.Font]::new("Arial", $fontSize, [Drawing.FontStyle]::Bold, [Drawing.GraphicsUnit]::Pixel)
            $format = [Drawing.StringFormat]::new()
            $format.Alignment = [Drawing.StringAlignment]::Center
            $format.LineAlignment = [Drawing.StringAlignment]::Center
            $format.Trimming = [Drawing.StringTrimming]::EllipsisWord
            $rect = [Drawing.RectangleF]::new(54, 126, 404, 220)
            $graphics.DrawString($item.palavra, $font, $brush, $rect, $format)

            $smallFont = [Drawing.Font]::new("Arial", 30, [Drawing.FontStyle]::Bold, [Drawing.GraphicsUnit]::Pixel)
            $graphics.DrawString("Nivel $($level.nivel)", $smallFont, $brush, [Drawing.RectangleF]::new(54, 360, 404, 60), $format)

            $bitmap.Save($fullImagePath, [Drawing.Imaging.ImageFormat]::Png)
            $graphics.Dispose()
            $bitmap.Dispose()
            $brush.Dispose()
            $lightBrush.Dispose()
            $pen.Dispose()
            $font.Dispose()
            $smallFont.Dispose()
            $format.Dispose()
        }
    }
}

$itemCount = ($database.categorias | ForEach-Object { $_.niveis } | ForEach-Object { $_.itens } | Measure-Object).Count
Write-Host "Generated database.json and $itemCount placeholder PNG files."
