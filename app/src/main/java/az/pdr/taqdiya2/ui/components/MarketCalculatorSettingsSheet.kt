package az.pdr.taqdiya2.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import az.pdr.taqdiya2.data.local.AppSettingsEntity
import az.pdr.taqdiya2.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MarketCalculatorSettingsSheet(
    settings: AppSettingsEntity,
    onToggleAutoLinkMoneyToFixed: () -> Unit,
    onToggleAutoLinkCountToCount: () -> Unit,
    onToggleAutoLinkWeightToWeight: () -> Unit,
    onUpdateDefaultPricingTab: (String) -> Unit,
    onToggleEnableQuickFixedBuyButton: () -> Unit,
    onAddUnitToPricingTab: (String, String) -> Unit,
    onRemoveUnitFromPricingTab: (String, String) -> Unit,
    onResetPricingTabUnitsToDefaults: () -> Unit,
    onToggleAutoKeepScreenAwakeInMarket: () -> Unit,
    onUpdateDefaultPricingCurrencyInRials: (Boolean) -> Unit,
    onToggleShowCleaningFeeOption: () -> Unit,
    onToggleShowPriceTrendBadges: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedUnitManagerTab by remember { mutableStateOf("COUNT") }
    var newTabUnitText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
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
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Calculate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "إعدادات السوق وحاسبة «احسب الثمن»",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تحكم كامل في ربط المقادير بالتبويبات الثلاثة وخيارات الحاسبة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("close_market_calculator_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "إغلاق",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

            // Section 1: Interactive Unit-to-Tab Manager (Add / Remove / Reset units per tab)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("unit_to_tab_manager_card"),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "1. جرد وتخصيص المقادير المرتبطة بكل تبويب",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "اختر التبويب لعرض المقادير التي تفتحه تلقائياً، أو أضف وأزل أي مقدار كما تريد",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onResetPricingTabUnitsToDefaults() }
                                .testTag("reset_pricing_tab_units_button"),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Replay,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "استعادة الافتراضي",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 3 Tabs Selector (Responsive height & clean wrapping if needed)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val managerTabs = listOf(
                            "WEIGHT" to "1. بالوزن",
                            "FIXED" to "2. مبلغ مقطوع",
                            "COUNT" to "3. بالعدد / العلبة"
                        )
                        managerTabs.forEach { (tabKey, tabLabel) ->
                            val isSelected = selectedUnitManagerTab == tabKey
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedUnitManagerTab = tabKey }
                                    .testTag("unit_manager_tab_$tabKey"),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                },
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tabLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    val activeTabUnits = remember(
                        selectedUnitManagerTab,
                        settings.weightUnitsCsv,
                        settings.fixedUnitsCsv,
                        settings.countUnitsCsv
                    ) {
                        val rawCsv = when (selectedUnitManagerTab) {
                            "WEIGHT" -> settings.weightUnitsCsv
                            "FIXED" -> settings.fixedUnitsCsv
                            else -> settings.countUnitsCsv
                        }
                        CurrencyFormatter.parseCustomUnits(rawCsv)
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = when (selectedUnitManagerTab) {
                                    "WEIGHT" -> "المقادير التي تفتح تبويب «1. بالوزن» (${activeTabUnits.size}):"
                                    "FIXED" -> "المقادير التي تفتح تبويب «2. مبلغ مقطوع» بجانب أي مبلغ مالي (${activeTabUnits.size}):"
                                    else -> "المقادير التي تفتح تبويب «3. بالعدد / العلبة» (${activeTabUnits.size}):"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                activeTabUnits.forEach { unitName ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = unitName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f))
                                                    .clickable {
                                                        onRemoveUnitFromPricingTab(selectedUnitManagerTab, unitName)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = "إزالة $unitName",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newTabUnitText,
                                    onValueChange = { newTabUnitText = it },
                                    placeholder = {
                                        Text(
                                            text = "أضف مقداراً جديداً لهذا التبويب...",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("new_tab_unit_input")
                                )
                                Button(
                                    onClick = {
                                        val clean = newTabUnitText.trim()
                                        if (clean.isNotEmpty()) {
                                            onAddUnitToPricingTab(selectedUnitManagerTab, clean)
                                            newTabUnitText = ""
                                        }
                                    },
                                    enabled = newTabUnitText.trim().isNotEmpty(),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(50.dp)
                                        .testTag("add_unit_to_tab_button")
                                ) {
                                    Text(
                                        text = "أضف",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Smart Auto-Linking Rules & Default Tab / Currency
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("market_pricing_settings_card"),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "2. قواعد الربط الذكي والافتراضيات:",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    MarketToolSwitchRow(
                        title = "ربط المبالغ المالية بتبويب «2. مبلغ مقطوع»",
                        subtitle = "أي رقم مع درهم أو ريال (5 دراهم، 50 درهم، 100 ريال) يفتح المبلغ المقطوع فوراً",
                        checked = settings.autoLinkMoneyToFixed,
                        onToggle = onToggleAutoLinkMoneyToFixed,
                        testTag = "toggle_auto_link_money_to_fixed"
                    )

                    MarketToolSwitchRow(
                        title = "ربط العبوات والعدد بتبويب «3. بالعدد / العلبة»",
                        subtitle = "مثل علبة، قرعة، باكية، بلاطو، حبة مع تعبئة العدد تلقائياً",
                        checked = settings.autoLinkCountToCount,
                        onToggle = onToggleAutoLinkCountToCount,
                        testTag = "toggle_auto_link_count_to_count"
                    )

                    MarketToolSwitchRow(
                        title = "ربط الأوزان بتبويب «1. بالوزن»",
                        subtitle = "مثل كيلو، نص كيلو، رابعة، غرام مع تحديد الوزن تلقائياً",
                        checked = settings.autoLinkWeightToWeight,
                        onToggle = onToggleAutoLinkWeightToWeight,
                        testTag = "toggle_auto_link_weight_to_weight"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                    // Default Pricing Tab When Item Has No Note
                    Text(
                        text = "التبويب الافتراضي عند فتح «احسب الثمن» لغرض بدون ملاحظة:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val defaultTabs = listOf(
                            "WEIGHT" to "1. بالوزن",
                            "FIXED" to "2. مبلغ مقطوع",
                            "COUNT" to "3. بالعدد / العلبة"
                        )
                        defaultTabs.forEach { (tabKey, tabLabel) ->
                            val isSelected = settings.defaultPricingTab == tabKey
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onUpdateDefaultPricingTab(tabKey) }
                                    .testTag("default_pricing_tab_$tabKey"),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                },
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tabLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                    // Default Calculator Currency (Dirham vs Rial)
                    Text(
                        text = "عملة الإدخال الافتراضية عند فتح الحاسبة:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isRialDefault = settings.defaultPricingCurrencyInRials
                        MarketOptionTile(
                            title = "إدخال بالدرهم",
                            badge = "افتراضي",
                            selected = !isRialDefault,
                            onClick = { onUpdateDefaultPricingCurrencyInRials(false) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("default_calc_currency_dirham")
                        )
                        MarketOptionTile(
                            title = "إدخال بالريال",
                            badge = "1 د.م = 20 ريال",
                            selected = isRialDefault,
                            onClick = { onUpdateDefaultPricingCurrencyInRials(true) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("default_calc_currency_rial")
                        )
                    }
                }
            }

            // Section 3: Market Experience Toggles
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("market_experience_toggles_card"),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "3. خيارات شاشة «عند البائع» والحاسبة:",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    MarketToolSwitchRow(
                        title = "زر الشراء السريع بلمسة واحدة (✓ د.م)",
                        subtitle = "إظهار زر الشراء المباشر على بطاقة الغرض عندما تكون ملاحظته مبلغاً مالياً",
                        checked = settings.enableQuickFixedBuyButton,
                        onToggle = onToggleEnableQuickFixedBuyButton,
                        testTag = "toggle_quick_fixed_buy_button"
                    )

                    MarketToolSwitchRow(
                        title = "إبقاء الشاشة مضاءة تلقائياً عند البائع",
                        subtitle = "تفعيل إضاءة الشاشة تلقائياً بمجرد الانتقال إلى تبويب «عند البائع»",
                        checked = settings.autoKeepScreenAwakeInMarket,
                        onToggle = onToggleAutoKeepScreenAwakeInMarket,
                        testTag = "toggle_auto_keep_screen_awake"
                    )

                    MarketToolSwitchRow(
                        title = "إظهار خيار ثمن التنقية للسمك والدجاج",
                        subtitle = "إظهار أزرار (+2 درهم، +3 درهم...) لتنقية الحوت والدجاج في تبويب الوزن",
                        checked = settings.showCleaningFeeOption,
                        onToggle = onToggleShowCleaningFeeOption,
                        testTag = "toggle_show_cleaning_fee"
                    )

                    MarketToolSwitchRow(
                        title = "إظهار مؤشر الغلاء والرخاء في البطاقات والحاسبة",
                        subtitle = "مقارنة السعر الحالي بالسجل التاريخي للغرض وإظهار شارة المعتاد/الغلاء/الرخاء",
                        checked = settings.showPriceTrendBadges,
                        onToggle = onToggleShowPriceTrendBadges,
                        testTag = "toggle_show_price_trend_badges"
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun MarketToolSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() }
        )
    }
}

@Composable
private fun MarketOptionTile(
    title: String,
    badge: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.outline
            }
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                color = if (selected) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSecondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
