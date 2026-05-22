$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$assetsRoot = Join-Path $root "app/src/main/assets"
$databasePath = Join-Path $assetsRoot "database.json"
$outputDir = Join-Path $root "output"
$sourceReportPath = Join-Path $outputDir "animal-audio-sources.csv"

New-Item -ItemType Directory -Force $outputDir | Out-Null

$sources = [ordered]@{
    "vaca" = @{
        Url = "https://dailysounds.org/api/play/84"
        Title = "Cow Moo"
        Provider = "DailySounds"
        License = "Royalty-free for apps and commercial/personal projects; attribution appreciated but not required."
    }
    "boi" = @{
        Url = "https://dailysounds.org/api/play/84"
        Title = "Cow Moo"
        Provider = "DailySounds"
        License = "Royalty-free for apps and commercial/personal projects; attribution appreciated but not required."
    }
    "gato" = @{
        Url = "https://dailysounds.org/api/play/37"
        Title = "Cat Meow"
        Provider = "DailySounds"
        License = "Royalty-free for apps and commercial/personal projects; attribution appreciated but not required."
    }
    "pato" = @{
        Url = "https://dailysounds.org/api/play/82"
        Title = "Duck Quack"
        Provider = "DailySounds"
        License = "Royalty-free for apps and commercial/personal projects; attribution appreciated but not required."
    }
    "sapo" = @{
        Url = "https://dailysounds.org/api/play/344"
        Title = "Frog Croaking"
        Provider = "DailySounds"
        License = "Royalty-free for apps and commercial/personal projects; attribution appreciated but not required."
    }
    "cobra" = @{
        Url = "https://dailysounds.org/api/play/483"
        Title = "Snake Hiss"
        Provider = "DailySounds"
        License = "Royalty-free for apps and commercial/personal projects; attribution appreciated but not required."
    }
    "pinto" = @{
        Url = "https://dailysounds.org/api/play/478"
        Title = "Rooster Crow"
        Provider = "DailySounds"
        License = "Royalty-free for apps and commercial/personal projects; attribution appreciated but not required."
    }
    "pintinho" = @{
        Url = "https://dailysounds.org/api/play/478"
        Title = "Rooster Crow"
        Provider = "DailySounds"
        License = "Royalty-free for apps and commercial/personal projects; attribution appreciated but not required."
    }
    "bode" = @{
        Url = "https://dailysounds.org/api/play/501"
        Title = "Goat Bleat"
        Provider = "DailySounds"
        License = "Royalty-free for apps and commercial/personal projects; attribution appreciated but not required."
    }
    "cavalo" = @{
        Url = "https://dailysounds.org/api/play/341"
        Title = "Horse Whinny"
        Provider = "DailySounds"
        License = "Royalty-free for apps and commercial/personal projects; attribution appreciated but not required."
    }
    "galinha" = @{
        Url = "https://dailysounds.org/api/play/39"
        Title = "Rooster Crow"
        Provider = "DailySounds"
        License = "Royalty-free for apps and commercial/personal projects; attribution appreciated but not required."
    }
    "macaco" = @{
        Url = "https://dailysounds.org/api/play/490"
        Title = "Monkey Chatter"
        Provider = "DailySounds"
        License = "Royalty-free for apps and commercial/personal projects; attribution appreciated but not required."
    }
    "ovelha" = @{
        Url = "https://dailysounds.org/api/play/502"
        Title = "Lamb Baa"
        Provider = "DailySounds"
        License = "Royalty-free for apps and commercial/personal projects; attribution appreciated but not required."
    }
    "cachorro" = @{
        Url = "https://dailysounds.org/api/play/36"
        Title = "Dog Bark"
        Provider = "DailySounds"
        License = "Royalty-free for apps and commercial/personal projects; attribution appreciated but not required."
    }
    "porco" = @{
        Url = "https://dailysounds.org/api/play/83"
        Title = "Pig Oink"
        Provider = "DailySounds"
        License = "Royalty-free for apps and commercial/personal projects; attribution appreciated but not required."
    }
    "abelha" = @{
        Url = "https://dailysounds.org/api/play/43"
        Title = "Bee Buzzing"
        Provider = "DailySounds"
        License = "Royalty-free for apps and commercial/personal projects; attribution appreciated but not required."
    }
    "elefante" = @{
        Url = "https://dailysounds.org/api/play/481"
        Title = "Elephant Trumpet"
        Provider = "DailySounds"
        License = "Royalty-free for apps and commercial/personal projects; attribution appreciated but not required."
    }
    "passarinho" = @{
        Url = "https://assets.mixkit.co/active_storage/sfx/23/23-preview.mp3"
        Title = "Little bird calling chirp"
        Provider = "Mixkit"
        License = "Free Mixkit sound effect for commercial and non-commercial projects under the Mixkit License."
    }
    "dinossauro" = @{
        Url = "https://assets.mixkit.co/active_storage/sfx/309/309-preview.mp3"
        Title = "Angry dragon growl"
        Provider = "Mixkit"
        License = "Free Mixkit sound effect for commercial and non-commercial projects under the Mixkit License."
    }
}

function Get-MatchingSource($item) {
    $source = " $($item.id.ToLowerInvariant()) $($item.palavra.ToLowerInvariant()) $($item.frase.ToLowerInvariant()) "
    foreach ($entry in $sources.GetEnumerator()) {
        $token = $entry.Key
        if ($source -match "(^|[\s-])$([Regex]::Escape($token))($|[\s-])") {
            return @{
                Token = $token
                Source = $entry.Value
            }
        }
    }
    return $null
}

function Save-Mp3($url, $path) {
    New-Item -ItemType Directory -Force (Split-Path -Parent $path) | Out-Null
    Invoke-WebRequest -Uri $url -UseBasicParsing -OutFile $path
    $file = Get-Item $path
    if ($file.Length -lt 1000) {
        throw "Downloaded audio is too small: $path"
    }
}

$db = Get-Content $databasePath -Raw | ConvertFrom-Json
$rows = @()
$downloadedByUrl = @{}

$animals = $db.categorias | Where-Object { $_.id -eq "animais" }
foreach ($level in $animals.niveis) {
    foreach ($item in $level.itens) {
        $match = Get-MatchingSource $item
        if (-not $match) {
            continue
        }

        $relativeSoundPath = $item.arquivoSom.Remove(0, "assets/".Length)
        $fullSoundPath = Join-Path $assetsRoot $relativeSoundPath
        $url = $match.Source.Url

        if (-not $downloadedByUrl.ContainsKey($url)) {
            $cachePath = Join-Path $outputDir ("audio-cache-" + ([Math]::Abs($url.GetHashCode())) + ".mp3")
            Save-Mp3 $url $cachePath
            $downloadedByUrl[$url] = $cachePath
        }

        New-Item -ItemType Directory -Force (Split-Path -Parent $fullSoundPath) | Out-Null
        Copy-Item $downloadedByUrl[$url] $fullSoundPath -Force

        $rows += [pscustomobject]@{
            itemId = $item.id
            palavra = $item.palavra
            frase = $item.frase
            arquivoSom = $item.arquivoSom
            token = $match.Token
            provider = $match.Source.Provider
            title = $match.Source.Title
            url = $url
            license = $match.Source.License
        }
    }
}

$rows | Export-Csv -NoTypeInformation -Encoding UTF8 $sourceReportPath

Write-Host "Animal sounds downloaded: $($rows.Count)"
Write-Host "Unique source files:       $($downloadedByUrl.Count)"
Write-Host "Source report:             $sourceReportPath"
