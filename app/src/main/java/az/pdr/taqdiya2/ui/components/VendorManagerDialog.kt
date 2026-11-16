package az.pdr.taqdiya2.ui.components

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.window.DialogProperties
import az.pdr.taqdiya2.data.local.VendorEntity
import az.pdr.taqdiya2.ui.viewmodel.UndoAction

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VendorManagerDialog(
    vendors: List<VendorEntity>,
    activeUndo: UndoAction?,
    onPerformUndo: (UndoAction) -> Unit,
    onDismissUndo: () -> Unit,
    onAddVendor: (String, String, String) -> Unit,
    onUpdateVendor: (VendorEntity, String, String, String) -> Unit,
    onMoveVendor: (Long, Boolean) -> Unit,
    onDeleteVendor: (VendorEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var editingVendor by remember { mutableStateOf<VendorEntity?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }

    var nameInput by remember { mutableStateOf("") }
    var specialtyInput by remember { mutableStateOf("") }
    var selectedIconKey by remember { mutableStateOf("store") }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 16.dp),
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "إدارة قائمة البائعين",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (!isAddingNew && editingVendor == null) {
                    TextButton(
                        onClick = {
                            nameInput = ""
                            specialtyInput = ""
                            selectedIconKey = "store"
                            isAddingNew = true
                        },
                        modifier = Modifier.testTag("start_add_vendor_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "إضافة بائع"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("بائع جديد", maxLines = 1, softWrap = false)
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

                if (isAddingNew || editingVendor != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = if (isAddingNew) "إضافة بائع جديد" else "تعديل بيانات البائع",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = {
                                nameInput = it
                                if (isAddingNew) {
                                    selectedIconKey = VendorIconMapper.inferIconKeyFromName(it, "store")
                                }
                            },
                            label = { Text("اسم البائع (مثال: مول البيض، مول الحوت...)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vendor_name_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = specialtyInput,
                            onValueChange = { specialtyInput = it },
                            label = { Text("وصف السلع (اختياري)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vendor_specialty_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "رمز وأيقونة البائع:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VendorIconMapper.availableIcons.forEach { option ->
                                FilterChip(
                                    selected = selectedIconKey == option.key,
                                    onClick = { selectedIconKey = option.key },
                                    label = { Text(option.label) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = option.icon,
                                            contentDescription = option.label,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                isAddingNew = false
                                editingVendor = null
                            }
                        ) {
                            Text("إلغاء")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val cleanName = nameInput.trim()
                                if (cleanName.isNotEmpty()) {
                                    if (isAddingNew) {
                                        onAddVendor(cleanName, specialtyInput, selectedIconKey)
                                    } else if (editingVendor != null) {
                                        onUpdateVendor(editingVendor!!, cleanName, specialtyInput, selectedIconKey)
                                    }
                                    isAddingNew = false
                                    editingVendor = null
                                }
                            },
                            modifier = Modifier.testTag("confirm_save_vendor_button")
                        ) {
                            Text("حفظ البائع")
                        }
                    }
                    }
                } else {
                    Text(
                        text = "يمكنك تعديل الأسماء أو الترتيب أو إضافة أي بائع عابر في السوق:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 390.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(vendors, key = { _, v -> v.id }) { index, vendor ->
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Top Row: Full width for Vendor Icon + Name + Specialty
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = VendorIconMapper.getIcon(vendor.iconKey, vendor.name),
                                            contentDescription = vendor.name,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = vendor.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (vendor.specialty.isNotBlank()) {
                                                Text(
                                                    text = vendor.specialty,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    // Bottom Row: Action Buttons aligned neatly without squeezing text
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { onMoveVendor(vendor.id, true) },
                                            enabled = index > 0,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.ArrowUpward,
                                                contentDescription = "تحريك للأعلى",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onMoveVendor(vendor.id, false) },
                                            enabled = index < vendors.lastIndex,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.ArrowDownward,
                                                contentDescription = "تحريك للأسفل",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                editingVendor = vendor
                                                nameInput = vendor.name
                                                specialtyInput = vendor.specialty
                                                selectedIconKey = VendorIconMapper.inferIconKeyFromName(vendor.name, vendor.iconKey)
                                                isAddingNew = false
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Edit,
                                                contentDescription = "تعديل البائع",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteVendor(vendor) },
                                            enabled = vendors.size > 1,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.DeleteOutline,
                                                contentDescription = "حذف البائع",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
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
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
