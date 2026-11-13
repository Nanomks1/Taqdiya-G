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
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import az.pdr.taqdiya2.util.CurrencyFormatter
import kotlin.math.abs

private data class PassingExpensePreset(
    val title: String,
    val defaultDirhams: Double
)

private val PASSING_EXPENSE_PRESETS = listOf(
    PassingExpensePreset("خبز", 5.0),
    PassingExpensePreset("طاكسي", 7.0),
    PassingExpensePreset("أكياس / ميكة", 1.0),
    PassingExpensePreset("ماء", 5.0),
    PassingExpensePreset("عساس / باركينغ", 3.0),
    PassingExpensePreset("حمال / كروسة", 5.0),
    PassingExpensePreset("قهوة / أتاي", 10.0),
    PassingExpensePreset("مصروف عابر", 5.0)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassingExpenseSheet(
    onAddExpense: (title: String, amountDirhams: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current

    var expenseTitle by remember { mutableStateOf("خبز") }
    var isInputInRials by remember { mutableStateOf(false) }
    var amountInputText by remember { mutableStateOf("5") }

    val rawAmount = amountInputText.toDoubleOrNull() ?: 0.0
    val amountDirhams = remember(rawAmount, isInputInRials) {
        val dh = if (isInputInRials) CurrencyFormatter.rialsToDirhams(rawAmount) else rawAmount
        CurrencyFormatter.roundMoney(dh.coerceAtLeast(0.0))
    }

    fun appendDigit(digit: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (digit == "." && amountInputText.contains(".")) return
        amountInputText = if (amountInputText == "0" && digit != ".") {
            digit
        } else if (amountInputText.length < 7) {
            amountInputText + digit
        } else {
            amountInputText
        }
    }

    fun backspaceDigit() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        amountInputText = if (amountInputText.length <= 1) "0" else amountInputText.dropLast(1)
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
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "+ مصروف عابر خارج البائعين",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "سجّل خبز، طاكسي، كيس، أو أي مصروف سريع ليُخصم من جيبك فوراً",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "إغلاق")
                }
            }

            // 2. Quick 1-Tap Category Chips
            Text(
                text = "اختر نوع المصروف بلمسة واحدة:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PASSING_EXPENSE_PRESETS.forEach { preset ->
                    val isSelected = expenseTitle == preset.title
                    Surface(
                        modifier = Modifier
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                expenseTitle = preset.title
                                val valToShow = if (isInputInRials) {
                                    CurrencyFormatter.dirhamsToRials(preset.defaultDirhams).toDouble()
                                } else {
                                    preset.defaultDirhams
                                }
                                amountInputText = CurrencyFormatter.formatPlainNumber(valToShow)
                            }
                            .testTag("passing_expense_preset_${preset.title}"),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = preset.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Custom Title Input
            OutlinedTextField(
                value = expenseTitle,
                onValueChange = { expenseTitle = it },
                label = { Text("بيان المصروف العابر (اختياري)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("passing_expense_title_input")
            )

            // 3. Quick Amount Presets
            val quickAmountsDh = listOf(1.0, 2.0, 3.0, 5.0, 7.0, 10.0, 15.0, 20.0)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickAmountsDh.forEach { presetDh ->
                    val isSelected = abs(amountDirhams - presetDh) < 0.009
                    Surface(
                        modifier = Modifier
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val target = if (isInputInRials) {
                                    CurrencyFormatter.dirhamsToRials(presetDh).toDouble()
                                } else {
                                    presetDh
                                }
                                amountInputText = CurrencyFormatter.formatPlainNumber(target)
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
                                text = CurrencyFormatter.formatDirhams(presetDh),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 4. Amount Display & Dirham/Rial Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مبلغ المصروف:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            val currentVal = amountInputText.toDoubleOrNull() ?: 0.0
                            val converted = if (isInputInRials) {
                                CurrencyFormatter.rialsToDirhams(currentVal)
                            } else {
                                CurrencyFormatter.dirhamsToRials(currentVal).toDouble()
                            }
                            isInputInRials = !isInputInRials
                            amountInputText = if (converted > 0.0) CurrencyFormatter.formatPlainNumber(converted) else "0"
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
                            text = if (isInputInRials) "إدخال بالريال ⇄ درهم" else "إدخال بالدرهم ⇄ ريال",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
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
                            text = "$amountInputText ${if (isInputInRials) "ريال" else "درهم"}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isInputInRials) {
                                "يساوي: ${CurrencyFormatter.formatDirhams(amountDirhams)}"
                            } else {
                                "يساوي: ${CurrencyFormatter.formatRials(amountDirhams)}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { amountInputText = "0" },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("مسح")
                    }
                }
            }

            // 5. Built-in Numpad
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

            // 6. Submit Button
            Button(
                onClick = {
                    if (amountDirhams > 0.0) {
                        onAddExpense(expenseTitle.trim().ifEmpty { "مصروف عابر" }, amountDirhams)
                        onDismiss()
                    }
                },
                enabled = amountDirhams > 0.0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("submit_passing_expense_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "خصم من الجيب (${CurrencyFormatter.formatDirhams(amountDirhams)})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
