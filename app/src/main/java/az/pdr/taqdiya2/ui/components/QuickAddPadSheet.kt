package az.pdr.taqdiya2.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import az.pdr.taqdiya2.data.local.DictionaryItemEntity
import az.pdr.taqdiya2.data.local.ShoppingItemEntity
import az.pdr.taqdiya2.data.local.VendorEntity
import az.pdr.taqdiya2.ui.viewmodel.UndoAction
import az.pdr.taqdiya2.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickAddPadSheet(
    vendors: List<VendorEntity>,
    dictionaryItems: List<DictionaryItemEntity>,
    shoppingItems: List<ShoppingItemEntity>,
    selectedVendorId: Long?,
    lastTouchedItemId: Long?,
    itemNameInput: String,
    itemNoteInput: String,
    autoMatchedVendorName: String?,
    activeUndo: UndoAction?,
    onPerformUndo: (UndoAction) -> Unit,
    onDismissUndo: () -> Unit,
    onSelectVendor: (Long) -> Unit,
    onToggleTile: (DictionaryItemEntity, Long?) -> Unit,
    onApplyQuickNote: (String) -> Unit,
    onClearTargetItem: () -> Unit,
    onAddCustomUnit: (String, Long, String) -> Unit,
    onRemoveCustomUnit: (String, String) -> Unit,
    onItemNameChanged: (String) -> Unit,
    onItemNoteChanged: (String) -> Unit,
    onSubmitCustomItem: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    var isNameFieldFocused by remember { mutableStateOf(false) }
    var isNoteFieldFocused by remember { mutableStateOf(false) }
    var showAddCustomUnitForTarget by remember { mutableStateOf(false) }
    var newCustomUnitText by remember { mutableStateOf("") }
    val isImeOpen = WindowInsets.isImeVisible
    val isTypingMode = isNameFieldFocused || isNoteFieldFocused || isImeOpen

    // When typing with keyboard open, pressing Back dismisses keyboard/focus first instead of closing the sheet
    BackHandler(enabled = isTypingMode) {
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    LaunchedEffect(isTypingMode) {
        if (isTypingMode) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    val activeVendorId = selectedVendorId ?: vendors.firstOrNull()?.id
    val currentVendor = vendors.firstOrNull { it.id == activeVendorId }

    val vendorDictionaryItems = remember(dictionaryItems, activeVendorId) {
        val forVendor = dictionaryItems.filter { it.defaultVendorId == activeVendorId }
        if (forVendor.isNotEmpty()) forVendor else dictionaryItems.take(15)
    }

    val activeShoppingMap = remember(shoppingItems) {
        shoppingItems.filter { !it.isDeleted && !it.isPurchased }
            .associateBy { CurrencyFormatter.normalizeArabicName(it.name) }
    }

    // Only show 1-tap note bar when an item was explicitly touched in the grid (never fallback to random previous item)
    val activeTargetItem = remember(shoppingItems, lastTouchedItemId) {
        if (lastTouchedItemId == null) null
        else shoppingItems.firstOrNull { it.id == lastTouchedItemId && !it.isDeleted && !it.isPurchased }
    }

    val targetItemDictEntry = remember(activeTargetItem, dictionaryItems) {
        if (activeTargetItem == null) null
        else {
            val norm = CurrencyFormatter.normalizeArabicName(activeTargetItem.name)
            dictionaryItems.firstOrNull { it.normalizedName == norm }
        }
    }

    val targetItemVendor = remember(activeTargetItem, vendors, currentVendor) {
        if (activeTargetItem == null) currentVendor
        else vendors.firstOrNull { it.id == activeTargetItem.vendorId } ?: currentVendor
    }

    val targetCustomUnitsSet = remember(targetItemDictEntry) {
        CurrencyFormatter.parseCustomUnits(targetItemDictEntry?.customUnitsCsv.orEmpty()).toSet()
    }

    val smartQuickNotes = remember(activeTargetItem, targetItemVendor, targetItemDictEntry) {
        if (activeTargetItem == null) emptyList()
        else CurrencyFormatter.getSmartUnitsForItem(
            itemName = activeTargetItem.name,
            vendor = targetItemVendor,
            customUnitsCsv = targetItemDictEntry?.customUnitsCsv.orEmpty()
        )
    }

    val typedInputSmartUnits = remember(itemNameInput, currentVendor, dictionaryItems) {
        val cleanTyped = itemNameInput.trim()
        val normTyped = CurrencyFormatter.normalizeArabicName(cleanTyped)
        val matchedDict = if (normTyped.isNotEmpty()) {
            dictionaryItems.firstOrNull { it.normalizedName == normTyped }
        } else null
        CurrencyFormatter.getSmartUnitsForItem(
            itemName = cleanTyped,
            vendor = currentVendor,
            customUnitsCsv = matchedDict?.customUnitsCsv.orEmpty()
        )
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
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // 1. Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "لوحة الإضافة السريعة باللمس",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "اضغط على المربع لإضافة السلعة أو إزالتها فوراً بدون كتابة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("close_quick_pad_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "إغلاق اللوحة",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Inline Global Undo Banner inside Quick Pad
            InlineUndoBanner(
                activeUndo = activeUndo,
                onPerformUndo = onPerformUndo,
                onDismiss = onDismissUndo
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Vendor Selector Strip (Warm Tonal Highlight)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                vendors.forEach { vendor ->
                    val isSelected = vendor.id == activeVendorId
                    val itemCount = shoppingItems.count { it.vendorId == vendor.id && !it.isDeleted && !it.isPurchased }
                    Surface(
                        modifier = Modifier
                            .heightIn(min = 50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectVendor(vendor.id)
                            }
                            .testTag("pad_vendor_tile_${vendor.id}"),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = VendorIconMapper.getIcon(vendor.iconKey),
                                contentDescription = vendor.name,
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = vendor.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (itemCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.secondary
                                            } else {
                                                MaterialTheme.colorScheme.secondaryContainer
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = itemCount.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onSecondary
                                        } else {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // When not actively typing in the bottom custom input, show the tactile grid and quick notes
            if (!isTypingMode) {
                Spacer(modifier = Modifier.height(12.dp))

                // 3. 3-Column Oversized Tactile Item Grid
                Text(
                    text = "سلع ${currentVendor?.name ?: "البائع"} (اضغط للإضافة الفورية):",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 135.dp, max = 205.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(vendorDictionaryItems, key = { it.id }) { dictItem ->
                        val existingInList = activeShoppingMap[dictItem.normalizedName]
                        val isAdded = existingInList != null

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleTile(dictItem, activeVendorId)
                                }
                                .testTag("tactile_tile_${dictItem.id}"),
                            color = if (isAdded) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = if (isAdded) 2.dp else 1.dp,
                                color = if (isAdded) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }
                            ),
                            shape = RoundedCornerShape(16.dp),
                            shadowElevation = if (isAdded) 0.dp else 1.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isAdded) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "مضاف",
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = dictItem.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isAdded) {
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (isAdded && existingInList != null && existingInList.note.isNotBlank()) {
                                    Text(
                                        text = existingInList.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        text = if (isAdded) "في القائمة" else "+ أضف",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isAdded) {
                                            MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                        } else {
                                            MaterialTheme.colorScheme.secondary
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. 1-Tap Ready Notes Bar for the Active Item (Smart + Custom Units)
                if (activeTargetItem != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "مقدار أو ملاحظة بلمسة واحدة لـ «${activeTargetItem.name}»:",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        showAddCustomUnitForTarget = false
                                        onClearTargetItem()
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "إخفاء شريط المقادير",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                smartQuickNotes.forEach { noteOption ->
                                    val isNoteSelected = activeTargetItem.note == noteOption
                                    val isCustomUnit = targetCustomUnitsSet.contains(noteOption)
                                    Surface(
                                        modifier = Modifier
                                            .heightIn(min = 38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onApplyQuickNote(noteOption)
                                            },
                                        color = if (isNoteSelected) {
                                            MaterialTheme.colorScheme.secondary
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (isNoteSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = noteOption,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (isNoteSelected) {
                                                    MaterialTheme.colorScheme.onSecondary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                            if (isCustomUnit) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = "حذف المقدار الخاص",
                                                    tint = if (isNoteSelected) {
                                                        MaterialTheme.colorScheme.onSecondary
                                                    } else {
                                                        MaterialTheme.colorScheme.error
                                                    },
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clickable {
                                                            onRemoveCustomUnit(activeTargetItem.name, noteOption)
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }

                                // "+ مقدار خاص" chip to add a custom unit specifically for this item
                                Surface(
                                    modifier = Modifier
                                        .heightIn(min = 38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            showAddCustomUnitForTarget = !showAddCustomUnitForTarget
                                        }
                                        .testTag("add_custom_unit_chip"),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "مقدار خاص",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (showAddCustomUnitForTarget) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newCustomUnitText,
                                        onValueChange = { newCustomUnitText = it },
                                        placeholder = { Text("مثال: قرعة كبيرة، نص باكية...") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            val cleanUnit = newCustomUnitText.trim()
                                            if (cleanUnit.isNotEmpty()) {
                                                onAddCustomUnit(
                                                    activeTargetItem.name,
                                                    activeTargetItem.vendorId,
                                                    cleanUnit
                                                )
                                                onApplyQuickNote(cleanUnit)
                                                newCustomUnitText = ""
                                                showAddCustomUnitForTarget = false
                                            }
                                        },
                                        enabled = newCustomUnitText.trim().isNotEmpty(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("حفظ")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // 5. Custom New Item Input Row (Always clearly visible above keyboard!)
            Surface(
                color = if (isTypingMode) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background,
                border = if (isTypingMode) BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary) else null,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (isTypingMode) 12.dp else 0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "أو اكتب غرضاً جديداً (يتعلمه القاموس تلقائياً):",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        if (isTypingMode) {
                            Text(
                                text = "إخفاء الكيبورد",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (autoMatchedVendorName != null) {
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "سيُضاف تلقائياً إلى: $autoMatchedVendorName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (currentVendor != null) {
                        Text(
                            text = "سيُضاف إلى: ${currentVendor.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = itemNameInput,
                            onValueChange = onItemNameChanged,
                            placeholder = {
                                Text(
                                    text = "اسم الغرض...",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.25f)
                                .onFocusChanged { isNameFieldFocused = it.isFocused }
                                .testTag("add_item_name_input"),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )

                        OutlinedTextField(
                            value = itemNoteInput,
                            onValueChange = onItemNoteChanged,
                            placeholder = {
                                Text(
                                    text = "مقدار/ملاحظة...",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(0.95f)
                                .onFocusChanged { isNoteFieldFocused = it.isFocused }
                                .testTag("add_item_note_input"),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    onSubmitCustomItem()
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                            )
                        )

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSubmitCustomItem()
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            },
                            enabled = itemNameInput.trim().isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("submit_add_item_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "إضافة",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "أضف",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Smart 1-Tap Unit Chips while typing a new item (e.g. "أتاي" -> علبة، "جافيل" -> قرعة، لتر)
                    if (itemNameInput.trim().isNotEmpty() || isTypingMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مقدار سريع:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            typedInputSmartUnits.forEach { unitChip ->
                                val isUnitSelected = itemNoteInput.trim() == unitChip
                                Surface(
                                    modifier = Modifier
                                        .heightIn(min = 34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onItemNoteChanged(if (isUnitSelected) "" else unitChip)
                                        },
                                    color = if (isUnitSelected) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (isUnitSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = unitChip,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isUnitSelected) {
                                                MaterialTheme.colorScheme.onSecondary
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
                    }
                }
            }
        }
    }
}
