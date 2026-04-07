package com.example.spendingsappandroid.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    // Primary — sky blue
    primary = Blue80,
    onPrimary = Blue20,
    primaryContainer = Blue30,
    onPrimaryContainer = Blue90,
    inversePrimary = BlueInverse,
    // Secondary — slate indigo
    secondary = Indigo80,
    onSecondary = Indigo20,
    secondaryContainer = Indigo30,
    onSecondaryContainer = Indigo90,
    // Tertiary — emerald green
    tertiary = Emerald80,
    onTertiary = Emerald20,
    tertiaryContainer = Emerald30,
    onTertiaryContainer = Emerald90,
    // Error
    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,
    // Background & surface stack
    background = Navy950,
    onBackground = OnDarkSurface,
    surface = Navy900,
    onSurface = OnDarkSurface,
    surfaceVariant = Navy600,
    onSurfaceVariant = OnDarkSurfaceVariant,
    surfaceContainerLowest = Navy850,
    surfaceContainerLow = Navy800,
    surfaceContainer = Navy750,
    surfaceContainerHigh = Navy700,
    surfaceContainerHighest = Navy650,
    // Inverse
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    // Outlines
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    scrim = Navy950,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
)

@Composable
fun SpendingsAppAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled — preserves our hand-crafted palette on all devices
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
