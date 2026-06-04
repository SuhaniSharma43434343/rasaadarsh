package com.example.rasaushadhies.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rasaushadhies.data.local.PractitionerProfile
import com.example.rasaushadhies.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    pendingUsers: List<Pair<String, PractitionerProfile>>, // userId to Profile mapping
    onApproveUser: (String, String) -> Unit, // userId, certificateType ("degree" or "registration")
    onRejectUser: (String, String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Admin Verification Console", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("Verify Practitioner Credentials", style = MaterialTheme.typography.labelSmall.copy(color = Muted))
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Log out", tint = PrimaryGreen)
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
        if (pendingUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉", fontSize = 64.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("All caught up!", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                    Text("No pending certificate verifications", style = MaterialTheme.typography.bodyMedium.copy(color = Muted))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(pendingUsers, key = { it.first }) { (userId, profile) ->
                    PendingUserCard(
                        userId = userId,
                        profile = profile,
                        onViewDocument = { url ->
                            try {
                                val uri = Uri.parse(url)
                                val intent = if (uri.scheme == "file") {
                                    val file = java.io.File(uri.path ?: "")
                                    val contentUri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(contentUri, context.contentResolver.getType(contentUri) ?: "*/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                } else {
                                    Intent(Intent.ACTION_VIEW, uri)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Opening document: $url", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        onApprove = { certType -> onApproveUser(userId, certType) },
                        onReject = { certType -> onRejectUser(userId, certType) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingUserCard(
    userId: String,
    profile: PractitionerProfile,
    onViewDocument: (String) -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // User Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    Text(
                        text = "${profile.qualification} · ${profile.clinicName}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Muted)
                    )
                    Text(
                        text = "Reg No: ${profile.registrationNo}",
                        style = MaterialTheme.typography.labelSmall.copy(color = PrimaryGreen, fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(Modifier.height(16.dp))

            // Certificates Section
            Text("Submitted Documents:", style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))

            Spacer(Modifier.height(12.dp))

            // Document 1: Degree
            DocumentRow(
                label = "Medical Degree Certificate",
                status = profile.degreeVerificationStatus,
                uri = profile.degreeCertificateUri,
                onView = { profile.degreeCertificateUri?.let(onViewDocument) },
                onApprove = { onApprove("degree") },
                onReject = { onReject("degree") }
            )

            Spacer(Modifier.height(12.dp))

            // Document 2: Registration
            DocumentRow(
                label = "Govt Registration Certificate",
                status = profile.registrationVerificationStatus,
                uri = profile.registrationCertificateUri,
                onView = { profile.registrationCertificateUri?.let(onViewDocument) },
                onApprove = { onApprove("registration") },
                onReject = { onReject("registration") }
            )
        }
    }
}

@Composable
private fun DocumentRow(
    label: String,
    status: String,
    uri: String?,
    onView: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
            .background(BackgroundColor.copy(0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
            if (uri != null) {
                Text(
                    text = "View Document ↗",
                    style = MaterialTheme.typography.labelMedium.copy(color = PrimaryGreen, fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .clickable(onClick = onView)
                        .padding(vertical = 4.dp)
                )
            } else {
                Text("Not Uploaded", style = MaterialTheme.typography.labelSmall.copy(color = Color.Red.copy(0.7f)))
            }
        }

        Spacer(Modifier.width(8.dp))

        if (status == "PENDING") {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onApprove) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Approve", tint = Color(0xFF2E7D32))
                }
                IconButton(onClick = onReject) {
                    Icon(Icons.Default.Cancel, contentDescription = "Reject", tint = Color(0xFFC62828))
                }
            }
        } else {
            val statusColor = when (status) {
                "APPROVED" -> Color(0xFF2E7D32)
                "REJECTED" -> Color(0xFFC62828)
                else -> Muted
            }
            Text(
                text = status,
                color = statusColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
