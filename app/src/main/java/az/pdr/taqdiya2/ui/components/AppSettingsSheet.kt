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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import az.pdr.taqdiya2.data.local.AppSettingsEntity
import az.pdr.taqdiya2.ui.theme.resolveUiScalePercent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppSettingsSheet(
    settings: AppSettingsEntity,
    onUpdateThemeMode: (String) -> Unit,
    onUpdateUiScaleMode: (String) -> Unit,
    onUpdateColorPalette: (String) -> Unit = {},
    onUpdateAnimationMode: (String) -> Unit = {},
    onUpdateFontFamilyKey: (String) -> Unit = {},
    onToggleShowRial: () -> Unit,
    onTogglePrivacyMask: () -> Unit,
    onUpdatePrivacyMaskPocketOnly: (Boolean) -> Unit = {},
    onSetPinCode: (String) -> Unit,
    onRemovePinCode: () -> Unit,
    onLockAppNow: () -> Unit,
    onOpenBackupRestore: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showPinEditor by remember { mutableStateOf(false) }
    var draftPin by remember { mutableStateOf("") }

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
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "الإعدادات والخصوصية والمظهر",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "تخصيص الإضاءة، الألوان، الحركات، حجم الخط، وحماية الكناش",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_settings_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "إغلاق الإعدادات"
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            // 1. Theme Mode (الوضع النهاري الساطع للسوق / الليلي)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1. إضاءة ومظهر الشاشة",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingsOptionTile(
                        title = "نهاري ساطع",
                        subtitle = "واضح تحت الشمس",
                        icon = Icons.Outlined.LightMode,
                        selected = settings.themeMode == "LIGHT",
                        onClick = { onUpdateThemeMode("LIGHT") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_mode_light_button")
                    )
                    SettingsOptionTile(
                        title = "ليلي مريح",
                        subtitle = "هادئ للعين بالليل",
                        icon = Icons.Outlined.DarkMode,
                        selected = settings.themeMode == "DARK",
                        onClick = { onUpdateThemeMode("DARK") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_mode_dark_button")
                    )
                    SettingsOptionTile(
                        title = "تلقائي",
                        subtitle = "حسب الهاتف",
                        icon = Icons.Outlined.BrightnessAuto,
                        selected = settings.themeMode == "SYSTEM",
                        onClick = { onUpdateThemeMode("SYSTEM") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_mode_system_button")
                    )
                }
            }

            // 1.5 Color Palette (نسق الألوان — زمرد وعنبر، ياقوتي ومرجاني فقط)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "نسق الألوان (يدعم النهاري والليلي):",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                val normalizedPalette = if (settings.colorPalette == "VIBRANT_COBALT") "VIBRANT_COBALT" else "TERRACOTTA"
                val palettes = listOf(
                    Triple("TERRACOTTA", "زمرد وعنبر", Pair(androidx.compose.ui.graphics.Color(0xFF0A6847), androidx.compose.ui.graphics.Color(0xFFD97706))),
                    Triple("VIBRANT_COBALT", "ياقوتي ومرجاني", Pair(androidx.compose.ui.graphics.Color(0xFF1E40AF), androidx.compose.ui.graphics.Color(0xFFEA580C)))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    palettes.forEach { (paletteKey, label, swatchPair) ->
                        val isSelected = normalizedPalette == paletteKey
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onUpdateColorPalette(paletteKey) }
                                .testTag("color_palette_${paletteKey.lowercase()}_button"),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(swatchPair.first)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(swatchPair.second)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 1.8 Animation Mode (حركات خفيفة وسريعة / إطفاء الحركات فقط)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "المؤثرات الحركية وسرعة الانتقال:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                val isAnimOff = settings.animationMode == "OFF"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScaleOptionTile(
                        title = "خفيفة وسريعة",
                        badge = "موصى به",
                        selected = !isAnimOff,
                        onClick = { onUpdateAnimationMode("LIGHT") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("anim_mode_light_button")
                    )
                    ScaleOptionTile(
                        title = "إطفاء الحركات",
                        badge = "فوري",
                        selected = isAnimOff,
                        onClick = { onUpdateAnimationMode("OFF") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("anim_mode_off_button")
                    )
                }
            }

            // 2. Font & Button Size Scaling (85% to 122% via - / + buttons & quick chips) + Font Family
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val currentScalePercent = resolveUiScalePercent(settings.uiScaleMode)
                val scaleDescription = when {
                    currentScalePercent <= 90 -> "مدمج وصغير"
                    currentScalePercent <= 97 -> "متوسط مدمج"
                    currentScalePercent <= 103 -> "عادي واضح"
                    currentScalePercent <= 114 -> "كبير للسوق"
                    else -> "ضخم جداً"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.FormatSize,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "2. حجم الخط ونوع الخطوط",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (currentScalePercent != 100) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onUpdateUiScaleMode("100") }
                                .testTag("ui_scale_reset_100_button"),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "إعادة لـ 100%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Tactile (- / +) Stepper Bar from 85% to 122%
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Decrease (-) Button
                            val canDecrease = currentScalePercent > 85
                            Surface(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = canDecrease) {
                                        val nextVal = when {
                                            currentScalePercent > 100 && currentScalePercent - 3 < 100 -> 100
                                            else -> (currentScalePercent - 3).coerceAtLeast(85)
                                        }
                                        onUpdateUiScaleMode(nextVal.toString())
                                    }
                                    .testTag("ui_scale_decrease_button"),
                                color = if (canDecrease) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Remove,
                                        contentDescription = "تصغير الخط",
                                        tint = if (canDecrease) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        },
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // Center Live Percentage & Range Info
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "$currentScalePercent%",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.testTag("ui_scale_percent_text")
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = scaleDescription,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "تحكم مرن من 85% إلى 122% بأزرار (- / +)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Increase (+) Button
                            val canIncrease = currentScalePercent < 122
                            Surface(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = canIncrease) {
                                        val nextVal = when {
                                            currentScalePercent < 100 && currentScalePercent + 3 > 100 -> 100
                                            else -> (currentScalePercent + 3).coerceAtMost(122)
                                        }
                                        onUpdateUiScaleMode(nextVal.toString())
                                    }
                                    .testTag("ui_scale_increase_button"),
                                color = if (canIncrease) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = "تكبير الخط",
                                        tint = if (canIncrease) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        },
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        // Quick 1-Tap Jump Presets (85% | 92% | 100% | 112% | 122%)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val quickStops = listOf(
                                Triple(85, "85%", "ui_scale_85_button"),
                                Triple(92, "92%", "ui_scale_92_button"),
                                Triple(100, "100%", "ui_scale_normal_button"),
                                Triple(112, "112%", "ui_scale_large_button"),
                                Triple(122, "122%", "ui_scale_xl_button")
                            )
                            quickStops.forEach { (pct, label, tag) ->
                                val isPresetSelected = currentScalePercent == pct
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onUpdateUiScaleMode(pct.toString()) }
                                        .testTag(tag),
                                    color = if (isPresetSelected) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    },
                                    border = if (isPresetSelected) {
                                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
                                    } else {
                                        null
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isPresetSelected) {
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Font Family Selector (IBM Plex | Vazirmatn | Baloo Bhaijaan 2 | System)
                Text(
                    text = "نوع الخط المستخدم في التطبيق:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                val normalizedSelectedFont = when (settings.fontFamilyKey) {
                    "CAIRO", "TAJAWAL", "CUSTOM", "" -> "IBM_PLEX"
                    else -> settings.fontFamilyKey
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2
                ) {
                    val fontFamilies = listOf(
                        Triple("IBM_PLEX", "خط IBM Plex Arabic", "تقني وواضح للأرقام"),
                        Triple("VAZIRMATN", "خط وزير (Vazirmatn)", "متوازن ومريح للقراءة"),
                        Triple("RUBIK", "خط روبيك (Rubik)", "عصري وهندسي أنيق"),
                        Triple("BALOO", "خط Baloo Bhaijaan 2", "ودود وبارز العناوين"),
                        Triple("SYSTEM", "خط الهاتف (النظام)", "خط جهازك الأصلي")
                    )
                    fontFamilies.forEach { (fontKey, title, badge) ->
                        ScaleOptionTile(
                            title = title,
                            badge = badge,
                            selected = normalizedSelectedFont == fontKey,
                            onClick = { onUpdateFontFamilyKey(fontKey) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("font_family_${fontKey.lowercase()}_button")
                        )
                    }
                }

                // Live Typography Preview Box (Regular / Medium / Bold)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "معاينة الخط الحالي بالأوزان الثلاثة:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "عادي (Regular): مطيشة وبطاطا — 12.5 درهم (250 ريال)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "متوسط (Medium): زيت الزيتون وأتاي — 85 درهم (1700 ريال)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "عريض (Bold): الباقي في الجيب الآن 188.5 درهم",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 3. Moroccan Rial Toggle (إظهار أو إخفاء الريال)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onToggleShowRial)
                    .testTag("toggle_show_rial_card"),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CurrencyExchange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "إظهار الريال بجانب الدرهم",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تحويل تلقائي (1 درهم = 20 ريال) في الجيب وبطاقات السلع",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = settings.showRial,
                        onCheckedChange = { onToggleShowRial() }
                    )
                }
            }

            // 4. Privacy Eye (عين الخصوصية لإخفاء المبالغ بـ ***)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("toggle_privacy_mask_card"),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onTogglePrivacyMask),
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
                                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (settings.privacyMaskAmounts) {
                                        Icons.Outlined.VisibilityOff
                                    } else {
                                        Icons.Outlined.Visibility
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "عين الخصوصية (إخفاء المبالغ بـ ***)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "إخفاء الرصيد والمصاريف عن أعين المتطفلين في السوق بضغطة واحدة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = settings.privacyMaskAmounts,
                            onCheckedChange = { onTogglePrivacyMask() }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                    Text(
                        text = "نطاق الإخفاء عند تفعيل عين الخصوصية:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isAllSelected = !settings.privacyMaskPocketOnly
                        val isPocketOnlySelected = settings.privacyMaskPocketOnly

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onUpdatePrivacyMaskPocketOnly(false) }
                                .testTag("privacy_scope_all_button"),
                            color = if (isAllSelected) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            },
                            border = BorderStroke(
                                width = if (isAllSelected) 1.5.dp else 1.dp,
                                color = if (isAllSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "إخفاء المبالغ كلها",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isAllSelected) {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "الجيب + أثمنة السلع",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isAllSelected) {
                                        MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onUpdatePrivacyMaskPocketOnly(true) }
                                .testTag("privacy_scope_pocket_only_button"),
                            color = if (isPocketOnlySelected) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            },
                            border = BorderStroke(
                                width = if (isPocketOnlySelected) 1.5.dp else 1.dp,
                                color = if (isPocketOnlySelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "إخفاء المبلغ في الجيب فقط",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isPocketOnlySelected) {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "تبقى أثمنة السلع ظاهرة",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPocketOnlySelected) {
                                        MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // 5. PIN Lock Protection (قفل الكناش برمز سري 4 أرقام)
            Surface(
                modifier = Modifier.fillMaxWidth(),
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
                    val hasPin = settings.pinCode.isNotBlank()
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
                                    imageVector = if (hasPin) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "قفل التطبيق برمز سري (PIN)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (hasPin) {
                                        "الحماية مفعلة (يطلب رمزاً من 4 أرقام عند فتح التطبيق)"
                                    } else {
                                        "غير مفعل حالياً — يفتح الكناش مباشرة بدون رمز"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasPin) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }

                    if (!showPinEditor) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    draftPin = ""
                                    showPinEditor = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("setup_or_change_pin_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    text = if (hasPin) "تغيير الرمز" else "تفعيل رمز سري",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (hasPin) {
                                Button(
                                    onClick = {
                                        onLockAppNow()
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("lock_app_now_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Text(
                                        text = "قفل الآن",
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = onRemovePinCode,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("remove_pin_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "إلغاء الرمز",
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        // Built-in Tactile Numpad for Setting 4-Digit PIN
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "أدخل رمزاً سرياً من 4 أرقام لحماية الكناش:",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )

                                // 4 PIN Boxes
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (i in 0 until 4) {
                                        val charStr = draftPin.getOrNull(i)?.toString() ?: ""
                                        Surface(
                                            modifier = Modifier.size(52.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(
                                                width = if (charStr.isNotEmpty()) 2.dp else 1.dp,
                                                color = if (charStr.isNotEmpty()) {
                                                    MaterialTheme.colorScheme.secondary
                                                } else {
                                                    MaterialTheme.colorScheme.outline
                                                }
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = if (charStr.isNotEmpty()) charStr else "-",
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                // 3x4 PIN Numpad
                                val rows = listOf(
                                    listOf("7", "8", "9"),
                                    listOf("4", "5", "6"),
                                    listOf("1", "2", "3"),
                                    listOf("C", "0", "BACK")
                                )
                                rows.forEach { rowKeys ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowKeys.forEach { key ->
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        when (key) {
                                                            "C" -> draftPin = ""
                                                            "BACK" -> if (draftPin.isNotEmpty()) {
                                                                draftPin = draftPin.dropLast(1)
                                                            }
                                                            else -> if (draftPin.length < 4) {
                                                                draftPin += key
                                                            }
                                                        }
                                                    }
                                                    .testTag("pin_setup_key_$key"),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    if (key == "BACK") {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                                            contentDescription = "حذف رقم",
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    } else {
                                                        Text(
                                                            text = if (key == "C") "مسح" else key,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            color = if (key == "C") {
                                                                MaterialTheme.colorScheme.error
                                                            } else {
                                                                MaterialTheme.colorScheme.primary
                                                            },
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            draftPin = ""
                                            showPinEditor = false
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("إلغاء")
                                    }

                                    Button(
                                        onClick = {
                                            if (draftPin.length == 4) {
                                                onSetPinCode(draftPin)
                                                showPinEditor = false
                                                draftPin = ""
                                            }
                                        },
                                        enabled = draftPin.length == 4,
                                        modifier = Modifier
                                            .weight(1.4f)
                                            .height(48.dp)
                                            .testTag("confirm_save_pin_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        )
                                    ) {
                                        Text(
                                            text = "حفظ وتفعيل الرمز",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Backup & Restore + Home Screen Widget
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenBackupRestore)
                    .testTag("settings_open_backup_restore_card"),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
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
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "6. النسخ الاحتياطي الشامل وودجت الشاشة",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تصدير واستيراد كامل بيانات الكناش (JSON) وتثبيت ودجت الهاتف",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SettingsOptionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
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
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (selected) Icons.Outlined.CheckCircle else icon,
                contentDescription = title,
                tint = if (selected) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ScaleOptionTile(
    title: String,
    badge: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
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
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
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
