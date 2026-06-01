package com.example.rasaushadhies

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rasaushadhies.ui.viewmodels.SecurityViewModel
import com.example.rasaushadhies.ui.viewmodels.SecurityViewModelFactory
import com.example.rasaushadhies.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSecurityResult: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("practitioner_prefs", android.content.Context.MODE_PRIVATE) }
    val securityViewModel: SecurityViewModel = viewModel(factory = SecurityViewModelFactory(prefs))
    
    var visible by remember { mutableStateOf(true) }
    val alpha = 1f
    val scale = 1f

    LaunchedEffect(Unit) {
        android.util.Log.d("AppDebug", "SplashScreen LaunchedEffect started")
        delay(500)
        
        // Security Audit
        val status = securityViewModel.performSecurityAudit()
        android.util.Log.d("AppDebug", "Security audit status: $status")
        val destination = when (status) {
            SecurityViewModel.SecurityState.VALID -> Routes.HOME
            SecurityViewModel.SecurityState.EXPIRED -> Routes.EXPIRED
            SecurityViewModel.SecurityState.TAMPERED -> Routes.TAMPERED
        }
        
        android.util.Log.d("AppDebug", "Navigating to: $destination")
        onSecurityResult(destination)
    }

    // Parchment color theme gradient
    val parchmentLight = Color(0xFFFCF8E8)
    val parchmentMain = Color(0xFFF2EAD3)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(parchmentLight, parchmentMain)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        android.util.Log.d("AppDebug", "SplashScreen drawing content")
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(alpha)
                .scale(scale)
                .padding(horizontal = 40.dp)
        ) {
            // Logo - Significantly larger as requested
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(400.dp)
                    .padding(bottom = 32.dp)
            )

            // Creative Tagline - Using PrimaryDarkGreen for readability on light background
            Text(
                text = "Reviving the Ancient Science of Rasaushadhi",
                fontSize = 15.sp,
                color = PrimaryDarkGreen,
                fontWeight = FontWeight.Bold,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                letterSpacing = 0.5.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Bottom Progress and Branding
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .alpha(alpha)
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .width(140.dp)
                    .height(3.dp)
                    .clip(CircleShape),
                color = PrimaryGreen, // Green looks better on parchment than Amber
                trackColor = PrimaryGreen.copy(alpha = 0.1f)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "v1.0  ·  Parul Institute of Ayurved",
                fontSize = 11.sp,
                color = PrimaryDarkGreen.copy(alpha = 0.6f),
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashScreenPreview() {
    RasaushadhiTheme {
        SplashScreen(onSecurityResult = {})
    }
}
