package ie.tus.mad.carstop1.login

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator()
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "CarStop",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // Email Input
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Password Input
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Login Button
                Button(
                    onClick = {
                        loading = true
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                loading = false
                                if (task.isSuccessful) {
                                    val user = auth.currentUser
                                    if (user != null) {
                                        if (user.email == "justinerenz98@gmail.com") {
                                            navController.navigate("admin_home_screen") {
                                                popUpTo("login_screen") { inclusive = true }
                                            }
                                        } else {
                                            navController.navigate("home_screen") {
                                                popUpTo("login_screen") { inclusive = true }
                                            }
                                        }
                                    }
                                } else {
                                    Toast.makeText(
                                        navController.context,
                                        task.exception?.localizedMessage ?: "Login Failed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF7C4DFF))
                ) {
                    Text("Login", color = Color.White)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Navigate to Sign Up Button
                TextButton(onClick = { navController.navigate("sign_up_screen") }) {
                    Text("Don't have an account? Sign Up")
                }
            }
        }
    }
}
