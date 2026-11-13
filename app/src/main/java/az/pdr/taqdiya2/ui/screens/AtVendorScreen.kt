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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LightMode
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
import az.pdr.taqdiya2.ui.components.VendorIconMapper
import az.pdr.taqdiya2.ui.components.VendorItemPricingSheet
import az.pdr.taqdiya2.ui.viewmodel.HomePrepUiState
import az.pdr.taqdiya2.util.CurrencyFormatter

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

    val currentVendorTotalDirhams = remember(purchasedItems) {
        purchasedItems.sumOf { it.totalDirhams }
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Top Market Header: Active Vendors Strip + Keep Screen Awake Toggle
                item(key = "market_vendor_selector") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "بائعو جولة اليوم (${vendorsWithItems.size}):",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            Surface(
                                modifier = Modifier
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(onClick = onToggleKeepScreenAwake)
                                    .testTag("toggle_keep_screen_awake_button"),
                                color = if (uiState.keepScreenAwakeInMarket) {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
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
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (uiState.keepScreenAwakeInMarket) "الشاشة مضاءة للسوق" else "إضاءة عادية",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (uiState.keepScreenAwakeInMarket) {
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Oversized Tactile Vendor Selector Cards
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

                                Surface(
                                    modifier = Modifier
                                        .width(168.dp)
                                        .height(82.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSelectMarketVendor(vendor.id)
                                        }
                                        .testTag("market_vendor_tab_${vendor.id}"),
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        vAllDone -> MaterialTheme.colorScheme.tertiaryContainer
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    border = BorderStroke(
                                        width = if (isSelected) 0.dp else 1.5.dp,
                                        color = if (vAllDone) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                                    ),
                                    shape = RoundedCornerShape(18.dp),
                                    shadowElevation = if (isSelected) 4.dp else 1.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = VendorIconMapper.getIcon(vendor.iconKey),
                                                contentDescription = vendor.name,
                                                tint = if (isSelected) {
                                                    MaterialTheme.colorScheme.onPrimary
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
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = vendor.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimary
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

                // 2. Waiting (Unpurchased) Items Section for Current Vendor
                if (selectedVendor != null) {
                    item(key = "vendor_waiting_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
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
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "غرض إضافي",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    items(waitingItems, key = { "waiting_${it.id}" }) { item ->
                        WaitingMarketItemCard(
                            item = item,
                            onOpenPricing = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                pricingItem = item
                            }
                        )
                    }

                    // 3. Purchased Items (In the Basket) Section
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
                            PurchasedMarketItemCard(
                                item = item,
                                onEditPricing = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    pricingItem = item
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

            // 4. Pinned Bottom Vendor Review Bar (Total + Item Counter)
            if (selectedVendor != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Right side: Purchased Items Counter (Never forget a bag on the scale!)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.tertiary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ShoppingBag,
                                    contentDescription = "عداد القفة",
                                    tint = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "في القفة: ${purchasedItems.size} من ${currentVendorItems.size} أغراض",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("vendor_basket_counter_text")
                                )
                                Text(
                                    text = if (waitingItems.isEmpty()) {
                                        "تأكد من أخذ جميع أكياسك (${purchasedItems.size})"
                                    } else {
                                        "بقي ${waitingItems.size} أغراض لم تُوزن بعد"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                )
                            }
                        }

                        // Left side: Current Vendor Total in Dirhams & Rials
                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "مجموع هذا البائع",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = CurrencyFormatter.formatDirhams(currentVendorTotalDirhams),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("vendor_total_dirhams_text")
                                )
                                Text(
                                    text = CurrencyFormatter.formatRials(currentVendorTotalDirhams),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.95f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val activePricingItem = pricingItem
    if (activePricingItem != null && selectedVendor != null) {
        VendorItemPricingSheet(
            item = activePricingItem,
            vendorName = selectedVendor.name,
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
}

@Composable
private fun WaitingMarketItemCard(
    item: ShoppingItemEntity,
    onOpenPricing: () -> Unit
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
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
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (item.unitPriceDirhams > 0.0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "آخر ثمن مسجل: ${CurrencyFormatter.formatDirhams(item.unitPriceDirhams)} (${CurrencyFormatter.formatRials(item.unitPriceDirhams)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Surface(
                color = MaterialTheme.colorScheme.secondary,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(50.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Calculate,
                        contentDescription = "احسب الثمن",
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "احسب الثمن",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PurchasedMarketItemCard(
    item: ShoppingItemEntity,
    onEditPricing: () -> Unit,
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
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )
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
