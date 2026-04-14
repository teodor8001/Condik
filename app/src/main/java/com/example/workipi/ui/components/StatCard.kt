package com.example.workipi.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Card statistica generica folosit in dashboard-uri.
 *
 * @param title     Eticheta cardului (ex: "Progres mediu proiecte")
 * @param value     Valoarea principala afisata mare (ex: "60%")
 * @param subtitle  Text secundar optional (ex: "din 4 proiecte active")
 * @param trend     Procent de schimbare fata de referinta; pozitiv = verde sus, negativ = rosu jos
 * @param icon      Iconita optionala afisata in coltul din dreapta-sus
 * @param hideable  Daca true, valoarea e ascunsa implicit si poate fi revelata cu iconita eye
 */
@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    trend: Float? = null,
    icon: ImageVector? = null,
    hideable: Boolean = false,
    modifier: Modifier = Modifier
) {
    var valueVisible by remember { mutableStateOf(!hideable) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Titlu + iconita dreapta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                // Daca e hideable, arata iconita eye; altfel iconita decorativa normala
                if (hideable) {
                    IconButton(
                        onClick = { valueVisible = !valueVisible },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (valueVisible)
                                Icons.Filled.Visibility
                            else
                                Icons.Filled.VisibilityOff,
                            contentDescription = if (valueVisible) "Ascunde valoarea" else "Arata valoarea",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Valoare principala — ascunsa sau vizibila
            if (valueVisible) {
                Text(
                    text = value,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            } else {
                Text(
                    text = "••••••",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Trend + subtitlu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (trend != null) {
                    val trendColor = if (trend >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    val trendIcon  = if (trend >= 0) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown
                    val trendText  = if (trend >= 0) "+%.1f%%".format(trend) else "%.1f%%".format(trend)

                    Icon(
                        imageVector = trendIcon,
                        contentDescription = null,
                        tint = trendColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = trendText,
                        style = MaterialTheme.typography.bodySmall,
                        color = trendColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}