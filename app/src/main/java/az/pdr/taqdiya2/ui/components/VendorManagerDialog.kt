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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import az.pdr.taqdiya2.data.local.VendorEntity

@Composable
fun VendorManagerDialog(
    vendors: List<VendorEntity>,
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
                        Text("بائع جديد")
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isAddingNew || editingVendor != null) {
                    Text(
                        text = if (isAddingNew) "إضافة بائع جديد" else "تعديل بيانات البائع",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("اسم البائع (مثال: مول الحوت، بائع السوق...)") },
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
                        text = "رمز البائع:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(vendors, key = { _, v -> v.id }) { index, vendor ->
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
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = VendorIconMapper.getIcon(vendor.iconKey),
                                            contentDescription = vendor.name,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = vendor.name,
                                                style = MaterialTheme.typography.titleSmall,
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

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { onMoveVendor(vendor.id, true) },
                                            enabled = index > 0
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.ArrowUpward,
                                                contentDescription = "تحريك للأعلى",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onMoveVendor(vendor.id, false) },
                                            enabled = index < vendors.lastIndex
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.ArrowDownward,
                                                contentDescription = "تحريك للأسفل",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                editingVendor = vendor
                                                nameInput = vendor.name
                                                specialtyInput = vendor.specialty
                                                selectedIconKey = vendor.iconKey
                                                isAddingNew = false
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Edit,
                                                contentDescription = "تعديل البائع",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteVendor(vendor) },
                                            enabled = vendors.size > 1
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.DeleteOutline,
                                                contentDescription = "حذف البائع",
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
