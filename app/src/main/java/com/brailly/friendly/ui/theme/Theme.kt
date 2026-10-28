package com.brailly.friendly.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.brailly.friendly.ui.designsystem.withDuolingoFonts

private val DarkColorScheme = darkColorScheme(
    primary = BraillyBlueLight,
    onPrimary = Color(0xFF06222F),
    primaryContainer = BraillyBlueDark,
    onPrimaryContainer = Color(0xFFCDE5F7),
    secondary = BraillyOrange,
    onSecondary = Color(0xFF3A2100),
    secondaryContainer = Color(0xFF5A3410),
    onSecondaryContainer = BraillyOrangeLight,
    tertiary = BraillyOrangeLight,
    background = BackgroundDark,
    onBackground = OnDark,
    surface = SurfaceDark,
    onSurface = OnDark,
    surfaceVariant = Color(0xFF2A2E34),
    onSurfaceVariant = OnDarkVariant,
    error = ErrorRed,
    outline = Color(0xFF3A3F46),
)

private val LightColorScheme = lightColorScheme(
    primary = BraillyBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDE5F7),
    onPrimaryContainer = BraillyBlueDark,
    secondary = BraillyOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE4CD),
    onSecondaryContainer = Color(0xFF5A3410),
    tertiary = BraillyOrange,
    background = BackgroundLight,
    onBackground = OnLight,
    surface = SurfaceLight,
    onSurface = OnLight,
    surfaceVariant = Color(0xFFEBEDF0),
    onSurfaceVariant = OnLightVariant,
    error = ErrorRed,
    outline = Color(0xFFD5D8DD),
)

@Composable
fun BraillyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        // Hierarki tipografi design system ikut diterapkan ke komponen Material
        // yang tidak di-restyle, misalnya AlertDialog dan FilterChip, supaya
        // tidak ada dua suara huruf yang bercampur di satu layar.
        typography = Typography.withDuolingoFonts(),
        shapes = BraillyShapes,
        content = content
    )
}
