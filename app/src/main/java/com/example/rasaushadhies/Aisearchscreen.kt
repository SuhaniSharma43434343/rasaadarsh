package com.example.rasaushadhies.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.rasaushadhies.ui.components.PrimaryButton
import com.example.rasaushadhies.ui.components.RasaTopBar
import com.example.rasaushadhies.ui.theme.*

// ─────────────────────────────────────────────────────────────
//  SCREEN 3 — AI Search Screen
// ─────────────────────────────────────────────────────────────

private val exampleQueries = listOf(
    "medicine for asthma",
    "पाण्डु रोग की औषधि",
    "weakness + fatigue",
    "skin disease treatment",
    "ज्वर की दवाई",
    "joint pain remedy",
    "श्वास की औषधि"
)

@Composable
fun AiSearchScreen(
    onBack: () -> Unit,
    onSearch: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Loading overlay dialog
    if (isLoading) {
        Dialog(onDismissRequest = {}) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = White)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    CircularProgressIndicator(color = AccentAmber)
                    Spacer(Modifier.height(16.dp))
                    Text("AI is searching…", fontWeight = FontWeight.Medium)
                    Text("Processing your query", fontSize = 12.sp, color = Muted)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            RasaTopBar(title = "AI Search", subtitle = "Natural language query", onBack = onBack)
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(20.dp)
        ) {

            // Illustration
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", fontSize = 56.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Ask about any Rasaushadhi",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "in English, Hindi, or Sanskrit",
                        fontSize = 13.sp,
                        color = Muted,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Input Card ─────────────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                border = BorderStroke(1.5.dp, PrimaryGreen.copy(0.5f)),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = {
                            Text(
                                text = "E.g.  medicine for anemia\n" +
                                        "       पाण्डु रोग की औषधि\n" +
                                        "       weakness + fatigue",
                                fontSize = 14.sp,
                                color = Muted,
                                lineHeight = 22.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor   = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor   = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    )

                    HorizontalDivider(color = DividerColor)
                    Spacer(Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { /* voice input */ }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Muted, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.weight(1f))
                        if (query.isNotEmpty()) {
                            TextButton(onClick = { query = "" }) {
                                Text("Clear", fontSize = 12.sp, color = Muted)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Search Button ──────────────────────────────
            PrimaryButton(
                text = "Search with AI",
                enabled = query.isNotBlank(),
                onClick = {
                    isLoading = true
                    onSearch(query)
                }
            )

            Spacer(Modifier.height(28.dp))

            // ── Example chips ──────────────────────────────
            Text(
                text = "TRY THESE EXAMPLES",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = Muted,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            FlowChips(
                items = exampleQueries,
                onChipClick = { query = it }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowChips(items: List<String>, onChipClick: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { label ->
            SuggestionChip(
                onClick = { onChipClick(label) },
                label = {
                    Text(text = label, fontSize = 13.sp)
                },
                icon = {
                    Text("✦", fontSize = 10.sp, color = AccentAmber)
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = SurfaceVariant
                )
            )
        }
    }
}
