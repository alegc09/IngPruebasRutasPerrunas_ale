import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.tzilacatzin.rutasperrunas.model.Paseo
import com.tzilacatzin.rutasperrunas.screen.homedueño.Mascota
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AgregarMascotaViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    // --- Estados del formulario ---
    private val _nombre = MutableStateFlow("")
    val nombre = _nombre.asStateFlow()

    private val _raza = MutableStateFlow("")
    val raza = _raza.asStateFlow()

    private val _edad = MutableStateFlow("")
    val edad = _edad.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // --- Eventos de la UI ---
    fun onNombreChange(nombre: String) {
        _nombre.value = nombre
    }

    fun onRazaChange(raza: String) {
        _raza.value = raza
    }
    fun onEdadChange(edad: String) {
        // Permitir solo números en el campo de edad
        if (edad.all { it.isDigit() }) {
            _edad.value = edad
        }
    }

    // --- Validación del formulario (sin imagen) ---
    fun esFormularioValido(): Boolean {
        return _nombre.value.isNotBlank() &&
                _raza.value.isNotBlank() &&
                _edad.value.isNotBlank()
    }

    // --- Lógica de negocio (sin imagen) ---
    fun agregarMascota(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (!esFormularioValido()) {
            onError("Todos los campos son obligatorios.")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val userId = auth.currentUser?.uid

            if (userId == null) {
                onError("No se pudo obtener el usuario.")
                _isLoading.value = false
                return@launch
            }

            try {
                // Guardar los datos de la mascota en Firestore (sin URL de imagen)
                val mascotaData = hashMapOf(
                    "nombre" to _nombre.value,
                    "raza" to _raza.value,
                    "edad" to _edad.value.toIntOrNull()
                )

                firestore.collection("users").document(userId)
                    .collection("mascotas")
                    .add(mascotaData)
                    .await()

                onSuccess() // Llama al callback de éxito

            } catch (e: Exception) {
                onError(e.message ?: "Ocurrió un error desconocido.")
            } finally {
                _isLoading.value = false
            }
        }
    }
}