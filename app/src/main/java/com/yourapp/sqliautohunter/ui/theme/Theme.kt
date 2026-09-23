package com.yourapp.sqliautohunter.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    primaryContainer = PrimaryDark,
    secondary = Secondary,
    secondaryContainer = SecondaryDark,
    surface = Surface,
    background = Background,
    error = Error,
    onPrimary = OnPrimary,
    onSecondary = OnSecondary,
    onSurface = OnSurface,
    onBackground = OnBackground,
    onError = OnError
)

@Composable
fun Theme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
