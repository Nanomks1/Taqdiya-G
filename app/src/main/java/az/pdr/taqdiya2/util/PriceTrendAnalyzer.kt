package az.pdr.taqdiya2.util

import az.pdr.taqdiya2.data.local.PriceHistoryEntity
import kotlin.math.abs
import kotlin.math.roundToInt

enum class PriceTrendStatus {
    CHEAPER,    // أخضر: الثمن منخفض عن المعتاد (يشجعني على شراء المزيد)
    STABLE,     // أصفر: ثمن عادي ومستقر
    EXPENSIVE,  // أحمر: الثمن مرتفع عن المعتاد (لشراء قدر الحاجة فقط)
    NO_HISTORY  // لا يوجد سجل سابق بعد
}

data class PriceEvaluation(
    val status: PriceTrendStatus,
    val currentUnitPriceDirhams: Double,
    val referenceUsualDirhams: Double,
    val lastPriceDirhams: Double,
    val minPriceDirhams: Double,
    val maxPriceDirhams: Double,
    val differenceDirhams: Double,
    val percentageChange: Int,
    val historyCount: Int,
    val badgeLabel: String,
    val headlineText: String,
    val recommendationText: String
)

object PriceTrendAnalyzer {

    fun evaluateItemPrice(
        currentUnitPriceDirhams: Double,
        purchaseMode: String,
        itemHistory: List<PriceHistoryEntity>,
        excludeShoppingItemId: Long? = null,
        fallbackLastPriceDirhams: Double = 0.0,
        unitSuffixOverride: String? = null
    ): PriceEvaluation {
        val validHistory = itemHistory
            .filter { record ->
                record.unitPriceDirhams > 0.0 &&
                    (excludeShoppingItemId == null || record.shoppingItemId != excludeShoppingItemId)
            }
            .sortedByDescending { it.recordedAt }

        val comparableHistory = validHistory.filter { it.purchaseMode == purchaseMode }

        val (effectivePrices, effectiveMode) = when {
            comparableHistory.isNotEmpty() -> {
                comparableHistory.map { it.unitPriceDirhams } to purchaseMode
            }
            validHistory.isNotEmpty() && currentUnitPriceDirhams <= 0.0 -> {
                val latestMode = validHistory.first().purchaseMode
                validHistory.filter { it.purchaseMode == latestMode }.map { it.unitPriceDirhams } to latestMode
            }
            validHistory.isEmpty() && fallbackLastPriceDirhams > 0.0 -> {
                listOf(fallbackLastPriceDirhams) to purchaseMode
            }
            else -> {
                emptyList<Double>() to purchaseMode
            }
        }

        if (effectivePrices.isEmpty()) {
            return PriceEvaluation(
                status = PriceTrendStatus.NO_HISTORY,
                currentUnitPriceDirhams = currentUnitPriceDirhams,
                referenceUsualDirhams = 0.0,
                lastPriceDirhams = 0.0,
                minPriceDirhams = 0.0,
                maxPriceDirhams = 0.0,
                differenceDirhams = 0.0,
                percentageChange = 0,
                historyCount = itemHistory.size,
                badgeLabel = "سعر جديد",
                headlineText = "لا يوجد ثمن سابق مسجل لهذه السلعة",
                recommendationText = "عند وضع الغرض في القفة سيُحفظ هذا الثمن كمرجع للمرات القادمة."
            )
        }

        val lastPrice = effectivePrices.first()
        val avgPrice = CurrencyFormatter.roundMoney(effectivePrices.average())
        val minPrice = effectivePrices.minOrNull() ?: lastPrice
        val maxPrice = effectivePrices.maxOrNull() ?: lastPrice
        val unitSuffix = when {
            effectiveMode == "FIXED" -> ""
            !unitSuffixOverride.isNullOrBlank() && effectiveMode == purchaseMode -> unitSuffixOverride
            effectiveMode == "WEIGHT" -> "/كغ"
            effectiveMode == "COUNT" -> "/حبة"
            else -> ""
        }

        if (currentUnitPriceDirhams <= 0.0) {
            return PriceEvaluation(
                status = PriceTrendStatus.STABLE,
                currentUnitPriceDirhams = 0.0,
                referenceUsualDirhams = avgPrice,
                lastPriceDirhams = lastPrice,
                minPriceDirhams = minPrice,
                maxPriceDirhams = maxPrice,
                differenceDirhams = 0.0,
                percentageChange = 0,
                historyCount = effectivePrices.size,
                badgeLabel = "المعتاد: ${CurrencyFormatter.formatDirhams(avgPrice)}$unitSuffix",
                headlineText = "آخر ثمن: ${CurrencyFormatter.formatDirhams(lastPrice)} (${CurrencyFormatter.formatRials(lastPrice)})",
                recommendationText = "المعدل المعتاد: ${CurrencyFormatter.formatDirhams(avgPrice)}$unitSuffix • أدنى ثمن: ${CurrencyFormatter.formatDirhams(minPrice)}"
            )
        }

        val diff = CurrencyFormatter.roundMoney(currentUnitPriceDirhams - avgPrice)
        val ratio = if (avgPrice > 0.0) (diff / avgPrice) else 0.0
        val pct = (ratio * 100.0).roundToInt()

        // Thresholds: at least 5% change AND at least 0.30 DH difference (for eggs/small items, 0.10 DH if avgPrice < 3 DH)
        val minAbsDiffThreshold = if (avgPrice <= 3.0) 0.10 else 0.40

        val status = when {
            ratio <= -0.05 && abs(diff) >= minAbsDiffThreshold -> PriceTrendStatus.CHEAPER
            ratio >= 0.05 && abs(diff) >= minAbsDiffThreshold -> PriceTrendStatus.EXPENSIVE
            else -> PriceTrendStatus.STABLE
        }

        val absDiffFormatted = CurrencyFormatter.formatDirhams(abs(diff))
        val absDiffRials = CurrencyFormatter.formatRials(abs(diff))

        return when (status) {
            PriceTrendStatus.CHEAPER -> PriceEvaluation(
                status = PriceTrendStatus.CHEAPER,
                currentUnitPriceDirhams = currentUnitPriceDirhams,
                referenceUsualDirhams = avgPrice,
                lastPriceDirhams = lastPrice,
                minPriceDirhams = minPrice,
                maxPriceDirhams = maxPrice,
                differenceDirhams = diff,
                percentageChange = pct,
                historyCount = effectivePrices.size,
                badgeLabel = "رخاء (${pct}%)",
                headlineText = "ثمن منخفض عن المعتاد بـ $absDiffFormatted ($absDiffRials)",
                recommendationText = "فرصة مشجعة لشراء المزيد! المعتاد: ${CurrencyFormatter.formatDirhams(avgPrice)}$unitSuffix (آخر مرة: ${CurrencyFormatter.formatDirhams(lastPrice)})"
            )

            PriceTrendStatus.EXPENSIVE -> PriceEvaluation(
                status = PriceTrendStatus.EXPENSIVE,
                currentUnitPriceDirhams = currentUnitPriceDirhams,
                referenceUsualDirhams = avgPrice,
                lastPriceDirhams = lastPrice,
                minPriceDirhams = minPrice,
                maxPriceDirhams = maxPrice,
                differenceDirhams = diff,
                percentageChange = pct,
                historyCount = effectivePrices.size,
                badgeLabel = "غلاء (+${pct}%)",
                headlineText = "ثمن مرتفع عن المعتاد بـ $absDiffFormatted ($absDiffRials)",
                recommendationText = "اشترِ قدر الحاجة فقط اليوم. المعتاد: ${CurrencyFormatter.formatDirhams(avgPrice)}$unitSuffix (آخر مرة: ${CurrencyFormatter.formatDirhams(lastPrice)})"
            )

            PriceTrendStatus.STABLE, PriceTrendStatus.NO_HISTORY -> PriceEvaluation(
                status = PriceTrendStatus.STABLE,
                currentUnitPriceDirhams = currentUnitPriceDirhams,
                referenceUsualDirhams = avgPrice,
                lastPriceDirhams = lastPrice,
                minPriceDirhams = minPrice,
                maxPriceDirhams = maxPrice,
                differenceDirhams = diff,
                percentageChange = pct,
                historyCount = effectivePrices.size,
                badgeLabel = "ثمن مستقر",
                headlineText = "ثمن عادي ومستقر قريب من المعتاد",
                recommendationText = "المعتاد: ${CurrencyFormatter.formatDirhams(avgPrice)}$unitSuffix (${CurrencyFormatter.formatRials(avgPrice)}) • آخر مرة: ${CurrencyFormatter.formatDirhams(lastPrice)}"
            )
        }
    }
}
