package com.example.rasaushadhies.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.rasaushadhies.ui.components.RasaTopBar
import com.example.rasaushadhies.ui.components.SectionLabel
import com.example.rasaushadhies.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    Scaffold(
        containerColor = SurfaceColor,
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
        ) {
            var fontSizeMultiplier by remember { mutableFloatStateOf(1f) }

            // ── Hero header with PrimaryGradient ────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryGradient)
                    .padding(20.dp)
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = White.copy(alpha = 0.2f)
                        ) {
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
                        style = MaterialTheme.typography.bodyLarge.copy(color = White.copy(alpha = 0.8f), fontWeight = FontWeight.Normal)
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
                    .offset(y = (-20).dp)
                    .background(BackgroundColor, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
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
                    SectionLabel(text = "Classical Reference (Shloka)", modifier = Modifier.padding(bottom = 12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, PrimaryGreen.copy(0.2f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = medicine.shloka,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = PrimaryDarkGreen,
                                    lineHeight = 22.sp,
                                    fontSize = 15.sp * fontSizeMultiplier
                                )
                            )
                        }
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
            Box(modifier = Modifier.background(PrimaryGreen.copy(0.1f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(item.quantity, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp * fontSizeMultiplier), color = PrimaryGreen)
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
            Box(
                modifier = Modifier.fillMaxSize().background(if (medicine.isBookmarked) androidx.compose.ui.graphics.Brush.linearGradient(listOf(AccentAmber, AccentAmberLight)) else PrimaryGradient),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = if (medicine.isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder, contentDescription = null, modifier = Modifier.size(20.dp))
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
