package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = ElegantDarkPrimary,
    onPrimary = ElegantDarkOnPrimary,
    primaryContainer = ElegantDarkSurfaceVariant,
    onPrimaryContainer = ElegantDarkSecondary,
    secondary = ElegantDarkSecondary,
    onSecondary = ElegantDarkOnSecondary,
    secondaryContainer = ElegantDarkSurfaceVariant,
    onSecondaryContainer = ElegantDarkSecondary,
    tertiary = ElegantDarkTertiary,
    onTertiary = ElegantDarkOnTertiary,
    background = ElegantDarkBackground,
    onBackground = Color(0xFFE6E1E5),
    surface = ElegantDarkSurface,
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = ElegantDarkSurfaceVariant,
    onSurfaceVariant = ElegantDarkOnSurfaceVariant,
    outline = ElegantDarkOutline,
    error = ElegantDarkError,
    onError = ElegantDarkOnError
  )

private val LightColorScheme = DarkColorScheme // Standard Elegant Dark always as requested

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to true for the Elegant Dark theme
  dynamicColor: Boolean = false, // Set to false to prevent system-wide colors from overriding our theme
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
