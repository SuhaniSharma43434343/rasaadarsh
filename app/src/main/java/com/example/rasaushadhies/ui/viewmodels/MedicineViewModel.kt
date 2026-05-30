package com.example.rasaushadhies.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rasaushadhies.ui.screens.Medicine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import android.content.SharedPreferences
import android.app.Application
import com.example.rasaushadhies.data.local.PractitionerProfile
import com.example.rasaushadhies.data.local.MedicineEntity
import com.example.rasaushadhies.data.local.Ingredient
import com.example.rasaushadhies.data.local.ClinicalProperties

class MedicineViewModel(
    private val dao: com.example.rasaushadhies.data.local.MedicineDao,
    private val prefs: SharedPreferences,
    private val application: Application
) : ViewModel() {
    
    private val gson = Gson()
    
    private val _medicines = MutableStateFlow<List<Medicine>>(emptyList())
    val medicines: StateFlow<List<Medicine>> = _medicines.asStateFlow()

    private val _isHindi = MutableStateFlow(false)
    val isHindi: StateFlow<Boolean> = _isHindi.asStateFlow()

    private val _recentMedicines = MutableStateFlow<List<Medicine>>(emptyList())
    val recentMedicines: StateFlow<List<Medicine>> = _recentMedicines.asStateFlow()

    private val _profile = MutableStateFlow(loadProfile())
    val profile: StateFlow<PractitionerProfile> = _profile.asStateFlow()

    fun toggleLanguage() {
        _isHindi.update { !it }
    }

    fun updateProfile(newProfile: PractitionerProfile) {
        _profile.value = newProfile
        saveProfile(newProfile)
    }

    private fun saveProfile(p: PractitionerProfile) {
        prefs.edit().apply {
            putString("p_name", p.name)
            putString("p_qual", p.qualification)
            putString("p_clinic", p.clinicName)
            putString("p_reg", p.registrationNo)
            putBoolean("p_setup", p.isSetupComplete)
            apply()
        }
    }

    private fun loadProfile(): PractitionerProfile {
        return PractitionerProfile(
            name = prefs.getString("p_name", "") ?: "",
            qualification = prefs.getString("p_qual", "") ?: "",
            clinicName = prefs.getString("p_clinic", "") ?: "",
            registrationNo = prefs.getString("p_reg", "") ?: "",
            isSetupComplete = prefs.getBoolean("p_setup", false)
        )
    }

    fun markAsViewed(medicineId: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dao.updateLastViewed(medicineId, System.currentTimeMillis())
            }
        }
    }

    /**
     * Creates a localized context based on the current app language state.
     */
    fun getLocalizedContext(context: android.content.Context, isHindi: Boolean): android.content.Context {
        val locale = if (isHindi) java.util.Locale("hi") else java.util.Locale.ENGLISH
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    init {
        viewModelScope.launch {
            try {
                android.util.Log.d("MedicineViewModel", "Initializing Database Seeding...")
                
                // Check if Db is empty or needs update, and seed on IO thread
                val count = withContext(Dispatchers.IO) { dao.getMedicineCount() }
                android.util.Log.d("MedicineViewModel", "Current record count: $count")

                val firstMed = if (count > 0) withContext(Dispatchers.IO) { dao.getAllMedicinesOnce().firstOrNull() } else null
                
                // Strict parity check for 250 records
                val EXPECTED_COUNT = 250 
                val isCorrupted = firstMed?.ingredients?.contains(", , ,") == true ||
                                 (firstMed?.ingredients?.contains("Ingredient (Sanskrit)") == true && !firstMed.ingredients.contains("|")) ||
                                 (firstMed?.benefits?.contains("No description available") == true) ||
                                 (firstMed?.name?.contains("Anilarir") == true && firstMed?.diseaseCategory != "Aamvata")
                
                val needsReseed = count != EXPECTED_COUNT || isCorrupted

                if (needsReseed) {
                    android.util.Log.i("MedicineViewModel", "Reseeding database... (Count: $count, Expected: $EXPECTED_COUNT)")
                    withContext(Dispatchers.IO) {
                        try {
                            dao.deleteAll()
                            android.util.Log.d("MedicineViewModel", "Database cleared. Starting fresh seeding from assets...")
                            
                            val inputStream = application.assets.open("medicines.json")
                            val reader = JsonReader(inputStream.bufferedReader())
                            
                            reader.beginArray()
                            val batch = mutableListOf<MedicineEntity>()
                            var totalInserted = 0
                            var processedCount = 0
                            
                            while (reader.hasNext()) {
                                try {
                                    val record: com.example.rasaushadhies.ui.data.MedicineRecord = gson.fromJson(reader, com.example.rasaushadhies.ui.data.MedicineRecord::class.java)
                                    processedCount++
                                    
                                    val entity = MedicineEntity(
                                        id = record.id,
                                        name = record.name,
                                        hindiName = record.hindiName, 
                                        benefits = record.benefits,
                                        ingredients = record.ingredients,
                                        preparation = record.preparation,
                                        dosage = record.`dose`, // Note: use backticks if dose is reserved, but usually fine
                                        anupana = record.anupana,
                                        isBookmarked = false,
                                        clinicalNotes = "",
                                        diseaseCategory = record.diseaseCategory,
                                        ingredientsList = try { gson.fromJson(record.ingredientsListJson, object : TypeToken<List<Ingredient>>() {}.type) } catch(e: Exception) { null } ?: emptyList()
                                    )
                                    
                                    batch.add(entity)
                                    
                                    if (batch.size >= 50) {
                                        dao.insertAll(batch)
                                        totalInserted += batch.size
                                        android.util.Log.d("MedicineViewModel", "Seeded batch of ${batch.size}. Total: $totalInserted")
                                        batch.clear()
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("MedicineViewModel", "Failed to parse record at index $processedCount: ${e.message}")
                                    // Skip bad record and continue
                                }
                            }
                            
                            if (batch.isNotEmpty()) {
                                dao.insertAll(batch)
                                totalInserted += batch.size
                            }
                            
                            reader.endArray()
                            reader.close()
                            
                            android.util.Log.i("MedicineViewModel", "Seeding successful: $totalInserted records inserted.")
                        } catch (e: Exception) {
                            android.util.Log.e("MedicineViewModel", "Critical seeding failure: ${e.message}", e)
                        }
                    }
                }
                
                // ONLY AFTER check/seeding is done, we start observing the database
                // This prevents the UI from catching a fleeting empty state during deleteAll()
                launch {
                    dao.getAllMedicines()
                        .map { entities ->
                            entities.map { entity ->
                                Medicine(
                                    id = entity.id,
                                    name = entity.name,
                                    hindiName = entity.hindiName,
                                    benefits = entity.benefits,
                                    ingredients = entity.ingredients,
                                    preparation = entity.preparation,
                                    dosage = entity.dosage,
                                    anupana = entity.anupana,
                                    isBookmarked = entity.isBookmarked,
                                    clinicalNotes = entity.clinicalNotes,
                                    ingredientsList = entity.ingredientsList,
                                    diseaseCategory = entity.diseaseCategory
                                )
                            }
                        }
                        .flowOn(Dispatchers.Default)
                        .collect { _medicines.value = it }
                }

                // Collect recently viewed
                launch {
                    dao.getRecentlyViewed()
                        .map { entities ->
                            entities.map { entity ->
                                Medicine(
                                    id = entity.id,
                                    name = entity.name,
                                    hindiName = entity.hindiName,
                                    benefits = entity.benefits,
                                    ingredients = entity.ingredients,
                                    preparation = entity.preparation,
                                    dosage = entity.dosage,
                                    anupana = entity.anupana,
                                    isBookmarked = entity.isBookmarked,
                                    clinicalNotes = entity.clinicalNotes,
                                    ingredientsList = entity.ingredientsList,
                                    diseaseCategory = entity.diseaseCategory
                                )
                            }
                        }
                        .flowOn(Dispatchers.IO)
                        .collect { mappedRecents ->
                            _recentMedicines.value = mappedRecents
                        }
                }
            } catch (e: Exception) {
                android.util.Log.e("MedicineViewModel", "Initialization crash prevented: ${e.message}", e)
            }
        }
    }

    fun toggleBookmark(medicineId: Int) {
        viewModelScope.launch {
                                         val currentMedicine = _medicines.value.find { it.id == medicineId }
                                         if (currentMedicine != null) {
                                             withContext(Dispatchers.IO) {
                                                 dao.updateBookmarkState(medicineId, !currentMedicine.isBookmarked)
                                             }
                                             // Force an immediate local update to the state list for instant UI reaction
                                             _medicines.update { currentList ->
                                                 currentList.map { 
                                                     if (it.id == medicineId) it.copy(isBookmarked = !it.isBookmarked) 
                                                     else it 
                                                 }
                                             }
                                         }
        }
    }

    fun toggleBookmarkByName(name: String) {
        viewModelScope.launch {
            val currentMedicine = _medicines.value.find { it.name.trim().equals(name.trim(), ignoreCase = true) }
            if (currentMedicine != null) {
                // Determine the new state. If we are explicitly saving from the AI, we ensure it saves.
                // It toggles it from AI currently, but actually AI says 'Saved X to your cabinet', so it should ideally force true.
                // Let's just toggle for now to match old behavior, or force it strictly to 'true'.
                // AI says: "Saved to your Cabinet", so we force save it (true) to prevent accidental unsaves.
                val newState = true
                withContext(Dispatchers.IO) {
                    dao.updateBookmarkState(currentMedicine.id, newState)
                }
                _medicines.update { currentList ->
                    currentList.map { 
                        if (it.id == currentMedicine.id) it.copy(isBookmarked = newState) 
                        else it 
                    }
                }
            }
        }
    }

    fun updateClinicalNotes(medicineId: Int, notes: String) {
        viewModelScope.launch {
            val currentMedicine = _medicines.value.find { it.id == medicineId }
            if (currentMedicine != null) {
                withContext(Dispatchers.IO) {
                    dao.updateClinicalNotes(medicineId, notes)
                }
            }
        }
    }
}

class MedicineViewModelFactory(
    private val dao: com.example.rasaushadhies.data.local.MedicineDao,
    private val prefs: SharedPreferences,
    private val application: Application
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedicineViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MedicineViewModel(dao, prefs, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
