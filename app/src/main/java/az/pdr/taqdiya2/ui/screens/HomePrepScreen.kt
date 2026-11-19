package az.pdr.taqdiya2.ui.screens

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import az.pdr.taqdiya2.data.local.DictionaryItemEntity
import az.pdr.taqdiya2.data.local.ShoppingItemEntity
import az.pdr.taqdiya2.data.local.VendorEntity
import az.pdr.taqdiya2.ui.components.CalmOutlinedActionButton
import az.pdr.taqdiya2.ui.components.CleanShoppingItemHeaderRow
import az.pdr.taqdiya2.ui.components.VendorIconMapper
import az.pdr.taqdiya2.ui.theme.ambientCardBorder
import az.pdr.taqdiya2.ui.theme.ambientCardShadow
import az.pdr.taqdiya2.ui.viewmodel.HomePrepUiState
import az.pdr.taqdiya2.util.CurrencyFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomePrepScreen(
    uiState: HomePrepUiState,
    onOpenQuickPadForVendor: (Long?) -> Unit,
    onQuickAddDictionaryItem: (DictionaryItemEntity) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onFilterVendorChanged: (Long?) -> Unit,
    onEditItemClick: (ShoppingItemEntity) -> Unit,
    onDeleteItemClick: (ShoppingItemEntity) -> Unit,
    onOpenDictionaryClick: () -> Unit,
    onOpenVendorsClick: () -> Unit,
    onOpenPriceTrackerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showSearchBar by rememberSaveable { mutableStateOf(false) }

    val activeItems = remember(uiState.shoppingItems, uiState.searchQuery, uiState.filterVendorId) {
        uiState.shoppingItems.filter { item ->
            !item.isDeleted &&
                (uiState.filterVendorId == null || item.vendorId == uiState.filterVendorId) &&
                (uiState.searchQuery.isBlank() ||
                    CurrencyFormatter.matchesArabicQuery(item.name, uiState.searchQuery) ||
                    CurrencyFormatter.matchesArabicQuery(item.note, uiState.searchQuery))
        }
    }

    val groupedByVendor = remember(activeItems) {
        activeItems.groupBy { it.vendorId }
    }

    val starterFrequentItems = remember(uiState.dictionaryItems) {
        uiState.dictionaryItems.take(12)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Scrollable Content (Clean, Uncluttered, Floating Ambient Cards)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Tactile Vendor Bento Tiles Strip
            item(key = "vendor_bento_strip") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "البائعون:",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val pillShape = RoundedCornerShape(10.dp)
                            Surface(
                                modifier = Modifier
                                    .heightIn(min = 34.dp)
                                    .clip(pillShape)
                                    .clickable { showSearchBar = !showSearchBar },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                shape = pillShape
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = "بحث",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .heightIn(min = 34.dp)
                                    .clip(pillShape)
                                    .clickable(onClick = onOpenPriceTrackerClick)
                                    .testTag("card_open_price_tracker_button"),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                                shape = pillShape
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.QueryStats,
                                        contentDescription = "كناش الأثمنة",
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "الأثمنة",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .heightIn(min = 34.dp)
                                    .clip(pillShape)
                                    .clickable(onClick = onOpenDictionaryClick)
                                    .testTag("card_open_dictionary_button"),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                shape = pillShape
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.MenuBook,
                                        contentDescription = "القاموس",
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "القاموس",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .heightIn(min = 34.dp)
                                    .clip(pillShape)
                                    .clickable(onClick = onOpenVendorsClick)
                                    .testTag("card_open_vendors_button"),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                shape = pillShape
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Storefront,
                                        contentDescription = "إدارة البائعين",
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "البائعون",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    if (showSearchBar || uiState.searchQuery.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchQueryChanged,
                            placeholder = { Text("ابحث في قائمة النواقص...") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Search, contentDescription = "بحث")
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChanged("") }) {
                                        Icon(Icons.Outlined.Clear, contentDescription = "مسح")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shopping_list_search_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Single-Line Flexible-Width Horizontal Vendor Cards
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        uiState.vendors.forEach { vendor ->
                            val count = uiState.shoppingItems.count { it.vendorId == vendor.id && !it.isDeleted }
                            val hasItems = count > 0
                            val bentoShape = RoundedCornerShape(14.dp)
                            Surface(
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .ambientCardShadow(
                                        shape = bentoShape,
                                        elevation = if (hasItems) 5.dp else 3.dp
                                    )
                                    .clip(bentoShape)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onOpenQuickPadForVendor(vendor.id)
                                    }
                                    .testTag("bento_vendor_${vendor.id}"),
                                color = MaterialTheme.colorScheme.surface,
                                border = ambientCardBorder(),
                                shape = bentoShape
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.11f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = VendorIconMapper.getIcon(vendor.iconKey, vendor.name),
                                            contentDescription = vendor.name,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Text(
                                        text = vendor.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )

                                    Surface(
                                        color = if (hasItems) {
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (hasItems) "$count أغراض" else "+ أضف",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (hasItems) {
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. If List is Empty: Show Instant 1-Tap Tactile Grid right on the screen!
            if (activeItems.isEmpty()) {
                item(key = "instant_starter_grid") {
                    val starterShape = RoundedCornerShape(22.dp)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .ambientCardShadow(shape = starterShape, elevation = 6.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = ambientCardBorder(),
                        shape = starterShape
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "كناش النواقص جاهز — اضغط على أي غرض لإضافته فوراً:",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "بلمسة إبهام واحدة يتوزع كل غرض تلقائياً على البائع المخصص له بدون كتابة:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                maxItemsInEachRow = 3
                            ) {
                                starterFrequentItems.forEach { dictItem ->
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 56.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onQuickAddDictionaryItem(dictItem)
                                            }
                                            .testTag("quick_chip_${dictItem.id}"),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "+ ${dictItem.name}",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // 3. Oversized Vendor Ledger Cards
                val vendorsToShow = uiState.vendors.filter { groupedByVendor.containsKey(it.id) }
                items(vendorsToShow, key = { "vendor_group_${it.id}" }) { vendor ->
                    val vendorItems = groupedByVendor[vendor.id].orEmpty()
                    OversizedVendorCard(
                        vendor = vendor,
                        items = vendorItems,
                        animateChanges = uiState.appSettings.animationMode != "OFF",
                        onOpenQuickPadForVendor = { onOpenQuickPadForVendor(vendor.id) },
                        onEditItem = onEditItemClick,
                        onDeleteItem = onDeleteItemClick
                    )
                }
            }
        }

        // Pinned Bottom Thumb-Zone Action Bar (Compact height to maximize list area)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenQuickPadForVendor(uiState.selectedVendorIdForInput)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .testTag("open_quick_pad_main_button"),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GridView,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "لوحة الإضافة السريعة باللمس (+)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun OversizedVendorCard(
    vendor: VendorEntity,
    items: List<ShoppingItemEntity>,
    animateChanges: Boolean = true,
    onOpenQuickPadForVendor: () -> Unit,
    onEditItem: (ShoppingItemEntity) -> Unit,
    onDeleteItem: (ShoppingItemEntity) -> Unit
) {
    val cardShape = RoundedCornerShape(22.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .ambientCardShadow(shape = cardShape, elevation = 6.dp)
            .then(
                if (animateChanges) {
                    Modifier.animateContentSize()
                } else {
                    Modifier
                }
            )
            .testTag("vendor_card_${vendor.id}"),
        color = MaterialTheme.colorScheme.surface,
        border = ambientCardBorder(),
        shape = cardShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Calm Vendor Header (neutral store badge + muted title + outlined '+ أضف' with primary '+')
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = VendorIconMapper.getIcon(vendor.iconKey, vendor.name),
                            contentDescription = vendor.name,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = vendor.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f),
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${items.size} أغراض مسجلة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        if (kotlin.math.abs(vendor.pendingBalanceDirhams) > 0.009) {
                            val isVendorOwingMe = vendor.pendingBalanceDirhams > 0.0
                            val absPending = kotlin.math.abs(vendor.pendingBalanceDirhams)
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                color = if (isVendorOwingMe) {
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f)
                                } else {
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isVendorOwingMe) {
                                        "بقا ليا عندو: ${az.pdr.taqdiya2.util.CurrencyFormatter.formatDirhams(absPending)}"
                                    } else {
                                        "بقا ليه عندي: ${az.pdr.taqdiya2.util.CurrencyFormatter.formatDirhams(absPending)}"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isVendorOwingMe) {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Direct "+ أضف" outlined button (only '+' icon is primary-colored)
                CalmOutlinedActionButton(
                    text = "أضف",
                    icon = Icons.Outlined.Add,
                    iconContentDescription = "إضافة",
                    onClick = onOpenQuickPadForVendor,
                    modifier = Modifier.testTag("vendor_card_add_${vendor.id}")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                thickness = 0.8.dp
            )

            // Ultra-Clean Item Rows (Hollow Ring Dot + Primary Bold Item Name + Plain Note | Calm Edit & Delete Icons)
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        thickness = 0.8.dp
                    )
                }
                CleanShoppingItemHeaderRow(
                    itemName = item.name,
                    itemNote = item.note,
                    onEditClick = { onEditItem(item) },
                    onDeleteClick = { onDeleteItem(item) },
                    editContentDescription = "تعديل الغرض",
                    deleteContentDescription = "إزالة من القائمة",
                    editTestTag = "edit_item_${item.id}",
                    deleteTestTag = "delete_item_${item.id}",
                    modifier = Modifier
                        .clickable { onEditItem(item) }
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}
