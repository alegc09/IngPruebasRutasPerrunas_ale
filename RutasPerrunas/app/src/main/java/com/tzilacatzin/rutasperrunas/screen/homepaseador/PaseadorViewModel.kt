package com.tzilacatzin.rutasperrunas.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.tzilacatzin.rutasperrunas.model.Paseo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PaseadorViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    // Lista de paseos disponibles (Estado SOLICITADO)
    private val _paseosDisponibles = MutableStateFlow<List<Paseo>>(emptyList())
    val paseosDisponibles = _paseosDisponibles.asStateFlow()

    // Paseo actual que estoy realizando
    private val _paseoActual = MutableStateFlow<Paseo?>(null)
    val paseoActual = _paseoActual.asStateFlow()

    init {
        escucharPaseosDisponibles()
    }

    // 1. Escuchar solicitudes en tiempo real
    private fun escucharPaseosDisponibles() {
        db.collection("paseos")
            .whereEqualTo("estado", "SOLICITADO")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val lista = snapshot.documents.mapNotNull { doc ->
                        val p = doc.toObject(Paseo::class.java)
                        p?.id = doc.id
                        p
                    }
                    _paseosDisponibles.value = lista
                }
            }
    }

    // 2. Cargar un paseo específico (para la pantalla del mapa)
    fun cargarPaseoEnTiempoReal(paseoId: String) {
        db.collection("paseos").document(paseoId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val p = snapshot.toObject(Paseo::class.java)
                    p?.id = snapshot.id
                    _paseoActual.value = p
                }
            }
    }

    // 3. Aceptar el paseo
    fun aceptarPaseo(paseoId: String) {
        val miId = auth.currentUser?.uid ?: return
        db.collection("paseos").document(paseoId).update(
            mapOf(
                "estado" to "ACEPTADO",
                "idPaseador" to miId
            )
        )
    }

    // 4. Cambiar estados
    fun iniciarPaseo(paseoId: String) {
        db.collection("paseos").document(paseoId).update("estado", "EN_PASEO")
    }

    fun finalizarPaseo(paseoId: String, codigoIngresado: String, codigoReal: String, onSuccess: () -> Unit, onError: () -> Unit) {
        if (codigoIngresado == codigoReal) {
            db.collection("paseos").document(paseoId).update("estado", "FINALIZADO")
            onSuccess()
        } else {
            onError()
        }
    }
}