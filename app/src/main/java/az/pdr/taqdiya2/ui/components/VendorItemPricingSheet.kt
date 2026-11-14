package az.pdr.taqdiya2.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import az.pdr.taqdiya2.data.local.PriceHistoryEntity
import az.pdr.taqdiya2.data.local.ShoppingItemEntity
import az.pdr.taqdiya2.util.CurrencyFormatter
import az.pdr.taqdiya2.util.PriceTrendAnalyzer
import kotlin.math.abs
import kotlin.math.roundToLong

private enum class ActiveNumpadField {
    PRICE,
    FREE_WEIGHT,
    COUNT_QTY
}

internal fun shouldShowCleaningFee(vendorName: String, itemName: String): Boolean {
    val combined = "${CurrencyFormatter.normalizeArabicName(vendorName)} ${CurrencyFormatter.normalizeArabicName(itemName)}"
    val fishAndPoultryKeywords = listOf(
        "سمك", "حوت", "سردين", "شطون", "ميرنا", "صول", "كروفيت", "كلمار", "قمرون", "فرخ", "بواجو", "شرغو", "كابايلا", "درعي", "باجو",
        "دجاج", "بيبي", "ديك رومي", "لداند", "حمام", "سمان", "ارنب", "قنيه", "قنية"
    )
    return fishAndPoultryKeywords.any { keyword -> combined.contains(keyword) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VendorItemPricingSheet(
    item: ShoppingItemEntity,
    vendorName: String,
    itemPriceHistory: List<PriceHistoryEntity>,
    fallbackLastPriceDirhams: Double,
    onOpenPriceHistory: () -> Unit,
    onConfirmPurchase: (
        purchaseMode: String,
        unitPriceDirhams: Double,
        quantity: Double,
        cleaningFeeDirhams: Double,
        totalDirhams: Double
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current

    // Determine smart initial mode and quantity from item state or note
    val initialMode = remember(item) {
        when {
            item.isPurchased -> item.purchaseMode
            item.name.contains("بيض") || item.note.contains("بلاطو") || item.note.contains("بيض") -> "COUNT"
            item.name.contains("ربيع") || item.name.contains("نعناع") || item.name.contains("كمون") -> "FIXED"
            else -> item.purchaseMode.ifBlank { "WEIGHT" }
        }
    }

    val initialQuantity = remember(item, initialMode) {
        if (item.isPurchased && item.quantity > 0.0) {
            item.quantity
        } else if (initialMode == "COUNT") {
            when {
                item.note.contains("30") || item.note.contains("بلاطو") -> 30.0
                item.note.contains("15") -> 15.0
                item.note.contains("10") -> 10.0
                else -> 1.0
            }
        } else {
            when {
                item.note.contains("نص رابعة") -> 0.125
                item.note.contains("3 رابعات") -> 0.75
                item.note.contains("رابعة") -> 0.25
                item.note.contains("نص كيلو") -> 0.5
                item.note.contains("2 كيلو") -> 2.0
                else -> 1.0
            }
        }
    }

    var selectedMode by remember { mutableStateOf(initialMode) }
    var inputInRials by remember { mutableStateOf(false) }

    val initialPriceDirhams = remember(item) {
        if (item.isPurchased && item.totalDirhams > 0.0) {
            if (item.purchaseMode == "FIXED") item.totalDirhams else item.unitPriceDirhams
        } else {
            item.unitPriceDirhams
        }
    }

    var priceText by remember {
        mutableStateOf(
            if (initialPriceDirhams > 0.0) CurrencyFormatter.formatAmountPlain(initialPriceDirhams) else "0"
        )
    }
    var replacePriceOnNextTap by remember { mutableStateOf(true) }

    var weightKg by remember { mutableDoubleStateOf(initialQuantity) }
    var freeWeightInGrams by remember { mutableStateOf(false) }
    var freeWeightText by remember {
        mutableStateOf(CurrencyFormatter.formatAmountPlain(initialQuantity))
    }
    var replaceWeightOnNextTap by remember { mutableStateOf(true) }

    var countText by remember {
        mutableStateOf(initialQuantity.roundToLong().coerceAtLeast(1L).toString())
    }
    var replaceCountOnNextTap by remember { mutableStateOf(true) }

    val canHaveCleaningFee = remember(vendorName, item.name) {
        shouldShowCleaningFee(vendorName, item.name)
    }

    var cleaningFeeDirhams by remember(canHaveCleaningFee) {
        mutableDoubleStateOf(if (item.isPurchased && canHaveCleaningFee) item.cleaningFeeDirhams else 0.0)
    }

    var activeNumpadField by remember { mutableStateOf(ActiveNumpadField.PRICE) }

    // Compute live numbers
    val rawEnteredPrice = priceText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val unitOrFixedPriceDirhams = if (inputInRials) {
        CurrencyFormatter.rialsToDirhams(rawEnteredPrice)
    } else {
        rawEnteredPrice
    }

    val effectiveQuantity = when (selectedMode) {
        "WEIGHT" -> weightKg.coerceAtLeast(0.0)
        "COUNT" -> (countText.toDoubleOrNull() ?: 1.0).coerceAtLeast(1.0)
        else -> 1.0
    }

    val effectiveCleaningFee = if (selectedMode == "WEIGHT" && canHaveCleaningFee) cleaningFeeDirhams else 0.0

    val calculatedTotalDirhams = when (selectedMode) {
        "WEIGHT" -> CurrencyFormatter.roundMoney((unitOrFixedPriceDirhams * effectiveQuantity) + effectiveCleaningFee)
        "FIXED" -> CurrencyFormatter.roundMoney(unitOrFixedPriceDirhams)
        "COUNT" -> CurrencyFormatter.roundMoney(unitOrFixedPriceDirhams * effectiveQuantity)
        else -> 0.0
    }

    fun onNumpadKey(key: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        when (activeNumpadField) {
            ActiveNumpadField.PRICE -> {
                if (key == "BACK") {
                    priceText = if (priceText.length <= 1) "0" else priceText.dropLast(1)
                    replacePriceOnNextTap = false
                } else if (key == ".") {
                    if (replacePriceOnNextTap) {
                        priceText = "0."
                        replacePriceOnNextTap = false
                    } else if (!priceText.contains(".")) {
                        priceText += "."
                    }
                } else {
                    if (replacePriceOnNextTap || priceText == "0") {
                        priceText = key
                        replacePriceOnNextTap = false
                    } else if (priceText.length < 7) {
                        priceText += key
                    }
                }
            }

            ActiveNumpadField.FREE_WEIGHT -> {
                if (key == "BACK") {
                    freeWeightText = if (freeWeightText.length <= 1) "0" else freeWeightText.dropLast(1)
                    replaceWeightOnNextTap = false
                } else if (key == ".") {
                    if (!freeWeightInGrams) {
                        if (replaceWeightOnNextTap) {
                            freeWeightText = "0."
                            replaceWeightOnNextTap = false
                        } else if (!freeWeightText.contains(".")) {
                            freeWeightText += "."
                        }
                    }
                } else {
                    if (replaceWeightOnNextTap || freeWeightText == "0") {
                        freeWeightText = key
                        replaceWeightOnNextTap = false
                    } else if (freeWeightText.length < 6) {
                        freeWeightText += key
                    }
                }
                val parsed = freeWeightText.replace(',', '.').toDoubleOrNull() ?: 0.0
                weightKg = if (freeWeightInGrams) parsed / 1000.0 else parsed
            }

            ActiveNumpadField.COUNT_QTY -> {
                if (key == "BACK") {
                    countText = if (countText.length <= 1) "1" else countText.dropLast(1)
                    replaceCountOnNextTap = false
                } else if (key != ".") {
                    if (replaceCountOnNextTap || countText == "0") {
                        countText = key
                        replaceCountOnNextTap = false
                    } else if (countText.length < 4) {
                        countText += key
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 1. Header Row: Item Name + Currency Toggle (Dirham <-> Rial) + Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    val subtitle = if (item.note.isNotBlank()) {
                        "$vendorName • ملاحظة: ${item.note}"
                    } else {
                        vendorName
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Dirham <-> Rial Quick Toggle Pill
                Surface(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val currentVal = priceText.replace(',', '.').toDoubleOrNull() ?: 0.0
                            if (!inputInRials) {
                                val rials = CurrencyFormatter.dirhamsToRials(currentVal)
                                priceText = rials.toString()
                                inputInRials = true
                            } else {
                                val dh = CurrencyFormatter.rialsToDirhams(currentVal)
                                priceText = CurrencyFormatter.formatAmountPlain(dh)
                                inputInRials = false
                            }
                            replacePriceOnNextTap = true
                        }
                        .testTag("pricing_currency_toggle"),
                    color = if (inputInRials) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SwapHoriz,
                            contentDescription = "تبديل العملة",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (inputInRials) "إدخال بالريال" else "إدخال بالدرهم",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("close_pricing_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "إغلاق",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Three Large Purchase Mode Tabs (WEIGHT | FIXED | COUNT)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val modes = listOf(
                    "WEIGHT" to "1. بالوزن",
                    "FIXED" to "2. مبلغ مقطوع",
                    "COUNT" to "3. بالعدد / البيض"
                )
                modes.forEach { (modeKey, label) ->
                    val isSelected = selectedMode == modeKey
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedMode = modeKey
                                activeNumpadField = ActiveNumpadField.PRICE
                                replacePriceOnNextTap = true
                            }
                            .testTag("mode_tab_$modeKey"),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Mode-Specific Interactive Controls
            when (selectedMode) {
                "WEIGHT" -> {
                    // Two interactive focus boxes: [ثمن الكيلو اليوم] & [الوزن]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isPriceFocused = activeNumpadField == ActiveNumpadField.PRICE
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    activeNumpadField = ActiveNumpadField.PRICE
                                    replacePriceOnNextTap = true
                                }
                                .testTag("focus_price_per_kilo_box"),
                            color = if (isPriceFocused) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = if (isPriceFocused) 2.dp else 1.dp,
                                color = if (isPriceFocused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (inputInRials) "ثمن الكيلو (بالريال)" else "ثمن الكيلو اليوم (درهم)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (inputInRials) "$priceText ريال" else "$priceText درهم",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (inputInRials || CurrencyFormatter.showRialState) {
                                    Text(
                                        text = if (inputInRials) {
                                            "= ${CurrencyFormatter.formatDirhams(unitOrFixedPriceDirhams)}"
                                        } else {
                                            "= ${CurrencyFormatter.formatRials(unitOrFixedPriceDirhams)}"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = "اضغط لتعديل الثمن",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        val isWeightFocused = activeNumpadField == ActiveNumpadField.FREE_WEIGHT
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    activeNumpadField = ActiveNumpadField.FREE_WEIGHT
                                    replaceWeightOnNextTap = true
                                }
                                .testTag("focus_free_weight_box"),
                            color = if (isWeightFocused) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = if (isWeightFocused) 2.dp else 1.dp,
                                color = if (isWeightFocused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "الوزن المختار (اضغط لوزن حر)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.formatMoroccanWeightLabel(weightKg),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (freeWeightInGrams) "إدخال حر بالغرام" else "إدخال حر بالكيلو",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.clickable {
                                        freeWeightInGrams = !freeWeightInGrams
                                        activeNumpadField = ActiveNumpadField.FREE_WEIGHT
                                        freeWeightText = if (freeWeightInGrams) {
                                            (weightKg * 1000).roundToLong().toString()
                                        } else {
                                            CurrencyFormatter.formatAmountPlain(weightKg)
                                        }
                                        replaceWeightOnNextTap = true
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Moroccan Preset Weight Buttons (1-Tap!)
                    val moroccanWeights = listOf(
                        1.0 to "كيلو (1)",
                        0.5 to "نص كيلو (0.5)",
                        0.75 to "3 رابعات",
                        0.25 to "رابعة (0.25)",
                        0.125 to "نص رابعة",
                        2.0 to "2 كيلو"
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 3
                    ) {
                        moroccanWeights.forEach { (kgVal, label) ->
                            val isSelected = abs(weightKg - kgVal) < 0.001
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        weightKg = kgVal
                                        freeWeightInGrams = false
                                        freeWeightText = CurrencyFormatter.formatAmountPlain(kgVal)
                                    }
                                    .testTag("weight_preset_$kgVal"),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onTertiary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    val showCleaningFeeRow = remember(vendorName, item.name) {
                        shouldShowCleaningFee(vendorName = vendorName, itemName = item.name)
                    }

                    if (showCleaningFeeRow) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Optional Cleaning Fee Row (ONLY for Chicken / Poultry / Fish / Seafood)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ثمن التنقية:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val cleaningOptions = listOf(0.0, 2.0, 3.0, 4.0, 5.0)
                            cleaningOptions.forEach { fee ->
                                val isFeeSelected = abs(cleaningFeeDirhams - fee) < 0.01
                                Surface(
                                    modifier = Modifier
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            cleaningFeeDirhams = fee
                                        },
                                    color = if (isFeeSelected) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (fee == 0.0) "بدون تنقية" else "+${fee.toInt()} درهم",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isFeeSelected) {
                                                MaterialTheme.colorScheme.onSecondary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                "FIXED" -> {
                    // Direct Fixed Amount Mode (e.g. 5 DH Cumin, 3 DH Mint/Coriander)
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (inputInRials) "المبلغ المقطوع بالريال" else "المبلغ المقطوع مباشرة بالدرهم",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = if (inputInRials) "$priceText ريال" else "$priceText درهم",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val fixedPresetsDh = listOf(1, 2, 3, 5, 10, 15, 20, 30)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 4
                    ) {
                        fixedPresetsDh.forEach { dhVal ->
                            val isSelected = abs(unitOrFixedPriceDirhams - dhVal.toDouble()) < 0.01
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        priceText = if (inputInRials) (dhVal * 20).toString() else dhVal.toString()
                                        replacePriceOnNextTap = true
                                    }
                                    .testTag("fixed_preset_$dhVal"),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$dhVal درهم",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onTertiary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                "COUNT" -> {
                    // Unit Price x Count (with Egg Presets: 10, 15, 30 Tray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isPriceFocused = activeNumpadField == ActiveNumpadField.PRICE
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    activeNumpadField = ActiveNumpadField.PRICE
                                    replacePriceOnNextTap = true
                                },
                            color = if (isPriceFocused) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = if (isPriceFocused) 2.dp else 1.dp,
                                color = if (isPriceFocused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (inputInRials) "ثمن الحبة (بالريال)" else "ثمن الحبة/القطعة (درهم)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (inputInRials) "$priceText ريال" else "$priceText درهم",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        val isCountFocused = activeNumpadField == ActiveNumpadField.COUNT_QTY
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    activeNumpadField = ActiveNumpadField.COUNT_QTY
                                    replaceCountOnNextTap = true
                                },
                            color = if (isCountFocused) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = if (isCountFocused) 2.dp else 1.dp,
                                color = if (isCountFocused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        val cur = (countText.toIntOrNull() ?: 1).coerceAtLeast(1)
                                        countText = (cur + 1).toString()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Outlined.Add, contentDescription = "زيادة")
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "العدد",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = countText,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val cur = (countText.toIntOrNull() ?: 1)
                                        if (cur > 1) countText = (cur - 1).toString()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Outlined.Remove, contentDescription = "نقصان")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val countPresets = listOf(
                        1 to "قطعة (1)",
                        2 to "قطعتان (2)",
                        6 to "6 حبات",
                        10 to "10 بيضات",
                        15 to "15 بيضة",
                        30 to "بلاطو (30)"
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 3
                    ) {
                        countPresets.forEach { (cnt, label) ->
                            val isSelected = (countText.toIntOrNull() ?: 0) == cnt
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        countText = cnt.toString()
                                        replaceCountOnNextTap = true
                                    }
                                    .testTag("count_preset_$cnt"),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onTertiary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3.5 Live Price Trend Indicator Banner (مؤشر الغلاء والرخاء: أخضر / أصفر / أحمر)
            val livePriceEvaluation = remember(
                unitOrFixedPriceDirhams,
                selectedMode,
                itemPriceHistory,
                item.id,
                fallbackLastPriceDirhams
            ) {
                PriceTrendAnalyzer.evaluateItemPrice(
                    currentUnitPriceDirhams = unitOrFixedPriceDirhams,
                    purchaseMode = selectedMode,
                    itemHistory = itemPriceHistory,
                    excludeShoppingItemId = if (item.isPurchased) item.id else null,
                    fallbackLastPriceDirhams = fallbackLastPriceDirhams
                )
            }

            PriceIndicatorBanner(
                evaluation = livePriceEvaluation,
                onOpenHistoryClick = onOpenPriceHistory
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Built-in Oversized Tactile Numpad
            val numpadRows = listOf(
                listOf("7", "8", "9"),
                listOf("4", "5", "6"),
                listOf("1", "2", "3"),
                listOf(".", "0", "BACK")
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                numpadRows.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowKeys.forEach { key ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onNumpadKey(key) }
                                    .testTag("pricing_numpad_$key"),
                                color = if (key == "BACK") {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                shape = RoundedCornerShape(14.dp),
                                shadowElevation = 1.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (key == "BACK") {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                            contentDescription = "مسح",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    } else {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Instant Total Display + Big Confirm Button (Harmonious in both Dark & Light Modes)
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary),
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "الثمن المحسوب فوراً:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = CurrencyFormatter.formatDirhams(calculatedTotalDirhams),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (CurrencyFormatter.showRialState || inputInRials) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = CurrencyFormatter.formatRials(calculatedTotalDirhams),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onConfirmPurchase(
                                selectedMode,
                                unitOrFixedPriceDirhams,
                                effectiveQuantity,
                                effectiveCleaningFee,
                                calculatedTotalDirhams
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        modifier = Modifier
                            .heightIn(min = 50.dp)
                            .testTag("confirm_item_purchase_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "تأكيد الشراء",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "وضع في القفة",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
