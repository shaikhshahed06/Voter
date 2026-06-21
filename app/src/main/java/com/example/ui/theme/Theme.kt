package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = GeoDarkPrimary,
    secondary = GeoDarkSecondary,
    tertiary = GeoDarkTertiary,
    background = GeoDarkBackground,
    surface = GeoDarkSurface,
    surfaceVariant = GeoDarkSurfaceVariant,
    outline = GeoDarkBorder,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
  )

private val LightColorScheme =
  lightColorScheme(
    primary = GeoPrimary,
    secondary = GeoSecondary,
    tertiary = GeoTertiary,
    background = GeoBackground,
    surface = GeoSurface,
    surfaceVariant = GeoSurfaceVariant,
    outline = GeoBorder,
    primaryContainer = GeoPrimaryContainer,
    secondaryContainer = GeoSecondaryContainer,
    tertiaryContainer = GeoTertiaryContainer,
    onPrimary = GeoOnPrimary,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = GeoOnSurface,
    onSurface = GeoOnSurface,
    onPrimaryContainer = GeoOnPrimaryContainer
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color can be enabled, but we default to false to prioritize our custom civic brand colors
  dynamicColor: Boolean = false,
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
