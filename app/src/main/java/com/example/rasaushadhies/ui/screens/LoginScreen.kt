package com.example.rasaushadhies.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
                    .background(PrimaryGradient, RoundedCornerShape(24.dp))
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
                Button(
                    onClick = onGoogleSignInClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkGreen)
                ) {
                    Text("Sign in with Google", style = MaterialTheme.typography.titleMedium.copy(color = White))
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

                Button(
                    onClick = {
                        val success = onAdminLoginClick(adminUsername, adminPassword)
                        if (!success) {
                            loginError = "Invalid admin credentials!"
                        } else {
                            loginError = null
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkGreen)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Login as Admin", style = MaterialTheme.typography.titleMedium.copy(color = White))
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
