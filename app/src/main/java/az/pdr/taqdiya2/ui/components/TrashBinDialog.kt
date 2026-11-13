package az.pdr.taqdiya2.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import az.pdr.taqdiya2.data.local.PassingExpenseEntity
import az.pdr.taqdiya2.data.local.ShoppingItemEntity
import az.pdr.taqdiya2.data.local.VendorEntity
import az.pdr.taqdiya2.util.CurrencyFormatter
import kotlin.math.ceil

@Composable
fun TrashBinDialog(
    deletedItems: List<ShoppingItemEntity>,
    deletedExpenses: List<PassingExpenseEntity>,
    deletedVendors: List<VendorEntity>,
    vendors: List<VendorEntity>,
    onRestoreShoppingItem: (ShoppingItemEntity) -> Unit,
    onRestorePassingExpense: (PassingExpenseEntity) -> Unit,
    onRestoreVendor: (VendorEntity) -> Unit,
    onPermanentDeleteShoppingItem: (ShoppingItemEntity) -> Unit,
    onPermanentDeletePassingExpense: (PassingExpenseEntity) -> Unit,
    onPermanentDeleteVendor: (VendorEntity) -> Unit,
    onEmptyAllTrash: () -> Unit,
    onDismiss: () -> Unit
) {
    var confirmEmptyAll by remember { mutableStateOf(false) }
    val totalCount = deletedItems.size + deletedExpenses.size + deletedVendors.size
    val vendorMap = remember(vendors, deletedVendors) {
        (vendors + deletedVendors).associateBy { it.id }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 20.dp),
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "سلة المحذوفات ($totalCount)",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "تُحفظ العناصر المحذوفة لمدة 30 يوماً قبل الحذف النهائي التلقائي",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (totalCount == 0) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "سلة المحذوفات فارغة ونظيفة",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "أي غرض أو مصروف أو بائع تحذفه سيظهر هنا لمدة 30 يوماً لتتمكن من استرجاعه في أي وقت.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    if (confirmEmptyAll) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "هل أنت متأكد من تفريغ سلة المحذوفات بالكامل؟ لا يمكن التراجع بعد الحذف النهائي.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { confirmEmptyAll = false },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("إلغاء")
                                    }
                                    Button(
                                        onClick = {
                                            confirmEmptyAll = false
                                            onEmptyAllTrash()
                                        },
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .testTag("confirm_empty_trash_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "نعم، تفريغ السلة نهائياً",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { confirmEmptyAll = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("request_empty_trash_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تفريغ سلة المحذوفات بالكامل ($totalCount)",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 390.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (deletedItems.isNotEmpty()) {
                            item(key = "header_items") {
                                Text(
                                    text = "الأغراض المحذوفة (${deletedItems.size}):",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(deletedItems, key = { "trash_item_${it.id}" }) { item ->
                                val vName = vendorMap[item.vendorId]?.name ?: "البائع"
                                val subtitle = buildString {
                                    append("البائع: $vName")
                                    if (item.note.isNotBlank()) {
                                        append(" • ${item.note}")
                                    }
                                    append(" • ${formatRemainingDays(item.deletedAt)}")
                                }
                                TrashEntryCard(
                                    title = item.name,
                                    subtitle = subtitle,
                                    onRestore = { onRestoreShoppingItem(item) },
                                    onPermanentDelete = { onPermanentDeleteShoppingItem(item) },
                                    restoreTestTag = "restore_trash_item_${item.id}",
                                    deleteTestTag = "perm_delete_trash_item_${item.id}"
                                )
                            }
                        }

                        if (deletedExpenses.isNotEmpty()) {
                            item(key = "header_expenses") {
                                Text(
                                    text = "المصاريف العابرة المحذوفة (${deletedExpenses.size}):",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(deletedExpenses, key = { "trash_exp_${it.id}" }) { expense ->
                                TrashEntryCard(
                                    title = "${expense.title} (${CurrencyFormatter.formatDirhams(expense.amountDirhams)})",
                                    subtitle = "مصروف عابر • ${formatRemainingDays(expense.deletedAt)}",
                                    onRestore = { onRestorePassingExpense(expense) },
                                    onPermanentDelete = { onPermanentDeletePassingExpense(expense) },
                                    restoreTestTag = "restore_trash_expense_${expense.id}",
                                    deleteTestTag = "perm_delete_trash_expense_${expense.id}"
                                )
                            }
                        }

                        if (deletedVendors.isNotEmpty()) {
                            item(key = "header_vendors") {
                                Text(
                                    text = "البائعون المحذوفون (${deletedVendors.size}):",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(deletedVendors, key = { "trash_vendor_${it.id}" }) { vendor ->
                                TrashEntryCard(
                                    title = vendor.name,
                                    subtitle = "بائع • ${formatRemainingDays(vendor.deletedAt)}",
                                    onRestore = { onRestoreVendor(vendor) },
                                    onPermanentDelete = { onPermanentDeleteVendor(vendor) },
                                    restoreTestTag = "restore_trash_vendor_${vendor.id}",
                                    deleteTestTag = "perm_delete_trash_vendor_${vendor.id}"
                                )
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

@Composable
private fun TrashEntryCard(
    title: String,
    subtitle: String,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit,
    restoreTestTag: String,
    deleteTestTag: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onRestore,
                    modifier = Modifier
                        .height(40.dp)
                        .testTag(restoreTestTag),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RestoreFromTrash,
                        contentDescription = "استرجاع",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "استرجاع",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onPermanentDelete,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag(deleteTestTag)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteForever,
                        contentDescription = "حذف نهائي",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun formatRemainingDays(deletedAt: Long?): String {
    if (deletedAt == null) return "متبقي 30 يوماً"
    val elapsedMs = (System.currentTimeMillis() - deletedAt).coerceAtLeast(0L)
    val thirtyDaysMs = 30L * 24L * 60L * 60L * 1000L
    val remainingMs = (thirtyDaysMs - elapsedMs).coerceAtLeast(0L)
    val remainingDays = ceil(remainingMs.toDouble() / (24.0 * 60.0 * 60.0 * 1000.0)).toInt().coerceIn(1, 30)
    return "متبقي $remainingDays يوم"
}
