package az.pdr.taqdiya2.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import az.pdr.taqdiya2.data.local.PassingExpenseEntity
import az.pdr.taqdiya2.data.local.ShoppingItemEntity
import az.pdr.taqdiya2.data.local.ShoppingRoundEntity
import az.pdr.taqdiya2.data.local.VendorEntity
import az.pdr.taqdiya2.ui.components.ConfirmVendorPaymentSheet
import az.pdr.taqdiya2.ui.components.VendorIconMapper
import az.pdr.taqdiya2.ui.components.VendorPendingBalanceSheet
import az.pdr.taqdiya2.ui.viewmodel.HomePrepUiState
import az.pdr.taqdiya2.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun PocketAuditScreen(
    uiState: HomePrepUiState,
    onOpenPassingExpenseClick: () -> Unit,
    onDeletePassingExpense: (PassingExpenseEntity) -> Unit,
    onConfirmVendorPayment: (
        vendor: VendorEntity,
        calculatedTotalDirhams: Double,
        actualPaidDirhams: Double,
        forgivenDiscountDirhams: Double,
        newVendorPendingBalanceDirhams: Double
    ) -> Unit,
    onUpdateVendorPendingBalance: (VendorEntity, Double) -> Unit,
    onFinishAndArchiveRound: (
        newPocketDirhams: Double,
        newRoundTitle: String,
        rolloverUnpurchasedItems: Boolean
    ) -> Unit,
    onLoadArchivedRoundItems: suspend (Long) -> List<ShoppingItemEntity>,
    onDeleteArchivedRound: (ShoppingRoundEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var paymentDialogVendor by remember { mutableStateOf<VendorEntity?>(null) }
    var pendingBalanceDialogVendor by remember { mutableStateOf<VendorEntity?>(null) }
    var showFinishRoundSheet by remember { mutableStateOf(false) }
    var viewingArchivedRound by remember { mutableStateOf<ShoppingRoundEntity?>(null) }

    val activeItems = remember(uiState.shoppingItems) {
        uiState.shoppingItems.filter { !it.isDeleted }
    }
    val purchasedItems = remember(activeItems) {
        activeItems.filter { it.isPurchased }
    }
    val waitingItems = remember(activeItems) {
        activeItems.filter { !it.isPurchased }
    }

    // Vendors that either have items in this round, a payment in this round, or a non-zero pending balance
    val auditVendors = remember(uiState.vendors, activeItems, uiState.vendorPayments) {
        val itemVendorIds = activeItems.map { it.vendorId }.toSet()
        val paidVendorIds = uiState.vendorPayments.map { it.vendorId }.toSet()
        uiState.vendors.filter { v ->
            itemVendorIds.contains(v.id) ||
                paidVendorIds.contains(v.id) ||
                abs(v.pendingBalanceDirhams) > 0.009
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Main Pocket Financial Audit Card (كشف جرد الجيب)
        item(key = "pocket_audit_summary_card") {
            PocketLedgerSummaryCard(
                uiState = uiState,
                purchasedItemsCount = purchasedItems.size,
                waitingItemsCount = waitingItems.size,
                onOpenFinishRound = { showFinishRoundSheet = true }
            )
        }

        // 2. Quick Passing Expenses Section (+ مصروف عابر خارج البائعين)
        item(key = "passing_expenses_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "المصاريف العابرة (${uiState.passingExpenses.size}):",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "خبز، طاكسي، أكياس، أو أي مصروف خارج البائعين",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = onOpenPassingExpenseClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("audit_add_passing_expense_button")
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "مصروف عابر",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        if (uiState.passingExpenses.isEmpty()) {
            item(key = "passing_expenses_empty") {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "لا توجد مصاريف عابرة مسجلة في هذه الجولة بعد.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uiState.passingExpenses, key = { "expense_${it.id}" }) { expense ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = expense.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = CurrencyFormatter.formatRials(expense.amountDirhams),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = CurrencyFormatter.formatDirhams(expense.amountDirhams),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { onDeletePassingExpense(expense) },
                                modifier = Modifier.testTag("delete_passing_expense_${expense.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "حذف ${expense.title}",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Vendor-by-Vendor Breakdown & Pending Change ("بقا ليا عندو / بقا ليه عندي")
        if (auditVendors.isNotEmpty()) {
            item(key = "vendor_breakdown_header") {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "كشف حساب البائعين والصرف العالق:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            items(auditVendors, key = { "audit_vendor_${it.id}" }) { vendor ->
                val vPurchased = purchasedItems.filter { it.vendorId == vendor.id }
                val vWaiting = waitingItems.filter { it.vendorId == vendor.id }
                val vCalcTotal = vPurchased.sumOf { it.totalDirhams }
                val vPayment = uiState.vendorPayments.firstOrNull { it.vendorId == vendor.id }
                val vActualPaid = vPayment?.actualPaidDirhams ?: vCalcTotal
                val hasPending = abs(vendor.pendingBalanceDirhams) > 0.009

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(18.dp)
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = VendorIconMapper.getIcon(vendor.iconKey),
                                        contentDescription = vendor.name,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = vendor.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "تم شراء ${vPurchased.size} أغراض" +
                                            if (vWaiting.isNotEmpty()) " (بقي ${vWaiting.size} في الانتظار)" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = CurrencyFormatter.formatDirhams(vActualPaid),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = CurrencyFormatter.formatRials(vActualPaid),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        if (vPayment != null && vPayment.forgivenDiscountDirhams > 0.0) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "حساب الميزان كان ${CurrencyFormatter.formatDirhams(vPayment.calculatedTotalDirhams)} — سامح البائع في ${CurrencyFormatter.formatDirhams(vPayment.forgivenDiscountDirhams)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        if (hasPending) {
                            val isVendorOwingMe = vendor.pendingBalanceDirhams > 0.0
                            val absPending = abs(vendor.pendingBalanceDirhams)
                            Surface(
                                color = if (isVendorOwingMe) {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.errorContainer
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isVendorOwingMe) {
                                            "بقا ليا عندو: ${CurrencyFormatter.formatDirhams(absPending)} (${CurrencyFormatter.formatRials(absPending)})"
                                        } else {
                                            "بقا ليه عندي: ${CurrencyFormatter.formatDirhams(absPending)} (${CurrencyFormatter.formatRials(absPending)})"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isVendorOwingMe) {
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        },
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { paymentDialogVendor = vendor },
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Payments,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (vPayment != null) "تعديل الدفع" else "تأكيد الدفع",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { pendingBalanceDialogVendor = vendor },
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Handshake,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (hasPending) "تصفية الصرف" else "صرف عالق",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Archived Shopping Rounds Section (أرشيف وسجل الجولات السابقة)
        item(key = "archived_rounds_header") {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "أرشيف الجولات السابقة (${uiState.archivedRounds.size}):",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (uiState.archivedRounds.isEmpty()) {
            item(key = "archived_rounds_empty") {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "لا توجد جولات مؤرشفة بعد. عند الانتهاء من التقدية والعودة للبيت، اضغط على «إنهاء الجولة وحفظها في الأرشيف» ليُحفظ سجل تقديتك هنا.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(uiState.archivedRounds, key = { "archived_round_${it.id}" }) { round ->
                val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault()) }
                val dateStr = dateFormat.format(Date(round.completedAt ?: round.createdAt))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { viewingArchivedRound = round }
                        .testTag("archived_round_card_${round.id}"),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = round.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$dateStr • ${round.archivedItemsCount} أغراض مشتراة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "خرجت بـ ${CurrencyFormatter.formatDirhams(round.initialPocketDirhams)} | صرفت ${CurrencyFormatter.formatDirhams(round.archivedTotalSpentDirhams)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { onDeleteArchivedRound(round) },
                            modifier = Modifier.testTag("delete_archived_round_${round.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "حذف الجولة من الأرشيف",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirm Vendor Payment Sheet
    val targetPaymentVendor = paymentDialogVendor
    if (targetPaymentVendor != null) {
        val vCalc = purchasedItems.filter { it.vendorId == targetPaymentVendor.id }.sumOf { it.totalDirhams }
        val vExisting = uiState.vendorPayments.firstOrNull { it.vendorId == targetPaymentVendor.id }
        ConfirmVendorPaymentSheet(
            vendor = targetPaymentVendor,
            calculatedItemsTotalDirhams = vCalc,
            existingPayment = vExisting,
            onConfirmPayment = { calcTotal, actualPaid, forgiven, newPending ->
                onConfirmVendorPayment(
                    targetPaymentVendor,
                    calcTotal,
                    actualPaid,
                    forgiven,
                    newPending
                )
            },
            onDismiss = { paymentDialogVendor = null }
        )
    }

    // Vendor Pending Balance Sheet
    val targetPendingVendor = pendingBalanceDialogVendor
    if (targetPendingVendor != null) {
        VendorPendingBalanceSheet(
            vendor = targetPendingVendor,
            onSavePendingBalance = { newPending ->
                onUpdateVendorPendingBalance(targetPendingVendor, newPending)
            },
            onDismiss = { pendingBalanceDialogVendor = null }
        )
    }

    // Finish & Archive Round Sheet
    if (showFinishRoundSheet) {
        FinishRoundSheet(
            remainingPocketDirhams = uiState.remainingPocketDirhams,
            waitingItemsCount = waitingItems.size,
            purchasedItemsCount = purchasedItems.size,
            totalActualSpentDirhams = uiState.totalActualSpentDirhams,
            onConfirmFinishRound = { newPocketDh, newTitle, rollover ->
                onFinishAndArchiveRound(newPocketDh, newTitle, rollover)
            },
            onDismiss = { showFinishRoundSheet = false }
        )
    }

    // View Archived Round Details Dialog
    val roundToView = viewingArchivedRound
    if (roundToView != null) {
        var archivedItems by remember(roundToView.id) { mutableStateOf<List<ShoppingItemEntity>>(emptyList()) }
        LaunchedEffect(roundToView.id) {
            archivedItems = onLoadArchivedRoundItems(roundToView.id)
        }

        AlertDialog(
            onDismissRequest = { viewingArchivedRound = null },
            containerColor = MaterialTheme.colorScheme.background,
            title = {
                Text(
                    text = roundToView.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "ميزانية البداية: ${CurrencyFormatter.formatDirhams(roundToView.initialPocketDirhams)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "مجموع ما صُرف: ${CurrencyFormatter.formatDirhams(roundToView.archivedTotalSpentDirhams)} (${CurrencyFormatter.formatRials(roundToView.archivedTotalSpentDirhams)})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            if (roundToView.archivedSavingsDirhams > 0.0) {
                                Text(
                                    text = "توفير المسامحة فالصرف: ${CurrencyFormatter.formatDirhams(roundToView.archivedSavingsDirhams)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    val boughtInArchive = archivedItems.filter { it.isPurchased }
                    if (boughtInArchive.isEmpty()) {
                        Text(
                            text = "لا توجد أغراض مشتراة مسجلة في هذه الجولة.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "الأغراض المشتراة (${boughtInArchive.size}):",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        boughtInArchive.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• ${item.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = CurrencyFormatter.formatDirhams(item.totalDirhams),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewingArchivedRound = null }) {
                    Text("إغلاق")
                }
            }
        )
    }
}

@Composable
private fun PocketLedgerSummaryCard(
    uiState: HomePrepUiState,
    purchasedItemsCount: Int,
    waitingItemsCount: Int,
    onOpenFinishRound: () -> Unit
) {
    val isOverBudget = uiState.netPocketBalanceDirhams < -0.009

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
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
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.secondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "كشف جرد الجيب",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "اشتريت $purchasedItemsCount أغراض • بقي $waitingItemsCount في القائمة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // 3 Financial Rows: Initial Pocket -> Total Spent -> Remaining in Pocket
            Surface(
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row 1: Initial Pocket
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "خرجت من البيت بـ:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "${CurrencyFormatter.formatDirhams(uiState.initialPocketDirhams)} (${CurrencyFormatter.formatRials(uiState.initialPocketDirhams)})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Row 2: Total Actual Spent
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "مجموع ما صرفته فعلياً:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                            )
                            if (uiState.totalPassingExpensesDirhams > 0.0) {
                                Text(
                                    text = "البائعين: ${CurrencyFormatter.formatDirhams(uiState.totalActualVendorPaidDirhams)} + عابر: ${CurrencyFormatter.formatDirhams(uiState.totalPassingExpensesDirhams)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                                )
                            }
                        }
                        Text(
                            text = "${CurrencyFormatter.formatDirhams(uiState.totalActualSpentDirhams)} (${CurrencyFormatter.formatRials(uiState.totalActualSpentDirhams)})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("audit_total_spent_text")
                        )
                    }

                    // Optional Row: Savings from Forgiven Small Change
                    if (uiState.totalForgivenSavingsDirhams > 0.0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Savings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "توفير المسامحة في الصرف:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "+ ${CurrencyFormatter.formatDirhams(uiState.totalForgivenSavingsDirhams)} (${CurrencyFormatter.formatRials(uiState.totalForgivenSavingsDirhams)})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))

                    // Row 3: Expected Remaining in Pocket
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isOverBudget) "تجاوزت ميزانية الجيب بـ:" else "الباقي المفترض في جيبك الآن:",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = CurrencyFormatter.formatRials(abs(uiState.netPocketBalanceDirhams)),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Surface(
                            color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = CurrencyFormatter.formatDirhams(abs(uiState.netPocketBalanceDirhams)),
                                style = MaterialTheme.typography.titleLarge,
                                color = if (isOverBudget) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onTertiary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                                    .testTag("audit_remaining_pocket_text")
                            )
                        }
                    }
                }
            }

            // Finish & Archive Round Button
            Button(
                onClick = onOpenFinishRound,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("open_finish_round_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Outlined.Archive, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "إنهاء الجولة وحفظها في الأرشيف",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinishRoundSheet(
    remainingPocketDirhams: Double,
    waitingItemsCount: Int,
    purchasedItemsCount: Int,
    totalActualSpentDirhams: Double,
    onConfirmFinishRound: (
        newPocketDirhams: Double,
        newRoundTitle: String,
        rolloverUnpurchasedItems: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var rolloverWaitingItems by remember { mutableStateOf(true) }
    var newRoundTitle by remember { mutableStateOf("جولة التقدية الحالية") }
    var nextPocketBudget by remember {
        mutableDoubleStateOf(
            if (remainingPocketDirhams > 0.0) remainingPocketDirhams else 200.0
        )
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "إنهاء الجولة وحفظ الأرشيف",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "إغلاق")
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "ملخص الجولة التي ستُحفظ في الأرشيف:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• عدد الأغراض المشتراة: $purchasedItemsCount أغراض",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• مجموع المصروف الفعلي: ${CurrencyFormatter.formatDirhams(totalActualSpentDirhams)} (${CurrencyFormatter.formatRials(totalActualSpentDirhams)})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (waitingItemsCount > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ترحيل النواقص المتبقية ($waitingItemsCount أغراض)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "نقل الأغراض التي لم تشترِها اليوم تلقائياً إلى الجولة الجديدة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Switch(
                            checked = rolloverWaitingItems,
                            onCheckedChange = { rolloverWaitingItems = it }
                        )
                    }
                }
            }

            Text(
                text = "ميزانية الجيب للجولة القادمة:",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            val budgetPresets = remember(remainingPocketDirhams) {
                val list = mutableListOf<Pair<String, Double>>()
                if (remainingPocketDirhams > 0.0) {
                    list.add("الباقي في الجيب (${CurrencyFormatter.formatDirhams(remainingPocketDirhams)})" to remainingPocketDirhams)
                }
                listOf(100.0, 150.0, 200.0, 300.0, 500.0).forEach { dh ->
                    if (abs(dh - remainingPocketDirhams) > 0.009) {
                        list.add(CurrencyFormatter.formatDirhams(dh) to dh)
                    }
                }
                list
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                budgetPresets.forEach { (label, dhValue) ->
                    val isSelected = abs(nextPocketBudget - dhValue) < 0.009
                    Surface(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { nextPocketBudget = dhValue },
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = newRoundTitle,
                onValueChange = { newRoundTitle = it },
                label = { Text("عنوان الجولة القادمة") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    onConfirmFinishRound(
                        nextPocketBudget,
                        newRoundTitle.trim().ifEmpty { "جولة التقدية الحالية" },
                        rolloverWaitingItems
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("confirm_finish_round_submit_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تأكيد الأرشفة وبدء جولة جديدة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
