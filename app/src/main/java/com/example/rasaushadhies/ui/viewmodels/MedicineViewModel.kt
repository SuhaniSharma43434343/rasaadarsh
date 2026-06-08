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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import android.graphics.BitmapFactory

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

    private val _currentUser = MutableStateFlow<FirebaseUser?>(FirebaseAuth.getInstance().currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _pendingUsers = MutableStateFlow<List<Pair<String, PractitionerProfile>>>(emptyList())
    val pendingUsers: StateFlow<List<Pair<String, PractitionerProfile>>> = _pendingUsers.asStateFlow()

    private var firestoreListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var adminListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                _currentUser.value = authResult.user
                _profile.value = loadProfile(authResult.user?.uid)
                syncProfileWithFirestore()
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun loginAsAdmin(username: String, password: String): Boolean {
        if (username.equals("admin", ignoreCase = true) && password == "admin123") {
            val adminProfile = PractitionerProfile(
                name = "Administrator",
                qualification = "Admin",
                clinicName = "Rasaadarsh HQ",
                registrationNo = "ADMIN-01",
                isSetupComplete = true,
                degreeVerificationStatus = "APPROVED",
                registrationVerificationStatus = "APPROVED",
                isAdmin = true
            )
            _profile.value = adminProfile
            saveProfile(adminProfile)

            // Sign out any active user first so the admin can log in anonymously
            if (FirebaseAuth.getInstance().currentUser != null) {
                FirebaseAuth.getInstance().signOut()
                _currentUser.value = null
            }

            val adminEmail = "admin@rasaadarsh.com"
            val adminPass = "admin123"

            FirebaseAuth.getInstance().signInWithEmailAndPassword(adminEmail, adminPass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val adminUser = task.result?.user
                        _currentUser.value = adminUser
                        saveProfile(adminProfile) // Ensure admin profile is saved locally under admin UID
                        android.util.Log.i("MedicineViewModel", "Admin logged in successfully via email/password: ${adminUser?.uid}")
                        if (adminUser != null) {
                            val docRef = FirebaseFirestore.getInstance().collection("users").document(adminUser.uid)
                            docRef.set(adminProfile, com.google.firebase.firestore.SetOptions.merge())
                                .addOnCompleteListener {
                                    listenToPendingUsers()
                                }
                        } else {
                            listenToPendingUsers()
                        }
                    } else {
                        val exception = task.exception
                        val errorMsg = exception?.message ?: "Unknown error"
                        android.util.Log.e("MedicineViewModel", "Admin login failed: $errorMsg")
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            android.widget.Toast.makeText(
                                application,
                                "Admin Login failed: $errorMsg. Trying fallback...",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                        android.util.Log.i("MedicineViewModel", "Admin account login failed. Attempting to create user account...")
                        FirebaseAuth.getInstance().createUserWithEmailAndPassword(adminEmail, adminPass)
                            .addOnCompleteListener { createCtx ->
                                if (createCtx.isSuccessful) {
                                    val adminUser = createCtx.result?.user
                                    _currentUser.value = adminUser
                                    saveProfile(adminProfile) // Ensure admin profile is saved locally under admin UID
                                    android.util.Log.i("MedicineViewModel", "Admin account created successfully: ${adminUser?.uid}")
                                    if (adminUser != null) {
                                        val docRef = FirebaseFirestore.getInstance().collection("users").document(adminUser.uid)
                                        docRef.set(adminProfile, com.google.firebase.firestore.SetOptions.merge())
                                            .addOnCompleteListener {
                                                listenToPendingUsers()
                                            }
                                    } else {
                                        listenToPendingUsers()
                                    }
                                } else {
                                    val regError = createCtx.exception?.message ?: "Unknown error"
                                    android.util.Log.e("MedicineViewModel", "Admin registration failed: $regError")
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        android.widget.Toast.makeText(
                                            application,
                                            "Admin creation failed: $regError. Trying anonymous fallback...",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    signInAnonymouslyFallback(adminProfile)
                                }
                            }
                    }
                }
            return true
        }
        return false
    }

    private fun signInAnonymouslyFallback(adminProfile: PractitionerProfile) {
        FirebaseAuth.getInstance().signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val adminUser = task.result?.user
                    _currentUser.value = adminUser
                    saveProfile(adminProfile) // Ensure admin profile is saved locally under admin UID
                    android.util.Log.d("MedicineViewModel", "Admin logged in anonymously (fallback): ${adminUser?.uid}")
                    if (adminUser != null) {
                        val docRef = FirebaseFirestore.getInstance().collection("users").document(adminUser.uid)
                        docRef.set(adminProfile, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener {
                                android.util.Log.i("MedicineViewModel", "Admin document created in Firestore successfully (fallback)")
                                listenToPendingUsers()
                            }
                            .addOnFailureListener { e ->
                                android.util.Log.e("MedicineViewModel", "Failed to write admin document in Firestore (fallback): ${e.message}", e)
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    android.widget.Toast.makeText(
                                        application,
                                        "Admin doc write failed: ${e.localizedMessage}",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                                listenToPendingUsers()
                            }
                    } else {
                        listenToPendingUsers()
                    }
                } else {
                    val errorMsg = task.exception?.message ?: "Unknown error"
                    android.util.Log.e("MedicineViewModel", "Admin Firebase sign-in failed (fallback): $errorMsg")
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        android.widget.Toast.makeText(
                            application,
                            "Admin Anonymous Login failed: $errorMsg",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
    }

    fun signOut(onSuccess: () -> Unit) {
        firestoreListener?.remove()
        firestoreListener = null
        stopListeningToPendingUsers()
        FirebaseAuth.getInstance().signOut()
        _currentUser.value = null
        _profile.value = PractitionerProfile()
        saveProfile(PractitionerProfile())
        onSuccess()
    }

    fun syncProfileWithFirestore() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        firestoreListener?.remove()

        val localProfile = loadProfile(user.uid)
        _profile.value = localProfile

        val docRef = FirebaseFirestore.getInstance().collection("users").document(user.uid)
        firestoreListener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("MedicineViewModel", "syncProfileWithFirestore error: ${error.message}", error)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(
                        application,
                        "Profile Sync error: ${error.localizedMessage}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val dbDegreeStatus = snapshot.getString("degreeVerificationStatus") ?: "NONE"
                val dbRegStatus = snapshot.getString("registrationVerificationStatus") ?: "NONE"
                
                val local = loadProfile(user.uid)
                
                // If local status is approved/rejected (set by local admin on same device) but Firestore still says PENDING (admin update permission failure fallback),
                // the user updates Firestore using their own user credentials.
                if ((local.degreeVerificationStatus == "APPROVED" && dbDegreeStatus == "PENDING") ||
                    (local.registrationVerificationStatus == "APPROVED" && dbRegStatus == "PENDING") ||
                    (local.degreeVerificationStatus == "REJECTED" && dbDegreeStatus == "PENDING") ||
                    (local.registrationVerificationStatus == "REJECTED" && dbRegStatus == "PENDING")) {
                    
                    val updates = hashMapOf<String, Any>()
                    if ((local.degreeVerificationStatus == "APPROVED" || local.degreeVerificationStatus == "REJECTED") && dbDegreeStatus == "PENDING") {
                        updates["degreeVerificationStatus"] = local.degreeVerificationStatus
                    }
                    if ((local.registrationVerificationStatus == "APPROVED" || local.registrationVerificationStatus == "REJECTED") && dbRegStatus == "PENDING") {
                        updates["registrationVerificationStatus"] = local.registrationVerificationStatus
                    }
                    docRef.update(updates)
                        .addOnSuccessListener {
                            android.util.Log.i("MedicineViewModel", "Self-healing sync successfully updated Firestore statuses to match local admin actions")
                        }
                }

                val p = PractitionerProfile(
                    name = snapshot.getString("name") ?: "",
                    qualification = snapshot.getString("qualification") ?: "",
                    clinicName = snapshot.getString("clinicName") ?: "",
                    registrationNo = snapshot.getString("registrationNo") ?: "",
                    isSetupComplete = snapshot.getBoolean("isSetupComplete") ?: false,
                    degreeCertificateUri = snapshot.getString("degreeCertificateUri"),
                    registrationCertificateUri = snapshot.getString("registrationCertificateUri"),
                    degreeVerificationStatus = if (local.degreeVerificationStatus == "APPROVED" || local.degreeVerificationStatus == "REJECTED") local.degreeVerificationStatus else dbDegreeStatus,
                    registrationVerificationStatus = if (local.registrationVerificationStatus == "APPROVED" || local.registrationVerificationStatus == "REJECTED") local.registrationVerificationStatus else dbRegStatus,
                    isAdmin = snapshot.getBoolean("isAdmin") ?: false
                )
                _profile.value = p
                saveProfile(p)
            } else if (snapshot != null) {
                val local = loadProfile(user.uid)
                if (local.isSetupComplete) {
                    val data = hashMapOf(
                        "name" to local.name,
                        "qualification" to local.qualification,
                        "clinicName" to local.clinicName,
                        "registrationNo" to local.registrationNo,
                        "isSetupComplete" to local.isSetupComplete,
                        "degreeCertificateUri" to local.degreeCertificateUri,
                        "registrationCertificateUri" to local.registrationCertificateUri,
                        "degreeVerificationStatus" to local.degreeVerificationStatus,
                        "registrationVerificationStatus" to local.registrationVerificationStatus,
                        "isAdmin" to local.isAdmin
                    )
                    docRef.set(data)
                }
            }
        }
    }

    fun uploadCertificate(uri: Uri, certificateType: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        
        // Convert the selected image into a compressed Base64 string so it can be saved in Firestore
        // and viewed on any other device without relying on Firebase Storage buckets.
        val base64String = try {
            val inputStream = application.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (originalBitmap != null) {
                val maxSize = 400
                val width = originalBitmap.width
                val height = originalBitmap.height
                val (newWidth, newHeight) = if (width > height) {
                    if (width > maxSize) {
                        Pair(maxSize, (height * (maxSize.toFloat() / width)).toInt())
                    } else {
                        Pair(width, height)
                    }
                } else {
                    if (height > maxSize) {
                        Pair((width * (maxSize.toFloat() / height)).toInt(), maxSize)
                    } else {
                        Pair(width, height)
                    }
                }
                val resizedBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
                val outputStream = java.io.ByteArrayOutputStream()
                resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 40, outputStream)
                val bytes = outputStream.toByteArray()
                android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
            } else {
                null
            }
        } catch (e: Exception) {
            onFailure(e)
            return
        }

        if (base64String == null) {
            onFailure(Exception("Failed to process certificate image"))
            return
        }

        val docRef = FirebaseFirestore.getInstance().collection("users").document(user.uid)
        val fieldUri = if (certificateType == "degree") "degreeCertificateUri" else "registrationCertificateUri"
        val fieldStatus = if (certificateType == "degree") "degreeVerificationStatus" else "registrationVerificationStatus"

        val currentProfile = _profile.value
        val updates = hashMapOf<String, Any>(
            fieldUri to base64String,
            fieldStatus to "PENDING",
            "name" to currentProfile.name,
            "qualification" to currentProfile.qualification,
            "clinicName" to currentProfile.clinicName,
            "registrationNo" to currentProfile.registrationNo,
            "isSetupComplete" to currentProfile.isSetupComplete,
            "isAdmin" to currentProfile.isAdmin
        )
        // Include the other cert URI/status if they exist
        if (certificateType == "degree") {
            currentProfile.registrationCertificateUri?.let { updates["registrationCertificateUri"] = it }
            updates["registrationVerificationStatus"] = currentProfile.registrationVerificationStatus
        } else {
            currentProfile.degreeCertificateUri?.let { updates["degreeCertificateUri"] = it }
            updates["degreeVerificationStatus"] = currentProfile.degreeVerificationStatus
        }

        docRef.set(updates, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                val latestProfile = _profile.value
                val updatedProfile = if (certificateType == "degree") {
                    latestProfile.copy(
                        degreeCertificateUri = base64String,
                        degreeVerificationStatus = "PENDING"
                    )
                } else {
                    latestProfile.copy(
                        registrationCertificateUri = base64String,
                        registrationVerificationStatus = "PENDING"
                    )
                }
                _profile.value = updatedProfile
                saveProfile(updatedProfile)
                saveLocalPendingUser(updatedProfile)
                onSuccess()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("MedicineViewModel", "Firestore certificate upload failed: ${e.message}", e)
                onFailure(Exception("Database sync failed: ${e.localizedMessage}. Please try again."))
            }
    }

    private fun saveLocalPendingUser(p: PractitionerProfile) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val json = gson.toJson(p)
            prefs.edit().putString("local_pending_user_${user.uid}", json).apply()
            android.util.Log.d("MedicineViewModel", "Saved local pending user: ${user.uid} -> $json")
        }
    }

    fun getLocalPendingUsers(): List<Pair<String, PractitionerProfile>> {
        val list = mutableListOf<Pair<String, PractitionerProfile>>()
        try {
            val allEntries = prefs.all
            for ((key, value) in allEntries) {
                if (key.startsWith("local_pending_user_") && value is String) {
                    val uid = key.substring("local_pending_user_".length)
                    val p = gson.fromJson(value, PractitionerProfile::class.java)
                    // Check if it has pending verifications
                    if (p.degreeVerificationStatus == "PENDING" || p.registrationVerificationStatus == "PENDING") {
                        list.add(Pair(uid, p))
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MedicineViewModel", "Error loading local pending users: ${e.message}")
        }
        return list
    }



    fun listenToPendingUsers() {
        adminListener?.remove()
        
        // Initial state loaded from local SharedPreferences
        val localList = getLocalPendingUsers()
        _pendingUsers.value = localList

        adminListener = FirebaseFirestore.getInstance().collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("MedicineViewModel", "Firestore listener error: ${error.message}", error)
                    val currentLocal = getLocalPendingUsers()
                    _pendingUsers.value = currentLocal
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        android.widget.Toast.makeText(
                            application,
                            "Admin Fetch error: ${error.localizedMessage}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val firestoreList = mutableListOf<Pair<String, PractitionerProfile>>()
                    for (doc in snapshot.documents) {
                        val degreeStatus = doc.getString("degreeVerificationStatus") ?: "NONE"
                        val regStatus = doc.getString("registrationVerificationStatus") ?: "NONE"

                        if (degreeStatus == "PENDING" || regStatus == "PENDING") {
                            val p = PractitionerProfile(
                                name = doc.getString("name") ?: "",
                                qualification = doc.getString("qualification") ?: "",
                                clinicName = doc.getString("clinicName") ?: "",
                                registrationNo = doc.getString("registrationNo") ?: "",
                                isSetupComplete = doc.getBoolean("isSetupComplete") ?: false,
                                degreeCertificateUri = doc.getString("degreeCertificateUri"),
                                registrationCertificateUri = doc.getString("registrationCertificateUri"),
                                degreeVerificationStatus = degreeStatus,
                                registrationVerificationStatus = regStatus,
                                isAdmin = doc.getBoolean("isAdmin") ?: false
                            )
                            firestoreList.add(Pair(doc.id, p))
                        }
                    }
                    val mergedList = (firestoreList + getLocalPendingUsers()).distinctBy { it.first }
                    _pendingUsers.value = mergedList
                }
            }
    }

    fun stopListeningToPendingUsers() {
        adminListener?.remove()
        adminListener = null
        _pendingUsers.value = emptyList()
    }

    fun approveCertificate(userId: String, certificateType: String) {
        val fieldStatus = if (certificateType == "degree") "degreeVerificationStatus" else "registrationVerificationStatus"
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .update(fieldStatus, "APPROVED")
            .addOnSuccessListener {
                android.util.Log.i("MedicineViewModel", "Firestore approve successful for user $userId ($certificateType)")
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(
                        application,
                        "Successfully approved $certificateType for this user!",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("MedicineViewModel", "Firestore approve failed for user $userId ($certificateType): ${e.message}", e)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(
                        application,
                        "Approval failed: ${e.localizedMessage}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }

        // 1. Update local pending user JSON
        val prefKey = "local_pending_user_$userId"
        val storedJson = prefs.getString(prefKey, null)
        if (storedJson != null) {
            try {
                val p = gson.fromJson(storedJson, PractitionerProfile::class.java)
                val updatedProfile = if (certificateType == "degree") {
                    p.copy(degreeVerificationStatus = "APPROVED")
                } else {
                    p.copy(registrationVerificationStatus = "APPROVED")
                }
                prefs.edit().putString(prefKey, gson.toJson(updatedProfile)).apply()
            } catch (e: Exception) { /* ignore */ }
        }

        // 2. ALSO update the user's specific SharedPreferences keys directly!
        val keyStatus = if (certificateType == "degree") "p_deg_status_$userId" else "p_reg_status_$userId"
        prefs.edit().putString(keyStatus, "APPROVED").apply()

        // 3. If this is the current active profile (testing on same device), update UI state flow immediately
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid == userId) {
            val latestProfile = _profile.value
            val updated = if (certificateType == "degree") {
                latestProfile.copy(degreeVerificationStatus = "APPROVED")
            } else {
                latestProfile.copy(registrationVerificationStatus = "APPROVED")
            }
            _profile.value = updated
        }

        val updatedList = _pendingUsers.value.map { pair ->
            if (pair.first == userId) {
                val p = pair.second
                val updatedProfile = if (certificateType == "degree") {
                    p.copy(degreeVerificationStatus = "APPROVED")
                } else {
                    p.copy(registrationVerificationStatus = "APPROVED")
                }
                Pair(userId, updatedProfile)
            } else {
                pair
            }
        }
        _pendingUsers.value = updatedList
    }

    fun rejectCertificate(userId: String, certificateType: String) {
        val fieldStatus = if (certificateType == "degree") "degreeVerificationStatus" else "registrationVerificationStatus"
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .update(fieldStatus, "REJECTED")
            .addOnSuccessListener {
                android.util.Log.i("MedicineViewModel", "Firestore reject successful for user $userId ($certificateType)")
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(
                        application,
                        "Successfully rejected $certificateType for this user.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("MedicineViewModel", "Firestore reject failed for user $userId ($certificateType): ${e.message}", e)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(
                        application,
                        "Rejection failed: ${e.localizedMessage}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }

        // 1. Update local pending user JSON
        val prefKey = "local_pending_user_$userId"
        val storedJson = prefs.getString(prefKey, null)
        if (storedJson != null) {
            try {
                val p = gson.fromJson(storedJson, PractitionerProfile::class.java)
                val updatedProfile = if (certificateType == "degree") {
                    p.copy(degreeVerificationStatus = "REJECTED")
                } else {
                    p.copy(registrationVerificationStatus = "REJECTED")
                }
                prefs.edit().putString(prefKey, gson.toJson(updatedProfile)).apply()
            } catch (e: Exception) { /* ignore */ }
        }

        // 2. ALSO update the user's specific SharedPreferences keys directly!
        val keyStatus = if (certificateType == "degree") "p_deg_status_$userId" else "p_reg_status_$userId"
        prefs.edit().putString(keyStatus, "REJECTED").apply()

        // 3. If this is the current active profile (testing on same device), update UI state flow immediately
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid == userId) {
            val latestProfile = _profile.value
            val updated = if (certificateType == "degree") {
                latestProfile.copy(degreeVerificationStatus = "REJECTED")
            } else {
                latestProfile.copy(registrationVerificationStatus = "REJECTED")
            }
            _profile.value = updated
        }

        val updatedList = _pendingUsers.value.map { pair ->
            if (pair.first == userId) {
                val p = pair.second
                val updatedProfile = if (certificateType == "degree") {
                    p.copy(degreeVerificationStatus = "REJECTED")
                } else {
                    p.copy(registrationVerificationStatus = "REJECTED")
                }
                Pair(userId, updatedProfile)
            } else {
                pair
            }
        }
        _pendingUsers.value = updatedList
    }

    fun toggleLanguage() {
        _isHindi.update { !it }
    }

    fun updateProfile(newProfile: PractitionerProfile) {
        val currentProfile = _profile.value
        val mergedProfile = newProfile.copy(
            degreeCertificateUri = currentProfile.degreeCertificateUri ?: newProfile.degreeCertificateUri,
            registrationCertificateUri = currentProfile.registrationCertificateUri ?: newProfile.registrationCertificateUri,
            degreeVerificationStatus = if (currentProfile.degreeVerificationStatus != "NONE" && currentProfile.degreeVerificationStatus.isNotBlank()) currentProfile.degreeVerificationStatus else newProfile.degreeVerificationStatus,
            registrationVerificationStatus = if (currentProfile.registrationVerificationStatus != "NONE" && currentProfile.registrationVerificationStatus.isNotBlank()) currentProfile.registrationVerificationStatus else newProfile.registrationVerificationStatus
        )

        _profile.value = mergedProfile
        saveProfile(mergedProfile)
        saveLocalPendingUser(mergedProfile)
        
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val docRef = FirebaseFirestore.getInstance().collection("users").document(user.uid)
            val data = hashMapOf<String, Any>(
                "name" to mergedProfile.name,
                "qualification" to mergedProfile.qualification,
                "clinicName" to mergedProfile.clinicName,
                "registrationNo" to mergedProfile.registrationNo,
                "isSetupComplete" to mergedProfile.isSetupComplete,
                "degreeVerificationStatus" to mergedProfile.degreeVerificationStatus,
                "registrationVerificationStatus" to mergedProfile.registrationVerificationStatus,
                "isAdmin" to mergedProfile.isAdmin
            )
            mergedProfile.degreeCertificateUri?.let { data["degreeCertificateUri"] = it }
            mergedProfile.registrationCertificateUri?.let { data["registrationCertificateUri"] = it }
            
            docRef.set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    android.util.Log.i("MedicineViewModel", "updateProfile Firestore sync successful for ${user.uid}")
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("MedicineViewModel", "updateProfile Firestore sync failed for ${user.uid}: ${e.message}", e)
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        android.widget.Toast.makeText(
                            application,
                            "Profile Save Sync failed: ${e.localizedMessage}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    private fun saveProfile(p: PractitionerProfile) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: (if (p.isAdmin) "admin" else "anonymous")
        prefs.edit().apply {
            putString("p_name_$uid", p.name)
            putString("p_qual_$uid", p.qualification)
            putString("p_clinic_$uid", p.clinicName)
            putString("p_reg_$uid", p.registrationNo)
            putBoolean("p_setup_$uid", p.isSetupComplete)
            if (p.degreeCertificateUri != null) {
                putString("p_deg_uri_$uid", p.degreeCertificateUri)
            }
            if (p.registrationCertificateUri != null) {
                putString("p_reg_uri_$uid", p.registrationCertificateUri)
            }
            putString("p_deg_status_$uid", p.degreeVerificationStatus)
            putString("p_reg_status_$uid", p.registrationVerificationStatus)
            putBoolean("p_is_admin_$uid", p.isAdmin)
            apply()
        }
    }

    private fun loadProfile(uid: String? = FirebaseAuth.getInstance().currentUser?.uid): PractitionerProfile {
        val user = FirebaseAuth.getInstance().currentUser
        val isUserAdmin = (user != null && user.email == "admin@rasaadarsh.com") || 
                (uid != null && prefs.getBoolean("p_is_admin_$uid", false)) ||
                (uid == "admin")
        
        if (isUserAdmin) {
            return PractitionerProfile(
                name = "Administrator",
                qualification = "Admin",
                clinicName = "Rasaadarsh HQ",
                registrationNo = "ADMIN-01",
                isSetupComplete = true,
                degreeVerificationStatus = "APPROVED",
                registrationVerificationStatus = "APPROVED",
                isAdmin = true
            )
        }

        val keySuffix = uid ?: "anonymous"
        return PractitionerProfile(
            name = prefs.getString("p_name_$keySuffix", "") ?: "",
            qualification = prefs.getString("p_qual_$keySuffix", "") ?: "",
            clinicName = prefs.getString("p_clinic_$keySuffix", "") ?: "",
            registrationNo = prefs.getString("p_reg_$keySuffix", "") ?: "",
            isSetupComplete = prefs.getBoolean("p_setup_$keySuffix", false),
            degreeCertificateUri = prefs.getString("p_deg_uri_$keySuffix", null),
            registrationCertificateUri = prefs.getString("p_reg_uri_$keySuffix", null),
            degreeVerificationStatus = prefs.getString("p_deg_status_$keySuffix", "NONE") ?: "NONE",
            registrationVerificationStatus = prefs.getString("p_reg_status_$keySuffix", "NONE") ?: "NONE",
            isAdmin = false
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
                
                val count = withContext(Dispatchers.IO) { dao.getMedicineCount() }
                val EXPECTED_COUNT = 250 

                // Only reseed if absolutely necessary to avoid UI hangs during startup
                val needsReseed = count != EXPECTED_COUNT

                if (needsReseed) {
                    android.util.Log.i("MedicineViewModel", "Seeding database... (Count: $count, Expected: $EXPECTED_COUNT)")
                    withContext(Dispatchers.IO) {
                        try {
                            dao.deleteAll()
                            val inputStream = application.assets.open("medicines.json")
                            val reader = JsonReader(inputStream.bufferedReader())
                            
                            reader.beginArray()
                            val batch = mutableListOf<MedicineEntity>()
                            var totalInserted = 0
                            
                            while (reader.hasNext()) {
                                try {
                                    val record: com.example.rasaushadhies.ui.data.MedicineRecord = gson.fromJson(reader, com.example.rasaushadhies.ui.data.MedicineRecord::class.java)
                                    
                                    val entity = MedicineEntity(
                                        id = record.id,
                                        name = record.name,
                                        hindiName = record.hindiName, 
                                        benefits = record.benefits,
                                        ingredients = record.ingredients,
                                        preparation = record.preparation,
                                        dosage = record.`dose`,
                                        anupana = record.anupana,
                                        reference = record.reference,
                                        shloka = record.shloka,
                                        isBookmarked = false,
                                        clinicalNotes = "",
                                        diseaseCategory = record.diseaseCategory,
                                        ingredientsList = try { gson.fromJson(record.ingredientsListJson, object : TypeToken<List<Ingredient>>() {}.type) } catch(e: Exception) { null } ?: emptyList()
                                    )
                                    
                                    batch.add(entity)
                                    
                                    if (batch.size >= 50) {
                                        dao.insertAll(batch)
                                        totalInserted += batch.size
                                        batch.clear()
                                    }
                                } catch (e: Exception) {
                                    // Skip bad records
                                }
                            }
                            
                            if (batch.isNotEmpty()) {
                                dao.insertAll(batch)
                                totalInserted += batch.size
                            }
                            
                            reader.endArray()
                            reader.close()
                            android.util.Log.i("MedicineViewModel", "Seeding successful: $totalInserted records.")
                        } catch (e: Exception) {
                            android.util.Log.e("MedicineViewModel", "Seeding failure: ${e.message}")
                        }
                    }
                }
                
                // Observe the database
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
                                    reference = entity.reference,
                                    shloka = entity.shloka,
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
                                    reference = entity.reference,
                                    shloka = entity.shloka,
                                    isBookmarked = entity.isBookmarked,
                                    clinicalNotes = entity.clinicalNotes,
                                    ingredientsList = entity.ingredientsList,
                                    diseaseCategory = entity.diseaseCategory
                                )
                            }
                        }
                        .flowOn(Dispatchers.IO)
                        .collect { _recentMedicines.value = it }
                }

                val loaded = loadProfile()
                if (loaded.isAdmin) {
                    listenToPendingUsers()
                } else if (FirebaseAuth.getInstance().currentUser != null) {
                    syncProfileWithFirestore()
                }
            } catch (e: Exception) {
                android.util.Log.e("MedicineViewModel", "Initialization Error: ${e.message}")
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
