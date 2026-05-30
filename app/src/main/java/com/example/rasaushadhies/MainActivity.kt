package com.example.rasaushadhies

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.ViewModelProvider
import androidx.activity.compose.LocalActivityResultRegistryOwner

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Dismiss the mandatory Android 12+ system splash instantly,
        // handing off immediately to the custom Splashscreen.kt
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Request runtime permissions for voice features
        val requestPermissionLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (!isGranted) {
                android.util.Log.w("MainActivity", "Microphone permission denied. Voice features will be disabled.")
            }
        }

        if (androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }

        // Use edge-to-edge for a modern look first, so it doesn't override our hide logic!
        enableEdgeToEdge()

        // Expand into cutout area (notch) to prevent blank background patches when hiding status bars
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // Hide the status bar completely
        val windowInsetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())

        setContent {
            val context = LocalContext.current
            val viewModel: com.example.rasaushadhies.ui.viewmodels.MedicineViewModel? = remember {
                try {
                    val appCtx = context.applicationContext
                    if (appCtx !is MedicineApplication) {
                        android.util.Log.e("MainActivity", "Critical failure: applicationContext is NOT MedicineApplication. Actual: ${appCtx?.javaClass?.name}")
                        null
                    } else {
                        val dao = appCtx.database.medicineDao()
                        val prefs = context.getSharedPreferences("practitioner_prefs", android.content.Context.MODE_PRIVATE)
                        val factory = com.example.rasaushadhies.ui.viewmodels.MedicineViewModelFactory(dao, prefs, appCtx)
                        ViewModelProvider(this@MainActivity, factory)
                            .get(com.example.rasaushadhies.ui.viewmodels.MedicineViewModel::class.java)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Critical ViewModel initialization failure: ${e.message}", e)
                    null
                }
            }

            if (viewModel != null) {
                val isHindi by viewModel.isHindi.collectAsState()
                val localizedContext = remember(isHindi) {
                    viewModel.getLocalizedContext(context, isHindi)
                }
                
                CompositionLocalProvider(
                    LocalContext provides localizedContext,
                    LocalActivityResultRegistryOwner provides this@MainActivity
                ) {
                    RasaadarshApp(viewModel)
                }
            } else {
                // Fail-safe view
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text("Initialization Error. Please restart the app.")
                }
            }
        }
    }
}