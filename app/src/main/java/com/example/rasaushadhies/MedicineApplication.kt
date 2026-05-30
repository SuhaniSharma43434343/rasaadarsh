package com.example.rasaushadhies

import android.app.Application
import com.example.rasaushadhies.data.local.AppDatabase

class MedicineApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
}
