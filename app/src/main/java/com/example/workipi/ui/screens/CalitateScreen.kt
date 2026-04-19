package com.example.workipi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.workipi.ui.components.LocalOpenDrawer

@Composable
fun CalitateScreen(navController: NavController) {
    val openDrawer = LocalOpenDrawer.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Buton hamburger pe telefon
        if (openDrawer != null) {
            IconButton(
                onClick = { openDrawer() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Deschide meniu",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Text(
            text = "Calitate — in constructie",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
