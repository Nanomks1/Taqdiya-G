package az.pdr.taqdiya2.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CallMerge
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import az.pdr.taqdiya2.data.local.DictionaryItemEntity
import az.pdr.taqdiya2.data.local.VendorEntity
import az.pdr.taqdiya2.ui.viewmodel.UndoAction

private enum class DictDialogMode {
    LIST,
    ADD_OR_EDIT,
    MERGE
}

@Composable
fun DictionaryDialog(
    dictionaryItems: List<DictionaryItemEntity>,
    vendors: List<VendorEntity>,
    activeUndo: UndoAction?,
    onPerformUndo: (UndoAction) -> Unit,
    onDismissUndo: () -> Unit,
    onAddEntry: (String, Long, String) -> Unit,
    onRenameEntry: (Long, String, Long, String) -> Unit,
    onMergeEntries: (DictionaryItemEntity, DictionaryItemEntity) -> Unit,
    onDeleteEntry: (DictionaryItemEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var mode by remember { mutableStateOf(DictDialogMode.LIST) }
    var searchQuery by remember { mutableStateOf("") }

    var editingEntry by remember { mutableStateOf<DictionaryItemEntity?>(null) }
    var nameInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var selectedVendorId by remember {
        mutableStateOf(vendors.firstOrNull()?.id ?: 1L)
    }

    var mergeSource by remember { mutableStateOf<DictionaryItemEntity?>(null) }
    var mergeTarget by remember { mutableStateOf<DictionaryItemEntity?>(null) }

    val vendorMap = remember(vendors) { vendors.associateBy { it.id } }

    val filteredItems = remember(dictionaryItems, searchQuery) {
        if (searchQuery.isBlank()) {
            dictionaryItems
        } else {
            val q = searchQuery.trim()
            dictionaryItems.filter { it.name.contains(q, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Column {
                Text(
                    text = "إدارة قاموس الأغراض الذكي",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (mode == DictDialogMode.LIST) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                editingEntry = null
                                nameInput = ""
                                noteInput = ""
                                selectedVendorId = vendors.firstOrNull()?.id ?: 1L
                                mode = DictDialogMode.ADD_OR_EDIT
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dict_add_new_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "إضافة غرض للقاموس",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة غرض")
                        }

                        OutlinedButton(
                            onClick = {
                                mergeSource = dictionaryItems.firstOrNull { it.name == "لوبيا بيضاء" }
                                    ?: dictionaryItems.getOrNull(1)
                                mergeTarget = dictionaryItems.firstOrNull { it.name == "لوبيا" }
                                    ?: dictionaryItems.firstOrNull()
                                mode = DictDialogMode.MERGE
                            },
                            enabled = dictionaryItems.size >= 2,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dict_open_merge_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CallMerge,
                                contentDescription = "دمج اسمين لغرض واحد",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("دمج اسمين")
                        }
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                InlineUndoBanner(
                    activeUndo = activeUndo,
                    onPerformUndo = onPerformUndo,
                    onDismiss = onDismissUndo
                )

                when (mode) {
                    DictDialogMode.LIST -> {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("ابحث في القاموس (مثال: لوبيا، مطيشة...)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "بحث في القاموس"
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dict_search_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredItems, key = { it.id }) { item ->
                                val vendorName = vendorMap[item.defaultVendorId]?.name ?: "بائع عام"
                                OutlinedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "البائع التلقائي: $vendorName • التكرار: ${item.usageCount}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    mergeSource = item
                                                    mergeTarget = dictionaryItems.firstOrNull { it.id != item.id }
                                                    mode = DictDialogMode.MERGE
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.CallMerge,
                                                    contentDescription = "دمج مع غرض آخر",
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    editingEntry = item
                                                    nameInput = item.name
                                                    noteInput = item.defaultNote
                                                    selectedVendorId = item.defaultVendorId
                                                    mode = DictDialogMode.ADD_OR_EDIT
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Edit,
                                                    contentDescription = "إعادة تسمية أو تعديل",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteEntry(item) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.DeleteOutline,
                                                    contentDescription = "حذف من القاموس",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    DictDialogMode.ADD_OR_EDIT -> {
                        Text(
                            text = if (editingEntry == null) {
                                "إضافة سلعة جديدة للقاموس الذكي"
                            } else {
                                "إعادة تسمية السلعة أو تعديل بائعها التلقائي"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("اسم السلعة") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dict_entry_name_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = noteInput,
                            onValueChange = { noteInput = it },
                            label = { Text("ملاحظة افتراضية (اختياري)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "البائع المخصص تلقائياً لهذه السلعة:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            vendors.forEach { vendor ->
                                FilterChip(
                                    selected = selectedVendorId == vendor.id,
                                    onClick = { selectedVendorId = vendor.id },
                                    label = { Text(vendor.name) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { mode = DictDialogMode.LIST }) {
                                Text("رجوع للقاموس")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val clean = nameInput.trim()
                                    if (clean.isNotEmpty()) {
                                        if (editingEntry == null) {
                                            onAddEntry(clean, selectedVendorId, noteInput)
                                        } else {
                                            onRenameEntry(
                                                editingEntry!!.id,
                                                clean,
                                                selectedVendorId,
                                                noteInput
                                            )
                                        }
                                        mode = DictDialogMode.LIST
                                    }
                                },
                                modifier = Modifier.testTag("dict_save_entry_button")
                            ) {
                                Text("حفظ في القاموس")
                            }
                        }
                    }

                    DictDialogMode.MERGE -> {
                        Text(
                            text = "دمج اسمين لسلعة واحدة (مثل «لوبيا» و«لوبيا بيضاء»)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "اختر الاسم القديم المراد دمجه، ثم اختر الاسم المعتمد الذي سيبقى في القاموس وتتوحد تحته جميع السجلات:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "1. الاسم الفرعي المراد دمجه وإزالته:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            dictionaryItems.forEach { entry ->
                                FilterChip(
                                    selected = mergeSource?.id == entry.id,
                                    onClick = {
                                        mergeSource = entry
                                        if (mergeTarget?.id == entry.id) {
                                            mergeTarget = dictionaryItems.firstOrNull { it.id != entry.id }
                                        }
                                    },
                                    label = { Text(entry.name) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "2. الاسم الأساسي المعتمد (الذي سيبقى):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            dictionaryItems.filter { it.id != mergeSource?.id }.forEach { entry ->
                                FilterChip(
                                    selected = mergeTarget?.id == entry.id,
                                    onClick = { mergeTarget = entry },
                                    label = { Text(entry.name) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val src = mergeSource
                        val dst = mergeTarget
                        if (src != null && dst != null && src.id != dst.id) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "معاينة عملية الدمج:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "سيتم تحويل «${src.name}» إلى «${dst.name}» وجمع عداد استخدامهما معاً (${src.usageCount + dst.usageCount} مرات).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { mode = DictDialogMode.LIST }) {
                                Text("رجوع")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val s = mergeSource
                                    val t = mergeTarget
                                    if (s != null && t != null && s.id != t.id) {
                                        onMergeEntries(s, t)
                                        mode = DictDialogMode.LIST
                                    }
                                },
                                enabled = src != null && dst != null && src.id != dst.id,
                                modifier = Modifier.testTag("dict_confirm_merge_button")
                            ) {
                                Text("تأكيد الدمج")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            if (mode == DictDialogMode.LIST) {
                TextButton(onClick = onDismiss) {
                    Text("إغلاق")
                }
            }
        }
    )
}
