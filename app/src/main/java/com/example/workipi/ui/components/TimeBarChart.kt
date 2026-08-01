package com.example.workipi.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.example.workipi.viewmodel.ChartSeries
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt

private enum class ChartRange(val label: String) { WEEK("Saptamana"), MONTH("Luna"), YEAR("An") }

private data class Window(val title: String, val labels: List<String>, val bucketOf: (LocalDate) -> Int)

private val seriesPalette = listOf(
    Color(0xFFE07B39), // portocaliu brand
    Color(0xFF1565C0), // albastru
    Color(0xFF2E7D32), // verde
    Color(0xFF7E57C2), // mov
)

/**
 * Grafic cu linii (xOy), multi-serie (ex. top 4 lucrari), cu dropdown de granularitate
 * (saptamana / luna / an) si navigare stanga/dreapta intre perioade (fara slide).
 * Refolosit de graficul din Detalii proiect si de cel din Acasa.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimeNavLineChart(
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
) {
    var range by remember { mutableStateOf(ChartRange.WEEK) }
    var offset by remember { mutableStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    LaunchedEffect(range) { offset = 0 }

    val window = remember(range, offset) { windowFor(range, offset) }
    val seriesValues = remember(series, window) {
        series.map { s ->
            val arr = DoubleArray(window.labels.size)
            s.points.forEach { p ->
                val b = window.bucketOf(p.date)
                if (b in arr.indices) arr[b] += p.quantity
            }
            s.name to arr.toList()
        }
    }
    val maxV = seriesValues.flatMap { it.second }.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = window.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Box {
                OutlinedButton(onClick = { menu = true }, shape = RoundedCornerShape(10.dp)) {
                    Text(range.label, style = MaterialTheme.typography.bodySmall)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    ChartRange.entries.forEach { r ->
                        DropdownMenuItem(text = { Text(r.label) }, onClick = { range = r; menu = false })
                    }
                }
            }
        }

        if (series.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                seriesValues.forEachIndexed { i, (name, _) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(seriesPalette[i % seriesPalette.size]),
                        )
                        Text(name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { offset -= 1 }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Perioada anterioara")
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                if (seriesValues.all { pair -> pair.second.all { it == 0.0 } }) {
                    Text(
                        text = "Fara date in aceasta perioada",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LineChart(
                        seriesValues = seriesValues,
                        labels = window.labels,
                        maxV = maxV,
                        axisColor = MaterialTheme.colorScheme.outline,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            IconButton(onClick = { offset += 1 }, enabled = offset < 0) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Perioada urmatoare")
            }
        }
    }
}

@Composable
private fun LineChart(
    seriesValues: List<Pair<String, List<Double>>>,
    labels: List<String>,
    maxV: Double,
    axisColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    Canvas(modifier = modifier) {
        val leftPad = 36f
        val bottomPad = 18f
        val plotW = (size.width - leftPad).coerceAtLeast(1f)
        val plotH = (size.height - bottomPad).coerceAtLeast(1f)
        val n = labels.size.coerceAtLeast(1)
        fun xAt(i: Int): Float = leftPad + if (n > 1) i.toFloat() / (n - 1) * plotW else plotW / 2
        fun yAt(v: Double): Float = (plotH - (v / maxV * plotH)).toFloat()

        // Axe
        drawLine(axisColor, Offset(leftPad, 0f), Offset(leftPad, plotH), strokeWidth = 2f)
        drawLine(axisColor, Offset(leftPad, plotH), Offset(size.width, plotH), strokeWidth = 2f)

        // Gridlines orizontale + etichete pe Y (0, jumatate, max)
        listOf(0.0, maxV / 2, maxV).forEach { gv ->
            val y = yAt(gv)
            drawLine(axisColor.copy(alpha = 0.25f), Offset(leftPad, y), Offset(size.width, y), strokeWidth = 1f)
            val layout = measurer.measure(AnnotatedString(gv.roundToInt().toString()), labelStyle)
            drawText(layout, topLeft = Offset(0f, y - layout.size.height / 2f))
        }

        // Etichete pe X
        labels.forEachIndexed { i, lbl ->
            if (lbl.isNotEmpty()) {
                val layout = measurer.measure(AnnotatedString(lbl), labelStyle)
                drawText(layout, topLeft = Offset(xAt(i) - layout.size.width / 2f, plotH + 2f))
            }
        }

        // Liniile seriilor
        seriesValues.forEachIndexed { si, pair ->
            val values = pair.second
            val color = seriesPalette[si % seriesPalette.size]
            for (i in 0 until values.size - 1) {
                drawLine(
                    color = color,
                    start = Offset(xAt(i), yAt(values[i])),
                    end = Offset(xAt(i + 1), yAt(values[i + 1])),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private fun windowFor(range: ChartRange, offset: Int): Window {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return when (range) {
        ChartRange.WEEK -> {
            val anchor = today.plus(offset * 7, DateTimeUnit.DAY)
            val monday = anchor.minus(anchor.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
            val sunday = monday.plus(6, DateTimeUnit.DAY)
            Window(
                title = "${monday.dayOfMonth} ${monthShortRo(monday.monthNumber)} - ${sunday.dayOfMonth} ${monthShortRo(sunday.monthNumber)}",
                labels = (0..6).map { dayShortRo(monday.plus(it, DateTimeUnit.DAY).dayOfWeek.isoDayNumber) },
                bucketOf = { d -> if (d in monday..sunday) (d.toEpochDays() - monday.toEpochDays()) else -1 },
            )
        }
        ChartRange.MONTH -> {
            val anchor = LocalDate(today.year, today.monthNumber, 1).plus(offset, DateTimeUnit.MONTH)
            val days = anchor.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth
            Window(
                title = "${monthNameRo(anchor.monthNumber)} ${anchor.year}",
                labels = (1..days).map { if ((it - 1) % 5 == 0) it.toString() else "" },
                bucketOf = { d -> if (d.year == anchor.year && d.monthNumber == anchor.monthNumber) d.dayOfMonth - 1 else -1 },
            )
        }
        ChartRange.YEAR -> {
            val year = today.year + offset
            Window(
                title = "$year",
                labels = (1..12).map { monthShortRo(it) },
                bucketOf = { d -> if (d.year == year) d.monthNumber - 1 else -1 },
            )
        }
    }
}

private fun dayShortRo(iso: Int): String =
    listOf("Lu", "Ma", "Mi", "Jo", "Vi", "Sa", "Du")[(iso - 1).coerceIn(0, 6)]

private fun monthShortRo(m: Int): String =
    listOf("Ian", "Feb", "Mar", "Apr", "Mai", "Iun", "Iul", "Aug", "Sep", "Oct", "Noi", "Dec")[(m - 1).coerceIn(0, 11)]

private fun monthNameRo(m: Int): String =
    listOf("Ianuarie", "Februarie", "Martie", "Aprilie", "Mai", "Iunie", "Iulie", "August", "Septembrie", "Octombrie", "Noiembrie", "Decembrie")[(m - 1).coerceIn(0, 11)]
