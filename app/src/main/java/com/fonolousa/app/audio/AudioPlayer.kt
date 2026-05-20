package com.fonolousa.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class AudioPlayer(context: Context) {
    private val assets = context.applicationContext.assets
    private val loaded = mutableMapOf<String, Int>()
    private val loading = mutableSetOf<String>()
    private val pendingPlay = mutableSetOf<Int>()

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
        soundPool.setOnLoadCompleteListener { pool, soundId, status ->
            if (status == 0 && pendingPlay.remove(soundId)) {
                pool.play(soundId, 1f, 1f, 1, 0, 1f)
            }
        }
    }

    fun play(assetPath: String) {
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
            // Missing MP3 assets are expected in the placeholder build.
        }
    }

    fun release() {
        soundPool.release()
        loaded.clear()
        loading.clear()
        pendingPlay.clear()
    }
}
