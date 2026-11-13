package az.pdr.taqdiya2.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

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
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
