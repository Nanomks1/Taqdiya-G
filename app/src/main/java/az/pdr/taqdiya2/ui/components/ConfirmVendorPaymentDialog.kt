package az.pdr.taqdiya2.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import az.pdr.taqdiya2.data.local.VendorEntity
import az.pdr.taqdiya2.data.local.VendorPaymentEntity
import az.pdr.taqdiya2.util.CurrencyFormatter
import kotlin.math.abs
import kotlin.math.floor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmVendorPaymentSheet(
    vendor: VendorEntity,
    calculatedItemsTotalDirhams: Double,
    existingPayment: VendorPaymentEntity?,
    onConfirmPayment: (
        calculatedTotalDirhams: Double,
        actualPaidDirhams: Double,
        forgivenDiscountDirhams: Double,
        newVendorPendingBalanceDirhams: Double
    ) -> Unit,
    onResetVendorPayment: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current

    // Previous pending balance on this vendor before this payment:
    // If existingPayment != null, we still show vendor.pendingBalanceDirhams if non-zero
    val previousPendingDirhams = vendor.pendingBalanceDirhams
    var settlePreviousPending by remember { mutableStateOf(abs(previousPendingDirhams) > 0.009) }

    // Net required today:
    // If vendor owed me (+previousPending), I subtract it from today's total.
    // If I owed vendor (-previousPending), I add abs(previousPending) to today's total.
    val netRequiredDirhams = remember(calculatedItemsTotalDirhams, previousPendingDirhams, settlePreviousPending) {
        val raw = if (settlePreviousPending) {
            calculatedItemsTotalDirhams - previousPendingDirhams
        } else {
            calculatedItemsTotalDirhams
        }
        CurrencyFormatter.roundMoney(raw.coerceAtLeast(0.0))
    }

    var isInputInRials by remember { mutableStateOf(false) }
    var paidInputText by remember(existingPayment, netRequiredDirhams) {
        val initialDh = existingPayment?.actualPaidDirhams ?: netRequiredDirhams
        mutableStateOf(
            if (initialDh > 0.0) CurrencyFormatter.formatPlainNumber(initialDh) else "0"
        )
    }

    val rawPaidValue = paidInputText.toDoubleOrNull() ?: 0.0
    val actualPaidDirhams = remember(rawPaidValue, isInputInRials) {
        val dh = if (isInputInRials) CurrencyFormatter.rialsToDirhams(rawPaidValue) else rawPaidValue
        CurrencyFormatter.roundMoney(dh.coerceAtLeast(0.0))
    }

    // Difference analysis:
    // diff = netRequiredDirhams - actualPaidDirhams
    // If diff > 0: user paid LESS than required (either vendor forgave small change OR it remains owed to vendor "بقا ليه عندي")
    // If diff < 0: user paid MORE than required (either change stays with vendor "بقا ليا عندو" OR settled)
    val unpaidDiffDirhams = CurrencyFormatter.roundMoney(netRequiredDirhams - actualPaidDirhams)

    // When paying less: true = "سامح ليا فالصرف (توفير)", false = "بقا ليه عندي للمرة الجاية"
    var treatUnderpaymentAsForgiven by remember { mutableStateOf(true) }
    // When paying more: true = "بقا ليا عندو الصرف للمرة الجاية", false = "أخذت الصرف كامل"
    var recordOverpaymentAsVendorOwesMe by remember { mutableStateOf(true) }

    // Quick suggested payment amounts in Dirhams
    val quickSuggestedDirhams = remember(netRequiredDirhams) {
        val suggestions = linkedSetOf<Double>()
        if (netRequiredDirhams > 0.0) {
            suggestions.add(netRequiredDirhams)
            val flooredWhole = floor(netRequiredDirhams)
            if (flooredWhole > 0.0 && flooredWhole < netRequiredDirhams) {
                suggestions.add(flooredWhole)
            }
            val flooredFive = floor(netRequiredDirhams / 5.0) * 5.0
            if (flooredFive > 0.0 && flooredFive < netRequiredDirhams) {
                suggestions.add(flooredFive)
            }
            val nextTen = (floor(netRequiredDirhams / 10.0) + 1.0) * 10.0
            if (nextTen > netRequiredDirhams) {
                suggestions.add(nextTen)
            }
            val nextFifty = when {
                netRequiredDirhams < 50.0 -> 50.0
                netRequiredDirhams < 100.0 -> 100.0
                else -> 200.0
            }
            if (nextFifty > netRequiredDirhams) {
                suggestions.add(nextFifty)
            }
        }
        suggestions.toList()
    }

    fun appendDigit(digit: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (digit == "." && paidInputText.contains(".")) return
        paidInputText = if (paidInputText == "0" && digit != ".") {
            digit
        } else if (paidInputText.length < 7) {
            paidInputText + digit
        } else {
            paidInputText
        }
    }

    fun backspaceDigit() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        paidInputText = if (paidInputText.length <= 1) "0" else paidInputText.dropLast(1)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "تأكيد الدفع: ${vendor.name}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "راجع الحساب وعدّل المبلغ المدفوع فعلياً ليُخصم من جيبك بدقة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "إغلاق")
                }
            }

            // 2. Calculated Items Total & Previous Pending Balance Card
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مجموع أغراض القفة اليوم:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${CurrencyFormatter.formatDirhams(calculatedItemsTotalDirhams)} (${CurrencyFormatter.formatRials(calculatedItemsTotalDirhams)})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (abs(previousPendingDirhams) > 0.009) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        val isVendorOwingMe = previousPendingDirhams > 0.0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isVendorOwingMe) {
                                        "بقا ليا عندو من قبل: ${CurrencyFormatter.formatDirhams(abs(previousPendingDirhams))}"
                                    } else {
                                        "بقا ليه عندي من قبل: ${CurrencyFormatter.formatDirhams(abs(previousPendingDirhams))}"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isVendorOwingMe) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "احتساب وتصفية هذا الصرف في حساب اليوم",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settlePreviousPending,
                                onCheckedChange = { checked ->
                                    settlePreviousPending = checked
                                    val updatedNet = if (checked) {
                                        (calculatedItemsTotalDirhams - previousPendingDirhams).coerceAtLeast(0.0)
                                    } else {
                                        calculatedItemsTotalDirhams
                                    }
                                    val valToShow = if (isInputInRials) {
                                        CurrencyFormatter.dirhamsToRials(updatedNet).toDouble()
                                    } else {
                                        updatedNet
                                    }
                                    paidInputText = CurrencyFormatter.formatPlainNumber(valToShow)
                                }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الصافي المطلوب أداؤه الآن:",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${CurrencyFormatter.formatDirhams(netRequiredDirhams)} (${CurrencyFormatter.formatRials(netRequiredDirhams)})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3. Quick 1-Tap Moroccan Rounding & Bill Buttons
            if (quickSuggestedDirhams.isNotEmpty()) {
                Text(
                    text = "أزرار سريعة للمبلغ المدفوع (المسامحة في الصرف أو أوراق نقدية):",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickSuggestedDirhams.forEach { suggestedDh ->
                        val isExact = abs(suggestedDh - netRequiredDirhams) < 0.009
                        val isDiscount = suggestedDh < netRequiredDirhams
                        val isSelected = abs(actualPaidDirhams - suggestedDh) < 0.009

                        val labelText = when {
                            isExact -> "المجموع (${CurrencyFormatter.formatDirhams(suggestedDh)})"
                            isDiscount -> "دفعت ${CurrencyFormatter.formatDirhams(suggestedDh)} (سامح فـ ${CurrencyFormatter.formatPlainNumber(netRequiredDirhams - suggestedDh)})"
                            else -> "عطيتو ${CurrencyFormatter.formatDirhams(suggestedDh)}"
                        }

                        Surface(
                            modifier = Modifier
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val targetVal = if (isInputInRials) {
                                        CurrencyFormatter.dirhamsToRials(suggestedDh).toDouble()
                                    } else {
                                        suggestedDh
                                    }
                                    paidInputText = CurrencyFormatter.formatPlainNumber(targetVal)
                                },
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = labelText,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 4. Actual Paid Display + Dirham/Rial Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المبلغ الذي دفعته فعلياً:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val currentVal = paidInputText.toDoubleOrNull() ?: 0.0
                            val converted = if (isInputInRials) {
                                CurrencyFormatter.rialsToDirhams(currentVal)
                            } else {
                                CurrencyFormatter.dirhamsToRials(currentVal).toDouble()
                            }
                            isInputInRials = !isInputInRials
                            paidInputText = if (converted > 0.0) CurrencyFormatter.formatPlainNumber(converted) else "0"
                        }
                        .testTag("payment_currency_toggle"),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SwapHoriz,
                            contentDescription = "تبديل العملة",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isInputInRials) "إدخال بالريال ⇄ درهم" else "إدخال بالدرهم ⇄ ريال",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$paidInputText ${if (isInputInRials) "ريال" else "درهم"}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("actual_paid_display_text")
                        )
                        Text(
                            text = if (isInputInRials) {
                                "يساوي: ${CurrencyFormatter.formatDirhams(actualPaidDirhams)}"
                            } else {
                                "يساوي: ${CurrencyFormatter.formatRials(actualPaidDirhams)}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { paidInputText = "0" },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("مسح")
                    }
                }
            }

            // 5. Difference Resolution (Forgiven Savings vs Pending Change "بقا ليا عندو / بقا ليه عندي")
            if (unpaidDiffDirhams > 0.009) {
                // User paid LESS than required!
                Text(
                    text = "دفعت أقل بـ ${CurrencyFormatter.formatDirhams(unpaidDiffDirhams)} (${CurrencyFormatter.formatRials(unpaidDiffDirhams)}) — كيف نحسب الفرق؟",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DifferenceChoiceCard(
                        title = "سامح ليا فالصرف",
                        subtitle = "توفير لك (${CurrencyFormatter.formatDirhams(unpaidDiffDirhams)})",
                        selected = treatUnderpaymentAsForgiven,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        onClick = { treatUnderpaymentAsForgiven = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("choice_forgiven_savings")
                    )
                    DifferenceChoiceCard(
                        title = "بقا ليه عندي",
                        subtitle = "دين للبائع للمرة الجاية",
                        selected = !treatUnderpaymentAsForgiven,
                        accentColor = MaterialTheme.colorScheme.error,
                        onClick = { treatUnderpaymentAsForgiven = false },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("choice_vendor_credit_owed")
                    )
                }
            } else if (unpaidDiffDirhams < -0.009) {
                // User paid MORE than required (e.g. gave a 50 DH bill for 46.50 DH)
                val extraPaidDirhams = abs(unpaidDiffDirhams)
                Text(
                    text = "أعطيت البائع زيادة ${CurrencyFormatter.formatDirhams(extraPaidDirhams)} (${CurrencyFormatter.formatRials(extraPaidDirhams)}) — هل بقي الصرف عنده؟",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DifferenceChoiceCard(
                        title = "بقا ليا عندو الصرف",
                        subtitle = "يذكرني بـ ${CurrencyFormatter.formatDirhams(extraPaidDirhams)} المرة الجاية",
                        selected = recordOverpaymentAsVendorOwesMe,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        onClick = { recordOverpaymentAsVendorOwesMe = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("choice_vendor_owes_me_change")
                    )
                    DifferenceChoiceCard(
                        title = "أخذت الصرف كامل",
                        subtitle = "خصم الصافي فقط (${CurrencyFormatter.formatDirhams(netRequiredDirhams)})",
                        selected = !recordOverpaymentAsVendorOwesMe,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = { recordOverpaymentAsVendorOwesMe = false },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("choice_took_all_change")
                    )
                }
            }

            // 6. Compact Tactile Numpad
            val numpadRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
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
                                    .clickable {
                                        if (key == "BACK") backspaceDigit() else appendDigit(key)
                                    },
                                color = if (key == "BACK") {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (key == "BACK") {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                            contentDescription = "حذف رقم",
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    } else {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.titleLarge,
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

            // 7. Confirm Payment Button
            Button(
                onClick = {
                    val basePending = if (settlePreviousPending) 0.0 else previousPendingDirhams
                    val finalActualPaid: Double
                    val finalForgiven: Double
                    val finalNewPending: Double

                    when {
                        unpaidDiffDirhams > 0.009 -> {
                            finalActualPaid = actualPaidDirhams
                            if (treatUnderpaymentAsForgiven) {
                                finalForgiven = unpaidDiffDirhams
                                finalNewPending = basePending
                            } else {
                                finalForgiven = 0.0
                                finalNewPending = basePending - unpaidDiffDirhams
                            }
                        }
                        unpaidDiffDirhams < -0.009 -> {
                            val extra = abs(unpaidDiffDirhams)
                            finalForgiven = 0.0
                            if (recordOverpaymentAsVendorOwesMe) {
                                // User gave the full bill and left the change with the vendor
                                finalActualPaid = actualPaidDirhams
                                finalNewPending = basePending + extra
                            } else {
                                // User took their change, so only netRequiredDirhams left the pocket
                                finalActualPaid = netRequiredDirhams
                                finalNewPending = basePending
                            }
                        }
                        else -> {
                            finalActualPaid = actualPaidDirhams
                            finalForgiven = 0.0
                            finalNewPending = basePending
                        }
                    }

                    onConfirmPayment(
                        calculatedItemsTotalDirhams,
                        finalActualPaid,
                        finalForgiven,
                        finalNewPending
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .testTag("confirm_vendor_payment_submit_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "تأكيد الدفع (${CurrencyFormatter.formatDirhams(actualPaidDirhams)})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (onResetVendorPayment != null && (existingPayment != null || calculatedItemsTotalDirhams > 0.0)) {
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onResetVendorPayment()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("reset_vendor_payment_sheet_button"),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إلغاء وتصفير الدفع والقفة لهذا البائع",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DifferenceChoiceCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = if (selected) accentColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) accentColor else MaterialTheme.colorScheme.outline
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) accentColor else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPendingBalanceSheet(
    vendor: VendorEntity,
    onSavePendingBalance: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current

    // true = "بقا ليا عندو" (positive), false = "بقا ليه عندي" (negative)
    var isVendorOwingMe by remember(vendor) {
        mutableStateOf(vendor.pendingBalanceDirhams >= 0.0)
    }
    var isInputInRials by remember { mutableStateOf(false) }
    var amountText by remember(vendor) {
        val absVal = abs(vendor.pendingBalanceDirhams)
        mutableStateOf(if (absVal > 0.0) CurrencyFormatter.formatPlainNumber(absVal) else "0")
    }

    val rawVal = amountText.toDoubleOrNull() ?: 0.0
    val amountDirhams = remember(rawVal, isInputInRials) {
        val dh = if (isInputInRials) CurrencyFormatter.rialsToDirhams(rawVal) else rawVal
        CurrencyFormatter.roundMoney(dh.coerceAtLeast(0.0))
    }

    fun appendDigit(digit: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (digit == "." && amountText.contains(".")) return
        amountText = if (amountText == "0" && digit != ".") {
            digit
        } else if (amountText.length < 7) {
            amountText + digit
        } else {
            amountText
        }
    }

    fun backspaceDigit() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        amountText = if (amountText.length <= 1) "0" else amountText.dropLast(1)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Handshake,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "الصرف العالق: ${vendor.name}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "سجّل ما بقي لك عند البائع أو ما بقي له عندك ليذكرك التطبيق",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "إغلاق")
                }
            }

            // Direction Selector: "بقا ليا عندو" vs "بقا ليه عندي"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DifferenceChoiceCard(
                    title = "بقا ليا عندو",
                    subtitle = "لي بذمة البائع (صرف باقي لي)",
                    selected = isVendorOwingMe,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    onClick = { isVendorOwingMe = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pending_type_vendor_owes_me")
                )
                DifferenceChoiceCard(
                    title = "بقا ليه عندي",
                    subtitle = "للبائع بذمتي (خصني نعطيه)",
                    selected = !isVendorOwingMe,
                    accentColor = MaterialTheme.colorScheme.error,
                    onClick = { isVendorOwingMe = false },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pending_type_i_owe_vendor")
                )
            }

            // Quick Small Change Presets
            val quickChangesDh = listOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 5.0, 10.0)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickChangesDh.forEach { presetDh ->
                    val isSelected = abs(amountDirhams - presetDh) < 0.009
                    Surface(
                        modifier = Modifier
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val target = if (isInputInRials) {
                                    CurrencyFormatter.dirhamsToRials(presetDh).toDouble()
                                } else {
                                    presetDh
                                }
                                amountText = CurrencyFormatter.formatPlainNumber(target)
                            },
                        color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${CurrencyFormatter.formatDirhams(presetDh)} (${CurrencyFormatter.formatRials(presetDh)})",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Display + Currency Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isVendorOwingMe) "مبلغ بقا ليا عندو:" else "مبلغ بقا ليه عندي:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            val currentVal = amountText.toDoubleOrNull() ?: 0.0
                            val converted = if (isInputInRials) {
                                CurrencyFormatter.rialsToDirhams(currentVal)
                            } else {
                                CurrencyFormatter.dirhamsToRials(currentVal).toDouble()
                            }
                            isInputInRials = !isInputInRials
                            amountText = if (converted > 0.0) CurrencyFormatter.formatPlainNumber(converted) else "0"
                        },
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SwapHoriz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isInputInRials) "ريال ⇄ درهم" else "درهم ⇄ ريال",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$amountText ${if (isInputInRials) "ريال" else "درهم"}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isInputInRials) {
                                CurrencyFormatter.formatDirhams(amountDirhams)
                            } else {
                                CurrencyFormatter.formatRials(amountDirhams)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (abs(vendor.pendingBalanceDirhams) > 0.009) {
                        OutlinedButton(
                            onClick = {
                                onSavePendingBalance(0.0)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("clear_pending_balance_button")
                        ) {
                            Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تصفية الصرف (0 درهم)")
                        }
                    }
                }
            }

            // Compact Numpad
            val numpadRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
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
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (key == "BACK") backspaceDigit() else appendDigit(key)
                                    },
                                color = if (key == "BACK") {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (key == "BACK") {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                            contentDescription = "حذف رقم",
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    } else {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val signedAmount = if (amountDirhams == 0.0) {
                        0.0
                    } else if (isVendorOwingMe) {
                        amountDirhams
                    } else {
                        -amountDirhams
                    }
                    onSavePendingBalance(signedAmount)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_pending_balance_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (amountDirhams == 0.0) {
                        "حفظ بدون صرف عالق (0 درهم)"
                    } else if (isVendorOwingMe) {
                        "حفظ: بقا ليا عندو ${CurrencyFormatter.formatDirhams(amountDirhams)}"
                    } else {
                        "حفظ: بقا ليه عندي ${CurrencyFormatter.formatDirhams(amountDirhams)}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
