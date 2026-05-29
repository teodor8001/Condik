package com.example.workipi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import com.example.workipi.data.model.Project
import com.example.workipi.navigation.Screen
import com.example.workipi.ui.components.LocalOpenDrawer
import com.example.workipi.viewmodel.OffersViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun OfertareScreen(
    navController: NavController,
    viewModel: OffersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val openDrawer = LocalOpenDrawer.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.loadOffers() }

    val ronFormatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("ro-RO")).apply {
        maximumFractionDigits = 0
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddProject.route) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Adauga oferta")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
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
                Column {
                    Text(
                        text = "Ofertare",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = when {
                            state.isLoading -> "Se incarca..."
                            state.errorMessage != null -> state.errorMessage!!
                            else -> "${state.offers.size} oferte"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (state.offers.isEmpty() && !state.isLoading && state.errorMessage == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Nicio oferta inca. Apasa + pentru a adauga.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.offers.forEach { offer ->
                        OfferCard(
                            offer = offer,
                            ronFormatter = ronFormatter,
                            onClick = {
                                navController.navigate(Screen.ProjectDetail.createRoute(offer.projectId))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OfferCard(
    offer: Project,
    ronFormatter: NumberFormat,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = offer.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = offer.adress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            offer.budget?.let {
                Text(
                    text = "Buget estimat: ${ronFormatter.format(it)} RON",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = "Termen: ${offer.endDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
