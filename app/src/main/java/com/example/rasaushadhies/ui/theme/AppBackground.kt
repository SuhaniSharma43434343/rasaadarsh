package com.example.rasaushadhies.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate

// ─────────────────────────────────────────────────────────────
//  Per-screen gradient definitions (Option 3)
// ─────────────────────────────────────────────────────────────

enum class ScreenBackground {
    HOME,           // warm amber-cream radial
    SEARCH_RESULTS, // cool mint-white linear
    DETAIL,         // forest-green top → warm cream bottom
    CHATBOT,        // deep teal-green subtle
    LIST,           // neutral warm parchment
    SAVED,          // soft gold-cream
    OTHER           // default warm cream
}

private fun gradientForScreen(type: ScreenBackground): Brush = when (type) {
    ScreenBackground.HOME -> Brush.radialGradient(
        colors = listOf(
            Color(0xFFF5E6C8),  // warm golden amber center
            Color(0xFFEDD9A3),  // mid honey
            Color(0xFFE8D08A)   // deeper golden parchment edge
        ),
        radius = 1800f
    )
    ScreenBackground.SEARCH_RESULTS -> Brush.verticalGradient(
        colors = listOf(
            Color(0xFFD6EAD7),  // fresh mint green top
            Color(0xFFEAF3E6),  // light mint mid
            Color(0xFFF2EDE0)   // warm cream bottom
        )
    )
    ScreenBackground.DETAIL -> Brush.verticalGradient(
        colors = listOf(
            Color(0xFFC8DDD1),  // rich forest green top
            Color(0xFFDDEDE4),  // soft green mid
            Color(0xFFF0E8D5)   // warm cream bottom
        )
    )
    ScreenBackground.CHATBOT -> Brush.radialGradient(
        colors = listOf(
            Color(0xFFCDE3D4),  // deep teal-green center
            Color(0xFFDCEBE1),  // mid
            Color(0xFFEDE6D8)   // warm edge
        ),
        radius = 1600f
    )
    ScreenBackground.LIST -> Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE8DFC8),  // rich warm parchment top
            Color(0xFFEFE7D0),  // mid
            Color(0xFFF3EBD8)   // lighter base
        )
    )
    ScreenBackground.SAVED -> Brush.radialGradient(
        colors = listOf(
            Color(0xFFF5E4A0),  // rich soft gold center
            Color(0xFFEDD98A),  // mid gold
            Color(0xFFE8CF78)   // deeper golden edge
        ),
        radius = 1600f
    )
    ScreenBackground.OTHER -> Brush.verticalGradient(
        colors = listOf(Color(0xFFEDE8D5), Color(0xFFE5DEC8))
    )
}

// ─────────────────────────────────────────────────────────────
//  Botanical watermark paths (Option 5)
//  Drawn at ~4% opacity so they're barely visible
// ─────────────────────────────────────────────────────────────

private fun DrawScope.drawLeaf(
    cx: Float, cy: Float,
    size: Float,
    rotation: Float,
    color: Color
) {
    rotate(rotation, pivot = Offset(cx, cy)) {
        val path = Path().apply {
            moveTo(cx, cy - size)
            cubicTo(
                cx + size * 0.6f, cy - size * 0.5f,
                cx + size * 0.6f, cy + size * 0.5f,
                cx, cy + size
            )
            cubicTo(
                cx - size * 0.6f, cy + size * 0.5f,
                cx - size * 0.6f, cy - size * 0.5f,
                cx, cy - size
            )
            close()
        }
        drawPath(path, color = color, style = Stroke(width = 4f, cap = StrokeCap.Round))
        // Midrib
        drawLine(color = color, start = Offset(cx, cy - size * 0.85f), end = Offset(cx, cy + size * 0.85f), strokeWidth = 3f)
        // Veins
        for (i in 1..4) {
            val vy = cy - size * 0.6f + i * (size * 0.3f)
            val vx = cx + size * 0.45f * (1f - i * 0.1f)
            drawLine(color = color, start = Offset(cx, vy), end = Offset(vx, vy - size * 0.15f), strokeWidth = 2f)
            drawLine(color = color, start = Offset(cx, vy), end = Offset(cx * 2 - vx, vy - size * 0.15f), strokeWidth = 2f)
        }
    }
}

private fun DrawScope.drawLotus(cx: Float, cy: Float, size: Float, color: Color) {
    val petalCount = 8
    for (i in 0 until petalCount) {
        val angle = (360f / petalCount) * i
        rotate(angle, pivot = Offset(cx, cy)) {
            val path = Path().apply {
                moveTo(cx, cy)
                cubicTo(
                    cx - size * 0.3f, cy - size * 0.6f,
                    cx - size * 0.15f, cy - size * 1.1f,
                    cx, cy - size * 1.2f
                )
                cubicTo(
                    cx + size * 0.15f, cy - size * 1.1f,
                    cx + size * 0.3f,  cy - size * 0.6f,
                    cx, cy
                )
                close()
            }
            drawPath(path, color = color, style = Stroke(width = 3.5f))
        }
    }
    // Inner petals
    for (i in 0 until petalCount) {
        val angle = (360f / petalCount) * i + 22.5f
        rotate(angle, pivot = Offset(cx, cy)) {
            val path = Path().apply {
                moveTo(cx, cy)
                cubicTo(
                    cx - size * 0.2f, cy - size * 0.4f,
                    cx - size * 0.1f, cy - size * 0.7f,
                    cx, cy - size * 0.75f
                )
                cubicTo(
                    cx + size * 0.1f, cy - size * 0.7f,
                    cx + size * 0.2f, cy - size * 0.4f,
                    cx, cy
                )
                close()
            }
            drawPath(path, color = color, style = Stroke(width = 2.5f))
        }
    }
    drawCircle(color = color, radius = size * 0.18f, center = Offset(cx, cy), style = Stroke(width = 3f))
}

private fun DrawScope.drawOmSymbolDot(cx: Float, cy: Float, r: Float, color: Color) {
    drawCircle(color = color, radius = r, center = Offset(cx, cy), style = Stroke(width = 1f))
    drawCircle(color = color, radius = r * 0.25f, center = Offset(cx, cy))
}

private fun DrawScope.drawBotanicalWatermark(screenType: ScreenBackground) {
    val w = size.width
    val h = size.height
    // 30% opacity — clearly visible watermark
    val c = Color(0xFF2E5339).copy(alpha = 0.28f)
    val cGold = Color(0xFFD4AF37).copy(alpha = 0.32f)

    when (screenType) {
        ScreenBackground.HOME -> {
            drawLotus(w * 0.85f, h * 0.06f, w * 0.28f, c)
            drawLeaf(w * 0.08f,  h * 0.25f, w * 0.14f, 35f,  c)
            drawLeaf(w * 0.88f, h * 0.55f, w * 0.16f, -20f, c)
            drawLeaf(w * 0.12f, h * 0.75f, w * 0.12f, 50f,  c)
            drawLeaf(w * 0.78f, h * 0.88f, w * 0.14f, -40f, c)
            drawLotus(w * 0.15f, h * 0.92f, w * 0.18f, cGold)
        }
        ScreenBackground.SEARCH_RESULTS -> {
            drawLeaf(w * 0.05f, h * 0.10f, w * 0.12f, 20f,  c)
            drawLeaf(w * 0.88f, h * 0.06f, w * 0.11f, -30f, c)
            drawLeaf(w * 0.85f, h * 0.50f, w * 0.13f, 15f,  c)
            drawLeaf(w * 0.06f, h * 0.72f, w * 0.12f, -25f, c)
            drawLotus(w * 0.5f, h * 0.96f, w * 0.16f, cGold)
        }
        ScreenBackground.DETAIL -> {
            drawLotus(w * 0.5f,  h * 0.04f, w * 0.24f, c)
            drawLeaf(w * 0.06f, h * 0.28f, w * 0.14f, 40f,  c)
            drawLeaf(w * 0.88f, h * 0.26f, w * 0.14f, -40f, c)
            drawLeaf(w * 0.05f, h * 0.65f, w * 0.12f, 25f,  c)
            drawLeaf(w * 0.90f, h * 0.70f, w * 0.12f, -25f, c)
            drawLotus(w * 0.5f,  h * 0.92f, w * 0.18f, cGold)
        }
        ScreenBackground.CHATBOT -> {
            drawLeaf(w * 0.05f, h * 0.06f, w * 0.12f, 15f,  c)
            drawLeaf(w * 0.88f, h * 0.10f, w * 0.13f, -20f, c)
            drawLotus(w * 0.5f, h * 0.5f,  w * 0.28f, Color(0xFF2E5339).copy(alpha = 0.18f))
            drawLeaf(w * 0.06f, h * 0.80f, w * 0.12f, 30f,  c)
            drawLeaf(w * 0.86f, h * 0.85f, w * 0.13f, -35f, c)
        }
        ScreenBackground.SAVED -> {
            drawLotus(w * 0.15f, h * 0.08f, w * 0.22f, cGold)
            drawLotus(w * 0.85f, h * 0.88f, w * 0.22f, cGold)
            drawLeaf(w * 0.86f, h * 0.30f, w * 0.14f, -20f, c)
            drawLeaf(w * 0.10f, h * 0.65f, w * 0.14f, 35f,  c)
        }
        else -> {
            drawLeaf(w * 0.07f, h * 0.12f, w * 0.13f, 25f,  c)
            drawLeaf(w * 0.86f, h * 0.18f, w * 0.12f, -30f, c)
            drawLeaf(w * 0.05f, h * 0.75f, w * 0.13f, 40f,  c)
            drawLotus(w * 0.82f, h * 0.82f, w * 0.18f, cGold)
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  Public composable — wrap any screen content with this
// ─────────────────────────────────────────────────────────────

@Composable
fun AppBackground(
    screenType: ScreenBackground = ScreenBackground.OTHER,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientForScreen(screenType))
    ) {
        // Botanical watermark layer
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawBotanicalWatermark(screenType)
        }
        // Screen content on top
        content()
    }
}
