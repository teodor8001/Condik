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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.workipi.data.mock.MockData
import com.example.workipi.data.model.MockEmployee
import com.example.workipi.navigation.Screen
import com.example.workipi.ui.components.LocalOpenDrawer
import kotlin.math.ceil

private data class MedalStyle(
    val bg: Color,
    val accent: Color,
    val emoji: String
)

private val medals = listOf(
    MedalStyle(Color(0xFFFFF8DC), Color(0xFFB8860B), "🥇"),
    MedalStyle(Color(0xFFF0F0F0), Color(0xFF707070), "🥈"),
    MedalStyle(Color(0xFFFAEEE4), Color(0xFF8B5E3C), "🥉"),
)

@Composable
fun LeaderboardScreen(navController: NavController) {
    val sorted       = MockData.employees.sortedByDescending { it.points }
    val topCount     = ceil(sorted.size / 2.0).toInt()
    val topEmployees = sorted.take(topCount)
    val openDrawer   = LocalOpenDrawer.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ---- Header ----
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Topuri",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Top $topCount din ${MockData.employees.size} angajati",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---- Top 3 carduri: locul 2 stanga, locul 1 mijloc, locul 3 dreapta ----
        if (topEmployees.size >= 3) {
            val order = listOf(1, 0, 2) // indecsi din topEmployees: 2nd, 1st, 3rd
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                order.forEach { empIndex ->
                    val position = empIndex + 1
                    TopThreeCard(
                        position = position,
                        employee = topEmployees[empIndex],
                        medal    = medals[empIndex],
                        modifier = Modifier.weight(1f),
                        onClick  = {
                            navController.navigate(
                                Screen.EmployeeDetail.createRoute(topEmployees[empIndex].id)
                            )
                        }
                    )
                }
            }
        }

        // ---- Lista locurile 4+ ----
        if (topEmployees.size > 3) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(topEmployees.drop(3)) { index, employee ->
                    LeaderboardCard(
                        position = index + 4,
                        employee = employee,
                        onClick  = {
                            navController.navigate(
                                Screen.EmployeeDetail.createRoute(employee.id)
                            )
                        }
                    )
                }
            }
        }
    }
}

// ---- Card pentru locurile 1, 2, 3 — layout vertical compact pentru telefon ----
@Composable
private fun TopThreeCard(
    position: Int,
    employee: MockEmployee,
    medal: MedalStyle,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = medal.bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Emoji medalie
            Text(text = medal.emoji, fontSize = 26.sp)

            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(medal.accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = employee.name
                        .split(" ")
                        .take(2)
                        .joinToString("") { it.first().uppercase() },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = medal.accent
                )
            }

            // Prenume (maxLines=1 pentru ecrane mici)
            Text(
                text = employee.name.split(" ").first(),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            // Puncte
            Text(
                text = "${employee.points} pts",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = medal.accent
            )
        }
    }
}

// ---- Card pentru locurile 4+ ----
@Composable
private fun LeaderboardCard(position: Int, employee: MockEmployee, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Numar pozitie
            Box(modifier = Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = "$position",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Avatar
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = employee.name
                        .split(" ")
                        .take(2)
                        .joinToString("") { it.first().uppercase() },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Nume + specialitate
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = employee.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = employee.primarySpecialty,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Puncte
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${employee.points}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "puncte",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}