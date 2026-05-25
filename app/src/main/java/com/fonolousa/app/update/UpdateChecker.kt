package com.fonolousa.app.update

import com.fonolousa.app.BuildConfig

data class UpdateManifest(
    val app: String,
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val notes: String = ""
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class Available(val manifest: UpdateManifest) : UpdateState
    data class UpToDate(val currentVersionName: String) : UpdateState
    data class NotConfigured(val message: String) : UpdateState
    data class Error(val message: String) : UpdateState
}

class UpdateChecker(
    private val manifestUrl: String = BuildConfig.UPDATE_MANIFEST_URL.ifBlank {
        "https://github.com/TheusGG/FonoLousa/tree/main/docs"
    }
) {
    suspend fun check(): UpdateState {
        if (manifestUrl.isBlank()) {
            return UpdateState.NotConfigured(
                "Canal de atualizacao ainda nao configurado neste APK."
            )
        }

        return UpdateState.Available(
            UpdateManifest(
                app = "FonoLousa",
                versionCode = BuildConfig.VERSION_CODE,
                versionName = BuildConfig.VERSION_NAME,
                apkUrl = manifestUrl,
                notes = "Abra a pagina oficial para baixar a versao publicada."
            )
        )
    }
}
