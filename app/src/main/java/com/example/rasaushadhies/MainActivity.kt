package com.example.rasaushadhies

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.ViewModelProvider
import com.example.rasaushadhies.ui.viewmodels.MedicineViewModel
import com.example.rasaushadhies.ui.viewmodels.MedicineViewModelFactory

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: MedicineViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge for modern Android UI handling
        enableEdgeToEdge()

        // Handle permissions
        val requestPermissionLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (!isGranted) {
                android.util.Log.w("MainActivity", "Microphone permission denied.")
            }
        }

        /*
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
        */

        // Initialize ViewModel safely outside of Composition
        try {
            val appCtx = applicationContext as? MedicineApplication
            if (appCtx == null) {
                android.util.Log.e("MainActivity", "Application context is not MedicineApplication")
            } else {
                val dao = appCtx.database.medicineDao()
                val prefs = getSharedPreferences("practitioner_prefs", android.content.Context.MODE_PRIVATE)
                val factory = MedicineViewModelFactory(dao, prefs, appCtx)
                viewModel = ViewModelProvider(this, factory)[MedicineViewModel::class.java]
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Critical ViewModel initialization failure: ${e.message}", e)
        }

        setContent {
            if (::viewModel.isInitialized) {
                RasaadarshApp(viewModel)
            } else {
                // Fail-safe view if initialization failed
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text("App Initialization Error. Please restart.")
                }
            }
        }
    }
}
