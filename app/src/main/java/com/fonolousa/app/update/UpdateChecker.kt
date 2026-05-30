package com.fonolousa.app.update

import com.fonolousa.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
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
    data class RemoteBehind(val currentVersionName: String, val remoteVersionName: String) : UpdateState
    data class NotConfigured(val message: String) : UpdateState
    data class Error(val message: String) : UpdateState
}

class UpdateChecker(
    private val manifestUrl: String = BuildConfig.UPDATE_MANIFEST_URL.ifBlank {
        DEFAULT_MANIFEST_URL
    }
) {
    suspend fun check(): UpdateState {
        if (manifestUrl.isBlank()) {
            return UpdateState.NotConfigured(
                "Canal de atualização ainda não configurado neste APK."
            )
        }
        if (!isTrustedUpdateUrl(manifestUrl)) {
            return UpdateState.NotConfigured(
                "Canal de atualização precisa usar HTTPS oficial do GitHub."
            )
        }

        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                connection = URL(manifestUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = REQUEST_TIMEOUT_MS
                connection.readTimeout = REQUEST_TIMEOUT_MS
                connection.requestMethod = "GET"
                if (connection.responseCode !in 200..299) {
                    return@withContext UpdateState.Error("Manifesto indisponível: HTTP ${connection.responseCode}.")
                }
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                    val manifest = parseManifest(reader.readText())
                    when {
                        manifest.app != APP_NAME -> UpdateState.Error("Manifesto de outro app: ${manifest.app}.")
                        !isTrustedUpdateUrl(manifest.apkUrl) -> UpdateState.Error("Link de APK fora do canal oficial.")
                        manifest.versionCode > BuildConfig.VERSION_CODE -> UpdateState.Available(manifest)
                        manifest.versionCode < BuildConfig.VERSION_CODE -> {
                            UpdateState.RemoteBehind(BuildConfig.VERSION_NAME, manifest.versionName)
                        }
                        else -> UpdateState.UpToDate(BuildConfig.VERSION_NAME)
                    }
                }
            } catch (error: Exception) {
                UpdateState.Error(error.message ?: "Não foi possível ler o manifesto de atualização.")
            } finally {
                connection?.disconnect()
            }
        }
    }

    companion object {
        private const val APP_NAME = "FonoLousa"
        private const val REQUEST_TIMEOUT_MS = 8000
        private const val DEFAULT_MANIFEST_URL =
            "https://raw.githubusercontent.com/TheusGG/FonoLousa/main/docs/fonolousa-update.json"
    }
}

internal fun isTrustedUpdateUrl(rawUrl: String): Boolean {
    return try {
        val uri = URI(rawUrl.trim())
        val scheme = uri.scheme?.lowercase()
        val host = uri.host?.lowercase()
        val path = uri.path.orEmpty().lowercase()

        scheme == "https" && when (host) {
            "raw.githubusercontent.com" -> path.startsWith("/theusgg/fonolousa/")
            "github.com" -> path.startsWith("/theusgg/fonolousa/")
            "theusgg.github.io" -> path.startsWith("/fonolousa/")
            else -> false
        }
    } catch (_: Exception) {
        false
    }
}

private fun parseManifest(json: String): UpdateManifest {
    val data = JSONObject(json)
    return UpdateManifest(
        app = data.getString("app"),
        versionCode = data.getInt("versionCode"),
        versionName = data.getString("versionName"),
        apkUrl = data.getString("apkUrl"),
        notes = data.optString("notes")
    )
}
