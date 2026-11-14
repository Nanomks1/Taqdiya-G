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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import az.pdr.taqdiya2.data.local.ShoppingItemEntity
import az.pdr.taqdiya2.data.local.VendorEntity
import az.pdr.taqdiya2.ui.components.ConfirmVendorPaymentSheet
import az.pdr.taqdiya2.ui.components.PriceTrendBadge
import az.pdr.taqdiya2.ui.components.VendorIconMapper
import az.pdr.taqdiya2.ui.components.VendorItemPricingSheet
import az.pdr.taqdiya2.ui.components.VendorPendingBalanceSheet
import az.pdr.taqdiya2.ui.viewmodel.HomePrepUiState
import az.pdr.taqdiya2.util.CurrencyFormatter
import az.pdr.taqdiya2.util.PriceEvaluation
import az.pdr.taqdiya2.util.PriceTrendAnalyzer
import kotlin.math.abs

@Composable
fun AtVendorScreen(
    uiState: HomePrepUiState,
    onSelectMarketVendor: (Long) -> Unit,
    onToggleKeepScreenAwake: () -> Unit,
    onConfirmItemPurchase: (
        item: ShoppingItemEntity,
        purchaseMode: String,
        unitPriceDirhams: Double,
        quantity: Double,
        cleaningFeeDirhams: Double,
        totalDirhams: Double
    ) -> Unit,
    onUnmarkItemPurchased: (ShoppingItemEntity) -> Unit,
    onDeleteItemClick: (ShoppingItemEntity) -> Unit,
    onConfirmVendorPayment: (
        vendor: VendorEntity,
        calculatedTotalDirhams: Double,
        actualPaidDirhams: Double,
        forgivenDiscountDirhams: Double,
        newVendorPendingBalanceDirhams: Double
    ) -> Unit,
    onResetVendorPaymentAndBasket: (VendorEntity, Boolean) -> Unit = { _, _ -> },
    onUpdateVendorPendingBalance: (VendorEntity, Double) -> Unit,
    onOpenPassingExpenseClick: () -> Unit,
    onOpenItemPriceHistory: (itemName: String, vendorId: Long, fallbackPriceDh: Double) -> Unit,
    onOpenQuickPadForVendor: (Long?) -> Unit,
    onReturnToHomePrep: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    // Keep screen awake while standing in the market if enabled
    DisposableEffect(uiState.keepScreenAwakeInMarket) {
        view.keepScreenOn = uiState.keepScreenAwakeInMarket
        onDispose {
            view.keepScreenOn = false
        }
    }

    var pricingItem by remember { mutableStateOf<ShoppingItemEntity?>(null) }
    var showConfirmVendorPayment by remember { mutableStateOf(false) }
    var showVendorPendingBalanceSheet by remember { mutableStateOf(false) }

    // Filter only vendors who have items in today's round
    val activeRoundItems = remember(uiState.shoppingItems) {
        uiState.shoppingItems.filter { !it.isDeleted }
    }

    val vendorsWithItems = remember(uiState.vendors, activeRoundItems) {
        val vendorIds = activeRoundItems.map { it.vendorId }.toSet()
        uiState.vendors.filter { vendorIds.contains(it.id) }
    }

    val selectedVendor: VendorEntity? = remember(vendorsWithItems, uiState.selectedMarketVendorId) {
        vendorsWithItems.firstOrNull { it.id == uiState.selectedMarketVendorId }
            ?: vendorsWithItems.firstOrNull()
    }

    val currentVendorItems = remember(activeRoundItems, selectedVendor) {
        if (selectedVendor == null) emptyList()
        else activeRoundItems.filter { it.vendorId == selectedVendor.id }
    }

    // Waiting (unpurchased) items on top, Purchased items on bottom
    val waitingItems = remember(currentVendorItems) {
        currentVendorItems.filter { !it.isPurchased }
    }
    val purchasedItems = remember(currentVendorItems) {
        currentVendorItems.filter { it.isPurchased }
    }

    val currentVendorCalculatedTotalDirhams = remember(purchasedItems) {
        purchasedItems.sumOf { it.totalDirhams }
    }

    val currentVendorConfirmedPayment = remember(uiState.vendorPayments, selectedVendor) {
        if (selectedVendor == null) null
        else uiState.vendorPayments.firstOrNull { it.vendorId == selectedVendor.id }
    }

    if (vendorsWithItems.isEmpty()) {
        // Empty State when no items have been added to today's round yet
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "لا توجد أغراض مسجلة للتسوق بعد",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "سجّل النواقص أولاً من شاشة التحضير في البيت أو افتح لوحة الإضافة اللمسية الآن ليظهر لك البائعون وأغراضهم هنا.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = { onOpenQuickPadForVendor(null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("فتح لوحة الإضافة السريعة باللمس", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onOpenPassingExpenseClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Outlined.ReceiptLong, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("+ تسجيل مصروف عابر (خبز، طاكسي...)", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onReturnToHomePrep,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("العودة لشاشة التحضير في البيت", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Main Scrollable Market Area
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Top Market Header: Active Vendors Strip + Passing Expense + Keep Screen Awake Toggle
                item(key = "market_vendor_selector") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "بائعو جولة اليوم (${vendorsWithItems.size}):",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Quick "+ مصروف عابر" Button
                                Surface(
                                    modifier = Modifier
                                        .heightIn(min = 36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(onClick = onOpenPassingExpenseClick)
                                        .testTag("market_passing_expense_button"),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ReceiptLong,
                                            contentDescription = "مصروف عابر",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "+ مصروف عابر",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                // Keep Screen Awake Toggle
                                Surface(
                                    modifier = Modifier
                                        .heightIn(min = 36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(onClick = onToggleKeepScreenAwake)
                                        .testTag("toggle_keep_screen_awake_button"),
                                    color = if (uiState.keepScreenAwakeInMarket) {
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (uiState.keepScreenAwakeInMarket) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.LightMode,
                                            contentDescription = "إضاءة الشاشة",
                                            tint = if (uiState.keepScreenAwakeInMarket) {
                                                MaterialTheme.colorScheme.onTertiaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (uiState.keepScreenAwakeInMarket) "مضاءة" else "عادية",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (uiState.keepScreenAwakeInMarket) {
                                                MaterialTheme.colorScheme.onTertiaryContainer
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Oversized Tactile Vendor Selector Cards (Adaptive height so text never clips on Large/Extra-Large fonts!)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            vendorsWithItems.forEach { vendor ->
                                val isSelected = selectedVendor?.id == vendor.id
                                val vItems = activeRoundItems.filter { it.vendorId == vendor.id }
                                val vBought = vItems.count { it.isPurchased }
                                val vTotalCount = vItems.size
                                val vAllDone = vBought == vTotalCount && vTotalCount > 0
                                val hasPendingChange = abs(vendor.pendingBalanceDirhams) > 0.009

                                Surface(
                                    modifier = Modifier
                                        .widthIn(min = 160.dp, max = 225.dp)
                                        .heightIn(min = 86.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSelectMarketVendor(vendor.id)
                                        }
                                        .testTag("market_vendor_tab_${vendor.id}"),
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                                        vAllDone -> MaterialTheme.colorScheme.tertiaryContainer
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.5.dp,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.secondary
                                            hasPendingChange -> MaterialTheme.colorScheme.secondary
                                            vAllDone -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.outline
                                        }
                                    ),
                                    shape = RoundedCornerShape(18.dp),
                                    shadowElevation = if (isSelected) 3.dp else 1.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = VendorIconMapper.getIcon(vendor.iconKey),
                                                contentDescription = vendor.name,
                                                tint = if (isSelected) {
                                                    MaterialTheme.colorScheme.secondary
                                                } else {
                                                    MaterialTheme.colorScheme.primary
                                                },
                                                modifier = Modifier.size(22.dp)
                                            )

                                            Surface(
                                                color = when {
                                                    vAllDone -> MaterialTheme.colorScheme.tertiary
                                                    isSelected -> MaterialTheme.colorScheme.secondary
                                                    else -> MaterialTheme.colorScheme.secondaryContainer
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "$vBought من $vTotalCount",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = when {
                                                        vAllDone -> MaterialTheme.colorScheme.onTertiary
                                                        isSelected -> MaterialTheme.colorScheme.onSecondary
                                                        else -> MaterialTheme.colorScheme.onSecondaryContainer
                                                    },
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

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
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Pending Change Reminder Banner ("بقا ليا عندو / بقا ليه عندي") if non-zero
                if (selectedVendor != null && abs(selectedVendor.pendingBalanceDirhams) > 0.009) {
                    item(key = "vendor_pending_balance_banner") {
                        val isVendorOwingMe = selectedVendor.pendingBalanceDirhams > 0.0
                        val absPending = abs(selectedVendor.pendingBalanceDirhams)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { showVendorPendingBalanceSheet = true }
                                .testTag("vendor_pending_balance_banner"),
                            color = if (isVendorOwingMe) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            },
                            border = BorderStroke(
                                width = 1.5.dp,
                                color = if (isVendorOwingMe) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Handshake,
                                        contentDescription = null,
                                        tint = if (isVendorOwingMe) {
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isVendorOwingMe) {
                                                "تذكير: بقا ليا عندو ${CurrencyFormatter.formatDirhams(absPending)} (${CurrencyFormatter.formatRials(absPending)})"
                                            } else {
                                                "تذكير: بقا ليه عندي ${CurrencyFormatter.formatDirhams(absPending)} (${CurrencyFormatter.formatRials(absPending)})"
                                            },
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isVendorOwingMe) {
                                                MaterialTheme.colorScheme.onTertiaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onErrorContainer
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "اضغط هنا لتصفية الصرف أو تعديله",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isVendorOwingMe) {
                                                MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                            } else {
                                                MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                            }
                                        )
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "تصفية / تعديل",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Waiting (Unpurchased) Items Section for Current Vendor
                if (selectedVendor != null) {
                    item(key = "vendor_waiting_header") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (waitingItems.isNotEmpty()) {
                                    "مطلوب الآن عند ${selectedVendor.name} (${waitingItems.size}):"
                                } else {
                                    "اكتملت جميع أغراض ${selectedVendor.name}!"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = if (waitingItems.isNotEmpty()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.tertiary
                                },
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Button to record pending change ("صرف عالق") anytime
                                Surface(
                                    modifier = Modifier
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { showVendorPendingBalanceSheet = true }
                                        .testTag("market_vendor_pending_balance_button"),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Handshake,
                                            contentDescription = "الصرف العالق",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "صرف عالق",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onOpenQuickPadForVendor(selectedVendor.id) }
                                        .testTag("market_add_extra_item_button"),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Add,
                                            contentDescription = "إضافة غرض",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "أضف غرضاً",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(waitingItems, key = { "waiting_${it.id}" }) { item ->
                        val normalized = CurrencyFormatter.normalizeArabicName(item.name)
                        val itemHistory = uiState.priceHistory.filter { it.normalizedName == normalized }
                        val usualEval = PriceTrendAnalyzer.evaluateItemPrice(
                            currentUnitPriceDirhams = 0.0,
                            purchaseMode = item.purchaseMode,
                            itemHistory = itemHistory,
                            fallbackLastPriceDirhams = item.unitPriceDirhams
                        )
                        WaitingMarketItemCard(
                            item = item,
                            usualPriceEvaluation = usualEval,
                            onOpenPricing = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                pricingItem = item
                            },
                            onOpenPriceHistory = {
                                onOpenItemPriceHistory(item.name, item.vendorId, item.unitPriceDirhams)
                            },
                            onDeleteItem = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDeleteItemClick(item)
                            }
                        )
                    }

                    // 4. Purchased Items (In the Basket) Section
                    if (purchasedItems.isNotEmpty()) {
                        item(key = "vendor_purchased_header") {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "في القفة عند ${selectedVendor.name} (${purchasedItems.size} أغراض):",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(purchasedItems, key = { "purchased_${it.id}" }) { item ->
                            val normalized = CurrencyFormatter.normalizeArabicName(item.name)
                            val itemHistory = uiState.priceHistory.filter { it.normalizedName == normalized }
                            val paidCompareUnit = if (item.purchaseMode == "FIXED") item.totalDirhams else item.unitPriceDirhams
                            val paidEvaluation = PriceTrendAnalyzer.evaluateItemPrice(
                                currentUnitPriceDirhams = paidCompareUnit,
                                purchaseMode = item.purchaseMode,
                                itemHistory = itemHistory,
                                excludeShoppingItemId = item.id,
                                fallbackLastPriceDirhams = 0.0
                            )
                            PurchasedMarketItemCard(
                                item = item,
                                priceEvaluation = paidEvaluation,
                                onEditPricing = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    pricingItem = item
                                },
                                onOpenPriceHistory = {
                                    onOpenItemPriceHistory(item.name, item.vendorId, paidCompareUnit)
                                },
                                onUnmarkPurchased = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onUnmarkItemPurchased(item)
                                }
                            )
                        }
                    }
                }
            }

            // 5. Compact Bottom Vendor Payment Bar (Shown ONLY when there are purchased items or a confirmed payment)
            if (selectedVendor != null && (purchasedItems.isNotEmpty() || currentVendorConfirmedPayment != null)) {
                val displayedTotalDirhams = currentVendorConfirmedPayment?.actualPaidDirhams
                    ?: currentVendorCalculatedTotalDirhams

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Right side: Compact Purchased Items Counter
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ShoppingBag,
                                        contentDescription = "عداد القفة",
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "في القفة: ${purchasedItems.size} من ${currentVendorItems.size} أغراض",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.testTag("vendor_basket_counter_text")
                                    )
                                    Text(
                                        text = if (currentVendorConfirmedPayment != null) {
                                            if (currentVendorConfirmedPayment.forgivenDiscountDirhams > 0.0) {
                                                "تم تأكيد الدفع (وفّرت ${CurrencyFormatter.formatDirhams(currentVendorConfirmedPayment.forgivenDiscountDirhams)})"
                                            } else {
                                                "تم تأكيد الدفع لهذا البائع"
                                            }
                                        } else if (waitingItems.isEmpty()) {
                                            "تأكد من أخذ جميع أكياسك (${purchasedItems.size})"
                                        } else {
                                            "بقي ${waitingItems.size} أغراض لم تُوزن بعد"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Left side: Current Vendor Total in Dirhams & Rials
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = CurrencyFormatter.formatDirhams(displayedTotalDirhams),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.testTag("vendor_total_dirhams_text")
                                    )
                                    if (uiState.appSettings.showRial) {
                                        Text(
                                            text = CurrencyFormatter.formatRials(displayedTotalDirhams),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        // Action Row: Confirm/Edit Payment + Cancel/Reset Payment
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showConfirmVendorPayment = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("open_confirm_vendor_payment_button"),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currentVendorConfirmedPayment != null) {
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.tertiary
                                    },
                                    contentColor = if (currentVendorConfirmedPayment != null) {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onTertiary
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (currentVendorConfirmedPayment != null) {
                                        Icons.Filled.CheckCircle
                                    } else {
                                        Icons.Outlined.Payments
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (currentVendorConfirmedPayment != null) {
                                        "تعديل الدفع (${CurrencyFormatter.formatDirhams(currentVendorConfirmedPayment.actualPaidDirhams)})"
                                    } else {
                                        "تأكيد الدفع للبائع"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onResetVendorPaymentAndBasket(selectedVendor, true)
                                    }
                                    .testTag("reset_vendor_payment_bar_button"),
                                color = MaterialTheme.colorScheme.errorContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Replay,
                                        contentDescription = "إلغاء وتصفير الدفع",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "إلغاء / تصفير",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
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

    val activePricingItem = pricingItem
    if (activePricingItem != null && selectedVendor != null) {
        val normalized = CurrencyFormatter.normalizeArabicName(activePricingItem.name)
        val itemHistory = uiState.priceHistory.filter { it.normalizedName == normalized }
        VendorItemPricingSheet(
            item = activePricingItem,
            vendorName = selectedVendor.name,
            itemPriceHistory = itemHistory,
            fallbackLastPriceDirhams = activePricingItem.unitPriceDirhams,
            onOpenPriceHistory = {
                onOpenItemPriceHistory(
                    activePricingItem.name,
                    activePricingItem.vendorId,
                    activePricingItem.unitPriceDirhams
                )
            },
            onConfirmPurchase = { mode, unitPriceDh, qty, cleaningDh, totalDh ->
                onConfirmItemPurchase(
                    activePricingItem,
                    mode,
                    unitPriceDh,
                    qty,
                    cleaningDh,
                    totalDh
                )
            },
            onDismiss = { pricingItem = null }
        )
    }

    if (showConfirmVendorPayment && selectedVendor != null) {
        ConfirmVendorPaymentSheet(
            vendor = selectedVendor,
            calculatedItemsTotalDirhams = currentVendorCalculatedTotalDirhams,
            existingPayment = currentVendorConfirmedPayment,
            onConfirmPayment = { calcTotal, actualPaid, forgiven, newPending ->
                onConfirmVendorPayment(
                    selectedVendor,
                    calcTotal,
                    actualPaid,
                    forgiven,
                    newPending
                )
            },
            onResetVendorPayment = {
                onResetVendorPaymentAndBasket(selectedVendor, true)
            },
            onDismiss = { showConfirmVendorPayment = false }
        )
    }

    if (showVendorPendingBalanceSheet && selectedVendor != null) {
        VendorPendingBalanceSheet(
            vendor = selectedVendor,
            onSavePendingBalance = { newPending ->
                onUpdateVendorPendingBalance(selectedVendor, newPending)
            },
            onDismiss = { showVendorPendingBalanceSheet = false }
        )
    }
}

@Composable
private fun WaitingMarketItemCard(
    item: ShoppingItemEntity,
    usualPriceEvaluation: PriceEvaluation,
    onOpenPricing: () -> Unit,
    onOpenPriceHistory: () -> Unit,
    onDeleteItem: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenPricing)
            .testTag("market_waiting_item_${item.id}"),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (item.note.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = item.note,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Calculate,
                                contentDescription = "احسب الثمن",
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "احسب الثمن",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onDeleteItem,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("market_delete_waiting_item_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "حذف ${item.name}",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (usualPriceEvaluation.referenceUsualDirhams > 0.0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "المعتاد: ${CurrencyFormatter.formatDirhams(usualPriceEvaluation.referenceUsualDirhams)} (${CurrencyFormatter.formatRials(usualPriceEvaluation.referenceUsualDirhams)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenPriceHistory)
                            .testTag("open_item_price_history_${item.id}"),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = "سجل الأثمنة",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "سجل الأثمنة",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
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

@Composable
private fun PurchasedMarketItemCard(
    item: ShoppingItemEntity,
    priceEvaluation: PriceEvaluation,
    onEditPricing: () -> Unit,
    onOpenPriceHistory: () -> Unit,
    onUnmarkPurchased: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onEditPricing)
            .testTag("market_purchased_item_${item.id}"),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "تم الشراء",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline
                            )
                            PriceTrendBadge(
                                evaluation = priceEvaluation,
                                onClick = onOpenPriceHistory
                            )
                        }
                        Text(
                            text = CurrencyFormatter.formatPurchaseDetails(item),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.formatDirhams(item.totalDirhams),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = CurrencyFormatter.formatRials(item.totalDirhams),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenPriceHistory),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = "سجل الأثمنة",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "سجل الأثمنة",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onEditPricing),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "تعديل السعر",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تعديل الحساب",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onUnmarkPurchased)
                        .testTag("unmark_purchased_${item.id}"),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Replay,
                            contentDescription = "إرجاع للانتظار",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "إرجاع للانتظار",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
