package com.example.rasaushadhies

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rasaushadhies.ui.screens.Medicine
import com.example.rasaushadhies.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
//  Data Models
// ─────────────────────────────────────────────────────────────────────────────

enum class ChatRole { USER, BOT }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val text: String,
    val isLoading: Boolean = false,
    val source: String = ""
)

data class ChatContext(
    val lastShownMedicines: List<Medicine> = emptyList(),
    val focusedMedicine: Medicine? = null,
    val pendingMoreMedicines: List<Medicine> = emptyList(),   // Feature 2: pagination
    val lastFuzzySuggestion: String? = null                   // Feature 12: fuzzy match
)

// ─────────────────────────────────────────────────────────────────────────────
//  Feature 3: Search History (SharedPrefs)
// ─────────────────────────────────────────────────────────────────────────────

private fun loadSearchHistory(prefs: android.content.SharedPreferences): List<String> {
    val json = prefs.getString("chat_search_history", "[]") ?: "[]"
    return try { val a = JSONArray(json); List(a.length()) { a.getString(it) } } catch (e: Exception) { emptyList() }
}

private fun saveSearchHistory(prefs: android.content.SharedPreferences, h: List<String>) {
    val a = JSONArray(); h.forEach { a.put(it) }
    prefs.edit().putString("chat_search_history", a.toString()).apply()
}

// ─────────────────────────────────────────────────────────────────────────────
//  Feature 10: Conversation persistence (SharedPrefs)
// ─────────────────────────────────────────────────────────────────────────────

private fun loadConvHistory(prefs: android.content.SharedPreferences): List<Pair<String, String>> {
    val json = prefs.getString("chat_history_v1", "[]") ?: "[]"
    return try {
        val a = JSONArray(json)
        List(a.length()) { val o = a.getJSONObject(it); Pair(o.getString("user"), o.getString("bot")) }
    } catch (e: Exception) { emptyList() }
}

private fun saveConvHistory(prefs: android.content.SharedPreferences, h: List<Pair<String, String>>) {
    val a = JSONArray(); h.forEach { (u, b) -> a.put(JSONObject().put("user", u).put("bot", b)) }
    prefs.edit().putString("chat_history_v1", a.toString()).apply()
}

private fun buildRestoredMessages(prefs: android.content.SharedPreferences): List<ChatMessage> {
    val h = loadConvHistory(prefs)
    if (h.isEmpty()) return emptyList()
    val msgs = mutableListOf<ChatMessage>()
    h.takeLast(5).forEach { (u, b) ->
        msgs.add(ChatMessage(role = ChatRole.USER, text = u))
        msgs.add(ChatMessage(role = ChatRole.BOT, text = b, source = "db"))
    }
    return msgs.takeLast(10)
}

// ─────────────────────────────────────────────────────────────────────────────
//  Feature 5: Hindi → English mapping
// ─────────────────────────────────────────────────────────────────────────────

private val HINDI_MAP = listOf(
    "बुखार" to "fever jwara", "ज्वर" to "fever jwara",
    "हृदय" to "heart hridayrog", "दिल" to "heart hridayrog",
    "खांसी" to "cough kasa", "कास" to "cough kasa",
    "श्वास" to "asthma shwas", "दमा" to "asthma shwas",
    "त्वचा" to "skin twacha", "चर्म" to "skin twacha",
    "पेट" to "stomach udara", "सिर" to "headache shiro",
    "आँख" to "eye netra", "कान" to "ear karna",
    "जोड़" to "joint arthritis sandhi",
    "मधुमेह" to "diabetes madhumeha", "शुगर" to "diabetes madhumeha",
    "रक्तचाप" to "blood pressure", "लीवर" to "liver yakrit",
    "यकृत" to "liver yakrit", "किडनी" to "kidney vrikka",
    "वृक्क" to "kidney vrikka", "पाण्डु" to "anemia pandu",
    "अर्श" to "hemorrhoids arsha"
)

private fun translateHindiQuery(q: String): String {
    var r = q
    HINDI_MAP.forEach { (h, e) -> if (r.contains(h)) r = r.replace(h, e) }
    return r
}

// ─────────────────────────────────────────────────────────────────────────────
//  Feature 6: Dosha Detector
// ─────────────────────────────────────────────────────────────────────────────

private fun detectDosha(s: String): String? {
    val t = s.lowercase()
    val v = listOf("dry skin","anxiety","bloating","constipation","joint pain","tremors","insomnia","vata","shushka","ruksha","anaha","kampavata").count { t.contains(it) }
    val p = listOf("fever","burning","acidity","inflammation","anger","pitta","jwara","daha","amlapitta","pittaja").count { t.contains(it) }
    val k = listOf("congestion","obesity","lethargy","mucus","kapha","shleshma","sthoulya","kaphaja","heaviness").count { t.contains(it) }
    val mx = maxOf(v, p, k)
    if (mx == 0) return null
    return when (mx) { v -> "Vata"; p -> "Pitta"; else -> "Kapha" }
}

private fun doshaGunaLabel(d: String) = when(d) { "Vata" -> "Vatahara"; "Pitta" -> "Pittahara"; else -> "Kaphahara" }
private fun doshaEmoji(d: String) = when(d) { "Vata" -> "💨"; "Pitta" -> "🔥"; else -> "💧" }

// ─────────────────────────────────────────────────────────────────────────────
//  Feature 2 (Guna) + 7 (Anupana) + 8 (Reference) + 9 (Ingredient) filters
// ─────────────────────────────────────────────────────────────────────────────

private data class GunaFilter(val label: String, val patterns: List<String>)
private data class AnupanaFilter(val label: String, val keywords: List<String>)
private data class ReferenceFilter(val label: String, val keywords: List<String>)

private val GUNA_FILTERS = listOf(
    GunaFilter("Sheeta Virya",  listOf("sheeta","cooling","sheetala","sheeta virya")),
    GunaFilter("Ushna Virya",   listOf("ushna","ushna virya","heating","tikshna")),
    GunaFilter("Tikta Rasa",    listOf("tikta","bitter")),
    GunaFilter("Kashaya Rasa",  listOf("kashaya","astringent")),
    GunaFilter("Madhura Rasa",  listOf("madhura","sweet","madhur")),
    GunaFilter("Katu Rasa",     listOf("katu","pungent","katuka")),
    GunaFilter("Amla Rasa",     listOf("amla","sour","acidic")),
    GunaFilter("Laghu Guna",    listOf("laghu","light quality")),
    GunaFilter("Guru Guna",     listOf("guru","heavy quality")),
    GunaFilter("Snigdha Guna",  listOf("snigdha","unctuous","oleating")),
    GunaFilter("Ruksha Guna",   listOf("ruksha","drying")),
    GunaFilter("Vatahara",      listOf("vatahara","vata hara","vata nashak","anti-vata")),
    GunaFilter("Pittahara",     listOf("pittahara","pitta hara","pitta nashak","anti-pitta")),
    GunaFilter("Kaphahara",     listOf("kaphahara","kapha hara","kapha nashak","anti-kapha")),
    GunaFilter("Tridoshahara",  listOf("tridosha","tridoshahara"))
)

private val ANUPANA_FILTERS = listOf(
    AnupanaFilter("Ghrita (Ghee)",          listOf("with ghee","anupana ghee","ghrita")),
    AnupanaFilter("Madhu (Honey)",          listOf("with honey","anupana honey","anupana madhu","with madhu")),
    AnupanaFilter("Dugdha (Milk)",          listOf("with milk","anupana milk","dugdha","ksheera")),
    AnupanaFilter("Ushna Udaka (Warm Water)",listOf("with warm water","ushna udaka","warm water"))
)

private val REFERENCE_FILTERS = listOf(
    ReferenceFilter("RasaRatnaSamucchay",   listOf("rasaratna","rasaratnasamu","rrs")),
    ReferenceFilter("Sharangdhar Samhita",  listOf("sharangdhar","sharangadhara")),
    ReferenceFilter("Ayurvedsara Sangraha", listOf("ayurvedsar","ayurvedsara")),
    ReferenceFilter("Classical Text",       listOf("classical text","classical"))
)

private fun extractGunaFilter(q: String): Pair<GunaFilter?, String> {
    var rem = q
    for (gf in GUNA_FILTERS) {
        for (p in gf.patterns) {
            if (q.contains(p, ignoreCase = true)) {
                rem = q.replace(p, "", ignoreCase = true).trim()
                return Pair(gf, rem)
            }
        }
    }
    return Pair(null, q)
}

private fun extractAnupanaFilter(q: String): AnupanaFilter? =
    ANUPANA_FILTERS.firstOrNull { af -> af.keywords.any { q.contains(it, ignoreCase = true) } }

private fun extractReferenceFilter(q: String): Pair<ReferenceFilter?, Int?> {
    val chap = Regex("chapter\\s*(\\d+)").find(q.lowercase())?.groupValues?.get(1)?.toIntOrNull()
    val rf = REFERENCE_FILTERS.firstOrNull { rf -> rf.keywords.any { q.contains(it, ignoreCase = true) } }
    return Pair(rf, chap)
}

private fun extractIngredientFilter(q: String): String? {
    val patterns = listOf(
        Regex("medicine containing (.+)"),
        Regex("(?:with ingredient|containing|ingredient|samagri) (.+)")
    )
    for (re in patterns) {
        val m = re.find(q.lowercase()) ?: continue
        val ing = m.groupValues[1].trim().split(" ").take(3).joinToString(" ")
        if (ing.isNotBlank()) return ing
    }
    return null
}

// ─────────────────────────────────────────────────────────────────────────────
//  Full DB search (no cap) + pagination
// ─────────────────────────────────────────────────────────────────────────────

private const val PAGE_SIZE = 10

private fun searchAllMedicines(query: String, all: List<Medicine>): List<Medicine> {
    if (query.isBlank()) return emptyList()
    val q = query.lowercase().trim()
    val stopWords = setOf(
        "tell", "me", "all", "medicine", "medicines", "for", "kya", "hai", "ke", "ka", "kis", "mein", "ki", "saath", "ko", "se", "are", "is", "the", "a", "an", "what", "how", "why", "who", "where", "which", "give", "show", "list", "find", "search", "about", "of", "and", "or", "in", "to", "with", "h", "kya-kya", "batao", "bataiye", "please", "formula", "formulation"
    )
    val words = q.split(Regex("[\\s,?.!:]+")).map { it.trim() }.filter { it.length > 2 && it !in stopWords }
    
    if (words.isEmpty()) {
        return all.filter { m ->
            val txt = "${m.name} ${m.hindiName} ${m.benefits} ${m.ingredients} ${m.diseaseCategory}".lowercase()
            txt.contains(q)
        }
    }
    
    return all.filter { m ->
        val txt = "${m.name} ${m.hindiName} ${m.benefits} ${m.ingredients} ${m.diseaseCategory}".lowercase()
        txt.contains(q) || words.any { txt.contains(it) }
    }.sortedByDescending { m ->
        val txt = "${m.name} ${m.hindiName} ${m.benefits} ${m.ingredients} ${m.diseaseCategory}".lowercase()
        val matchesCount = words.count { txt.contains(it) }
        val nameMatch = if (m.name.lowercase().contains(q) || m.hindiName.lowercase().contains(q)) 5 else 0
        matchesCount + nameMatch
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Feature 12: Levenshtein fuzzy match
// ─────────────────────────────────────────────────────────────────────────────

private fun levenshtein(a: String, b: String): Int {
    val m = a.length; val n = b.length
    val dp = Array(m + 1) { IntArray(n + 1) }
    for (i in 0..m) dp[i][0] = i
    for (j in 0..n) dp[0][j] = j
    for (i in 1..m) for (j in 1..n)
        dp[i][j] = if (a[i-1] == b[j-1]) dp[i-1][j-1] else 1 + minOf(dp[i-1][j], dp[i][j-1], dp[i-1][j-1])
    return dp[m][n]
}

private fun findClosestDisease(query: String, all: List<Medicine>): String? {
    if (query.length <= 3) return null
    val candidates = mutableSetOf<String>()
    all.forEach { m ->
        candidates.add(m.name.lowercase())
        m.diseaseCategory.split(",").forEach { candidates.add(it.trim().lowercase()) }
    }
    val q = query.lowercase()
    val best = candidates.filter { it.isNotBlank() && it.length in 3..40 }
        .minByOrNull { levenshtein(q, it) } ?: return null
    return if (levenshtein(q, best) <= 3) best else null
}

// ─────────────────────────────────────────────────────────────────────────────
//  isAiIntent + helper predicates
// ─────────────────────────────────────────────────────────────────────────────

private fun isAiIntent(q: String): Boolean {
    val comp = listOf("difference","compare","versus","vs","better","contrast","between")
    val reas = listOf("why","how","mechanism","action","work","explain","reason","cause")
    val cplx = listOf("best","top","rank","among","considering","recommend","suggest","which one")
    return (comp + reas + cplx).any { q.contains(it) }
}

private fun isShowMoreIntent(q: String) =
    listOf("show more","aur dikhao","more results","aur batao","more medicines","next page").any { q.contains(it) }

private fun isSaveIntent(q: String) =
    listOf("save","bookmark","add to cabinet","remember this","keep this","save this").any { q.contains(it) }

private fun isPreparationQuery(q: String) =
    listOf("preparation", "prepare", "how to make", "method", "process", "nirmana", "nirman", "vidhi", "banane", "kaise banaye", "kaise banate", "banane ki vidhi").any { q.contains(it) }

private fun isDoseQuery(q: String) =
    listOf("dose", "dosage", "matra", "how much", "amount", "kitna").any { q.contains(it) }

private fun isIngredientQuery(q: String) =
    listOf("ingredient", "samagri", "made of", "ghatak", "composition", "ingredients", "contains", "milakar").any { q.contains(it) }

private fun isBenefitQuery(q: String) =
    listOf("benefit", "use", "fayde", "treats", "helps", "upyog", "fayda", "faida", "work").any { q.contains(it) }

private fun isFullDetailsQuery(q: String) =
    listOf("detail", "full", "complete", "about", "tell me", "batao").any { q.contains(it) }

private fun isHindiQuery(q: String): Boolean {
    val lower = q.lowercase().trim()
    if (lower.any { it.code in 0x0900..0x097F }) return true
    val hindiKeywords = listOf(
        "kya", "hai", "hain", "kaise", "sath", "saath", "ke", "ka", "ki", "ko", "se", 
        "ghatak", "samagri", "matra", "anupan", "anupana", "granth", "grantha", "varnit", 
        "kisme", "kaha", "fayda", "fayde", "upyog", "batao", "bataiye", "banao", "banaye", 
        "vidhi", "diya", "jata", "kis", "mein"
    )
    val words = lower.split(Regex("[\\s,?.!:]+")).map { it.trim() }
    return words.any { it in hindiKeywords }
}

private fun isAnupanaQuery(q: String) =
    listOf("anupana", "anupan", "vehicle", "along with", "take with", "with what", "ke sath", "saath").any { q.contains(it) }

private fun isReferenceQuery(q: String) =
    listOf("reference", "source", "book", "text", "shloka", "grantha", "granth", "varnit", "sandarbha", "kisme", "kaha", "verse", "chapter").any { q.contains(it) }

// ─────────────────────────────────────────────────────────────────────────────
//  Format helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun formatFullDetails(m: Medicine, forceHindi: Boolean): String = buildString {
    if (forceHindi) {
        append("🌿 **${m.hindiName.ifBlank { m.name }}**")
        if (m.hindiName.isNotBlank() && m.name.isNotBlank() && m.name != m.hindiName) {
            append("  *(${m.name})*")
        }
        append("\n\n📌 **लाभ (Benefits):**\n${m.benefits}\n\n")
        append("🧪 **घटक द्रव्य (Ingredients):**\n${m.ingredients}\n\n")
        append("💊 **मात्रा (Dose):** ${m.dosage}")
        if (m.anupana.isNotBlank()) append("\n🍯 **अनुपान (Anupana):** ${m.anupana}")
        if (m.reference.isNotBlank()) append("\n📚 **सन्दर्भ ग्रन्थ (Reference):** ${m.reference}")
        append("\n\n📋 **निर्माण विधि (Preparation):**\n${m.preparation}")
    } else {
        append("🌿 **${m.name}**")
        if (m.hindiName.isNotBlank()) append("  *(${m.hindiName})*")
        append("\n\n📌 **Benefits:**\n${m.benefits}\n\n")
        append("🧪 **Ingredients:**\n${m.ingredients}\n\n")
        append("💊 **Dose:** ${m.dosage}")
        if (m.anupana.isNotBlank()) append("\n🍯 **Anupana:** ${m.anupana}")
        if (m.reference.isNotBlank()) append("\n📚 **Ref:** ${m.reference}")
        append("\n\n📋 **Preparation:**\n${m.preparation}")
    }
}

private fun formatPreparation(m: Medicine, forceHindi: Boolean) =
    if (forceHindi) "📋 **${m.hindiName.ifBlank { m.name }} — निर्माण विधि (Preparation Method)**\n\n${m.preparation}"
    else "📋 **${m.name} — Preparation (निर्माण विधि)**\n\n${m.preparation}"

private fun formatDose(m: Medicine, forceHindi: Boolean) =
    if (forceHindi) "💊 **${m.hindiName.ifBlank { m.name }} — मात्रा एवं अनुपान (Dose & Anupana)**\n\nमात्रा (Dose): ${m.dosage}\nअनुपान (Anupana): ${m.anupana}"
    else "💊 **${m.name} — Dose (मात्रा)**\n\nDose: ${m.dosage}\nAnupana: ${m.anupana}"

private fun formatIngredients(m: Medicine, forceHindi: Boolean) =
    if (forceHindi) "🧪 **${m.hindiName.ifBlank { m.name }} — घटक द्रव्य (Ingredients)**\n\n${m.ingredients}"
    else "🧪 **${m.name} — Ingredients (घटक द्रव्य)**\n\n${m.ingredients}"

private fun formatBenefits(m: Medicine, forceHindi: Boolean) =
    if (forceHindi) "✨ **${m.hindiName.ifBlank { m.name }} — उपयोग एवं लाभ (Benefits)**\n\n${m.benefits}"
    else "✨ **${m.name} — Benefits (लाभ)**\n\n${m.benefits}"

private fun formatAnupana(m: Medicine, forceHindi: Boolean) =
    if (forceHindi) "🍯 **${m.hindiName.ifBlank { m.name }} — अनुपान (Anupana / Vehicle)**\n\nइसके साथ लें (Take with): ${m.anupana}"
    else "🍯 **${m.name} — Anupana / Vehicle**\n\nTake with: ${m.anupana}"

private fun formatReference(m: Medicine, forceHindi: Boolean) = buildString {
    if (forceHindi) {
        append("📚 **${m.hindiName.ifBlank { m.name }} — ग्रन्थ सन्दर्भ एवं श्लोक (Reference & Shloka)**\n\n")
        if (m.reference.isNotBlank()) {
            append("📌 **सन्दर्भ (Reference):** ${m.reference}\n\n")
        }
        if (m.shloka.isNotBlank()) {
            append("📖 **संस्कृत श्लोक (Sanskrit Shloka):**\n`${m.shloka}`\n")
        }
    } else {
        append("📚 **${m.name} — Reference & Sanskrit Shloka (ग्रन्थ सन्दर्भ)**\n\n")
        if (m.reference.isNotBlank()) {
            append("📌 **Reference:** ${m.reference}\n\n")
        }
        if (m.shloka.isNotBlank()) {
            append("📖 **Sanskrit Shloka:**\n`${m.shloka}`\n")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  buildLocalAnswer (Feature 2: pagination + filter headers)
// ─────────────────────────────────────────────────────────────────────────────

private fun buildLocalAnswer(
    query: String,
    results: List<Medicine>,
    context: ChatContext,
    gunaFilter: GunaFilter? = null,
    anupanaFilter: AnupanaFilter? = null,
    referenceFilter: String? = null,
    ingredientFilter: String? = null,
    detectedDosha: String? = null,
    isNextPage: Boolean = false
): Pair<String, ChatContext> {
    val forceHindi = isHindiQuery(query)
    if (results.isEmpty()) {
        val noResMsg = if (forceHindi) "मुझे \"$query\" के लिए कोई औषधि नहीं मिली। बीमारी, लक्षण या घटक का नाम खोजें।" 
                       else "I couldn't find medicines for \"$query\". Try a disease name, symptom, or ingredient."
        return Pair(noResMsg, context)
    }

    if (results.size == 1) {
        val m = results[0]
        return Pair(formatFullDetails(m, forceHindi), context.copy(focusedMedicine = m, lastShownMedicines = results, pendingMoreMedicines = emptyList()))
    }

    val page = results.take(PAGE_SIZE)
    val pending = results.drop(PAGE_SIZE)

    val reply = buildString {
        if (!isNextPage) {
            val tags = mutableListOf<String>()
            if (detectedDosha != null) tags.add("${doshaEmoji(detectedDosha)} Dosha: **$detectedDosha**")
            if (gunaFilter != null) tags.add("🌿 **${gunaFilter.label}**")
            if (anupanaFilter != null) tags.add("🍯 **${anupanaFilter.label}**")
            if (referenceFilter != null) tags.add("📚 **$referenceFilter**")
            if (ingredientFilter != null) tags.add("🧪 Contains: **$ingredientFilter**")
            if (tags.isNotEmpty()) append(tags.joinToString("  ·  ") + "\n\n")
            if (forceHindi) {
                append("🔍 \"$query\" के लिए **${results.size} औषधियां** मिलीं:\n\n")
            } else {
                append("🔍 Found **${results.size} medicines** for \"$query\":\n\n")
            }
        } else {
            if (forceHindi) {
                append("📋 **अगली ${page.size} औषधियां:**\n\n")
            } else {
                append("📋 **Next ${page.size} medicines:**\n\n")
            }
        }
        page.forEachIndexed { i, m ->
            val nameToShow = if (forceHindi && m.hindiName.isNotBlank()) m.hindiName else m.name
            val secondaryName = if (forceHindi && m.hindiName.isNotBlank()) m.name else m.hindiName
            append("${i + 1}. **$nameToShow**")
            if (secondaryName.isNotBlank() && secondaryName != nameToShow) append(" *($secondaryName)*")
            append("\n   📌 ${m.benefits.take(80)}${if (m.benefits.length > 80) "..." else ""}\n\n")
        }
        if (pending.isNotEmpty()) {
            if (forceHindi) {
                append("📋 ${results.size} में से ${page.size} दिख रही हैं। और देखने के लिए **\"aur dikhao\"** कहें।")
            } else {
                append("📋 Showing ${page.size} of ${results.size}. Say **\"show more\"** for ${pending.size} more.")
            }
        } else {
            if (forceHindi) {
                append("💡 विस्तृत विवरण के लिए **\"1 के बारे में बताओ\"** या **\"3 की मात्रा\"** कहें।")
            } else {
                append("💡 Say **\"tell me about 1\"** or **\"dose of 3\"** for full details.")
            }
        }
    }

    return Pair(reply, context.copy(
        lastShownMedicines = page,
        focusedMedicine = null,
        pendingMoreMedicines = pending
    ))
}

// ─────────────────────────────────────────────────────────────────────────────
//  tryHandleFollowUp (numbers 1-20 + detail intents)
// ─────────────────────────────────────────────────────────────────────────────

private fun tryHandleFollowUp(q: String, ctx: ChatContext): Pair<String, ChatContext>? {
    val list = ctx.lastShownMedicines
    val focused = ctx.focusedMedicine
    if (list.isEmpty() && focused == null) return null
    val forceHindi = isHindiQuery(q)

    val nums = mapOf(
        "1" to 0,"one" to 0,"first" to 0,"pehla" to 0,"pahla" to 0,
        "2" to 1,"two" to 1,"second" to 1,"doosra" to 1,"dusra" to 1,
        "3" to 2,"three" to 2,"third" to 2,"teesra" to 2,
        "4" to 3,"four" to 3,"fourth" to 3,"chautha" to 3,
        "5" to 4,"five" to 4,"fifth" to 4,
        "6" to 5,"six" to 5,"sixth" to 5,
        "7" to 6,"seven" to 6,"seventh" to 6,
        "8" to 7,"eight" to 7,"eighth" to 7,
        "9" to 8,"nine" to 8,"ninth" to 8,
        "10" to 9,"ten" to 9,"tenth" to 9,
        "11" to 10,"eleven" to 10,"12" to 11,"twelve" to 11,
        "13" to 12,"thirteen" to 12,"14" to 13,"fourteen" to 13,
        "15" to 14,"fifteen" to 14,"16" to 15,"17" to 16,"18" to 17,
        "19" to 18,"20" to 19,"twenty" to 19
    )

    for ((word, idx) in nums) {
        if (q.contains(word) && idx < list.size) {
            val m = list[idx]
            val reply = when {
                isPreparationQuery(q) -> formatPreparation(m, forceHindi)
                isDoseQuery(q) -> formatDose(m, forceHindi)
                isIngredientQuery(q) -> formatIngredients(m, forceHindi)
                isBenefitQuery(q) -> formatBenefits(m, forceHindi)
                else -> formatFullDetails(m, forceHindi)
            }
            return Pair(reply, ctx.copy(focusedMedicine = m))
        }
    }

    if (focused != null) {
        val reply = when {
            isPreparationQuery(q) -> formatPreparation(focused, forceHindi)
            isDoseQuery(q) -> formatDose(focused, forceHindi)
            isIngredientQuery(q) -> formatIngredients(focused, forceHindi)
            isBenefitQuery(q) -> formatBenefits(focused, forceHindi)
            isFullDetailsQuery(q) -> formatFullDetails(focused, forceHindi)
            else -> null
        }
        if (reply != null) return Pair(reply, ctx)
    }
    return null
}

// ─────────────────────────────────────────────────────────────────────────────
//  resolveQuery — master routing (all 9 features wired)
// ─────────────────────────────────────────────────────────────────────────────

private fun isGreetingOrConversational(q: String): Boolean {
    val lower = q.lowercase().trim()
    val greetings = listOf(
        "hi", "hello", "hey", "namaste", "pranam", "greetings", "good morning", "good afternoon", "good evening", "yo", "hola"
    )
    val conversational = listOf(
        "your name", "who are you", "who is this", "what is you name", "whats your name", "what is your name",
        "how are you", "how's it going", "how do you do", "what can you do", "help me", "what are you"
    )
    
    // Check for exact greeting match
    if (greetings.any { lower == it }) return true
    
    // Check if query is conversational question
    if (conversational.any { lower.contains(it) }) return true
    
    return false
}

private suspend fun resolveQuery(
    userText: String,
    allMedicines: List<Medicine>,
    context: ChatContext,
    history: List<Pair<String, String>>,
    onToggleBookmarkByName: (String) -> Unit,
    apiKey: String
): Triple<String, String, ChatContext> {

    val q = userText.trim().lowercase()

    // Intercept greetings / conversational inputs first
    if (isGreetingOrConversational(q)) {
        val ai = callOpenRouterApi(userText, emptyList(), history, apiKey)
        return Triple(ai, "ai", context)
    }

    // Save/Bookmark
    if (isSaveIntent(q)) {
        val f = context.focusedMedicine
        return if (f != null) {
            onToggleBookmarkByName(f.name)
            Triple("🌿 **Saved ${f.name} to your Cabinet.**", "db", context)
        } else {
            Triple("Which medicine would you like to save? Mention its name first.", "db", context)
        }
    }

    // Intercept: specific medicine-targeted queries (Hindi or English names)
    // Don't intercept if it has AI intent (e.g. comparison / reasoning)
    val isAi = isAiIntent(q)
    val matchedMedicine = if (!isAi) {
        allMedicines.firstOrNull { medicine ->
            val mName = medicine.name.lowercase().trim()
            val mHindi = medicine.hindiName.lowercase().trim()
            (mName.isNotEmpty() && q.contains(mName)) || (mHindi.isNotEmpty() && q.contains(mHindi))
        }
    } else null

    val forceHindi = isHindiQuery(q)
    if (matchedMedicine != null) {
        val reply = when {
            isPreparationQuery(q) -> formatPreparation(matchedMedicine, forceHindi)
            isDoseQuery(q) -> formatDose(matchedMedicine, forceHindi)
            isIngredientQuery(q) -> formatIngredients(matchedMedicine, forceHindi)
            isBenefitQuery(q) -> formatBenefits(matchedMedicine, forceHindi)
            isReferenceQuery(q) -> formatReference(matchedMedicine, forceHindi)
            isAnupanaQuery(q) -> formatAnupana(matchedMedicine, forceHindi)
            else -> formatFullDetails(matchedMedicine, forceHindi)
        }
        val newContext = context.copy(focusedMedicine = matchedMedicine, lastShownMedicines = listOf(matchedMedicine))
        return Triple(reply, "db", newContext)
    }

    // Feature 12: "yes" confirms fuzzy suggestion
    if ((q == "yes" || q == "haan" || q == "ha") && context.lastFuzzySuggestion != null) {
        val sug = context.lastFuzzySuggestion
        val res = searchAllMedicines(sug, allMedicines)
        return if (res.isNotEmpty()) {
            val (r, c) = buildLocalAnswer(sug, res, context.copy(lastFuzzySuggestion = null))
            Triple(r, "db", c)
        } else {
            Triple("❌ Couldn't find results for \"$sug\". Try another term.", "db", context.copy(lastFuzzySuggestion = null))
        }
    }

    // Feature 2: Show more
    if (isShowMoreIntent(q) && context.pendingMoreMedicines.isNotEmpty()) {
        val next = context.pendingMoreMedicines.take(PAGE_SIZE)
        val rem  = context.pendingMoreMedicines.drop(PAGE_SIZE)
        val (r, c) = buildLocalAnswer("more results", next, context.copy(pendingMoreMedicines = rem), isNextPage = true)
        return Triple(r, "db", c)
    }

    // Follow-up (tell me about 3, dose, preparation…)
    val fu = tryHandleFollowUp(q, context)
    if (fu != null) return Triple(fu.first, "db", fu.second)

    // Feature 5: Hindi translation
    val tq = translateHindiQuery(q)

    // Feature 9: ingredient
    val ingFilter = extractIngredientFilter(tq)
    // Feature 8: reference
    val (refFilter, chapNum) = extractReferenceFilter(tq)
    // Feature 7: anupana
    val anuFilter = extractAnupanaFilter(tq)
    // Guna filter
    val (gunaFilter, qNoGuna) = extractGunaFilter(tq)

    // Base search
    var base: List<Medicine> = when {
        ingFilter != null ->
            allMedicines.filter { it.ingredients.lowercase().contains(ingFilter) }
        refFilter != null ->
            allMedicines.filter { m ->
                refFilter.keywords.any { k -> m.reference.lowercase().contains(k) } &&
                (chapNum == null || m.reference.contains("$chapNum", ignoreCase = true))
            }
        else -> searchAllMedicines(qNoGuna.ifBlank { tq }, allMedicines)
    }

    // Apply anupana filter
    if (anuFilter != null)
        base = base.filter { m -> anuFilter.keywords.any { k -> m.anupana.lowercase().contains(k.lowercase()) } }

    // Apply guna filter
    var relevant = if (gunaFilter != null)
        base.filter { m ->
            val t = "${m.benefits} ${m.ingredients} ${m.preparation}".lowercase()
            gunaFilter.patterns.any { p -> t.contains(p, ignoreCase = true) }
        }
    else base

    // Feature 6: Auto-detect dosha if no explicit guna
    val dosha = if (gunaFilter == null && anuFilter == null) detectDosha(tq) else null
    if (dosha != null && relevant.isNotEmpty()) {
        val dp = GUNA_FILTERS.firstOrNull { it.label == doshaGunaLabel(dosha) }?.patterns ?: emptyList()
        val df = relevant.filter { m ->
            val t = "${m.benefits} ${m.ingredients}".lowercase()
            dp.any { p -> t.contains(p, ignoreCase = true) }
        }
        if (df.isNotEmpty()) relevant = df
    }

    val exact = relevant.firstOrNull { it.name.lowercase() == q }
    val isListQuery = listOf("all", "list", "show", "medicines", "every").any { q.contains(it) } && 
                      listOf("for", "in", "of", "about").any { q.contains(it) }

    return when {
        relevant.isNotEmpty() -> {
            val use = if (exact != null) listOf(exact) else relevant
            val (reply, newCtx) = buildLocalAnswer(
                query = userText,
                results = use,
                context = context,
                gunaFilter = gunaFilter,
                anupanaFilter = anuFilter,
                referenceFilter = refFilter?.label,
                ingredientFilter = ingFilter,
                detectedDosha = dosha
            )
            if (isAi && !isListQuery) {
                val ai = callOpenRouterApi(userText, use.take(3), history, apiKey)
                val final = if (ai.startsWith("⚠️")) reply else "$reply\n\n---\n🤖 *Additional context:*\n$ai"
                Triple(final, "db", newCtx)
            } else {
                Triple(reply, "db", newCtx)
            }
        }
        else -> {
            // Feature 12: fuzzy suggest before AI
            val sug = findClosestDisease(q, allMedicines)
            if (sug != null) return Triple(
                "🤔 **Did you mean \"$sug\"?**\n\nSay **\"yes\"** to search, or rephrase.",
                "db",
                context.copy(lastFuzzySuggestion = sug)
            )
            val ai = callOpenRouterApi(userText, emptyList(), history, apiKey)
            if (ai.startsWith("⚠️"))
                Triple("❌ No results for \"$userText\".\n\nTry a disease name, symptom, or Sanskrit term.", "db", context)
            else
                Triple(ai, "ai", context)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  API helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun buildSystemPrompt(meds: List<Medicine>): String {
    val ctx = meds.joinToString("\n\n") { "NAME: ${it.name}\nBENEFITS: ${it.benefits}\nINGREDIENTS: ${it.ingredients}\nDOSE: ${it.dosage}" }
    return """
        You are an expert Ayurveda assistant for the Rasaushadhi app.
        Database context: $ctx.
        Rules: 1. Be concise. 2. List ingredients as bullets. 3. Answer based on context.
    """.trimIndent()
}

private suspend fun callOpenRouterApi(
    userQuery: String,
    meds: List<Medicine>,
    history: List<Pair<String, String>>,
    apiKey: String
): String = withContext(Dispatchers.IO) {
    try {
        val key = if (apiKey.isNotBlank()) apiKey else BuildConfig.OPENROUTER_API_KEY
        val msgs = JSONArray()
        msgs.put(JSONObject().put("role", "system").put("content", buildSystemPrompt(meds)))
        history.takeLast(2).forEach { (u, b) ->
            msgs.put(JSONObject().put("role", "user").put("content", u))
            msgs.put(JSONObject().put("role", "assistant").put("content", b))
        }
        msgs.put(JSONObject().put("role", "user").put("content", userQuery))
        val body = JSONObject().apply {
            put("model", "meta-llama/llama-3.3-70b-instruct")
            put("messages", msgs)
        }
        val conn = (URL("https://openrouter.ai/api/v1/chat/completions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $key")
            setRequestProperty("HTTP-Referer", "https://rasaushadhies.com")
            setRequestProperty("X-Title", "RASAADARSH")
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
        val code = conn.responseCode
        if (code == 200) {
            JSONObject(conn.inputStream.bufferedReader(Charsets.UTF_8).readText())
                .getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        } else {
            val err = try { conn.errorStream?.bufferedReader(Charsets.UTF_8)?.readText() ?: "" } catch (e: Exception) { "" }
            when (code) {
                401 -> "⚠️ Invalid API Key (401)."
                402 -> "⚠️ Insufficient Credits (402)."
                404 -> "⚠️ Model not found (404). Check model name."
                429 -> "⚠️ Rate limit (429). Try again shortly."
                else -> "⚠️ API Error $code: $err"
            }
        }
    } catch (e: Exception) { "⚠️ Connection failed: ${e.message}" }
}

private fun findMatchingMedicines(text: String, all: List<Medicine>): List<Medicine> {
    if (text.isBlank()) return emptyList()
    return all.filter { m ->
        val re = Regex("(?<![\\p{L}\\p{N}])${Regex.escape(m.name)}(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
        re.containsMatchIn(text) || (m.hindiName.isNotBlank() &&
            Regex("(?<![\\p{L}\\p{N}])${Regex.escape(m.hindiName)}(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE).containsMatchIn(text))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  AiChatbotScreen Composable
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatbotScreen(
    allMedicines: List<Medicine>,
    onBack: () -> Unit,
    onToggleBookmarkByName: (String) -> Unit,
    onMedicineClick: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("practitioner_prefs", android.content.Context.MODE_PRIVATE) }

    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var chatContext by remember { mutableStateOf(ChatContext()) }
    var customApiKey by remember { mutableStateOf(prefs.getString("openrouter_api_key", "") ?: "") }
    var showKeyDialog by remember { mutableStateOf(false) }

    var activeSpeakingMessageId by remember { mutableStateOf<String?>(null) }
    val tts = remember {
        var ttsInstance: android.speech.tts.TextToSpeech? = null
        ttsInstance = android.speech.tts.TextToSpeech(ctx) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                ttsInstance?.language = java.util.Locale("hi", "IN")
            }
        }
        ttsInstance
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.shutdown()
        }
    }

    LaunchedEffect(tts) {
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (activeSpeakingMessageId == utteranceId) {
                        activeSpeakingMessageId = null
                    }
                }
            }
            override fun onError(utteranceId: String?) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (activeSpeakingMessageId == utteranceId) {
                        activeSpeakingMessageId = null
                    }
                }
            }
        })
    }

    fun toggleSpeak(messageId: String, text: String) {
        if (activeSpeakingMessageId == messageId) {
            tts?.stop()
            activeSpeakingMessageId = null
        } else {
            tts?.stop()
            activeSpeakingMessageId = messageId
            
            val hasHindi = text.any { it.code in 0x0900..0x097F }
            if (hasHindi) {
                tts?.language = java.util.Locale("hi", "IN")
            } else {
                tts?.language = java.util.Locale.US
            }
            
            val cleanText = text
                .replace("**", "")
                .replace("*", "")
                .replace("`", "")
                .replace(Regex("(?m)^\\|.*\\|$"), "")
                .trim()
                
            tts?.speak(cleanText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, messageId)
        }
    }

    // Feature 3: Search history
    var searchHistory by remember { mutableStateOf(loadSearchHistory(prefs)) }

    // Feature 10: Conversation history pairs
    val convHistory = remember { mutableStateListOf<Pair<String, String>>() }

    val welcomeText = "🌿 Namaste! I am your Rasaushadhi assistant.\n\n" +
        "Search by disease, symptom, ingredient, or medicine name.\n\n" +
        "**Examples:**\n" +
        "• fever / बुखार / jwara\n" +
        "• hridayrog → all heart medicines\n" +
        "• sheeta virya skin disease\n" +
        "• medicine containing Tamra Bhasma\n" +
        "• from rasaratna chapter 19\n" +
        "• with ghee anupana\n\n" +
        "Then say **\"tell me about 2\"** for full details."

    val messages = remember {
        mutableStateListOf(ChatMessage(role = ChatRole.BOT, text = welcomeText))
    }

    // Feature 10: Restore previous conversation on launch
    LaunchedEffect(Unit) {
        val saved = buildRestoredMessages(prefs)
        if (saved.isNotEmpty()) messages.addAll(saved)
        loadConvHistory(prefs).forEach { convHistory.add(it) }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() || isLoading) return
        val input = userText.trim()
        inputText = ""
        isLoading = true

        // Feature 3: save to history
        if (searchHistory.firstOrNull() != input) {
            val updated = (listOf(input) + searchHistory).take(10)
            searchHistory = updated
            saveSearchHistory(prefs, updated)
        }

        messages.add(ChatMessage(role = ChatRole.USER, text = input))
        messages.add(ChatMessage(role = ChatRole.BOT, text = "", isLoading = true))

        scope.launch {
            val (reply, source, newCtx) = withContext(Dispatchers.Default) {
                resolveQuery(input, allMedicines, chatContext, convHistory.toList(), onToggleBookmarkByName, customApiKey)
            }
            val idx = messages.indexOfFirst { it.isLoading }
            if (idx >= 0) messages[idx] = ChatMessage(role = ChatRole.BOT, text = reply, source = source)
            chatContext = newCtx
            convHistory.add(Pair(input, reply))
            if (convHistory.size > 5) convHistory.removeAt(0)
            // Feature 10: persist
            saveConvHistory(prefs, convHistory.toList())
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            Box(modifier = Modifier.background(PrimaryGradient).windowInsetsPadding(WindowInsets.statusBars)) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Rasa Assistant", color = White, style = MaterialTheme.typography.titleLarge)
                            Text("Knowledge Engine Active", color = White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
                        }
                    },
                    actions = {
                        // Clear chat
                        IconButton(onClick = {
                            messages.clear()
                            convHistory.clear()
                            chatContext = ChatContext()
                            prefs.edit().remove("chat_history_v1").apply()
                            messages.add(ChatMessage(role = ChatRole.BOT, text = welcomeText))
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear Chat", tint = White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    windowInsets = WindowInsets(0, 0, 0, 0)
                )
            }
        },
        bottomBar = {
            val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
                if (res.resultCode == android.app.Activity.RESULT_OK) {
                    val spoken = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                    if (!spoken.isNullOrBlank()) inputText = spoken
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                // Feature 3: Recent history chips (when chatting) OR starter chips (first message)
                if (messages.size == 1) {
                    val starters = listOf("Jwara medicines","Hridayrog","Aamvata","Kasa treatment","Madhumeha","from rasaratna")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(starters) { s ->
                            SuggestionChip(
                                onClick = { sendMessage(s) },
                                label = { Text(s, fontSize = 12.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = GlassWhite, labelColor = PrimaryDarkGreen),
                                border = BorderStroke(1.dp, PrimaryGreen.copy(0.3f)),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                } else if (inputText.isBlank() && searchHistory.isNotEmpty()) {
                    Row(modifier = Modifier.padding(start = 16.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Recent:", fontSize = 11.sp, color = Muted, fontWeight = FontWeight.SemiBold)
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchHistory) { h ->
                            SuggestionChip(
                                onClick = { sendMessage(h) },
                                label = { Text(h, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = GlassWhite, labelColor = PrimaryDarkGreen),
                                border = BorderStroke(1.dp, PrimaryGreen.copy(0.3f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.widthIn(max = 150.dp)
                            )
                        }
                    }
                }

                ChatInputBar(
                    value = inputText,
                    onValueChange = { inputText = it },
                    onSend = { sendMessage(inputText) },
                    onVoiceInput = {
                        voiceLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your query...")
                        })
                    },
                    isLoading = isLoading
                )
            }
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        com.example.rasaushadhies.ui.theme.AppBackground(
            screenType = com.example.rasaushadhies.ui.theme.ScreenBackground.CHATBOT
        ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 12.dp + innerPadding.calculateTopPadding(),
                end = 16.dp,
                bottom = 8.dp + innerPadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(messages.size, key = { messages[it].id }) { i ->
                ChatBubble(
                    message = messages[i],
                    allMedicines = allMedicines,
                    onToggleBookmarkByName = onToggleBookmarkByName,
                    onMedicineClick = onMedicineClick,
                    activeSpeakingMessageId = activeSpeakingMessageId,
                    onSpeakToggle = { id, text -> toggleSpeak(id, text) }
                )
            }
        }
        } // end AppBackground
    }

    // API Key dialog
    if (showKeyDialog) {
        var tempKey by remember { mutableStateOf(customApiKey) }
        var visible by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showKeyDialog = false },
            title = { Text("Configure OpenRouter Key", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = tempKey, onValueChange = { tempKey = it },
                    label = { Text("OpenRouter API Key (sk-or-v1-...)") },
                    singleLine = true,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = PrimaryGreen)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryGreen, focusedLabelColor = PrimaryGreen),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    prefs.edit().putString("openrouter_api_key", tempKey.trim()).apply()
                    customApiKey = tempKey.trim()
                    showKeyDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)) { Text("Save", color = White) }
            },
            dismissButton = {
                TextButton(onClick = { showKeyDialog = false }) { Text("Cancel", color = PrimaryGreen) }
            },
            containerColor = White, shape = RoundedCornerShape(16.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ChatBubble — animated slide-in + inline medicine cards
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatBubble(
    message: ChatMessage,
    allMedicines: List<Medicine>,
    onToggleBookmarkByName: (String) -> Unit,
    onMedicineClick: (Int) -> Unit,
    activeSpeakingMessageId: String?,
    onSpeakToggle: (String, String) -> Unit
) {
    val isUser = message.role == ChatRole.USER
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(message.id) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 2 }, animationSpec = tween(300))
    ) {
        Row(
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            if (!isUser) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(PrimaryGradient), contentAlignment = Alignment.Center) {
                    Text("🌿", fontSize = 16.sp)
                }
                Spacer(Modifier.width(8.dp))
            }

            Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    ),
                    color = Color.Transparent,
                    shadowElevation = if (isUser) 2.dp else 0.dp,
                    modifier = Modifier.widthIn(max = 300.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .then(if (isUser) Modifier.background(PrimaryGradient) else Modifier.background(GlassWhite))
                            .border(1.dp, Brush.linearGradient(listOf(White.copy(0.2f), Color.Transparent)), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        if (message.isLoading) {
                            TypingIndicator()
                        } else {
                            val blocks = remember(message.text) { parseMessageBlocks(message.text) }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                blocks.forEach { block ->
                                    when (block) {
                                        is MessageContentBlock.TextBlock -> {
                                            Text(
                                                text = parseMarkdown(block.text),
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    color = if (isUser) White else TextPrimary,
                                                    fontSize = 15.sp
                                                )
                                            )
                                        }
                                        is MessageContentBlock.TableBlock -> {
                                            RenderTable(block, isUser)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (!isUser && !message.isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    ) {
                        if (message.source.isNotBlank()) {
                            Text(
                                text = if (message.source == "db") "⚡ VERIFIED RECORD" else "🤖 ASSISTANT REASONING",
                                style = MaterialTheme.typography.labelSmall.copy(color = Muted, fontSize = 9.sp)
                            )
                        }
                        val isSpeaking = activeSpeakingMessageId == message.id
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Speak text",
                            tint = PrimaryGreen,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onSpeakToggle(message.id, message.text) }
                        )
                    }
                }

                // Inline medicine cards
                if (!isUser && !message.isLoading) {
                    val matches = remember(message.text, allMedicines) { findMatchingMedicines(message.text, allMedicines) }
                    matches.forEach { med ->
                        MedicineInlineCard(
                            medicine = med,
                            onToggleBookmark = { onToggleBookmarkByName(med.name) },
                            onClick = { onMedicineClick(med.id) }
                        )
                    }
                }
            }

            if (isUser) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(Brush.linearGradient(listOf(AccentAmber, AccentAmberLight))),
                    contentAlignment = Alignment.Center
                ) { Text("👤", fontSize = 16.sp) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  TypingIndicator (sine-wave bouncing dots)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TypingIndicator() {
    val inf = rememberInfiniteTransition(label = "typing")
    val phase by inf.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(6.dp)) {
        for (i in 0 until 3) {
            val off = i * (Math.PI / 3)
            val y = kotlin.math.sin(phase.toDouble() + off).toFloat() * 6.dp.value
            val a = ((kotlin.math.sin(phase.toDouble() + off) + 1.0) / 2.0 * 0.6 + 0.4).toFloat()
            Box(Modifier.size(8.dp).offset(y = y.dp).background(PrimaryGreen.copy(alpha = a), CircleShape))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  MedicineInlineCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MedicineInlineCard(medicine: Medicine, onToggleBookmark: () -> Unit, onClick: () -> Unit) {
    var bookmarked by remember(medicine.isBookmarked) { mutableStateOf(medicine.isBookmarked) }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.padding(top = 8.dp).widthIn(max = 300.dp).clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(Modifier.size(28.dp).background(PrimaryGreen.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                        Text("🌿", fontSize = 14.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(medicine.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = PrimaryDarkGreen, fontSize = 14.sp))
                        if (medicine.hindiName.isNotBlank()) Text(medicine.hindiName, style = MaterialTheme.typography.bodySmall.copy(color = Muted, fontSize = 11.sp))
                    }
                }
                val sc by animateFloatAsState(if (bookmarked) 1.2f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "bk")
                IconButton(onClick = { bookmarked = !bookmarked; onToggleBookmark() }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (bookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = null, tint = if (bookmarked) AccentAmber else Muted.copy(0.4f),
                        modifier = Modifier.size(18.dp).scale(sc)
                    )
                }
            }
            if (medicine.benefits.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(medicine.benefits, style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary.copy(0.8f), fontSize = 12.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = DividerColor.copy(0.5f))
            TextButton(onClick = onClick, contentPadding = PaddingValues(0.dp), modifier = Modifier.align(Alignment.End).height(28.dp)) {
                Text("View Details", style = MaterialTheme.typography.labelMedium.copy(color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp))
                Icon(Icons.Default.ChevronRight, null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ChatInputBar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatInputBar(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit, onVoiceInput: () -> Unit, isLoading: Boolean) {
    Surface(color = GlassWhite, shadowElevation = 12.dp, modifier = Modifier.fillMaxWidth().border(1.dp, White.copy(0.5f))) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 10.dp, bottom = 20.dp).windowInsetsPadding(WindowInsets.navigationBars),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(0.dp),
                border = BorderStroke(1.dp, DividerColor),
                modifier = Modifier.weight(1f)
            ) {
                TextField(
                    value = value, onValueChange = onValueChange,
                    placeholder = { Text("Ask about medicines or health...", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Send
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSend = { onSend() }
                    )
                )
            }
            // Feature 12: Pulsing mic button
            val micActive = !isLoading
            val infiniteMic = rememberInfiniteTransition(label = "mic")
            val pulseScale by infiniteMic.animateFloat(
                initialValue = 1f, targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    tween(700, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse
                ),
                label = "pulseScale"
            )
            val pulseAlpha by infiniteMic.animateFloat(
                initialValue = 0.5f, targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    tween(700, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse
                ),
                label = "pulseAlpha"
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp)
            ) {
                // Pulsing ring behind mic icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .scale(pulseScale)
                        .background(PrimaryGreen.copy(alpha = pulseAlpha), CircleShape)
                )
                IconButton(onClick = onVoiceInput, enabled = !isLoading, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Mic, "Voice", tint = PrimaryGreen)
                }
            }
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = onSend, enabled = value.isNotBlank() && !isLoading,
                modifier = Modifier.size(48.dp).then(
                    if (value.isNotBlank()) Modifier.background(PrimaryGradient, CircleShape)
                    else Modifier.background(Muted.copy(0.2f), CircleShape)
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null, tint = White)
            }
        }
    }
}

fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        val boldParts = text.split("**")
        boldParts.forEachIndexed { boldIndex, boldPart ->
            val isBold = boldIndex % 2 == 1
            val italicParts = boldPart.split("*")
            italicParts.forEachIndexed { italicIndex, italicPart ->
                val isItalic = italicIndex % 2 == 1
                val style = when {
                    isBold && isItalic -> SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
                    isBold -> SpanStyle(fontWeight = FontWeight.Bold)
                    isItalic -> SpanStyle(fontStyle = FontStyle.Italic)
                    else -> null
                }
                if (style != null) {
                    pushStyle(style)
                    append(italicPart)
                    pop()
                } else {
                    append(italicPart)
                }
            }
        }
    }
}

sealed class MessageContentBlock {
    data class TextBlock(val text: String) : MessageContentBlock()
    data class TableBlock(val headers: List<String>, val rows: List<List<String>>) : MessageContentBlock()
}

fun parseMessageBlocks(text: String): List<MessageContentBlock> {
    val blocks = mutableListOf<MessageContentBlock>()
    val lines = text.split("\n")
    var inTable = false
    var headers = listOf<String>()
    val rows = mutableListOf<List<String>>()
    var currentTextAccumulator = StringBuilder()

    fun flushTextAccumulator() {
        if (currentTextAccumulator.isNotEmpty()) {
            val content = currentTextAccumulator.toString().trim()
            if (content.isNotEmpty()) {
                blocks.add(MessageContentBlock.TextBlock(content))
            }
            currentTextAccumulator = StringBuilder()
        }
    }

    var i = 0
    while (i < lines.size) {
        val line = lines[i].trim()
        if (line.startsWith("|") && line.endsWith("|")) {
            val cells = line.split("|").map { it.trim() }.drop(1).dropLast(1)
            
            if (!inTable) {
                val nextLine = if (i + 1 < lines.size) lines[i + 1].trim() else ""
                val isSeparator = nextLine.startsWith("|") && nextLine.contains("-") && nextLine.endsWith("|")
                if (isSeparator) {
                    flushTextAccumulator()
                    inTable = true
                    headers = cells
                    rows.clear()
                    i += 2 // skip header and separator lines
                    continue
                }
            } else {
                rows.add(cells)
                i++
                continue
            }
        }
        
        if (inTable) {
            blocks.add(MessageContentBlock.TableBlock(headers, rows.toList()))
            inTable = false
        }
        
        currentTextAccumulator.append(lines[i]).append("\n")
        i++
    }
    
    if (inTable) {
        blocks.add(MessageContentBlock.TableBlock(headers, rows.toList()))
    }
    flushTextAccumulator()
    
    return blocks
}

@Composable
private fun RenderTable(block: MessageContentBlock.TableBlock, isUser: Boolean) {
    val borderColor = if (isUser) White.copy(alpha = 0.5f) else DividerColor
    val headerBg = if (isUser) White.copy(alpha = 0.15f) else PrimaryGreen.copy(alpha = 0.08f)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    ) {
        // Headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg)
                .padding(vertical = 8.dp, horizontal = 12.dp)
        ) {
            block.headers.forEach { header ->
                Text(
                    text = parseMarkdown(header),
                    fontWeight = FontWeight.Bold,
                    color = if (isUser) White else PrimaryDarkGreen,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                )
            }
        }
        
        HorizontalDivider(color = borderColor)
        
        // Rows
        block.rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { cell ->
                    Text(
                        text = parseMarkdown(cell),
                        color = if (isUser) White else TextPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    )
                }
            }
            if (rowIndex < block.rows.lastIndex) {
                HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
            }
        }
    }
}

