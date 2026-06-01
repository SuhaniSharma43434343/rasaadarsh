package com.example.rasaushadhies

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.rasaushadhies.ui.screens.*
import com.example.rasaushadhies.ui.theme.RasaushadhiTheme
import com.example.rasaushadhies.ui.viewmodels.MedicineViewModel

object Routes {
    const val SPLASH  = "splash"
    const val HOME    = "home"
    const val SEARCH  = "ai_search"
    const val CHAT    = "ai_chatbot"
    const val RESULTS = "results/{query}"
    const val DETAIL  = "detail/{medicineId}"
    const val LIST    = "list"
    const val SAVED   = "saved"
    const val ABOUT   = "about"
    const val PROFILE = "profile"
    const val EXPIRED = "expired"
    const val TAMPERED = "tampered"
}

@Composable
fun RasaadarshApp(viewModel: MedicineViewModel) {
    val navController = rememberNavController()
    val allMedicines by viewModel.medicines.collectAsState()
    val isHindi by viewModel.isHindi.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val context = LocalContext.current

    RasaushadhiTheme {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.SPLASH
            ) {

                composable(Routes.SPLASH) {
                    SplashScreen(
                        onSecurityResult = { destination ->
                            navController.navigate(destination) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.EXPIRED) {
                    AccessExpiredScreen()
                }

                composable(Routes.TAMPERED) {
                    SecurityTamperScreen()
                }

                composable(Routes.HOME) {
                    val recentMedicines by viewModel.recentMedicines.collectAsState()
                    HomeScreen(
                        isHindi        = isHindi,
                        allMedicines   = allMedicines,
                        recentMedicines = recentMedicines,
                        onLanguageToggle = { viewModel.toggleLanguage() },
                        onSearch       = { q -> navController.navigate("results/${Uri.encode(q)}") },
                        onDiseaseClick = { d -> navController.navigate("results/${Uri.encode(d)}") },
                        onAllMedicines = { navController.navigate(Routes.LIST) },
                        onSaved        = { navController.navigate(Routes.SAVED) },
                        onAiSearch     = { navController.navigate(Routes.CHAT) },
                        onAbout        = { navController.navigate(Routes.ABOUT) },
                        onProfile      = { navController.navigate(Routes.PROFILE) }
                    )
                }

                composable(Routes.SEARCH) {
                    AiSearchScreen(
                        onBack   = { navController.popBackStack() },
                        onSearch = { q -> navController.navigate("results/${Uri.encode(q)}") }
                    )
                }

                composable(Routes.CHAT) {
                    AiChatbotScreen(
                        allMedicines = allMedicines,
                        onBack = { navController.popBackStack() },
                        onToggleBookmarkByName = { name -> viewModel.toggleBookmarkByName(name) }
                    )
                }

                composable(
                    route = Routes.RESULTS,
                    arguments = listOf(navArgument("query") { type = NavType.StringType })
                ) { backStackEntry ->
                    val query = backStackEntry.arguments?.getString("query") ?: ""

                    val filteredResults = remember(query, allMedicines) {
                        allMedicines.filter { m ->
                            com.example.rasaushadhies.util.MedicineSearchUtils.matchesQuery(m, query)
                        }
                    }

                    SearchResultsScreen(
                        isHindi         = isHindi,
                        onLanguageToggle = { viewModel.toggleLanguage() },
                        query           = query,
                        results         = filteredResults,
                        onBack          = { navController.popBackStack() },
                        onMedicineClick = { id -> navController.navigate("detail/$id") },
                        onToggleBookmark = { id -> viewModel.toggleBookmark(id) }
                    )
                }

                composable(
                    route = Routes.DETAIL,
                    arguments = listOf(navArgument("medicineId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getInt("medicineId") ?: -1
                    val medicine = allMedicines.find { it.id == id }

                    if (medicine != null) {
                        LaunchedEffect(id) {
                            viewModel.markAsViewed(id)
                        }

                        MedicineDetailScreen(
                            isHindi  = isHindi,
                            onLanguageToggle = { viewModel.toggleLanguage() },
                            medicine = medicine,
                            onBack   = { navController.popBackStack() },
                            onShare  = {
                                try {
                                    val medName = if (isHindi) medicine.hindiName else medicine.name
                                    val shareContent = buildString {
                                        append("🌿 $medName\n\n")
                                        
                                        if (medicine.shloka.isNotBlank()) {
                                            append(if (isHindi) "📖 शास्त्रीय श्लोक:\n" else "📖 CLASSICAL SHLOKA:\n")
                                            append(medicine.shloka)
                                            append("\n\n")
                                        }

                                        append(if (isHindi) "📌 लाभ:\n" else "📌 BENEFITS:\n")
                                        append(medicine.benefits)
                                        append("\n\n")
                                        append(if (isHindi) "🧪 घटक:\n" else "🧪 COMPOSITION:\n")
                                        append(medicine.ingredients)
                                        append("\n\n")
                                        append(if (isHindi) "💊 मात्रा एवं अनुपान:\n" else "💊 DOSE & ANUPANA:\n")
                                        append("${medicine.dosage}\n")
                                        append(if (isHindi) "अनुपान: ${medicine.anupana}" else "Vehicle: ${medicine.anupana}")
                                        append("\n\n")
                                        append(if (isHindi) "📋 निर्माण विधि:\n" else "📋 PREPARATION:\n")
                                        append(medicine.preparation)
                                        append("\n\n")

                                        if (profile.isSetupComplete) {
                                            append(if (isHindi) "प्रसारितकर्ता:\n" else "Shared by:\n")
                                            append("Dr. ${profile.name} (${profile.qualification})\n")
                                            append("${profile.clinicName}\n\n")
                                        }
                                        append(if (isHindi) "RASAADARSH ऐप से साझा किया गया" else "Shared from RASAADARSH App")
                                    }

                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "RASAADARSH: $medName")
                                        putExtra(Intent.EXTRA_TEXT, shareContent)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }

                                    context.startActivity(Intent.createChooser(shareIntent, "Share Medicine"))
                                } catch (e: Exception) {
                                    android.util.Log.e("Navigation", "Share error", e)
                                }
                            },
                            onToggleBookmark = { viewModel.toggleBookmark(medicine.id) },
                            onUpdateNotes = { mid, text -> viewModel.updateClinicalNotes(mid, text) }
                        )
                    } else {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    }
                }

                composable(Routes.LIST) {
                    MedicineListScreen(
                        isHindi         = isHindi,
                        onLanguageToggle = { viewModel.toggleLanguage() },
                        medicines       = allMedicines,
                        onBack          = { navController.popBackStack() },
                        onMedicineClick = { id -> navController.navigate("detail/$id") }
                    )
                }

                composable(Routes.SAVED) {
                    SavedScreen(
                        isHindi         = isHindi,
                        onLanguageToggle = { viewModel.toggleLanguage() },
                        saved           = allMedicines.filter { it.isBookmarked },
                        onBack          = { navController.popBackStack() },
                        onMedicineClick = { id -> navController.navigate("detail/$id") },
                        onBrowse        = { navController.navigate(Routes.LIST) }
                    )
                }

                composable(Routes.ABOUT) {
                    AboutScreen(onBack = { navController.popBackStack() })
                }

                composable(Routes.PROFILE) {
                    ProfileScreen(
                        profile = profile,
                        onBack = { navController.popBackStack() },
                        onSave = { updatedProfile ->
                            viewModel.updateProfile(updatedProfile)
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
