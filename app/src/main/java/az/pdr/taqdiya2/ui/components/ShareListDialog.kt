package az.pdr.taqdiya2.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import az.pdr.taqdiya2.data.local.ShoppingItemEntity
import az.pdr.taqdiya2.data.local.VendorEntity
import az.pdr.taqdiya2.util.CurrencyFormatter

@Composable
fun ShareListDialog(
    vendors: List<VendorEntity>,
    shoppingItems: List<ShoppingItemEntity>,
    onDismiss: () -> Unit,
    onMessageCopied: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedVendorId by remember { mutableStateOf<Long?>(null) }

    val activeMissingItems = remember(shoppingItems) {
        shoppingItems.filter { !it.isDeleted && !it.isPurchased }
    }

    val visibleItemsForVendor = remember(activeMissingItems, selectedVendorId) {
        if (selectedVendorId == null) activeMissingItems
        else activeMissingItems.filter { it.vendorId == selectedVendorId }
    }

    var checkedItemIds by remember(activeMissingItems) {
        mutableStateOf(activeMissingItems.map { it.id }.toSet())
    }

    val autoFormattedText = remember(vendors, shoppingItems, selectedVendorId, checkedItemIds) {
        CurrencyFormatter.buildShareText(
            vendors = vendors,
            items = shoppingItems,
            selectedVendorId = selectedVendorId,
            selectedItemIds = checkedItemIds
        )
    }

    var editableText by remember { mutableStateOf(autoFormattedText) }
    var isTextManuallyEdited by remember { mutableStateOf(false) }

    LaunchedEffect(autoFormattedText) {
        if (!isTextManuallyEdited) {
            editableText = autoFormattedText
        }
    }

    val vendorsWithItems = remember(vendors, activeMissingItems) {
        val activeVendorIds = activeMissingItems.map { it.vendorId }.toSet()
        vendors.filter { activeVendorIds.contains(it.id) }
    }

    val vendorMap = remember(vendors) {
        vendors.associateBy { it.id }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "مشاركة قائمة النواقص نصياً",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "حدد الأغراض التي تريد إرسالها، ويمكنك تعديل نص الرسالة بحرية قبل المشاركة:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 1. Vendor Filter Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedVendorId == null,
                        onClick = {
                            selectedVendorId = null
                            isTextManuallyEdited = false
                        },
                        label = { Text("القائمة كاملة") }
                    )
                    vendorsWithItems.forEach { vendor ->
                        FilterChip(
                            selected = selectedVendorId == vendor.id,
                            onClick = {
                                selectedVendorId = vendor.id
                                isTextManuallyEdited = false
                            },
                            label = { Text(vendor.name) }
                        )
                    }
                }

                // 2. Selective Item Checkboxes
                if (visibleItemsForVendor.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    val allVisibleSelected = visibleItemsForVendor.all { checkedItemIds.contains(it.id) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الأغراض المحددة (${visibleItemsForVendor.count { checkedItemIds.contains(it.id) }} من ${visibleItemsForVendor.size}):",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (allVisibleSelected) "إلغاء تحديد الكل" else "تحديد الكل",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val visibleIds = visibleItemsForVendor.map { it.id }.toSet()
                                    checkedItemIds = if (allVisibleSelected) {
                                        checkedItemIds - visibleIds
                                    } else {
                                        checkedItemIds + visibleIds
                                    }
                                    isTextManuallyEdited = false
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("share_toggle_all_items_button")
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 76.dp, max = 165.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 4.dp)
                        ) {
                            visibleItemsForVendor.forEach { item ->
                                val isChecked = checkedItemIds.contains(item.id)
                                val vName = vendorMap[item.vendorId]?.name
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            checkedItemIds = if (isChecked) {
                                                checkedItemIds - item.id
                                            } else {
                                                checkedItemIds + item.id
                                            }
                                            isTextManuallyEdited = false
                                        }
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            checkedItemIds = if (checked) {
                                                checkedItemIds + item.id
                                            } else {
                                                checkedItemIds - item.id
                                            }
                                            isTextManuallyEdited = false
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (item.note.isNotBlank()) "${item.name} (${item.note})" else item.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (selectedVendorId == null && vName != null) {
                                            Text(
                                                text = vName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Editable Share Message Text Field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "نص الرسالة (قابل للتعديل):",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    if (isTextManuallyEdited) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    isTextManuallyEdited = false
                                    editableText = autoFormattedText
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "إعادة توليد النص",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = editableText,
                    onValueChange = {
                        editableText = it
                        isTextManuallyEdited = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    minLines = 4,
                    maxLines = 7,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editable_share_message_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("قائمة التقدية", editableText)
                            clipboard.setPrimaryClip(clip)
                            onMessageCopied("تم نسخ قائمة التقدية للحافظة")
                            onDismiss()
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_share_text_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "نسخ النص",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "نسخ النص",
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Button(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, editableText)
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "مشاركة قائمة التقدية عبر")
                            context.startActivity(shareIntent)
                            onDismiss()
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("send_whatsapp_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "إرسال عبر واتساب",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "مشاركة / واتساب",
                            maxLines = 1,
                            softWrap = false
                        )
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

