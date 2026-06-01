package com.example.rasaushadhies.ui.screens

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rasaushadhies.ui.components.DiseaseChip
import com.example.rasaushadhies.ui.theme.*

// ─────────────────────────────────────────────────────────────
//  SCREEN 2 — Home Screen
// ─────────────────────────────────────────────────────────────

private val popularDiseases = listOf(
    Pair("Aamvata", "✋"),
    Pair("Agnimandya", "🩺"),
    Pair("Amlapita", "🧪"),
    Pair("Arsha", "🔥"),
    Pair("Atisara", "💧"),
    Pair("Bhagandar", "🪱"),
    Pair("Grahani", "🍃"),
    Pair("Hriday Roga", "🫀"),
    Pair("Jwara", "🌡️"),
    Pair("Kasa", "🗣️"),
    Pair("Shiro Roga", "🧠"),
    Pair("Shwas Hikka", "🫁")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isHindi: Boolean,
    allMedicines: List<com.example.rasaushadhies.ui.screens.Medicine> = emptyList(),
    recentMedicines: List<com.example.rasaushadhies.ui.screens.Medicine> = emptyList(),
    onLanguageToggle: () -> Unit,
    onSearch: (String) -> Unit,
    onDiseaseClick: (String) -> Unit,
    onAllMedicines: () -> Unit,
    onSaved: () -> Unit,
    onAiSearch: () -> Unit,
    onAbout: () -> Unit,
    onProfile: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                searchQuery = spokenText
                onSearch(spokenText)
            }
        }
    }

    LaunchedEffect(Unit) {
        android.util.Log.d("AppDebug", "HomeScreen LaunchedEffect started")
        visible = true
    }

    android.util.Log.d("AppDebug", "HomeScreen drawing content")
    Scaffold(
        containerColor = BackgroundColor,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {

            // ── Top Hero Area (Solid Green) ────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryDarkGreen) 
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = visible,
                            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically()
                        ) {
                            Text(
                                text = "RASAADARSH",
                                style = MaterialTheme.typography.displayLarge.copy(color = White, fontSize = 28.sp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = onLanguageToggle) {
                                Text(
                                    text = if (isHindi) "EN | हिन्दी" else "EN | हिन्दी",
                                    color = AccentAmberLight,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = onAbout,
                                    modifier = Modifier.background(White.copy(0.2f), RoundedCornerShape(12.dp)).size(36.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = "About", tint = White, modifier = Modifier.size(20.dp))
                                }
                                Text("About", color = White.copy(0.8f), fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Glassmorphic Search bar
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(0.dp),
                        colors = CardDefaults.cardColors(containerColor = White.copy(0.15f)),
                        border = BorderStroke(1.dp, androidx.compose.ui.graphics.Brush.verticalGradient(listOf(White.copy(0.5f), Color.Transparent))),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp).fillMaxHeight()
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = White)
                            Spacer(Modifier.width(12.dp))
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text("Search medicine or disease...", color = White.copy(0.7f), fontSize = 14.sp)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        if (searchQuery.isNotBlank()) {
                                            focusManager.clearFocus()
                                            onSearch(searchQuery)
                                        }
                                    }
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = White,
                                    unfocusedTextColor = White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isHindi) "hi-IN" else "en-US")
                                    }
                                    voiceLauncher.launch(intent)
                                }
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Search", tint = White)
                            }
                            IconButton(
                                onClick = { 
                                    if (searchQuery.isNotBlank()) {
                                        focusManager.clearFocus()
                                        onSearch(searchQuery)
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(PrimaryDarkGreen, CircleShape)
                            ) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Search", tint = White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            // ── Main content with Topo BG ───────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                // Removed Topo Pattern Background to prevent texture too large errors

                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {

                    // Popular Diseases
                    Text(
                        text = "Browse by Disease",
                        style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontSize = 18.sp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 28.dp)
                    ) {
                        items(popularDiseases.size) { i ->
                            DarkDiseaseChip(
                                label = popularDiseases[i].first,
                                emoji = popularDiseases[i].second,
                                onClick = { onDiseaseClick(popularDiseases[i].first) }
                            )
                        }
                    }

                    // Browse section (3D Widgets)
                    Text(
                        text = "Browse",
                        style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontSize = 18.sp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(15.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                    ) {
                        val uniqueCategories = allMedicines.map { it.diseaseCategory }.distinct().filter { it != "Other" && it.isNotBlank() }
                        val chipsList = uniqueCategories.take(3).ifEmpty { listOf("Ayurvedic", "Bhasma", "Ras") }

                        AdvancedWidget(
                            title = { Text("All Medicines", color = White, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)) },
                            subtitle = { Text("Browse A-Z", color = White.copy(0.8f), maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)) },
                            imageRes = com.example.rasaushadhies.R.drawable.pill_3d,
                            bgBrush = PrimaryGradient,
                            overlayTitle = "Quick Filter",
                            overlayContent = {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    chipsList.take(2).forEach { chip ->
                                        Box(
                                            modifier = Modifier
                                                .background(White.copy(0.1f), RoundedCornerShape(16.dp))
                                                .border(1.dp, White.copy(0.3f), RoundedCornerShape(16.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(chip, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, color = White, fontSize = 10.sp, modifier = Modifier.widthIn(max = 60.dp))
                                        }
                                    }
                                }
                            },
                            onClick = onAllMedicines,
                            modifier = Modifier.weight(1f)
                        )
                        val savedMedicines = allMedicines.filter { it.isBookmarked }
                        val totalSaved = savedMedicines.size
                        AdvancedWidget(
                            title = { 
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                                    Text("Saved", color = White, maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp))
                                    Spacer(Modifier.width(6.dp))
                                    Box(modifier = Modifier.background(White.copy(0.8f), RoundedCornerShape(16.dp)).padding(horizontal = 6.dp, vertical = 4.dp)) {
                                        Text(totalSaved.toString(), color = PrimaryDarkGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            subtitle = { 
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                                    Text("Bookmarked", color = White.copy(0.8f), maxLines = 1, softWrap = false, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp))
                                    Spacer(Modifier.width(6.dp))
                                    Box(modifier = Modifier.background(White.copy(0.8f), RoundedCornerShape(16.dp)).padding(horizontal = 6.dp, vertical = 4.dp)) {
                                        Text(totalSaved.toString(), color = PrimaryDarkGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            imageRes = com.example.rasaushadhies.R.drawable.bookmark_3d,
                            bgBrush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(AccentAmberLight, AccentAmber)),
                            overlayTitle = "Recently Saved",
                            overlayContent = {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val displayedSaved = savedMedicines.take(2)
                                    if (displayedSaved.isEmpty()) {
                                        Text("No saved medicines", color = PrimaryDarkGreen, fontSize = 11.sp)
                                    } else {
                                        displayedSaved.forEach { med ->
                                            val medName = if (isHindi) med.hindiName else med.name
                                            Row(
                                                modifier = Modifier
                                                    .background(White.copy(0.8f), RoundedCornerShape(8.dp))
                                                    .padding(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Image(
                                                    painter = androidx.compose.ui.res.painterResource(id = com.example.rasaushadhies.R.drawable.pill_3d),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(Color.Transparent)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(medName, color = PrimaryDarkGreen, fontSize = 9.sp, lineHeight = 10.sp, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 60.dp))
                                            }
                                        }
                                    }
                                }
                            },
                            onClick = onSaved,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Recently Viewed
                    if (recentMedicines.isNotEmpty()) {
                        Text(
                            text = "Recently Viewed",
                            style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontSize = 18.sp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 32.dp)
                        ) {
                            items(recentMedicines.size) { i ->
                                val med = recentMedicines[i]
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = CardBg),
                                    elevation = CardDefaults.cardElevation(8.dp),
                                    modifier = Modifier.width(160.dp).clickable { onSearch(med.name) }
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Box(
                                                modifier = Modifier.size(36.dp).background(PrimaryGreen.copy(0.1f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("${med.id}", color = TextPrimary, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("${med.id}", style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                                                Text("Views", style = MaterialTheme.typography.labelSmall.copy(color = Muted, fontSize = 9.sp))
                                            }
                                        }
                                        Spacer(Modifier.height(12.dp))
                                        Text(med.name, style = MaterialTheme.typography.titleSmall.copy(color = TextPrimary), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                        Text(med.hindiName, style = MaterialTheme.typography.labelSmall.copy(color = Muted), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                        Spacer(Modifier.height(12.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                            Text("🫙", fontSize = 24.sp)
                                            Spacer(Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Dosage ⭐", style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                                                val rating = (med.id % 5) + 1
                                                Text("${rating}.5 views", style = MaterialTheme.typography.labelSmall.copy(color = Muted, fontSize = 9.sp))
                                            }
                                            Icon(
                                                imageVector = if (med.isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                                                contentDescription = null, 
                                                tint = Color(0xFFD32F2F), 
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Advanced AI Dashboard
                    AdvancedAiDashboard(onClick = onAiSearch)
                    
                    Spacer(Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
fun DarkDiseaseChip(label: String, emoji: String, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryDarkGreen),
        border = BorderStroke(1.dp, White.copy(0.1f)),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Text(label, color = White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
        }
    }
}

@Composable
private fun AdvancedWidget(
    title: @Composable () -> Unit,
    subtitle: @Composable () -> Unit,
    imageRes: Int,
    bgBrush: androidx.compose.ui.graphics.Brush,
    overlayTitle: String,
    overlayContent: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = modifier.fillMaxWidth().height(180.dp).clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxSize().background(bgBrush)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = imageRes),
                    contentDescription = null,
                    modifier = Modifier.size(42.dp).clip(CircleShape)
                )
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    title()
                    Spacer(Modifier.height(2.dp))
                    subtitle()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp).padding(bottom = 8.dp)
            ) {
                Column {
                    val tooltipColor = White.copy(0.2f)
                    androidx.compose.foundation.Canvas(modifier = Modifier.padding(start = 24.dp).size(12.dp, 6.dp)) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width / 2f, 0f)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(path, color = tooltipColor)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(White.copy(0.2f), RoundedCornerShape(16.dp))
                            .border(1.dp, White.copy(0.3f), RoundedCornerShape(16.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(overlayTitle, color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Spacer(Modifier.height(4.dp))
                            overlayContent()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancedAiDashboard(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(12.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryDarkGreen),
        border = BorderStroke(1.dp, PrimaryGreen.copy(0.3f)),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Heartbeat & Orb
            Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                // Removed heartbeat wave to prevent potential SurfaceFlinger crashes on emulators
                
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .background(PrimaryDarkGreen, CircleShape)
                        .border(2.dp, White.copy(0.5f), CircleShape)
                ) {
                    Text("AI", color = White, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            Text("Your Ayurvedic Health Companion:", style = MaterialTheme.typography.titleMedium.copy(color = White, fontWeight = FontWeight.Bold))
            Text("Ask me about dosage or interactions", style = MaterialTheme.typography.bodySmall.copy(color = Muted))
            

        }
    }
}
