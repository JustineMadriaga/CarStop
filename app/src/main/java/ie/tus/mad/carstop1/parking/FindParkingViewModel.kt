package ie.tus.mad.carstop1.parking

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalDateTime

class FindParkingViewModel : ViewModel() {

    fun confirmReservationWithPayment(
        spaceId: Int,
        hours: Int,
        totalCost: Double,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseUrl = "https://carstop-b1c30-default-rtdb.europe-west1.firebasedatabase.app/"
        val userRef = FirebaseDatabase.getInstance(databaseUrl).getReference("users/$uid")
        val spaceRef = FirebaseDatabase.getInstance(databaseUrl).getReference("parking_spaces/space_$spaceId")
        val transactionsRef = FirebaseDatabase.getInstance(databaseUrl).getReference("transactions/$uid")

        userRef.child("balance").get().addOnSuccessListener { snapshot ->
            val balance = snapshot.getValue(Double::class.java) ?: 0.0
            if (balance >= totalCost) {
                // Deduct balance
                userRef.child("balance").setValue(balance - totalCost)

                // Reserve the space
                val now = LocalDateTime.now().toString()
                spaceRef.updateChildren(
                    mapOf(
                        "status" to "Reserved",
                        "reservedBy" to uid,
                        "reservedAt" to now,
                        "reservedForHours" to hours
                    )
                )

                // Save transaction
                transactionsRef.push().setValue(
                    mapOf(
                        "spaceId" to "space_$spaceId",
                        "amount" to totalCost,
                        "timestamp" to now
                    )
                )

                onSuccess()
            } else {
                onFailure()
            }
        }
    }
}
