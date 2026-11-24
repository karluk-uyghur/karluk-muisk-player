package com.example.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.R
import com.example.domain.model.AccentOption
import com.example.domain.model.AppLanguage
import com.example.domain.model.TextColorOption
import com.example.domain.model.ThemeAtmosphere
import com.example.domain.model.UserSettings

val SpaceGroteskFont = FontFamily(Font(R.font.space_grotesk, FontWeight.Normal))
val PlusJakartaFont = FontFamily(Font(R.font.plus_jakarta_sans, FontWeight.Normal))
val NotoArabicFont = FontFamily(Font(R.font.noto_sans_arabic, FontWeight.Normal))
val JetBrainsMonoFont = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal))

@Immutable
data class KarlukDesignTokens(
    val bgTop: Color,
    val bgMiddle: Color,
    val bgBottom: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val accentViolet: Color,
    val emerald: Color,
    val glassSurface: Color,
    val glassSurfaceElevated: Color,
    val glassBorder: Color,
    val glassBorderBright: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isLightTheme: Boolean,
    val glassOpacity: Float,
    val reducedMotion: Boolean,
    val lowPowerMode: Boolean,
    val customBackgroundUri: String? = null
)

val LocalKarlukTokens = staticCompositionLocalOf {
    KarlukDesignTokens(
        bgTop = Color(0xFF040B1F),
        bgMiddle = Color(0xFF081840),
        bgBottom = Color(0xFF0B224E),
        accentPrimary = Color(0xFF00D4FF),
        accentSecondary = Color(0xFF1E6BFF),
        accentViolet = Color(0xFF7C4DFF),
        emerald = Color(0xFF12E29C),
        glassSurface = Color(0xFF162C5E).copy(alpha = 0.24f),
        glassSurfaceElevated = Color(0xFF1E3A78).copy(alpha = 0.36f),
        glassBorder = Color.White.copy(alpha = 0.18f),
        glassBorderBright = Color(0xFF00D4FF).copy(alpha = 0.52f),
        textPrimary = Color(0xFFF4F8FF),
        textSecondary = Color(0xFFAEC2E8),
        isLightTheme = false,
        glassOpacity = 0.14f,
        reducedMotion = false,
        lowPowerMode = false
    )
}

fun resolveTextColorPair(
    option: TextColorOption,
    isLight: Boolean,
    customTextColorArgb: Long = 0L
): Pair<Color, Color> {
    if (customTextColorArgb != 0L) {
        val customPrimary = Color(customTextColorArgb)
        val customSecondary = customPrimary.copy(alpha = 0.78f)
        return customPrimary to customSecondary
    }
    val defaultPair = if (isLight) {
        Color(0xFF0A1938) to Color(0xFF3E5682)
    } else {
        Color(0xFFF4F8FF) to Color(0xFFA9BFE6)
    }
    return when (option) {
        TextColorOption.AUTO -> defaultPair
        TextColorOption.PURE_WHITE -> Color(0xFFFFFFFF) to Color(0xFFD6E4FF)
        TextColorOption.SKY_CYAN -> Color(0xFF56E0FF) to Color(0xFF9BEFFF)
        TextColorOption.GOLD_AMBER -> Color(0xFFFFD54F) to Color(0xFFFFE58F)
        TextColorOption.EMERALD_MINT -> Color(0xFF54F0B4) to Color(0xFF9FF6D4)
        TextColorOption.ROSE_CORAL -> Color(0xFFFF8FAB) to Color(0xFFFFBCCB)
        TextColorOption.OBSIDIAN_BLACK -> Color(0xFF0A152E) to Color(0xFF283A5E)
    }
}

@Composable
fun KarlukTheme(
    settings: UserSettings,
    content: @Composable () -> Unit
) {
    val isLight = settings.themeAtmosphere == ThemeAtmosphere.SILVER_FROST

    val accentColor = when (settings.accentOption) {
        AccentOption.CYAN -> Color(0xFF00D4FF)
        AccentOption.ROYAL_BLUE -> Color(0xFF2D7DFF)
        AccentOption.VIOLET -> Color(0xFF9162FF)
        AccentOption.EMERALD -> Color(0xFF14E39D)
    }

    val secondaryAccent = when (settings.accentOption) {
        AccentOption.CYAN -> Color(0xFF1E6BFF)
        AccentOption.ROYAL_BLUE -> Color(0xFF00D4FF)
        AccentOption.VIOLET -> Color(0xFF00D4FF)
        AccentOption.EMERALD -> Color(0xFF1E6BFF)
    }

    val (bgTop, bgMiddle, bgBottom) = when (settings.themeAtmosphere) {
        ThemeAtmosphere.DEEP_NAVY -> Triple(Color(0xFF040B1F), Color(0xFF081840), Color(0xFF0B224E))
        ThemeAtmosphere.MIDNIGHT_VIOLET -> Triple(Color(0xFF08061C), Color(0xFF140D3A), Color(0xFF1D134D))
        ThemeAtmosphere.ROYAL_OCEAN -> Triple(Color(0xFF031129), Color(0xFF0A2658), Color(0xFF123A7D))
        ThemeAtmosphere.SILVER_FROST -> Triple(Color(0xFFEEF4FF), Color(0xFFDCE8FC), Color(0xFFCADBFA))
    }

    val (textPrimary, textSecondary) = resolveTextColorPair(
        option = settings.textColorOption,
        isLight = isLight,
        customTextColorArgb = settings.customTextColorArgb
    )

    val opacity = settings.glassOpacity.coerceIn(0.06f, 0.32f)
    val tokens = if (isLight) {
        KarlukDesignTokens(
            bgTop = bgTop,
            bgMiddle = bgMiddle,
            bgBottom = bgBottom,
            accentPrimary = if (settings.accentOption == AccentOption.CYAN) Color(0xFF0088CC) else accentColor,
            accentSecondary = secondaryAccent,
            accentViolet = Color(0xFF6B3DF5),
            emerald = Color(0xFF0B9E6B),
            glassSurface = Color.White.copy(alpha = (opacity + 0.50f).coerceAtMost(0.82f)),
            glassSurfaceElevated = Color.White.copy(alpha = (opacity + 0.65f).coerceAtMost(0.92f)),
            glassBorder = Color(0xFF1E448A).copy(alpha = 0.16f),
            glassBorderBright = accentColor.copy(alpha = 0.65f),
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            isLightTheme = true,
            glassOpacity = opacity,
            reducedMotion = settings.reducedMotion,
            lowPowerMode = settings.lowPowerMode,
            customBackgroundUri = settings.customBackgroundUri
        )
    } else {
        KarlukDesignTokens(
            bgTop = bgTop,
            bgMiddle = bgMiddle,
            bgBottom = bgBottom,
            accentPrimary = accentColor,
            accentSecondary = secondaryAccent,
            accentViolet = Color(0xFF7C4DFF),
            emerald = Color(0xFF12E29C),
            glassSurface = Color(0xFF142B63).copy(alpha = opacity + 0.14f),
            glassSurfaceElevated = Color(0xFF1A3880).copy(alpha = opacity + 0.28f),
            glassBorder = Color(0xFFD8E6FF).copy(alpha = 0.18f),
            glassBorderBright = accentColor.copy(alpha = 0.60f),
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            isLightTheme = false,
            glassOpacity = opacity,
            reducedMotion = settings.reducedMotion,
            lowPowerMode = settings.lowPowerMode,
            customBackgroundUri = settings.customBackgroundUri
        )
    }

    val colorScheme: ColorScheme = if (isLight) {
        lightColorScheme(
            primary = tokens.accentPrimary,
            onPrimary = Color.White,
            secondary = tokens.accentSecondary,
            onSecondary = Color.White,
            tertiary = tokens.accentViolet,
            background = bgMiddle,
            onBackground = tokens.textPrimary,
            surface = tokens.glassSurfaceElevated,
            onSurface = tokens.textPrimary,
            surfaceVariant = tokens.glassSurface,
            onSurfaceVariant = tokens.textSecondary,
            outline = tokens.glassBorder,
            error = Color(0xFFD9385A)
        )
    } else {
        darkColorScheme(
            primary = tokens.accentPrimary,
            onPrimary = Color(0xFF041126),
            secondary = tokens.accentSecondary,
            onSecondary = Color.White,
            tertiary = tokens.accentViolet,
            background = bgMiddle,
            onBackground = tokens.textPrimary,
            surface = Color(0xFF0C1E4A),
            onSurface = tokens.textPrimary,
            surfaceVariant = Color(0xFF12285E),
            onSurfaceVariant = tokens.textSecondary,
            outline = tokens.glassBorder,
            error = Color(0xFFFF5E7E)
        )
    }

    val isRtlLang = settings.language == AppLanguage.UYGHUR || settings.language == AppLanguage.ARABIC
    val displayFamily = if (isRtlLang) NotoArabicFont else SpaceGroteskFont
    val bodyFamily = if (isRtlLang) NotoArabicFont else PlusJakartaFont

    val typography = Typography(
        headlineLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 30.sp
        ),
        titleLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 19.sp,
            lineHeight = 26.sp
        ),
        titleMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 19.sp
        ),
        labelLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        labelMedium = TextStyle(
            fontFamily = JetBrainsMonoFont,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 15.sp
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp
        )
    )

    val shapes = Shapes(
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(18.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(30.dp)
    )

    CompositionLocalProvider(LocalKarlukTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}
