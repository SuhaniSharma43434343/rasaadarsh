package com.example.rasaushadhies.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rasaushadhies.ui.theme.*
import androidx.compose.animation.core.*
import com.example.rasaushadhies.ui.screens.bounceClick

@Composable
fun LoginScreen(
    onGoogleSignInClick: () -> Unit,
    onAdminLoginClick: (String, String) -> Boolean,
    errorMessage: String? = null
) {
    var showAdminLogin by remember { mutableStateOf(false) }
    var adminUsername by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf<String?>(null) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    com.example.rasaushadhies.ui.theme.AppBackground(
        screenType = com.example.rasaushadhies.ui.theme.ScreenBackground.SAVED
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(90.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .background(PrimaryGradient, RoundedCornerShape(24.dp))
                    .border(2.dp, AccentAmber.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            ) {
                Text("🌿", fontSize = 42.sp)
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Welcome to Rasaadarsh",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
            )

            Text(
                text = "Verified Ayurvedic Knowledge Engine",
                color = Muted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 48.dp)
            )

            if (errorMessage != null || loginError != null) {
                Text(
                    text = errorMessage ?: loginError ?: "",
                    color = Color.Red,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (!showAdminLogin) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryDarkGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .bounceClick(onGoogleSignInClick)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sign in with Google", style = MaterialTheme.typography.titleMedium.copy(color = White))
                    }
                }

                Spacer(Modifier.height(16.dp))

                TextButton(
                    onClick = { showAdminLogin = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = PrimaryGreen)
                ) {
                    Text("Access Admin Portal")
                }
            } else {
                OutlinedTextField(
                    value = adminUsername,
                    onValueChange = { adminUsername = it },
                    label = { Text("Admin Username") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        focusedLabelColor = PrimaryGreen
                    )
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = adminPassword,
                    onValueChange = { adminPassword = it },
                    label = { Text("Admin Password") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        focusedLabelColor = PrimaryGreen
                    )
                )

                Spacer(Modifier.height(24.dp))

                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryDarkGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .bounceClick {
                            val success = onAdminLoginClick(adminUsername, adminPassword)
                            if (!success) {
                                loginError = "Invalid admin credentials!"
                            } else {
                                loginError = null
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = White)
                        Spacer(Modifier.width(8.dp))
                        Text("Login as Admin", style = MaterialTheme.typography.titleMedium.copy(color = White))
                    }
                }

                Spacer(Modifier.height(16.dp))

                TextButton(
                    onClick = { showAdminLogin = false; loginError = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = PrimaryGreen)
                ) {
                    Text("Back to Practitioner Login")
                }
            }
        }
    }
}
