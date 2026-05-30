package com.example.rasaushadhies.util

import com.example.rasaushadhies.ui.screens.Medicine

object MedicineSearchUtils {
    
    fun matchesQuery(m: Medicine, query: String): Boolean {
        if (query.isBlank()) return true
        
        // Split query into individual terms for broad matching
        val searchTerms = query.split("/", " ", ",").map { it.trim() }.filter { it.isNotEmpty() }
        
        return searchTerms.any { term ->
            val matchesBasic = m.id.toString() == term ||
                    m.name.contains(term, ignoreCase = true) ||
                    m.hindiName.contains(term, ignoreCase = true) ||
                    m.benefits.contains(term, ignoreCase = true) ||
                    m.ingredients.contains(term, ignoreCase = true) ||
                    m.preparation.contains(term, ignoreCase = true) ||
                    m.dosage.contains(term, ignoreCase = true) ||
                    m.anupana.contains(term, ignoreCase = true) ||
                    m.diseaseCategory.contains(term, ignoreCase = true)
            
            matchesBasic || isDiseaseSynonymMatch(m, term) || isIngredientMatch(m, term)
        }
    }

    private fun isDiseaseSynonymMatch(m: Medicine, keyword: String): Boolean {
        val variations = when (keyword.lowercase()) {
            "anemia", "anaemia", "पाण्डु", "pandu" -> listOf("pandu", "anaemia", "anemia", "haemoglobin", "blood deficiency")
            "cough", "कास", "kasa" -> listOf("kasa", "cough", "bronchitis")
            "asthma", "श्वास", "shwasa" -> listOf("shwasa", "asthma", "breathing", "respiratory")
            "fever", "ज्वर", "jwara" -> listOf("jwara", "fever", "pyrexia", "temperature")
            "indigestion", "अग्निमांद्य", "mandagni" -> listOf("ajirna", "indigestion", "digestive", "appetite", "mandagni", "metabolism")
            "skin disease", "त्वचा", "kushtha" -> listOf("kushtha", "skin", "dermatitis", "psoriasis", "eczema", "leprosy", "itching")
            "arthritis", "वातरोग", "vataroga" -> listOf("vataroga", "arthritis", "joint pain", "amavata", "rheumatoid", "stiffness", "inflammation")
            "diabetes", "मधुमेह", "prameha" -> listOf("prameha", "diabetes", "urinary", "sugar", "madhumeha")
            "jaundice", "कमला", "kamala" -> listOf("kamala", "jaundice", "liver", "hepatitis", "bilirubin")
            "piles", "अर्ध", "arsha", "hemorrhoids" -> listOf("arsha", "piles", "hemorrhoids", "anal", "rectal")
            else -> emptyList()
        }
        
        if (variations.isEmpty()) return false
        
        return variations.any { v ->
            m.name.contains(v, true) || 
            m.benefits.contains(v, true)
        }
    }

    fun isIngredientMatch(m: Medicine, keyword: String): Boolean {
        val variations = when (keyword.lowercase()) {
            "parada", "shuddha parada", "mercury" -> listOf("parada", "parad", "mercury", "hingula", "mercuric", "kajjali", "rasasindura", "cinnabar", "sutaka", "mercurial")
            "gandhak", "shuddha gandhak", "sulphur" -> listOf("gandhak", "gandhaka", "sulphur", "sulfur", "sulphide", "sulfide", "kajjali", "bali", "sulphuric")
            "vatsanabha", "aconite" -> listOf("vatsanabha", "vatsanabhi", "aconite", "visha", "bachhnaag", "bachnag", "vatsanabhi", "ferox")
            "loha", "iron" -> listOf("loha", "lauha", "iron", "ferrous", "ferric")
            "tamra", "copper" -> listOf("tamra", "copper", "cuprum")
            "abhraka", "mica" -> listOf("abhraka", "mica", "biotite")
            else -> listOf(keyword)
        }
        return variations.any { v ->
            m.name.contains(v, true) || 
            m.ingredients.contains(v, true) || 
            m.preparation.contains(v, true)
        }
    }

    fun isDoshaMatch(m: Medicine, keyword: String): Boolean {
        val variations = when (keyword.lowercase()) {
            "pitta" -> listOf("pitta", "pitt", "paittika", "pittaja")
            "vata"  -> listOf("vata", "vat", "vayu", "vatika", "vataja")
            "kapha" -> listOf("kapha", "kaph", "shleshm", "kaphaja")
            else    -> listOf(keyword)
        }
        return variations.any { v ->
            m.name.contains(v, true) || 
            m.hindiName.contains(v, true) ||
            m.benefits.contains(v, true) ||
            m.ingredients.contains(v, true)
        }
    }
}
