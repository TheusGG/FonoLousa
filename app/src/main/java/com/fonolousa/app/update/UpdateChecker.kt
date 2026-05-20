package com.fonolousa.app.update

import com.fonolousa.app.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

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
    private val manifestUrl: String = BuildConfig.UPDATE_MANIFEST_URL
) {
    private val gson = Gson()

    suspend fun check(): UpdateState = withContext(Dispatchers.IO) {
        if (manifestUrl.isBlank()) {
            return@withContext UpdateState.NotConfigured(
                "Canal de atualizacao ainda nao configurado neste APK."
            )
        }

        try {
            val connection = (URL(manifestUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
            }

            connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val manifest = gson.fromJson(reader, UpdateManifest::class.java)
                if (manifest.versionCode > BuildConfig.VERSION_CODE) {
                    UpdateState.Available(manifest)
                } else {
                    UpdateState.UpToDate(BuildConfig.VERSION_NAME)
                }
            }
        } catch (error: Exception) {
            UpdateState.Error(error.message ?: "Nao foi possivel verificar atualizacao.")
        }
    }
}
