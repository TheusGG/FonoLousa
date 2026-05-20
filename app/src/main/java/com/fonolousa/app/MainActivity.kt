package com.fonolousa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.fonolousa.app.audio.AudioPlayer
import com.fonolousa.app.data.DataRepository
import com.fonolousa.app.ui.FonoLousaApp
import com.fonolousa.app.ui.theme.FonoLousaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val repository = remember { DataRepository(this) }
            val audioPlayer = remember { AudioPlayer(this) }

            DisposableEffect(Unit) {
                onDispose { audioPlayer.release() }
            }

            FonoLousaTheme {
                FonoLousaApp(
                    repository = repository,
                    audioPlayer = audioPlayer
                )
            }
        }
    }
}
