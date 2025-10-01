package ie.tus.mad.carstop1.admin

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun AdminHomeScreen(navController: NavHostController) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()

    var totalSpaces by remember { mutableStateOf(0) }
    var reservedSpaces by remember { mutableStateOf(0) }
    var occupiedSpaces by remember { mutableStateOf(0) }
    var activeReservations by remember { mutableStateOf(0) }

    val databaseUrl = "https://carstop-b1c30-default-rtdb.europe-west1.firebasedatabase.app/"
    val parkingRef = FirebaseDatabase.getInstance(databaseUrl).getReference("parking_spaces")

    LaunchedEffect(Unit) {
        parkingRef.get().addOnSuccessListener { snapshot ->
            var total = 0
            var reserved = 0
            var occupied = 0
            var active = 0

            for (child in snapshot.children) {
                total++
                val status = child.child("status").getValue(String::class.java) ?: "Available"
                val reservedBy = child.child("reservedBy").getValue(String::class.java)

                if (status == "Reserved") reserved++
                if (status == "Occupied") occupied++
                if (!reservedBy.isNullOrEmpty()) active++
            }

            totalSpaces = total
            reservedSpaces = reserved
            occupiedSpaces = occupied
            activeReservations = active
        }
    }

    ModalDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Admin Menu",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(
                    onClick = {
                        auth.signOut()
                        navController.navigate("login_screen") {
                            popUpTo("admin_home_screen") { inclusive = true }
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
                    title = { Text("Welcome Admin") },
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
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Dashboard Card
                    Card(
                        elevation = 8.dp,
                        backgroundColor = Color(0xFFF3F3F3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Dashboard",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            DashboardItem("Total Spaces", totalSpaces)
                            DashboardItem("Reserved Spaces", reservedSpaces)
                            DashboardItem("Occupied Spaces", occupiedSpaces)
                            DashboardItem("Active Reservations", activeReservations)
                        }
                    }

                    Text(
                        "Parking Status Overview",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Pie Chart
                    PieChart(
                        statusCounts = mapOf(
                            "Available" to (totalSpaces - reservedSpaces - occupiedSpaces),
                            "Reserved" to reservedSpaces,
                            "Occupied" to occupiedSpaces
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LegendItem(Color(0xFF4CAF50), "Available")
                        LegendItem(Color(0xFFFFC107), "Reserved")
                        LegendItem(Color(0xFFF44336), "Occupied")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Navigation Button
                    Button(
                        onClick = { navController.navigate("manage_parking_screen") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text("Manage Parking")
                    }
                }
            }
        )
    }
}

@Composable
fun DashboardItem(label: String, value: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 16.sp)
        Text(value.toString(), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun PieChart(statusCounts: Map<String, Int>, modifier: Modifier = Modifier) {
    val total = statusCounts.values.sum().toFloat()
    val angles = statusCounts.map { (it.value / total) * 360f }

    val colors = listOf(
        Color(0xFF4CAF50), // Available
        Color(0xFFFFC107), // Reserved
        Color(0xFFF44336)  // Occupied
    )

    Canvas(
        modifier = modifier
            .height(200.dp)
            .fillMaxWidth()
    ) {
        var startAngle = 0f
        angles.forEachIndexed { index, angle ->
            drawArc(
                color = colors.getOrElse(index) { Color.Gray },
                startAngle = startAngle,
                sweepAngle = angle,
                useCenter = true,
                size = Size(size.width, size.height)
            )
            startAngle += angle
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color = color, shape = RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontSize = 14.sp)
    }
}
