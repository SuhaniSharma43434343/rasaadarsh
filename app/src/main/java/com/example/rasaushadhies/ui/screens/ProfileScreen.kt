package com.example.rasaushadhies.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rasaushadhies.data.local.PractitionerProfile
import com.example.rasaushadhies.ui.theme.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.automirrored.filled.ExitToApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profile: PractitionerProfile,
    onBack: () -> Unit,
    onSave: (PractitionerProfile) -> Unit,
    onUploadDegreeCertificate: (android.net.Uri) -> Unit,
    onUploadRegistrationCertificate: (android.net.Uri) -> Unit,
    onLogout: () -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var qualification by remember { mutableStateOf(profile.qualification) }
    var clinicName by remember { mutableStateOf(profile.clinicName) }
    var registrationNo by remember { mutableStateOf(profile.registrationNo) }
    
    val degreePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let(onUploadDegreeCertificate)
    }

    val registrationPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let(onUploadRegistrationCertificate)
    }
    
    val isFormValid = name.isNotBlank() && qualification.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Practitioner Profile", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Log out",
                            tint = Color(0xFFC62828)
                        )
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar Placeholder
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(PrimaryGradient, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = White
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                text = if (profile.isSetupComplete) "Professional Identity" else "Setup Your Profile",
                style = MaterialTheme.typography.titleMedium.copy(color = PrimaryDarkGreen)
            )
            
            Text(
                text = "Personalize your shared clinical reports",
                style = MaterialTheme.typography.bodySmall.copy(color = Muted)
            )
            
            Spacer(Modifier.height(32.dp))
            
            // Form Fields
            ProfileTextField(
                label = "Full Name",
                value = name,
                onValueChange = { name = it },
                icon = Icons.Default.Person
            )
            
            Spacer(Modifier.height(16.dp))
            
            ProfileTextField(
                label = "Qualification (e.g. BAMS, MD)",
                value = qualification,
                onValueChange = { qualification = it },
                icon = Icons.Default.School
            )
            
            Spacer(Modifier.height(16.dp))
            
            ProfileTextField(
                label = "Clinic / Hospital Name",
                value = clinicName,
                onValueChange = { clinicName = it },
                icon = Icons.Default.HomeWork
            )
            
            Spacer(Modifier.height(16.dp))
            
            ProfileTextField(
                label = "Registration Number",
                value = registrationNo,
                onValueChange = { registrationNo = it },
                icon = Icons.Default.Badge
            )
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                text = "Verification Documents Required",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(Modifier.height(12.dp))

            CertificateUploadCard(
                label = "Medical Degree Certificate",
                status = profile.degreeVerificationStatus,
                onUploadClick = { degreePickerLauncher.launch("*/*") }
            )

            Spacer(Modifier.height(16.dp))

            CertificateUploadCard(
                label = "Govt Registration Certificate",
                status = profile.registrationVerificationStatus,
                onUploadClick = { registrationPickerLauncher.launch("*/*") }
            )
            
            Spacer(Modifier.height(40.dp))
            
            Button(
                onClick = {
                    onSave(profile.copy(
                        name = name,
                        qualification = qualification,
                        clinicName = clinicName,
                        registrationNo = registrationNo,
                        isSetupComplete = true
                    ))
                },
                enabled = isFormValid,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryDarkGreen,
                    contentColor = White
                )
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save Professional Profile", style = MaterialTheme.typography.titleMedium)
            }
            
            Spacer(Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFC62828)
                ),
                border = BorderStroke(1.dp, Color(0xFFC62828))
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Log Out", style = MaterialTheme.typography.titleMedium)
            }
            
            Spacer(Modifier.height(16.dp))
            
            if (!isFormValid) {
                Text(
                    "* Name and Qualification are required for sharing",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Red.copy(0.7f))
                )
            }
        }
    }
}

@Composable
fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = PrimaryGreen) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryGreen,
            unfocusedBorderColor = DividerColor,
            focusedLabelColor = PrimaryGreen
        )
    )
}

@Composable
fun CertificateUploadCard(
    label: String,
    status: String,
    onUploadClick: () -> Unit
) {
    val statusColor = when (status) {
        "APPROVED" -> Color(0xFF2E7D32)
        "PENDING" -> Color(0xFFE65100)
        "REJECTED" -> Color(0xFFC62828)
        else -> Muted
    }
    
    val statusText = when (status) {
        "NONE" -> "Not Uploaded"
        else -> status
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        border = BorderStroke(1.dp, DividerColor.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                Text(
                    text = "Status: $statusText",
                    color = statusColor,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Button(
                onClick = onUploadClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(if (status == "NONE" || status == "REJECTED") "Upload" else "Re-upload", fontSize = 13.sp)
            }
        }
    }
}
