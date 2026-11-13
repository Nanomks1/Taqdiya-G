package az.pdr.taqdiya2.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.LocalDining
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.ui.graphics.vector.ImageVector

object VendorIconMapper {
    data class IconOption(
        val key: String,
        val label: String,
        val icon: ImageVector
    )

    val availableIcons = listOf(
        IconOption("store", "حانوت / عطار", Icons.Outlined.Storefront),
        IconOption("eco", "خضر ونعناع", Icons.Outlined.Eco),
        IconOption("nutrition", "فواكه وديسير", Icons.Outlined.LocalDining),
        IconOption("set_meal", "لحوم ودجاج وبيض", Icons.Outlined.Restaurant),
        IconOption("cleaning", "دروݣري ونظافة", Icons.Outlined.CleaningServices),
        IconOption("handyman", "دوزان وأدوات", Icons.Outlined.Build),
        IconOption("kitchen", "مواعن ومطبخ", Icons.Outlined.Kitchen),
        IconOption("checkroom", "حوايج وملابس", Icons.Outlined.Checkroom),
        IconOption("shopping_bag", "بائع عام / سوق", Icons.Outlined.ShoppingBag)
    )

    fun getIcon(key: String): ImageVector {
        return availableIcons.firstOrNull { it.key == key }?.icon ?: Icons.Outlined.Storefront
    }
}
