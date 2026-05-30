package com.example.rasaushadhies

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
fun RasaadarshApp(viewModel: MedicineViewModel = viewModel()) {
    val navController = rememberNavController()
    val allMedicines by viewModel.medicines.collectAsState()
    val isHindi by viewModel.isHindi.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val context = LocalContext.current

    RasaushadhiTheme {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.SPLASH,
                enterTransition = { slideInHorizontally(animationSpec = tween(300)) { fullWidth -> fullWidth / 4 } + fadeIn(animationSpec = tween(300)) },
                exitTransition = { fadeOut(animationSpec = tween(300)) },
                popEnterTransition = { fadeIn(animationSpec = tween(300)) },
                popExitTransition = { slideOutHorizontally(animationSpec = tween(300)) { fullWidth -> fullWidth / 4 } + fadeOut(animationSpec = tween(300)) }
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
                                    val shareText = if (isHindi) {
                                        """
                                            🌿 ${medicine.hindiName} (${medicine.name})
                                            
                                            📌 लाभ (Benefits):
                                            ${medicine.benefits}
                                            
                                            🧪 घटक (Composition):
                                            ${medicine.ingredients}
                                            
                                            💊 मात्रा एवं अनुपान (Dose):
                                            ${medicine.dosage}
                                            अनुपान: ${medicine.anupana}
                                            
                                            📋 निर्माण विधि (Preparation):
                                            ${medicine.preparation}
                                            
                                            ⚠️ अस्वीकरण: शास्त्रीय ग्रंथों से संकलित सामग्री। केवल चिकित्सक की देखरेख में उपयोग करें।
                                            
                                            RASAADARSH App से साझा किया गया
                                        """.trimIndent()
                                    } else {
                                        """
                                            🌿 ${medicine.name} (${medicine.hindiName})
                                            
                                            📌 BENEFITS:
                                            ${medicine.benefits}
                                            
                                            🧪 COMPOSITION:
                                            ${medicine.ingredients}
                                            
                                            💊 DOSE & ANUPANA:
                                            ${medicine.dosage}
                                            Vehicle: ${medicine.anupana}
                                            
                                            📋 PREPARATION:
                                            ${medicine.preparation}
                                            
                                            

                                            ⚠️ Disclaimer: Classical Ayurvedic formulation. Must only be used under supervision of a qualified Physician.
                                            
                                            Shared from RASAADARSH App
                                        """.trimIndent()
                                    }
                                    
                                    val finalShareText = if (profile.isSetupComplete) {
                                        val signature = if (isHindi) {
                                            "\n\nप्रसारितकर्ता:\nडॉ. ${profile.name} (${profile.qualification})\n${profile.clinicName}\nRASAADARSH ऐप से साझा किया गया"
                                        } else {
                                            "\n\nShared by:\nDr. ${profile.name} (${profile.qualification})\n${profile.clinicName}\nShared from RASAADARSH App"
                                        }
                                        shareText.substringBefore("Shared from RASAADARSH App") + signature
                                    } else {
                                        shareText
                                    }

                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "RASAADARSH: ${if (isHindi) medicine.hindiName else medicine.name}")
                                        putExtra(Intent.EXTRA_TEXT, finalShareText)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    }

                                    val chooser = Intent.createChooser(shareIntent, "Share Medicine").apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    }

                                    context.startActivity(chooser)

                                } catch (e: Exception) {
                                    e.printStackTrace()
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