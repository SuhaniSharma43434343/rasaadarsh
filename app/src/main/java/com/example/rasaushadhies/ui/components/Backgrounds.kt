package com.example.rasaushadhies.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.rasaushadhies.ui.theme.*
import java.util.*
import kotlin.math.sin

/**
 * A high-performance, 4-layer Ayurvedic background.
 * 1. Dinacharya Gradient (Time-based color)
 * 2. Mesh Blobs (Prana/Energy movement)
 * 3. Procedural Nadi Lines (Safe version of Topo pattern)
 */
@Composable
fun AyurvedicBackground(
    modifier: Modifier = Modifier,
    showBlobs: Boolean = true,
    showLines: Boolean = true,
    content: @Composable () -> Unit
) {
    // 1. Dinacharya Gradient Logic (Simplified to Day/Night based on hour)
    val calendar = remember { Calendar.getInstance() }
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    
    val isDay = hour in 6..18
    val topColor by animateColorAsState(
        targetValue = if (isDay) PrimaryDarkGreen else Color(0xFF12251B),
        animationSpec = tween(2000), label = "topColor"
    )
    val bottomColor by animateColorAsState(
        targetValue = if (isDay) BackgroundColor else Color(0xFF0A120E),
        animationSpec = tween(2000), label = "bottomColor"
    )

    // Capture Composable colors for use in Canvas
    val accentAmberLight = AccentAmberLight
    val primaryGreen = PrimaryGreen

    Box(modifier = modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(topColor, bottomColor))
    )) {
        
        // 2. Mesh Blobs (Prana Energy)
        if (showBlobs) {
            val infiniteTransition = rememberInfiniteTransition(label = "blobs")
            val blobOffset1 by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = 100f,
                animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse),
                label = "blob1"
            )
            val blobOffset2 by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = -100f,
                animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Reverse),
                label = "blob2"
            )

            Canvas(modifier = Modifier.fillMaxSize().alpha(0.15f).blur(80.dp)) {
                drawCircle(
                    color = accentAmberLight,
                    radius = size.width * 0.4f,
                    center = Offset(size.width * 0.2f + blobOffset1, size.height * 0.3f)
                )
                drawCircle(
                    color = primaryGreen,
                    radius = size.width * 0.5f,
                    center = Offset(size.width * 0.8f, size.height * 0.7f + blobOffset2)
                )
            }
        }

        // 3. Procedural Nadi Lines (Topo Pattern replacement)
        if (showLines) {
            Canvas(modifier = Modifier.fillMaxSize().alpha(0.05f)) {
                val width = size.width
                val height = size.height
                val spacing = 40.dp.toPx()
                
                for (y in 0..(height / spacing).toInt()) {
                    val path = Path()
                    path.moveTo(0f, y * spacing)
                    
                    for (x in 0..(width / 20).toInt()) {
                        val px = x * 20f
                        // Create a wavy line using sine
                        val py = (y * spacing) + (sin(x * 0.2f + y) * 15f)
                        path.lineTo(px, py)
                    }
                    
                    drawPath(
                        path = path,
                        color = Color.White,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }

        // 4. Content Layer
        content()
    }
}
