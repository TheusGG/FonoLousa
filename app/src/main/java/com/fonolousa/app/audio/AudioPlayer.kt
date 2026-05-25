package com.fonolousa.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import java.util.Locale

class AudioPlayer(context: Context) {
    private val assets = context.applicationContext.assets
    private val loaded = mutableMapOf<String, Int>()
    private val loading = mutableSetOf<String>()
    private val pendingPlay = mutableSetOf<Int>()
    private var ttsReady = false
    private val pendingSpeech = mutableListOf<String>()
    private var textToSpeech: TextToSpeech? = null

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        .build()

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
        soundPool.setOnLoadCompleteListener { pool, soundId, status ->
            if (status == 0 && pendingPlay.remove(soundId)) {
                pool.play(soundId, 1f, 1f, 1, 0, 1f)
            }
        }
    }

    fun play(assetPath: String, fallbackText: String) {
        val normalizedPath = assetPath.removePrefix("assets/")
        val soundId = loaded[normalizedPath]
        if (soundId != null) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
            return
        }
        if (loading.contains(normalizedPath)) return

        try {
            val descriptor = assets.openFd(normalizedPath)
            val newSoundId = soundPool.load(descriptor, 1)
            descriptor.close()
            loaded[normalizedPath] = newSoundId
            loading.add(normalizedPath)
            pendingPlay.add(newSoundId)
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
        soundPool.release()
        textToSpeech?.shutdown()
        textToSpeech = null
        loaded.clear()
        loading.clear()
        pendingPlay.clear()
        pendingSpeech.clear()
    }
}
