package ie.tus.mad.carstop1.home

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun HomeScreen(viewModel: HomeViewModel, onFindParkingClick: () -> Unit, navController: NavController) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()

    var balance by remember { mutableStateOf(0.0) }
    val uid = auth.currentUser?.uid
    val databaseUrl = "https://carstop-b1c30-default-rtdb.europe-west1.firebasedatabase.app/"

    // Fetch user balance
    LaunchedEffect(uid) {
        if (uid != null) {
            val userRef = FirebaseDatabase.getInstance(databaseUrl).getReference("users/$uid")
            userRef.child("balance").get().addOnSuccessListener { snapshot ->
                balance = snapshot.getValue(Double::class.java) ?: 0.0
            }
        }
    }

    ModalDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Menu",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = {
                        auth.signOut()
                        navController.navigate("login_screen") {
                            popUpTo("home_screen") { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                ) {
                    Text("Sign Out", color = Color.White)
                }
                Button(
                    onClick = {
                        navController.navigate("settings_screen")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Settings")
                }
                Spacer(modifier = Modifier.height(8.dp))

            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("CarStop") },
                    actions = {
                        IconButton(onClick = {
                            scope.launch {
                                if (drawerState.isOpen) drawerState.close() else drawerState.open()
                            }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu Icon")
                        }
                    }
                )
            },
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Balance Display Card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                        elevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Account Balance",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "€${"%.2f".format(balance)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons
                    Button(
                        onClick = onFindParkingClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Find Parking", fontSize = 18.sp)
                    }
                    Button(
                        onClick = { navController.navigate("payment_history_screen") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Payment History", fontSize = 18.sp)
                    }
                    Button(
                        onClick = { navController.navigate("my_reservations_screen") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("My Reservations", fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Google Map
                    Text(
                        text = "Car Park Location",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val tusMoylishLatLng = LatLng(52.6784, -8.6490) // TUS Moylish
                    val cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(tusMoylishLatLng, 16f)
                    }

                    GoogleMap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(horizontal = 8.dp),
                        cameraPositionState = cameraPositionState
                    ) {
                        Marker(
                            state = MarkerState(position = tusMoylishLatLng),
                            title = "TUS Moylish Campus",
                            snippet = "Car Park Location"
                        )
                    }
                }
            }
        )
    }
}
