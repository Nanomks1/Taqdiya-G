package az.pdr.taqdiya2.util

import az.pdr.taqdiya2.data.local.ShoppingItemEntity
import az.pdr.taqdiya2.data.local.VendorEntity
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object CurrencyFormatter {
    const val RIALS_PER_DIRHAM = 20.0

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

    fun formatDirhams(dirhams: Double): String {
        return "${formatAmountPlain(dirhams)} درهم"
    }

    fun formatRials(dirhams: Double): String {
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
            .lowercase(Locale.ROOT)
    }

    fun buildShareText(
        vendors: List<VendorEntity>,
        items: List<ShoppingItemEntity>,
        selectedVendorId: Long? = null
    ): String {
        val activeItems = items.filter { !it.isDeleted && !it.isPurchased }
        val filteredItems = if (selectedVendorId != null) {
            activeItems.filter { it.vendorId == selectedVendorId }
        } else {
            activeItems
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
