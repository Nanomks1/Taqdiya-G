package az.pdr.taqdiya2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import az.pdr.taqdiya2.ui.theme.ambientCardBorder
import az.pdr.taqdiya2.ui.theme.ambientCardShadow
import az.pdr.taqdiya2.util.CurrencyFormatter

@Composable
fun TopPocketBar(
    remainingPocketDirhams: Double,
    initialPocketDirhams: Double,
    totalActualSpentDirhams: Double = 0.0,
    totalMissingItemsCount: Int,
    privacyMaskAmounts: Boolean = CurrencyFormatter.privacyMaskState,
    showRial: Boolean = CurrencyFormatter.showRialState,
    onTogglePrivacyMask: () -> Unit = {},
    onOpenSettingsClick: () -> Unit = {},
    onEditBudgetClick: () -> Unit,
    onOpenToolsSheetClick: () -> Unit,
    onShareListClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            // Row 1: Compact App Title + Soft Item Count Badge + Floating Action Pills
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
                        text = "التقدية",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier.testTag("top_bar_missing_count_badge")
                    ) {
                        Text(
                            text = "$totalMissingItemsCount أغراض",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pillShape = RoundedCornerShape(10.dp)
                    Surface(
                        modifier = Modifier
                            .heightIn(min = 32.dp)
                            .ambientCardShadow(shape = pillShape, elevation = 2.dp)
                            .clip(pillShape)
                            .clickable(onClick = onShareListClick)
                            .testTag("share_list_button"),
                        color = MaterialTheme.colorScheme.surface,
                        border = ambientCardBorder(),
                        shape = pillShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "مشاركة القائمة",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "واتساب",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .heightIn(min = 32.dp)
                            .ambientCardShadow(shape = pillShape, elevation = 2.dp)
                            .clip(pillShape)
                            .clickable(onClick = onOpenToolsSheetClick)
                            .testTag("open_tools_button"),
                        color = MaterialTheme.colorScheme.surface,
                        border = ambientCardBorder(),
                        shape = pillShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = "أدوات الكناش",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "الأدوات",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .size(32.dp)
                            .ambientCardShadow(shape = pillShape, elevation = 2.dp)
                            .clip(pillShape)
                            .clickable(onClick = onOpenSettingsClick)
                            .testTag("open_settings_button"),
                        color = MaterialTheme.colorScheme.surface,
                        border = ambientCardBorder(),
                        shape = pillShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "الإعدادات والخصوصية",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Prominent Pocket Hero Card ("الباقي في الجيب الآن") with slim vertical height
            val heroShape = RoundedCornerShape(15.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .ambientCardShadow(shape = heroShape, elevation = 5.dp)
                    .clip(heroShape)
                    .clickable(onClick = onEditBudgetClick)
                    .testTag("edit_pocket_budget_button"),
                color = MaterialTheme.colorScheme.primary,
                shape = heroShape
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Right side: Duotone Wallet Container + Label + Dirham & Rial amounts
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccountBalanceWallet,
                                contentDescription = "ميزانية الجيب",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(9.dp))

                        val isPocketUnset = initialPocketDirhams <= 0.0 && totalActualSpentDirhams <= 0.0

                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = when {
                                    privacyMaskAmounts -> "الباقي في الجيب (مخفي)"
                                    isPocketUnset -> "لم تحدد مبلغ الجيب بعد"
                                    else -> "الباقي في الجيب الآن"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (isPocketUnset && !privacyMaskAmounts) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "0 درهم",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = RoundedCornerShape(7.dp)
                                    ) {
                                        Text(
                                            text = "حدد المبلغ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = CurrencyFormatter.formatPocketDirhams(remainingPocketDirhams),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )

                                    if (showRial) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(7.dp)
                                        ) {
                                            Text(
                                                text = CurrencyFormatter.formatPocketRials(remainingPocketDirhams),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Left side: Privacy Eye & Edit Budget Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (privacyMaskAmounts) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)
                                    }
                                )
                                .clickable(onClick = onTogglePrivacyMask)
                                .testTag("privacy_eye_toggle_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (privacyMaskAmounts) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (privacyMaskAmounts) "إظهار المبالغ" else "إخفاء المبالغ بعين الخصوصية",
                                tint = if (privacyMaskAmounts) {
                                    MaterialTheme.colorScheme.onSecondary
                                } else {
                                    MaterialTheme.colorScheme.onPrimary
                                },
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "تعديل ميزانية الجيب",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
}
