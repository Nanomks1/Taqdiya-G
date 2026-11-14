package az.pdr.taqdiya2.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import az.pdr.taqdiya2.data.local.ShoppingItemEntity
import az.pdr.taqdiya2.data.local.VendorEntity
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object CurrencyFormatter {
    const val RIALS_PER_DIRHAM = 20.0

    var showRialState by mutableStateOf(true)
    var privacyMaskState by mutableStateOf(false)
    var privacyMaskPocketOnlyState by mutableStateOf(false)

    fun dirhamsToRials(dirhams: Double): Long {
        return (dirhams * RIALS_PER_DIRHAM).roundToLong()
    }

    fun rialsToDirhams(rials: Double): Double {
        return ((rials / RIALS_PER_DIRHAM) * 100.0).roundToLong() / 100.0
    }

    fun roundMoney(amount: Double): Double {
        return ((amount * 100.0).roundToLong()) / 100.0
    }

    fun formatAmountPlain(amount: Double): String {
        val rounded = roundMoney(amount)
        return if (abs(rounded - rounded.roundToLong()) < 0.001) {
            String.format(Locale.US, "%d", rounded.roundToLong())
        } else {
            String.format(Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
        }
    }

    fun formatPlainNumber(amount: Double): String {
        return formatAmountPlain(amount)
    }

    fun formatDirhams(dirhams: Double, allowMask: Boolean = true): String {
        val shouldMask = allowMask && privacyMaskState && !privacyMaskPocketOnlyState
        if (shouldMask) {
            return "*** درهم"
        }
        return "${formatAmountPlain(dirhams)} درهم"
    }

    fun formatRials(dirhams: Double, allowMask: Boolean = true): String {
        val shouldMask = allowMask && privacyMaskState && !privacyMaskPocketOnlyState
        if (!showRialState) {
            return if (shouldMask) "*** د.م" else "${formatAmountPlain(dirhams)} د.م"
        }
        if (shouldMask) {
            return "*** ريال"
        }
        val rials = dirhamsToRials(dirhams)
        return "$rials ريال"
    }

    fun formatPocketDirhams(dirhams: Double, allowMask: Boolean = true): String {
        if (allowMask && privacyMaskState) {
            return "*** درهم"
        }
        return "${formatAmountPlain(dirhams)} درهم"
    }

    fun formatPocketRials(dirhams: Double, allowMask: Boolean = true): String {
        if (!showRialState) {
            return if (allowMask && privacyMaskState) "*** د.م" else "${formatAmountPlain(dirhams)} د.م"
        }
        if (allowMask && privacyMaskState) {
            return "*** ريال"
        }
        val rials = dirhamsToRials(dirhams)
        return "$rials ريال"
    }


    fun formatMoroccanWeightLabel(kg: Double): String {
        return when {
            abs(kg - 1.0) < 0.001 -> "كيلو (1 كغ)"
            abs(kg - 0.5) < 0.001 -> "نص كيلو (0.5 كغ)"
            abs(kg - 0.75) < 0.001 -> "3 رابعات (0.75 كغ)"
            abs(kg - 0.25) < 0.001 -> "رابعة (0.25 كغ)"
            abs(kg - 0.125) < 0.001 -> "نص رابعة (0.125 كغ)"
            abs(kg - 2.0) < 0.001 -> "2 كيلو"
            kg < 1.0 -> "${(kg * 1000).roundToLong()} غرام"
            else -> "${formatAmountPlain(kg)} كغ"
        }
    }

    fun formatPurchaseDetails(item: ShoppingItemEntity): String {
        return when (item.purchaseMode) {
            "WEIGHT" -> {
                val weightText = formatMoroccanWeightLabel(item.quantity)
                val baseText = "$weightText × ${formatDirhams(item.unitPriceDirhams)}/كغ"
                if (item.cleaningFeeDirhams > 0.0) {
                    "$baseText (+ ${formatDirhams(item.cleaningFeeDirhams)} تنقية)"
                } else {
                    baseText
                }
            }
            "FIXED" -> {
                "شراء بمبلغ مقطوع مباشر"
            }
            "COUNT" -> {
                val countInt = item.quantity.roundToLong()
                val countLabel = when (countInt) {
                    30L -> "بلاطو (30 حبة)"
                    15L -> "15 حبة / نص بلاطو"
                    10L -> "10 حبات"
                    1L -> "قطعة واحدة"
                    2L -> "قطعتان (2)"
                    else -> "$countInt قطع/حبات"
                }
                "$countLabel × ${formatDirhams(item.unitPriceDirhams)}"
            }
            else -> ""
        }
    }

    fun normalizeArabicName(input: String): String {
        return input.trim()
            .replace(Regex("\\s+"), " ")
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ة', 'ه')
            .replace('ى', 'ي')
            .replace('ڤ', 'ف')
            .lowercase(Locale.ROOT)
    }

    fun parseCustomUnits(csv: String): List<String> {
        if (csv.isBlank()) return emptyList()
        return csv.split("|")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    fun encodeCustomUnits(units: List<String>): String {
        return units.map { it.trim().replace("|", "") }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString("|")
    }

    /**
     * Returns smart default units tailored to the specific item name and vendor specialty,
     * preceded by any user-defined custom units saved for this item.
     */
    fun getSmartUnitsForItem(
        itemName: String,
        vendor: VendorEntity?,
        customUnitsCsv: String = ""
    ): List<String> {
        val custom = parseCustomUnits(customUnitsCsv)
        val norm = normalizeArabicName(itemName)
        val vendorNorm = normalizeArabicName(vendor?.name.orEmpty() + " " + vendor?.specialty.orEmpty())
        val iconKey = vendor?.iconKey.orEmpty()

        val smartDefaults: List<String> = when {
            // 1. Eggs
            norm.contains("بيض") -> listOf(
                "بلاطو", "نص بلاطو", "10 بيضات", "5 بيضات", "15 بيضة", "بلدي"
            )
            // 2. Cleaning & Liquids (Javel, soap, oil, milk, water, detergent)
            norm.contains("جافيل") || norm.contains("زيت") || norm.contains("حليب") ||
                norm.contains("ماء") || norm.contains("خل") || norm.contains("عصير") ||
                norm.contains("سائل") -> listOf(
                "قرعة", "لتر", "2 لتر", "5 لتر", "قرعة صغيرة", "قرعة كبيرة", "علبة"
            )
            // 3. Tea, Coffee, Packaged Grocery, Detergent Powder, Cheese, Butter
            norm.contains("اتاي") || norm.contains("قهوه") || norm.contains("طون") ||
                norm.contains("فرماج") || norm.contains("جبن") || norm.contains("زبده") ||
                norm.contains("ياغورت") || norm.contains("مسحوق") || norm.contains("صابون") ||
                norm.contains("خميره") || norm.contains("بسكويت") || norm.contains("معجون") -> listOf(
                "علبة", "علبة صغيرة", "علبة كبيرة", "باكيه", "خنيشة", "حبة", "2 حبات", "رابعة"
            )
            // 4. Herbs & Greens (Rbi3, Na3na3, Karafs, Khoss)
            norm.contains("ربيع") || norm.contains("نعناع") || norm.contains("معدنوس") ||
                norm.contains("قزبور") || norm.contains("كرافس") || norm.contains("خس") ||
                norm.contains("شيبه") -> listOf(
                "ربطة", "2 ربطات", "3 دراهم", "5 دراهم", "طري"
            )
            // 5. Spices (Kamoun, Ibzar, Tahmira, Kharqoum, Skinjbir)
            norm.contains("كمون") || norm.contains("ابزار") || norm.contains("تحميره") ||
                norm.contains("خرقوم") || norm.contains("سكنجبير") || norm.contains("قرفه") ||
                norm.contains("راس الحانوت") || norm.contains("عطريه") -> listOf(
                "5 دراهم", "10 دراهم", "رابعة", "نص رابعة", "مطحون", "حبوب"
            )
            // 6. Cleaning / Droguerie Vendor
            iconKey == "cleaning" || vendorNorm.contains("درو") || vendorNorm.contains("نظافه") -> listOf(
                "قرعة", "علبة", "باكيه", "لتر", "2 لتر", "خنيشة", "حبة", "2 حبات"
            )
            // 7. Hardware / Dozan / Mwa3en / Clothes Vendor
            iconKey == "handyman" || iconKey == "kitchen" || iconKey == "checkroom" ||
                vendorNorm.contains("دوزان") || vendorNorm.contains("مواعن") || vendorNorm.contains("حوايج") -> listOf(
                "حبة", "2 حبات", "علبة", "باكية", "طقم", "حجم متوسط", "حجم كبير"
            )
            // 8. Butcher / Chicken / Meat
            iconKey == "set_meal" || vendorNorm.contains("كزار") || vendorNorm.contains("دجاج") ||
                norm.contains("دجاج") || norm.contains("لحم") || norm.contains("كفته") -> listOf(
                "كيلو", "نص كيلو", "رابعة", "2 كيلو", "دجاجة متوسطة", "بلاطو", "مطحون"
            )
            // 9. Attar / Grocery (General: Legumes, Flour, Sugar + Packaged goods)
            iconKey == "store" || vendorNorm.contains("عطار") || vendorNorm.contains("حانوت") -> listOf(
                "علبة", "قرعة", "باكيه", "كيلو", "نص كيلو", "رابعة", "2 كيلو", "خنيشة", "5 دراهم"
            )
            // 10. Vegetables & Fruits (Default Weight & Freshness notes)
            else -> listOf(
                "كيلو", "نص كيلو", "2 كيلو", "رابعة", "3 رابعات", "3 كيلو", "ربطة", "طايبة", "حجم متوسط"
            )
        }

        return (custom + smartDefaults).distinct()
    }

    fun buildShareText(
        vendors: List<VendorEntity>,
        items: List<ShoppingItemEntity>,
        selectedVendorId: Long? = null,
        selectedItemIds: Set<Long>? = null
    ): String {
        val activeItems = items.filter { !it.isDeleted && !it.isPurchased }
        val vendorFiltered = if (selectedVendorId != null) {
            activeItems.filter { it.vendorId == selectedVendorId }
        } else {
            activeItems
        }
        val filteredItems = if (selectedItemIds != null) {
            vendorFiltered.filter { selectedItemIds.contains(it.id) }
        } else {
            vendorFiltered
        }

        if (filteredItems.isEmpty()) {
            return "قائمة التقدية فارغة حالياً."
        }

        val vendorMap = vendors.associateBy { it.id }
        val sb = StringBuilder()

        if (selectedVendorId != null) {
            val vendorName = vendorMap[selectedVendorId]?.name ?: "البائع"
            sb.append("قائمة التقدية - ").append(vendorName).append(":\n")
            sb.append("--------------------\n")
            filteredItems.forEachIndexed { index, item ->
                sb.append("${index + 1}. ${item.name}")
                if (item.note.isNotBlank()) {
                    sb.append(" (${item.note.trim()})")
                }
                sb.append("\n")
            }
        } else {
            sb.append("قائمة النواقص للتقدية:\n")
            sb.append("====================\n")
            val grouped = filteredItems.groupBy { it.vendorId }
            val orderedVendors = vendors.filter { grouped.containsKey(it.id) }
            orderedVendors.forEachIndexed { vIndex, vendor ->
                if (vIndex > 0) sb.append("\n")
                sb.append("[ ").append(vendor.name).append(" ]\n")
                grouped[vendor.id].orEmpty().forEach { item ->
                    sb.append("- ").append(item.name)
                    if (item.note.isNotBlank()) {
                        sb.append(" (${item.note.trim()})")
                    }
                    sb.append("\n")
                }
            }
            val otherItems = filteredItems.filter { item -> orderedVendors.none { it.id == item.vendorId } }
            if (otherItems.isNotEmpty()) {
                if (orderedVendors.isNotEmpty()) sb.append("\n")
                sb.append("[ متفرقات ]\n")
                otherItems.forEach { item ->
                    sb.append("- ").append(item.name)
                    if (item.note.isNotBlank()) {
                        sb.append(" (${item.note.trim()})")
                    }
                    sb.append("\n")
                }
            }
        }
        sb.append("--------------------\n")
        sb.append("المجموع: ${filteredItems.size} أغراض")
        return sb.toString()
    }
}
