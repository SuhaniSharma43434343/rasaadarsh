package com.example.rasaushadhies.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rasaushadhies.data.local.PractitionerProfile
import com.example.rasaushadhies.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationStatusScreen(
    profile: PractitionerProfile,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val isRejected = profile.degreeVerificationStatus == "REJECTED" || 
                     profile.registrationVerificationStatus == "REJECTED"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Verification", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = White,
                    titleContentColor = PrimaryDarkGreen
                )
            )
        },
        containerColor = BackgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(90.dp)
                    .background(
                        if (isRejected) Color(0xFFC62828).copy(0.15f) else PrimaryGreen.copy(0.15f),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Icon(
                    imageVector = if (isRejected) Icons.Default.NewReleases else Icons.Default.HourglassEmpty,
                    contentDescription = null,
                    tint = if (isRejected) Color(0xFFC62828) else PrimaryGreen,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = if (isRejected) "Verification Unsuccessful" else "Verification In Progress",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            val statusMessage = if (isRejected) {
                val detail = buildString {
                    if (profile.degreeVerificationStatus == "REJECTED") append("• Medical Degree Certificate was rejected.\n")
                    if (profile.registrationVerificationStatus == "REJECTED") append("• Government Registration Certificate was rejected.\n")
                }
                "Your uploaded certificates were reviewed and not approved:\n\n$detail\nPlease edit your profile and re-upload valid documents."
            } else {
                "Our administration team is reviewing your uploaded Medical Degree and Government Registration certificates. This process usually takes 24-48 hours."
            }

            Text(
                text = statusMessage,
                color = Muted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = onNavigateToProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkGreen)
            ) {
                Text(
                    text = if (isRejected) "Re-upload Documents" else "View Submitted Profile",
                    style = MaterialTheme.typography.titleMedium.copy(color = White)
                )
            }

            Spacer(Modifier.height(16.dp))

            TextButton(
                onClick = onLogout,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFC62828))
            ) {
                Text("Log Out / Sign In with Another Account", fontWeight = FontWeight.Bold)
            }
        }
    }
}
