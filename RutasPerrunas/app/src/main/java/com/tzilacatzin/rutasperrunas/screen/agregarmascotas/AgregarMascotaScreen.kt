import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tzilacatzin.rutasperrunas.ui.theme.AzulClaro
import com.tzilacatzin.rutasperrunas.ui.theme.Blanco
import com.tzilacatzin.rutasperrunas.ui.theme.Negro

@Composable
fun AgregarMascotaScreen(
    viewModel: AgregarMascotaViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Estados del formulario observados desde el ViewModel
    val nombre by viewModel.nombre.collectAsStateWithLifecycle()
    val raza by viewModel.raza.collectAsStateWithLifecycle()
    val edad by viewModel.edad.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Blanco
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), // Para que el formulario sea scrollable
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Agregar Nueva Mascota",
                style = MaterialTheme.typography.headlineMedium,
                color = Negro
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Campos del formulario
            OutlinedTextField(
                value = nombre,
                onValueChange = viewModel::onNombreChange,
                label = { Text("Nombre de la mascota") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AzulClaro,
                    unfocusedBorderColor = Color.Gray
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = raza,
                onValueChange = viewModel::onRazaChange,
                label = { Text("Raza") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AzulClaro,
                    unfocusedBorderColor = Color.Gray
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = edad,
                onValueChange = viewModel::onEdadChange,
                label = { Text("Edad (en años)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AzulClaro,
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.weight(1f)) // Empuja los botones hacia abajo

            // Botón para agregar
            if (isLoading) {
                CircularProgressIndicator(color = AzulClaro)
            } else {
                Button(
                    onClick = {
                        viewModel.agregarMascota(
                            onSuccess = {
                                Toast.makeText(
                                    context,
                                    "Mascota agregada con éxito",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onNavigateBack()
                            },
                            onError = { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AzulClaro),
                    enabled = viewModel.esFormularioValido() && !isLoading
                ) {
                    Text(text = "Agregar Perro", color = Negro)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botón para regresar
            TextButton(onClick = onNavigateBack) {
                Text("Regresar a mis mascotas", color = AzulClaro)
            }
        }
    }
}