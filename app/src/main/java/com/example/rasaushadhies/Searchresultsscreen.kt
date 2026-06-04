package com.example.rasaushadhies.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.example.rasaushadhies.ui.components.RasaTopBar
import com.example.rasaushadhies.ui.theme.*
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle

@Composable
fun highlightText(text: String, query: String): androidx.compose.ui.text.AnnotatedString {
    if (query.isBlank()) return buildAnnotatedString { append(text) }
    
    val startIndex = text.indexOf(query, ignoreCase = true)
    if (startIndex == -1) return buildAnnotatedString { append(text) }
    
    return buildAnnotatedString {
        append(text.substring(0, startIndex))
        withStyle(style = SpanStyle(background = AccentAmber.copy(alpha = 0.4f))) {
            append(text.substring(startIndex, startIndex + query.length))
        }
        append(text.substring(startIndex + query.length))
    }
}

// ─── Data model ───────────────────────────────────────────────
@androidx.compose.runtime.Immutable
data class Medicine(
    val id: Int,
    val name: String,
    val hindiName: String,
    val benefits: String,
    val ingredients: String,
    val preparation: String,
    val dosage: String,
    val anupana: String = "",
    val reference: String = "",
    val shloka: String = "",
    val isBookmarked: Boolean = false,
    val clinicalNotes: String = "",
    
    // Structured Clinical Data
    val ingredientsList: List<com.example.rasaushadhies.data.local.Ingredient> = emptyList(),
    val diseaseCategory: String = "Other"
)

enum class FilterCategory { DOSHA, INGREDIENT, DISEASE }

data class MedicineFilter(val label: String, val keyword: String, val category: FilterCategory)

val PREDEFINED_FILTERS = listOf(
    MedicineFilter("Vata", "Vata", FilterCategory.DOSHA),
    MedicineFilter("Pitta", "Pitta", FilterCategory.DOSHA),
    MedicineFilter("Kapha", "Kapha", FilterCategory.DOSHA),
    MedicineFilter("Vatsanabha", "Vatsanabha", FilterCategory.INGREDIENT),
    MedicineFilter("Hingula", "Hingula", FilterCategory.INGREDIENT),
    MedicineFilter("Tamra", "Tamra", FilterCategory.INGREDIENT),
    MedicineFilter("Loha", "Loha", FilterCategory.INGREDIENT),
    MedicineFilter("Abhraka", "Abhraka", FilterCategory.INGREDIENT),
    MedicineFilter("Asthma", "Asthma", FilterCategory.DISEASE),
    MedicineFilter("Fever", "Fever", FilterCategory.DISEASE),
    MedicineFilter("Arthritis", "Arthritis", FilterCategory.DISEASE)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipRow(
    filters: List<MedicineFilter>,
    selectedFilters: Set<MedicineFilter>,
    onFilterToggle: (MedicineFilter) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            val isSelected = selectedFilters.contains(filter)
            FilterChip(
                selected = isSelected,
                onClick = { onFilterToggle(filter) },
                label = { Text(filter.label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),
                    selectedLabelColor = PrimaryDarkGreen
                )
            )
        }
    }
}

@Composable
fun SearchResultsScreen(
    isHindi: Boolean,
    onLanguageToggle: () -> Unit,
    query: String,
    results: List<Medicine>,
    onBack: () -> Unit,
    onMedicineClick: (Int) -> Unit,
    onToggleBookmark: (Int) -> Unit
) {
    var selectedFilters by remember { mutableStateOf(setOf<MedicineFilter>()) }
    var visible by remember { mutableStateOf(false) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(Unit) {
        visible = true
    }

    val displayedResults = remember(results, selectedFilters) {
        if (selectedFilters.isEmpty()) results else {
            results.filter { m ->
                selectedFilters.all { filter ->
                    val k = filter.keyword
                    when (filter.category) {
                        FilterCategory.DOSHA -> com.example.rasaushadhies.util.MedicineSearchUtils.isDoshaMatch(m, k)
                        FilterCategory.INGREDIENT -> com.example.rasaushadhies.util.MedicineSearchUtils.isIngredientMatch(m, k)
                        FilterCategory.DISEASE -> m.name.contains(k, true) || m.benefits.contains(k, true)
                    }
                }
            }
        }
    }

    com.example.rasaushadhies.ui.theme.AppBackground(
        screenType = com.example.rasaushadhies.ui.theme.ScreenBackground.SEARCH_RESULTS
    ) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            RasaTopBar(
                title = androidx.compose.ui.res.stringResource(id = com.example.rasaushadhies.R.string.results_title),
                subtitle = "${displayedResults.size} medicines found",
                onBack = onBack,
                isHindi = isHindi,
                onLanguageToggle = onLanguageToggle
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(PrimaryGradient))

            val availableFilters = remember(results) {
                PREDEFINED_FILTERS.filter { filter ->
                    results.any { m ->
                        val k = filter.keyword
                        when (filter.category) {
                            FilterCategory.DOSHA -> com.example.rasaushadhies.util.MedicineSearchUtils.isDoshaMatch(m, k)
                            FilterCategory.INGREDIENT -> com.example.rasaushadhies.util.MedicineSearchUtils.isIngredientMatch(m, k)
                            FilterCategory.DISEASE -> m.name.contains(k, true) || m.benefits.contains(k, true)
                        }
                    }
                }
            }

            // Filter chips row with right-edge fade gradient hint
            Box(modifier = Modifier.fillMaxWidth()) {
                FilterChipRow(
                    filters = availableFilters,
                    selectedFilters = selectedFilters,
                    onFilterToggle = { filter ->
                        selectedFilters = if (selectedFilters.contains(filter)) selectedFilters - filter else selectedFilters + filter
                    }
                )
                // Right-edge fade to hint scrollability
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(40.dp)
                        .height(56.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, BackgroundColor)
                            )
                        )
                )
            }

            HorizontalDivider(color = DividerColor.copy(alpha = 0.5f))

            if (displayedResults.isEmpty()) {
                androidx.compose.animation.AnimatedVisibility(visible = visible, enter = androidx.compose.animation.fadeIn()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize().padding(32.dp)
                    ) {
                        Text("🔍", fontSize = 64.sp)
                        Text("No medicines found", style = MaterialTheme.typography.headlineMedium)
                        Text("Try different keywords or symptoms", style = MaterialTheme.typography.bodyMedium.copy(color = Muted), textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 32.dp)) {
                    itemsIndexed(displayedResults) { index, medicine ->
                        // Staggered entrance: each card delays by index * 40ms
                        var cardVisible by remember { mutableStateOf(false) }
                        LaunchedEffect(medicine.id) {
                            kotlinx.coroutines.delay(index.coerceAtMost(10) * 40L)
                            cardVisible = true
                        }
                        AnimatedVisibility(
                            visible = cardVisible,
                            enter = fadeIn(tween(300)) + slideInVertically(
                                initialOffsetY = { it / 2 },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            )
                        ) {
                            MedicineResultCard(
                                index = index + 1,
                                medicine = medicine,
                                query = query,
                                onClick = { onMedicineClick(medicine.id) },
                                onToggleBookmark = { onToggleBookmark(medicine.id) }
                            )
                        }
                    }
                }
            }
        }
    }
    } // end AppBackground
}

@Composable
fun MedicineResultCard(
    index: Int,
    medicine: Medicine,
    query: String,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Serial Number Badge
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(32.dp).background(PrimaryGreen.copy(0.1f), CircleShape)) {
                    Text(text = "${medicine.id}", color = PrimaryGreen, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold))
                }
                
                Spacer(Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = highlightText(medicine.name, query), style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkGreen))
                    Text(text = medicine.hindiName, style = MaterialTheme.typography.bodySmall.copy(color = Muted))
                    Spacer(Modifier.height(4.dp))
                    Text(text = highlightText(medicine.benefits, query), style = MaterialTheme.typography.labelSmall.copy(color = PrimaryDarkGreen.copy(0.6f), fontWeight = FontWeight.Medium), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Muted.copy(0.4f), modifier = Modifier.size(20.dp))

                IconButton(onClick = onToggleBookmark, modifier = Modifier.size(40.dp)) {
                    // Bookmark pop animation
                    val bookmarkScale by animateFloatAsState(
                        targetValue = if (medicine.isBookmarked) 1.3f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "bookmarkScale"
                    )
                    Icon(
                        imageVector = if (medicine.isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (medicine.isBookmarked) AccentAmber else Muted.copy(0.2f),
                        modifier = Modifier.size(18.dp).scale(bookmarkScale)
                    )
                }
            }
        }
        HorizontalDivider(color = DividerColor.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
    }
}