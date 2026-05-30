package com.example.rasaushadhies.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush

import androidx.compose.foundation.isSystemInDarkTheme

// ─── Colour Palette ───────────────────────────────────────────
val PrimaryGreen: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF3F6E4D) else Color(0xFF2E5339)
val PrimaryDarkGreen: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF1B3525) else Color(0xFF1B3525)
val AccentAmber: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFD4AF37) else Color(0xFFD4AF37)
val AccentAmberLight: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFE5C158) else Color(0xFFE5C158)
val SurfaceColor: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF121A15) else Color(0xFFFAFAF7)
val SurfaceVariant: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF1E2D24) else Color(0xFFF0EDE6)
val BackgroundColor: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF0D1410) else Color(0xFFF3F1EC)
val OnSurface: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFE5EBE7) else Color(0xFF1C1C1A)
val Muted: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFA0AFA5) else Color(0xFF757570)
val DividerColor: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF2A3E31) else Color(0xFFE5E2DB)
val CardBg: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF1A261D) else Color(0xFFFFFFFF)
val White: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF1E2D24) else Color(0xFFFFFFFF)
val TrueWhite: Color = Color(0xFFFFFFFF)
val ChipBg: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF1B3525) else Color(0xFFEAF2EC)
val ChipText: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFF8CC2A0) else Color(0xFF2D5C3E)
val TextPrimary: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0xFFE5EBE7) else Color(0xFF1C1C1A)

// ─── Modern Gradient Brushes ─────────────────────────────────

val PrimaryGradient: Brush
    @Composable get() = Brush.verticalGradient(
        colors = listOf(PrimaryGreen, PrimaryDarkGreen)
    )

val SurfaceGradient: Brush
    @Composable get() = Brush.radialGradient(
        colors = listOf(if (isSystemInDarkTheme()) Color(0xFF1A261D) else Color(0xFFFFFFFF), SurfaceColor),
        radius = 2000f
    )

val GlassWhite: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0x33FFFFFF) else Color(0xB3FFFFFF) // 70% opacity white
val GlassBorder: Color
    @Composable get() = if (isSystemInDarkTheme()) Color(0x33FFFFFF) else Color(0x33000000) // 20% opacity black

// LightColorScheme moved to RasaushadhiTheme

// ─── Typography ───────────────────────────────────────────────
val AppTypography: Typography
    @Composable get() = Typography(
        displayLarge  = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold,   color = OnSurface),
        headlineLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold,   color = OnSurface),
        headlineMedium= TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = OnSurface),
        titleLarge    = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = OnSurface),
        titleMedium   = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = OnSurface),
        bodyLarge     = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, color = OnSurface),
        bodyMedium    = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, color = OnSurface),
        bodySmall     = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, color = Muted),
        labelSmall    = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp, color = Muted),
    )

// ─── Theme ────────────────────────────────────────────────────
// ✅ UPDATED: Removed system splash theme completely
@Composable
fun RasaushadhiTheme(content: @Composable () -> Unit) {
    val LightColorScheme = lightColorScheme(
        primary          = PrimaryGreen,
        onPrimary        = White,
        primaryContainer = Color(0xFFB8D4C4),
        secondary        = AccentAmber,
        onSecondary      = White,
        background       = SurfaceColor,
        surface          = SurfaceColor,
        surfaceVariant   = SurfaceVariant,
        onSurface        = OnSurface,
        onSurfaceVariant = Muted,
        outline          = DividerColor,
    )

    val colorScheme = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary          = PrimaryGreen,
            onPrimary        = TrueWhite,
            primaryContainer = Color(0xFF1B3525),
            secondary        = AccentAmber,
            onSecondary      = TrueWhite,
            background       = BackgroundColor,
            surface          = SurfaceColor,
            surfaceVariant   = SurfaceVariant,
            onSurface        = OnSurface,
            onSurfaceVariant = Muted,
            outline          = DividerColor,
        )
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content = content
    )
}