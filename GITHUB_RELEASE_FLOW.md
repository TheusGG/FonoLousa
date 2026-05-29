# Fluxo de Atualizacao via GitHub

Este projeto publica o canal de atualizacao pela pasta `docs/` do branch `main`.
O APK, a pagina de download e o manifesto precisam ser atualizados no mesmo commit.

## Configuracao unica

1. Suba este projeto para `https://github.com/TheusGG/FonoLousa`.
2. No GitHub, abra `Settings > Pages`.
3. Em `Build and deployment`, escolha:
   - Source: `Deploy from a branch`
   - Branch: `main`
   - Folder: `/docs`
4. Salve.

Depois disso, os canais publicos serao:

```text
https://theusgg.github.io/FonoLousa/
https://raw.githubusercontent.com/TheusGG/FonoLousa/main/docs/fonolousa-update.json
```

## Gerar pacote para uma versao

Execute o build de release apontando para o manifesto publico do `main`:

```powershell
powershell -ExecutionPolicy Bypass -File .\build-apk.ps1 -ManifestUrl "https://raw.githubusercontent.com/TheusGG/FonoLousa/main/docs/fonolousa-update.json" -ApkUrl "https://github.com/TheusGG/FonoLousa/raw/main/docs/FonoLousa-release.apk" -BuildType Release -ApkFileName FonoLousa-release.apk
```

Depois copie `output/FonoLousa-release.apk`, `output/fonolousa-update.json` e
`output/index.html` para `docs/`, e atualize `docs/FonoLousa-release.apk.sha256`.

Isso gera:

```text
output/FonoLousa-release.apk
docs/FonoLousa-release.apk
docs/index.html
docs/fonolousa-update.json
```

## Testar sem baixar APK manualmente

Com o emulador ou tablet conectado no ADB:

```powershell
powershell -ExecutionPolicy Bypass -File test-dev.ps1
```

Para LDPlayer, se ele nao aparecer em `adb devices`, tente:

```powershell
powershell -ExecutionPolicy Bypass -File test-dev.ps1 -Connect 127.0.0.1:5555
```

Esse fluxo compila, instala por cima e abre o app direto no emulador/tablet.

## Publicar

1. Commit e push dos arquivos do projeto e da pasta `docs/`.
2. Instale o APK no tablet usando:

```text
https://theusgg.github.io/FonoLousa/
```

## Atualizar o app depois

1. Aumente `versionCode` e `versionName` em `app/build.gradle.kts`.
2. Rode `build-apk.ps1` em modo Release.
3. Copie os artefatos para `docs/`.
4. Faca commit/push do APK, manifesto, pagina e SHA.
5. No tablet, abra o app e toque em atualizar.
