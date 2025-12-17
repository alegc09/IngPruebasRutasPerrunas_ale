import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tzilacatzin.rutasperrunas.screen.homedueño.HomeDueñoScreen
import com.tzilacatzin.rutasperrunas.screen.homepaseador.HomePaseadorScreen

// Importaciones de las pantallas que necesitas
@Composable
fun NavigationWrapper(
    navHostController: NavHostController,
    auth: FirebaseAuth,
    db: FirebaseFirestore
) {
    NavHost(navController = navHostController, startDestination = "login") {
        composable("login") {
            // Pasando las dependencias y callbacks de navegación necesarios
            LoginScreen(
                auth = auth,
                db = db,
                navController = navHostController,
                // Usar las mismas claves de ruta definidas en las composable()
                NavigateToSingup = { navHostController.navigate("singup") },
                NavigateToHomeDueño = { navHostController.navigate("homeDueño") },
                NavigateToHomePaseador = { navHostController.navigate("homePaseador") }
            )
        }
        composable("singup") {
            // Pasando las dependencias y el callback de navegación
            SingupScreen(
                auth = auth,
                db = db,
                NavigateToLogin = {
                    navHostController.navigate("login") {
                        // Limpia el backstack hasta 'login' para que el usuario no pueda volver a 'singup'
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("homeDueño") {
            // HomeDueñoScreen no necesita props porque usa un ViewModel interno para obtener los datos
            HomeDueñoScreen(
                auth = auth,
                NavigationToLogin = { navHostController.navigate("login") },
                NavigationToAgregarMascota = { navHostController.navigate("agregarMascota") }
            )
        }
        composable("agregarMascota") {
            AgregarMascotaScreen(
                // La acción del botón "Regresar" te lleva a la pantalla anterior
                onNavigateBack = {
                    navHostController.popBackStack()
                }
            )
        }
        composable("homePaseador") {
            HomePaseadorScreen()
        }
    }
}