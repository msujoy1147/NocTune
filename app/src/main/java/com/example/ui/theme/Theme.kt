package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CoffeeBrown,
    secondary = SoftLatte,
    tertiary = WarmCream,
    background = DeepEspresso,
    surface = DarkMocha,
    onPrimary = WarmCream,
    onSecondary = DeepEspresso,
    onTertiary = DeepEspresso,
    onBackground = WarmCream,
    onSurface = WarmCream,
    surfaceVariant = DarkMocha,
    onSurfaceVariant = SecondaryText
)

private val LightColorScheme = lightColorScheme(
    primary = CoffeeBrown,
    secondary = SoftLatte,
    tertiary = DeepEspresso,
    background = WarmCream,
    surface = WarmCream,
    onPrimary = DeepEspresso,
    onSecondary = DeepEspresso,
    onTertiary = WarmCream,
    onBackground = DeepEspresso,
    onSurface = DeepEspresso,
    surfaceVariant = SoftLatte,
    onSurfaceVariant = DeepEspresso
)

@Composable
fun NocTuneTheme(
    darkTheme: Boolean = true, // Force dark luxury lounge theme by default for premium feel
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
