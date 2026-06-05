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

private val LightColorScheme = lightColorScheme(
    primary = CoffeeBrown,
    secondary = SoftLatte,
    tertiary = Color(0xFF140D2B),
    background = Color(0xFFFFFFFF), // Pure white background
    surface = Color(0xFFF4F3F8),    // Clean elegant surface/card background
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFF040209),
    onBackground = Color(0xFF140D2B),
    onSurface = Color(0xFF140D2B),
    surfaceVariant = Color(0xFFF4F3F8),
    onSurfaceVariant = Color(0xFF6E6E73)
)

fun isColorDark(color: Color): Boolean {
    val luminance = 0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue
    return luminance < 0.5f
}

fun blendColor(color1: Color, color2: Color, ratio: Float): Color {
    return Color(
        red = color1.red * (1 - ratio) + color2.red * ratio,
        green = color1.green * (1 - ratio) + color2.green * ratio,
        blue = color1.blue * (1 - ratio) + color2.blue * ratio,
        alpha = color1.alpha * (1 - ratio) + color2.alpha * ratio
    )
}

@Composable
fun NocTuneTheme(
    darkTheme: Boolean = true, // Force dark luxury lounge theme by default for premium feel
    themeColorHex: String? = null,
    themeBrightness: Float = 0.6f,
    content: @Composable () -> Unit
) {
    val targetBlack = Color(0xFF020105)
    val targetWhite = Color(0xFFFAFAFC)
    val finalBgColor: Color

    if (themeColorHex != null) {
        var baseBg = Color(0xFF151030)
        try {
            baseBg = Color(android.graphics.Color.parseColor(themeColorHex))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val isBaseDark = isColorDark(baseBg)
        finalBgColor = if (isBaseDark) {
            blendColor(baseBg, targetBlack, 1f - themeBrightness)
        } else {
            blendColor(baseBg, targetWhite, 1f - themeBrightness)
        }
    } else {
        // Dynamic brightness scaling for the default luxury night theme
        val defaultBaseBg = if (darkTheme) Color(0xFF151030) else Color(0xFFFFFFFF)
        finalBgColor = if (darkTheme) {
            blendColor(defaultBaseBg, targetBlack, 1f - themeBrightness)
        } else {
            blendColor(defaultBaseBg, targetWhite, 1f - themeBrightness)
        }
    }

    val isBgDark = isColorDark(finalBgColor)
    val appColors: AppColors
    val colorScheme: androidx.compose.material3.ColorScheme

    if (isBgDark) {
        val deepEspressoVal = finalBgColor
        val darkMochaVal = blendColor(finalBgColor, Color.White, 0.08f * themeBrightness + 0.02f)
        
        // Scale action components and typography
        val coffeeBrownVal = blendColor(CoffeeBrown, targetBlack, 0.45f * (1f - themeBrightness))
        val softLatteVal = blendColor(SoftLatte, targetBlack, 0.45f * (1f - themeBrightness))
        val warmCreamVal = blendColor(Color(0xFFE8E5EE), Color(0xFF807A8A), 1f - themeBrightness)
        val secondaryTextVal = blendColor(Color(0xFFCAC5D6), Color(0xFF4C4656), 1f - themeBrightness)

        appColors = AppColors(
            deepEspresso = deepEspressoVal,
            darkMocha = darkMochaVal,
            coffeeBrown = coffeeBrownVal,
            softLatte = softLatteVal,
            warmCream = warmCreamVal,
            secondaryText = secondaryTextVal,
            isNight = true
        )
        
        colorScheme = DarkColorScheme.copy(
            primary = coffeeBrownVal,
            secondary = softLatteVal,
            tertiary = warmCreamVal,
            background = deepEspressoVal,
            surface = darkMochaVal,
            onPrimary = warmCreamVal,
            onSecondary = deepEspressoVal,
            onTertiary = deepEspressoVal,
            onBackground = warmCreamVal,
            onSurface = warmCreamVal,
            surfaceVariant = darkMochaVal,
            onSurfaceVariant = secondaryTextVal
        )
    } else {
        val deepEspressoVal = finalBgColor
        val darkMochaVal = blendColor(finalBgColor, Color.Black, 0.06f * themeBrightness + 0.02f)
        
        val coffeeBrownVal = blendColor(CoffeeBrown, targetWhite, 0.4f * (1f - themeBrightness))
        val softLatteVal = blendColor(SoftLatte, targetWhite, 0.4f * (1f - themeBrightness))
        val warmCreamVal = blendColor(Color(0xFF140D2B), Color(0xFF7E7A8A), 1f - themeBrightness)
        val secondaryTextVal = blendColor(Color(0xFF6E6E73), Color(0xFF9FA0A5), 1f - themeBrightness)

        appColors = AppColors(
            deepEspresso = deepEspressoVal,
            darkMocha = darkMochaVal,
            coffeeBrown = coffeeBrownVal,
            softLatte = softLatteVal,
            warmCream = warmCreamVal,
            secondaryText = secondaryTextVal,
            isNight = false
        )
        
        colorScheme = LightColorScheme.copy(
            primary = coffeeBrownVal,
            secondary = softLatteVal,
            tertiary = warmCreamVal,
            background = deepEspressoVal,
            surface = darkMochaVal,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = deepEspressoVal,
            onBackground = warmCreamVal,
            onSurface = warmCreamVal,
            surfaceVariant = darkMochaVal,
            onSurfaceVariant = secondaryTextVal
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
