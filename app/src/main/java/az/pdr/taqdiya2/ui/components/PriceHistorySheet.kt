package az.pdr.taqdiya2.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import az.pdr.taqdiya2.data.local.DictionaryItemEntity
import az.pdr.taqdiya2.data.local.PriceHistoryEntity
import az.pdr.taqdiya2.data.local.VendorEntity
import az.pdr.taqdiya2.ui.theme.TrendHighRed
import az.pdr.taqdiya2.ui.theme.TrendLowGreen
import az.pdr.taqdiya2.ui.theme.TrendNormalAmber
import az.pdr.taqdiya2.ui.viewmodel.UndoAction
import az.pdr.taqdiya2.util.CurrencyFormatter
import az.pdr.taqdiya2.util.PriceEvaluation
import az.pdr.taqdiya2.util.PriceTrendAnalyzer
import az.pdr.taqdiya2.util.PriceTrendStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PriceIndicatorBanner(
    evaluation: PriceEvaluation,
    onOpenHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor: Color
    val containerColor: Color
    val icon = when (evaluation.status) {
        PriceTrendStatus.CHEAPER -> {
            accentColor = TrendLowGreen
            containerColor = TrendLowGreen.copy(alpha = 0.13f)
            Icons.AutoMirrored.Outlined.TrendingDown
        }
        PriceTrendStatus.STABLE -> {
            accentColor = TrendNormalAmber
            containerColor = TrendNormalAmber.copy(alpha = 0.14f)
            Icons.AutoMirrored.Outlined.TrendingFlat
        }
        PriceTrendStatus.EXPENSIVE -> {
            accentColor = TrendHighRed
            containerColor = TrendHighRed.copy(alpha = 0.13f)
            Icons.AutoMirrored.Outlined.TrendingUp
        }
        PriceTrendStatus.NO_HISTORY -> {
            accentColor = MaterialTheme.colorScheme.primary
            containerColor = MaterialTheme.colorScheme.surfaceVariant
            Icons.Outlined.History
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpenHistoryClick)
            .testTag("price_indicator_banner"),
        color = containerColor,
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = evaluation.badgeLabel,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = evaluation.headlineText,
                        style = MaterialTheme.typography.titleSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = evaluation.recommendationText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = "سجل الأثمنة",
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "السجل (${evaluation.historyCount})",
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PriceTrendBadge(
    evaluation: PriceEvaluation,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (evaluation.status == PriceTrendStatus.NO_HISTORY) return

    val accentColor = when (evaluation.status) {
        PriceTrendStatus.CHEAPER -> TrendLowGreen
        PriceTrendStatus.STABLE -> TrendNormalAmber
        PriceTrendStatus.EXPENSIVE -> TrendHighRed
        PriceTrendStatus.NO_HISTORY -> MaterialTheme.colorScheme.primary
    }

    val icon = when (evaluation.status) {
        PriceTrendStatus.CHEAPER -> Icons.AutoMirrored.Outlined.TrendingDown
        PriceTrendStatus.STABLE -> Icons.AutoMirrored.Outlined.TrendingFlat
        PriceTrendStatus.EXPENSIVE -> Icons.AutoMirrored.Outlined.TrendingUp
        PriceTrendStatus.NO_HISTORY -> Icons.Outlined.History
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = accentColor.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = evaluation.badgeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemPriceHistorySheet(
    itemName: String,
    defaultVendorId: Long,
    vendors: List<VendorEntity>,
    allPriceHistory: List<PriceHistoryEntity>,
    fallbackLastPriceDirhams: Double = 0.0,
    activeUndo: UndoAction? = null,
    onPerformUndo: (UndoAction) -> Unit = {},
    onDismissUndo: () -> Unit = {},
    onAddManualPriceRecord: (
        itemName: String,
        vendorId: Long,
        purchaseMode: String,
        unitPriceDirhams: Double
    ) -> Unit,
    onDeletePriceRecord: (PriceHistoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val normalizedTarget = remember(itemName) { CurrencyFormatter.normalizeArabicName(itemName) }

    val itemRecords = remember(allPriceHistory, normalizedTarget) {
        allPriceHistory
            .filter { it.normalizedName == normalizedTarget }
            .sortedByDescending { it.recordedAt }
    }

    val latestMode = itemRecords.firstOrNull()?.purchaseMode ?: "WEIGHT"
    val latestPrice = itemRecords.firstOrNull()?.unitPriceDirhams ?: fallbackLastPriceDirhams
    val evaluation = remember(itemRecords, latestPrice, latestMode) {
        PriceTrendAnalyzer.evaluateItemPrice(
            currentUnitPriceDirhams = latestPrice,
            purchaseMode = latestMode,
            itemHistory = itemRecords,
            fallbackLastPriceDirhams = fallbackLastPriceDirhams
        )
    }

    var showAddManualRow by remember { mutableStateOf(false) }
    var manualPriceInput by remember { mutableStateOf("") }

    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QueryStats,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "سجل أثمنة: $itemName",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "مؤشر الغلاء والرخاء وتاريخ مشترياتك",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "إغلاق")
                }
            }

            InlineUndoBanner(
                activeUndo = activeUndo,
                onPerformUndo = onPerformUndo,
                onDismiss = onDismissUndo
            )

            // 3 Summary Stat Boxes (أدنى ثمن أخضر | المعدل المعتاد أصفر | أعلى ثمن أحمر)
            if (evaluation.referenceUsualDirhams > 0.0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PriceStatBox(
                        label = "أدنى ثمن (رخاء)",
                        priceDirhams = evaluation.minPriceDirhams,
                        accentColor = TrendLowGreen,
                        modifier = Modifier.weight(1f)
                    )
                    PriceStatBox(
                        label = "المعدل المعتاد",
                        priceDirhams = evaluation.referenceUsualDirhams,
                        accentColor = TrendNormalAmber,
                        modifier = Modifier.weight(1f)
                    )
                    PriceStatBox(
                        label = "أعلى ثمن (غلاء)",
                        priceDirhams = evaluation.maxPriceDirhams,
                        accentColor = TrendHighRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Manual Price Entry Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل التواريخ والأسعار (${itemRecords.size}):",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showAddManualRow = !showAddManualRow }
                        .testTag("toggle_add_manual_price_button"),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تسجيل ثمن يدوي",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (showAddManualRow) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualPriceInput,
                            onValueChange = { manualPriceInput = it },
                            label = { Text("الثمن بالدرهم") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_price_input_field")
                        )
                        Button(
                            onClick = {
                                val dh = manualPriceInput.replace(',', '.').toDoubleOrNull() ?: 0.0
                                if (dh > 0.0) {
                                    val targetVId = vendors.firstOrNull { it.id == defaultVendorId }?.id
                                        ?: vendors.firstOrNull()?.id ?: 1L
                                    onAddManualPriceRecord(itemName, targetVId, latestMode, dh)
                                    manualPriceInput = ""
                                    showAddManualRow = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Text("حفظ")
                        }
                    }
                }
            }

            if (itemRecords.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "لا توجد أسعار مسجلة بعد لـ «$itemName». سيُحفظ السعر تلقائياً في كل مرة تشتري فيها هذه السلعة من شاشة [عند البائع].",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                val maxPriceInList = (itemRecords.maxOfOrNull { it.unitPriceDirhams } ?: 1.0).coerceAtLeast(1.0)
                val avgPriceInList = evaluation.referenceUsualDirhams

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(itemRecords, key = { "price_rec_${it.id}" }) { record ->
                        val dateStr = dateFormatter.format(Date(record.recordedAt))
                        val barFraction = (record.unitPriceDirhams / maxPriceInList).toFloat().coerceIn(0.18f, 1f)
                        val recordColor = when {
                            record.unitPriceDirhams <= avgPriceInList * 0.95 -> TrendLowGreen
                            record.unitPriceDirhams >= avgPriceInList * 1.05 -> TrendHighRed
                            else -> TrendNormalAmber
                        }
                        val statusWord = when {
                            record.unitPriceDirhams <= avgPriceInList * 0.95 -> "رخاء"
                            record.unitPriceDirhams >= avgPriceInList * 1.05 -> "غلاء"
                            else -> "مستقر"
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = CurrencyFormatter.formatDirhams(record.unitPriceDirhams),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = recordColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "(${CurrencyFormatter.formatRials(record.unitPriceDirhams)})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Surface(
                                                color = recordColor.copy(alpha = 0.14f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = statusWord,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = recordColor,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "$dateStr • ${record.vendorName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeletePriceRecord(record) },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .testTag("delete_price_record_${record.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "حذف السعر",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // Visual comparative bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(barFraction)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(recordColor)
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

@Composable
private fun PriceStatBox(
    label: String,
    priceDirhams: Double,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = accentColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = CurrencyFormatter.formatDirhams(priceDirhams),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = CurrencyFormatter.formatRials(priceDirhams),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AllItemsPriceTrackerDialog(
    dictionaryItems: List<DictionaryItemEntity>,
    priceHistory: List<PriceHistoryEntity>,
    onSelectItemForHistory: (DictionaryItemEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = remember(dictionaryItems, searchQuery) {
        dictionaryItems.filter { item ->
            searchQuery.isBlank() || CurrencyFormatter.matchesArabicQuery(item.name, searchQuery)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 20.dp),
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Column {
                Text(
                    text = "كناش الأثمنة ومؤشر الغلاء والرخاء",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "اضغط على أي سلعة لعرض تاريخ أسعارها ومقارنة الغلاء والرخاء",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن سلعة (مطيشة، دجاج، لوبيا...)") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredItems, key = { "tracker_${it.id}" }) { dictItem ->
                        val itemHistory = priceHistory.filter { it.normalizedName == dictItem.normalizedName }
                        val latestMode = itemHistory.firstOrNull()?.purchaseMode ?: "WEIGHT"
                        val latestPrice = itemHistory.firstOrNull()?.unitPriceDirhams ?: dictItem.lastPriceDirhams
                        val evaluation = PriceTrendAnalyzer.evaluateItemPrice(
                            currentUnitPriceDirhams = latestPrice,
                            purchaseMode = latestMode,
                            itemHistory = itemHistory,
                            fallbackLastPriceDirhams = dictItem.lastPriceDirhams
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectItemForHistory(dictItem) }
                                .testTag("price_tracker_item_${dictItem.id}"),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = dictItem.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        PriceTrendBadge(evaluation = evaluation)
                                    }
                                    Text(
                                        text = if (evaluation.referenceUsualDirhams > 0.0) {
                                            "المعتاد: ${CurrencyFormatter.formatDirhams(evaluation.referenceUsualDirhams)} • السجلات: ${itemHistory.size}"
                                        } else {
                                            "لم يُسجل ثمن بعد"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (latestPrice > 0.0) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = CurrencyFormatter.formatDirhams(latestPrice),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = CurrencyFormatter.formatRials(latestPrice),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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
