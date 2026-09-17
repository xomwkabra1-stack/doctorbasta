package com.example.data.repository

import com.example.BuildConfig
import com.example.data.local.ChatDao
import com.example.data.model.ChatMessage
import com.example.data.model.GeminiContent
import com.example.data.model.GeminiGenerationConfig
import com.example.data.model.GeminiPart
import com.example.data.model.GeminiRequest
import com.example.data.remote.GeminiClient
import com.example.data.security.SecureKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ChatRepository(
    private val chatDao: ChatDao,
    private val secureKeyManager: SecureKeyManager
) {

    val allMessages: Flow<List<ChatMessage>> = chatDao.getAllMessages()

    suspend fun saveMessage(message: ChatMessage): Long = withContext(Dispatchers.IO) {
        chatDao.insertMessage(message)
    }

    suspend fun updateMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        chatDao.updateMessage(message)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        chatDao.clearChat()
    }

    suspend fun getMessageCount(): Int = withContext(Dispatchers.IO) {
        chatDao.getMessageCount()
    }

    /**
     * Resolves the active API key with strict security:
     * 1. User-configured Keystore encrypted key
     * 2. BuildConfig fallback (if provided during build)
     */
    fun getActiveApiKey(): String? {
        val userKey = secureKeyManager.getApiKey()
        if (!userKey.isNullOrBlank()) {
            return userKey.trim()
        }
        val buildKey = BuildConfig.GEMINI_API_KEY
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey.trim()
        }
        return null
    }

    fun getSelectedModel(): String {
        return secureKeyManager.getSelectedModel()
    }

    suspend fun testApiKeyConnection(rawApiKey: String, model: String): Result<String> = withContext(Dispatchers.IO) {
        val key = rawApiKey.trim()
        if (key.isBlank()) {
            return@withContext Result.failure(Exception("تکایە کلیلی API بنووسە پێش تاقیکردنەوە."))
        }

        try {
            val testRequest = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = "سڵاو دکتۆر"))
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.2f,
                    maxOutputTokens = 20
                )
            )

            val response = GeminiClient.service.generateContent(model, key, testRequest)
            if (response.isSuccessful && response.body()?.candidates?.isNotEmpty() == true) {
                Result.success("پەیوەندی سەرکەوتوو بوو! مۆدێلی $model و کلیلەکە بە تەواوی ئامادەن.")
            } else {
                val errorCode = response.code()
                val kurdishError = when (errorCode) {
                    400 -> "داواکارییەکە ڕەتکرایەوە (400). تکایە دڵنیابەرەوە لە دروستی مۆدێلی $model."
                    401, 403 -> "کلیلی API نادروستە یان ڕێگەی پێنەدراوە (401/403). تکایە کلیلەکەت لە Google AI Studio پپشکنە."
                    404 -> "مۆدێلی داواکراو ($model) لەم بەستەرەدا نەدۆزرایەوە (404)."
                    429 -> "سنووری داواکارییەکانی ئەم کلیلە تەواو بووە (429 Quota Exceeded)."
                    500, 503 -> "سیستەمی Gemini کاتییە لەکارکەوتووە، تکایە دواتر هەوڵ بدەرەوە."
                    else -> "هەڵەیەک لە پەیوەندیدا ڕوویدا ($errorCode)."
                }
                Result.failure(Exception(kurdishError))
            }
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "هەڵەی تۆڕ"
            Result.failure(Exception("نەتوانرا پەیوەندی ببەسترێت. تکایە هێڵی ئینتەرنێتەکەت بپشکنە: $msg"))
        }
    }

    suspend fun consultDrBasta(
        userPrompt: String,
        recentHistory: List<ChatMessage>,
        targetMessageIdToUpdate: Long? = null
    ): ChatMessage = withContext(Dispatchers.IO) {
        // Immediate Crisis & Safety verification
        val isCrisis = isEmergencyCrisis(userPrompt)
        if (isCrisis) {
            val crisisResponse = generateEmergencyCrisisResponse()
            val msg = if (targetMessageIdToUpdate != null) {
                ChatMessage(
                    id = targetMessageIdToUpdate,
                    text = crisisResponse,
                    sender = "counselor",
                    timestamp = System.currentTimeMillis(),
                    isCrisisWarning = true
                ).also { updateMessage(it) }
            } else {
                val newMsg = ChatMessage(
                    text = crisisResponse,
                    sender = "counselor",
                    timestamp = System.currentTimeMillis(),
                    isCrisisWarning = true
                )
                val id = saveMessage(newMsg)
                newMsg.copy(id = id)
            }
            return@withContext msg
        }

        val apiKey = getActiveApiKey()
        val model = getSelectedModel()

        if (!apiKey.isNullOrBlank()) {
            try {
                val systemPrompt = getDrBastaSystemPrompt()
                
                // Build history context (up to last 8 messages)
                val historyContents = mutableListOf<GeminiContent>()
                val limitedHistory = recentHistory.takeLast(8)
                for (item in limitedHistory) {
                    val role = if (item.isUser) "user" else "model"
                    historyContents.add(
                        GeminiContent(
                            role = role,
                            parts = listOf(GeminiPart(text = item.text))
                        )
                    )
                }

                // Append current user prompt
                historyContents.add(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = userPrompt))
                    )
                )

                val request = GeminiRequest(
                    contents = historyContents,
                    systemInstruction = GeminiContent(
                        parts = listOf(GeminiPart(text = systemPrompt))
                    ),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.75f,
                        maxOutputTokens = 1500
                    )
                )

                val response = GeminiClient.service.generateContent(model, apiKey, request)
                if (response.isSuccessful && response.body() != null) {
                    val candidate = response.body()?.candidates?.firstOrNull()
                    val replyText = candidate?.content?.parts?.firstOrNull()?.text?.trim()
                    if (!replyText.isNullOrBlank()) {
                        val finalAnswer = cleanAndFormatKurdishResponse(replyText)
                        val counselorMsg = if (targetMessageIdToUpdate != null) {
                            ChatMessage(
                                id = targetMessageIdToUpdate,
                                text = finalAnswer,
                                sender = "counselor",
                                timestamp = System.currentTimeMillis()
                            ).also { updateMessage(it) }
                        } else {
                            val newMsg = ChatMessage(
                                text = finalAnswer,
                                sender = "counselor",
                                timestamp = System.currentTimeMillis()
                            )
                            val id = saveMessage(newMsg)
                            newMsg.copy(id = id)
                        }
                        return@withContext counselorMsg
                    }
                }
            } catch (e: Exception) {
                // Fall back gracefully to rich internal Kurdish counseling engine
            }
        }

        // Comprehensive Kurdish counselor fallback engine
        val fallbackText = generateRichKurdishCounselorAdvice(userPrompt)
        val counselorMsg = if (targetMessageIdToUpdate != null) {
            ChatMessage(
                id = targetMessageIdToUpdate,
                text = fallbackText,
                sender = "counselor",
                timestamp = System.currentTimeMillis()
            ).also { updateMessage(it) }
        } else {
            val newMsg = ChatMessage(
                text = fallbackText,
                sender = "counselor",
                timestamp = System.currentTimeMillis()
            )
            val id = saveMessage(newMsg)
            newMsg.copy(id = id)
        }
        return@withContext counselorMsg
    }

    private fun cleanAndFormatKurdishResponse(text: String): String {
        return text
            .replace("As an AI", "")
            .replace("وەک زیرەکییەکی دەستکرد", "")
            .replace("وەک مۆدێلێکی زمانی", "")
            .trim()
    }

    private fun isEmergencyCrisis(text: String): Boolean {
        val lower = text.lowercase()
        val crisisKeywords = listOf(
            "خۆکوشتن", "خۆم دەکوژم", "خۆم دەخنکێنم", "کۆتایی بە ژیانم",
            "ئازاری خۆم دەدەم", "خۆئازاردان", "دەمرم", "توندوتیژی",
            "ئەمەوێ بمرم", "نامەوێ بژیم", "ژیانم بێ مانا بووە و خۆم لەناو دەبەم",
            "گیانی خۆم دەستێنم", "تەرمم", "دەست بەسەر ژیانمدا بگرم بە مەرگ"
        )
        return crisisKeywords.any { lower.contains(it) }
    }

    private fun generateEmergencyCrisisResponse(): String {
        return """
برا یان خوشکی ئازیزم، من زۆر نیگەرانتم و ئێستا سەلامەتیی تۆ تاکە شتی گرنگە بۆ هەموومان! تکایە لە دڵەوە گوێم لێ بگرە: ❤️

لەم ساتەدا بیرکردنەوەکانت زۆر تاریک و قورسن، بەڵام ئەمە کاتییە و دەڕوات. تکایە بە تەنیا مەمێنەرەوە. لەبەر ئەوەی من ڕاوێژکارێکی دەروونیی ژیری دەستکردم و لە باری فریاکەوتنی خێرادا ناتوانم ڕاستەوخۆ دەستت بگرم:

١. دەستبەجێ پەیوەندی بە نزیکترین و دڵسۆزترین کەسی ژیانتەوە بکە (دایک، باوک، هاوسەر، برایەک یان هاوڕێیەکی نزیکت) و پێی بڵێ: «پێویستم بەوەیە کەسێک ئێستا لە تەنیشتم بێت».
٢. سەردانی نزیکترین نەخۆشخانەی فریاکەوتن (الطوارئ) بکە لە شارەکەتدا، یان دەستبەجێ پەیوەندی بە ژمارە فریاگوزارییەکانی فریاکەوتن (١٢٢ یان ١١٢) بکە.
٣. لە هەر ئامراز و کەلوپەلێکی مەترسیدار دووربکەوە، چەند هەناسەیەکی قووڵ هەڵمژە و لە شوێنێک دابنیشە کە کەسانی تری لێبێت.

هیچ کێشەیەک و هیچ ناڕەحەتییەک ئەوەندە گەورە نییە کە شایەنی کۆتاییهێنان بە گیانی تۆ بێت. هەمیشە دەرگای چارەسەر هەیە کاتێک کەسێکی پسپۆڕ گوێت لێ دەگرێت. ژیانی تۆ زۆر زۆر بەنرخە، تکایە سەلامەتیت بپارێزە!
        """.trimIndent()
    }

    private fun getDrBastaSystemPrompt(): String {
        return """
You are the authentic, highly intelligent, warm, and deeply experienced psychological counselor:
"دکتۆر بەستە | دکتۆری دەرونی 🧠"

CORE MISSION:
You are created specifically to help Kurdish people understand and navigate their emotions, thoughts, behaviors, relationships, anxiety, overthinking, stress, sadness, anger, low confidence, loneliness, and everyday psychological struggles.

KURDISH LANGUAGE MASTERY (CRITICAL):
- Always speak exclusively in natural, fluent, idiomatically authentic Sorani Kurdish (کوردیی سۆرانی).
- Master colloquial Kurdish phrasing, Kurdish idioms, metaphors, slang, informal tones, and cultural context.
- Write with correct Kurdish orthography, spelling, grammar, and punctuation (e.g., using «ڵ»، «ڕ»، «ێ»، «ۆ»، «ڤ» properly).
- NEVER do literal or word-for-word translation from English.
- Avoid stiff, robotic, academic, or textbook Arabic-influenced phrasing; sound like an articulate, compassionate, experienced Kurdish psychologist talking directly over a warm cup of tea.
- Vary your openings! DO NOT start every message with the same repetitive phrase like "سڵاو لە چاوت براکەم". Use natural, diverse, engaging conversational beginnings.
- Never say "وەک ژیرییەکی دەستکرد" (As an AI). You are Dr. Basta.

DEPTH & SUBSTANCE (DO NOT BE OVERLY SHORT):
- Do NOT provide superficial, generic, 2-line answers.
- For typical psychological inquiries, deliver a thorough, rich, actionable consultation ranging between 2 to 6 well-structured paragraphs (around 150 to 400 Kurdish words).
- If the user explicitly asks for a quick short tip, adapt and be concise. If they ask for detailed analysis, provide comprehensive, structured insights.

CONSULTATION FRAMEWORK FOR EVERY RESPONSE:
1. Direct Empathy & Validation: Identify what the user is truly going through beneath their words. Validate their feeling without sounding condescending.
2. Clear Psychological Insight: Explain simply and accurately what is occurring in the brain, nervous system, or psychological pattern (e.g. why the brain defaults to worst-case scenarios in anxiety).
3. 3 to 5 Concrete Practical Steps: Give realistic, executable actions they can do right now and in their daily routine.
4. Relatable Real-Life Example: Share a short, vivid, down-to-earth example or metaphor that clicks immediately in Kurdish culture.
5. Thoughtful Follow-up Question: Conclude with a warm, open question that invites further reflection.

HUMOR & TONE:
- Infuse friendly, natural Kurdish humor and street-level "عەشایەری" warmth when appropriate (e.g. comparing an overthinking brain to a smartphone running 100 apps in the background, making the battery heat up).
- STRICT SAFETY BAN: NEVER make jokes about suicide, self-harm, trauma, abuse, domestic violence, death, grief, severe clinical illness, or genuine suffering.

ETHICAL & CLINICAL BOUNDARIES:
- Never claim to diagnose a psychiatric disorder with absolute certainty over chat. Instead use respectful exploratory language: "ئەم نیشانانە دەکرێت پەیوەندییان بە ... هەبێت، بەڵام بۆ دەستنیشانکردنی دروست و یەکلاکەرەوە، پێویستە پسپۆڕی دەروونی ڕاوێژت لەگەڵ بکات."
- Distinguish between normal, temporary emotional fluctuations and chronic clinical symptoms requiring in-person psychiatric or clinical psychologist support.
- Do NOT prescribe or modify medications.
- If suicidal intent, self-harm, or immediate danger is detected, switch immediately to emergency safety mode: full compassion, zero humor, and urging direct emergency contact with hospitals and loved ones.

SILENT INTERNAL QUALITY CONTROL:
Before submitting the response, verify that the Kurdish sounds fully human, grammatically sound, emotionally supportive, practically rich, and free of repetitive clichés.
        """.trimIndent()
    }

    private fun generateRichKurdishCounselorAdvice(prompt: String): String {
        val p = prompt.lowercase()
        return when {
            p.contains("overthinking") || p.contains("بیرکردنەوە") || p.contains("مێشک") || p.contains("خەو") || p.contains("بیر") -> {
                """
دەزانم چەندە ماندووکەرە کاتێک مێشکت وەک مەکینەیەکی بێ وەستان کاردەکات و ڕێگەت پێنادات چەند ساتێک لە ئارامیدا هەناسە بدەیت. گرفتەکە ئەوەیە زۆر بیرکردنەوە (Overthinking) خۆی لە خۆیدا کێشەی سەرەکی نییە، بەڵکو نیشانەی ئەوەیە مێشکت دەیەوێت بە بەردەوامی کۆنتڕۆڵی شتە نادیارەکانی داهاتوو بکات.

مێشکی مرۆڤ وەک مۆبایلێکی زیرەک وایە؛ ئەگەر پەنجا ئەپ لە پشتەوە بە کراوەیی بهێڵیتەوە، بێگومان دەست دەکات بە گەرمبوون و باترییەکەی بە خێرایی دادەبەزێت! مێشکیشت ئێستا لە گەرمبووندایە و دەڵێت برا گیان کەمێک پشووم بدێ. لە دەروونناسیدا، ئەم دۆخە پێی دەوترێت (Cognitive Overload)، واتە بارکردنی زیاتر لە توانای هۆشیاری.

بۆ ئەوەی مێشکت لەم بازنە داخراوە بهێنیتە دەرەوە، ئەم ٤ هەنگاوە تاقی بکەرەوە:

١. یاسای «دابەزاندنی بیرکردنەوە لەسەر کاغەز»: پەڕەیەک و قەڵەمێک بهێنە و هەموو ئەو شتانەی لە مێشکتدا دەخولێنەوە بە بێ سانسۆر بنووسە. کاتێک بیرکردنەوەکان دەبینیت، مێشکت باوەڕ دەکات کە لەبیر ناچن و کەمێک ئارام دەبێتەوە.
٢. دەستنیشانکردنی بازنەی دەسەڵات: لە هەموو ئەو بابەتانەی نیگەرانت کردووە، بە ڕاستگۆیی لە خۆت بپرسە: «ئایا ئێستا دەتوانم بە شێوەیەکی کرداری هیچ شتێک لەمبارەیەوە بکەم؟» ئەگەر ناتوانیت، کەواتە خەمخواردنی ئێستا تەنیا بەفیڕۆدانی وزەتە.
٣. دانانی کاتی دیاریکراو بۆ خەمخواردن (Worry Time): بۆ نموونە ڕۆژانە کاتژمێر ٥ی ئێوارە ١٥ خولەک دابنێ بۆ بیرکردنەوە لە کێشەکان. ئەگەر لە کاتەکانی تردا بیرێکی ناخۆش هات، بە خۆت بڵێ: «ئێستا کاتی نییە، لە کاتژمێر ٥ بیرت لێ دەکەمەوە».
٤. ڕاهێنانی بەستنەوە بە جەستە: کاتێک مێشکت زۆر ڕۆیشت، تەرکیز بخەرە سەر هەستە جەستەییەکانت؛ بۆ نموونە دەستت لەسەر مێزەکە دابنێ، یان قاچت بە توندی لەسەر زەوی دابنێ تا هۆشت بگەڕێتەوە بۆ ساتەوەختی ئێستا.

دەتوانیت لەم بەرنامەیەشدا ڕاهێنانی هەناسەدانی ٤-٧-٨ ئەنجام بدەیت. پێت چۆنە پێم بڵێیت چی بابەتێکە کە زیاتر لە هەموو شتێک مێشکتی داگیرکردووە؟
                """.trimIndent()
            }
            p.contains("دڵەڕاوکێ") || p.contains("ترس") || p.contains("anxiety") || p.contains("شڵەژاو") || p.contains("پەشۆکاو") || p.contains("پانیک") -> {
                """
هەستکردن بە دڵەڕاوکێ و تەنگەنەفەسی یەکێکە لە ناخۆشترین هەستەکان، بەڵام یەکەم شت کە دەمەوێت بزانیت ئەوەیە: تۆ پارێزراویت، جەڵتە لێت نادات، مێشکت لەدەست نادەیت و ئەم هەستە تووندەش تێدەپەڕێت.

دڵەڕاوکێ (Anxiety) لە بنەڕەتدا بەشێکە لە سیستەمی بەرگریی لەشی ئێمە. وەک زەنگی ئاگادارکردنەوەی ئاگر وایە لە بینایەکدا؛ کێشەکە لەوەدایە کە هەندێکجار زەنگەکە بە هەڵە لێدەدات، تەنانەت کاتێک تەنیا کەمێک دووکەڵی ئاسایی چێشتلێنان هەیە نەک ئاگرێکی ڕاستەقینە! لەم ساتەدا هۆرمۆنی ئەدریناڵین لە جەستەتدا بەرزە و جەستەت وا دەزانێت دەبێت لەگەڵ شێرێکدا شەڕ بکات.

ئەم ٤ ڕێکارە یەکسەر جێبەجێ بکە بۆ خاوکردنەوەی سیستەمی دەماریت:

١. فێنککردنەوەی دەموچاو (Diver's Reflex): بە ئاوی سارد دەست و دەموچاوت بشۆ یان پارچەیەک بەفر لەناو دەستتدا بگرە. ئەمە بە خێرایی ڕێژەی لێدانی دڵ کەمدەکاتەوە.
٢. تەکنیکی پێنج هەست (٥-٤-٣-٢-١): بە چاوەکانت ٥ شت لە دەوروبەرت ببینە، ٤ شت دەستیان لێبدە و زبری یان نەرمییەکەیان هەست پێبکە، ٣ دەنگ ببیستە، ٢ شت بۆن بکە، و ١ شت تامی بکە. ئەم کارە مێشکت لە ترسی ناوخۆیی دەهێنێتە دەرەوە بۆ واقیعی دەرەوە.
٣. هەناسەدانی درێژ بۆ دەرەوە: کاتێک هەناسە دەدەیتە دەرەوە، با کاتەکەی دوو هێندەی هەڵمژین بێت (بۆ نموونە ٤ چرکە هەڵمژین و ٨ چرکە دانەوە). ئەمە بە زۆر جەستە هێمن دەکاتەوە.
٤. گفتوگۆی ناوخۆیی ئارامکەرەوە: بە نەرمی بە خۆت بڵێ: «ئەمە تەنیا هەستێکی کاتییە و دەڕوات، مەترسییەکی ڕاستەقینە لەسەر ژیانم نییە».

ئایا ئەم پەشۆکاوییە لەناکاو پەیدا بوو یان لەبەر بارودۆخێکی دیاریکراوە کە پەستانی لەسەر دروست کردوویت؟
                """.trimIndent()
            }
            p.contains("پەیوەندی") || p.contains("خۆشەویست") || p.contains("هاوسەر") || p.contains("دڵپیسی") || p.contains("خیانەت") || p.contains("جیابوونەوە") -> {
                """
کێشەکانی پەیوەندیی سۆزداری و هاوسەرگیری زۆرجار بە ئازارترین تاقیکردنەوەن، چون بەشە هەرە هەستیار و لاوازەکانی ناخی ئێمە دەردەخەن. 

لە زۆربەی پەیوەندییەکاندا، کێشەی سەرەکی نەبوونی خۆشەویستی نییە، بەڵکو نەبوونی لێکتێگەیشتن و قسەکردنی ڕوون و ڕاشکاوە. کاتێک ناتوانین بە ڕوونی بڵێین چی دەمانەوێت، پەنا دەبەینە بەر توڕەبوون، ساردبوونەوە، یان گلەیی بەردەوام. لە دەروونناسیدا دەوترێت: «تۆڕەبوون لە پەیوەندیدا، زۆرجار هاوارێکە بۆ داواکردنی پەیوەستبوون و هەستپێکردن».

بۆ ئەوەی بە شێوازێکی تەندروست ڕووبەڕووی ئەم دۆخە ببیتەوە:

١. کاتی ئارامبوونەوە دابنێ: لە لوتکەی توڕەیی و ئازاردا هیچ بڕیارێکی گرنگ مەدە و چاتی درێژ مەکە. باشترین ڕستە ئەوەیە بڵێیت: «من ئێستا زۆر هەستیم و نامەوێت قسەیەک بکەم لێی پەشیمان بمەوە، با کەمێک هێمن ببینەوە و پاشان قسە دەکەین».
٢. گۆڕینی شێوازی قسەکردن لە «تۆ» بۆ «من»: لەبری ئەوەی بڵێیت: «تۆ هەمیشە گرنگیم پێنادەیت و وایت»، بڵێ: «من کاتێک دەبینم چەند کاتژمێرێک نامەم بۆ نانووسیت، هەست بە تەنیایی و کەمبایەخی دەکەم». ئەم جیاوازییە دەرگای گفتوگۆ دەکاتەوە لەبری دروستکردنی سەنگەر.
٣. پاراستنی سنوورە شەخسییەکان (Boundaries): خۆشەویستی بە مانای تواندنەوەی کەسایەتی یەکتر نییە. هەردووکتان مافی ئەوەتان هەیە کاتی تایبەت بە خۆتان هەبێت بەبێ گومان و چاودێریی نەخۆشانە.
٤. بیرکردنەوە لە واقیعی پەیوەندییەکە: پەیوەندییەکان وەک فلیمە ڕۆمانسییەکان نین کە هەموو ڕۆژ گوڵ و پێکەنین بێت؛ هەوراز و نشێوی پێویستی بە تێگەیشتن و کاتی هاوبەش هەیە.

دەتەوێت کەمێک زیاترم بۆ باس بکەیت تا پێکەوە سەیری ڕەهەندەکانی بکەین؟
                """.trimIndent()
            }
            p.contains("بێتاقەت") || p.contains("خەم") || p.contains("خەمۆک") || p.contains("گریان") || p.contains("تەنیا") || p.contains("بێزار") -> {
                """
زۆر ئازاربەخشە کاتێک هەست دەکەیت بارێکی قورس لەسەر سنگتە و تاقەتی دەوروبەرت و تەنانەت تاقەتی خۆشت نییە. لە دڵەوە هەستت پێدەکەم و پێت دەڵێم کە هەستەکانت تەواو ڕەوان.

مرۆڤ ڕۆبۆت نییە تا ٢٤ کاتژمێر پێبکەنێت و وزەی لە ١٠٠ بێت. هەندێک ڕۆژ هەن کە وزەی دەروونمان دەگاتە سفر. لە ڕوانگەی پزیشکییەوە، خەمۆکی و بێتاقەتی بەردەوام پەیامێکی جەستەیە کە پێمان دەڵێت: «تۆ بۆ ماوەیەکی زۆر بەرگەی پەستانت گرتووە، ئێستا پێویستت بە پشوویەکی قووڵە».

چەند هەنگاوی بچووک بەڵام کاریگەر:

١. بەزەیی بە خۆتدا بێتەوە (Self-Compassion): خۆت لۆمە مەکە بۆ ئەوەی کە بێتاقەتیت؛ مەیخەرە سەر بێ توانایی خۆت. ڕێگە بدە ئەگەر پێویستیت بە گریانە، کەمێک بگریت، چونکە فرمێسک هۆرمۆنە پەستێنەرەکان لە لەش دەردەکات.
٢. دەستکەوتی بچووک لەبری گەورە: چاوەڕێی ئەوە لە خۆت مەکە کارێکی مەزن ئەنجام بدەیت. تەنیا پەرداخێک ئاو بخۆرەوە، سەری پەنجەرەکە بکەرەوە تا کەمێک هەوای پاک بێتە ژوورەوە، یان پێخەفەکەت ڕێکبخە. هەموو هەنگاوێکی بچووک سەرکەوتنە.
٣. ڕۆیشتن لەبەر ڕووناکی: ڕۆژانە تەنانەت بۆ ١٠ خولەکیش بێت بچۆ بەر تیشکی خۆر؛ تیشکی خۆر یارمەتی ڕێکخستنی ماددەی سیرۆتۆنین دەدات لە مێشکدا.
٤. پاراستنی کاتی خەو: هەوڵبدە لە یەک کاتدا بخەویت و هەستیت، چون خەوی شێواو دەروون زۆر بێهێز دەکات.

تێبینییەکی پزیشکیی گرنگ: ئەگەر ئەم دۆخەی بێتاقەتی و نەبوونی چێژ لە ژیان زیاتر لە دوو هەفتەیە بەردەوامە و ڕێگری لە کارە ڕۆژانەکانت دەکات، ئەوە نیشانەیەکە کە پێویستی بە هەڵسەنگاندنی ڕاستەوخۆی پزیشک یان ڕاوێژکاری دەروونی هەیە.
                """.trimIndent()
            }
            p.contains("متمانە") || p.contains("شەرم") || p.contains("لاواز") || p.contains("خەڵک") || p.contains("قسەی خەڵک") -> {
                """
متمانە بەخۆبوون (Self-Confidence) شتێک نییە مرۆڤ لەگەڵ خۆیدا لەدایکبووبێت و ئیتر تەواو؛ بەڵکو وەک ماسولکەی جەستە وایە، هەتا زیاتر کاری پێبکەیت و ڕاهێنانی پێبکەیت، بەهێزتر و ڕەقتر دەبێت.

کێشەی زۆربەی ئەو کەسانەی متمانەیان بەخۆیان کەمە ئەوەیە کە دەکەونە داوی دیاردەیەک لە دەروونناسیدا کە پێی دەوترێت (Spotlight Effect)، واتە وا هەست دەکەن پڕۆژێکتەرێکی گەورە لەسەر سەریانە و هەموو دنیا چاودێری هەڵە و قسەکانیان دەکات! لە کاتێکدا ڕاستییەکە ئەوەیە کە ٩٥٪ی خەڵک بە تەواوی سەرقاڵی کێشە، مۆبایل و ژیانی خۆیانن و هیچ کاتێکیان نییە بەو وردییە سەیری تۆ بکەن 😂.

چۆن متمانەت بنیاد بنێیتەوە؟

١. ڕاگرتنی بەراوردکاریی ژەهراوی: تۆ لە پشتی پەردەی ژیانی خۆت ئاگاداریت، بەڵام تەنیا دیوی جوان و ڕازاوەی لاپەڕەی ئینستاگرامی خەڵک دەبینیت. هەرگیز ناخی خۆت لەگەڵ ڕووکەشی خەڵک بەراورد مەکە.
٢. دەستپێکردن بە هەنگاوی بچووک لە دەرەوەی ناوچەی ئاسوودەیی (Comfort Zone): کاتێک شتێکت دەوێت، لە شوێنە گشتییەکان پرسیار بکە، لە کۆڕێکدا ڕای خۆت دەربڕە تەنانەت ئەگەر کەمێک دەنگت بلەرێت. ئاساییە لەرزین، بەڵام بێدەنگ مەبە.
٣. هاوڕێیەتی لەگەڵ هەڵەکردن: مرۆڤی سەرکەوتوو کەسێک نییە کە هەڵە ناکات، بەڵکو کەسێکە کە لە هەڵەکردن ناترسێت. هەر هەڵەیەک وانەیەکی بەنرخە بۆ پێگەیشتنت.
٤. دیاریکردنی خاڵە بەهێزەکانت: سێ لەو کارانەی کە تێیاندا لێهاتووی یان خەڵک دەستخۆشیت لێدەکەن بنووسەوە و لە شوێنێکی دیاردا دایبنێ.

پێت چۆنە پێم بڵێیت چ کاتێک یان لە چ شوێنێکدا زیاتر هەست دەکەیت شەرم یان بێمتمانەیی دەستت دەبەستێت؟
                """.trimIndent()
            }
            p.contains("توڕە") || p.contains("شەڕ") || p.contains("قین") || p.contains("هاوار") -> {
                """
توڕەبوون هەستێکی تەواو سروشتییە و هەموو مرۆڤێک پێیدا تێپەڕ دەبێت، بەڵام گرفتەکە ئەوەیە کە توڕەبوون وەک ئاگر وایە؛ دەکرێت ژەمێک خواردنی پێ لێبنێیت، یان دەکرێت تەواوی ماڵەکەت پێ بسووتێنیت!

لە دەروونناسیدا، توڕەبوون پێی دەوترێت «هەستی پلە دوو» (Secondary Emotion). ئەمە واتە چی؟ واتە لە ژێرەوە هەمیشە هەستێکی تر هەیە کە ئازاری هەیە—وەک هەستکردن بە سووکایەتی، بێدەسەڵاتی، نادادپەروەری، یان ترس—بەڵام لەبەر ئەوەی ئەو هەستانە زۆر ناسکن، دەروونمان توڕەبوون دەکاتە قەڵغان بۆ بەرگری لە خۆی.

بۆ کۆنتڕۆڵکردنی توڕەیی پێش ئەوەی ئەو تۆ کۆنتڕۆڵ بکات:

١. یاسای ١٠ چرکە پێش وەڵامدانەوە: کاتێک خوێنت گەرم دەبێت و هەست دەکەیت دەتەوێت بتەقیتەوە، لە ١ تا ١٠ بە هێواشی بژمێرە و قسە مەکە. بەشی ژیربێژیی مێشک پێویستی بە چەند چرکەیەکە تا کۆنتڕۆڵ وەربگرێتەوە.
٢. شوێنەکە بەجێبهێڵە: ئەگەر لە شوێنێکدا گفتوگۆ تووند بوو، بڵێ «پێویستم بە کەمێک هەوایە» و بڕۆ دەرەوە.
٣. دەرکردنی وزە بە شێوەیەکی فیزیکی: توڕەیی وزەیەکی جەستەییە؛ بە وەرزشکردن، پیاسەی خێرا، یان تەنانەت دەستشتن بە ئاوی سارد ئەو وزە لە جەستەتدا بەتاڵ بکەرەوە.
٤. لە خۆت بپرسە: «ئایا ئەم بابەتە دوای ساڵێکی تر هەر گرنگ دەبێت؟» لە زۆربەی کاتەکاندا وەڵامەکە نەخێرە.

چ بابەتێک یان چ ڕەفتارێک زیاتر لە هەموو شتێک ئاگری توڕەییت هەڵدەگیرسێنێت؟
                """.trimIndent()
            }
            else -> {
                """
سڵاو و ڕێز لە دڵەوە بۆت. زۆر سوپاس بۆ ئەوەی کە هاتوویت و متمانەت کردووە بۆ ئەوەی ناخی خۆت دەرببڕیت. دەربڕینی هەستەکان یەکەمین و گرنگترین هەنگاوە بەرەو ئارامیی دەروونی.

پێویستە هەمیشە لە بیرت بێت: دەروونی مرۆڤ وەک کەشوهەوا وایە؛ ڕۆژێک هەتاوە، ڕۆژێک باران و هەورە. هیچ کەشوهەوایەک هەتا سەر نامێنێتەوە و گۆڕانکاری بەشێکی حەتمییە لە ژیان. ئەگەر ئێستا لە دۆخێکی ناخۆشدایت، دڵنیابە ڕێگای دەربازبوون و چارەسەرکردن بوونی هەیە.

چەند ڕێنماییەکی گشتیی بەسوود بۆ ژیانی دەروونیت:
١. ڕۆژانە کاتێک بۆ گفتوگۆ لەگەڵ خۆت یان نووسینی یاداشتەکانت دابنێ.
٢. جیاوازی بکە لە نێوان ئەو شتانەی لەژێر دەسەڵاتی تۆدان لەگەڵ ئەو شتانەی لە دەرەوەی کۆنتڕۆڵی تۆدان.
٣. لە داواکردنی یارمەتی شەرم مەکە، تەندروستی دەروونی هێندەی تەندروستی جەستەیی شایەنی گرنگیپێدانە.

فەرموو، زیاترم بۆ ڕوون بکەرەوە کە چی لە مێشکتدا قورسایی دروست کردووە، با پێکەوە قسەی لەسەر بکەین! 🧠
                """.trimIndent()
            }
        }
    }
}
