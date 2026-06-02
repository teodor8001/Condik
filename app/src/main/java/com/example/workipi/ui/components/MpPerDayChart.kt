package com.example.workipi.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workipi.viewmodel.SkillMpSeries
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt

private val SeriesColors = listOf(
    Color(0xFFE07B39),
    Color(0xFF2E7D32),
    Color(0xFF1565C0),
    Color(0xFFC62828),
)

private val MonthsRoShort = listOf(
    "ian", "feb", "mar", "apr", "mai", "iun",
    "iul", "aug", "sep", "oct", "nov", "dec",
)

private fun LocalDate.formatRoShort(): String =
    "$dayOfMonth ${MonthsRoShort[monthNumber - 1]}"

// Padduri folosite atat in DrawScope cat si in pointerInput → constante
private val LeftPadDp = 44.dp
private val RightPadDp = 12.dp
private val TopPadDp = 12.dp
private val BottomPadDp = 30.dp
private val ChartHeightDp = 240.dp

@Composable
fun MpPerDayChart(
    series: List<SkillMpSeries>,
    chartDays: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        text = "Mp / zi pe lucrare",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Top 4 lucrari • ultimele $chartDays zile",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (series.isNotEmpty()) {
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

            if (series.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Nu exista pontari mp in ultimele $chartDays zile.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return@Column
            }

            ChartLegend(series)

            ChartBody(series = series, chartDays = chartDays)
        }
    }
}

@Composable
private fun ChartBody(series: List<SkillMpSeries>, chartDays: Int) {
    val density = LocalDensity.current
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val cardColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val tooltipBg = MaterialTheme.colorScheme.surface
    val tooltipBorder = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    val maxValue = max(
        1f,
        series.flatMap { it.points }.maxOfOrNull { it.mp } ?: 1f,
    )
    val dates: List<LocalDate> = series.firstOrNull()?.points?.map { it.date } ?: emptyList()

    // Animatie linii (0 → 1) la prima compozitie / cand seria se schimba
    var animated by remember(series) { mutableStateOf(false) }
    LaunchedEffect(series) {
        animated = false
        delay(50)
        animated = true
    }
    val progress by animateFloatAsState(
        targetValue = if (animated) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "chart-progress",
    )

    // Tap state — index zi selectata
    var tappedIndex by remember(series) { mutableStateOf<Int?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val leftPadPx = with(density) { LeftPadDp.toPx() }
    val rightPadPx = with(density) { RightPadDp.toPx() }
    val topPadPx = with(density) { TopPadDp.toPx() }
    val bottomPadPx = with(density) { BottomPadDp.toPx() }

    Box(modifier = Modifier.fillMaxWidth().height(ChartHeightDp)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(series) {
                    detectTapGestures { offset ->
                        val w = size.width.toFloat()
                        val plotW = w - leftPadPx - rightPadPx
                        if (offset.x < leftPadPx || offset.x > w - rightPadPx) {
                            tappedIndex = null
                            return@detectTapGestures
                        }
                        val rel = (offset.x - leftPadPx) / plotW
                        val idx = (rel * (chartDays - 1)).roundToInt()
                            .coerceIn(0, chartDays - 1)
                        tappedIndex = if (tappedIndex == idx) null else idx
                    }
                }
        ) {
            canvasSize = IntSize(size.width.toInt(), size.height.toInt())
            val w = size.width
            val h = size.height
            val plotW = w - leftPadPx - rightPadPx
            val plotH = h - topPadPx - bottomPadPx

            // Grid orizontal
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = topPadPx + (plotH * i / gridLines)
                drawLine(
                    color = gridColor,
                    start = Offset(leftPadPx, y),
                    end = Offset(w - rightPadPx, y),
                    strokeWidth = 1f,
                    pathEffect = if (i == 0 || i == gridLines) null
                    else PathEffect.dashPathEffect(floatArrayOf(4f, 8f)),
                )
                val value = (maxValue * (gridLines - i) / gridLines).toInt()
                drawContext.canvas.nativeCanvas.drawText(
                    "$value",
                    4f,
                    y + 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = labelColor.toArgb()
                        textSize = 10.sp.toPx()
                        isAntiAlias = true
                    },
                )
            }

            // Pre-calc puncte pentru toate seriile
            val seriesPoints: List<List<Offset>> = series.map { s ->
                s.points.mapIndexed { i, p ->
                    val px = leftPadPx + plotW * i / (s.points.size - 1).coerceAtLeast(1)
                    val py = topPadPx + plotH * (1f - p.mp / maxValue)
                    Offset(px, py)
                }
            }

            // Cliprect pentru animatia "creste-stanga-dreapta"
            clipRect(
                left = leftPadPx,
                top = 0f,
                right = leftPadPx + plotW * progress,
                bottom = h,
            ) {
                series.forEachIndexed { idx, _ ->
                    val color = SeriesColors[idx % SeriesColors.size]
                    val pts = seriesPoints[idx]
                    if (pts.isEmpty()) return@forEachIndexed

                    val linePath = smoothPath(pts)
                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(pts.last().x, topPadPx + plotH)
                        lineTo(pts.first().x, topPadPx + plotH)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.18f),
                                color.copy(alpha = 0.0f),
                            ),
                            startY = topPadPx,
                            endY = topPadPx + plotH,
                        ),
                    )
                    drawPath(
                        path = linePath,
                        color = color,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }

            // Cerc pe ultimul punct (azi) — apare la finalul animatiei
            if (progress >= 0.99f) {
                series.forEachIndexed { idx, _ ->
                    val color = SeriesColors[idx % SeriesColors.size]
                    val pts = seriesPoints[idx]
                    if (pts.isEmpty()) return@forEachIndexed
                    val last = pts.last()
                    drawCircle(color = cardColor, radius = 6.dp.toPx(), center = last)
                    drawCircle(color = color, radius = 4.5f.dp.toPx(), center = last)
                }
            }

            // Linie verticala + puncte la indexul selectat
            tappedIndex?.let { idx ->
                if (idx !in dates.indices) return@let
                val xpos = leftPadPx + plotW * idx / (chartDays - 1).coerceAtLeast(1)
                drawLine(
                    color = onSurfaceColor.copy(alpha = 0.4f),
                    start = Offset(xpos, topPadPx),
                    end = Offset(xpos, topPadPx + plotH),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
                )
                seriesPoints.forEachIndexed { sIdx, pts ->
                    if (idx in pts.indices) {
                        val color = SeriesColors[sIdx % SeriesColors.size]
                        drawCircle(color = cardColor, radius = 5.dp.toPx(), center = pts[idx])
                        drawCircle(color = color, radius = 3.5f.dp.toPx(), center = pts[idx])
                    }
                }
            }

            // Etichete X
            val xLabelPaint = android.graphics.Paint().apply {
                color = labelColor.toArgb()
                textSize = 10.sp.toPx()
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
            }
            val todayLabelPaint = android.graphics.Paint().apply {
                color = SeriesColors[0].toArgb()
                textSize = 10.sp.toPx()
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            val step = 5
            val indices = ((0 until chartDays step step) + (chartDays - 1)).distinct().sorted()
            indices.forEach { i ->
                if (i >= dates.size) return@forEach
                val px = leftPadPx + plotW * i / (chartDays - 1).coerceAtLeast(1)
                val isToday = i == chartDays - 1
                drawContext.canvas.nativeCanvas.drawText(
                    if (isToday) "azi" else dates[i].formatRoShort(),
                    px,
                    h - 8.dp.toPx(),
                    if (isToday) todayLabelPaint else xLabelPaint,
                )
            }
        }

        // Tooltip overlay (Composable)
        tappedIndex?.let { idx ->
            if (idx in dates.indices && canvasSize.width > 0) {
                val plotW = canvasSize.width.toFloat() - leftPadPx - rightPadPx
                val xPx = leftPadPx + plotW * idx / (chartDays - 1).coerceAtLeast(1)
                val tooltipWidthDp = 180.dp
                val tooltipWidthPx = with(density) { tooltipWidthDp.toPx() }
                val xClamped = xPx.coerceIn(
                    tooltipWidthPx / 2,
                    canvasSize.width - tooltipWidthPx / 2,
                )
                val xDp = with(density) { (xClamped - tooltipWidthPx / 2).toDp() }
                TooltipBox(
                    date = dates[idx],
                    rows = series.mapIndexed { sIdx, s ->
                        TooltipRow(
                            color = SeriesColors[sIdx % SeriesColors.size],
                            name = s.name,
                            value = s.points[idx].mp,
                        )
                    },
                    bg = tooltipBg,
                    border = tooltipBorder,
                    modifier = Modifier
                        .padding(start = xDp, top = 8.dp)
                        .width(tooltipWidthDp),
                )
            }
        }
    }
}

private data class TooltipRow(val color: Color, val name: String, val value: Float)

@Composable
private fun TooltipBox(
    date: LocalDate,
    rows: List<TooltipRow>,
    bg: Color,
    border: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(1.dp)
            .background(bg, RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = date.formatRoShort(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            rows.forEach { r ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(r.color),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = r.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${r.value.toInt()} mp",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = r.color,
                    )
                }
            }
        }
    }
}

/**
 * Monotone cubic Hermite spline (Fritsch-Carlson). Garanteaza ca interpolarea NU
 * overshoot-eaza dincolo de valorile date — curba ramane in [min(y), max(y)] pe
 * fiecare segment. Esential pentru date care nu pot fi negative (ex. mp lucrati).
 */
private fun smoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    if (points.size == 1) {
        path.moveTo(points[0].x, points[0].y)
        return path
    }

    val n = points.size
    val dx = FloatArray(n - 1)
    val dy = FloatArray(n - 1)
    val secant = FloatArray(n - 1)
    for (i in 0 until n - 1) {
        dx[i] = points[i + 1].x - points[i].x
        dy[i] = points[i + 1].y - points[i].y
        secant[i] = if (dx[i] != 0f) dy[i] / dx[i] else 0f
    }

    // Tangentele initiale
    val m = FloatArray(n)
    m[0] = secant[0]
    m[n - 1] = secant[n - 2]
    for (i in 1 until n - 1) {
        // Schimbare de semn intre secante (sau una zero) → tangenta = 0 la punctul ala.
        // Asta evita overshoot-ul la minime/maxime locale.
        m[i] = if (secant[i - 1] * secant[i] <= 0f) 0f
        else (secant[i - 1] + secant[i]) / 2f
    }

    // Constrangerea Fritsch-Carlson pentru monotonicitate stricta
    for (i in 0 until n - 1) {
        if (secant[i] == 0f) {
            m[i] = 0f
            m[i + 1] = 0f
        } else {
            val a = m[i] / secant[i]
            val b = m[i + 1] / secant[i]
            val h = a * a + b * b
            if (h > 9f) {
                val t = 3f / kotlin.math.sqrt(h)
                m[i] = t * a * secant[i]
                m[i + 1] = t * b * secant[i]
            }
        }
    }

    // Hermite → Bezier: control points la 1/3 din segment
    path.moveTo(points[0].x, points[0].y)
    for (i in 0 until n - 1) {
        val c1x = points[i].x + dx[i] / 3f
        val c1y = points[i].y + m[i] * dx[i] / 3f
        val c2x = points[i + 1].x - dx[i] / 3f
        val c2y = points[i + 1].y - m[i + 1] * dx[i] / 3f
        path.cubicTo(c1x, c1y, c2x, c2y, points[i + 1].x, points[i + 1].y)
    }
    return path
}

@Composable
private fun ChartLegend(series: List<SkillMpSeries>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        series.forEachIndexed { idx, s ->
            val color = SeriesColors[idx % SeriesColors.size]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(color.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = s.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = color,
                )
            }
        }
    }
}
