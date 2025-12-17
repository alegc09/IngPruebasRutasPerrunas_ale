package com.tzilacatzin.rutasperrunas.screen.homepaseador

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tzilacatzin.rutasperrunas.viewmodel.PaseadorViewModel
import com.tzilacatzin.rutasperrunas.ui.theme.VerdeOscuro

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePaseadorScreen(
    viewModel: PaseadorViewModel = viewModel(),
    onNavegarAlMapa: (String) -> Unit // Recibe el ID del paseo
) {
    val paseos by viewModel.paseosDisponibles.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paseos Disponibles") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VerdeOscuro)
            )
        }
    ) { padding ->
        if (paseos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No hay solicitudes de paseo por ahora 😴")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(paseos) { paseo ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable { onNavegarAlMapa(paseo.id) }, // Al clic, vamos al mapa
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Mascotas: ${paseo.nombresMascotas.joinToString(", ")}", style = MaterialTheme.typography.titleMedium)
                            Text("Ganancia: $${paseo.costoTotal}", color = VerdeOscuro)
                            Text("Estado: ${paseo.estado}")
                        }
                    }
                }
            }
        }
    }
}