package com.example.rasaushadhies.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rasaushadhies.ui.theme.*

@Composable
fun SecurityTamperScreen() {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Warning Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFFFFF7E6), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Tampering",
                        tint = Color(0xFFFFA000),
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                Spacer(Modifier.height(24.dp))
                
                Text(
                    text = "Security Warning",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFE65100)
                    ),
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(12.dp))
                
                Text(
                    text = "System date tampering detected. To ensure data integrity, please set your phone's date and time to 'Automatic'.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Muted,
                        lineHeight = 24.sp
                    ),
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(32.dp))
                
                Button(
                    onClick = {
                        try {
                            context.startActivity(android.content.Intent(android.provider.Settings.ACTION_DATE_SETTINGS))
                        } catch (e: Exception) {
                            // Fallback if settings can't be opened
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Open Date Settings", fontWeight = FontWeight.Bold)
                }
                
                Spacer(Modifier.height(24.dp))
                
                // Exit Button
                TextButton(
                    onClick = { (context as? Activity)?.finish() }
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Muted)
                    Spacer(Modifier.width(8.dp))
                    Text("Exit App", color = Muted)
                }
            }
        }
    }
}
