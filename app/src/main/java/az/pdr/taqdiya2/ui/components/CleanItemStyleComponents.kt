package az.pdr.taqdiya2.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import az.pdr.taqdiya2.ui.theme.LocalIsDarkTheme

/**
 * Calm outlined action pill matching the ultra-clean 8E visual style:
 * neutral surface, thin crisp border, dark neutral label, and optional primary-colored icon (e.g., '+').
 */
@Composable
fun CalmOutlinedActionButton(
    text: String,
    icon: ImageVector,
    iconContentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val pillShape = RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier
            .heightIn(min = 38.dp)
            .clip(pillShape)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.85f)),
        shape = pillShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = iconContentDescription,
                tint = iconTint,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Soft tinted card action button matching Screenshot 5 primary card CTA
 * (e.g., quick buy "شراء بـ 20 د.م" or sole "احسب الثمن"):
 * gentle primary-tinted fill, subtle primary border, primary icon & bold label.
 */
@Composable
fun CalmTintedCardButton(
    text: String,
    icon: ImageVector,
    iconContentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val isDark = LocalIsDarkTheme.current
    val buttonShape = RoundedCornerShape(14.dp)
    Surface(
        modifier = modifier
            .heightIn(min = 46.dp)
            .clip(buttonShape)
            .clickable(onClick = onClick),
        color = accentColor.copy(alpha = if (isDark) 0.16f else 0.09f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = if (isDark) 0.42f else 0.30f)),
        shape = buttonShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = iconContentDescription,
                tint = accentColor,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Calm neutral outlined card action button matching Screenshot 5 secondary card CTA
 * (e.g., secondary "احسب الثمن" alongside quick buy):
 * subtle off-surface fill, crisp neutral border, muted icon & dark neutral bold label.
 */
@Composable
fun CalmNeutralCardButton(
    text: String,
    icon: ImageVector,
    iconContentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val buttonShape = RoundedCornerShape(14.dp)
    Surface(
        modifier = modifier
            .heightIn(min = 46.dp)
            .clip(buttonShape)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.35f else 0.30f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.85f)),
        shape = buttonShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = iconContentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Ultra-clean item row anatomy (style 8E):
 * Start (Right in RTL): Hollow primary ring dot (○) + primary-colored bold item name.
 * End (Left in RTL): Crisp plain note/quantity/price text + thin vertical divider (|) + calm minimal Edit & Delete icons.
 */
@Composable
fun CleanShoppingItemHeaderRow(
    itemName: String,
    itemNote: String,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    editContentDescription: String = "تعديل الغرض",
    deleteContentDescription: String = "إزالة من القائمة",
    editTestTag: String? = null,
    deleteTestTag: String? = null
) {
    val mutedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Start (Right in RTL): Hollow primary ring dot + primary bold item name
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = itemName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // End (Left in RTL): Plain note + thin vertical divider + borderless Edit & Delete icons
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (itemNote.isNotBlank()) {
                Text(
                    text = itemNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 120.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.8f))
                )
                Spacer(modifier = Modifier.width(2.dp))
            }

            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .size(42.dp)
                    .then(if (editTestTag != null) Modifier.testTag(editTestTag) else Modifier)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = editContentDescription,
                    tint = mutedIconColor,
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .size(42.dp)
                    .then(if (deleteTestTag != null) Modifier.testTag(deleteTestTag) else Modifier)
            ) {
                Icon(
                    imageVector = Icons.Outlined.RemoveCircleOutline,
                    contentDescription = deleteContentDescription,
                    tint = mutedIconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
