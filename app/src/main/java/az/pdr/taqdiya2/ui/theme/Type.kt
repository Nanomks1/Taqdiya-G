package az.pdr.taqdiya2.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import az.pdr.taqdiya2.R

val CairoFontFamily = FontFamily(
    Font(R.font.cairo, FontWeight.Normal),
    Font(R.font.cairo, FontWeight.Medium),
    Font(R.font.cairo, FontWeight.SemiBold),
    Font(R.font.cairo, FontWeight.Bold)
)

val TajawalFontFamily = FontFamily(
    Font(R.font.tajawal, FontWeight.Normal),
    Font(R.font.tajawal, FontWeight.Medium),
    Font(R.font.tajawal, FontWeight.SemiBold),
    Font(R.font.tajawal, FontWeight.Bold)
)

private val ArabicPlatformStyle = PlatformTextStyle(
    includeFontPadding = true
)

private val ArabicLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

fun buildAppTypography(
    primaryFontFamily: FontFamily = CairoFontFamily,
    secondaryFontFamily: FontFamily = TajawalFontFamily
): Typography {
    return Typography(
        headlineLarge = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            platformStyle = ArabicPlatformStyle,
            lineHeightStyle = ArabicLineHeightStyle
        ),
        headlineMedium = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            platformStyle = ArabicPlatformStyle,
            lineHeightStyle = ArabicLineHeightStyle
        ),
        headlineSmall = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            platformStyle = ArabicPlatformStyle,
            lineHeightStyle = ArabicLineHeightStyle
        ),
        titleLarge = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            lineHeight = 26.sp,
            platformStyle = ArabicPlatformStyle,
            lineHeightStyle = ArabicLineHeightStyle
        ),
        titleMedium = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            platformStyle = ArabicPlatformStyle,
            lineHeightStyle = ArabicLineHeightStyle
        ),
        titleSmall = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            platformStyle = ArabicPlatformStyle,
            lineHeightStyle = ArabicLineHeightStyle
        ),
        bodyLarge = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            platformStyle = ArabicPlatformStyle,
            lineHeightStyle = ArabicLineHeightStyle
        ),
        bodyMedium = TextStyle(
            fontFamily = secondaryFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            platformStyle = ArabicPlatformStyle,
            lineHeightStyle = ArabicLineHeightStyle
        ),
        bodySmall = TextStyle(
            fontFamily = secondaryFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            platformStyle = ArabicPlatformStyle,
            lineHeightStyle = ArabicLineHeightStyle
        ),
        labelLarge = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            platformStyle = ArabicPlatformStyle,
            lineHeightStyle = ArabicLineHeightStyle
        ),
        labelMedium = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            platformStyle = ArabicPlatformStyle,
            lineHeightStyle = ArabicLineHeightStyle
        ),
        labelSmall = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            platformStyle = ArabicPlatformStyle,
            lineHeightStyle = ArabicLineHeightStyle
        )
    )
}

val AppTypography = buildAppTypography()
