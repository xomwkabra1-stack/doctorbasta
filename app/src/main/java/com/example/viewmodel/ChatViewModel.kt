package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessage
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MoodOption(
    val emoji: String,
    val labelKurdish: String,
    val prompt: String
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository

    val messages: StateFlow<List<ChatMessage>>
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _selectedMood = MutableStateFlow<String?>(null)
    val selectedMood: StateFlow<String?> = _selectedMood.asStateFlow()

    private val _showBreathingDialog = MutableStateFlow(false)
    val showBreathingDialog: StateFlow<Boolean> = _showBreathingDialog.asStateFlow()

    private val _showEmergencyDialog = MutableStateFlow(false)
    val showEmergencyDialog: StateFlow<Boolean> = _showEmergencyDialog.asStateFlow()

    private val _showAboutDialog = MutableStateFlow(false)
    val showAboutDialog: StateFlow<Boolean> = _showAboutDialog.asStateFlow()

    val moodOptions = listOf(
        MoodOption("😊", "ئارام و باشم", "دکتۆر گیان، ئەمڕۆ هەست بە ئارامی و باشی دەکەم، چۆن ئەم هەستە بپارێزم؟"),
        MoodOption("🤯", "زۆر بیردەکەمەوە", "دکتۆر بەستە، مێشکم زۆر سەرقاڵە و زۆر بیردەکەمەوە (Overthinking)، چی بکەم؟"),
        MoodOption("😟", "دڵەڕاوکێم هەیە", "هەست بە دڵەڕاوکێ و ترس دەکەم بێ هۆکار، دەتوانیت ئارامم بکەیتەوە؟"),
        MoodOption("😞", "بێتاقەتم", "ئەمڕۆ زۆر بێتاقەتم و تاقەتی هیچم نییە، چی ڕێنماییەکم دەکەیت؟"),
        MoodOption("😤", "توڕە و شڵەژاوم", "زوو توڕە دەبم لەسەر شتی کەم، چۆن کۆنتڕۆڵی توڕەیی خۆم بکەم؟"),
        MoodOption("💔", "دڵشکاوم", "کێشەیەکم لە پەیوەندییەکەمدا هەیە و هەست بە دڵتەنگییەکی قووڵ دەکەم.")
    )

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ChatRepository(database.chatDao())
        messages = repository.allMessages.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // If it's a completely fresh start, add a warm Kurdish welcome message from Dr. Basta
        viewModelScope.launch {
            if (repository.getMessageCount() == 0) {
                val welcome = ChatMessage(
                    text = """
سڵاو لە چاوت برا و خوشکم! من «دکتۆر بەستە»م 🧠
ڕاوێژکاری دەروونی تۆم بە زمانی شیرینی کوردی. 

لێرەم تا بە دڵسۆزی گوێت لێبگرم لەسەر هەموو ئەو کێشە، بیرکردنەوە، دڵەڕاوکێ و هەستانەی کە لە ناختدا هەیە. بە کوردییەکی سادە و دۆستانە لەگەڵم بدوێ، هەر پرسیار یان ناڕەحەتییەکت هەیە بیڵێ، پێکەوە شیی دەکەینەوە!

دەتوانیت لە ڕێگەی نیشانەکانی خوارەوە دەست پێبکەیت یان یەکسەر نامەکەت بنووسیت.
                    """.trimIndent(),
                    sender = "counselor",
                    timestamp = System.currentTimeMillis()
                )
                repository.saveMessage(welcome)
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    fun sendMessage(content: String? = null) {
        val textToSend = (content ?: _inputText.value).trim()
        if (textToSend.isBlank() || _isLoading.value) return

        _inputText.value = ""
        _isLoading.value = true

        viewModelScope.launch {
            val userMessage = ChatMessage(
                text = textToSend,
                sender = "user",
                timestamp = System.currentTimeMillis()
            )
            repository.saveMessage(userMessage)

            val currentHistory = messages.value
            repository.consultDrBasta(textToSend, currentHistory)

            _isLoading.value = false
        }
    }

    fun selectMood(mood: MoodOption) {
        _selectedMood.value = mood.labelKurdish
        sendMessage(mood.prompt)
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearHistory()
            // Re-insert Dr. Basta welcome message
            val welcome = ChatMessage(
                text = "سڵاو لە چاوت! چاتەکە پاککرایەوە. هەر کاتێک ئامادە بوویت، من لێرەم و گوێت لێ دەگرم 🧠",
                sender = "counselor",
                timestamp = System.currentTimeMillis()
            )
            repository.saveMessage(welcome)
        }
    }

    fun openBreathingDialog() {
        _showBreathingDialog.value = true
    }

    fun closeBreathingDialog() {
        _showBreathingDialog.value = false
    }

    fun openEmergencyDialog() {
        _showEmergencyDialog.value = true
    }

    fun closeEmergencyDialog() {
        _showEmergencyDialog.value = false
    }

    fun openAboutDialog() {
        _showAboutDialog.value = true
    }

    fun closeAboutDialog() {
        _showAboutDialog.value = false
    }
}
