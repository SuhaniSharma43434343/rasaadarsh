package com.example.rasaushadhies.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rasaushadhies.R
import com.example.rasaushadhies.ui.theme.*

@Composable
fun AccessExpiredScreen() {
    val context = LocalContext.current
    
    // Admin Details
    val adminPhone = "+919000000000" // Placeholder, replace with actual
    val adminWhatsapp = "919000000000"
    
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
                        .background(Color(0xFFFFF1F1), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_dialog_info),
                        contentDescription = "Expired",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                Spacer(Modifier.height(24.dp))
                
                Text(
                    text = "Access Expired",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFD32F2F)
                    ),
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(12.dp))
                
                Text(
                    text = "Your application subscription has ended. Please contact the administrator to renew your license.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Muted,
                        lineHeight = 24.sp
                    ),
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(32.dp))
                
                // WhatsApp Button
                Button(
                    onClick = {
                        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$adminWhatsapp&text=Hello Admin, my RASAADARSH access has expired. Please assist.")
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Icon(painterResource(id = android.R.drawable.stat_notify_chat), contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("WhatsApp Administrator", fontWeight = FontWeight.Bold)
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Call Button
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$adminPhone"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryGreen)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryGreen)
                    Spacer(Modifier.width(12.dp))
                    Text("Call Administrator", color = PrimaryGreen, fontWeight = FontWeight.Bold)
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
