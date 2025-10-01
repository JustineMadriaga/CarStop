package ie.tus.mad.carstop1.parking

import android.widget.Toast
import androidx.compose.foundation.background
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Reservation(
    val spaceId: Int,
    val reservedForHours: Int?,
    val reservedAt: String?,
    val status: String // Active, Cancelled, Completed
)

@Composable
fun MyReservationsScreen(viewModel: FindParkingViewModel = viewModel()) {
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid
    val databaseUrl = "https://carstop-b1c30-default-rtdb.europe-west1.firebasedatabase.app/"
    val database: DatabaseReference = FirebaseDatabase.getInstance(databaseUrl).getReference("parking_spaces")
    val historyRef: DatabaseReference = FirebaseDatabase.getInstance(databaseUrl).getReference("reservations_history/$uid")
    val context = LocalContext.current

    var reservations by remember { mutableStateOf(listOf<Reservation>()) }
    var loading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf<Pair<Boolean, Reservation?>>(false to null) }

    LaunchedEffect(refreshTrigger) {
        loading = true
        val myReservations = mutableListOf<Reservation>()

        database.get().addOnSuccessListener { snapshot ->
            for (spaceSnapshot in snapshot.children) {
                val reservedBy = spaceSnapshot.child("reservedBy").getValue(String::class.java)
                if (reservedBy == uid) {
                    val spaceId = spaceSnapshot.key?.removePrefix("space_")?.toIntOrNull()
                    val reservedForHours = spaceSnapshot.child("reservedForHours").getValue(Int::class.java)
                    val reservedAt = spaceSnapshot.child("reservedAt").getValue(String::class.java)
                    if (spaceId != null) {
                        myReservations.add(
                            Reservation(spaceId, reservedForHours, reservedAt, "Active")
                        )
                    }
                }
            }
            historyRef.get().addOnSuccessListener { historySnapshot ->
                for (historyItem in historySnapshot.children) {
                    val spaceId = historyItem.child("spaceId").getValue(Int::class.java)
                    val reservedForHours = historyItem.child("reservedForHours").getValue(Int::class.java)
                    val reservedAt = historyItem.child("reservedAt").getValue(String::class.java)
                    val status = historyItem.child("status").getValue(String::class.java) ?: "Completed"
                    if (spaceId != null) {
                        myReservations.add(
                            Reservation(spaceId, reservedForHours, reservedAt, status)
                        )
                    }
                }
                reservations = myReservations.sortedByDescending { it.spaceId }
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "My Reservations",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (reservations.isEmpty()) {
            Text("No reservations found.", fontSize = 18.sp)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(reservations) { reservation ->
                    ReservationCard(
                        reservation = reservation,
                        onCancel = {
                            showCancelDialog = true to reservation
                        },
                        onExtend = {
                            refreshTrigger = !refreshTrigger
                        }
                    )
                }
            }
        }
    }

    if (showCancelDialog.first) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false to null },
            title = { Text("Cancel Reservation?") },
            text = { Text("Are you sure you want to cancel this reservation?") },
            confirmButton = {
                Button(onClick = {
                    showCancelDialog.second?.let { reservation ->
                        database.child("space_${reservation.spaceId}").updateChildren(
                            mapOf(
                                "status" to "Available",
                                "reservedBy" to null,
                                "reservedAt" to null,
                                "reservedForHours" to null
                            )
                        )
                        historyRef.push().setValue(
                            mapOf(
                                "spaceId" to reservation.spaceId,
                                "reservedAt" to reservation.reservedAt,
                                "reservedForHours" to reservation.reservedForHours,
                                "status" to "Cancelled"
                            )
                        )
                        Toast.makeText(context, "Reservation cancelled.", Toast.LENGTH_SHORT).show()
                    }
                    showCancelDialog = false to null
                    refreshTrigger = !refreshTrigger
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelDialog = false to null }) {
                    Text("No")
                }
            }
        )
    }
}

@Composable
fun ReservationCard(
    reservation: Reservation,
    onCancel: () -> Unit,
    onExtend: () -> Unit
) {
    val formatter = DateTimeFormatter.ISO_DATE_TIME
    val context = LocalContext.current
    var showExtendDialog by remember { mutableStateOf(false) }
    var extendHoursInput by remember { mutableStateOf("") }
    var timeLeft by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance("https://carstop-b1c30-default-rtdb.europe-west1.firebasedatabase.app/")
    val userRef = database.getReference("users/${auth.currentUser?.uid}")
    val parkingRef = database.getReference("parking_spaces/space_${reservation.spaceId}")

    LaunchedEffect(Unit) {
        if (reservation.status == "Active" && reservation.reservedAt != null && reservation.reservedForHours != null) {
            while (true) {
                try {
                    val startTime = LocalDateTime.parse(reservation.reservedAt, formatter)
                    val expiryTime = startTime.plusHours(reservation.reservedForHours.toLong())
                    val now = LocalDateTime.now()
                    val duration = Duration.between(now, expiryTime)
                    timeLeft = if (!duration.isNegative) {
                        "${duration.toHours()}h ${duration.toMinutes() % 60}m left"
                    } else "Expired"
                } catch (_: Exception) {
                    timeLeft = ""
                }
                delay(60000)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7FA)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Space #${reservation.spaceId}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Reserved For: ${reservation.reservedForHours ?: "?"} hours")
            Text("Reserved At: ${reservation.reservedAt ?: "?"}")
            Text("Status: ${reservation.status}", fontWeight = FontWeight.SemiBold)
            if (reservation.status == "Active") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(timeLeft, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                        Text("Cancel")
                    }
                    OutlinedButton(onClick = { showExtendDialog = true }) {
                        Text("Extend")
                    }
                }
            }
        }
    }

    if (showExtendDialog) {
        AlertDialog(
            onDismissRequest = { showExtendDialog = false },
            title = { Text("Extend Reservation") },
            text = {
                Column {
                    Text("Enter extra hours (max 5 total):")
                    OutlinedTextField(
                        value = extendHoursInput,
                        onValueChange = { extendHoursInput = it },
                        label = { Text("Hours") },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val extraHours = extendHoursInput.toIntOrNull() ?: 0
                    val currentHours = reservation.reservedForHours ?: 0
                    val newTotal = currentHours + extraHours
                    val cost = extraHours * 1.5

                    if (extraHours <= 0 || newTotal > 5) {
                        Toast.makeText(context, "Invalid extension.", Toast.LENGTH_SHORT).show()
                    } else {
                        userRef.child("balance").get().addOnSuccessListener { snapshot ->
                            val balance = snapshot.getValue(Double::class.java) ?: 0.0
                            if (balance >= cost) {
                                userRef.child("balance").setValue(balance - cost)
                                parkingRef.child("reservedForHours").setValue(newTotal)
                                database.getReference("transactions/${auth.currentUser?.uid}").push().setValue(
                                    mapOf(
                                        "spaceId" to "space_${reservation.spaceId}",
                                        "amount" to cost,
                                        "timestamp" to System.currentTimeMillis(),
                                        "note" to "Extended reservation by $extraHours hour(s)"
                                    )
                                )
                                Toast.makeText(context, "Reservation extended!", Toast.LENGTH_SHORT).show()
                                onExtend()
                            } else {
                                Toast.makeText(context, "Insufficient funds.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    showExtendDialog = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExtendDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
