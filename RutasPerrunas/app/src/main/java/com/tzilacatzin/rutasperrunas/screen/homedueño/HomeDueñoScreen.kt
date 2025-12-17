package com.tzilacatzin.rutasperrunas.screen.homedueño

import android.widget.Toast
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
// Importa los colores que sí existen en tu archivo Color.kt
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
    var mascotasSeleccionadas by remember { mutableStateOf(setOf<String>()) }

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
                            // Acción: Abrir diálogo de pago
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
                    // --- LÓGICA CONDICIONAL PARA PASEAR ---
                    if (metodoDePago.isNullOrBlank()) {
                        // Si no hay método de pago, abre el diálogo
                        mascotaViewModel.onAbrirDialogoPago()
                        Toast.makeText(context, "Por favor, agrega un método de pago para continuar", Toast.LENGTH_LONG).show()
                    } else {
                        // Si ya hay método de pago, procede con la lógica del paseo
                        // TODO: Lógica para iniciar paseo con perros seleccionados
                        Toast.makeText(context, "Iniciando paseo...", Toast.LENGTH_SHORT).show()
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

// --- NUEVO COMPOSABLE PARA EL DIÁLOGO ---

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
                            enabled = numeroTarjeta.length == 16, // Solo se activa si el número es válido
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