package com.example.rasaushadhies.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rasaushadhies.ui.components.LetterAvatar
import com.example.rasaushadhies.ui.components.RasaTopBar
import com.example.rasaushadhies.ui.components.SectionLabel
import com.example.rasaushadhies.ui.theme.*

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  SCREEN 6 â€” Medicine List Screen  (All Medicines Aâ€“Z)
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineListScreen(
    isHindi: Boolean,
    onLanguageToggle: () -> Unit,
    medicines: List<Medicine>,
    onBack: () -> Unit,
    onMedicineClick: (Int) -> Unit
) {
    var filterQuery by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var selectedFilters by remember { mutableStateOf(setOf<MedicineFilter>()) }

    val filtered = remember(filterQuery, selectedFilters, medicines) {
        val baseFiltered = if (filterQuery.isBlank()) medicines
        else medicines.filter {
            com.example.rasaushadhies.util.MedicineSearchUtils.matchesQuery(it, filterQuery)
        }

        if (selectedFilters.isEmpty()) baseFiltered else {
            baseFiltered.filter { m ->
                selectedFilters.all { filter ->
                    val k = filter.keyword
                    when (filter.category) {
                        FilterCategory.DOSHA -> {
                            com.example.rasaushadhies.util.MedicineSearchUtils.isDoshaMatch(m, k)
                        }
                        FilterCategory.INGREDIENT -> com.example.rasaushadhies.util.MedicineSearchUtils.isIngredientMatch(m, k)
                        FilterCategory.DISEASE -> m.name.contains(k, true) || m.benefits.contains(k, true) || m.diseaseCategory.contains(k, true)
                    }
                }
            }
        }
    }

    val expandedSaver = androidx.compose.runtime.saveable.listSaver<androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>, String>(
        save = { it.filter { entry -> entry.value }.keys.toList() },
        restore = { list ->
            val map = mutableStateMapOf<String, Boolean>()
            list.forEach { map[it] = true }
            map
        }
    )
    val expandedDiseases = androidx.compose.runtime.saveable.rememberSaveable(saver = expandedSaver) { mutableStateMapOf<String, Boolean>() }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val grouped = remember(filtered) {
        filtered.groupBy { it.diseaseCategory }
    }

    com.example.rasaushadhies.ui.theme.AppBackground(
        screenType = com.example.rasaushadhies.ui.theme.ScreenBackground.LIST
    ) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                RasaTopBar(
                    title = androidx.compose.ui.res.stringResource(id = com.example.rasaushadhies.R.string.all_medicines),
                    subtitle = "${filtered.size} records",
                    onBack = onBack,
                    isHindi = isHindi,
                    onLanguageToggle = onLanguageToggle
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryDarkGreen)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = White.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = White.copy(0.7f), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            TextField(
                                value = filterQuery,
                                onValueChange = { filterQuery = it },
                                placeholder = { Text("Filter medicines...", color = White.copy(0.5f), fontSize = 14.sp) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor   = androidx.compose.ui.graphics.Color.Transparent,
                                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                    focusedIndicatorColor   = androidx.compose.ui.graphics.Color.Transparent,
                                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                    focusedTextColor        = White,
                                    unfocusedTextColor      = White,
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            FilterChipRow(
                filters = PREDEFINED_FILTERS,
                selectedFilters = selectedFilters,
                onFilterToggle = { filter ->
                    selectedFilters = if (selectedFilters.contains(filter)) {
                        selectedFilters - filter
                    } else {
                        selectedFilters + filter
                    }
                }
            )

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                grouped.forEach { (disease, meds) ->
                    val isExpanded = expandedDiseases[disease] ?: false
                    item(key = disease) {
                        DiseaseGroupHeader(
                            disease = disease,
                            count = meds.size,
                            isExpanded = isExpanded,
                            onClick = { expandedDiseases[disease] = !isExpanded }
                        )
                    }
                    item(key = "content_$disease") {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isExpanded,
                            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                                meds.forEachIndexed { index, medicine ->
                                    MedicineListRow(
                                        index = index + 1,
                                        medicine = medicine,
                                        onClick = { onMedicineClick(medicine.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    } // end AppBackground
}

@Composable
private fun DiseaseGroupHeader(
    disease: String,
    count: Int,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) PrimaryGreen.copy(0.05f) else White
        ),
        border = BorderStroke(1.dp, if (isExpanded) PrimaryGreen.copy(0.2f) else DividerColor.copy(0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = disease,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isExpanded) PrimaryDarkGreen else Color.Black
                    )
                )
                Text(
                    text = "$count medicines",
                    style = MaterialTheme.typography.labelSmall.copy(color = Muted)
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = if (isExpanded) PrimaryGreen else Muted
            )
        }
    }
}

@Composable
private fun MedicineListRow(index: Int, medicine: Medicine, onClick: () -> Unit) {
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
                    .size(42.dp)
                    .background(PrimaryGreen.copy(0.1f), CircleShape)
            ) {
                Text(
                    text = "${medicine.id}",
                    color = PrimaryGreen,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = medicine.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // Caution message/icon removed per user request
                }
                Text(
                    text = medicine.hindiName,
                    fontSize = 12.sp,
                    color = Muted
                )
                
                // Tags Row removed per user request
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  SCREEN 7 â€” Saved Medicines Screen
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
fun SavedScreen(
    isHindi: Boolean,
    onLanguageToggle: () -> Unit,
    saved: List<Medicine> = emptyList(),
    onBack: () -> Unit,
    onMedicineClick: (Int) -> Unit,
    onBrowse: () -> Unit
) {
    com.example.rasaushadhies.ui.theme.AppBackground(
        screenType = com.example.rasaushadhies.ui.theme.ScreenBackground.SAVED
    ) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            RasaTopBar(
                title    = androidx.compose.ui.res.stringResource(id = com.example.rasaushadhies.R.string.saved),
                subtitle = "${saved.size} ${androidx.compose.ui.res.stringResource(id = com.example.rasaushadhies.R.string.bookmarked)}",
                onBack   = onBack,
                isHindi  = isHindi,
                onLanguageToggle = onLanguageToggle
            )
        }
    ) { innerPadding ->

        if (saved.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp)
            ) {
                Text("ðŸ”–", fontSize = 64.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "No saved medicines yet",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tap the bookmark icon on any medicine to save it here",
                    color = Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Button(
                    onClick = onBrowse,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Browse Medicines")
                }
            }
        } else {
            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(innerPadding)
            ) {
                item {
                    Text(
                        text = "YOUR SAVED LIST",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = PrimaryGreen,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                itemsIndexed(saved) { index, medicine ->
                    MedicineListRow(
                        index    = index + 1,
                        medicine = medicine,
                        onClick  = { onMedicineClick(medicine.id) }
                    )
                }
            }
        }
    }
    } // end AppBackground
}
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  SCREEN 8 â€” About Screen
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),  // â† fixes white gap
        topBar = {
            RasaTopBar(title = "About", onBack = onBack)
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
        ) {

            // â”€â”€ Header â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(PrimaryDarkGreen, androidx.compose.ui.graphics.Color(0xFF3A6B4A))
                        )
                    )
                    .padding(vertical = 36.dp, horizontal = 24.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(White.copy(0.15f))
                ) {
                    Text("à¥", fontSize = 38.sp, color = AccentAmber)
                }
                Spacer(Modifier.height(16.dp))
                Text("RASAADARSH", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = White)
                Spacer(Modifier.height(4.dp))
                Text("Version 1.0.0", fontSize = 12.sp, color = White.copy(0.5f))
            }

            // â”€â”€ Cards â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Column(modifier = Modifier.padding(20.dp)) {

                AboutCard(title = "ABOUT THE APP") {
                    Text(
                        text = "Knowledge Base App for Conserving and Disseminating Information of Rasaushadhies (Ayurvedic Mineral-based Medicines).\n\n" +
                                "This application allow healthcare professionals to search classical Rasashastra texts by symptom, disease, or medicine name in English, Hindi, or Sanskrit.",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }

                Spacer(Modifier.height(14.dp))

                AboutCard(
                    title = "DEVELOPED FOR",
                    bgColor = SurfaceVariant
                ) {
                    Text(
                        "Parul Institute of Ayurved and Research",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Parul University, Vadodara, Gujarat", color = Muted, fontSize = 13.sp)
                }

                Spacer(Modifier.height(14.dp))

                AboutCard(title = "TECHNOLOGY") {
                    TechRow("Platform",      "Android (Java / Kotlin)")
                    TechRow("Backend",       "Smart Search Engine")
                    TechRow("Local Storage", "Room Database")
                    TechRow("UI",            "Jetpack Compose")
                    TechRow("Min SDK",       "API 24 (Android 7.0)")
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Made with â™¥ for Ayurvedic medicine conservation",
                    fontSize = 12.sp,
                    color = Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Â© 2024  Â·  Parul University",
                    fontSize = 11.sp,
                    color = Muted.copy(0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun AboutCard(
    title: String,
    bgColor: androidx.compose.ui.graphics.Color = CardBg,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(if (bgColor == CardBg) 1.dp else 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionLabel(text = title, modifier = Modifier.padding(bottom = 10.dp))
            content()
        }
    }
}

@Composable
private fun TechRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(label, fontSize = 13.sp, color = Muted, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
