package az.pdr.taqdiya2.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
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

private val DarkColorScheme = darkColorScheme(
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

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun TaqdiyaTheme(
    themeMode: String = "LIGHT",
    uiScaleMode: String = "NORMAL",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        "DARK" -> true
        "SYSTEM" -> systemDark
        else -> false
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    val baseDensity = LocalDensity.current
    val (densityScale, fontScaleMultiplier) = when (uiScaleMode) {
        "LARGE" -> 1.04f to 1.10f
        "EXTRA_LARGE" -> 1.07f to 1.18f
        else -> 1.0f to 1.0f
    }
    val scaledDensity = Density(
        density = baseDensity.density * densityScale,
        fontScale = baseDensity.fontScale * fontScaleMultiplier
    )

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
