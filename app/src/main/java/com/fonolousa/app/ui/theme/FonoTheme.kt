package com.fonolousa.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ChalkWhite = Color(0xFFF8F8EE)
val ChalkGreen = Color(0xFF1B5E20)
val ChalkGreenAlt = Color(0xFF2E7D32)
val ChalkShadow = Color(0x66000000)

private val colorScheme = darkColorScheme(
    primary = ChalkWhite,
    onPrimary = ChalkGreen,
    background = ChalkGreen,
    onBackground = ChalkWhite,
    surface = Color(0x332E7D32),
    onSurface = ChalkWhite
)

@Composable
fun FonoLousaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
