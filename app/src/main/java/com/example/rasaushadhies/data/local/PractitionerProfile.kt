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
    val isSetupComplete: Boolean = false,
    val degreeCertificateUri: String? = null,
    val registrationCertificateUri: String? = null,
    val degreeVerificationStatus: String = "NONE", // NONE, PENDING, APPROVED, REJECTED
    val registrationVerificationStatus: String = "NONE", // NONE, PENDING, APPROVED, REJECTED
    val isAdmin: Boolean = false
)
