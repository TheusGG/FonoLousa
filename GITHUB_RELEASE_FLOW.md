# Fluxo de Atualizacao via GitHub

Este projeto usa dois recursos do GitHub:

- **GitHub Releases** para hospedar o APK.
- **GitHub Pages** para hospedar `index.html` e `fonolousa-update.json`.

## Configuracao unica

1. Suba este projeto para `https://github.com/TheusGG/FonoLousa`.
2. No GitHub, abra `Settings > Pages`.
3. Em `Build and deployment`, escolha:
   - Source: `Deploy from a branch`
   - Branch: `main`
   - Folder: `/docs`
4. Salve.

Depois disso, o canal web sera:

```text
https://theusgg.github.io/FonoLousa/
https://theusgg.github.io/FonoLousa/fonolousa-update.json
```

## Gerar pacote para uma versao

Execute:

```powershell
powershell -ExecutionPolicy Bypass -File build-github-release.ps1
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

Isso gera:

```text
output/FonoLousa-debug.apk
docs/index.html
docs/fonolousa-update.json
```

## Publicar

1. Commit e push dos arquivos do projeto e da pasta `docs/`.
2. Crie uma Release no GitHub com a tag `v1.0.0`.
3. Anexe `output/FonoLousa-debug.apk` na Release.
4. Instale o APK no tablet usando:

```text
https://theusgg.github.io/FonoLousa/
```

## Atualizar o app depois

1. Aumente `versionCode` e `versionName` em `app/build.gradle.kts`.
2. Rode `build-github-release.ps1` com a nova versao.
3. Publique nova Release com tag correspondente, por exemplo `v1.0.1`.
4. Faça commit/push do novo `docs/fonolousa-update.json`.
5. No tablet, abra o app e toque em atualizar.
