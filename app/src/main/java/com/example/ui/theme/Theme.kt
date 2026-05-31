package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

data class AppColors(
    val deepEspresso: Color,
    val darkMocha: Color,
    val coffeeBrown: Color,
    val softLatte: Color,
    val warmCream: Color,
    val secondaryText: Color,
    val isNight: Boolean
)

val LocalAppColors = staticCompositionLocalOf {
    AppColors(
        deepEspresso = Color(0xFF040209),
        darkMocha = Color(0xFF090615),
        coffeeBrown = Color(0xFF6B4EE0),
        softLatte = Color(0xFF9079EC),
        warmCream = Color(0xFFCAC5D6),
        secondaryText = Color(0xFF676176),
        isNight = true
    )
}

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

private val LightColorScheme = darkColorScheme( // Also use darkColorScheme for twilight mode to protect the eyes completely
    primary = CoffeeBrown,
    secondary = SoftLatte,
    tertiary = WarmCream,
    background = Color(0xFF0F0B1E), // Soft midnight twilight background (very dim and gentle)
    surface = Color(0xFF16112C),    // Cozy twilight surface/card background
    onPrimary = WarmCream,
    onSecondary = Color(0xFF0F0B1E),
    onTertiary = Color(0xFF0F0B1E),
    onBackground = WarmCream,
    onSurface = WarmCream,
    surfaceVariant = Color(0xFF16112C),
    onSurfaceVariant = SecondaryText
)

@Composable
fun NocTuneTheme(
    darkTheme: Boolean = true, // Force dark luxury lounge theme by default for premium feel
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val appColors = if (darkTheme) {
        AppColors(
            deepEspresso = DeepEspresso,
            darkMocha = DarkMocha,
            coffeeBrown = CoffeeBrown,
            softLatte = SoftLatte,
            warmCream = WarmCream,
            secondaryText = SecondaryText,
            isNight = true
        )
    } else {
        AppColors(
            deepEspresso = Color(0xFF0F0B1E), // Soft twilight dark purple
            darkMocha = Color(0xFF16112C),    // Cozy twilight surface/card background
            coffeeBrown = CoffeeBrown,
            softLatte = SoftLatte,
            warmCream = WarmCream,
            secondaryText = SecondaryText,
            isNight = false
        )
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
