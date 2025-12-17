package com.tzilacatzin.rutasperrunas.screen.homedueño

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Modelo de datos para la mascota
data class Mascota(
    val id: String = "",
    val nombre: String = "",
    val raza: String = ""
)

// ViewModel para manejar la lógica de la pantalla
class MascotaViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val userId: String? get() = auth.currentUser?.uid

    // --- Estados de la UI ---
    private val _mascotas = MutableStateFlow<List<Mascota>>(emptyList())
    val mascotas: StateFlow<List<Mascota>> = _mascotas

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // --- NUEVOS ESTADOS PARA EL MÉTODO DE PAGO ---
    private val _metodoDePago = MutableStateFlow<String?>(null)
    val metodoDePago: StateFlow<String?> = _metodoDePago

    private val _mostrarDialogoPago = MutableStateFlow(false)
    val mostrarDialogoPago = _mostrarDialogoPago.asStateFlow()

    private val _numeroTarjetaInput = MutableStateFlow("")
    val numeroTarjetaInput = _numeroTarjetaInput.asStateFlow()

    private val _isSavingPayment = MutableStateFlow(false)
    val isSavingPayment = _isSavingPayment.asStateFlow()


    init {
        // Cargar todos los datos necesarios al iniciar
        cargarDatosDelDueño()
    }

    private fun cargarDatosDelDueño() {
        if (userId == null) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Ejecuta ambas cargas en paralelo para más eficiencia
                cargarMascotas()
                cargarMetodoDePago()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun cargarMascotas() {
        try {
            val snapshot = db.collection("users").document(userId!!)
                .collection("mascotas").get().await()
            _mascotas.value = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Mascota::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            println("Error al cargar mascotas: ${e.message}")
            _mascotas.value = emptyList()
        }
    }

    // Carga el método de pago desde el documento del usuario
    private suspend fun cargarMetodoDePago() {
        try {
            val document = db.collection("users").document(userId!!).get().await()
            val tarjeta = document.getString("metodoDePago")
            _metodoDePago.value = tarjeta
            // Pre-llena el campo de texto del diálogo si ya existe una tarjeta
            _numeroTarjetaInput.value = tarjeta ?: ""
        } catch (e: Exception) {
            println("Error al cargar método de pago: ${e.message}")
            _metodoDePago.value = null
        }
    }

    // --- NUEVOS MÉTODOS PARA GESTIONAR EL DIÁLOGO ---

    fun onNumeroTarjetaChange(numero: String) {
        // Permitir solo números y un máximo de 16 dígitos
        if (numero.all { it.isDigit() } && numero.length <= 16) {
            _numeroTarjetaInput.value = numero
        }
    }

    fun guardarMetodoDePago(onSuccess: () -> Unit) {
        if (userId == null || _numeroTarjetaInput.value.length != 16) {
            // Podrías añadir un callback de error aquí si quieres
            return
        }
        viewModelScope.launch {
            _isSavingPayment.value = true
            try {
                // Guarda o actualiza el campo 'metodoDePago' en el documento del usuario
                db.collection("users").document(userId!!)
                    .set(mapOf("metodoDePago" to _numeroTarjetaInput.value), com.google.firebase.firestore.SetOptions.merge())
                    .await()
                _metodoDePago.value = _numeroTarjetaInput.value // Actualiza el estado local
                onSuccess()
            } catch (e: Exception) {
                println("Error al guardar método de pago: ${e.message}")
            } finally {
                _isSavingPayment.value = false
            }
        }
    }

    fun onAbrirDialogoPago() {
        // Asegura que el texto del diálogo esté actualizado con la info más reciente
        _numeroTarjetaInput.value = _metodoDePago.value ?: ""
        _mostrarDialogoPago.value = true
    }

    fun onCerrarDialogoPago() {
        _mostrarDialogoPago.value = false
    }
}