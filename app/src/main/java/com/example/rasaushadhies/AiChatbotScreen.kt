package com.example.rasaushadhies

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
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

// Removed massive FULL_DB_CONTEXT_STRING to prevent LMK/OOM

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
    val focusedMedicine: Medicine? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatbotScreen(
    allMedicines: List<Medicine>,
    onBack: () -> Unit,
    onToggleBookmarkByName: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var chatContext by remember { mutableStateOf(ChatContext()) }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                role = ChatRole.BOT,
                text = "🌿 Namaste! I am your Rasaushadhi assistant.\n\nSearch by medicine name, symptom, or disease. Then ask follow-ups like:\n• \"tell me about 2\"\n• \"preparation method\"\n• \"what is the difference between...\""
            )
        )
    }

    val conversationHistory = remember { mutableStateListOf<Pair<String, String>>() }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() || isLoading) return
        val currentInput = userText
        inputText = ""
        isLoading = true

        messages.add(ChatMessage(role = ChatRole.USER, text = currentInput))
        messages.add(ChatMessage(role = ChatRole.BOT, text = "", isLoading = true))

        coroutineScope.launch {
            val result = withContext(Dispatchers.Default) {
                resolveQuery(
                    userText = currentInput,
                    allMedicines = allMedicines,
                    context = chatContext,
                    history = conversationHistory.toList(),
                    onToggleBookmarkByName = onToggleBookmarkByName
                )
            }

            val (reply, source, newContext) = result

            val idx = messages.indexOfFirst { it.isLoading }
            if (idx >= 0) {
                messages[idx] = ChatMessage(role = ChatRole.BOT, text = reply, source = source)
            }

            chatContext = newContext
            conversationHistory.add(Pair(currentInput, reply))
            if (conversationHistory.size > 5) conversationHistory.removeAt(0)
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    windowInsets = WindowInsets(0, 0, 0, 0)
                )
            }
        },
        bottomBar = {
            val voiceLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == android.app.Activity.RESULT_OK) {
                    val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                    if (!spokenText.isNullOrBlank()) {
                        inputText = spokenText
                    }
                }
            }

            ChatInputBar(
                value = inputText,
                onValueChange = { inputText = it },
                onSend = { sendMessage(inputText) },
                onVoiceInput = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your query...")
                    }
                    voiceLauncher.launch(intent)
                },
                isLoading = isLoading
            )
        },
        containerColor = BackgroundColor,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(message = msg)
                }
            }
        }
    }
}

private fun isAiIntent(q: String): Boolean {
    val comparisonWords = listOf("difference", "compare", "versus", "vs", "better", "contrast", "between")
    val reasoningWords = listOf("why", "how", "mechanism", "action", "work", "explain", "reason", "cause")
    val complexWords = listOf("best", "top", "rank", "among", "considering", "recommend", "suggest", "which one")
    
    val words = q.lowercase().split(Regex("\\s+"))
    return comparisonWords.any { q.contains(it) } || 
           reasoningWords.any { q.contains(it) } || 
           complexWords.any { q.contains(it) }
}

private suspend fun resolveQuery(
    userText: String,
    allMedicines: List<Medicine>,
    context: ChatContext,
    history: List<Pair<String, String>>,
    onToggleBookmarkByName: (String) -> Unit
): Triple<String, String, ChatContext> {

    val q = userText.trim().lowercase()

    // 0. Handle Save/Bookmark Intent
    if (isSaveIntent(q)) {
        val focused = context.focusedMedicine
        return if (focused != null) {
            // Found a medicine in current context to save
            onToggleBookmarkByName(focused.name) 
            Triple("🌿 **Saved ${focused.name} to your Cabinet.**\n\nYou can find it anytime in the 'Saved' section.", "db", context)
        } else {
            Triple("Which medicine would you like me to save? Please mention its name first.", "db", context)
        }
    }

    // 1. Check Follow-ups (tell me about 2, etc) -> DB
    val followUpResult = tryHandleFollowUp(q, context)
    if (followUpResult != null) {
        return Triple(followUpResult.first, "db", followUpResult.second)
    }

    // 2. Fetch relevant from provided list
    val query = userText.lowercase().trim()
    val relevant = if (query.isEmpty()) emptyList() else allMedicines.filter { 
        it.name.lowercase().contains(query) || 
        it.hindiName.lowercase().contains(query) ||
        it.benefits.lowercase().contains(query)
    }.take(5)
    
    val exactMatch = relevant.firstOrNull { it.name.lowercase() == q }
    val wordsCount = q.split(Regex("\\s+")).size

    // 3. CORE IDEA: When to use Local DB
    // - Exact Name Match
    // - Simple Details (handled in search/followup)
    // - Short Queries (<= 3 words) AND not a hard AI intent
    // - Symptom search (handled by 'relevant' list)
    val isSimpleLookup = exactMatch != null || 
                         (relevant.isNotEmpty() && wordsCount <= 3 && !isAiIntent(q))

    return if (isSimpleLookup) {
        val listToUse = if (exactMatch != null) listOf(exactMatch) else relevant
        val (reply, newContext) = buildLocalAnswer(userText, listToUse, context)
        Triple(reply, "db", newContext)
    } else {
        // 4. COMPLEX/HARD CASES -> AI Assistant
        // If DB is empty, or query is complex, or Comparison/Reasoning intent detected
        val reply = callOpenRouterApi(userText, relevant, history)
        
        // --- OFFLINE/ERROR FALLBACK ---
        if (reply.startsWith("⚠️") && relevant.isNotEmpty()) {
            val (fallbackReply, newContext) = buildLocalAnswer(userText, relevant, context)
            Triple("📡 *AI Offline - showing local results:*\n\n$fallbackReply", "db", newContext)
        } else {
            val newContext = if (relevant.isNotEmpty())
                context.copy(lastShownMedicines = relevant, focusedMedicine = if (relevant.size == 1) relevant[0] else null)
            else context
            Triple(reply, "ai", newContext)
        }
    }
}

private fun buildLocalAnswer(
    query: String,
    results: List<Medicine>,
    context: ChatContext
): Pair<String, ChatContext> {
    if (results.isEmpty()) {
        return Pair("I couldn't find a specific medicine matching \"$query\". Try asking in more detail so I can help.", context)
    }

    if (results.size == 1) {
        val m = results[0]
        return Pair(formatFullDetails(m), context.copy(focusedMedicine = m, lastShownMedicines = results))
    }

    val reply = buildString {
        append("🔍 Found ${results.size} medicines matching your query:\n\n")
        results.forEachIndexed { index, m ->
            append("${index + 1}. **${m.name}** — ${m.benefits.take(60)}...\n")
        }
        append("\n💡 Ask \"tell me about 1\" or \"preparation method of the second one\" for more details.")
    }
    return Pair(reply, context.copy(lastShownMedicines = results, focusedMedicine = null))
}

private fun tryHandleFollowUp(q: String, context: ChatContext): Pair<String, ChatContext>? {
    val list = context.lastShownMedicines
    val focused = context.focusedMedicine
    if (list.isEmpty() && focused == null) return null

    val numberWords = mapOf(
        "1" to 0, "one" to 0, "first" to 0, "pehla" to 0,
        "2" to 1, "two" to 1, "second" to 1, "doosra" to 1,
        "3" to 2, "three" to 2, "third" to 2, "teesra" to 2,
        "4" to 3, "four" to 3, "fourth" to 3,
        "5" to 4, "five" to 4, "fifth" to 4,
        "6" to 5, "six" to 5, "sixth" to 5
    )

    for ((word, idx) in numberWords) {
        if (q.contains(word) && idx < list.size) {
            val m = list[idx]
            val reply = when {
                isPreparationQuery(q) -> formatPreparation(m)
                isDoseQuery(q) -> formatDose(m)
                isIngredientQuery(q) -> formatIngredients(m)
                isBenefitQuery(q) -> formatBenefits(m)
                else -> formatFullDetails(m)
            }
            return Pair(reply, context.copy(focusedMedicine = m))
        }
    }

    if (focused != null) {
        val reply = when {
            isPreparationQuery(q) -> formatPreparation(focused)
            isDoseQuery(q) -> formatDose(focused)
            isIngredientQuery(q) -> formatIngredients(focused)
            isBenefitQuery(q) -> formatBenefits(focused)
            isFullDetailsQuery(q) -> formatFullDetails(focused)
            else -> null
        }
        if (reply != null) return Pair(reply, context)
    }
    return null
}

private fun isSaveIntent(q: String) = listOf("save", "bookmark", "add to cabinet", "remember this", "keep this", "save this").any { q.contains(it) }
private fun isPreparationQuery(q: String) = listOf("preparation", "prepare", "how to make", "method", "process").any { q.contains(it) }
private fun isDoseQuery(q: String) = listOf("dose", "dosage", "matra", "how much", "amount").any { q.contains(it) }
private fun isIngredientQuery(q: String) = listOf("ingredient", "samagri", "contains", "made of").any { q.contains(it) }
private fun isBenefitQuery(q: String) = listOf("benefit", "use", "fayde", "treats", "helps").any { q.contains(it) }
private fun isFullDetailsQuery(q: String) = listOf("detail", "full", "complete", "about", "tell me").any { q.contains(it) }

private fun formatFullDetails(m: Medicine): String = buildString {
    append("🌿 **${m.name}**\n\n")
    append("📌 **Benefits:** ${m.benefits}\n\n")
    append("🧪 **Ingredients:** ${m.ingredients}\n\n")
    append("💊 **Dose:** ${m.dosage}\n")
    if (m.anupana.isNotBlank()) append("Anupana: ${m.anupana}\n")
    append("\n📋 **Preparation:** ${m.preparation}")
}

private fun formatPreparation(m: Medicine) = "📋 **${m.name} — Preparation**\n\n${m.preparation}"
private fun formatDose(m: Medicine) = "💊 **${m.name} — Dose**\n\nDose: ${m.dosage}\nAnupana: ${m.anupana}"
private fun formatIngredients(m: Medicine) = "🧪 **${m.name} — Ingredients**\n\n${m.ingredients}"
private fun formatBenefits(m: Medicine) = "✨ **${m.name} — Benefits**\n\n${m.benefits}"

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.USER
    Row(
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        if (!isUser) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(PrimaryGradient), 
                contentAlignment = Alignment.Center
            ) {
                Text("🌿", fontSize = 16.sp)
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp, 
                    topEnd = 16.dp, 
                    bottomStart = if (isUser) 16.dp else 4.dp, 
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = Color.Transparent,
                shadowElevation = if (isUser) 2.dp else 0.dp,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Box(
                    modifier = Modifier
                        .then(
                            if (isUser) Modifier.background(PrimaryGradient)
                            else Modifier.background(GlassWhite)
                        )
                        .border(
                            width = 1.dp, 
                            brush = Brush.linearGradient(listOf(White.copy(0.2f), Color.Transparent)),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp)
                ) {
                    if (message.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PrimaryGreen, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = message.text, 
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = if (isUser) White else TextPrimary,
                                fontSize = 15.sp
                            )
                        )
                    }
                }
            }
            if (!isUser && message.source.isNotBlank() && !message.isLoading) {
                Text(
                    text = if (message.source == "db") "⚡ VERIFIED RECORD" else "🤖 ASSISTANT REASONING",
                    style = MaterialTheme.typography.labelSmall.copy(color = Muted, fontSize = 9.sp),
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(
                    Brush.linearGradient(listOf(AccentAmber, AccentAmberLight))
                ), 
                contentAlignment = Alignment.Center
            ) {
                Text("👤", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String, 
    onValueChange: (String) -> Unit, 
    onSend: () -> Unit, 
    onVoiceInput: () -> Unit,
    isLoading: Boolean
) {
    Surface(
        color = GlassWhite,
        shadowElevation = 12.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, White.copy(alpha = 0.5f))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .windowInsetsPadding(WindowInsets.navigationBars), 
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
                    value = value, 
                    onValueChange = onValueChange,
                    placeholder = { Text("Ask about medicines or health...", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    )
                )
            }
            IconButton(
                onClick = onVoiceInput,
                enabled = !isLoading,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = PrimaryGreen)
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onSend, 
                enabled = value.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(48.dp)
                    .then(
                        if (value.isNotBlank()) Modifier.background(PrimaryGradient, CircleShape)
                        else Modifier.background(Muted.copy(0.2f), CircleShape)
                    )
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = White)
            }
        }
    }
}

private fun buildSystemPrompt(relevantMedicines: List<Medicine>): String {

    // Removed the greedy context builder that used the entire DB.
    // Instead, only use the medicines found by the local search.
    val medicineContext = relevantMedicines.joinToString("\n\n") { m ->
        "NAME: ${m.name}\nBENEFITS: ${m.benefits}\nINGREDIENTS: ${m.ingredients}\nDOSE: ${m.dosage}"
    }

    return """
        You are an expert Ayurveda assistant for the Rasaushadhi app. 
        Database context: $medicineContext. 
        
        Rules:
        1. Be very concise.
        2. Format ingredients as a bulleted list.
        3. Use context to answer complex questions across all medicines.
    """.trimIndent()
}

private suspend fun callOpenRouterApi(
    userQuery: String,
    relevantMedicines: List<Medicine>,
    history: List<Pair<String, String>>
): String = withContext(Dispatchers.IO) {
    try {
        val apiKey = BuildConfig.OPENROUTER_API_KEY
        val url = URL("https://openrouter.ai/api/v1/chat/completions")
        
        val messages = JSONArray()
        
        // System Prompt with context
        val sysPrompt = buildSystemPrompt(relevantMedicines)
        messages.put(JSONObject().put("role", "system").put("content", sysPrompt))

        // History
        history.takeLast(2).forEach { (u, b) ->
            messages.put(JSONObject().put("role", "user").put("content", u))
            messages.put(JSONObject().put("role", "assistant").put("content", b))
        }

        // Current Query
        messages.put(JSONObject().put("role", "user").put("content", userQuery))

        val requestBody = JSONObject().apply {
            put("model", "google/gemini-2.0-flash-001")
            put("messages", messages)
        }

        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("HTTP-Referer", "https://rasaushadhies.com")
            setRequestProperty("X-Title", "RASAADARSH")
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 15_000
        }

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(requestBody.toString()) }

        val code = conn.responseCode
        if (code == 200) {
            val response = conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
            JSONObject(response)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } else {
            val errorBody = try {
                conn.errorStream?.bufferedReader(Charsets.UTF_8)?.readText() ?: "(no error body)"
            } catch (e: Exception) { "(could not read error body)" }

            when (code) {
                401 -> "⚠️ Invalid OpenRouter Key (401). Check your sk-or-v1 key."
                402 -> "⚠️ Insufficient Credits (402). OpenRouter account needs fuel."
                429 -> "⚠️ OpenRouter Rate Limit (429). Slow down a bit."
                else -> "⚠️ OpenRouter Error $code: $errorBody"
            }
        }
    } catch (e: Exception) { "⚠️ Connection failed: ${e.message}" }
}
