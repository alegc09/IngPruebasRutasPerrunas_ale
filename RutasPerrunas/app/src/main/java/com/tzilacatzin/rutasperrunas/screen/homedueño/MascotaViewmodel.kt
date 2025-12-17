package com.tzilacatzin.rutasperrunas.screen.homedueño

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration // Importante para manejar el listener
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.tzilacatzin.rutasperrunas.model.Paseo
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

class MascotaViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val userId: String? get() = auth.currentUser?.uid

    // --- Estados de la UI ---
    private val _mascotas = MutableStateFlow<List<Mascota>>(emptyList())
    val mascotas: StateFlow<List<Mascota>> = _mascotas

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // --- Estados Pagos ---
    private val _metodoDePago = MutableStateFlow<String?>(null)
    val metodoDePago: StateFlow<String?> = _metodoDePago

    private val _mostrarDialogoPago = MutableStateFlow(false)
    val mostrarDialogoPago = _mostrarDialogoPago.asStateFlow()

    private val _numeroTarjetaInput = MutableStateFlow("")
    val numeroTarjetaInput = _numeroTarjetaInput.asStateFlow()

    private val _isSavingPayment = MutableStateFlow(false)
    val isSavingPayment = _isSavingPayment.asStateFlow()

    // --- Estado Paseo Activo ---
    private val _paseoActivo = MutableStateFlow<Paseo?>(null)
    val paseoActivo = _paseoActivo.asStateFlow()

    // Variable para guardar el registro del listener y limpiarlo si hace falta
    private var mascotasListener: ListenerRegistration? = null

    init {
        // Cargar datos al iniciar
        cargarDatosDelDueño()
        escucharPaseoActivo()
    }

    private fun cargarDatosDelDueño() {
        if (userId == null) return

        // 1. CAMBIO AQUÍ: Llamamos a la escucha en tiempo real en lugar de carga única
        escucharMascotasEnTiempoReal()

        // Cargar lo demás (Pagos)
        viewModelScope.launch {
            _isLoading.value = true
            try {
                cargarMetodoDePago()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- ESTA ES LA FUNCIÓN NUEVA CORREGIDA ---
    private fun escucharMascotasEnTiempoReal() {
        if (userId == null) return

        // Usamos addSnapshotListener en lugar de get().await()
        mascotasListener = db.collection("users").document(userId!!)
            .collection("mascotas")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("Error al escuchar mascotas: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Mapeamos los documentos cada vez que cambien en la base de datos
                    val lista = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Mascota::class.java)?.copy(id = doc.id)
                    }
                    _mascotas.value = lista
                }
            }
    }

    // (Asegúrate de limpiar el listener cuando el ViewModel muera para no gastar recursos)
    override fun onCleared() {
        super.onCleared()
        mascotasListener?.remove()
    }

    // ... (El resto de tus funciones: cargarMetodoDePago, solicitarPaseo, etc. se quedan IGUAL) ...

    private suspend fun cargarMetodoDePago() {
        try {
            val document = db.collection("users").document(userId!!).get().await()
            val tarjeta = document.getString("metodoDePago")
            _metodoDePago.value = tarjeta
            _numeroTarjetaInput.value = tarjeta ?: ""
        } catch (e: Exception) {
            println("Error al cargar método de pago: ${e.message}")
            _metodoDePago.value = null
        }
    }

    private fun escucharPaseoActivo() {
        if (userId == null) return

        db.collection("paseos")
            .whereEqualTo("idDuenio", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null && !snapshot.isEmpty) {
                    val listaPaseos = snapshot.documents.mapNotNull { doc ->
                        val p = doc.toObject(Paseo::class.java)
                        p?.id = doc.id
                        p
                    }
                    val enCurso = listaPaseos.firstOrNull { it.estado != "FINALIZADO" }
                    _paseoActivo.value = enCurso
                } else {
                    _paseoActivo.value = null
                }
            }
    }

    fun solicitarPaseo(
        listaMascotasSeleccionadas: List<Mascota>,
        latitud: Double,
        longitud: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onError("No hay sesión activa")
            return
        }

        val codigoSecreto = (1000..9999).random().toString()
        val nombres = listaMascotasSeleccionadas.map { it.nombre }
        val costo = nombres.size * 50.0

        val nuevoPaseo = Paseo(
            idDuenio = userId,
            nombresMascotas = nombres,
            estado = "SOLICITADO",
            codigoFin = codigoSecreto,
            costoTotal = costo,
            latitud = latitud,
            longitud = longitud
        )

        Firebase.firestore.collection("paseos")
            .add(nuevoPaseo)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.message ?: "Error al solicitar") }
    }

    fun onNumeroTarjetaChange(numero: String) {
        if (numero.all { it.isDigit() } && numero.length <= 16) {
            _numeroTarjetaInput.value = numero
        }
    }

    fun guardarMetodoDePago(onSuccess: () -> Unit) {
        if (userId == null || _numeroTarjetaInput.value.length != 16) {
            return
        }
        viewModelScope.launch {
            _isSavingPayment.value = true
            try {
                db.collection("users").document(userId!!)
                    .set(mapOf("metodoDePago" to _numeroTarjetaInput.value), com.google.firebase.firestore.SetOptions.merge())
                    .await()
                _metodoDePago.value = _numeroTarjetaInput.value
                onSuccess()
            } catch (e: Exception) {
                println("Error al guardar método de pago: ${e.message}")
            } finally {
                _isSavingPayment.value = false
            }
        }
    }

    fun onAbrirDialogoPago() {
        _numeroTarjetaInput.value = _metodoDePago.value ?: ""
        _mostrarDialogoPago.value = true
    }

    fun onCerrarDialogoPago() {
        _mostrarDialogoPago.value = false
    }
}