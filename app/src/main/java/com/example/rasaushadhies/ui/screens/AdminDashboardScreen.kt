package com.example.rasaushadhies.ui.screens

import android.content.Intent
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.rasaushadhies.data.local.PractitionerProfile
import com.example.rasaushadhies.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import com.example.rasaushadhies.ui.components.shimmerEffect

suspend fun loadBitmapFromUri(context: android.content.Context, uriString: String): ImageBitmap? {
    return withContext(Dispatchers.IO) {
        try {
            // Check if it is a Base64 string and decode directly
            val isBase64 = uriString.length > 200 && (
                uriString.startsWith("/9j/") ||
                uriString.startsWith("data:image") ||
                (!uriString.startsWith("http") && !uriString.startsWith("content") && !uriString.startsWith("file") && !uriString.contains("/storage/") && !uriString.contains("/data/"))
            )
            if (isBase64) {
                val cleanBase64 = if (uriString.contains(",")) uriString.substringAfter(",") else uriString
                val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                return@withContext bitmap?.asImageBitmap()
            }

            val uri = Uri.parse(uriString)
            val inputStream: InputStream? = when {
                uriString.startsWith("/") -> {
                    java.io.File(uriString).inputStream()
                }
                uri.scheme == "file" -> {
                    val path = uri.path ?: uriString.substringAfter("file://")
                    java.io.File(path).inputStream()
                }
                uri.scheme == "content" -> {
                    context.contentResolver.openInputStream(uri)
                }
                uriString.startsWith("http") -> {
                    val connection = java.net.URL(uriString).openConnection() as java.net.HttpURLConnection
                    connection.doInput = true
                    connection.connect()
                    connection.inputStream
                }
                else -> {
                    try {
                        java.io.File(uriString).inputStream()
                    } catch (e: Exception) {
                        try {
                            context.contentResolver.openInputStream(uri)
                        } catch (e2: Exception) {
                            val connection = java.net.URL(uriString).openConnection() as java.net.HttpURLConnection
                            connection.doInput = true
                            connection.connect()
                            connection.inputStream
                        }
                    }
                }
            }
            if (inputStream != null) {
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                bitmap?.asImageBitmap()
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("AdminDashboard", "Error loading bitmap from URI: $uriString", e)
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    pendingUsers: List<Pair<String, PractitionerProfile>>, // userId to Profile mapping
    onApproveUser: (String, String) -> Unit, // userId, certificateType ("degree" or "registration")
    onRejectUser: (String, String) -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToMedicines: () -> Unit,
    onNavigateToChatbot: () -> Unit
) {
    val context = LocalContext.current
    var activePreviewUri by remember { mutableStateOf<String?>(null) }
    var activePreviewTitle by remember { mutableStateOf<String?>(null) }

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
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh list", tint = PrimaryGreen)
                    }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Quick Access Tools Header
            item {
                Text(
                    text = "Quick Access Tools",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
            }

            // Section 2: Quick Access Cards (Medicines & Chatbot)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Medicines Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = White),
                        elevation = CardDefaults.cardElevation(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp)
                            .bounceClick(onNavigateToMedicines)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(PrimaryGreen, PrimaryDarkGreen)))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Medicines Directory",
                                    tint = White,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text("Medicines", style = MaterialTheme.typography.titleMedium.copy(color = White, fontWeight = FontWeight.Bold))
                                    Text("Browse database", style = MaterialTheme.typography.bodySmall.copy(color = White.copy(0.8f)))
                                }
                            }
                        }
                    }

                    // Chatbot Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = White),
                        elevation = CardDefaults.cardElevation(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp)
                            .bounceClick(onNavigateToChatbot)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(AccentAmberLight, AccentAmber)))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "AI Chatbot",
                                    tint = White,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text("AI Chatbot", style = MaterialTheme.typography.titleMedium.copy(color = White, fontWeight = FontWeight.Bold))
                                    Text("Consult assistant", style = MaterialTheme.typography.bodySmall.copy(color = White.copy(0.8f)))
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Practitioner Verifications Header
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Practitioner Verifications",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                )
            }

            // Section 4: Practitioner Verifications List / Empty State
            if (pendingUsers.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🎉", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("All caught up!", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary))
                            Text("No pending certificate verifications", style = MaterialTheme.typography.bodyMedium.copy(color = Muted))
                        }
                    }
                }
            } else {
                items(pendingUsers, key = { it.first }) { (userId, profile) ->
                    PendingUserCard(
                        userId = userId,
                        profile = profile,
                        modifier = Modifier.animateItem(),
                        onViewDocument = { url ->
                            activePreviewUri = url
                            activePreviewTitle = if (url.contains("degree")) "Medical Degree Certificate" else "Govt Registration Certificate"
                        },
                        onApprove = { certType -> onApproveUser(userId, certType) },
                        onReject = { certType -> onRejectUser(userId, certType) }
                    )
                }
            }
        }
    }


    if (activePreviewUri != null) {
        Dialog(onDismissRequest = { activePreviewUri = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activePreviewTitle ?: "Document Preview",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                        )
                        IconButton(onClick = { activePreviewUri = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    var bitmap by remember(activePreviewUri) { mutableStateOf<ImageBitmap?>(null) }
                    var isLoading by remember(activePreviewUri) { mutableStateOf(true) }
                    var hasError by remember(activePreviewUri) { mutableStateOf(false) }

                    LaunchedEffect(activePreviewUri) {
                        isLoading = true
                        hasError = false
                        val uriStr = activePreviewUri
                        if (uriStr != null) {
                            val loaded = loadBitmapFromUri(context, uriStr)
                            if (loaded != null) {
                                bitmap = loaded
                            } else {
                                hasError = true
                            }
                        }
                        isLoading = false
                    }

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .shimmerEffect()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .background(BackgroundColor, RoundedCornerShape(12.dp))
                                .border(1.dp, DividerColor, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (hasError || bitmap == null) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(Icons.Default.Cancel, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("Failed to load document", style = MaterialTheme.typography.bodyMedium.copy(color = Muted))
                                    Spacer(Modifier.height(4.dp))
                                    Text("Path: $activePreviewUri", style = MaterialTheme.typography.labelSmall.copy(color = Muted), maxLines = 2)
                                }
                            } else {
                                Image(
                                    bitmap = bitmap!!,
                                    contentDescription = "Certificate Preview",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingUserCard(
    userId: String,
    profile: PractitionerProfile,
    modifier: Modifier = Modifier,
    onViewDocument: (String) -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = modifier.fillMaxWidth()
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
