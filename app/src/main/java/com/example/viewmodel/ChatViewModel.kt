package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessage
import com.example.data.repository.ChatRepository
import com.example.data.security.SecureKeyManager
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

enum class AppScreen {
    CHAT,
    SETTINGS
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    val secureKeyManager = SecureKeyManager(application)
    private val repository: ChatRepository

    val messages: StateFlow<List<ChatMessage>>

    private val _currentScreen = MutableStateFlow(AppScreen.CHAT)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _selectedMood = MutableStateFlow<String?>(null)
    val selectedMood: StateFlow<String?> = _selectedMood.asStateFlow()

    // Dialog state
    private val _showBreathingDialog = MutableStateFlow(false)
    val showBreathingDialog: StateFlow<Boolean> = _showBreathingDialog.asStateFlow()

    private val _showEmergencyDialog = MutableStateFlow(false)
    val showEmergencyDialog: StateFlow<Boolean> = _showEmergencyDialog.asStateFlow()

    private val _showAboutDialog = MutableStateFlow(false)
    val showAboutDialog: StateFlow<Boolean> = _showAboutDialog.asStateFlow()

    // Settings state
    private val _maskedApiKey = MutableStateFlow(secureKeyManager.getMaskedApiKey())
    val maskedApiKey: StateFlow<String?> = _maskedApiKey.asStateFlow()

    private val _selectedModel = MutableStateFlow(secureKeyManager.getSelectedModel())
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _isTestingKey = MutableStateFlow(false)
    val isTestingKey: StateFlow<Boolean> = _isTestingKey.asStateFlow()

    private val _testResultMessage = MutableStateFlow<String?>(null)
    val testResultMessage: StateFlow<String?> = _testResultMessage.asStateFlow()

    private val _testSuccess = MutableStateFlow<Boolean?>(null)
    val testSuccess: StateFlow<Boolean?> = _testSuccess.asStateFlow()

    val moodOptions = listOf(
        MoodOption("🤯", "زۆر بیرکردنەوە", "دکتۆر بەستە، مێشکم زۆر سەرقاڵە و بێ وەستان بیر دەکەمەوە (Overthinking)، تکایە ڕێگایەکم پێ بڵێ بۆ ئارامبوونەوە."),
        MoodOption("😟", "دڵەڕاوکێ و ترس", "هەست بە دڵەڕاوکێ (Anxiety) و ترسی بێ هۆکار دەکەم، لێدانی دڵم خێرایە، چۆن هێور ببمەوە؟"),
        MoodOption("😞", "بێتاقەتی و خەمۆکی", "ئەم ماوەیە زۆر بێتاقەتم، تاقەتی هیچم نییە و دەروونم ماندووە، ئامۆژگاریت چییە بۆم؟"),
        MoodOption("💔", "کێشەی پەیوەندی", "لە پەیوەندییە سۆزدارییەکەمدا کێشەم هەیە، هەست بە دڵشکاوی و تێنەگەیشتن دەکەم."),
        MoodOption("😤", "توڕەیی و شڵەژان", "زوو کۆنتڕۆڵی خۆم لەدەست دەدەم و لەسەر شتی کەم توڕە دەبم، چۆن زاڵ بم بەسەر توڕەییمدا؟"),
        MoodOption("🎯", "متمانە بەخۆبوون", "متمانەم بەخۆم کەمە و لە قسەکردن لەناو خەڵک شەرم دەکەم، چۆن متمانەم بەهێز بکەم؟")
    )

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ChatRepository(database.chatDao(), secureKeyManager)
        messages = repository.allMessages.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed welcome greeting on clean launch
        viewModelScope.launch {
            if (repository.getMessageCount() == 0) {
                val welcome = ChatMessage(
                    text = """
سڵاو و ڕێز لە چاوتان، بەخێربێن بۆ لای «دکتۆر بەستە» 🧠

من لێرەم وەک ڕاوێژکاری دەروونی تۆ بۆ ئەوەی بە دڵسۆزی گوێت لێ بگرم دەربارەی هەموو ئەو هەستانەی کە لە ناختدا دروست دەبن؛ لە زۆر بیرکردنەوە (Overthinking)، دڵەڕاوکێ، سترێسی ڕۆژانە، کێشەی پەیوەندییەکان، بێتاقەتی و تەنیایی.

بە کوردییەکی سادە و دۆستانە لەگەڵم بدوێ. دەتوانیت لە ڕێگەی بابەتەکانی خوارەوە دەست پێبکەیت یان ڕاستەوخۆ کێشەکەت بنووسیت.
                    """.trimIndent(),
                    sender = "counselor",
                    timestamp = System.currentTimeMillis()
                )
                repository.saveMessage(welcome)
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
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

    fun regenerateResponse(counselorMessage: ChatMessage) {
        if (_isLoading.value) return
        val currentList = messages.value
        val counselorIndex = currentList.indexOfFirst { it.id == counselorMessage.id }
        if (counselorIndex <= 0) return

        // Find preceding user message
        val precedingUserMessage = currentList.subList(0, counselorIndex).lastOrNull { it.isUser }
        val prompt = precedingUserMessage?.text ?: return

        _isLoading.value = true
        viewModelScope.launch {
            val historyUpToUser = currentList.subList(0, counselorIndex)
            repository.consultDrBasta(
                userPrompt = prompt,
                recentHistory = historyUpToUser,
                targetMessageIdToUpdate = counselorMessage.id
            )
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
            val welcome = ChatMessage(
                text = "سڵاو لە چاوت! گفتوگۆیەکی نوێمان دەستپێکرد. هەر کاتێک ئامادە بوویت، من لێرەم و گوێت لێ دەگرم 🧠",
                sender = "counselor",
                timestamp = System.currentTimeMillis()
            )
            repository.saveMessage(welcome)
        }
    }

    // Settings actions
    fun saveApiKey(rawKey: String): Boolean {
        val success = secureKeyManager.saveApiKey(rawKey)
        if (success) {
            _maskedApiKey.value = secureKeyManager.getMaskedApiKey()
            _testResultMessage.value = "کلیلی API بە سەرکەوتوویی لە کۆگای پارێزراوی Keystore پاشەکەوت کرا!"
            _testSuccess.value = true
        } else {
            _testResultMessage.value = "هەڵە ڕوویدا لە پاشەکەوتکردنی کلیلەکە."
            _testSuccess.value = false
        }
        return success
    }

    fun clearApiKey() {
        secureKeyManager.clearApiKey()
        _maskedApiKey.value = null
        _testResultMessage.value = "کلیلی پاشەکەوتکراو سڕایەوە."
        _testSuccess.value = null
    }

    fun setSelectedModel(model: String) {
        secureKeyManager.saveSelectedModel(model)
        _selectedModel.value = model
    }

    fun testApiKey(keyToTest: String? = null) {
        val key = keyToTest?.takeIf { it.isNotBlank() } ?: secureKeyManager.getApiKey()
        if (key.isNullOrBlank()) {
            _testResultMessage.value = "هیچ کلیلێک دیاری نەکراوە بۆ تاقیکردنەوە."
            _testSuccess.value = false
            return
        }

        _isTestingKey.value = true
        _testResultMessage.value = null
        _testSuccess.value = null

        viewModelScope.launch {
            val result = repository.testApiKeyConnection(key, _selectedModel.value)
            _isTestingKey.value = false
            if (result.isSuccess) {
                _testResultMessage.value = result.getOrNull()
                _testSuccess.value = true
            } else {
                _testResultMessage.value = result.exceptionOrNull()?.message ?: "هەڵەی نەزانراو ڕوویدا."
                _testSuccess.value = false
            }
        }
    }

    fun resetSettings() {
        secureKeyManager.resetAllSettings()
        _maskedApiKey.value = null
        _selectedModel.value = SecureKeyManager.DEFAULT_MODEL
        _testResultMessage.value = "هەموو ڕێکخستنەکان گەڕێنرانەوە بۆ دۆخی سەرەتا."
        _testSuccess.value = null
    }

    fun clearTestStatus() {
        _testResultMessage.value = null
        _testSuccess.value = null
    }

    // Dialog toggles
    fun openBreathingDialog() { _showBreathingDialog.value = true }
    fun closeBreathingDialog() { _showBreathingDialog.value = false }
    fun openEmergencyDialog() { _showEmergencyDialog.value = true }
    fun closeEmergencyDialog() { _showEmergencyDialog.value = false }
    fun openAboutDialog() { _showAboutDialog.value = true }
    fun closeAboutDialog() { _showAboutDialog.value = false }
}
