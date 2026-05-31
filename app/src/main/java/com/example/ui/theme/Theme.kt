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
        deepEspresso = Color(0xFF1E1814),
        darkMocha = Color(0xFF2A211C),
        coffeeBrown = Color(0xFFB08968),
        softLatte = Color(0xFFDDB892),
        warmCream = Color(0xFFF8F4F0),
        secondaryText = Color(0xFFCBB9A8),
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
            deepEspresso = WarmCream,
            darkMocha = WarmCream,
            coffeeBrown = DeepEspresso,
            softLatte = SoftLatte,
            warmCream = DeepEspresso,
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
