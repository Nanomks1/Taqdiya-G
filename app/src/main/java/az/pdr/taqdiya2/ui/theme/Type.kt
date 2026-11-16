package az.pdr.taqdiya2.ui.theme

import android.content.Context
import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalTextApi::class)
fun loadAssetFontFamilyOrFallback(
    context: Context,
    fontFamilyKey: String
): FontFamily {
    val normalizedKey = when (fontFamilyKey) {
        "CAIRO", "TAJAWAL", "CUSTOM", "" -> "IBM_PLEX"
        else -> fontFamilyKey
    }

    if (normalizedKey == "SYSTEM") {
        return FontFamily.SansSerif
    }

    return runCatching {
        val availableFonts = context.assets.list("fonts")?.toSet() ?: emptySet()
        when (normalizedKey) {
            "VAZIRMATN" -> {
                if ("vazirmatn_var.ttf" !in availableFonts) {
                    FontFamily.SansSerif
                } else {
                    val weights = listOf(
                        FontWeight.Normal,
                        FontWeight.Medium,
                        FontWeight.SemiBold,
                        FontWeight.Bold
                    )
                    FontFamily(
                        weights.map { w ->
                            Font(
                                path = "fonts/vazirmatn_var.ttf",
                                assetManager = context.assets,
                                weight = w,
                                variationSettings = FontVariation.Settings(
                                    FontVariation.weight(w.weight)
                                )
                            )
                        }
                    )
                }
            }

            "BALOO" -> {
                if ("baloo_bhaijaan_2_var.ttf" !in availableFonts) {
                    FontFamily.SansSerif
                } else {
                    val weights = listOf(
                        FontWeight.Normal,
                        FontWeight.Medium,
                        FontWeight.SemiBold,
                        FontWeight.Bold
                    )
                    FontFamily(
                        weights.map { w ->
                            Font(
                                path = "fonts/baloo_bhaijaan_2_var.ttf",
                                assetManager = context.assets,
                                weight = w,
                                variationSettings = FontVariation.Settings(
                                    FontVariation.weight(w.weight)
                                )
                            )
                        }
                    )
                }
            }

            "RUBIK" -> {
                val rubikFile = when {
                    "rubik_var.ttf" in availableFonts -> "rubik_var.ttf"
                    else -> availableFonts.firstOrNull {
                        it.lowercase().contains("rubik") &&
                            (it.lowercase().endsWith(".ttf") || it.lowercase().endsWith(".otf"))
                    }
                }
                if (rubikFile == null) {
                    FontFamily.SansSerif
                } else {
                    val weights = listOf(
                        FontWeight.Normal,
                        FontWeight.Medium,
                        FontWeight.SemiBold,
                        FontWeight.Bold
                    )
                    runCatching {
                        FontFamily(
                            weights.map { w ->
                                Font(
                                    path = "fonts/$rubikFile",
                                    assetManager = context.assets,
                                    weight = w,
                                    variationSettings = FontVariation.Settings(
                                        FontVariation.weight(w.weight)
                                    )
                                )
                            }
                        )
                    }.getOrElse {
                        FontFamily(
                            weights.map { w ->
                                Font(
                                    path = "fonts/$rubikFile",
                                    assetManager = context.assets,
                                    weight = w
                                )
                            }
                        )
                    }
                }
            }

            else -> {
                val hasIbmPlex = "ibm_plex_regular.ttf" in availableFonts &&
                    "ibm_plex_medium.ttf" in availableFonts &&
                    "ibm_plex_bold.ttf" in availableFonts
                if (!hasIbmPlex) {
                    FontFamily.SansSerif
                } else {
                    FontFamily(
                        Font(
                            path = "fonts/ibm_plex_regular.ttf",
                            assetManager = context.assets,
                            weight = FontWeight.Normal
                        ),
                        Font(
                            path = "fonts/ibm_plex_medium.ttf",
                            assetManager = context.assets,
                            weight = FontWeight.Medium
                        ),
                        Font(
                            path = "fonts/ibm_plex_medium.ttf",
                            assetManager = context.assets,
                            weight = FontWeight.SemiBold
                        ),
                        Font(
                            path = "fonts/ibm_plex_bold.ttf",
                            assetManager = context.assets,
                            weight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }.getOrDefault(FontFamily.SansSerif)
}

private val ArabicPlatformStyle = PlatformTextStyle(
    includeFontPadding = true
)

private val ArabicLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

fun buildAppTypography(
    primaryFontFamily: FontFamily = FontFamily.SansSerif,
    secondaryFontFamily: FontFamily = primaryFontFamily,
    fontSizeScale: Float = 1.0f
): Typography {
    return Typography(
        headlineLarge = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (28f * fontSizeScale).sp,
            lineHeight = (36f * fontSizeScale).sp,
            fontFeatureSettings = "tnum",
            platformStyle = ArabicPlatformStyle,
            lineHeightStyle = ArabicLineHeightStyle
        ),
        headlineMedium = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (24f * fontSizeScale).sp,
            lineHeight = (32f * fontSizeScale).sp,
            fontFeatureSettings = "tnum",
            platformStyle = ArabicPlatformStyle,
            lineHeightStyle = ArabicLineHeightStyle
        ),
        headlineSmall = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (20f * fontSizeScale).sp,
            lineHeight = (28f * fontSizeScale).sp,
            fontFeatureSettings = "tnum",
            platformStyle = ArabicPlatformStyle,
            lineHeightStyle = ArabicLineHeightStyle
        ),
        titleLarge = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (19f * fontSizeScale).sp,
            lineHeight = (26f * fontSizeScale).sp,
            fontFeatureSettings = "tnum",
            platformStyle = ArabicPlatformStyle,
            lineHeightStyle = ArabicLineHeightStyle
        ),
        titleMedium = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (17f * fontSizeScale).sp,
            lineHeight = (24f * fontSizeScale).sp,
            fontFeatureSettings = "tnum",
            platformStyle = ArabicPlatformStyle,
            lineHeightStyle = ArabicLineHeightStyle
        ),
        titleSmall = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (15f * fontSizeScale).sp,
            lineHeight = (22f * fontSizeScale).sp,
            fontFeatureSettings = "tnum",
            platformStyle = ArabicPlatformStyle,
            lineHeightStyle = ArabicLineHeightStyle
        ),
        bodyLarge = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (17f * fontSizeScale).sp,
            lineHeight = (24f * fontSizeScale).sp,
            fontFeatureSettings = "tnum",
            platformStyle = ArabicPlatformStyle,
            lineHeightStyle = ArabicLineHeightStyle
        ),
        bodyMedium = TextStyle(
            fontFamily = secondaryFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (15f * fontSizeScale).sp,
            lineHeight = (21f * fontSizeScale).sp,
            fontFeatureSettings = "tnum",
            platformStyle = ArabicPlatformStyle,
            lineHeightStyle = ArabicLineHeightStyle
        ),
        bodySmall = TextStyle(
            fontFamily = secondaryFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (13f * fontSizeScale).sp,
            lineHeight = (19f * fontSizeScale).sp,
            fontFeatureSettings = "tnum",
            platformStyle = ArabicPlatformStyle,
            lineHeightStyle = ArabicLineHeightStyle
        ),
        labelLarge = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (15f * fontSizeScale).sp,
            lineHeight = (21f * fontSizeScale).sp,
            fontFeatureSettings = "tnum",
            platformStyle = ArabicPlatformStyle,
            lineHeightStyle = ArabicLineHeightStyle
        ),
        labelMedium = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (13f * fontSizeScale).sp,
            lineHeight = (18f * fontSizeScale).sp,
            fontFeatureSettings = "tnum",
            platformStyle = ArabicPlatformStyle,
            lineHeightStyle = ArabicLineHeightStyle
        ),
        labelSmall = TextStyle(
            fontFamily = primaryFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11f * fontSizeScale).sp,
            lineHeight = (16f * fontSizeScale).sp,
            fontFeatureSettings = "tnum",
            platformStyle = ArabicPlatformStyle,
            lineHeightStyle = ArabicLineHeightStyle
        )
    )
}

val AppTypography = buildAppTypography()
