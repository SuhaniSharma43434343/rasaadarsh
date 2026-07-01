package com.example.rasaushadhies

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
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
    const val LOGIN    = "login"
    const val VERIFY_STATUS = "verify_status"
    const val ADMIN_DASHBOARD = "admin_dashboard"
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RasaadarshApp(viewModel: MedicineViewModel) {
    val navController = rememberNavController()
    val allMedicines by viewModel.medicines.collectAsState()
    val isHindi by viewModel.isHindi.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current

    RasaushadhiTheme {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize().imePadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            SharedTransitionLayout {
                NavHost(
                    navController = navController,
                    startDestination = Routes.SPLASH,
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeIn(tween(300))
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { -it / 3 },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeOut(tween(300))
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { -it / 3 },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeIn(tween(300))
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeOut(tween(300))
                    }
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
                            onProfile      = {
                                val user = viewModel.currentUser.value
                                val prof = viewModel.profile.value
                                if (user == null && !prof.isAdmin) {
                                    navController.navigate(Routes.LOGIN)
                                } else if (prof.isAdmin) {
                                    navController.navigate(Routes.ADMIN_DASHBOARD)
                                } else {
                                    navController.navigate(Routes.PROFILE)
                                }
                            }
                        )
                    }

                    composable(Routes.SEARCH) {
                        AiSearchScreen(
                            onBack   = { navController.popBackStack() },
                            onSearch = { q -> navController.navigate("results/${Uri.encode(q)}") }
                        )
                    }

                    composable(Routes.CHAT) {
                        val user = currentUser
                        val prof = profile
                        if (user == null && !prof.isAdmin) {
                            LaunchedEffect(Unit) {
                                navController.navigate(Routes.LOGIN) {
                                    popUpTo(Routes.HOME)
                                }
                            }
                            Box(modifier = Modifier.fillMaxSize())
                        } else if (user != null && !prof.isAdmin) {
                            if (!prof.isSetupComplete) {
                                LaunchedEffect(Unit) {
                                    navController.navigate(Routes.PROFILE) {
                                        popUpTo(Routes.HOME)
                                    }
                                }
                                Box(modifier = Modifier.fillMaxSize())
                            } else if (prof.degreeVerificationStatus != "APPROVED" || prof.registrationVerificationStatus != "APPROVED") {
                                LaunchedEffect(Unit) {
                                    navController.navigate(Routes.VERIFY_STATUS) {
                                        popUpTo(Routes.HOME)
                                    }
                                }
                                Box(modifier = Modifier.fillMaxSize())
                            } else {
                                AiChatbotScreen(
                                    allMedicines = allMedicines,
                                    onBack = { navController.popBackStack() },
                                    onToggleBookmarkByName = { name -> viewModel.toggleBookmarkByName(name) },
                                    onMedicineClick = { id -> navController.navigate("detail/$id") }
                                )
                            }
                        } else {
                            // Admin is allowed
                            AiChatbotScreen(
                                allMedicines = allMedicines,
                                onBack = { navController.popBackStack() },
                                onToggleBookmarkByName = { name -> viewModel.toggleBookmarkByName(name) },
                                onMedicineClick = { id -> navController.navigate("detail/$id") }
                            )
                        }
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

                        val user = currentUser
                        val prof = profile
                        if (user == null && !prof.isAdmin) {
                            LaunchedEffect(Unit) {
                                navController.navigate(Routes.LOGIN) {
                                    popUpTo(Routes.HOME)
                                }
                            }
                            Box(modifier = Modifier.fillMaxSize())
                        } else if (user != null && !prof.isAdmin) {
                            if (!prof.isSetupComplete) {
                                LaunchedEffect(Unit) {
                                    navController.navigate(Routes.PROFILE) {
                                        popUpTo(Routes.HOME)
                                    }
                                }
                                Box(modifier = Modifier.fillMaxSize())
                            } else if (prof.degreeVerificationStatus != "APPROVED" || prof.registrationVerificationStatus != "APPROVED") {
                                LaunchedEffect(Unit) {
                                    navController.navigate(Routes.VERIFY_STATUS) {
                                        popUpTo(Routes.HOME)
                                    }
                                }
                                Box(modifier = Modifier.fillMaxSize())
                            } else {
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
                                                android.util.Log.e("Navigation", "Shared error", e)
                                            }
                                        },
                                        onToggleBookmark = { viewModel.toggleBookmark(medicine.id) },
                                        onUpdateNotes = { mid, text -> viewModel.updateClinicalNotes(mid, text) }
                                    )
                                } else {
                                    LaunchedEffect(Unit) { navController.popBackStack() }
                                }
                            }
                        } else {
                            // Admin is allowed
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
                                            android.util.Log.e("Navigation", "Shared error", e)
                                        }
                                    },
                                    onToggleBookmark = { viewModel.toggleBookmark(medicine.id) },
                                    onUpdateNotes = { mid, text -> viewModel.updateClinicalNotes(mid, text) }
                                )
                            } else {
                                LaunchedEffect(Unit) { navController.popBackStack() }
                            }
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
                            },
                            onUploadDegreeCertificate = { uri ->
                                viewModel.uploadCertificate(
                                    uri = uri,
                                    certificateType = "degree",
                                    onSuccess = {
                                        Toast.makeText(context, "Degree certificate uploaded successfully!", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = { e ->
                                        Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            onUploadRegistrationCertificate = { uri ->
                                viewModel.uploadCertificate(
                                    uri = uri,
                                    certificateType = "registration",
                                    onSuccess = {
                                        Toast.makeText(context, "Registration certificate uploaded successfully!", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = { e ->
                                        Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            onLogout = {
                                viewModel.signOut(
                                    onSuccess = {
                                        navController.navigate(Routes.HOME) {
                                            popUpTo(Routes.PROFILE) { inclusive = true }
                                        }
                                    }
                                )
                            }
                        )
                    }

                    composable(Routes.LOGIN) {
                        LaunchedEffect(currentUser, profile) {
                            if (profile.isAdmin) {
                                // Admin always goes to the admin dashboard
                                navController.navigate(Routes.ADMIN_DASHBOARD) {
                                    popUpTo(Routes.LOGIN) { inclusive = true }
                                }
                            } else if (currentUser != null) {
                                if (!profile.isSetupComplete) {
                                    navController.navigate(Routes.PROFILE) {
                                        popUpTo(Routes.LOGIN) { inclusive = true }
                                    }
                                } else if (profile.degreeVerificationStatus != "APPROVED" || profile.registrationVerificationStatus != "APPROVED") {
                                    navController.navigate(Routes.VERIFY_STATUS) {
                                        popUpTo(Routes.LOGIN) { inclusive = true }
                                    }
                                } else {
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.LOGIN) { inclusive = true }
                                    }
                                }
                            }
                        }

                        val googleSignInLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartActivityForResult()
                        ) { result ->
                            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
                            try {
                                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                                val idToken = account.idToken
                                if (idToken != null) {
                                    viewModel.signInWithGoogle(
                                        idToken = idToken,
                                        onSuccess = {
                                            Toast.makeText(context, "Signed in with Google successfully!", Toast.LENGTH_SHORT).show()
                                        },
                                        onFailure = { e ->
                                            Toast.makeText(context, "Authentication failed: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                } else {
                                    Toast.makeText(context, "Sign-In failed: Could not retrieve ID token.", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: com.google.android.gms.common.api.ApiException) {
                                val errorMsg = when (e.statusCode) {
                                    10 -> "Sign-In Error: SHA-1 fingerprint not registered in Firebase. Please add your debug SHA-1 in Firebase Console."
                                    12500 -> "Sign-In Error: Google Play Services update required."
                                    12501 -> "Sign-In cancelled by user."
                                    7 -> "Sign-In Error: Network unavailable. Please check your connection."
                                    else -> "Sign-In failed (code ${e.statusCode}): ${e.message}"
                                }
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                android.util.Log.e("Navigation", "Google Sign-In ApiException: code=${e.statusCode}, msg=${e.message}")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Sign-In Exception: ${e.message}", Toast.LENGTH_LONG).show()
                                android.util.Log.e("Navigation", "Google Sign-In Exception", e)
                            }
                        }

                        LoginScreen(
                            onGoogleSignInClick = {
                                val webClientId = context.getString(com.example.rasaushadhies.R.string.default_web_client_id)
                                val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestIdToken(webClientId)
                                    .requestEmail()
                                    .build()
                                val signInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                                // Force Google account chooser dialog to show up by signing out first
                                signInClient.signOut().addOnCompleteListener {
                                    googleSignInLauncher.launch(signInClient.signInIntent)
                                }
                            },
                            onAdminLoginClick = { username, password ->
                                viewModel.loginAsAdmin(username, password)
                            }
                        )
                    }

                    composable(Routes.VERIFY_STATUS) {
                        LaunchedEffect(profile) {
                            if (profile.degreeVerificationStatus == "APPROVED" && profile.registrationVerificationStatus == "APPROVED") {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.VERIFY_STATUS) { inclusive = true }
                                }
                            }
                        }

                        VerificationStatusScreen(
                            profile = profile,
                            onNavigateToProfile = {
                                navController.navigate(Routes.PROFILE)
                            },
                            onLogout = {
                                viewModel.signOut(
                                    onSuccess = {
                                        navController.navigate(Routes.HOME) {
                                            popUpTo(Routes.VERIFY_STATUS) { inclusive = true }
                                        }
                                    }
                                )
                            }
                        )
                    }

                    composable(Routes.ADMIN_DASHBOARD) {
                        val pendingUsers by viewModel.pendingUsers.collectAsState()
                        AdminDashboardScreen(
                            pendingUsers = pendingUsers,
                            onApproveUser = { userId, certType ->
                                viewModel.approveCertificate(userId, certType)
                            },
                            onRejectUser = { userId, certType ->
                                viewModel.rejectCertificate(userId, certType)
                            },
                            onRefresh = {
                                viewModel.listenToPendingUsers()
                            },
                            onLogout = {
                                viewModel.signOut(
                                    onSuccess = {
                                        navController.navigate(Routes.HOME) {
                                            popUpTo(Routes.ADMIN_DASHBOARD) { inclusive = true }
                                        }
                                    }
                                )
                            },
                            onNavigateToMedicines = {
                                navController.navigate(Routes.LIST)
                            },
                            onNavigateToChatbot = {
                                navController.navigate(Routes.CHAT)
                            }
                        )
                    }
                }
            }
        }
    }
}

