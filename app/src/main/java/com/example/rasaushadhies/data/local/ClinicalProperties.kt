package com.example.rasaushadhies.data.local

/**
 * Structured Ayurvedic clinical properties (Guna/Karma).
 */
data class ClinicalProperties(
    val dose: String = "",
    val taste: String = "",
    val smell: String = "",
    val color: String = "",
    val virya: String = "",
    val vipaka: String = ""
)
