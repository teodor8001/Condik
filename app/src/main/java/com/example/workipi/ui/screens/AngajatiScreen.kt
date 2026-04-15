package com.example.workipi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.workipi.data.mock.MockData
import com.example.workipi.data.model.Employee
import com.example.workipi.data.model.EmployeeLevel
import com.example.workipi.navigation.Screen
import com.example.workipi.ui.components.LevelBadge
import com.example.workipi.ui.components.LocalOpenDrawer

private enum class SortBy(val label: String) {
    ALFABETIC("Alfabetic"),
    RANG_ASC("Rang ↑"),
    RANG_DESC("Rang ↓")
}

private val levelOrder = listOf(
    EmployeeLevel.JUNIOR,
    EmployeeLevel.MID,
    EmployeeLevel.SENIOR,
    EmployeeLevel.LEAD
)

@Composable
fun AngajatiScreen(navController: NavController) {
    val employees  = MockData.employees
    val openDrawer = LocalOpenDrawer.current
    var sortBy by remember { mutableStateOf(SortBy.ALFABETIC) }

    val sorted = remember(sortBy) {
        when (sortBy) {
            SortBy.ALFABETIC  -> employees.sortedBy { it.name }
            SortBy.RANG_ASC   -> employees.sortedBy { levelOrder.indexOf(it.level) }
            SortBy.RANG_DESC  -> employees.sortedByDescending { levelOrder.indexOf(it.level) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---- Header ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        text = "Angajati",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Lista completa",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Total angajati dreapta sus
            Box(
                modifier = Modifier
                    .padding(top = 4.dp, end = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${employees.size}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Angajati",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ---- Sort chips ----
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortBy.entries.forEach { option ->
                val selected = sortBy == option
                FilterChip(
                    selected = selected,
                    onClick  = { sortBy = option },
                    label    = { Text(option.label, style = MaterialTheme.typography.bodySmall) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor    = MaterialTheme.colorScheme.primary,
                        selectedLabelColor        = MaterialTheme.colorScheme.onPrimary,
                        containerColor            = MaterialTheme.colorScheme.surface,
                        labelColor                = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        // ---- Lista scrollabila ----
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(sorted) { index, employee ->
                    AngajatRow(
                        number   = index + 1,
                        employee = employee,
                        onClick  = {
                            navController.navigate(
                                Screen.EmployeeDetail.createRoute(employee.id)
                            )
                        }
                    )
                    if (index < sorted.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AngajatRow(number: Int, employee: Employee, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Numar ordine
        Text(
            text = "$number.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp)
        )

        // Avatar initiale
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = employee.name
                    .split(" ")
                    .take(2)
                    .joinToString("") { it.first().uppercase() },
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Nume + specialitate principala
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = employee.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = employee.primarySpecialty,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LevelBadge(level = employee.level)
    }
}