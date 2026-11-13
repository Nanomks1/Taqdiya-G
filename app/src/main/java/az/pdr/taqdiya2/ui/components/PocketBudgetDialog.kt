package az.pdr.taqdiya2.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PocketBudgetDialog(
    currentBudgetDirhams: Double,
    onDismiss: () -> Unit,
    onSaveDirhams: (Double) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var inputInRials by remember { mutableStateOf(false) }
    var textValue by remember {
        mutableStateOf(CurrencyFormatter.formatAmountPlain(currentBudgetDirhams))
    }

    val numericValue = textValue.replace(',', '.').toDoubleOrNull() ?: 0.0
    val calculatedDirhams = if (inputInRials) {
        CurrencyFormatter.rialsToDirhams(numericValue)
    } else {
        numericValue
    }

    fun appendDigit(digit: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (digit == ".") {
            if (!textValue.contains(".")) {
                textValue = if (textValue.isEmpty()) "0." else "$textValue."
            }
        } else {
            textValue = if (textValue == "0") digit else textValue + digit
        }
    }

    fun backspace() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        textValue = if (textValue.length <= 1) "0" else textValue.dropLast(1)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Text(
                text = "ميزانية الجيب قبل الخروج",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. Big High-Contrast Live Display Screen
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (inputInRials) "$textValue ريال" else "$textValue درهم",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "يساوي: ${CurrencyFormatter.formatDirhams(calculatedDirhams)}  •  ${CurrencyFormatter.formatRials(calculatedDirhams)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Large Tactile Currency Switch: Dirham <-> Rial
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                if (inputInRials) {
                                    val currentDh = CurrencyFormatter.rialsToDirhams(numericValue)
                                    textValue = CurrencyFormatter.formatAmountPlain(currentDh)
                                    inputInRials = false
                                }
                            }
                            .testTag("budget_mode_dirham_chip"),
                        color = if (!inputInRials) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "إدخال بالدرهم",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (!inputInRials) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Outlined.SwapHoriz,
                        contentDescription = "تبديل",
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                if (!inputInRials) {
                                    val rials = CurrencyFormatter.dirhamsToRials(numericValue)
                                    textValue = rials.toString()
                                    inputInRials = true
                                }
                            }
                            .testTag("budget_mode_rial_chip"),
                        color = if (inputInRials) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "إدخال بالريال",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (inputInRials) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. 1-Tap Preset Amounts
                val presetsDirhams = listOf(50, 100, 150, 200, 300, 500)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetsDirhams.forEach { presetDh ->
                        Surface(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    textValue = if (inputInRials) {
                                        (presetDh * 20).toString()
                                    } else {
                                        presetDh.toString()
                                    }
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$presetDh درهم",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Built-in Giant Tactile Numpad (No tiny system keyboard!)
                val rows = listOf(
                    listOf("7", "8", "9"),
                    listOf("4", "5", "6"),
                    listOf("1", "2", "3"),
                    listOf(".", "0", "BACK")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowKeys.forEach { key ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            if (key == "BACK") backspace() else appendDigit(key)
                                        }
                                        .testTag("numpad_key_$key"),
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveDirhams(calculatedDirhams.coerceAtLeast(0.0))
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_pocket_budget_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "حفظ ميزانية الجيب (${CurrencyFormatter.formatDirhams(calculatedDirhams)})",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إلغاء")
            }
        }
    )
}
