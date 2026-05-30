package com.example.rasaushadhies.data.local

/**
 * Represents the professional identity of the practitioner using the app.
 * This information is used to personalize clinical reports and sharing signatures.
 */
data class PractitionerProfile(
    val name: String = "",
    val qualification: String = "",
    val clinicName: String = "",
    val registrationNo: String = "",
    val isSetupComplete: Boolean = false
)
