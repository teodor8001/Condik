package com.example.workipi.ui.theme.generalUiComponents

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.workipi.ui.theme.CORNER_SHAPE
import com.example.workipi.ui.theme.DEFAULT_ICON_SIZE
import com.example.workipi.ui.theme.ELEVATION
import com.example.workipi.ui.theme.LARGER_SPACING
import com.example.workipi.ui.theme.PADDING
import com.example.workipi.ui.theme.WEIGHT

@Composable
private fun cardBorder(): BorderStroke =
    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

@Composable
fun LargerInformationCard(
    title: String,
    value: String,
    modifier: Modifier,
) {
    //TODO not required yet
}

@Composable
fun InformationCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        shape = RoundedCornerShape(CORNER_SHAPE.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = ELEVATION.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = cardBorder(),
    ) {
        Column(
            modifier = Modifier.padding(PADDING.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LARGER_SPACING.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(WEIGHT)
                )

                if (icon != null) {
                    Icon(
                        modifier = Modifier.size(DEFAULT_ICON_SIZE.dp),
                        imageVector = icon,
                        contentDescription = "",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            CondikValueText(value)
        }
    }
}

