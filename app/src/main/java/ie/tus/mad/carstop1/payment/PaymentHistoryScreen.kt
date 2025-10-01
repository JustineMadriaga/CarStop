package ie.tus.mad.carstop1.payment

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

data class Transaction(
    val spaceId: String = "-",
    val amount: Double = 0.0,
    val timestamp: Long = 0L
)

@Composable
fun PaymentHistoryScreen() {
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid
    val databaseUrl = "https://carstop-b1c30-default-rtdb.europe-west1.firebasedatabase.app/"
    val userRef = FirebaseDatabase.getInstance(databaseUrl).getReference("users/$uid")
    val transactionsRef = FirebaseDatabase.getInstance(databaseUrl).getReference("transactions/$uid")

    val context = LocalContext.current

    var balance by remember { mutableStateOf(0.0) }
    var transactions by remember { mutableStateOf(listOf<Transaction>()) }
    var showTopUpDialog by remember { mutableStateOf(false) }
    var customAmount by remember { mutableStateOf("") }
    var topUpAmount by remember { mutableStateOf(0.0) }

    LaunchedEffect(Unit) {
        userRef.child("balance").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                balance = snapshot.getValue(Double::class.java) ?: 0.0
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        transactionsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Transaction>()
                for (child in snapshot.children) {
                    val spaceId = child.child("spaceId").getValue(String::class.java) ?: "-"
                    val amount = child.child("amount").getValue(Double::class.java) ?: 0.0
                    val timestampAny = child.child("timestamp").value
                    val timestamp = when (timestampAny) {
                        is Long -> timestampAny
                        is String -> timestampAny.toLongOrNull() ?: 0L
                        else -> 0L
                    }
                    list.add(Transaction(spaceId, amount, timestamp))
                }
                transactions = list.sortedByDescending { it.timestamp }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Balance Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Current Balance", fontSize = 18.sp)
                Text("€${"%.2f".format(balance)}", fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Top-Up Buttons
        Text("Top Up Your Account", fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    topUpAmount = 5.0
                    showTopUpDialog = true
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("€5.00")
            }
            Button(
                onClick = {
                    topUpAmount = 10.0
                    showTopUpDialog = true
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("€10.00")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                topUpAmount = 0.0
                showTopUpDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Custom Amount")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Transaction List
        Text("Transaction History", fontSize = 22.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))

        if (transactions.isEmpty()) {
            Text("No transactions yet.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(transactions) { tx ->
                    TransactionCard(tx)
                }
            }
        }
    }

    if (showTopUpDialog) {
        AlertDialog(
            onDismissRequest = {
                customAmount = ""
                topUpAmount = 0.0
                showTopUpDialog = false
            },
            title = { Text("Confirm Top-Up") },
            text = {
                if (topUpAmount == 0.0) {
                    OutlinedTextField(
                        value = customAmount,
                        onValueChange = { customAmount = it },
                        label = { Text("Enter amount (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                } else {
                    Text("Add €${"%.2f".format(topUpAmount)} to your account?")
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amountToAdd = if (topUpAmount == 0.0) customAmount.toDoubleOrNull() ?: 0.0 else topUpAmount
                    if (amountToAdd > 0) {
                        userRef.child("balance").setValue(balance + amountToAdd)
                        transactionsRef.push().setValue(
                            mapOf(
                                "spaceId" to "-",
                                "amount" to amountToAdd,
                                "timestamp" to System.currentTimeMillis()
                            )
                        )
                        Toast.makeText(context, "Top-up successful!", Toast.LENGTH_SHORT).show()
                    }
                    customAmount = ""
                    topUpAmount = 0.0
                    showTopUpDialog = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    customAmount = ""
                    topUpAmount = 0.0
                    showTopUpDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TransactionCard(transaction: Transaction) {
    val dateFormatted = remember(transaction.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(transaction.timestamp))
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFECEFF1))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Space: ${transaction.spaceId}", fontSize = 16.sp)
            Text("Amount: €${"%.2f".format(transaction.amount)}", fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
            Text("Date: $dateFormatted", fontSize = 14.sp, color = Color.Gray)
        }
    }
}
