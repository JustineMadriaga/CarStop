package ie.tus.mad.carstop1.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.FirebaseDatabase
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class AdminParkingSpace(
    val id: String,
    val status: String = "Available",
    val distance: Double = 0.0,
    val reservedAt: String? = null,
    val reservedForHours: Int? = null,
    val reservedBy: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageParkingScreen() {
    val context = LocalContext.current
    val databaseUrl = "https://carstop-b1c30-default-rtdb.europe-west1.firebasedatabase.app/"
    val parkingRef = FirebaseDatabase.getInstance(databaseUrl).getReference("parking_spaces")
    val reservationsRef = FirebaseDatabase.getInstance(databaseUrl).getReference("reservations")

    var parkingSpaces by remember { mutableStateOf<List<AdminParkingSpace>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        parkingRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val list = mutableListOf<AdminParkingSpace>()
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    val status = child.child("status").getValue(String::class.java) ?: "Available"
                    val distance = child.child("distance").getValue(Double::class.java) ?: 0.0
                    val reservedAt = child.child("reservedAt").getValue(String::class.java)
                    val reservedForHours = child.child("reservedForHours").getValue(Int::class.java)
                    val reservedBy = child.child("reservedBy").getValue(String::class.java)

                    list.add(
                        AdminParkingSpace(
                            id = id,
                            status = status,
                            distance = distance,
                            reservedAt = reservedAt,
                            reservedForHours = reservedForHours,
                            reservedBy = reservedBy
                        )
                    )
                }
                parkingSpaces = list
                isLoading = false
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Manage Parking Spaces") })
        }
    ) { padding ->
        if (isLoading) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(parkingSpaces, key = { it.id }) { space ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(
                                        "Space ${space.id.replace("space_", "")}",
                                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Status: ${space.status}",
                                        color = when (space.status) {
                                            "Available" -> Color(0xFF4CAF50)
                                            "Reserved" -> Color(0xFFFFC107)
                                            "Occupied" -> Color(0xFFF44336)
                                            else -> Color.Gray
                                        },
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            color = when (space.status) {
                                                "Available" -> Color.Green
                                                "Reserved" -> Color.Yellow
                                                "Occupied" -> Color.Red
                                                else -> Color.Gray
                                            },
                                            shape = CircleShape
                                        )
                                )
                            }

                            Text("Distance: ${"%.1f".format(space.distance)} cm", fontSize = 14.sp)

                            if (space.status == "Reserved" && space.reservedAt != null && space.reservedForHours != null) {
                                val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                                val reservedAt = LocalDateTime.parse(space.reservedAt, formatter)
                                val now = LocalDateTime.now()
                                val timeElapsed = Duration.between(reservedAt, now).toHours()
                                val hoursLeft = space.reservedForHours - timeElapsed

                                Text(
                                    text = if (hoursLeft > 0) "Time Left: $hoursLeft hour(s)" else "Reservation Expired",
                                    color = Color.DarkGray
                                )
                            }

                            if (space.status == "Reserved" && space.reservedBy != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        parkingRef.child(space.id).updateChildren(
                                            mapOf(
                                                "status" to "Available",
                                                "reservedAt" to null,
                                                "reservedBy" to null,
                                                "reservedForHours" to null
                                            )
                                        )
                                        reservationsRef.child(space.reservedBy).push().setValue(
                                            mapOf(
                                                "spaceId" to space.id,
                                                "status" to "Removed by Admin",
                                                "timestamp" to System.currentTimeMillis()
                                            )
                                        )
                                        Toast.makeText(context, "Reservation removed by Admin.", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Remove Reservation")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
