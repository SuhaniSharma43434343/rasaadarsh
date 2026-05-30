package com.example.rasaushadhies.ui.data

import com.example.rasaushadhies.data.local.Ingredient
import com.example.rasaushadhies.data.local.ClinicalProperties
import com.google.gson.Gson
import com.example.rasaushadhies.data.local.MedicineTypeConverters

/**
 * MedicineRecord is the data model for the underlying medicine data.
 * It is used for parsing the medicines.json asset and database seeding.
 */
data class MedicineRecord(
    val id: Int,
    val name: String,
    val hindiName: String,
    val benefits: String,
    val ingredients: String,
    val dose: String,
    val preparation: String,
    val anupana: String,
    val diseaseCategory: String = "Other",
    
    // Structured JSON Fields for Seeding
    val ingredientsListJson: String = "[]"
)

/**
 * MedicineDatabase previously held a massive hardcoded list of medicines.
 * This data has been migrated to assets/medicines.json to prevent memory crashes (LMK).
 * The MedicineViewModel now handles streaming ingestion of this data into Room.
 */
object MedicineDatabase {
    // Kept as an empty list to avoid breaking references; search now happens in ViewModel/Room
    val ALL: List<MedicineRecord> = emptyList()
    
    fun search(query: String, limit: Int = 10): List<MedicineRecord> {
        // This is now legacy; AI Chatbot and Search Results use the Room database via ViewModel.
        return emptyList()
    }
}
