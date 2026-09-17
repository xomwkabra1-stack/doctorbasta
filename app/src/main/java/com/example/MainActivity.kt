package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val chatViewModel: ChatViewModel = viewModel()
                    val currentScreen by chatViewModel.currentScreen.collectAsStateWithLifecycle()

                    BackHandler(enabled = currentScreen == AppScreen.SETTINGS) {
                        chatViewModel.navigateTo(AppScreen.CHAT)
                    }

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            AppScreen.CHAT -> ChatScreen(viewModel = chatViewModel)
                            AppScreen.SETTINGS -> SettingsScreen(viewModel = chatViewModel)
                        }
                    }
                }
            }
        }
    }
}
