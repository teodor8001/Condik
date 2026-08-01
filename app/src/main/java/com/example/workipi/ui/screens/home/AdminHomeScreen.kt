package com.example.workipi.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import com.example.workipi.data.mock.MockSession
import com.example.workipi.ui.components.LocalOpenDrawer
import com.example.workipi.ui.components.StatCard
import com.example.workipi.ui.components.TimeNavLineChart
import com.example.workipi.ui.components.TotalMpBarChart
import com.example.workipi.viewmodel.ChartSeries
import com.example.workipi.viewmodel.HomeUiState
import com.example.workipi.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private fun signedPct(value: Float): String {
    val r = value.roundToInt()
    return (if (r > 0) "+" else "") + "$r%"
}

@Composable
private fun ProductieChartCard(
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Top 4 lucrari (mp / timp)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            TimeNavLineChart(
                series = series,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
fun AdminHomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val user = MockSession.currentUser ?: return
    val state by viewModel.uiState.collectAsState()
    val openDrawer = LocalOpenDrawer.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val fitsFourInRow = maxWidth >= 700.dp
        val hPad = if (maxWidth > 600.dp) 32.dp else 20.dp

        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true,
        ) { page ->
            when (page) {
                0 -> Page1(
                    user = user,
                    state = state,
                    openDrawer = openDrawer,
                    fitsFourInRow = fitsFourInRow,
                    hPad = hPad,
                    onGoNext = { scope.launch { pagerState.animateScrollToPage(1) } },
                )
                1 -> Page2(
                    state = state,
                    hPad = hPad,
                    onGoBack = { scope.launch { pagerState.animateScrollToPage(0) } },
                )
            }
        }

        // Indicator dot pe centru-dreapta — feedback de pozitie
        PagerDots(
            currentPage = pagerState.currentPage,
            count = 2,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
        )
    }
}

@Composable
private fun Page1(
    user: com.example.workipi.data.model.MockUser,
    state: HomeUiState,
    openDrawer: (() -> Unit)?,
    fitsFourInRow: Boolean,
    hPad: androidx.compose.ui.unit.Dp,
    onGoNext: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (openDrawer != null) 4.dp else hPad, end = hPad, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (openDrawer != null) {
                IconButton(onClick = { openDrawer() }) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Deschide meniu",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                state.companyName?.let { firma ->
                    Text(
                        text = firma,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = "Buna ziua, ${user.name.split(" ").first()}!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Iata un sumar al activitatii curente",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = hPad)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Statistici generale",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            state.errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            val cards = listOf<@Composable (Modifier) -> Unit>(
                { mod ->
                    StatCard(
                        title = "Proiecte active",
                        value = "${state.activeProjects}",
                        subtitle = "in lucru acum",
                        icon = Icons.Filled.AssignmentTurnedIn,
                        modifier = mod,
                    )
                },
                { mod ->
                    StatCard(
                        title = "Media mp / zi",
                        value = "${state.avgMpPerDay.toInt()}",
                        subtitle = "ultimele 30 zile",
                        icon = Icons.Filled.SquareFoot,
                        modifier = mod,
                    )
                },
                { mod ->
                    StatCard(
                        title = "Oameni in santier",
                        value = "${state.peopleCheckedIn}",
                        subtitle = "checked-in",
                        icon = Icons.Filled.Groups,
                        modifier = mod,
                    )
                },
                { mod ->
                    StatCard(
                        title = "Revizii de facut",
                        value = "${state.inspectionsToDo}",
                        subtitle = "in asteptare",
                        icon = Icons.Filled.FactCheck,
                        modifier = mod,
                    )
                },
            )

            if (fitsFourInRow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    cards.forEach { card -> card(Modifier.weight(1f)) }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    cards.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            pair.forEach { card -> card(Modifier.weight(1f)) }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            // Rand grafice — ocupa restul spatiului vertical
            if (fitsFourInRow) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ProductieChartCard(
                        series = state.chartSeriesFull,
                        modifier = Modifier.weight(0.7f).fillMaxHeight(),
                    )
                    TotalMpBarChart(
                        bars = state.barChart,
                        modifier = Modifier.weight(0.3f).fillMaxHeight(),
                    )
                }
            } else {
                ProductieChartCard(
                    series = state.chartSeriesFull,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                )
                TotalMpBarChart(
                    bars = state.barChart,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Sageata jos — buton tonal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            FilledTonalIconButton(
                onClick = onGoNext,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = "Mai multe",
                )
            }
        }
    }
}

@Composable
private fun Page2(
    state: HomeUiState,
    hPad: androidx.compose.ui.unit.Dp,
    onGoBack: () -> Unit,
) {
    val fitsFourInRow = LocalConfigurationWidthDp() >= 700

    Column(modifier = Modifier.fillMaxSize()) {
        // Sageata sus — buton tonal pentru a reveni la pagina 1
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            FilledTonalIconButton(
                onClick = onGoBack,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.ExpandLess,
                    contentDescription = "Inapoi",
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = hPad)
                .padding(top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ---- Rand 1: 4 carduri financiare ----
            CardsRow(
                fitsFourInRow = fitsFourInRow,
                cards = listOf(
                    @Composable { mod: Modifier ->
                        StatCard(
                            title = "Progres firma",
                            value = state.companyProgress?.let { signedPct(it) } ?: "—",
                            subtitle = "ultimele 2 contracte",
                            trend = state.companyProgress,
                            icon = Icons.Filled.TrendingUp,
                            modifier = mod,
                        )
                    },
                    @Composable { mod: Modifier ->
                        StatCard(
                            title = "Eficienta",
                            value = "${state.efficiency.roundToInt()}%",
                            subtitle = "media din ${state.finalizedCount} finalizate",
                            icon = Icons.Filled.Speed,
                            modifier = mod,
                        )
                    },
                    @Composable { mod: Modifier ->
                        StatCard(
                            title = "Bugete in lucru",
                            value = "${state.budgetsInProgress.roundToInt()}%",
                            subtitle = "media toate proiectele",
                            icon = Icons.Filled.Savings,
                            modifier = mod,
                        )
                    },
                    @Composable { mod: Modifier ->
                        StatCard(
                            title = "Profit anticipat",
                            value = "${state.anticipatedProfit.roundToInt()} RON",
                            subtitle = "din ${state.activeCount} active",
                            icon = Icons.Filled.TrendingUp,
                            modifier = mod,
                        )
                    },
                ),
            )

            // ---- Rand 2: 2 grafice (50/50) ----
            if (fitsFourInRow) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PlaceholderChartCard(
                        title = "Grafic 1",
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    PlaceholderChartCard(
                        title = "Grafic 2",
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            } else {
                PlaceholderChartCard(
                    title = "Grafic 1",
                    modifier = Modifier.fillMaxWidth(),
                )
                PlaceholderChartCard(
                    title = "Grafic 2",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ---- Rand 3: 4 carduri ----
            CardsRow(
                fitsFourInRow = fitsFourInRow,
                cards = listOf(
                    @Composable { mod: Modifier ->
                        StatCard(
                            title = "Proiecte viitoare",
                            value = "${state.offersCount}",
                            subtitle = "oferte",
                            icon = Icons.Filled.Work,
                            modifier = mod,
                        )
                    },
                    @Composable { mod: Modifier ->
                        StatCard(
                            title = "Proiecte incheiate",
                            value = "${state.finalizedCount}",
                            subtitle = "finalizate",
                            icon = Icons.Filled.AssignmentTurnedIn,
                            modifier = mod,
                        )
                    },
                    @Composable { mod: Modifier ->
                        StatCard(
                            title = "Bugete totale",
                            value = "${state.totalBudget.roundToInt()} RON",
                            icon = Icons.Filled.Savings,
                            modifier = mod,
                        )
                    },
                    @Composable { mod: Modifier ->
                        StatCard(
                            title = "Profit total",
                            value = "${state.totalProfit.roundToInt()} RON",
                            icon = Icons.Filled.TrendingUp,
                            modifier = mod,
                        )
                    },
                ),
            )
        }
    }
}

@Composable
private fun CardsRow(
    fitsFourInRow: Boolean,
    cards: List<@Composable (Modifier) -> Unit>,
) {
    if (fitsFourInRow) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            cards.forEach { card -> card(Modifier.weight(1f)) }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            cards.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    pair.forEach { card -> card(Modifier.weight(1f)) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PlaceholderStatCard(label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlaceholderChartCard(title: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LocalConfigurationWidthDp(): Int {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    return configuration.screenWidthDp
}

@Composable
private fun PagerDots(
    currentPage: Int,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(count) { i ->
            val active = i == currentPage
            Box(
                modifier = Modifier
                    .size(width = if (active) 8.dp else 6.dp, height = if (active) 22.dp else 6.dp)
                    .clip(if (active) RoundedCornerShape(4.dp) else CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
            )
        }
    }
}
