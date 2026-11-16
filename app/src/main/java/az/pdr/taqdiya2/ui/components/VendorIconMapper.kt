package az.pdr.taqdiya2.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.SetMeal
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object VendorIconMapper {
    data class IconOption(
        val key: String,
        val label: String,
        val icon: ImageVector
    )

    /**
     * Custom Apple / Fruit Vector for "مول الفواكه" (Replaces Fork & Spoon)
     */
    val FruitAppleIcon: ImageVector by lazy {
        ImageVector.Builder(
            name = "FruitApple",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Stem & Leaf at top
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 6.5f)
                quadTo(12f, 3.2f, 14.8f, 2.5f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12.2f, 5.2f)
                quadTo(9.2f, 2.5f, 7.5f, 4.2f)
                quadTo(9.2f, 6.2f, 12.2f, 5.2f)
            }
            // Apple Body
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(12f, 7.2f)
                curveTo(9.5f, 6.2f, 5.2f, 7.5f, 5.2f, 12.4f)
                curveTo(5.2f, 16.8f, 8.2f, 21.2f, 10.6f, 21.2f)
                curveTo(11.3f, 21.2f, 11.6f, 20.7f, 12f, 20.7f)
                curveTo(12.4f, 20.7f, 12.7f, 21.2f, 13.4f, 21.2f)
                curveTo(15.8f, 21.2f, 18.8f, 16.8f, 18.8f, 12.4f)
                curveTo(18.8f, 7.5f, 14.5f, 6.2f, 12f, 7.2f)
                close()
            }
        }.build()
    }

    /**
     * Custom Butcher Cleaver Vector for "الكزار ومول الدجاج" (Replaces Fork & Knife)
     */
    val ButcherCleaverIcon: ImageVector by lazy {
        ImageVector.Builder(
            name = "ButcherCleaver",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Wide Cleaver Blade + Handle
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Blade rectangle/curve
                moveTo(4f, 6f)
                lineTo(15.5f, 6f)
                lineTo(15.5f, 14.5f)
                quadTo(9.5f, 15.2f, 4f, 13.5f)
                close()
                // Handle extending right/down
                moveTo(15.5f, 8f)
                lineTo(20.5f, 8f)
                quadTo(21.5f, 8f, 21.5f, 9.2f)
                lineTo(21.5f, 10.5f)
                quadTo(21.5f, 11.5f, 20.5f, 11.5f)
                lineTo(15.5f, 11.5f)
            }
            // Hanging hole in cleaver blade corner
            path(
                fill = SolidColor(Color.Black),
                stroke = null
            ) {
                moveTo(7.2f, 9.0f)
                arcTo(1.2f, 1.2f, 0f, true, true, 7.19f, 9.0f)
                close()
            }
        }.build()
    }

    /**
     * Custom Cooking Pot / Tagine-Pot Vector for "مول المواعن" (Replaces Refrigerator icon)
     */
    val CookingPotIcon: ImageVector by lazy {
        ImageVector.Builder(
            name = "CookingPot",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Lid knob
                moveTo(10f, 5f)
                lineTo(14f, 5f)
                moveTo(12f, 5f)
                lineTo(12f, 7.5f)
                // Lid dome
                moveTo(5.5f, 9.5f)
                quadTo(12f, 6.2f, 18.5f, 9.5f)
                // Pot rim
                moveTo(4.5f, 9.5f)
                lineTo(19.5f, 9.5f)
                // Pot body
                moveTo(6f, 9.5f)
                lineTo(6.8f, 18.2f)
                quadTo(7f, 20f, 9f, 20f)
                lineTo(15f, 20f)
                quadTo(17f, 20f, 17.2f, 18.2f)
                lineTo(18f, 9.5f)
                // Left handle
                moveTo(6f, 12f)
                lineTo(3.2f, 12f)
                lineTo(3.2f, 14.5f)
                lineTo(6.3f, 14.5f)
                // Right handle
                moveTo(18f, 12f)
                lineTo(20.8f, 12f)
                lineTo(20.8f, 14.5f)
                lineTo(17.7f, 14.5f)
            }
        }.build()
    }

    /**
     * Custom Egg Vector for "مول البيض"
     */
    val EggVendorIcon: ImageVector by lazy {
        ImageVector.Builder(
            name = "EggVendor",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 3.2f)
                curveTo(8.5f, 3.2f, 5.8f, 9.2f, 5.8f, 14.2f)
                curveTo(5.8f, 18.2f, 8.5f, 20.8f, 12f, 20.8f)
                curveTo(15.5f, 20.8f, 18.2f, 18.2f, 18.2f, 14.2f)
                curveTo(18.2f, 9.2f, 15.5f, 3.2f, 12f, 3.2f)
                close()
            }
        }.build()
    }

    val availableIcons = listOf(
        IconOption("store", "حانوت / عطار", Icons.Outlined.Storefront),
        IconOption("eco", "مول الخضرة", Icons.Outlined.Eco),
        IconOption("nutrition", "مول الفواكه", FruitAppleIcon),
        IconOption("set_meal", "الكزار والدجاج", ButcherCleaverIcon),
        IconOption("egg", "مول البيض", EggVendorIcon),
        IconOption("fish", "مول الحوت / السمك", Icons.Outlined.SetMeal),
        IconOption("cleaning", "دروݣري ونظافة", Icons.Outlined.CleaningServices),
        IconOption("handyman", "مول الدوزان", Icons.Outlined.Build),
        IconOption("kitchen", "مول المواعن", CookingPotIcon),
        IconOption("checkroom", "مول الحوايج", Icons.Outlined.Checkroom),
        IconOption("bakery", "مخبزة / خبز", Icons.Outlined.BakeryDining),
        IconOption("shopping_bag", "بائع عام / سوق", Icons.Outlined.ShoppingBag)
    )

    fun inferIconKeyFromName(vendorName: String, currentKey: String = "store"): String {
        val clean = vendorName.trim()
        if (clean.isEmpty()) return currentKey
        // If user explicitly picked a non-default iconKey, respect it unless it's "store" for a specialized vendor
        if (currentKey != "store") return currentKey
        return when {
            clean.contains("بيض") -> "egg"
            clean.contains("حوت") || clean.contains("سمك") || clean.contains("سردين") -> "fish"
            clean.contains("فواكه") || clean.contains("ديسير") || clean.contains("فاكه") -> "nutrition"
            clean.contains("خضر") || clean.contains("ربيع") || clean.contains("نعناع") -> "eco"
            clean.contains("كزار") || clean.contains("جزار") || clean.contains("دجاج") || clean.contains("لحم") -> "set_meal"
            clean.contains("درو") || clean.contains("نظاف") -> "cleaning"
            clean.contains("دوزان") || clean.contains("حداد") -> "handyman"
            clean.contains("مواعن") || clean.contains("أواني") || clean.contains("اواني") -> "kitchen"
            clean.contains("حوايج") || clean.contains("ملابس") -> "checkroom"
            clean.contains("خبز") || clean.contains("مخبز") || clean.contains("فران") -> "bakery"
            else -> currentKey
        }
    }

    fun getIcon(key: String, vendorName: String = ""): ImageVector {
        val effectiveKey = inferIconKeyFromName(vendorName, key)
        return availableIcons.firstOrNull { it.key == effectiveKey }?.icon
            ?: availableIcons.firstOrNull { it.key == key }?.icon
            ?: Icons.Outlined.Storefront
    }
}
