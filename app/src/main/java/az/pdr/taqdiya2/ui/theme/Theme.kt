package az.pdr.taqdiya2.ui.theme

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val LocalIsDarkTheme = compositionLocalOf { false }

@Composable
fun Modifier.ambientCardShadow(
    shape: Shape = RoundedCornerShape(18.dp),
    elevation: Dp = 6.dp
): Modifier {
    val isDark = LocalIsDarkTheme.current
    val primary = MaterialTheme.colorScheme.primary
    return if (isDark) {
        this.shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.6f),
            spotColor = Color.Black.copy(alpha = 0.6f)
        )
    } else {
        this.shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = primary.copy(alpha = 0.24f),
            spotColor = primary.copy(alpha = 0.18f)
        )
    }
}

@Composable
fun ambientCardBorder(): BorderStroke {
    val isDark = LocalIsDarkTheme.current
    return if (isDark) {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
    } else {
        BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    }
}

private val TerracottaLightColorScheme = lightColorScheme(
    primary = SlateInkPrimaryLight,
    onPrimary = OnSlateInkPrimaryLight,
    primaryContainer = SlateInkContainerLight,
    onPrimaryContainer = OnSlateInkContainerLight,
    secondary = TerracottaSecondaryLight,
    onSecondary = OnTerracottaSecondaryLight,
    secondaryContainer = TerracottaContainerLight,
    onSecondaryContainer = OnTerracottaContainerLight,
    tertiary = OliveTertiaryLight,
    onTertiary = OnOliveTertiaryLight,
    tertiaryContainer = OliveContainerLight,
    onTertiaryContainer = OnOliveContainerLight,
    background = LimestoneBackgroundLight,
    onBackground = OnLimestoneBackgroundLight,
    surface = IvorySurfaceLight,
    onSurface = OnIvorySurfaceLight,
    surfaceVariant = SandSurfaceVariantLight,
    onSurfaceVariant = OnSandSurfaceVariantLight,
    outline = WarmOutlineLight
)

private val TerracottaDarkColorScheme = darkColorScheme(
    primary = SlateInkPrimaryDark,
    onPrimary = OnSlateInkPrimaryDark,
    primaryContainer = SlateInkContainerDark,
    onPrimaryContainer = OnSlateInkContainerDark,
    secondary = TerracottaSecondaryDark,
    onSecondary = OnTerracottaSecondaryDark,
    secondaryContainer = TerracottaContainerDark,
    onSecondaryContainer = OnTerracottaContainerDark,
    tertiary = OliveTertiaryDark,
    onTertiary = OnOliveTertiaryDark,
    tertiaryContainer = OliveContainerDark,
    onTertiaryContainer = OnOliveContainerDark,
    background = LimestoneBackgroundDark,
    onBackground = OnLimestoneBackgroundDark,
    surface = IvorySurfaceDark,
    onSurface = OnIvorySurfaceDark,
    surfaceVariant = SandSurfaceVariantDark,
    onSurfaceVariant = OnSandSurfaceVariantDark,
    outline = WarmOutlineDark
)

private val CalmSageLightColorScheme = lightColorScheme(
    primary = SagePrimaryLight,
    onPrimary = OnSagePrimaryLight,
    primaryContainer = SageContainerLight,
    onPrimaryContainer = OnSageContainerLight,
    secondary = SageSecondaryLight,
    onSecondary = OnSageSecondaryLight,
    secondaryContainer = SageSecondaryContainerLight,
    onSecondaryContainer = OnSageSecondaryContainerLight,
    tertiary = SageTertiaryLight,
    onTertiary = OnSageTertiaryLight,
    tertiaryContainer = SageTertiaryContainerLight,
    onTertiaryContainer = OnSageTertiaryContainerLight,
    background = SageBackgroundLight,
    onBackground = OnSageBackgroundLight,
    surface = SageSurfaceLight,
    onSurface = OnSageSurfaceLight,
    surfaceVariant = SageSurfaceVariantLight,
    onSurfaceVariant = OnSageSurfaceVariantLight,
    outline = SageOutlineLight
)

private val CalmSageDarkColorScheme = darkColorScheme(
    primary = SagePrimaryDark,
    onPrimary = OnSagePrimaryDark,
    primaryContainer = SageContainerDark,
    onPrimaryContainer = OnSageContainerDark,
    secondary = SageSecondaryDark,
    onSecondary = OnSageSecondaryDark,
    secondaryContainer = SageSecondaryContainerDark,
    onSecondaryContainer = OnSageSecondaryContainerDark,
    tertiary = SageTertiaryDark,
    onTertiary = OnSageTertiaryDark,
    tertiaryContainer = SageTertiaryContainerDark,
    onTertiaryContainer = OnSageTertiaryContainerDark,
    background = SageBackgroundDark,
    onBackground = OnSageBackgroundDark,
    surface = SageSurfaceDark,
    onSurface = OnSageSurfaceDark,
    surfaceVariant = SageSurfaceVariantDark,
    onSurfaceVariant = OnSageSurfaceVariantDark,
    outline = SageOutlineDark
)

private val VibrantCobaltLightColorScheme = lightColorScheme(
    primary = CobaltPrimaryLight,
    onPrimary = OnCobaltPrimaryLight,
    primaryContainer = CobaltContainerLight,
    onPrimaryContainer = OnCobaltContainerLight,
    secondary = CobaltSecondaryLight,
    onSecondary = OnCobaltSecondaryLight,
    secondaryContainer = CobaltSecondaryContainerLight,
    onSecondaryContainer = OnCobaltSecondaryContainerLight,
    tertiary = CobaltTertiaryLight,
    onTertiary = OnCobaltTertiaryLight,
    tertiaryContainer = CobaltTertiaryContainerLight,
    onTertiaryContainer = OnCobaltTertiaryContainerLight,
    background = CobaltBackgroundLight,
    onBackground = OnCobaltBackgroundLight,
    surface = CobaltSurfaceLight,
    onSurface = OnCobaltSurfaceLight,
    surfaceVariant = CobaltSurfaceVariantLight,
    onSurfaceVariant = OnCobaltSurfaceVariantLight,
    outline = CobaltOutlineLight
)

private val VibrantCobaltDarkColorScheme = darkColorScheme(
    primary = CobaltPrimaryDark,
    onPrimary = OnCobaltPrimaryDark,
    primaryContainer = CobaltContainerDark,
    onPrimaryContainer = OnCobaltContainerDark,
    secondary = CobaltSecondaryDark,
    onSecondary = OnCobaltSecondaryDark,
    secondaryContainer = CobaltSecondaryContainerDark,
    onSecondaryContainer = OnCobaltSecondaryContainerDark,
    tertiary = CobaltTertiaryDark,
    onTertiary = OnCobaltTertiaryDark,
    tertiaryContainer = CobaltTertiaryContainerDark,
    onTertiaryContainer = OnCobaltTertiaryContainerDark,
    background = CobaltBackgroundDark,
    onBackground = OnCobaltBackgroundDark,
    surface = CobaltSurfaceDark,
    onSurface = OnCobaltSurfaceDark,
    surfaceVariant = CobaltSurfaceVariantDark,
    onSurfaceVariant = OnCobaltSurfaceVariantDark,
    outline = CobaltOutlineDark
)

private val RoyalPlumLightColorScheme = lightColorScheme(
    primary = PlumPrimaryLight,
    onPrimary = OnPlumPrimaryLight,
    primaryContainer = PlumContainerLight,
    onPrimaryContainer = OnPlumContainerLight,
    secondary = PlumSecondaryLight,
    onSecondary = OnPlumSecondaryLight,
    secondaryContainer = PlumSecondaryContainerLight,
    onSecondaryContainer = OnPlumSecondaryContainerLight,
    tertiary = PlumTertiaryLight,
    onTertiary = OnPlumTertiaryLight,
    tertiaryContainer = PlumTertiaryContainerLight,
    onTertiaryContainer = OnPlumTertiaryContainerLight,
    background = PlumBackgroundLight,
    onBackground = OnPlumBackgroundLight,
    surface = PlumSurfaceLight,
    onSurface = OnPlumSurfaceLight,
    surfaceVariant = PlumSurfaceVariantLight,
    onSurfaceVariant = OnPlumSurfaceVariantLight,
    outline = PlumOutlineLight
)

private val RoyalPlumDarkColorScheme = darkColorScheme(
    primary = PlumPrimaryDark,
    onPrimary = OnPlumPrimaryDark,
    primaryContainer = PlumContainerDark,
    onPrimaryContainer = OnPlumContainerDark,
    secondary = PlumSecondaryDark,
    onSecondary = OnPlumSecondaryDark,
    secondaryContainer = PlumSecondaryContainerDark,
    onSecondaryContainer = OnPlumSecondaryContainerDark,
    tertiary = PlumTertiaryDark,
    onTertiary = OnPlumTertiaryDark,
    tertiaryContainer = PlumTertiaryContainerDark,
    onTertiaryContainer = OnPlumTertiaryContainerDark,
    background = PlumBackgroundDark,
    onBackground = OnPlumBackgroundDark,
    surface = PlumSurfaceDark,
    onSurface = OnPlumSurfaceDark,
    surfaceVariant = PlumSurfaceVariantDark,
    onSurfaceVariant = OnPlumSurfaceVariantDark,
    outline = PlumOutlineDark
)

fun resolveColorScheme(colorPalette: String, darkTheme: Boolean): ColorScheme {
    return when (colorPalette) {
        "CALM_SAGE" -> if (darkTheme) CalmSageDarkColorScheme else CalmSageLightColorScheme
        "VIBRANT_COBALT" -> if (darkTheme) VibrantCobaltDarkColorScheme else VibrantCobaltLightColorScheme
        "ROYAL_PLUM" -> if (darkTheme) RoyalPlumDarkColorScheme else RoyalPlumLightColorScheme
        else -> if (darkTheme) TerracottaDarkColorScheme else TerracottaLightColorScheme
    }
}

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun TaqdiyaTheme(
    themeMode: String = "LIGHT",
    uiScaleMode: String = "NORMAL",
    colorPalette: String = "TERRACOTTA",
    fontFamilyKey: String = "IBM_PLEX",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        "DARK" -> true
        "SYSTEM" -> systemDark
        else -> false
    }
    val colorScheme = resolveColorScheme(colorPalette, darkTheme)

    val view = LocalView.current
    val context = view.context
    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as? Activity)?.window
            if (window != null) {
                window.decorView.setBackgroundColor(colorScheme.background.toArgb())
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    val typography = remember(fontFamilyKey) {
        val resolvedFamily = loadAssetFontFamilyOrFallback(
            context = context,
            fontFamilyKey = fontFamilyKey
        )
        val perFontScale = if (fontFamilyKey == "RUBIK") 0.91f else 1.0f
        buildAppTypography(
            primaryFontFamily = resolvedFamily,
            secondaryFontFamily = resolvedFamily,
            fontSizeScale = perFontScale
        )
    }

    val baseDensity = LocalDensity.current
    val fontScaleMultiplier = when (uiScaleMode) {
        "LARGE" -> 1.08f
        "EXTRA_LARGE" -> 1.16f
        else -> 1.0f
    }
    val effectiveFontScale = if (uiScaleMode == "NORMAL") {
        baseDensity.fontScale
    } else {
        (baseDensity.fontScale * fontScaleMultiplier).coerceAtMost(1.26f)
    }
    val scaledDensity = Density(
        density = baseDensity.density,
        fontScale = effectiveFontScale
    )

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalDensity provides scaledDensity,
        LocalIsDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = AppShapes,
            content = content
        )
    }
}
