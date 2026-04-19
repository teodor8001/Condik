package com.example.workipi.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.workipi.data.mock.MockData
import com.example.workipi.data.mock.MockSession
import com.example.workipi.ui.components.LocalOpenDrawer
import com.example.workipi.ui.components.StatCard
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AdminHomeScreen(navController: NavController) {
    val user       = MockSession.currentUser ?: return
    val projects   = MockData.projects
    val openDrawer = LocalOpenDrawer.current

    // ---- Metrici calculate ----
    val avgProgress = projects.map { it.progress }.average().toFloat()

    val totalLastMonth  = projects.sumOf { it.revenueLastMonth }
    val totalThisMonth  = projects.sumOf { it.revenueThisMonth }
    val revenueChangePct = if (totalLastMonth > 0)
        ((totalThisMonth - totalLastMonth) / totalLastMonth * 100).toFloat()
    else 0f

    val estimatedGross = projects.sumOf { it.budget - it.currentCosts }

    // Formatare RON
    val ronFormatter = NumberFormat.getNumberInstance(Locale("ro", "RO")).apply {
        maximumFractionDigits = 0
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isTabletLandscape = maxWidth > 600.dp
        val hPad = if (isTabletLandscape) 32.dp else 20.dp

        Column(modifier = Modifier.fillMaxSize()) {

            // ---- Header FIX (nu scrolleaza pe telefon) ----
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

            // ---- Continut scrollabil ----
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = hPad)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
            // ---- Sectiune Statistici Generale ----
            Text(
                text = "Statistici generale",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (isTabletLandscape) {
                // Tabletă: 3 carduri pe un rand
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        title     = "Progres mediu proiecte",
                        value     = "${(avgProgress * 100).roundToInt()}%",
                        subtitle  = "din ${projects.size} proiecte active",
                        icon      = Icons.Filled.BarChart,
                        modifier  = Modifier.weight(1f)
                    )
                    StatCard(
                        title     = "Venit fata de luna trecuta",
                        value     = if (revenueChangePct >= 0)
                            "+${"%.1f".format(revenueChangePct)}%"
                        else
                            "${"%.1f".format(revenueChangePct)}%",
                        trend     = revenueChangePct,
                        icon      = Icons.Filled.TrendingUp,
                        modifier  = Modifier.weight(1f)
                    )
                    StatCard(
                        title     = "Venit brut estimat",
                        value     = "${ronFormatter.format(estimatedGross)} RON",
                        subtitle  = "buget ramas nealocat",
                        hideable  = true,
                        modifier  = Modifier.weight(1f)
                    )
                }
            } else {
                // Telefon: carduri stivuite
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        title    = "Progres mediu proiecte",
                        value    = "${(avgProgress * 100).roundToInt()}%",
                        subtitle = "din ${projects.size} proiecte active",
                        icon     = Icons.Filled.BarChart,
                        modifier = Modifier.fillMaxWidth()
                    )
                    StatCard(
                        title    = "Venit fata de luna trecuta",
                        value    = if (revenueChangePct >= 0)
                            "+${"%.1f".format(revenueChangePct)}%"
                        else
                            "${"%.1f".format(revenueChangePct)}%",
                        trend    = revenueChangePct,
                        icon     = Icons.Filled.TrendingUp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    StatCard(
                        title    = "Venit brut estimat",
                        value    = "${ronFormatter.format(estimatedGross)} RON",
                        subtitle = "buget ramas nealocat",
                        hideable = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            } // sfarsit Column scrollabil
        } // sfarsit Column exterior (fillMaxSize)
    } // sfarsit BoxWithConstraints
}