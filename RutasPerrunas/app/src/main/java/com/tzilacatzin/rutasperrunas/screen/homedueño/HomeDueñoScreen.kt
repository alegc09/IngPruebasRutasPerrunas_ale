package com.tzilacatzin.rutasperrunas.screen.homedueño

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.tzilacatzin.rutasperrunas.model.Paseo
import com.tzilacatzin.rutasperrunas.ui.theme.AzulClaro
import com.tzilacatzin.rutasperrunas.ui.theme.Blanco
import com.tzilacatzin.rutasperrunas.ui.theme.Negro
import com.tzilacatzin.rutasperrunas.ui.theme.VerdeOscuro

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDueñoScreen(
    auth: FirebaseAuth,
    NavigationToLogin: () -> Unit,
    NavigationToAgregarMascota: () -> Unit,
    mascotaViewModel: MascotaViewModel = viewModel()
) {
    val context = LocalContext.current
    var menuExpandido by remember { mutableStateOf(false) }

    // Estados observados desde el ViewModel
    val mascotas by mascotaViewModel.mascotas.collectAsStateWithLifecycle()
    val isLoading by mascotaViewModel.isLoading.collectAsStateWithLifecycle()
    val metodoDePago by mascotaViewModel.metodoDePago.collectAsStateWithLifecycle()
    val mostrarDialogo by mascotaViewModel.mostrarDialogoPago.collectAsStateWithLifecycle()

    // OBSERVAMOS SI HAY PASEO ACTIVO
    val paseoActivo by mascotaViewModel.paseoActivo.collectAsStateWithLifecycle()

    var mascotasSeleccionadas by remember { mutableStateOf(setOf<String>()) }

    // 1. Preparamos el cliente de ubicación
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // 2. Preparamos el lanzador de permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
        if (granted) {
            Toast.makeText(context, "Permiso concedido, vuelve a dar clic", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Se requiere permiso de ubicación para pasear", Toast.LENGTH_LONG).show()
        }
    }

    // --- MANEJO DEL DIÁLOGO ---
    if (mostrarDialogo) {
        DialogoMetodoPago(
            viewModel = mascotaViewModel,
            onDismiss = { mascotaViewModel.onCerrarDialogoPago() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Mascotas", color = Blanco) },
                navigationIcon = {
                    IconButton(onClick = { menuExpandido = true }) {
                        Icon(
                            Icons.Filled.Menu,
                            contentDescription = "Menú",
                            tint = Blanco
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VerdeOscuro
                )
            )
        },
        containerColor = Blanco
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(modifier = Modifier.fillMaxWidth()) {
                DropdownMenu(
                    expanded = menuExpandido,
                    onDismissRequest = { menuExpandido = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Blanco)
                ) {
                    DropdownMenuItem(
                        text = { Text("Métodos de Pago", color = Negro) },
                        onClick = {
                            menuExpandido = false
                            mascotaViewModel.onAbrirDialogoPago()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Cerrar Sesión", color = Negro) },
                        onClick = {
                            menuExpandido = false
                            auth.signOut()
                            NavigationToLogin()
                        }
                    )
                }
            }

            // --- LÓGICA DE INTERFAZ: ¿HAY PASEO ACTIVO? ---
            if (paseoActivo != null) {
                // CASO A: SI HAY PASEO, MOSTRAMOS LA INFORMACIÓN Y EL CÓDIGO
                MuestraInfoPaseoActivo(paseo = paseoActivo!!)

            } else {
                // CASO B: NO HAY PASEO, MOSTRAMOS LA LISTA Y BOTONES
                if (isLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AzulClaro)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(mascotas, key = { it.id }) { mascota ->
                            MascotaItem(
                                mascota = mascota,
                                estaSeleccionado = mascota.id in mascotasSeleccionadas,
                                onCheckedChange = { seleccionado ->
                                    mascotasSeleccionadas = if (seleccionado) {
                                        mascotasSeleccionadas + mascota.id
                                    } else {
                                        mascotasSeleccionadas - mascota.id
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { NavigationToAgregarMascota() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AzulClaro)
                ) {
                    Text(text = "Agregar Perro", fontSize = 16.sp, color = Negro)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (metodoDePago.isNullOrBlank()) {
                            mascotaViewModel.onAbrirDialogoPago()
                            Toast.makeText(context, "Agrega método de pago", Toast.LENGTH_LONG).show()
                        } else {
                            // VERIFICAR PERMISOS
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                    if (location != null) {
                                        val perrosAEnvia = mascotas.filter { it.id in mascotasSeleccionadas }
                                        mascotaViewModel.solicitarPaseo(
                                            listaMascotasSeleccionadas = perrosAEnvia,
                                            latitud = location.latitude,
                                            longitud = location.longitude,
                                            onSuccess = {
                                                Toast.makeText(context, "¡Paseo Solicitado!", Toast.LENGTH_LONG).show()
                                                mascotasSeleccionadas = emptySet()
                                            },
                                            onError = { error -> Toast.makeText(context, error, Toast.LENGTH_LONG).show() }
                                        )
                                    } else {
                                        Toast.makeText(context, "Enciende tu GPS e intenta de nuevo", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                permissionLauncher.launch(arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AzulClaro),
                    enabled = mascotasSeleccionadas.isNotEmpty()
                ) {
                    Text(text = "Pasear Perro", fontSize = 16.sp, color = Negro)
                }
            }
        }
    }
}

// --- NUEVO COMPOSABLE: TARJETA DE PASEO ACTIVO ---
@Composable
fun MuestraInfoPaseoActivo(paseo: Paseo) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Blanco)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🐶 Paseo en Curso", style = MaterialTheme.typography.headlineSmall, color = VerdeOscuro)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Estado actual:", color = Color.Gray)
            Text(paseo.estado, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(24.dp))

            // LÓGICA DEL CÓDIGO
            if (paseo.estado == "EN_PASEO" || paseo.estado == "ACEPTADO") {
                Text("Entrégale este código al paseador al finalizar:", textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = paseo.codigoFin,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = AzulClaro,
                    letterSpacing = 4.sp
                )
            } else if (paseo.estado == "SOLICITADO") {
                CircularProgressIndicator(color = VerdeOscuro)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Buscando paseador cercano...", fontStyle = FontStyle.Italic)
            }
        }
    }
}

// --- DIÁLOGOS Y ELEMENTOS DE UI ---

@Composable
fun DialogoMetodoPago(
    viewModel: MascotaViewModel,
    onDismiss: () -> Unit
) {
    val numeroTarjeta by viewModel.numeroTarjetaInput.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSavingPayment.collectAsStateWithLifecycle()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Blanco,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Método de Pago",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Negro,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = numeroTarjeta,
                    onValueChange = { viewModel.onNumeroTarjetaChange(it) },
                    label = { Text("Número de Tarjeta (16 dígitos)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzulClaro,
                        unfocusedBorderColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))

                if (isSaving) {
                    CircularProgressIndicator(color = AzulClaro)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancelar", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.guardarMetodoDePago(onSuccess = onDismiss)
                            },
                            enabled = numeroTarjeta.length == 16,
                            colors = ButtonDefaults.buttonColors(containerColor = AzulClaro)
                        ) {
                            Text("Guardar", color = Negro)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MascotaItem(
    mascota: Mascota,
    estaSeleccionado: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Blanco)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Pets,
            contentDescription = "Icono de mascota",
            tint = AzulClaro,
            modifier = Modifier.size(40.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mascota.nombre,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Negro
            )
            Text(
                text = mascota.raza,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        Checkbox(
            checked = estaSeleccionado,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = AzulClaro,
                uncheckedColor = Color.Gray
            )
        )
    }
}