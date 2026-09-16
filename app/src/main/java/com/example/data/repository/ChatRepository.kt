package com.example.data.repository

import com.example.BuildConfig
import com.example.data.local.ChatDao
import com.example.data.model.ChatMessage
import com.example.data.model.GeminiContent
import com.example.data.model.GeminiGenerationConfig
import com.example.data.model.GeminiPart
import com.example.data.model.GeminiRequest
import com.example.data.remote.GeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ChatRepository(private val chatDao: ChatDao) {

    val allMessages: Flow<List<ChatMessage>> = chatDao.getAllMessages()

    suspend fun saveMessage(message: ChatMessage): Long = withContext(Dispatchers.IO) {
        chatDao.insertMessage(message)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        chatDao.clearChat()
    }

    suspend fun getMessageCount(): Int = withContext(Dispatchers.IO) {
        chatDao.getMessageCount()
    }

    suspend fun consultDrBasta(userPrompt: String, recentHistory: List<ChatMessage>): ChatMessage = withContext(Dispatchers.IO) {
        // Check for immediate crisis / safety trigger in Kurdish
        val isCrisis = isEmergencyCrisis(userPrompt)
        if (isCrisis) {
            val crisisResponse = generateEmergencyCrisisResponse()
            val msg = ChatMessage(
                text = crisisResponse,
                sender = "counselor",
                timestamp = System.currentTimeMillis(),
                isCrisisWarning = true
            )
            saveMessage(msg)
            return@withContext msg
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasValidKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (hasValidKey) {
            try {
                val systemPrompt = getDrBastaSystemPrompt()
                
                // Build history turns (up to last 6 messages to keep context lean and fast)
                val historyContents = mutableListOf<GeminiContent>()
                val limitedHistory = recentHistory.takeLast(6)
                for (item in limitedHistory) {
                    val role = if (item.isUser) "user" else "model"
                    historyContents.add(
                        GeminiContent(
                            role = role,
                            parts = listOf(GeminiPart(text = item.text))
                        )
                    )
                }

                // Add current user prompt
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
                        temperature = 0.7f,
                        maxOutputTokens = 1000
                    )
                )

                val response = GeminiClient.service.generateContent(apiKey, request)
                if (response.isSuccessful && response.body() != null) {
                    val candidate = response.body()?.candidates?.firstOrNull()
                    val replyText = candidate?.content?.parts?.firstOrNull()?.text?.trim()
                    if (!replyText.isNullOrBlank()) {
                        val counselorMsg = ChatMessage(
                            text = replyText,
                            sender = "counselor",
                            timestamp = System.currentTimeMillis()
                        )
                        saveMessage(counselorMsg)
                        return@withContext counselorMsg
                    }
                }
            } catch (e: Exception) {
                // Fallback on network or API failure
            }
        }

        // Smart Kurdish counselor fallback engine adhering to Dr. Basta's persona
        val fallbackText = generateKurdishFallbackAdvice(userPrompt)
        val counselorMsg = ChatMessage(
            text = fallbackText,
            sender = "counselor",
            timestamp = System.currentTimeMillis()
        )
        saveMessage(counselorMsg)
        return@withContext counselorMsg
    }

    private fun isEmergencyCrisis(text: String): Boolean {
        val lower = text.lowercase()
        val crisisKeywords = listOf(
            "خۆکوشتن", "خۆم دەکوژم", "خۆم دەخنکێنم", "کۆتایی بە ژیانم",
            "ئازاری خۆم دەدەم", "خۆئازاردان", "دەمرم", "توندوتیژی",
            "ئەمەوێ بمرم", "نامەوێ بژیم", "ژیانم بێ مانا بووە و خۆم لەناو دەبەم"
        )
        return crisisKeywords.any { lower.contains(it) }
    }

    private fun generateEmergencyCrisisResponse(): String {
        return """
برا یان خوشکی بەڕێزم، زۆر نیگەرانتم و لە دڵەوە لەگەڵتم. لەم ساتەدا سەلامەتی و ژیانی تۆ بۆ هەموومان گرنگترین شتە! ❤️

تکایە بە تەنیا مەمێنەرەوە. لەبەر ئەوەی من ڕاوێژکارێکی زیرەکی دەستکردم و ناتوانم لە باری لەناکاو و فریاکەوتندا دەستبەجێ بە شێوەی ڕاستەوخۆ دەستت بگرم:

١. هەر ئێستا پەیوەندی بە کەسێکی زۆر نزیک و باوەڕپێکراوتەوە بکە (دایک، باوک، هاوسەر، برایەک یان هاوڕێیەکی دڵسۆزت).
٢. سەردانی نزیکترین نەخۆشخانە یان بنکەی فریاکەوتنی پزیشکی بکە لە شارەکەتدا، یان پەیوەندی بە هێڵی فریاکەوتنی ١٢٢ بکە.
٣. چەند هەناسەیەکی قووڵ هەڵمژە و لە جێگەیەک دابنیشە کە تەنیا نەبیت.

ژیانی تۆ زۆر بەنرخە، تەنانەت ئەگەر ئێستا هەموو دەرگاکان داخراو دیار بن، هەمیشە ڕێگای چارەسەر هەیە کاتێک کەسێکی پسپۆڕ یان دڵسۆز گوێت لێ دەگرێت. تکایە ئێستا سەلامەتیت بپارێزە!
        """.trimIndent()
    }

    private fun getDrBastaSystemPrompt(): String {
        return """
You are a personalized AI psychological counselor created specifically for "Dr. Basta".

Your identity:
- Name: دکتۆر بەستە
- Role: Psychological Counselor / Mental Health Assistant
- Purpose: Help people understand emotions, thoughts, behaviors, relationships, anxiety, stress, overthinking, confidence issues, sadness, anger, loneliness, everyday psychological problems.
- Safety: You are NOT a replacement for a licensed psychiatrist, psychologist, or emergency medical service. Never claim to diagnose someone with certainty. Give possibilities, ask useful questions when necessary, and recommend professional help when appropriate.

PERSONALITY:
- Be extremely fast, intelligent, understanding, practical, and emotionally aware.
- Understand Kurdish Sorani naturally, including colloquial Kurdish, slang, spelling mistakes, short messages, and indirect expressions.

LANGUAGE STYLE:
- Always speak in natural Sorani Kurdish.
- Use very simple, everyday Kurdish.
- Sound like a real Kurdish person talking to another person, not like a textbook or robot.
- Use a friendly, warm, "عەشایەری" / street-level conversational style.
- Avoid complicated psychological terminology unless necessary; explain it immediately in simple Kurdish.
- Keep answers clear and easy to understand.
- Do not sound overly formal or academic.
- Do not repeatedly say "I am an AI".
- Do not give unnecessarily long answers.

HUMOR:
- Mix light comedy into your answers when appropriate.
- Natural Kurdish friendly humor (e.g., "مێشکت وەک مۆبایلە، ئەگەر ٢٠٠ ئەپ لە پشتەوە بکەیتەوە، ئەویش دەڵێت برا گیان منیش مرۆڤم 😂").
- NEVER joke about serious trauma, suicide, abuse, death, severe mental illness, or someone's suffering.

RESPONSE STYLE:
1. Understand what they really mean.
2. Give main answer quickly.
3. Explain psychological reason simply.
4. Give 3–5 practical steps they can actually do.
5. If useful, ask one short follow-up question.
6. Add light humor when appropriate.

RELATIONSHIP QUESTIONS:
- Explain both sides fairly, communication, boundaries, respect, realistic expectations.

MENTAL HEALTH SAFETY:
- If suicide, self-harm, emergency is mentioned: Take seriously, NO humor, encourage immediate contact with emergency services, hospital, or trusted person.

DIAGNOSIS:
- Never confidently diagnose. Use: "ئەم نیشانانە دەتوانێت لەگەڵ ... هاوشێوە بێت، بەڵام بۆ دڵنیابوون پێویستی بە هەڵسەنگاندنی پسپۆڕ هەیە."

MEDICATION:
- Do not prescribe or change medication. Encourage consultation with a doctor.

Signature identity:
دکتۆر بەستە | دکتۆری دەرونی 🧠
(Only add signature when appropriate, not necessarily every short chat message).
        """.trimIndent()
    }

    private fun generateKurdishFallbackAdvice(prompt: String): String {
        val p = prompt.lowercase()
        return when {
            p.contains("overthinking") || p.contains("بیر") || p.contains("مێشک") || p.contains("خەو") -> {
                """
سڵاو لە چاوت برا یان خوشکم! کێشەی زۆر بیرکردنەوە (Overthinking) یەکێکە لە دیارترین گرفتەکانی ئەم سەردەمەمانە.

مێشکت وەک مۆبایل وایە، ئەگەر ٢٠٠ ئەپ لە پشتەوە بە کراوەیی بهێڵیتەوە، بێگومان باتری دادەبەزێت و دەست دەکات بە هەڵپروکان، مێشکیشت دەڵێت برا گیان منیش مرۆڤم 😂!

هۆکارەکەی زۆرجار ئەوەیە کە مێشکت دەیەوێت کۆنتڕۆڵی داهاتوویەکی نەزانراو بکات، یان لە هەڵەیەکی ڕابردوو دەترسێت.

٣ هەنگاوی کرداری کە ئێستا بیکەیت:
١. یاسای ٥ دەقیقە: هەرچییەک لە مێشکتدایە بیهێنە سەر کاغەز، با مێشکت بزانێت خەمەکە تۆمار کراوە و لەبیر ناچێت.
٢. جیاکردنەوەی شتەکان: لە خۆت بپرسە "ئایا ئەم شتە ئێستا لەژێر کۆنتڕۆڵی مندایە؟" ئەگەر نەبوو، کات بۆ خەمخواردنی دابنێ نەک ئێستا.
٣. چەند هەناسەیەکی قووڵ: ٤ چرکە هەناسە هەڵمژە، ٤ چرکە ڕایگرە، ٦ چرکە بیدەرەوە.

دەتوانیت لە بەشی ڕاهێنانی ئەم ئەپەش ڕاهێنانی هەناسەدان بکەیت. چیت هەیە زیاتر قسەی لەسەر بکەین؟
                """.trimIndent()
            }
            p.contains("دڵەڕاوکێ") || p.contains("ترس") || p.contains("anxiety") || p.contains("پەشۆکاو") -> {
                """
دەستت لەسەر دڵت دابنێ، هیچ شتێکی خراپ ڕوونادات و تۆ پارێزراویت! 🌿

دڵەڕاوکێ (Anxiety) زەنگی ئاگادارکردنەوەی جەستەیە؛ وەک زەنگی ئاگری ماڵ وایە کە کاتێک کەسێک کەمێک تووکە دووکەڵ دەبینێت زەنگ لێدەدات، واتە جەستەت زۆر بە وریاییەوە لەخۆت دەڕوانێت، بەڵام هەندێکجار زەنگەکە بە هەڵە لێدەدات.

ئەم ٣ هەنگاوە تاقی بکەرەوە:
١. تەکنیکی ٥-٤-٣-٢-١: ٥ شت ببینە لە ژوورەکەتدا، ٤ شت دەستی لێبدە، ٣ دەنگ ببیستە، ٢ بۆن بکە، ١ تامی بکە. ئەمە یەکسەر هۆشت دەهێنێتەوە بۆ ساتەوەختی ئێستا.
٢. دەموچاوت بە ئاوی سارد بشۆ: ئەمە لێدانی دڵت خاو دەکاتەوە.
٣. بە خۆت بڵێ: "ئەمە تەنیا هەستێکی کاتییە و دەڕوات، مەترسییەکی ڕاستەقینە نییە."

ئایا ئەم ترسە لەناکاو هات یان ماوەیەکە لەگەڵتە؟
                """.trimIndent()
            }
            p.contains("پەیوەندی") || p.contains("خۆشەویست") || p.contains("هاوسەر") || p.contains("دڵپیسی") || p.contains("شکاو") -> {
                """
پەیوەندیی نێوان دوو کەس وەک باخچە وایە، ئەگەر ئاوی نەدەیت وشک دەبێت، بەڵام ئەگەر بە فیشەک و هاوار ئاوی بدەیت تێکی دەدەیت!

لە کێشەی پەیوەندیدا، هەمیشە دوو ڕوانگە هەیە. زۆرجار کەسەکان دژی یەکتر نین، بەڵکو تێنەگەیشتن لە چاوەڕوانیی یەکتر دروستی دەکات.

هەنگاوی کرداری بۆ پەیوەندییەکان:
١. کاتێک توڕەیت بڕیار مەدە و چات مەکە. بڵێ "با کەمێک هێمن ببینەوە پاشان قسە دەکەینەوە".
٢. بەکارهێنانی ڕستەی «من هەست دەکەم» لەبری «تۆ هەمیشە وایت»: تاوانبارکردن دەرگای گفتوگۆ دادەخات.
٣. ڕێز و سنوور ڕوون بکەرەوە: بێ متمانەیی بە پشکنینی مۆبایل چارەسەر نابێت، بە گفتوگۆی ڕاشکاو دەبێت.

دەتەوێت وردتر باسی کەیسەکەت بکەیت تا پێ پێکەوە سەیری هەردوو لایەنەکەی بکەین؟
                """.trimIndent()
            }
            p.contains("بێتاقەت") || p.contains("خەم") || p.contains("خەمۆک") || p.contains("گریان") || p.contains("دڵتەنگ") -> {
                """
گیانەکەم، زۆر ئاساییە مرۆڤ ڕۆژانێک هەست بە بێتاقەتی بکات، ناکرێت ڕۆبۆت بین و هەمیشە ٢٤ کاتژمێر پێبکەنین!

خەمۆکی و دڵتەنگی پەیامێکی جەستەیە پێمان دەڵێت: "کەمێک پشوو بدە، زۆرت لە خۆت کردووە".

ئەم چەند هەنگاوە سادەیە ئەنجام بدە:
١. بڕۆ بۆ پیاسەیەکی کورت یان کەمێک لەبەر هەوای پاک دابنیشە.
٢. بڕیارە قورسەکان دوابخە بۆ کاتێک کە وزەت باشتر دەبێت.
٣. تەنیا یەک کاری زۆر بچووک تەواو بکە (وەک ڕێکخستنی جێگەکەت یان خواردنەوەی پەرداخێک ئاو).
٤. لەگەڵ کەسێک کە زۆرت خۆشدەوێت چەند قسەیەک بکە.

ئەگەر ئەم بێتاقەتییە زیاتر لە دوو هەفتەیە بەردەوامە و هێزت لێ دەبڕێت، ئەم نیشانانە دەتوانێت لەگەڵ خەمۆکی هاوشێوە بێت، بۆیە ڕاوێژ بە پزیشکی دەروونی باشترین هەنگاوە.
                """.trimIndent()
            }
            p.contains("متمانە") || p.contains("شەرم") || p.contains("خەڵک") -> {
                """
متمانە بەخۆبوون ماسولکەیە، هەرچەند زیاتر ڕاهێنانی پێ بکەیت بەهێزتر دەبێت!

بزانە، خەڵکی ٩٠٪ی کاتەکەیان سەرقاڵی کێشە و ژیانی خۆیانن، کەس ئەوەندە بە وردی چاودێری هەموو جوڵەیەکت ناکات وەک تۆ بیرت لێکردۆتەوە 😂!

ئەم هەنگاوانە بگرەبەر:
١. بەراوردکاری ڕابگرە: تۆ لەگەڵ ڕابردووی خۆت بەراورد بکە نەک لەگەڵ لاپەڕەی ئینستاگرامی خەڵک.
٢. دەستکەوتە بچووکەکانت بنووسەوە، تەنانەت بچووکترین شت.
٣. لە هەڵەکردن مەترسە: هەموو کەسێک هەڵە دەکات و ئەوە بەشێکە لە فێربوون.
                """.trimIndent()
            }
            p.contains("سڵاو") || p.contains("چۆنی") || p.contains("باشی") || p.contains("کێیت") -> {
                """
سڵاو و ڕێز برا و خوشکی بەڕێزم! هیوادارم دڵت ئارام و ڕۆژت پڕ لە باشی بێت.

من «دکتۆر بەستە»م، ڕاوێژکاری دەروونی تۆم. لێرەم بۆ ئەوەی گوێت لێبگرم لەسەر هەستەکانت، دڵەڕاوکێ، زۆر بیرکردنەوە، کێشەی پەیوەندی، یان هەر شتێک کە لە مێشکتدا قورسایی دروست کردووە.

ئەمڕۆ مێشک و دڵت چۆنە؟ چی لەسەر دڵتە بە کوردییەکی سادە و دۆستانە پێم بڵێ! 😊
                """.trimIndent()
            }
            else -> {
                """
پەیامەکەت بە جوانی گەیشت، دەستت خۆش بێت کە قسە دەکەیت و ناخی خۆت ناڕێژیتەوە!

لە دەروونزانیدا یاسایەکی زێڕین هەیە: "هەستەکانت کاتێک دەردەبڕیت، نیوەی قورساییەکەی لەسەر شانت نامێنێت".

ئامۆژگاری کرداری من بۆ تۆ:
١. کەمێک خاو ببەرەوە و ئاوێک بخۆرەوە.
٢. هەوڵبدە لە یەک دوو دێڕدا بۆم باس بکەیت کە چی ڕووی داوە یان چ هەستێک زیاتر ئازارت دەدات.
٣. پێکەوە هەنگاو بە هەنگاو ڕێگای چارەسەرکردن و هێوربوونەوەکەی دەدۆزینەوە.

فەرموو زیاترم بۆ باس بکە، لێرەم و گوێت لێ دەگرم! 🌿
                """.trimIndent()
            }
        }
    }
}
