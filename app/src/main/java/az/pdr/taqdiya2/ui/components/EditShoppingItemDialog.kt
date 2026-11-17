package az.pdr.taqdiya2.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import az.pdr.taqdiya2.data.local.DictionaryItemEntity
import az.pdr.taqdiya2.data.local.ShoppingItemEntity
import az.pdr.taqdiya2.data.local.VendorEntity
import az.pdr.taqdiya2.util.CurrencyFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditShoppingItemDialog(
    item: ShoppingItemEntity,
    vendors: List<VendorEntity>,
    dictionaryItems: List<DictionaryItemEntity> = emptyList(),
    onAddCustomUnit: (String, Long, String, String?) -> Unit = { _, _, _, _ -> },
    onRemoveCustomUnit: (String, String) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    onSave: (String, String, Long) -> Unit
) {
    var nameInput by remember { mutableStateOf(item.name) }
    var noteInput by remember { mutableStateOf(item.note) }
    var selectedVendorId by remember { mutableStateOf(item.vendorId) }
    var showAddCustomUnitInput by remember { mutableStateOf(false) }
    var customUnitText by remember { mutableStateOf("") }
    var customUnitTargetTab by remember { mutableStateOf("COUNT") }

    val selectedVendor = remember(vendors, selectedVendorId) {
        vendors.firstOrNull { it.id == selectedVendorId }
    }

    val dictEntry = remember(nameInput, dictionaryItems) {
        val norm = CurrencyFormatter.normalizeArabicName(nameInput)
        dictionaryItems.firstOrNull {
            CurrencyFormatter.normalizeArabicName(it.name) == norm || it.normalizedName == norm
        }
    }

    val customUnitsSet = remember(dictEntry) {
        CurrencyFormatter.parseCustomUnits(dictEntry?.customUnitsCsv.orEmpty()).toSet()
    }

    val smartUnits = remember(nameInput, selectedVendor, dictEntry) {
        CurrencyFormatter.getSmartUnitsForItem(
            itemName = nameInput,
            vendor = selectedVendor,
            customUnitsCsv = dictEntry?.customUnitsCsv.orEmpty()
        )
    }

    val smartModifiers = remember(nameInput) {
        CurrencyFormatter.getSmartUnitModifiersForItem(nameInput.ifBlank { item.name })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تعديل الغرض والمقدار",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("اسم الغرض") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_item_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("المقدار أو الملاحظة (مثال: علبة كبيرة، 2 كيلو...)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_item_note_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 1. Base Units Row
                Text(
                    text = "1. اختر الوحدة أو المقدار لـ «${nameInput.ifBlank { item.name }}»:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    smartUnits.forEach { unitOption ->
                        val isSelected = noteInput.trim() == unitOption ||
                            noteInput.trim().split(Regex("\\s+")).contains(unitOption)
                        val isCustom = customUnitsSet.contains(unitOption)
                        Surface(
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    noteInput = CurrencyFormatter.applySmartBaseUnit(noteInput, unitOption)
                                },
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = unitOption,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onSecondary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                                if (isCustom) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "حذف المقدار الخاص",
                                        tint = if (isSelected) {
                                            MaterialTheme.colorScheme.onSecondary
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        },
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                onRemoveCustomUnit(nameInput.ifBlank { item.name }, unitOption)
                                            }
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .heightIn(min = 36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showAddCustomUnitInput = !showAddCustomUnitInput
                            },
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "مقدار خاص",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 2. Composable Size & Count Modifiers (صغيرة / متوسطة / كبيرة / ×2 / ×3 ...)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "2. حدد الحجم أو العدد (يُدمج تلقائياً مع الوحدة، مثل: علبة + كبيرة):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val defaultContainer = smartUnits.firstOrNull() ?: "علبة"
                    smartModifiers.forEach { modChip ->
                        val isModActive = CurrencyFormatter.isNoteModifierActive(noteInput, modChip)
                        Surface(
                            modifier = Modifier
                                .heightIn(min = 34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    noteInput = CurrencyFormatter.applySmartNoteModifier(
                                        currentNote = noteInput,
                                        modifierChip = modChip,
                                        defaultContainerIfEmpty = defaultContainer
                                    )
                                },
                            color = if (isModActive) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isModActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = modChip,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isModActive) {
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

                if (showAddCustomUnitInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ربط هذا المقدار في حاسبة الثمن بـ:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val tabChoices = listOf(
                            "COUNT" to "3. بالعدد / العلبة",
                            "WEIGHT" to "1. بالوزن",
                            "FIXED" to "2. مبلغ مقطوع"
                        )
                        tabChoices.forEach { (tabKey, tabLabel) ->
                            val isTabSelected = customUnitTargetTab == tabKey
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { customUnitTargetTab = tabKey },
                                color = if (isTabSelected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                border = BorderStroke(
                                    width = if (isTabSelected) 1.5.dp else 1.dp,
                                    color = if (isTabSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tabLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isTabSelected) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customUnitText,
                            onValueChange = { customUnitText = it },
                            placeholder = { Text("مقدار جديد (مثال: قرعة، باكية...)") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val cleanUnit = customUnitText.trim()
                                val targetName = nameInput.trim().ifEmpty { item.name }
                                if (cleanUnit.isNotEmpty()) {
                                    onAddCustomUnit(targetName, selectedVendorId, cleanUnit, customUnitTargetTab)
                                    noteInput = cleanUnit
                                    customUnitText = ""
                                    showAddCustomUnitInput = false
                                }
                            },
                            enabled = customUnitText.trim().isNotEmpty(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("إضافة")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "3. البائع المخصص لهذه السلعة (اضغط لنقل الغرض لبائع آخر):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    vendors.forEach { vendor ->
                        val isVendorSelected = selectedVendorId == vendor.id
                        val isLinkedInDict = dictEntry != null && CurrencyFormatter.dictItemMatchesVendor(
                            dictEntry.defaultVendorId,
                            dictEntry.secondaryVendorIdsCsv,
                            vendor.id
                        )
                        Surface(
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedVendorId = vendor.id }
                                .testTag("edit_item_vendor_chip_${vendor.id}"),
                            color = if (isVendorSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            border = BorderStroke(
                                width = if (isVendorSelected) 1.5.dp else 1.dp,
                                color = if (isVendorSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = VendorIconMapper.getIcon(vendor.iconKey, vendor.name),
                                    contentDescription = null,
                                    tint = if (isVendorSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isLinkedInDict && !isVendorSelected) {
                                        "${vendor.name} ★"
                                    } else {
                                        vendor.name
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isVendorSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = nameInput.trim()
                    if (clean.isNotEmpty()) {
                        onSave(clean, noteInput.trim(), selectedVendorId)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_edit_item_button")
            ) {
                Text("حفظ التعديل")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
