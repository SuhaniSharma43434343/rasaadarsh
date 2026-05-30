package com.example.rasaushadhies.util

/**
 * DoseCalculator provides utilities for scaling medicine doses based on standard
 * clinical rules used in Ayurvedic and modern pharmacology integrations.
 */
object DoseCalculator {

    /**
     * Calculates dose based on Age using Young's Rule.
     * Formula: (Age / (Age + 12)) * Adult Dose
     * Recommended for children under 12.
     */
    fun calculateByAge(adultDoseMg: Double, ageYears: Int): Double {
        if (ageYears <= 0) return 0.0
        return if (ageYears >= 18) adultDoseMg
        else (ageYears.toDouble() / (ageYears + 12)) * adultDoseMg
    }

    /**
     * Calculates dose based on Weight using Clark's Rule.
     * Formula: (Weight in kg / 70) * Adult Dose
     * 70kg is the standard reference adult weight.
     */
    fun calculateByWeight(adultDoseMg: Double, weightKg: Double): Double {
        if (weightKg <= 0) return 0.0
        return (weightKg / 70.0) * adultDoseMg
    }

    /**
     * Extracts numerical value from dose string (e.g., "250 mg" -> 250.0)
     */
    fun parseDose(doseString: String): Double {
        val regex = Regex("""(\d+\.?\d*)""")
        return regex.find(doseString)?.groupValues?.get(1)?.toDoubleOrNull() ?: 125.0
    }
}
