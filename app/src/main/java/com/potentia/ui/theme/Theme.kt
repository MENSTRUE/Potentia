package com.potentia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ivory = Color(0xFFFAF9F6)
val Charcoal = Color(0xFF171714)
val Gold = Color(0xFFC69A3A)
val DeepGold = Color(0xFF9A7224)
val Stone = Color(0xFFE8E5DE)
val Muted = Color(0xFF6F6C65)
val Success = Color(0xFF4A7C59)
val Warning = Color(0xFFD8A33A)

private val PotentiaColors = lightColorScheme(
    primary = Gold,
    onPrimary = Ivory,
    background = Ivory,
    onBackground = Charcoal,
    surface = Color.White,
    onSurface = Charcoal,
    secondary = DeepGold,
    outline = Stone
)

@Composable
fun PotentiaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PotentiaColors,
        content = content
    )
}
