package com.example.rasaushadhies.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import com.example.rasaushadhies.ui.components.RasaTopBar
import com.example.rasaushadhies.ui.components.SectionLabel
import com.example.rasaushadhies.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MedicineDetailScreen(
    isHindi: Boolean,
    onLanguageToggle: () -> Unit,
    medicine: Medicine,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onToggleBookmark: () -> Unit,
    onUpdateNotes: (Int, String) -> Unit
) {
    com.example.rasaushadhies.ui.theme.AppBackground(
        screenType = com.example.rasaushadhies.ui.theme.ScreenBackground.DETAIL
    ) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            RasaTopBar(
                title = medicine.name,
                subtitle = medicine.hindiName,
                onBack = onBack,
                isHindi = isHindi,
                onLanguageToggle = onLanguageToggle,
                actions = {
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share",
                            tint = White
                        )
                    }
                    IconButton(onClick = onToggleBookmark) {
                        Icon(
                            imageVector = if (medicine.isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (medicine.isBookmarked) AccentAmber else White
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        // Feature 2: Collapsible hero — track scroll
        val scrollState = rememberScrollState()
        val heroMaxHeight = 200.dp
        val heroMinHeight = 0.dp
        val heroHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { heroMaxHeight.toPx() }
        val heroHeight by remember {
            derivedStateOf {
                val collapsed = (scrollState.value / heroHeightPx).coerceIn(0f, 1f)
                androidx.compose.ui.unit.lerp(heroMaxHeight, heroMinHeight, collapsed)
            }
        }
        val heroContentAlpha by remember {
            derivedStateOf {
                1f - (scrollState.value / (heroHeightPx * 0.6f)).coerceIn(0f, 1f)
            }
        }

        // Entrance animation state
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        val heroAlpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = tween(500),
            label = "heroAlpha"
        )
        val bodySlide by animateDpAsState(
            targetValue = if (visible) 0.dp else 60.dp,
            animationSpec = tween(500, easing = FastOutSlowInEasing),
            label = "bodySlide"
        )
        val bodyAlpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = tween(500, delayMillis = 150),
            label = "bodyAlpha"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
        ) {
            var fontSizeMultiplier by remember { mutableFloatStateOf(1f) }

            // ── Collapsing Hero header ────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
                    .graphicsLayer { alpha = heroAlpha }
                    .background(PrimaryGradient),
                contentAlignment = Alignment.BottomStart
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .graphicsLayer { alpha = heroContentAlpha }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(8.dp), color = White.copy(alpha = 0.2f)) {
                            Text(
                                text = "SR NO. ${medicine.id}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(color = White, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = medicine.hindiName,
                        style = MaterialTheme.typography.bodyLarge.copy(color = White.copy(alpha = 0.8f))
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = medicine.name,
                        style = MaterialTheme.typography.displayLarge.copy(color = White, fontSize = 28.sp)
                    )
                }
            }

            // ── Body content ───────────────────────────────
            Column(
                modifier = Modifier
                    .graphicsLayer { alpha = bodyAlpha }
                    .background(Color(0xEEF0EBDF), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(20.dp)
            ) {

                // Text Size Adjuster
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text("Text Size:", style = MaterialTheme.typography.labelSmall, color = Muted)
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { if (fontSizeMultiplier > 0.8f) fontSizeMultiplier -= 0.1f }, modifier = Modifier.size(28.dp).background(CardBg, CircleShape)) {
                        Text("A-", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { if (fontSizeMultiplier < 1.4f) fontSizeMultiplier += 0.1f }, modifier = Modifier.size(28.dp).background(CardBg, CircleShape)) {
                        Text("A+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                    }
                }

                // Clinical Properties Grid
                SectionLabel(text = "Ayurvedic Clinical Properties", modifier = Modifier.padding(bottom = 12.dp))
                PropertiesGrid(medicine.dosage, medicine.anupana, fontSizeMultiplier)
                Spacer(Modifier.height(24.dp))

                // Classical Shloka / Reference Section
                if (medicine.shloka.isNotBlank()) {
                    var showFocusMode by remember { mutableStateOf(false) }
                    
                    SectionLabel(text = "Classical Reference (Shloka)", modifier = Modifier.padding(bottom = 12.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                            .clickable { showFocusMode = true },
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, PrimaryGreen.copy(0.2f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Feature 9: Tiro Devanagari font for Sanskrit shloka
                            Text(
                                text = medicine.shloka,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = com.example.rasaushadhies.ui.theme.TiroDevanagariFontFamily,
                                    fontStyle = FontStyle.Italic,
                                    color = PrimaryDarkGreen,
                                    lineHeight = 26.sp,
                                    fontSize = 16.sp * fontSizeMultiplier
                                )
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Fullscreen, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Tap to focus", style = MaterialTheme.typography.labelSmall, color = PrimaryGreen)
                            }
                        }
                    }

                    if (showFocusMode) {
                        ShlokaFocusMode(
                            shloka = medicine.shloka,
                            onDismiss = { showFocusMode = false }
                        )
                    }
                }

                // Therapeutic Benefits
                SectionLabel(text = "Therapeutic Range & Benefits", modifier = Modifier.padding(bottom = 12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    border = BorderStroke(1.dp, DividerColor.copy(0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(medicine.benefits, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp * fontSizeMultiplier))
                    }
                }

                // Ingredients Table
                SectionLabel(text = "Ingredients (Ghatak Dravya)", modifier = Modifier.padding(bottom = 12.dp))
                IngredientsTable(medicine.ingredientsList, medicine.ingredients, fontSizeMultiplier)
                Spacer(Modifier.height(24.dp))

                // Method of Preparation
                SectionLabel(text = "Method of Preparation", modifier = Modifier.padding(bottom = 12.dp))
                PreparationSteps(medicine.preparation, fontSizeMultiplier)
                Spacer(Modifier.height(24.dp))

                // Reference
                if (medicine.reference.isNotBlank()) {
                    SectionLabel(text = "Source Reference", modifier = Modifier.padding(bottom = 12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = White),
                        border = BorderStroke(1.dp, DividerColor.copy(0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(medicine.reference, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, color = Muted))
                        }
                    }
                }

                // Personal Practice Notes
                SectionLabel(text = "Personal Practice Notes", modifier = Modifier.padding(bottom = 12.dp))
                var noteText by remember { mutableStateOf(medicine.clinicalNotes) }
                OutlinedTextField(
                    value = noteText,
                    onValueChange = {
                        noteText = it
                        onUpdateNotes(medicine.id, it)
                    },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("Record your clinical observations here...") },
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = White,
                        unfocusedContainerColor = White,
                        focusedIndicatorColor = PrimaryGreen,
                        unfocusedIndicatorColor = DividerColor
                    )
                )

                Spacer(Modifier.height(32.dp))

                // Actions
                ActionButtons(medicine, onToggleBookmark, onShare)
                Spacer(Modifier.height(50.dp))
            }
        }
    }
    } // end AppBackground
}

@Composable
fun PropertiesGrid(mainDose: String, anupana: String, fontSizeMultiplier: Float = 1f) {
    val items = mutableListOf<Triple<String, String, String>>()
    
    if (mainDose.isNotBlank() && mainDose != "Consult Physician") {
        items.add(Triple("Dose", mainDose, "🥄"))
    }
    
    if (anupana.isNotBlank() && anupana != "N/A") {
        items.add(Triple("Anupana", anupana, "💧"))
    }

    if (items.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (i in items.indices step 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PropertyCard(items[i].first, items[i].second, items[i].third, Modifier.weight(1f), fontSizeMultiplier)
                if (i + 1 < items.size) {
                    PropertyCard(items[i + 1].first, items[i + 1].second, items[i + 1].third, Modifier.weight(1f), fontSizeMultiplier)
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun PropertyCard(label: String, value: String, icon: String, modifier: Modifier = Modifier, fontSizeMultiplier: Float) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, DividerColor.copy(0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 16.sp)
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelSmall.copy(color = Muted, fontWeight = FontWeight.Bold))
            }
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, color = PrimaryDarkGreen, fontSize = 14.sp * fontSizeMultiplier))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IngredientsTable(list: List<com.example.rasaushadhies.data.local.Ingredient>, raw: String, fontSizeMultiplier: Float = 1f) {
    if (list.isEmpty()) {
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(16.dp)) {
            Text(raw, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp * fontSizeMultiplier), color = TextPrimary)
        }
        return
    }

    val validList = list.filter { it.sanskritName.trim() != "Ingredient" && it.sanskritName.trim() != "(Sanskrit)" }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in validList.indices step 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IngredientCard(validList[i], modifier = Modifier.weight(1f), fontSizeMultiplier)
                if (i + 1 < validList.size) {
                    IngredientCard(validList[i + 1], modifier = Modifier.weight(1f), fontSizeMultiplier)
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun IngredientCard(item: com.example.rasaushadhies.data.local.Ingredient, modifier: Modifier = Modifier, fontSizeMultiplier: Float) {
    val cleanSanskritName = item.sanskritName.replace(Regex("\\s*\\(.*?\\)"), "").trim()

    // Feature 10: parse quantity to a visual bar ratio (0.0 – 1.0)
    val barRatio = remember(item.quantity) {
        val q = item.quantity.lowercase()
        when {
            q.contains("equal") || q.contains("sama") -> 1.0f
            q.contains("half") || q.contains("ardha") -> 0.5f
            q.contains("quarter") || q.contains("1/4") -> 0.25f
            q.contains("double") || q.contains("dviguna") -> 1.0f
            q.contains("1/8") -> 0.125f
            q.contains("1/16") -> 0.0625f
            else -> {
                // Try to extract a number (gm / mg / ratti)
                val num = Regex("(\\d+(?:\\.\\d+)?)").find(q)?.groupValues?.get(1)?.toFloatOrNull()
                if (num != null) (num / 500f).coerceIn(0.05f, 1f) else 0.5f
            }
        }
    }
    val animatedBar by animateFloatAsState(
        targetValue = barRatio,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "ingredientBar"
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, DividerColor.copy(0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(cleanSanskritName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp * fontSizeMultiplier), color = PrimaryDarkGreen)
            Spacer(Modifier.height(4.dp))
            Text(item.englishName, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp * fontSizeMultiplier), color = Muted)
            Spacer(Modifier.height(8.dp))
            // Quantity badge
            Box(modifier = Modifier.background(PrimaryGreen.copy(0.1f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(item.quantity, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp * fontSizeMultiplier), color = PrimaryGreen)
            }
            Spacer(Modifier.height(8.dp))
            // Feature 10: quantity proportion bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(DividerColor, RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedBar)
                        .height(4.dp)
                        .background(
                            Brush.horizontalGradient(listOf(PrimaryGreen, AccentAmber)),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}

@Composable
fun PreparationSteps(raw: String, fontSizeMultiplier: Float = 1f) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, DividerColor.copy(0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val normalizedRaw = raw.replace("\\n", "\n")
            val steps = normalizedRaw.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
            if (steps.isEmpty()) {
                Text("No preparation details available.", style = MaterialTheme.typography.bodyLarge, color = Muted)
            } else {
                steps.forEachIndexed { index, step ->
                    val cleanStep = step.replaceFirst(Regex("^\\d+\\.\\s*"), "")
                    val isLast = index == steps.lastIndex
                    Row(modifier = Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.Top) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
                            Box(
                                modifier = Modifier.padding(top = 2.dp).size(24.dp).background(PrimaryGreen.copy(0.15f), androidx.compose.foundation.shape.CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "${index + 1}", style = MaterialTheme.typography.labelSmall.copy(color = PrimaryDarkGreen, fontWeight = FontWeight.Bold))
                            }
                            if (!isLast) {
                                Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(PrimaryGreen.copy(0.2f)))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = cleanStep,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp * fontSizeMultiplier),
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = if (isLast) 0.dp else 24.dp).padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButtons(medicine: Medicine, onToggleBookmark: () -> Unit, onShare: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onToggleBookmark,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            // Bookmark pop scale animation
            val bookmarkScale by animateFloatAsState(
                targetValue = if (medicine.isBookmarked) 1.15f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "bookmarkScale"
            )
            Box(
                modifier = Modifier.fillMaxSize().background(if (medicine.isBookmarked) Brush.linearGradient(listOf(AccentAmber, AccentAmberLight)) else PrimaryGradient),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (medicine.isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp).scale(bookmarkScale)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(if (medicine.isBookmarked) "Saved to Cabinet" else "Save to Cabinet", style = MaterialTheme.typography.titleLarge.copy(color = White, fontSize = 16.sp))
                }
            }
        }

        OutlinedButton(
            onClick = onShare,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            border = BorderStroke(1.dp, PrimaryGreen),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Share with Patients", style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp))
            }
        }
    }
}

@Composable
fun ShlokaFocusMode(
    shloka: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PrimaryGradient)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(White.copy(0.08f), Color.Transparent),
                                radius = 600f
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CLASSICAL SHLOKA",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = White.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                            )
                            Text(
                                text = "Focus Mode",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .background(White.copy(alpha = 0.15f), CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        colors = CardDefaults.cardColors(containerColor = White.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, White.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = shloka,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = White,
                                    lineHeight = 38.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1.2f))

                    Text(
                        text = "🌿 Study daily to master the art of Rasaushadhi",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
