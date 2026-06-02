package com.example.workipi.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.workipi.viewmodel.SkillMpBar
import kotlinx.coroutines.delay
import kotlin.math.max

private val BarColors = listOf(
    Color(0xFFE07B39),
    Color(0xFF2E7D32),
    Color(0xFF1565C0),
    Color(0xFFC62828),
)

@Composable
fun TotalMpBarChart(
    bars: List<SkillMpBar>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        text = "Total mp",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Top 4 lucrari",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (bars.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "mp",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            if (bars.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Nu exista pontari mp.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return@Column
            }

            BarsCanvas(bars, modifier = Modifier.fillMaxWidth().weight(1f))
            BarsLegend(bars)
        }
    }
}

@Composable
private fun BarsCanvas(bars: List<SkillMpBar>, modifier: Modifier = Modifier) {
    var animated by remember(bars) { mutableStateOf(false) }
    LaunchedEffect(bars) {
        animated = false
        delay(50)
        animated = true
    }
    val progress by animateFloatAsState(
        targetValue = if (animated) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "bar-progress",
    )

    val maxValue = max(1f, bars.maxOf { it.totalMp })

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val topPad = 20.dp.toPx() // loc pentru valoare deasupra barei
        val bottomPad = 4.dp.toPx()
        val plotH = h - topPad - bottomPad

        val barCount = bars.size
        val gap = 10.dp.toPx()
        val totalGap = gap * (barCount - 1)
        val barWidth = (w - totalGap) / barCount

        bars.forEachIndexed { idx, bar ->
            val color = BarColors[idx % BarColors.size]
            val targetH = plotH * (bar.totalMp / maxValue) * progress
            val x = idx * (barWidth + gap)
            val y = topPad + plotH - targetH

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(color, color.copy(alpha = 0.75f)),
                    startY = y,
                    endY = y + targetH,
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, targetH),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            )

            // Valoare deasupra barei (apare la final de animatie)
            if (progress > 0.8f) {
                val alpha = ((progress - 0.8f) / 0.2f).coerceIn(0f, 1f)
                val labelY = y - 4.dp.toPx()
                drawContext.canvas.nativeCanvas.drawText(
                    bar.totalMp.toInt().toString(),
                    x + barWidth / 2f,
                    labelY,
                    android.graphics.Paint().apply {
                        this.color = color.copy(alpha = alpha).toArgb()
                        textSize = 11.dp.toPx()
                        isAntiAlias = true
                        isFakeBoldText = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    },
                )
            }
        }
    }
}

@Composable
private fun BarsLegend(bars: List<SkillMpBar>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        bars.forEachIndexed { idx, bar ->
            val color = BarColors[idx % BarColors.size]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    text = bar.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${bar.totalMp.toInt()} mp",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                )
            }
        }
    }
}

