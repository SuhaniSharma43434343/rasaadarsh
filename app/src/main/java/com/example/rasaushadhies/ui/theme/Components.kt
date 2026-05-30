package com.example.rasaushadhies.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rasaushadhies.ui.theme.*

// ─── Top App Bar ──────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RasaTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    isHindi: Boolean = false,
    onLanguageToggle: (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(color = White)
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(color = White.copy(alpha = 0.7f))
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                }
            }
        },
        actions = {
            if (onLanguageToggle != null) {
                TextButton(
                    onClick = onLanguageToggle,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = if (isHindi) "EN" else "हिन्दी",
                        color = AccentAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            actions()
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDarkGreen),
        windowInsets = WindowInsets.statusBars
    )
}

// ─── Section Header ───────────────────────────────────────────
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            Modifier
                .width(24.dp)
                .height(1.5.dp)
                .background(PrimaryGreen)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = PrimaryGreen
        )
    }
}

// ─── Disease Chip ─────────────────────────────────────────────
@Composable
fun DiseaseChip(
    label: String,
    onClick: () -> Unit = {},
    selected: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) PrimaryGreen else ChipBg,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .border(1.dp, PrimaryGreen.copy(alpha = 0.4f), RoundedCornerShape(50))
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (selected) White else ChipText,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

// ─── Letter Avatar ────────────────────────────────────────────
@Composable
fun LetterAvatar(letter: String, size: Int = 40, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(PrimaryGreen)
    ) {
        Text(
            text = letter.take(1).uppercase(),
            color = White,
            fontSize = (size * 0.45).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─── Info Card Section ────────────────────────────────────────
@Composable
fun InfoSection(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionLabel(text = title, modifier = Modifier.padding(bottom = 10.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

// ─── Primary Button ───────────────────────────────────────────
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = PrimaryGreen
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─── Outlined Button ──────────────────────────────────────────
@Composable
fun OutlinedPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        border = ButtonDefaults.outlinedButtonBorder,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

// ─── Medicine List Row Item ───────────────────────────────────
@Composable
fun MedicineListItem(
    index: Int,
    name: String,
    hindiName: String,
    diseases: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, PrimaryGreen, CircleShape)
            ) {
                Text(
                    text = "$index",
                    fontSize = 13.sp,
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = hindiName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
                Text(
                    text = diseases,
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryGreen.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}