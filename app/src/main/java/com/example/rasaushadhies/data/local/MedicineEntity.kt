package com.example.rasaushadhies.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey
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
    val lastViewedTimestamp: Long = 0L,
    
    // Modern Structured Fields (Stored via TypeConverters as JSON)
    val ingredientsList: List<Ingredient> = emptyList(),
    
    // Exact mapping for the 25 diseases
    val diseaseCategory: String = "Other"
)
