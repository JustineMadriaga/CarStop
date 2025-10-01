package ie.tus.mad.carstop1.parking

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.database.*

data class ParkingSpace(
    val id: String,
    val status: String = "Available"
)

@Composable
fun FindParkingScreen(viewModel: FindParkingViewModel = viewModel()) {
    val context = LocalContext.current

    var showCostDialog by remember { mutableStateOf(false) }
    var reservationHoursInput by remember { mutableStateOf("") }
    var calculatedCost by remember { mutableStateOf(0.0) }
    var selectedSpaceId by remember { mutableStateOf<String?>(null) }
    var parkingSpaces by remember { mutableStateOf<List<ParkingSpace>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val databaseUrl = "https://carstop-b1c30-default-rtdb.europe-west1.firebasedatabase.app/"

    LaunchedEffect(Unit) {
        val ref = FirebaseDatabase.getInstance(databaseUrl).getReference("parking_spaces")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ParkingSpace>()
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    val status = child.child("status").getValue(String::class.java) ?: "Available"
                    list.add(ParkingSpace(id, status))
                }
                parkingSpaces = list
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                isLoading = false
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Find Parking",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(parkingSpaces, key = { it.id }) { space ->
                    val targetColor = when (space.status) {
                        "Available" -> Color(0xFF81C784)
                        "Reserved" -> Color(0xFFFFF176)
                        "Occupied" -> Color(0xFFE57373)
                        else -> Color.LightGray
                    }
                    val animatedColor by animateColorAsState(targetValue = targetColor)

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (space.status == "Available") {
                                    selectedSpaceId = space.id
                                    showCostDialog = true
                                } else {
                                    Toast.makeText(context, "This space is not available.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .background(animatedColor)
                            .padding(4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Space ${space.id.removePrefix("space_")}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Status: ${space.status}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCostDialog && selectedSpaceId != null) {
        AlertDialog(
            onDismissRequest = {
                showCostDialog = false
                reservationHoursInput = ""
            },
            title = { Text("Reserve Space", fontSize = 22.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = reservationHoursInput,
                        onValueChange = { reservationHoursInput = it },
                        label = { Text("Enter hours (1-5)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hours = reservationHoursInput.toIntOrNull() ?: 0
                        if (hours in 1..5) {
                            calculatedCost = hours * 1.5
                            val spaceNumber = selectedSpaceId!!.removePrefix("space_").toInt()

                            viewModel.confirmReservationWithPayment(
                                spaceNumber,
                                hours,
                                calculatedCost,
                                onSuccess = {
                                    Toast.makeText(context, "Reservation Successful!", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = {
                                    Toast.makeText(context, "Insufficient Funds!", Toast.LENGTH_SHORT).show()
                                }
                            )
                            showCostDialog = false
                            reservationHoursInput = ""
                        } else {
                            Toast.makeText(context, "Enter a number between 1 and 5.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showCostDialog = false
                        reservationHoursInput = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
