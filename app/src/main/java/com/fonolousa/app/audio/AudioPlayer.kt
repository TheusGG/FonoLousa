package com.fonolousa.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import java.util.Locale

class AudioPlayer(context: Context) {
    private val assets = context.applicationContext.assets
    private var ttsReady = false
    private val pendingSpeech = mutableListOf<String>()
    private var textToSpeech: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null

    init {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                textToSpeech?.language = Locale.forLanguageTag("pt-BR")
                textToSpeech?.setSpeechRate(0.85f)
                pendingSpeech.toList().forEach(::speak)
                pendingSpeech.clear()
            }
        }
    }

    fun play(assetPath: String, fallbackText: String) {
        val normalizedPath = assetPath.removePrefix("assets/")

        try {
            stopMedia()
            assets.openFd(normalizedPath).use { descriptor ->
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                    setOnPreparedListener { player -> player.start() }
                    setOnCompletionListener { player ->
                        player.release()
                        if (mediaPlayer == player) {
                            mediaPlayer = null
                        }
                    }
                    setOnErrorListener { player, _, _ ->
                        player.release()
                        if (mediaPlayer == player) {
                            mediaPlayer = null
                        }
                        speak(fallbackText)
                        true
                    }
                    prepareAsync()
                }
            }
        } catch (_: Exception) {
            speak(fallbackText)
        }
    }

    fun playVictory() {
        speak("Muito bem!")
    }

    private fun speak(text: String) {
        if (text.isBlank()) return
        if (!ttsReady) {
            pendingSpeech.add(text)
            return
        }
        textToSpeech?.speak(text.lowercase(Locale.forLanguageTag("pt-BR")), TextToSpeech.QUEUE_FLUSH, null, "fono-${System.nanoTime()}")
    }

    fun release() {
        stopMedia()
        textToSpeech?.shutdown()
        textToSpeech = null
        pendingSpeech.clear()
    }

    private fun stopMedia() {
        mediaPlayer?.let { player ->
            runCatching {
                if (player.isPlaying) {
                    player.stop()
                }
            }
            player.release()
        }
        mediaPlayer = null
    }
}
