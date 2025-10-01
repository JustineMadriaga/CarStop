package ie.tus.mad.carstop1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import ie.tus.mad.carstop1.admin.AdminHomeScreen
import ie.tus.mad.carstop1.admin.ManageParkingScreen
import ie.tus.mad.carstop1.home.HomeScreen
import ie.tus.mad.carstop1.home.HomeViewModel
import ie.tus.mad.carstop1.login.LoginScreen
import ie.tus.mad.carstop1.login.SignUpScreen
import ie.tus.mad.carstop1.parking.FindParkingScreen
import ie.tus.mad.carstop1.parking.FindParkingViewModel
import ie.tus.mad.carstop1.parking.MyReservationsScreen
import ie.tus.mad.carstop1.payment.PaymentHistoryScreen
import ie.tus.mad.carstop1.settings.SettingsScreen
import ie.tus.mad.carstop1.settings.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val settingsViewModel: SettingsViewModel = viewModel()

    val darkMode by settingsViewModel.darkMode.collectAsState()

    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(auth.currentUser) {
        val user = auth.currentUser
        startDestination = if (user != null) {
            if (user.email == "justinerenz98@gmail.com") {
                "admin_home_screen"
            } else {
                "home_screen"
            }
        } else {
            "login_screen"
        }
    }

    if (startDestination != null) {
        MaterialTheme(
            colors = if (darkMode) darkColors() else lightColors()
        ) {
            NavHost(navController = navController, startDestination = startDestination!!) {
                composable("login_screen") {
                    LoginScreen(navController = navController)
                }
                composable("sign_up_screen") {
                    SignUpScreen(navController = navController)
                }
                composable("home_screen") {
                    val viewModel = HomeViewModel()
                    HomeScreen(
                        viewModel = viewModel,
                        onFindParkingClick = {
                            navController.navigate("find_parking_screen")
                        },
                        navController = navController
                    )
                }
                composable("find_parking_screen") {
                    val viewModel = FindParkingViewModel()
                    FindParkingScreen(viewModel = viewModel)
                }
                composable("my_reservations_screen") {
                    MyReservationsScreen()
                }
                composable("payment_history_screen") {
                    PaymentHistoryScreen()
                }
                composable("manage_parking_screen") {
                    ManageParkingScreen()
                }
                composable("admin_home_screen") {
                    AdminHomeScreen(navController)
                }
                composable("settings_screen") {
                    SettingsScreen(navController = navController)
                }
            }
        }
    }
}
