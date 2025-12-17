package com.tzilacatzin.rutasperrunas.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.tzilacatzin.rutasperrunas.viewmodel.PaseadorViewModel

@Composable
fun MapaPaseoScreen(
    paseoId: String,
    viewModel: PaseadorViewModel = viewModel(),
    onPaseoFinalizado: () -> Unit
) {
    // 1. Cargar el paseo específico al abrir la pantalla
    LaunchedEffect(paseoId) {
        viewModel.cargarPaseoEnTiempoReal(paseoId)
    }

    // Observamos el estado del paseo
    val paseoActual by viewModel.paseoActual.collectAsStateWithLifecycle()

    // Estado de la cámara del mapa
    val cameraPositionState = rememberCameraPositionState()

    // Variable para guardar la zona (el círculo azul)
    var zonaPaseo by remember { mutableStateOf<LatLng?>(null) }

    // --- LÓGICA AUTOMÁTICA (Auto-Zona y Auto-Enfoque) ---
    LaunchedEffect(paseoActual) {
        paseoActual?.let { paseo ->
            val ubicacionPerro = LatLng(paseo.latitud, paseo.longitud)

            // A. Mover la cámara a donde está el perro
            cameraPositionState.position = CameraPosition.fromLatLngZoom(ubicacionPerro, 17f)

            // B. Si ya acepté el paseo (o ya estoy paseando), marco la zona automáticamente
            if (paseo.estado == "ACEPTADO" || paseo.estado == "EN_PASEO") {
                zonaPaseo = ubicacionPerro
            }
        }
    }

    // --- INTERFAZ DE USUARIO ---
    if (paseoActual == null) {
        // Pantalla de carga si aún no baja la info de Firebase
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        // Forzamos que 'paseo' no sea nulo para usarlo abajo
        val paseo = paseoActual!!

        Box(modifier = Modifier.fillMaxSize()) {

            // 1. EL MAPA
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = true)
            ) {
                // Marcador Rojo: Ubicación original del perro/dueño
                Marker(
                    state = MarkerState(position = LatLng(paseo.latitud, paseo.longitud)),
                    title = "Ubicación del Perro"
                )

                // Círculo Azul: Zona de paseo (Solo se ve si zonaPaseo tiene datos)
                zonaPaseo?.let { centro ->
                    Circle(
                        center = centro,
                        radius = 200.0, // 200 metros a la redonda
                        strokeColor = Color.Blue,
                        fillColor = Color(0x220000FF)
                    )
                }
            }

            // 2. LA TARJETA DE CONTROL (Abajo)
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // Textos informativos
                    Text("Mascotas: ${paseo.nombresMascotas.joinToString()}", style = MaterialTheme.typography.titleMedium)
                    Text("Estado actual: ${paseo.estado}", color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- BOTONES QUE CAMBIAN SEGÚN EL ESTADO ---
                    when (paseo.estado) {
                        "SOLICITADO" -> {
                            // Estado 1: Nadie lo ha tomado. Botón para ACEPTAR.
                            Button(
                                onClick = { viewModel.aceptarPaseo(paseo.id) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)) // Verde
                            ) {
                                Text("Aceptar Paseo")
                            }
                        }

                        "ACEPTADO" -> {
                            // Estado 2: Ya acepté. Botón para INICIAR.
                            Text("Dirígete a la zona marcada para recoger al perro.")
                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { viewModel.iniciarPaseo(paseo.id) },
                                // Se activa automáticamente gracias al LaunchedEffect de arriba
                                enabled = zonaPaseo != null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Ya tengo al perro (Iniciar)")
                            }
                        }

                        "EN_PASEO" -> {
                            // Estado 3: Estamos caminando. Pedir código para FINALIZAR.
                            var codigo by remember { mutableStateOf("") }

                            OutlinedTextField(
                                value = codigo,
                                onValueChange = { codigo = it },
                                label = { Text("Código de fin (Pedir al dueño)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    viewModel.finalizarPaseo(paseo.id, codigo, paseo.codigoFin,
                                        onSuccess = { onPaseoFinalizado() },
                                        onError = { /* Puedes mostrar un Toast aquí si quieres */ }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)) // Rojo
                            ) {
                                Text("Finalizar Paseo")
                            }
                        }

                        "FINALIZADO" -> {
                            Text("¡Paseo completado con éxito! ✅", color = Color(0xFF4CAF50))
                            Button(onClick = { onPaseoFinalizado() }, modifier = Modifier.fillMaxWidth()) {
                                Text("Salir")
                            }
                        }
                    }
                }
            }
        }
    }
}