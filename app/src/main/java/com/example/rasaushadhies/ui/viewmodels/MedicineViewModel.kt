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
import kotlinx.coroutines.flow.update

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

            FirebaseAuth.getInstance().signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _currentUser.value = task.result?.user
                        android.util.Log.d("MedicineViewModel", "Admin logged in anonymously to Firebase")
                        listenToPendingUsers()
                    } else {
                        android.util.Log.e("MedicineViewModel", "Admin Firebase sign-in failed: ${task.exception?.message}")
                        listenToPendingUsers()
                    }
                }
            return true
        }
        return false
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

        val docRef = FirebaseFirestore.getInstance().collection("users").document(user.uid)
        firestoreListener = docRef.addSnapshotListener { snapshot, error ->
            if (snapshot != null && snapshot.exists()) {
                val p = PractitionerProfile(
                    name = snapshot.getString("name") ?: "",
                    qualification = snapshot.getString("qualification") ?: "",
                    clinicName = snapshot.getString("clinicName") ?: "",
                    registrationNo = snapshot.getString("registrationNo") ?: "",
                    isSetupComplete = snapshot.getBoolean("isSetupComplete") ?: false,
                    degreeCertificateUri = snapshot.getString("degreeCertificateUri"),
                    registrationCertificateUri = snapshot.getString("registrationCertificateUri"),
                    degreeVerificationStatus = snapshot.getString("degreeVerificationStatus") ?: "NONE",
                    registrationVerificationStatus = snapshot.getString("registrationVerificationStatus") ?: "NONE",
                    isAdmin = snapshot.getBoolean("isAdmin") ?: false
                )
                _profile.value = p
                saveProfile(p)
            } else {
                val local = loadProfile()
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

    fun uploadCertificate(uri: Uri, certificateType: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        
        // Copy the content from URI to a temp file in the app's cache directory to prevent Uri permission denial
        val tempFile = try {
            val extension = application.contentResolver.getType(uri)?.let { mimeType ->
                android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            } ?: "jpg"
            val file = java.io.File(application.cacheDir, "temp_cert_${certificateType}_${System.currentTimeMillis()}.$extension")
            application.contentResolver.openInputStream(uri)?.use { inputStream ->
                java.io.FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            file
        } catch (e: Exception) {
            onFailure(e)
            return
        }

        val storageRef = FirebaseStorage.getInstance().reference
            .child("certificates/${user.uid}/${certificateType}")

        storageRef.putFile(Uri.fromFile(tempFile))
            .addOnSuccessListener {
                tempFile.delete() // Clean up temp file
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val docRef = FirebaseFirestore.getInstance().collection("users").document(user.uid)
                    val fieldUri = if (certificateType == "degree") "degreeCertificateUri" else "registrationCertificateUri"
                    val fieldStatus = if (certificateType == "degree") "degreeVerificationStatus" else "registrationVerificationStatus"

                    val currentProfile = _profile.value
                    val updates = hashMapOf<String, Any>(
                        fieldUri to downloadUri.toString(),
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
                            // Update local profile state flow immediately so screen updates instantly
                            val latestProfile = _profile.value
                            val updatedProfile = if (certificateType == "degree") {
                                latestProfile.copy(
                                    degreeCertificateUri = downloadUri.toString(),
                                    degreeVerificationStatus = "PENDING"
                                )
                            } else {
                                latestProfile.copy(
                                    registrationCertificateUri = downloadUri.toString(),
                                    registrationVerificationStatus = "PENDING"
                                )
                            }
                            _profile.value = updatedProfile
                            saveProfile(updatedProfile)
                            saveLocalPendingUser(updatedProfile)
                            onSuccess()
                        }
                        .addOnFailureListener { e -> 
                            // Save locally even if Firestore fails
                            val latestProfile = _profile.value
                            val updatedProfile = if (certificateType == "degree") {
                                latestProfile.copy(
                                    degreeCertificateUri = downloadUri.toString(),
                                    degreeVerificationStatus = "PENDING"
                                )
                            } else {
                                latestProfile.copy(
                                    registrationCertificateUri = downloadUri.toString(),
                                    registrationVerificationStatus = "PENDING"
                                )
                            }
                            _profile.value = updatedProfile
                            saveProfile(updatedProfile)
                            saveLocalPendingUser(updatedProfile)
                            onFailure(e) 
                        }
                }.addOnFailureListener { e -> onFailure(e) }
            }
            .addOnFailureListener { storageException ->
                // Firebase Storage failed! Fallback to local files & Firestore set
                android.util.Log.w("MedicineViewModel", "Firebase Storage failed, running local/Firestore fallback: ${storageException.message}")
                
                try {
                    // Copy to persistent files directory
                    val persistentFile = java.io.File(application.filesDir, "cert_${certificateType}_${user.uid}.${tempFile.extension}")
                    tempFile.copyTo(persistentFile, overwrite = true)
                    tempFile.delete()

                    val localUriString = Uri.fromFile(persistentFile).toString()
                    val docRef = FirebaseFirestore.getInstance().collection("users").document(user.uid)
                    val fieldUri = if (certificateType == "degree") "degreeCertificateUri" else "registrationCertificateUri"
                    val fieldStatus = if (certificateType == "degree") "degreeVerificationStatus" else "registrationVerificationStatus"

                    val currentProfile = _profile.value
                    val updates = hashMapOf<String, Any>(
                        fieldUri to localUriString,
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
                            android.util.Log.i("MedicineViewModel", "Local certificate fallback saved to Firestore successfully")
                            val currentProfile = _profile.value
                            val updatedProfile = if (certificateType == "degree") {
                                currentProfile.copy(
                                    degreeCertificateUri = localUriString,
                                    degreeVerificationStatus = "PENDING"
                                )
                            } else {
                                currentProfile.copy(
                                    registrationCertificateUri = localUriString,
                                    registrationVerificationStatus = "PENDING"
                                )
                            }
                            _profile.value = updatedProfile
                            saveProfile(updatedProfile)
                            saveLocalPendingUser(updatedProfile)
                            onSuccess()
                        }
                        .addOnFailureListener { firestoreException ->
                            // Firestore also failed! Save directly to SharedPreferences / local state as absolute fallback
                            android.util.Log.e("MedicineViewModel", "Firestore also failed: ${firestoreException.message}")
                            val currentProfile = _profile.value
                            val updatedProfile = if (certificateType == "degree") {
                                currentProfile.copy(
                                    degreeCertificateUri = localUriString,
                                    degreeVerificationStatus = "PENDING"
                                )
                            } else {
                                currentProfile.copy(
                                    registrationCertificateUri = localUriString,
                                    registrationVerificationStatus = "PENDING"
                                )
                            }
                            _profile.value = updatedProfile
                            saveProfile(updatedProfile)
                            saveLocalPendingUser(updatedProfile)
                            onSuccess()
                        }
                } catch (e: Exception) {
                    tempFile.delete()
                    onFailure(storageException) // return original storage error
                }
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
            .addOnFailureListener {
                android.util.Log.w("MedicineViewModel", "Firestore approve failed: ${it.message}")
            }

        // Update local SharedPreferences key
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

                // If this is the current active profile (testing on same device), update UI immediately
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUid == userId) {
                    _profile.value = updatedProfile
                    saveProfile(updatedProfile)
                }
            } catch (e: Exception) { /* ignore */ }
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
            .addOnFailureListener {
                android.util.Log.w("MedicineViewModel", "Firestore reject failed: ${it.message}")
            }

        // Update local SharedPreferences key
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

                // If this is the current active profile (testing on same device), update UI immediately
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUid == userId) {
                    _profile.value = updatedProfile
                    saveProfile(updatedProfile)
                }
            } catch (e: Exception) { /* ignore */ }
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
        // Merge the new profile inputs with the existing local profile's certificate URIs & status to prevent overwriting them with null
        val currentProfile = _profile.value
        val mergedProfile = newProfile.copy(
            degreeCertificateUri = currentProfile.degreeCertificateUri ?: newProfile.degreeCertificateUri,
            registrationCertificateUri = currentProfile.registrationCertificateUri ?: newProfile.registrationCertificateUri,
            degreeVerificationStatus = if (currentProfile.degreeVerificationStatus != "NONE") currentProfile.degreeVerificationStatus else newProfile.degreeVerificationStatus,
            registrationVerificationStatus = if (currentProfile.registrationVerificationStatus != "NONE") currentProfile.registrationVerificationStatus else newProfile.registrationVerificationStatus
        )

        _profile.value = mergedProfile
        saveProfile(mergedProfile)
        
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val docRef = FirebaseFirestore.getInstance().collection("users").document(user.uid)
            val data = hashMapOf(
                "name" to mergedProfile.name,
                "qualification" to mergedProfile.qualification,
                "clinicName" to mergedProfile.clinicName,
                "registrationNo" to mergedProfile.registrationNo,
                "isSetupComplete" to mergedProfile.isSetupComplete,
                "degreeCertificateUri" to mergedProfile.degreeCertificateUri,
                "registrationCertificateUri" to mergedProfile.registrationCertificateUri,
                "degreeVerificationStatus" to mergedProfile.degreeVerificationStatus,
                "registrationVerificationStatus" to mergedProfile.registrationVerificationStatus,
                "isAdmin" to mergedProfile.isAdmin
            )
            docRef.set(data, com.google.firebase.firestore.SetOptions.merge())
        }
    }

    private fun saveProfile(p: PractitionerProfile) {
        prefs.edit().apply {
            putString("p_name", p.name)
            putString("p_qual", p.qualification)
            putString("p_clinic", p.clinicName)
            putString("p_reg", p.registrationNo)
            putBoolean("p_setup", p.isSetupComplete)
            putString("p_deg_uri", p.degreeCertificateUri)
            putString("p_reg_uri", p.registrationCertificateUri)
            putString("p_deg_status", p.degreeVerificationStatus)
            putString("p_reg_status", p.registrationVerificationStatus)
            putBoolean("p_is_admin", p.isAdmin)
            apply()
        }
    }

    private fun loadProfile(): PractitionerProfile {
        return PractitionerProfile(
            name = prefs.getString("p_name", "") ?: "",
            qualification = prefs.getString("p_qual", "") ?: "",
            clinicName = prefs.getString("p_clinic", "") ?: "",
            registrationNo = prefs.getString("p_reg", "") ?: "",
            isSetupComplete = prefs.getBoolean("p_setup", false),
            degreeCertificateUri = prefs.getString("p_deg_uri", null),
            registrationCertificateUri = prefs.getString("p_reg_uri", null),
            degreeVerificationStatus = prefs.getString("p_deg_status", "NONE") ?: "NONE",
            registrationVerificationStatus = prefs.getString("p_reg_status", "NONE") ?: "NONE",
            isAdmin = prefs.getBoolean("p_is_admin", false)
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
